package com.example.elementalmagic;

import com.example.elementalmagic.worldgen.ModStructures;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(MagicMod.MODID)
public class MagicMod {
    public static final String MODID = "elementalmagic";

    public MagicMod(IEventBus modBus) {
        ModRegistry.register(modBus);
        ModStructures.register(modBus);
    }
}
