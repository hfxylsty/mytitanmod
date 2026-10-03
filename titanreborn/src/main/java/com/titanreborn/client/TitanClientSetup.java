package com.titanreborn.client;

import com.titanreborn.ModEntities;
import com.titanreborn.TitanReborn;
import com.titanreborn.client.renderer.RottenColossusRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 客户端初始化。
 *
 * <p><b>必须在 {@code value = Dist.CLIENT} 下注册</b>：专用服务端没有这些类，
 * 若在通用事件里引用客户端类，服务器启动会崩。
 *
 * <p>M0.4 起渲染器换成 GeckoLib 实现（{@link RottenColossusRenderer}）。
 * 旧的 {@code TitanZombieRenderer}（原版 HumanoidModel + 6 倍缩放）保留文件作为对照，
 * 但**不再注册**——同一实体类型只能有一个渲染器。
 */
@EventBusSubscriber(modid = TitanReborn.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TitanClientSetup {

    private TitanClientSetup() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TITAN_ZOMBIE.get(), RottenColossusRenderer::new);
    }
}
