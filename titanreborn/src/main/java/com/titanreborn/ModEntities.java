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
     * <p><b>体型：32 格高 × 16 格宽</b>（≈16 倍僵尸）。
     * 碰撞箱必须与模型严格对应，否则会"看着打中了却打不到"：
     * 模型包围盒 X[-128,128] Y[0,512] → 256 × 512 像素 = <b>16 × 32 格</b>（1 格 = 16 像素）。
     *
     * <p>宽度依据：模型交付包标注 "BOSS / 32 blocks high / 16 blocks wide"，
     * 且 `validation.json` 的 base_bounds 为 min[-128,0,-72] max[128,512,64]，
     * 故宽取 <b>16</b>（早期文档里的 8 格是过时值，已更正）。
     *
     * <p>追踪范围给 32：32 格高的实体在默认 5 区块下会在视野里"突然出现"。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<TitanZombie>> TITAN_ZOMBIE =
            ENTITY_TYPES.register("titan_zombie",
                    () -> EntityType.Builder.of(TitanZombie::new, MobCategory.MONSTER)
                            .sized(16.0F, 32.0F)
                            .clientTrackingRange(32)
                            .updateInterval(2)
                            .build("titan_zombie"));
}
