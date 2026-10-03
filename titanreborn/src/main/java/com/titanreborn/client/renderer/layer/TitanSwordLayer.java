package com.titanreborn.client.renderer.layer;

import com.titanreborn.entity.TitanZombie;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * 手持巨型铁剑。
 *
 * <p>原版设定里泰坦「手持巨型铁剑」。**剑不烘进模型**——Minecraft 的惯例是武器
 * 作为「手持物品」单独渲染，好处是换武器不用重做模型、掉落时自然掉剑。
 *
 * <p>用 GeckoLib 现成的 {@link BlockAndItemGeoLayer}：它按骨骼名取物品栈并渲染。
 * 这里只在 {@code right_arm} 骨骼上返回铁剑，其余骨骼返回 null。
 *
 * <p><b>为什么需要放大</b>：手持物品默认按原版物品尺寸渲染（约 0.5 格），
 * 而泰坦高 32 格，剑需要放到约 8~10 格才有"巨剑"的比例感。
 *
 * <p>握点参考（来自模型交付说明）：右手父骨骼 {@code right_arm_lower}，
 * 握点相对其 pivot 约 {@code (0, -84, -40)}（像素）。
 */
public class TitanSwordLayer extends BlockAndItemGeoLayer<TitanZombie> {

    /** 剑的放大倍率：原版铁剑约 0.5 格，放到 8 格量级 → 约 16 倍。设计值，待调。 */
    private static final float SWORD_SCALE = 16.0F;

    /** 持剑骨骼（交付模型里的前臂骨骼）。 */
    private static final String HAND_BONE = "right_arm_lower";

    public TitanSwordLayer(GeoRenderer<TitanZombie> renderer) {
        super(renderer);
    }

    @Override
    protected ItemStack getStackForBone(GeoBone bone, TitanZombie animatable) {
        if (HAND_BONE.equals(bone.getName())) {
            return new ItemStack(Items.IRON_SWORD);
        }
        return null;
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
                                      TitanZombie animatable, MultiBufferSource bufferSource,
                                      float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.scale(SWORD_SCALE, SWORD_SCALE, SWORD_SCALE);
        // 握把对齐：模型握点相对骨骼 pivot 约 (0,-84,-40) 像素 = (0,-5.25,-2.5) 格
        poseStack.translate(0.0D, -5.25D, -2.5D);
        super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource,
                partialTick, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
