package com.titanreborn.item;

import com.titanreborn.entity.TitanAction;
import com.titanreborn.entity.TitanZombie;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/**
 * 泰坦调试杖 —— 用于手动播放泰坦的动作，验证动画状态机。
 *
 * <p><b>用途</b>：右键依次循环 {@code 正常 → 待机 → 行走 → 攻击 → 正常}，
 * 作用对象是**最近的僵尸泰坦**（搜索半径 {@link #SEARCH_RADIUS} 格）。
 *
 * <p><b>为什么是"锁定"语义</b>：待机/行走是循环动画，需要保持住才能看清；
 * 攻击是单次动画。因此调试杖把动作**锁定**在当前项，直到再次循环回
 * {@link TitanAction#NORMAL} 才交回自动逻辑（移动则 walk、静止则 idle）。
 *
 * <p>这是**开发期工具**，正式发布前应移除或只在创造模式可用。
 */
public class DebugWandItem extends Item {

    /** 搜索泰坦的半径（格）。泰坦很大，给足距离。 */
    private static final double SEARCH_RADIUS = 48.0D;

    public DebugWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            // 客户端只回成功，实际逻辑在服务端（实体状态必须由服务端改）
            return InteractionResultHolder.success(stack);
        }

        TitanZombie titan = findNearestTitan(level, player);
        if (titan == null) {
            player.displayClientMessage(
                    Component.literal("§c附近 " + (int) SEARCH_RADIUS + " 格内没有僵尸泰坦"), true);
            return InteractionResultHolder.fail(stack);
        }

        TitanAction newAction = titan.cycleAction();
        player.displayClientMessage(
                Component.literal("§a僵尸泰坦 #" + titan.getId() + " 动作 -> §e" + newAction.displayName()), false);

        return InteractionResultHolder.success(stack);
    }

    /** 找离玩家最近的僵尸泰坦。 */
    private static TitanZombie findNearestTitan(Level level, Player player) {
        AABB area = player.getBoundingBox().inflate(SEARCH_RADIUS);
        List<TitanZombie> titans = level.getEntitiesOfClass(TitanZombie.class, area);
        return titans.stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7开发调试工具：控制最近的僵尸泰坦"));
        tooltip.add(Component.literal("§7右键：§f循环 正常→待机→原地走→§a向前走§f→攻击"));
        tooltip.add(Component.literal("§7选中§a向前走§7时它会真的往前移动"));
        tooltip.add(Component.literal("§7作用对象：最近的僵尸泰坦（48 格内）"));
    }
}
