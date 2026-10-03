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
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 腐骸泰坦（Rotten Colossus）—— M0/M1 的验证实体。
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
 * {@code client/model/RottenColossusModel}。
 *
 * <p>多部件实现依据 NeoForge 的 {@code IEntityExtension}：
 * 覆写 {@code isMultipartEntity()} 与 {@code getParts()}，部件继承
 * {@link net.neoforged.neoforge.entity.PartEntity}。
 * <b>宿主负责每 tick 摆放部件</b>（部件不存档、不独立同步）。
 *
 * <p><b>部件名与模型骨骼名必须一致</b>：破防逻辑按名字定位甲片，
 * 而视觉位置由 GeckoLib 的骨骼决定（见 {@code geo/entity/rotten_colossus.geo.json}）。
 */
public class TitanZombie extends Monster implements GeoEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger("titanreborn/part");

    /** 部件偏移：x=横向(右为正)，y=高度，z=前后(前为正)，单位=方块。 */
    private static final double PLATE_HEIGHT = 7.0D;
    private static final double PLATE_FORWARD = 0.7D;

    // ---- GeckoLib 动画（M0.4）----

    /** 控制器名，供 triggerAnim 使用。 */
    private static final String MAIN_CONTROLLER = "main";

    /** 动画名，必须与 animations/entity/rotten_colossus.animation.json 里的键一致。 */
    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.rotten_colossus.idle");
    private static final RawAnimation WALK =
            RawAnimation.begin().thenLoop("animation.rotten_colossus.walk");
    private static final RawAnimation ATTACK =
            RawAnimation.begin().thenPlay("animation.rotten_colossus.attack");

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
        this.head = new TitanPart(this, "head", 3.0F, 3.0F, new Vec3(0.0D, 10.5D, 1.2D));
        this.chest = new TitanPart(this, "chest", 4.0F, 4.0F, new Vec3(0.0D, 6.5D, 0.5D));
        this.bonePlates = new TitanPart[] {
                new TitanPart(this, "bone_plate_0", 1.6F, 1.6F, new Vec3(-1.2D, PLATE_HEIGHT, PLATE_FORWARD)),
                new TitanPart(this, "bone_plate_1", 1.6F, 1.6F, new Vec3(0.0D, PLATE_HEIGHT, PLATE_FORWARD)),
                new TitanPart(this, "bone_plate_2", 1.6F, 1.6F, new Vec3(1.2D, PLATE_HEIGHT, PLATE_FORWARD)),
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

    // ==================== GeckoLib 动画（M0.4） ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<TitanZombie> main =
                new AnimationController<>(this, MAIN_CONTROLLER, 5, state -> {
                    // 攻击动画用 triggerAnim 触发，这里只负责待机/行走的循环
                    if (state.isMoving()) {
                        return state.setAndContinue(WALK);
                    }
                    return state.setAndContinue(IDLE);
                });
        // 可触发动画必须在控制器里登记，否则 triggerAnim 找不到
        main.triggerableAnim("attack", ATTACK);
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    /** 播放一次攻击动画（由战斗逻辑调用）。 */
    public void playAttackAnimation() {
        this.triggerAnim(MAIN_CONTROLLER, "attack");
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
