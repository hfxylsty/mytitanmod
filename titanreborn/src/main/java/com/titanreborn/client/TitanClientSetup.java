package com.titanreborn.client;

import com.titanreborn.ModEntities;
import com.titanreborn.TitanReborn;
import com.titanreborn.client.renderer.TitanZombieRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 客户端初始化。
 *
 * <p><b>必须在 {@code value = Dist.CLIENT} 下注册</b>：专用服务端没有这些类，
 * 若在通用事件里引用客户端类，服务器启动会崩（M0 验收项 A11 就查这个）。
 */
@EventBusSubscriber(modid = TitanReborn.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TitanClientSetup {

    private TitanClientSetup() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TITAN_ZOMBIE.get(), TitanZombieRenderer::new);
    }
}
