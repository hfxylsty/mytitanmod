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
     * <p><b>体型依据原版设定</b>：32 格高 × 8 格宽（≈16 倍僵尸）。
     * 碰撞箱必须与模型尺寸严格对应，否则会出现"看着打中了却打不到"：
     * 模型 512 像素高 = 32 格，128 像素宽 = 8 格（1 格 = 16 像素）。
     *
     * <p>追踪范围给 32：32 格高的实体在默认 5 区块下会在视野里"突然出现"。
     * 更新间隔给 2，降低巨型实体的同步开销。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<TitanZombie>> TITAN_ZOMBIE =
            ENTITY_TYPES.register("titan_zombie",
                    () -> EntityType.Builder.of(TitanZombie::new, MobCategory.MONSTER)
                            .sized(8.0F, 32.0F)
                            .clientTrackingRange(32)
                            .updateInterval(2)
                            .build("titan_zombie"));
}
