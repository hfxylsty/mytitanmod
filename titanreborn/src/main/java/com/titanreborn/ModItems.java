package com.titanreborn;

import com.titanreborn.item.DebugWandItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册。
 *
 * <p>命名遵循 {@code docs/10-content-roster.md} 的原创命名纪律：类别词可通用，
 * 个体专名必须自建，不得与原模组物品名相同。
 *
 * <p><b>贴图现状</b>：M0/M1 阶段使用纯色占位贴图（16×16），
 * 目的是让物品在物品栏里正常显示而不是黑紫方块。美术在 M2 阶段替换。
 */
public final class ModItems {

    private ModItems() {}

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(TitanReborn.MODID);

    // ==================== 材料线：陨核（fellcore）====================

    /** 陨核碎片：装备线的基础材料。 */
    public static final DeferredItem<Item> FELLCORE_SHARD =
            ITEMS.registerSimpleItem("fellcore_shard",
                    new Item.Properties().rarity(Rarity.UNCOMMON));

    /** 陨核锭：由碎片合成，用于装备。 */
    public static final DeferredItem<Item> FELLCORE_INGOT =
            ITEMS.registerSimpleItem("fellcore_ingot",
                    new Item.Properties().rarity(Rarity.RARE));

    /** 泰坦骨骼：泰坦掉落的材料。 */
    public static final DeferredItem<Item> TITAN_BONE =
            ITEMS.registerSimpleItem("titan_bone");

    // ==================== 功能道具 ====================

    /** 催生灵液：玩家可控的召唤道具（见 docs/03-gameplay-systems.md §6.1）。 */
    public static final DeferredItem<Item> QUICKENING_ICHOR =
            ITEMS.registerSimpleItem("quickening_ichor",
                    new Item.Properties().rarity(Rarity.RARE).stacksTo(16));

    /** 泰坦残响：附身召唤物用的精魂。 */
    public static final DeferredItem<Item> TITAN_ECHO =
            ITEMS.registerSimpleItem("titan_echo",
                    new Item.Properties().rarity(Rarity.RARE).stacksTo(16));

    // ==================== 武器 ====================

    /** 裂骨战锤：M1 的破防武器（耐久与数值为设计值，待平衡）。 */
    public static final DeferredItem<Item> MARROW_MAUL =
            ITEMS.registerSimpleItem("marrow_maul",
                    new Item.Properties().rarity(Rarity.EPIC).durability(2500));

    // ==================== 开发调试工具 ====================

    /**
     * 泰坦调试杖：右键循环播放泰坦动作，用于验证动画状态机。
     *
     * <p><b>开发期工具</b>，正式发布前应移除，或改为只在创造模式可用。
     * 见 {@link com.titanreborn.item.DebugWandItem}。
     */
    public static final DeferredItem<DebugWandItem> DEBUG_WAND =
            ITEMS.register("debug_wand",
                    () -> new DebugWandItem(new Item.Properties()
                            .rarity(Rarity.EPIC)
                            .stacksTo(1)));

    // ==================== 生成蛋 ====================

    /**
     * 僵尸泰坦生成蛋。
     *
     * <p>用 NeoForge 的 {@link DeferredSpawnEggItem}（接受 {@code Supplier<EntityType>}），
     * 而不是原版 {@code SpawnEggItem}——后者在构造时要实体类型已存在，
     * 与 DeferredRegister 的延迟注册时序冲突。
     */
    public static final DeferredItem<DeferredSpawnEggItem> TITAN_ZOMBIE_SPAWN_EGG =
            ITEMS.register("titan_zombie_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.TITAN_ZOMBIE,
                            0x2E4B2E, 0x8FA88F, new Item.Properties()));
}
