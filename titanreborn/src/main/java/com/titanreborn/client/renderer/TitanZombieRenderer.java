package com.titanreborn.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.titanreborn.client.model.TitanZombieModel;
import com.titanreborn.entity.TitanZombie;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * 僵尸泰坦渲染器（GeckoLib）。
 *
 * <h2>手持武器</h2>
 *
 * <p>泰坦本质就是一只僵尸，所以直接复用原版的装备槽：{@link TitanZombie} 构造时把
 * {@code Items.IRON_SWORD} 放进 {@code MAINHAND}，本渲染器用 GeckoLib 官方的
 * {@link BlockAndItemGeoLayer} 把它画在 {@code right_arm} 骨骼上。
 *
 * <p>这样剑是**真正的原版物品**——不需要在 geo 里建模、不需要占贴图集，
 * 而且换武器、附魔、掉落、被捡起这些能力全部白送。
 *
 * <h2>坐标约定（已实测）</h2>
 *
 * <p>{@code BlockAndItemGeoLayer.renderForBone} 内部会先调
 * {@code RenderUtil.translateAndRotateMatrixForBone}，所以进入
 * {@code renderStackForBone} 时 PoseStack 已经在**骨骼枢轴点**（肩部）并已应用骨骼旋转。
 *
 * <p>GeckoLib 的枢轴点会除以 16，因此 PoseStack 的单位是**格**而不是像素。
 * 手臂立方体从枢轴向下延伸 160 像素 = 10 格，所以手掌在骨骼局部
 * {@code (0, -10, 0)} 处 —— 必须用**负** Y。
 *
 * <h2>朝向</h2>
 *
 * <p>不使用手算旋转：通过 {@link HandItemRenderer} 以
 * {@code THIRD_PERSON_RIGHT_HAND} 上下文渲染，由原版应用它自己的手持变换。
 */
public class TitanZombieRenderer extends GeoEntityRenderer<TitanZombie> {

    /** 持剑骨骼名。 */
    private static final String HAND_BONE = "right_arm";

    /** 手掌相对肩部枢轴的偏移（格）。手臂长 160 像素 = 10 格，方向为骨骼局部 -Y。 */
    private static final double HAND_OFFSET_BLOCKS = 10.0D;

    /**
     * 剑相对手掌的微调（格），在**骨骼局部坐标系**里。手臂绕 X 轴前伸约 80 度，
     * 因此局部轴与世界轴的对应为：
     * <pre>
     *   局部 +X  ->  世界左右（僵尸的右手侧）
     *   局部 +Y  ->  世界前方
     *   局部 +Z  ->  世界上方
     * </pre>
     * 所以「往前」= 增大 FORWARD，「往下」= 减小 UP（取负值）。
     */
    private static final double SWORD_NUDGE_FORWARD = -1.5D;
    private static final double SWORD_NUDGE_UP = -3.5D;

    /** 原版 ItemInHandLayer 的手部偏移（格），见 renderArmWithItem。 */
    private static final float HAND_OFFSET_X = 1.0F / 16.0F;
    private static final float HAND_OFFSET_Y = 0.125F;
    private static final float HAND_OFFSET_Z = 0.625F;

    /** 泰坦相对普通生物放大 16 倍（原版物品模型是 1/16 格尺度）。 */
    private static final float TITAN_SCALE = 16.0F;

    public TitanZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new TitanZombieModel());
        // 32 格高的实体，阴影相应放大
        this.shadowRadius = 8.0F;

        this.addRenderLayer(new BlockAndItemGeoLayer<TitanZombie>(this) {
            @Override
            protected ItemStack getStackForBone(GeoBone bone, TitanZombie animatable) {
                if (!HAND_BONE.equals(bone.getName())) {
                    return null;
                }
                ItemStack stack = animatable.getItemBySlot(EquipmentSlot.MAINHAND);
                return stack.isEmpty() ? null : stack;
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
                                              TitanZombie animatable, MultiBufferSource bufferSource,
                                              float partialTick, int packedLight, int packedOverlay) {
                poseStack.pushPose();
                // ① 从肩部沿手臂走到手掌（骨骼局部 -Y）
                poseStack.translate(0.0D, -HAND_OFFSET_BLOCKS, 0.0D);
                // 手掌位置的微调（格）。绕 X 的旋转不改变 X 轴，故：
                //   局部 +X = 世界"右"，局部 +Z = 世界"下"
                poseStack.translate(0.0D, SWORD_NUDGE_FORWARD, SWORD_NUDGE_UP);
                // ② 建立**原版手部空间**，这是 THIRD_PERSON_RIGHT_HAND 显示变换的前提。
                //    见 ItemInHandLayer#renderArmWithItem：这两次旋转不可省，
                //    否则物品会按错误基准空间变换（实测表现为剑尖朝斜后方）。
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
                // ③ 放大到泰坦尺度（原版物品模型是 1/16 格）
                //    ④ 把手里的剑从"沿手臂方向"抬成"垂直于手臂"。
                //    原版 THIRD_PERSON_RIGHT_HAND 假设手臂垂下，剑沿手臂伸出才是正握；
                //    泰坦手臂前伸，需再俯仰 90°，举起来时剑刃才竖直向上。
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
                poseStack.scale(TITAN_SCALE, TITAN_SCALE, TITAN_SCALE);
                // ④ 原版手部偏移，按泰坦倍率放大
                //    注意：这里**不能**再乘 TITAN_SCALE。translate 在已缩放坐标系里
                //    执行，数值会被 scale 自动放大；再乘一次会变成 160 格开外。
                poseStack.translate(0.0F, 0.0F, 0.0F);
                // ⑤ 用原版第三人称右手姿态渲染
                HandItemRenderer.render(stack, poseStack, bufferSource, packedLight, packedOverlay);
                poseStack.popPose();
            }
        });
    }
}
