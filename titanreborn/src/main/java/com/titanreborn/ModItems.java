package com.titanreborn;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册表。
 */
public final class ModItems {

    private ModItems() {}

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(TitanReborn.MODID);

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
