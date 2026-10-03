package com.titanreborn.client.renderer;

import com.titanreborn.client.model.TitanZombieModel;
import com.titanreborn.client.renderer.layer.TitanArmorLayer;
import com.titanreborn.client.renderer.layer.TitanSwordLayer;
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
 * <p><b>挂载的渲染层</b>（对应原版外观设定）：
 * <ul>
 *   <li>{@link TitanSwordLayer} —— 手持巨型铁剑（不烘进模型，换武器无需改模型）</li>
 *   <li>{@link TitanArmorLayer} —— 低血量时包覆全身的亮绿透明护甲</li>
 * </ul>
 * 橙黄色周身粒子仍待接入（在实体或客户端 tick 里生成）。
 */
public class TitanZombieRenderer extends GeoEntityRenderer<TitanZombie> {

    public TitanZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new TitanZombieModel());
        // 32 格高的实体，阴影相应放大
        this.shadowRadius = 8.0F;
        // 手持巨型铁剑
        this.addRenderLayer(new TitanSwordLayer(this));
        // 低血量亮绿透明护甲
        this.addRenderLayer(new TitanArmorLayer(this));
    }
}
