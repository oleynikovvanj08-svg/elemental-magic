package com.example.elementalmagic;

import com.example.elementalmagic.boss.ModBosses;
import com.example.elementalmagic.worldgen.ModStructures;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(MagicMod.MODID)
public class MagicMod {
    public static final String MODID = "elementalmagic";

    public MagicMod(IEventBus modBus) {
        ModRegistry.register(modBus);
        NeoForge.EVENT_BUS.register(ManaEnchantHandler.class);
        ModStructures.register(modBus);
        ModBosses.register(modBus);
    }
}
