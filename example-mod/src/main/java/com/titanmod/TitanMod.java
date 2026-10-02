package com.titanmod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(TitanMod.MODID)
public class TitanMod {
    public static final String MODID = "titanmod";

    public TitanMod(IEventBus bus) {
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
    }
}
