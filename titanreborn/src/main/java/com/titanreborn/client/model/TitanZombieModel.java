package com.titanreborn.client.model;

import com.titanreborn.TitanReborn;
import com.titanreborn.entity.TitanZombie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * 僵尸泰坦的 GeckoLib 模型。
 *
 * <p>{@link DefaultedEntityGeoModel} 按固定约定推导三个资源路径
 * （依据 GeckoLib 4.9.3 的 {@code DefaultedGeoModel}）：
 * <ul>
 *   <li>模型：{@code assets/<ns>/geo/entity/<path>.geo.json}</li>
 *   <li>贴图：{@code assets/<ns>/textures/entity/<path>.png}</li>
 *   <li>动画：{@code assets/<ns>/animations/entity/<path>.animation.json}</li>
 * </ul>
 *
 * <p>路径名用 {@code titan_zombie}，与实体注册 id 保持一致——
 * 这样"注册名 / 资源名 / 类名"三者统一，找文件时不需要二次翻译。
 *
 * <p>构造参数 true 表示启用头部朝向自动跟随（骨骼名必须叫 {@code head}）。
 */
public class TitanZombieModel extends DefaultedEntityGeoModel<TitanZombie> {

    public static final ResourceLocation ASSET_PATH =
            ResourceLocation.fromNamespaceAndPath(TitanReborn.MODID, "titan_zombie");

    /**
     * 贴图改用**游戏内原版僵尸贴图**。
     *
     * <p>2026-10-12 确定的外观基线是「原版僵尸等比放大 16 倍 + 同比例铁剑」，
     * 模型 UV 已按原版 64×64 贴图对齐（见 geo 文件的 {@code texture_width/height: 64}），
     * 所以这里不再用模组自带的 {@code titanreborn:textures/entity/titan_zombie.png}。
     *
     * <p>原版贴图路径取自 1.21.1 客户端资源包（已核对 jar 内条目）：
     * {@code assets/minecraft/textures/entity/zombie/zombie.png}（64×64）。
     */
    public static final ResourceLocation VANILLA_ZOMBIE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    public TitanZombieModel() {
        super(ASSET_PATH, true);
    }

    @Override
    public ResourceLocation getTextureResource(TitanZombie animatable) {
        return VANILLA_ZOMBIE_TEXTURE;
    }
}
