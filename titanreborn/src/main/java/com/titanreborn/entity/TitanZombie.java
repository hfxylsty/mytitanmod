package com.titanreborn.entity;

import com.titanreborn.entity.part.TitanPart;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 僵尸泰坦（Zombie Titan）—— M0/M1 的验证实体。
 *
 * <p>M0 验证三件事：
 * <ol>
 *   <li>M0.2 实体能注册、能生成、能渲染；</li>
 *   <li>M0.3 多部件能被击中并把伤害转发给宿主；</li>
 *   <li>M0.5 伤害事件能改写最终伤害。</li>
 * </ol>
 *
 * <p>M0.4 起改用 GeckoLib 渲染（Bedrock 几何模型 + 关键帧动画）：
 * 实现 {@link GeoEntity} 让实体携带动画能力，模型见
 * {@code client/model/TitanZombieModel}。
 *
 * <p>多部件实现依据 NeoForge 的 {@code IEntityExtension}：
 * 覆写 {@code isMultipartEntity()} 与 {@code getParts()}，部件继承
 * {@link net.neoforged.neoforge.entity.PartEntity}。
 * <b>宿主负责每 tick 摆放部件</b>（部件不存档、不独立同步）。
 *
 * <p><b>部件名与模型骨骼名必须一致</b>：破防逻辑按名字定位甲片，
 * 而视觉位置由 GeckoLib 的骨骼决定（见 {@code geo/entity/titan_zombie.geo.json}）。
 */
public class TitanZombie extends Monster implements GeoEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger("titanreborn/part");

    // ---- 多部件偏移（2026-10-12 按「原版僵尸 ×16」新模型重算）----
    // 单位：方块。x=横向(右为正)，y=高度(脚底为 0)，z=前后(**-Z 是正面**)。
    //
    // ⚠️ 关键：PartEntity.moveToOwner 走的是 Entity.setPos，而 setPos 给的是
    // **包围盒底面中心**（包围盒在 y 方向是 [y, y+height]），不是盒子中心。
    // 所以下面填的是模型对应立方体的**底面 Y**，不是 pivot、也不是几何中心。
    // 旧版把骨骼 pivot（中心）填了进来，导致所有部件整体偏高 1.5~4 格——
    // 表现就是"看着打中了却打不到"。这次按新模型重算修正。
    //
    //   头部：立方体 origin[-64,384,-64] size[128,128,128]
    //         → 格 x[-4,4] y[24,32] z[-4,4]   （8×8×8，与部件尺寸完全一致）
    //   躯干：立方体 origin[-64,192,-32] size[128,192,64]
    //         → 格 x[-4,4] y[12,24] z[-2,2]   （8 宽 × 12 高 × 4 厚）
    //         ⚠️ 部件用 EntityDimensions.scalable(宽,高)，水平截面是正方形，
    //            厚度无法单独设置，所以躯干部件在 z 方向会比模型厚 4 格。

    /** 头部部件：底面 y=24，8 × 8 格。 */
    private static final Vec3 HEAD_OFFSET = new Vec3(0.0D, 24.0D, 0.0D);

    /** 躯干部件：底面 y=12，8 宽 × 12 高。 */
    private static final Vec3 CHEST_OFFSET = new Vec3(0.0D, 12.0D, 0.0D);

    // ---- GeckoLib 动画（M0.4）----

    /** 控制器名，供调试与日志使用。 */
    private static final String MAIN_CONTROLLER = "main";

    /** 自动逻辑用的两个循环动画（对应 {@link TitanAction#IDLE} / {@link TitanAction#WALK}）。 */
    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.titan_zombie.idle");
    private static final RawAnimation WALK =
            RawAnimation.begin().thenLoop("animation.titan_zombie.walk");

    /**
     * 攻击动画（播放一次）。
     *
     * <p>定义为常量而不是每 tick 新建：状态机 lambda 每 tick 都会执行，
     * 在里面 {@code new RawAnimation} 会持续产生垃圾对象。
     */
    private static final RawAnimation ATTACK_ONCE =
            RawAnimation.begin().then(
                    TitanAction.ATTACK.animationName(),
                    software.bernie.geckolib.animation.Animation.LoopType.PLAY_ONCE);

    /**
     * 攻击动画时长（tick）。用于战斗驱动的攻击自动回落到自动逻辑。
     *
     * <p>依据交付模型的动作时长：attack = 1.2 秒 = 24 tick，留一点余量取 26。
     * <b>注意</b>：这里写死时长是权宜之计；若以后换了攻击动画时长，必须同步改。
     */
    private static final int ATTACK_DURATION_TICKS = 26;

    /** 战斗驱动的攻击剩余 tick；> 0 时到点自动回 {@link TitanAction#NORMAL}。 */
    private int attackTicks;

    /**
     * 动作状态的**同步数据**。
     *
     * <p>⚠️ 这里踩过一个让"状态机看起来是死的"的大坑：动画是在**客户端**渲染时选的
     * （{@code registerControllers} 的 lambda 跑在渲染线程），而状态切换发生在
     * **服务端**（战斗逻辑、调试杖）。早先用的是普通 Java 字段，改动永远传不到客户端，
     * 于是客户端手里始终是初始值 {@link TitanAction#NORMAL}：
     * <ul>
     *   <li>调试杖/战斗设的 {@code ATTACK} 只在服务端日志里看得见，模型永远不播；</li>
     *   <li>游戏内浮层读到的"动作"也一直是「正常（自动）」，与服务端日志互相矛盾
     *       （2026-10-11 实测：服务端日志写「攻击 attack」，浮层同时显示「正常（自动）」）。</li>
     * </ul>
     * 所以状态必须走 {@link SynchedEntityData}（原版实体字段同步），
     * 服务端一改，客户端下一 tick 就能看到。
     */
    private static final EntityDataAccessor<Integer> DATA_ACTION =
            SynchedEntityData.defineId(TitanZombie.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    private final TitanPart[] parts;
    private final TitanPart head;
    private final TitanPart chest;

    public TitanZombie(EntityType<? extends TitanZombie> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 500;

        // 部件在构造时建好并缓存（NeoForge 注释要求 getParts() 返回缓存值）
        // 尺寸取模型对应立方体的包围盒（像素/16）。
        // 2026-10-12：外观基线改为「原版僵尸 ×16」，三片腐骨甲片已从设计里去掉，
        // 所以部件收敛成**头 + 胸**两个（四肢不做单独判定，命中即算在宿主身上）。
        this.head = new TitanPart(this, "head", 8.0F, 8.0F, HEAD_OFFSET);
        this.chest = new TitanPart(this, "chest", 8.0F, 12.0F, CHEST_OFFSET);
        this.parts = new TitanPart[] { head, chest };
    }

    /** 属性在 {@link com.titanreborn.ModAttributes} 里通过 {@code EntityAttributeCreationEvent} 注册。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 2048.0D)      // 设计值，待平衡
                .add(Attributes.ATTACK_DAMAGE, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ARMOR, 22.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    // ==================== AI（M1） ====================

    /**
     * 移动与攻击的 AI 目标（goals）。
     *
     * <p>⚠️ 2026-10-12 排查：本类原先**一个 goal 都没注册**。{@code Monster} 只提供属性，
     * 既不会自己走路也不会打人，于是泰坦杵在原地不动——这直接导致：
     * <ul>
     *   <li>动画状态机的「自动」档永远选不到 walk（实体根本没在移动）；</li>
     *   <li>近战入口 {@link #doHurtTarget} 永远不会被调用，攻击动画永远不播。</li>
     * </ul>
     * 也就是说，"动画像死的"有一半原因不是动画，而是**实体本身没有行为**。
     *
     * <p>这里按原版僵尸的最小组合补上：靠近并攻击玩家、随机踱步、会看人。
     * 9.6 格宽的巨型生物寻路不一定处处可行，先保证「会动、会打」，
     * 卡住的位置等实际试玩再调（例如换 {@code RandomStrollGoal} 或自定义移动控制）。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ==================== 动作状态机（M1） ====================

    /**
     * 状态机核心（**跑在客户端渲染线程**）。
     *
     * <p>选哪个动画只依据 {@link #getAction()}（同步数据）与自动逻辑；
     * 这里不做任何服务端才有的判断。
     *
     * <p>每 tick 都调用是安全的：GeckoLib 的 {@code AnimationController.setAnimation}
     * 会先比较 {@code RawAnimation}，相等且无需重载时直接跳过（读过 4.9.3 字节码确认），
     * 不会把动画重置回第 0 帧。
     *
     * <p>{@code NORMAL} 分支的移动判断做了两层：GeckoLib 的 {@code state.isMoving()}
     * 加一层实体自身速度兜底——泰坦体型大、移动慢，只靠前者容易判成"静止"而一直播待机。
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<TitanZombie> main = new AnimationController<>(this, MAIN_CONTROLLER, 0, state -> {
            TitanAction current = getAction();

            if (current == TitanAction.ATTACK) {
                return state.setAndContinue(ATTACK_ONCE);
            }
            if (current == TitanAction.IDLE) {
                return state.setAndContinue(IDLE);
            }
            if (current == TitanAction.WALK) {
                return state.setAndContinue(WALK);
            }

            // NORMAL：交回自动逻辑
            boolean moving = state.isMoving() || this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;
            return state.setAndContinue(moving ? WALK : IDLE);
        });
        controllers.add(main);
    }

    /**
     * 注册同步数据。
     *
     * <p>动作状态是服务端改、客户端读，必须走同步通道（原因见 {@link #DATA_ACTION}）。
     */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, TitanAction.NORMAL.ordinal());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    /** 当前动作状态（客户端读到的就是服务端同步过来的值）。 */
    public TitanAction getAction() {
        TitanAction[] all = TitanAction.values();
        int index = this.entityData.get(DATA_ACTION);
        return (index >= 0 && index < all.length) ? all[index] : TitanAction.NORMAL;
    }

    /**
     * 切换动作状态。
     *
     * <p>只在状态真的变化时记录日志，避免刷屏。写入的是同步数据，
     * 所以客户端渲染线程下一 tick 就能拿到新状态。
     *
     * @param newAction 目标状态；{@link TitanAction#NORMAL} 表示交回自动逻辑
     * @return 是否真的发生了状态变化
     */
    public boolean setAction(TitanAction newAction) {
        TitanAction old = getAction();
        if (old == newAction) {
            return false;
        }
        this.entityData.set(DATA_ACTION, newAction.ordinal());
        LOGGER.info("[动作状态机] {} -> {} （实体 {}）",
                old.displayName(), newAction.displayName(), this.getId());
        return true;
    }

    /** 循环到下一个调试动作（供调试物品使用）。 */
    public TitanAction cycleAction() {
        // 调试切换时清掉攻击计时，避免上一次战斗攻击的余量把状态拉回去
        this.attackTicks = 0;
        TitanAction next = getAction().next();
        setAction(next);
        return next;
    }

    /**
     * 播放一次攻击动画（由战斗逻辑调用），**到点自动回落到自动逻辑**。
     *
     * <p>与调试杖的区别：调试杖用 {@link #cycleAction()}，动作会一直锁定住；
     * 战斗驱动的攻击必须能自己结束，否则泰坦会一直举着剑。
     */
    public void playAttackAnimation() {
        if (setAction(TitanAction.ATTACK)) {
            this.attackTicks = ATTACK_DURATION_TICKS;
        }
    }

    // ==================== 多部件（M0.3） ====================

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Nullable
    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    @Override
    public void tick() {
        super.tick();

        // 战斗驱动的攻击到点自动回落（否则泰坦会一直举着剑）
        if (attackTicks > 0 && --attackTicks == 0 && getAction() == TitanAction.ATTACK) {
            setAction(TitanAction.NORMAL);
        }

        // 宿主负责摆放部件：必须每 tick 做，否则部件会留在原地
        float yaw = this.getYRot();
        double yawRad = Math.toRadians(yaw);
        for (TitanPart part : parts) {
            part.moveToOwner(yaw, yawRad);
        }
        // 部件不 tick 自身的 AI/物理，但仍需参与基础更新
        for (TitanPart part : parts) {
            part.setOldPosAndRot();
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        // 宿主移除时同步移除部件，避免残留
        for (TitanPart part : parts) {
            part.remove(reason);
        }
        super.remove(reason);
    }

    /**
     * 部件受击入口。{@link TitanPart#hurt} 会转发到这里。
     *
     * <p>M0.3 的验证点：日志里必须能区分是哪个部件被打中。
     *
     * <p>2026-10-12：三片腐骨甲片已随外观基线简化去掉，破防判定因此暂时停用
     * （见 {@code combat/TitanDamageRules}），新的破防条件等机制方案确定后再实现。
     */
    public boolean hurtPart(TitanPart part, DamageSource source, float amount) {
        LOGGER.info("[M0.3] 命中部件={} 数量={} 伤害={} 来源={}",
                part.getPartName(), part.getHitCount(), amount, source.getMsgId());

        // 把伤害交给宿主统一处理（伤害规则在 M0.5 的 LivingIncomingDamageEvent 里）
        return super.hurt(source, amount);
    }

    // ==================== 泰坦基础规则 ====================

    /**
     * 近战命中时播一次攻击动画。
     *
     * <p>2026-10-11 排查发现的缺口：{@link #playAttackAnimation()} **没有任何调用方**——
     * 也就是说泰坦真的打人时永远不会播攻击动作，只有调试杖能手动切到那个动作。
     * 这里把它接到原版近战入口 {@code Mob.doHurtTarget}：服务端打中就置状态，
     * 再由 {@link #DATA_ACTION} 同步给客户端渲染线程。
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            playAttackAnimation();
        }
        return hurt;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;   // 泰坦免疫摔落
    }

    @Override
    protected boolean isAffectedByFluids() {
        return false;   // 泰坦不受流体推动
    }
}
