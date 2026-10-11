package com.titanreborn;

import com.titanreborn.entity.TitanZombie;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 实体注册表。
 *
 * <p>1.21.1 的实体注册是**四步链路**，缺一不可（见 docs/02-architecture.md §2.3）：
 * <ol>
 *   <li>本类：注册 {@link EntityType}</li>
 *   <li>{@link ModAttributes}：注册属性（{@code EntityAttributeCreationEvent}）</li>
 *   <li>{@link com.titanreborn.client.TitanClientSetup}：注册渲染器（仅客户端）</li>
 *   <li>{@link ModItems}：注册生成蛋（可选）</li>
 * </ol>
 */
public final class ModEntities {

    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TitanReborn.MODID);

    /**
     * 僵尸泰坦。
     *
     * <p><b>碰撞箱：9.6 × 31.2 格</b>（2026-10-12 按原版僵尸比例改定）。
     *
     * <p>算法：原版僵尸实体是 {@code 0.6 × 1.95} 格，泰坦是它等比放大 16 倍
     * → {@code 0.6 × 16 = 9.6}、{@code 1.95 × 16 = 31.2}。
     *
     * <p><b>为什么不是 16 × 32</b>：16 × 32 来自模型包围盒（256 × 512 像素，含双臂），
     * 那是**视觉**尺寸。原版同样是「模型整体约 1 格宽 / 碰撞箱只有 0.6 格」，
     * 两者本来就不相等；照搬模型包围盒会让判定比身体宽出一大截，
     * 表现为"打空气也掉血"。
     *
     * <p>追踪范围给 32：32 格高的实体在默认 5 区块下会在视野里"突然出现"。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<TitanZombie>> TITAN_ZOMBIE =
            ENTITY_TYPES.register("titan_zombie",
                    () -> EntityType.Builder.of(TitanZombie::new, MobCategory.MONSTER)
                            .sized(9.6F, 31.2F)
                            .clientTrackingRange(32)
                            .updateInterval(2)
                            .build("titan_zombie"));
}
