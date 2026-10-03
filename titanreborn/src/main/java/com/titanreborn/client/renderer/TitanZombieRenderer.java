package com.titanreborn.client.renderer;

import com.titanreborn.client.model.TitanZombieModel;
import com.titanreborn.entity.TitanZombie;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 僵尸泰坦渲染器。
 *
 * <p>两个关键点：
 * <ol>
 *   <li><b>缩放</b>：覆写 {@code scale} 把模型放大到与碰撞箱匹配。
 *       缩放只影响显示，碰撞箱在 {@code EntityType.Builder.sized()} 里单独设，
 *       两者必须对齐，否则会出现"看起来打中了却打不到"。</li>
 *   <li><b>阴影</b>：巨型实体默认阴影半径偏小，覆写 {@code getShadowRadius} 放大。</li>
 * </ol>
 */
public class TitanZombieRenderer extends MobRenderer<TitanZombie, TitanZombieModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    /** 模型缩放倍率，必须与 ModEntities 里的碰撞箱 4.0 × 12.0 对应（僵尸本体约 0.6 × 1.95）。 */
    private static final float MODEL_SCALE = 6.0F;

    public TitanZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new TitanZombieModel(context.bakeLayer(ModelLayers.ZOMBIE)), 2.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(TitanZombie entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(TitanZombie entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }

    @Override
    protected float getShadowRadius(TitanZombie entity) {
        return 3.0F;
    }

    @Override
    protected boolean shouldShowName(TitanZombie entity) {
        return true;   // 泰坦始终显示名字，便于 M0 验证时确认是哪个实体
    }
}
