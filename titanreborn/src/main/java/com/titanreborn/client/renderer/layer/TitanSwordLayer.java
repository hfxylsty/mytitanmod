package com.titanreborn.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.titanreborn.client.model.TitanIronSwordModel;
import com.titanreborn.entity.TitanZombie;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * 把**独立的 3D 铁剑模型**渲染到泰坦右手上。
 *
 * <h2>2026-10-17 重写原因</h2>
 *
 * <p>旧实现继承 {@code BlockAndItemGeoLayer} 并渲染 {@code Items.IRON_SWORD} 物品栈。
 * 两个问题：
 * <ol>
 *   <li>物品渲染是**扁平贴图**（物品栏图标那种），不是立体剑；</li>
 *   <li>物品的手持变换在 16 倍缩放下完全失控，剑跑到了头顶。</li>
 * </ol>
 * 我们其实有现成的立体剑模型（{@code titan_iron_sword.geo.json}，12 个立方体），
 * 旧代码根本没用它。本类改为直接渲染那个模型。
 *
 * <h2>坐标</h2>
 *
 * <p>挂载骨骼 {@code right_arm}：pivot 在肩部 y=384。手臂前伸时（idle xRot≈-80°），
 * 骨骼局部 -Y 方向就是"沿手臂指向手掌"。
 *
 * <p>剑模型握把在原点、剑刃朝 +Y，长约 172 像素（10.75 格）。
 * 渲染时额外绕 X 轴转 {@link #BLADE_PITCH}，把剑刃从"顺着手臂向前"转成"竖直向上"。
 *
 * <p>⚠️ 手感仍需进游戏微调，改下列常量即可（单位：格 / 度）。
 */
public class TitanSwordLayer extends GeoRenderLayer<TitanZombie> {

    private final TitanIronSwordModel swordModel = new TitanIronSwordModel();

    /** 持剑骨骼（必须是模型里**有立方体**的骨骼，否则 GeckoLib 不会回调）。 */
    private static final String HAND_BONE = "right_arm";

    /** 手臂长度（格）：手臂立方体 8 格高，pivot 在肩，手掌在局部 -Y 方向 8 格处。 */
    private static final double ARM_LENGTH_BLOCKS = 8.0D;

    /** 握点微调（格），叠加在手臂长度之上。 */
    private static final double GRIP_X = 0.0D;
    private static final double GRIP_Y = 0.0D;
    private static final double GRIP_Z = 0.0D;

    /** 剑刃倾角（度）：把"顺臂向前"转成"竖直向上"，略前倾更自然。 */
    private static final float BLADE_PITCH = -95.0F;

    /** 剑的附加偏航（度），0 = 剑刃朝正前。 */
    private static final float BLADE_YAW = 0.0F;

    public TitanSwordLayer(GeoRenderer<TitanZombie> renderer) {
        super(renderer);
    }

    /**
     * 只处理右手骨骼。
     *
     * <p>{@link GeoRenderLayer#renderForBone} 由 GeckoLib 对**每个骨骼**回调一次，
     * 姿势矩阵此时已经变换到该骨骼的局部空间，直接画即可。
     */
    @Override
    public void renderForBone(PoseStack poseStack, TitanZombie animatable, GeoBone bone,
                              RenderType renderType, MultiBufferSource bufferSource,
                              VertexConsumer buffer, float partialTick,
                              int packedLight, int packedOverlay) {
        if (!HAND_BONE.equals(bone.getName())) {
            return;
        }

        poseStack.pushPose();
        // 沿手臂方向走到手掌
        poseStack.translate(GRIP_X, -ARM_LENGTH_BLOCKS + GRIP_Y, GRIP_Z);
        // 让剑刃竖起来（并可微调偏航）
        poseStack.mulPose(Axis.XP.rotationDegrees(BLADE_PITCH));
        if (BLADE_YAW != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(BLADE_YAW));
        }

        RenderType type = RenderType.entityCutoutNoCull(swordModel.getTextureResource(animatable));
        BakedGeoModel sword = swordModel.getBakedModel(swordModel.getModelResource(animatable));
        this.renderer.reRender(sword, poseStack, bufferSource, animatable,
                type, bufferSource.getBuffer(type),
                partialTick, packedLight, packedOverlay, 0xFFFFFFFF);
        poseStack.popPose();
    }
}
