package com.example.elementalmagic;

import com.example.elementalmagic.boss.ModBosses;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(MagicMod.MODID)
public class MagicMod {
    public static final String MODID = "elementalmagic";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MagicMod(IEventBus modEventBus) {
        // Регистрация всех реестров мода: предметы, блоки, сущности, биом-модификаторы и т. д.
        ModRegistry.register(modEventBus);

        // Боссы: сущности и атрибуты живут в ModBosses
        ModBosses.register(modEventBus);

        LOGGER.info("Elemental Magic loaded");
    }
}
