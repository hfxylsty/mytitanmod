package com.titanreborn.client.renderer.layer;

import com.titanreborn.client.model.TitanZombieArmorModel;
import com.titanreborn.entity.TitanZombie;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * 低血量「亮绿透明护甲」渲染层。
 *
 * <p>对应原版设定：「低血量时会穿上一层**包覆全身的亮绿色透明护甲**」。
 *
 * <p>实现方式：护甲是一个**独立模型**（{@code titan_zombie_armor.geo.json}），
 * 骨骼名与本体一致但立方体各外扩 3 像素，因此：
 * <ul>
 *   <li>动画自动同步——GeckoLib 按骨骼名驱动，无需额外代码；</li>
 *   <li>护甲层不含甲片（甲片属于破防机制，与护甲无关）。</li>
 * </ul>
 *
 * <p>触发条件：血量比例低于 {@link #ARMOR_HEALTH_THRESHOLD} 时才渲染。
 *
 * <p><b>透明度</b>：贴图 alpha≈140（半透明），必须用支持透明的渲染类型
 * （{@code RenderType.entityTranslucent}），否则会渲染成不透明或直接不可见。
 */
public class TitanArmorLayer extends GeoRenderLayer<TitanZombie> {

    /** 低血量阈值：血量比例低于此值时显示护甲。设计值，待平衡。 */
    public static final float ARMOR_HEALTH_THRESHOLD = 0.35F;

    private final TitanZombieArmorModel armorModel = new TitanZombieArmorModel();

    public TitanArmorLayer(GeoRenderer<TitanZombie> renderer) {
        super(renderer);
    }

    /** 供外部（HUD / 调试 / 伤害规则）查询当前是否处于护甲状态。 */
    public static boolean shouldWearArmor(TitanZombie titan) {
        float max = titan.getMaxHealth();
        return max > 0.0F && (titan.getHealth() / max) < ARMOR_HEALTH_THRESHOLD;
    }

    @Override
    public void render(PoseStack poseStack, TitanZombie animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {

        if (!shouldWearArmor(animatable)) {
            return;
        }

        // 用护甲模型自己的 baked model 与贴图重渲染一遍。
        // 骨骼名与本体一致，所以动画状态自动同步，不需要额外驱动。
        BakedGeoModel armor = armorModel.getBakedModel(armorModel.getModelResource(animatable));
        RenderType armorRenderType = RenderType.entityTranslucent(
                armorModel.getTextureResource(animatable));

        this.renderer.reRender(
                armor,
                poseStack, bufferSource, animatable,
                armorRenderType, bufferSource.getBuffer(armorRenderType),
                partialTick, packedLight, packedOverlay, 0xFFFFFFFF);
    }
}
