package com.titanreborn.client.renderer.layer;

import com.titanreborn.entity.TitanZombie;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
 * <h2>2026-10-14 修复</h2>
 *
 * <p><b>问题 1：剑完全不显示。</b>骨骼名绑的是 {@code right_arm_lower}，
 * 但外观基线换成「原版僵尸 ×16」简化模型后，**那个骨骼只剩一个 pivot、一个立方体都没有**
 * （见 geo：{@code {"name":"right_arm_lower","pivot":[-96,192,0],"parent":"right_arm"}} 无 cubes）。
 * GeckoLib 只对有几何体的骨骼调用本层，所以剑永远不渲染。
 * 现在改绑确实有立方体的 {@code right_arm}。
 *
 * <p><b>问题 2：剑是「平贴图」。</b>用户描述「不是被拿着，而是默认的贴图状态，
 * 和物品栏里的贴图一样」。这里用 {@link Axis} 旋转把剑摆成握持姿态。
 *
 * <p><b>关于 {@code ItemDisplayContext}</b>：本来打算覆写
 * {@code getTransformTypeForStack} 换成手持上下文，但本项目的构建环境里
 * <b>javac 读不了 {@code net.minecraft.world.item.ItemDisplayContext} 的 class 文件</b>
 * （单独引用它 javac 就报「类文件无效」；`Item` 等同包类型正常）。
 * 这是 NeoForge 的 {@code IExtensibleEnum} 字节码增强与该 class 文件属性的兼容问题，
 * 已用独立小文件复现确认与业务代码无关。
 * 因此**改为用旋转矩阵自行控制姿态**，既绕开该类型，也省掉一次对外部上下文的依赖。
 *
 * <h2>坐标注意</h2>
 * GeckoLib 的骨骼局部空间单位是**模型像素**（1 格 = 16 像素）。
 * {@code right_arm} 的 pivot 在 y=384（肩），立方体下沿在 y=192（手），
 * 所以从 pivot 往下约 12 格才是握点。
 *
 * <p>⚠️ 握点与角度仍需进游戏微调：改 {@link #GRIP_X/Y/Z} 与
 * {@link #PITCH}/{@link #YAW}/{@link #ROLL} 即可，不必动模型。
 */
public class TitanSwordLayer extends BlockAndItemGeoLayer<TitanZombie> {

    /** 剑的放大倍率：原版铁剑约 0.5 格，放到 8 格量级 → 约 16 倍。设计值，待调。 */
    private static final float SWORD_SCALE = 16.0F;

    /**
     * 持剑骨骼。
     *
     * <p>必须选**有立方体**的骨骼，否则 GeckoLib 不会为它调用本层。
     * 当前简化模型里 {@code right_arm_lower} 是空占位骨骼，不能用。
     */
    private static final String HAND_BONE = "right_arm";

    /** 握点偏移（格）：从 {@code right_arm} 的 pivot（肩 y=384 像素）往下挪到手掌。 */
    private static final double GRIP_X = 0.0D;
    private static final double GRIP_Y = -12.0D;
    private static final double GRIP_Z = 0.0D;

    /** 剑的姿态（角度）。默认竖直下指，略微前倾更像"提着剑"。 */
    private static final float PITCH = -20.0F;
    private static final float YAW = 0.0F;
    private static final float ROLL = 0.0F;

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
        // 顺序很重要：先按格平移到位，再缩放，最后旋转姿态
        poseStack.translate(GRIP_X, GRIP_Y, GRIP_Z);
        poseStack.scale(SWORD_SCALE, SWORD_SCALE, SWORD_SCALE);
        if (PITCH != 0.0F) poseStack.mulPose(Axis.XP.rotationDegrees(PITCH));
        if (YAW != 0.0F) poseStack.mulPose(Axis.YP.rotationDegrees(YAW));
        if (ROLL != 0.0F) poseStack.mulPose(Axis.ZP.rotationDegrees(ROLL));
        super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource,
                partialTick, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
