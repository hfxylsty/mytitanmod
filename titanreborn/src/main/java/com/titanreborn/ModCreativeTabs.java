package com.titanreborn;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 创造模式物品栏分组。
 *
 * <p><b>为什么必须有这个类</b>：物品注册进 {@code Registries.ITEM} 只是"存在于注册表"，
 * 并<b>不会</b>自动出现在创造模式物品栏里。要让玩家能拿到，必须把它加入某个
 * {@link CreativeModeTab} 的展示列表——否则就会遇到"注册了但物品栏里找不到"。
 *
 * <p>本模组所有物品都放进自己的分组（而不是塞进原版分类），便于查找。
 */
public final class ModCreativeTabs {

    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TitanReborn.MODID);

    /** 本模组的主分组。显示名走语言文件键 {@code itemGroup.titanreborn.main}。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + TitanReborn.MODID + ".main"))
                    .icon(() -> new ItemStack(ModItems.FELLCORE_INGOT.get()))
                    .displayItems((parameters, output) -> {
                        // 按"材料 → 道具 → 武器 → 生成蛋"的顺序排列，便于查找。
                        // 新增物品必须在这里登记，否则玩家在物品栏里看不到它。
                        output.accept(ModItems.FELLCORE_SHARD.get());
                        output.accept(ModItems.FELLCORE_INGOT.get());
                        output.accept(ModItems.TITAN_BONE.get());

                        output.accept(ModItems.QUICKENING_ICHOR.get());
                        output.accept(ModItems.TITAN_ECHO.get());

                        output.accept(ModItems.MARROW_MAUL.get());

                        output.accept(ModItems.TITAN_ZOMBIE_SPAWN_EGG.get());
                    })
                    .build());
}
