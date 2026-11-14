package com.example.elementalmagic;

import com.example.elementalmagic.boss.ModBosses;
import com.example.elementalmagic.worldgen.ModStructures;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(MagicMod.MODID)
public class MagicMod {
    public static final String MODID = "elementalmagic";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MagicMod(IEventBus modBus) {
        ModRegistry.register(modBus);
        ModStructures.register(modBus);
        ModBosses.register(modBus);
        LOGGER.info("Elemental Magic loaded");
    }
}
