package com.titanreborn.entity;

import com.titanreborn.entity.part.TitanPart;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
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

    // ---- 多部件偏移 ----
    // 单位：方块。x=横向(右为正)，y=高度(脚底为 0)，z=前后(**-Z 是正面**)。
    // 数值直接取自交付模型 ge o的骨骼 pivot 与立方体范围（1 格 = 16 像素）：
    //   head          pivot(0,384,0)         → (0, 24.0, 0)
    //   bone_plate_0  pivot(-34,312,-37)     → (-2.125, 19.5, -2.3125)
    //   bone_plate_1  pivot(-2,298,-37)      → (-0.125, 18.625, -2.3125)
    //   bone_plate_2  pivot(30,312,-37)      → (1.875, 19.5, -2.3125)
    // 注意：模型正面朝 -Z，所以甲片的 z 是负值。

    /** 三片腐骨甲片的中心偏移（必须与模型骨骼对位，否则"看得见打不到"）。 */
    private static final Vec3[] PLATE_OFFSETS = {
            new Vec3(-2.125D, 19.50D, -2.3125D),
            new Vec3(-0.125D, 18.625D, -2.3125D),
            new Vec3(1.875D, 19.50D, -2.3125D),
    };

    /** 头部部件中心（模型 head 中心约 Y=448 像素 = 28 格）。 */
    private static final Vec3 HEAD_OFFSET = new Vec3(0.0D, 28.0D, 0.0D);

    /** 躯干中心（模型 torso 段约 Y=152~368 像素）。 */
    private static final Vec3 TORSO_OFFSET = new Vec3(0.0D, 16.25D, 0.0D);

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
     * 动画状态机的当前状态。
     *
     * <p>{@link TitanAction#NORMAL} = 交回自动逻辑（移动则 walk、静止则 idle）；
     * 其他值 = 手动锁定在该动作。调试物品与战斗逻辑都通过
     * {@link #setAction(TitanAction)} 改变它。
     */
    private TitanAction action = TitanAction.NORMAL;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    private final TitanPart[] parts;
    private final TitanPart head;
    private final TitanPart chest;
    private final TitanPart[] bonePlates;

    public TitanZombie(EntityType<? extends TitanZombie> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 500;

        // 部件在构造时建好并缓存（NeoForge 注释要求 getParts() 返回缓存值）
        // 尺寸按模型对应骨骼的立方体包围盒给（像素/16）
        this.head = new TitanPart(this, "head", 8.0F, 8.0F, HEAD_OFFSET);
        this.chest = new TitanPart(this, "chest", 9.0F, 9.0F, TORSO_OFFSET);
        this.bonePlates = new TitanPart[] {
                new TitanPart(this, "bone_plate_0", 1.75F, 3.0F, PLATE_OFFSETS[0]),
                new TitanPart(this, "bone_plate_1", 1.75F, 3.0F, PLATE_OFFSETS[1]),
                new TitanPart(this, "bone_plate_2", 1.75F, 3.0F, PLATE_OFFSETS[2]),
        };
        this.parts = new TitanPart[] { head, chest, bonePlates[0], bonePlates[1], bonePlates[2] };
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

    // ==================== 动作状态机（M1） ====================

    /**
     * 状态机核心。
     *
     * <p><b>为什么用 {@code setAnimation} 而不是 {@code setAndContinue}</b>：
     * {@code setAndContinue} 会把动画走"过渡队列"（transition），过渡期间需要若干 tick
     * 才真正生效；而 {@code setAnimation} 是**立即替换当前动画**。
     * 我们的调试场景需要"按下就立刻看到那个动作"，所以用立即替换。
     *
     * <p>每 tick 都调用 {@code setAnimation} 是安全的：传入的 {@link RawAnimation}
     * 是静态常量，GeckoLib 用值比较（{@code RawAnimation} 覆写了 equals/hashCode），
     * 相同动画不会重播。
     *
     * <pre>
     *   action 是具体动作（调试锁定）→ 播该动作
     *   action == NORMAL            → 移动播 walk，静止播 idle
     * </pre>
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<TitanZombie> main = new AnimationController<>(this, MAIN_CONTROLLER, 0, state -> {
            if (action == TitanAction.ATTACK) {
                state.setAnimation(ATTACK_ONCE);
            } else if (action == TitanAction.IDLE) {
                state.setAnimation(IDLE);
            } else if (action == TitanAction.WALK) {
                state.setAnimation(WALK);
            } else {
                // NORMAL：交回自动逻辑
                state.setAnimation(state.isMoving() ? WALK : IDLE);
            }
            return PlayState.CONTINUE;
        });
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    /** 当前动作状态。 */
    public TitanAction getAction() {
        return action;
    }

    /**
     * 切换动作状态。
     *
     * <p>只在状态真的变化时记录日志，避免刷屏。
     *
     * @param newAction 目标状态；{@link TitanAction#NORMAL} 表示交回自动逻辑
     * @return 是否真的发生了状态变化
     */
    public boolean setAction(TitanAction newAction) {
        if (this.action == newAction) {
            return false;
        }
        TitanAction old = this.action;
        this.action = newAction;
        LOGGER.info("[动作状态机] {} -> {} （实体 {}）",
                old.displayName(), newAction.displayName(), this.getId());
        return true;
    }

    /** 循环到下一个调试动作（供调试物品使用）。 */
    public TitanAction cycleAction() {
        // 调试切换时清掉攻击计时，避免上一次战斗攻击的余量把状态拉回去
        this.attackTicks = 0;
        setAction(this.action.next());
        return this.action;
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

    public TitanPart[] getBonePlates() {
        return bonePlates;
    }

    @Override
    public void tick() {
        super.tick();

        // 战斗驱动的攻击到点自动回落（否则泰坦会一直举着剑）
        if (attackTicks > 0 && --attackTicks == 0 && action == TitanAction.ATTACK) {
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
     */
    public boolean hurtPart(TitanPart part, DamageSource source, float amount) {
        int broken = 0;
        for (TitanPart plate : bonePlates) {
            if (plate.getHitCount() > 0) {
                broken++;
            }
        }
        LOGGER.info("[M0.3] 命中部件={} 数量={} 伤害={} 甲片已触发={}/{} 来源={}",
                part.getPartName(), part.getHitCount(), amount, broken, bonePlates.length,
                source.getMsgId());

        // 把伤害交给宿主统一处理（伤害规则在 M0.5 的 LivingIncomingDamageEvent 里）
        return super.hurt(source, amount);
    }

    // ==================== 泰坦基础规则 ====================

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;   // 泰坦免疫摔落
    }

    @Override
    protected boolean isAffectedByFluids() {
        return false;   // 泰坦不受流体推动
    }
}
