package com.titanreborn.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用反射调用 {@code ItemRenderer.renderStatic}，以原版"第三人称右手"上下文渲染物品。
 *
 * <h2>为什么必须用反射</h2>
 *
 * <p>本环境的 javac **无法解析 {@code net.minecraft.world.item.ItemDisplayContext}
 * 的 class 文件**（NeoForge 的扩展枚举补丁导致 {@code RuntimeInvisibleParameterAnnotations}
 * 属性异常），只要在源码里写这个类型就编译失败。
 *
 * <p>但那个 class 文件在**运行时是完全正常的**——游戏自己就在用它渲染物品。
 * 所以改走反射：编译期不引用该类型，运行时用 {@code Class.forName} 拿到枚举常量，
 * 再用 {@code Method.invoke} 调用渲染方法。
 *
 * <h2>为什么必须用 THIRD_PERSON_RIGHT_HAND</h2>
 *
 * <p>{@code BlockAndItemGeoLayer} 的默认上下文是 {@code NONE}，它不做任何手持变换，
 * 物品以原始模型空间画出，需要调用方手算全部旋转与偏移；这些变换还会叠加在
 * 骨骼旋转之上，结果难以预测。
 *
 * <p>换成 {@code THIRD_PERSON_RIGHT_HAND} 后，原版会应用它自己的手持变换
 * （这正是玩家手持铁剑时的演示效果），调用方只需把坐标系移到手掌并缩放到泰坦尺寸。
 */
public final class HandItemRenderer {

    private static final Logger LOG = LoggerFactory.getLogger("titanreborn/hand-item");

    /** {@code ItemDisplayContext.THIRD_PERSON_RIGHT_HAND}，反射取得。 */
    private static Object thirdPersonRightHand;

    /** {@code ItemRenderer.renderStatic(ItemStack, ItemDisplayContext, int, int, PoseStack,
     *  MultiBufferSource, Level, int)}，反射取得。 */
    private static Method renderStatic;

    /** 反射初始化失败时只报一次，避免刷屏。 */
    private static boolean failed = false;

    private HandItemRenderer() {}

    /**
     * 以原版第三人称右手姿态渲染物品。
     *
     * @param poseStack 调用前应已定位到手掌位置，并缩放到目标尺寸
     * @return 是否成功渲染（反射初始化失败时返回 false）
     */
    public static boolean render(ItemStack stack, PoseStack poseStack, MultiBufferSource buffers,
                                 int packedLight, int packedOverlay) {
        ensureInit();
        if (failed) {
            return false;
        }
        Level level = Minecraft.getInstance().level;
        try {
            renderStatic.invoke(Minecraft.getInstance().getItemRenderer(), stack, thirdPersonRightHand,
                    packedLight, packedOverlay, poseStack, buffers, level, 0);
            return true;
        } catch (Throwable t) {
            failed = true;
            LOG.error("[铁剑] 反射渲染物品失败", t);
            return false;
        }
    }

    private static void ensureInit() {
        if (thirdPersonRightHand != null || failed) {
            return;
        }
        try {
            Class<?> ctxClass = Class.forName("net.minecraft.world.item.ItemDisplayContext");
            Object[] constants = ctxClass.getEnumConstants();
            for (Object c : constants) {
                if ("THIRD_PERSON_RIGHT_HAND".equals(((Enum<?>) c).name())) {
                    thirdPersonRightHand = c;
                    break;
                }
            }
            if (thirdPersonRightHand == null) {
                throw new IllegalStateException("找不到 THIRD_PERSON_RIGHT_HAND");
            }
            renderStatic = net.minecraft.client.renderer.entity.ItemRenderer.class.getMethod(
                    "renderStatic",
                    ItemStack.class, ctxClass, int.class, int.class,
                    PoseStack.class, MultiBufferSource.class, Level.class, int.class);
        } catch (Throwable t) {
            failed = true;
            LOG.error("[铁剑] 反射初始化失败", t);
        }
    }
}
