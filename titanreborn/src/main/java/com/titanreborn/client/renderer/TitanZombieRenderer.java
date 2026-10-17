package com.titanreborn.client.renderer;

import com.titanreborn.client.model.TitanZombieModel;
import com.titanreborn.entity.TitanZombie;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 僵尸泰坦渲染器（GeckoLib）。
 *
 * <p>模型来自 {@code geo/entity/titan_zombie.geo.json}，由 {@link TitanZombieModel}
 * 按约定推导路径（模型 {@code geo/entity/}、贴图 {@code textures/entity/}、
 * 动画 {@code animations/entity/}）。
 *
 * <p><b>尺寸</b>：模型包围盒 256 × 512 像素 = <b>16 × 32 格</b>（1 格 = 16 像素），
 * 与碰撞箱 {@code sized(16.0F, 32.0F)} 严格对应，**因此不需要额外缩放**。
 * 若调整模型尺寸，必须同步改 {@code ModEntities.TITAN_ZOMBIE} 的碰撞箱。
 *
 * <p><b>已移除的渲染层</b>：
 * <ul>
 *   <li>{@code TitanArmorLayer}（低血量亮绿护甲）——2026-10-12 定的外观基线是
 *       「原版僵尸 ×16 + 同比例铁剑」，不做护甲层；类保留在工程里，将来要用再挂回来；</li>
 *   <li>{@code TitanSwordLayer}（手持铁剑）——2026-10-17 改为把剑直接烘进模型
 *       （geo 新增 {@code sword} 骨骼），避免运行时坐标换算。</li>
 * </ul>
 *
 * <p>橙黄色周身粒子由 {@code client/TitanParticles} 负责，不在渲染器里。
 */
public class TitanZombieRenderer extends GeoEntityRenderer<TitanZombie> {

    public TitanZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new TitanZombieModel());
        // 32 格高的实体，阴影相应放大
        this.shadowRadius = 8.0F;
    }
}
