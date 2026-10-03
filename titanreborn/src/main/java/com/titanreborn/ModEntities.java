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
     * M0 验证实体：僵尸泰坦。
     *
     * <p>体型先给 4.0 × 12.0（约 6 倍僵尸），用于验证"巨型实体"的碰撞箱、
     * 追踪范围与渲染缩放是否对齐。最终倍率见 docs/10-content-roster.md（设计值，待平衡）。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<TitanZombie>> TITAN_ZOMBIE =
            ENTITY_TYPES.register("titan_zombie",
                    () -> EntityType.Builder.of(TitanZombie::new, MobCategory.MONSTER)
                            .sized(4.0F, 12.0F)
                            .clientTrackingRange(16)   // 巨型实体：默认 5 太近，远处不渲染
                            .updateInterval(2)
                            .build("titan_zombie"));
}
