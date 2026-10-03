package com.titanreborn;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Titan Reborn 主类。
 *
 * <p>只做一件事：把各个 {@code DeferredRegister} 挂到 mod 事件总线上。
 * 业务逻辑一律不放这里。
 *
 * <p><b>注意</b>：新增注册器必须在此登记，否则内容不会进注册表
 * （典型症状：编译通过、游戏能开，但物品/实体不存在）。
 */
@Mod(TitanReborn.MODID)
public class TitanReborn {

    public static final String MODID = "titanreborn";

    public TitanReborn(IEventBus bus) {
        // M0.2: 实体与物品注册
        ModEntities.ENTITY_TYPES.register(bus);
        ModItems.ITEMS.register(bus);
    }
}
