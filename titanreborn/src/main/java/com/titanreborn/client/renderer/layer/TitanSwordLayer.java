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
 * <p><b>握点（2026-10-12 改）</b>：外观基线换成「原版僵尸 ×16」模型后，
 * {@code right_arm_lower} 的 pivot 就在手腕上（新模型手臂 y 192~384 像素，
 * pivot 落在 192 = 手腕），所以剑直接挂在骨骼原点上即可，
 * <b>不再需要</b>旧模型时代那个 {@code (0,-5.25,-2.5)} 格的补偿偏移——
 * 那个值是按旧模型的前臂几何算的，套到新模型上会把剑压到手腕下方 5 格。
 *
 * <p>⚠️ 握点的最终手感仍需**进游戏看一眼**（当前标为人工待验项）；
 * 若剑离手或插进手臂，调 {@link #GRIP_OFFSET_*} 三个分量即可，不用改模型。
 */
public class TitanSwordLayer extends BlockAndItemGeoLayer<TitanZombie> {

    /** 剑的放大倍率：原版铁剑约 0.5 格，放到 8 格量级 → 约 16 倍。设计值，待调。 */
    private static final float SWORD_SCALE = 16.0F;

    /** 持剑骨骼（交付模型里的前臂骨骼，新模型已确认保留该名字）。 */
    private static final String HAND_BONE = "right_arm_lower";

    /** 握点补偿（格）。新模型下挂在骨骼原点，留 0；需要微调时改这里。 */
    private static final double GRIP_OFFSET_X = 0.0D;
    private static final double GRIP_OFFSET_Y = 0.0D;
    private static final double GRIP_OFFSET_Z = 0.0D;

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
        poseStack.translate(GRIP_OFFSET_X, GRIP_OFFSET_Y, GRIP_OFFSET_Z);
        super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource,
                partialTick, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
