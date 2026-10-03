package com.titanreborn.client.renderer;

import com.titanreborn.client.model.RottenColossusModel;
import com.titanreborn.entity.TitanZombie;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 腐骸泰坦渲染器（GeckoLib）。
 *
 * <p>替换掉 M0 阶段那个"6 倍缩放的原版僵尸"方案。
 * 模型来自 {@code geo/entity/rotten_colossus.geo.json}（Bedrock 几何格式），
 * 由 {@link RottenColossusModel} 按约定推导路径。
 *
 * <p><b>关于缩放</b>：Bedrock 模型的坐标单位是像素（1 格 = 16 像素），
 * 我们的模型高约 190 像素 ≈ 11.9 格，与碰撞箱 {@code sized(4.0F, 12.0F)} 基本对齐，
 * 所以这里不再额外缩放。若以后调整模型尺寸，必须同步改
 * {@code ModEntities.TITAN_ZOMBIE} 的碰撞箱，否则会出现"看着打中了却打不到"。
 */
public class RottenColossusRenderer extends GeoEntityRenderer<TitanZombie> {

    public RottenColossusRenderer(EntityRendererProvider.Context context) {
        super(context, new RottenColossusModel());
        // 巨型实体的阴影半径
        this.shadowRadius = 3.0F;
    }

    /** 贴图路径由 RottenColossusModel 推导，这里仅作日志/调试用途时可用。 */
    @Override
    public ResourceLocation getTextureLocation(TitanZombie animatable) {
        return super.getTextureLocation(animatable);
    }
}
