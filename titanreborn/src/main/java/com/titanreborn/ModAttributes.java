package com.titanreborn;

import com.titanreborn.entity.TitanZombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/**
 * 实体属性注册。
 *
 * <p>1.21.1 里属性**不能**在实体构造器里注册，必须在 MOD 总线的
 * {@link EntityAttributeCreationEvent} 里做（实体注册之后、common setup 之前）。
 *
 * <p>注意：同一个 {@code EntityType} 重复 put 会抛 {@code IllegalStateException}。
 */
@EventBusSubscriber(modid = TitanReborn.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModAttributes {

    private ModAttributes() {}

    @SubscribeEvent
    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.TITAN_ZOMBIE.get(), TitanZombie.createAttributes().build());
    }
}
