package com.titanreborn.client.renderer;

import com.titanreborn.client.model.TitanZombieModel;
import com.titanreborn.entity.TitanZombie;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 僵尸泰坦渲染器（GeckoLib）。
 *
 * <p>模型来自 {@code geo/entity/titan_zombie.geo.json}（Bedrock 几何格式），
 * 由 {@link TitanZombieModel} 按约定推导路径：
 * 模型 {@code geo/entity/}、贴图 {@code textures/entity/}、动画 {@code animations/entity/}。
 *
 * <p><b>缩放说明</b>：Bedrock 模型坐标单位是像素（1 格 = 16 像素）。
 * 本模型的包围盒是 128 × 512 像素 = 8 × 32 格，与碰撞箱
 * {@code sized(8.0F, 32.0F)} 严格对应，**因此不需要额外缩放**。
 * 若调整模型尺寸，必须同步改 {@code ModEntities.TITAN_ZOMBIE} 的碰撞箱。
 *
 * <p><b>后续要加的层</b>（M2）：
 * <ul>
 *   <li>手持巨型铁剑（手持物品层，不烘进模型）</li>
 *   <li>橙黄色周身粒子</li>
 *   <li>低血量亮绿色透明护甲（独立护甲层 geo）</li>
 * </ul>
 * 见 {@code docs/31-art-spec-rotten-colossus.md}。
 */
public class TitanZombieRenderer extends GeoEntityRenderer<TitanZombie> {

    public TitanZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new TitanZombieModel());
        // 32 格高的实体，阴影要相应放大
        this.shadowRadius = 6.0F;
    }
}
