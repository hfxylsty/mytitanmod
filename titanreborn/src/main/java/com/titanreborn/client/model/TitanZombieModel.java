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

    public TitanZombieModel() {
        super(ASSET_PATH, true);
    }
}
