package com.titanreborn.client;

import com.titanreborn.TitanReborn;
import com.titanreborn.entity.TitanZombie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动画调试浮层 —— 在屏幕上直接显示僵尸泰坦的**动画状态与播放进度**。
 *
 * <p><b>为什么需要它</b>：动画"没播"和"播了但幅度太小"在游戏里都是"站着不动"，
 * 肉眼分不清。这个浮层把内部状态直接打在屏幕上，给出决定性证据：
 * <ul>
 *   <li><b>动画</b>：控制器当前实际在播的动画名（不是我们代码里记的字段，
 *       而是从 GeckoLib 控制器读出来的真值）</li>
 *   <li><b>计数器</b>：只要这个数字在跳，就说明**动画确实在推进**</li>
 *   <li><b>控制器</b>：GeckoLib 控制器的运行状态（RUNNING / STOPPED 等）</li>
 * </ul>
 *
 * <p>计数器的做法：浮层每渲染帧检查"当前动画名"是否变了，变了就归零，
 * 否则加 1。所以它等价于"这个动画已经播了多久"。计数不动 = 动画没在推进。
 *
 * <p>只在附近 48 格内有泰坦时显示。开发期工具。
 */
@EventBusSubscriber(modid = TitanReborn.MODID, value = Dist.CLIENT)
public final class TitanAnimDebugOverlay {

    private static final double RANGE = 48.0D;
    private static final String CONTROLLER_NAME = "main";

    /** 每个实体上一次看到的动画名 + 已累计的帧数，用来判断动画是否在推进。 */
    private static final Map<Integer, String> LAST_ANIM = new HashMap<>();
    private static final Map<Integer, Integer> ANIM_FRAMES = new HashMap<>();

    private TitanAnimDebugOverlay() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) {
            return;
        }

        AABB area = mc.player.getBoundingBox().inflate(RANGE);
        List<TitanZombie> titans = mc.level.getEntitiesOfClass(TitanZombie.class, area);
        if (titans.isEmpty()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int y = 6;
        for (TitanZombie titan : titans) {
            List<Component> lines = buildLines(mc, titan);
            for (Component line : lines) {
                graphics.drawString(mc.font, line, 6, y, 0xFFFFFF, true);
                y += 11;
            }
            y += 2;
        }
    }

    private static List<Component> buildLines(Minecraft mc, TitanZombie titan) {
        int id = titan.getId();
        AnimationController<TitanZombie> controller = controllerOf(titan);

        String playing = "无控制器";
        String ctrlState = "n/a";
        if (controller != null) {
            var queued = controller.getCurrentAnimation();
            playing = (queued == null || queued.animation().name() == null)
                    ? "无动画"
                    : queued.animation().name().replace("animation.titan_zombie.", "");
            ctrlState = String.valueOf(controller.getAnimationState());
        }

        // 动画是否在推进：动画名变了就归零，否则累加
        String last = LAST_ANIM.get(id);
        int frames = (playing.equals(last)) ? ANIM_FRAMES.getOrDefault(id, 0) + 1 : 0;
        LAST_ANIM.put(id, playing);
        ANIM_FRAMES.put(id, frames);

        String advancing = frames > 0 ? "§a推进中(" + frames + ")" : "§c未推进";

        return List.of(
                Component.literal(String.format(
                        "§e泰坦#%d §7动作=§b%s §7状态=§f%s",
                        id, titan.getAction().displayName(), ctrlState)),
                Component.literal(String.format(
                        "§7控制器动画=§d%s §7%s §7血量=§c%.0f/%.0f",
                        playing, advancing, titan.getHealth(), titan.getMaxHealth())));
    }

    /** 类型化地取出主控制器（用实体自己的泛型，避免强转）。 */
    private static AnimationController<TitanZombie> controllerOf(TitanZombie titan) {
        try {
            AnimatableManager<TitanZombie> manager =
                    titan.getAnimatableInstanceCache().getManagerForId(titan.hashCode());
            return manager == null ? null : manager.getAnimationControllers().get(CONTROLLER_NAME);
        } catch (Throwable t) {
            // 调试浮层绝不能因为取不到状态而崩游戏
            return null;
        }
    }
}
