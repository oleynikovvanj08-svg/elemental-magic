package com.example.elementalmagic;

import com.example.elementalmagic.magic.ManaEnchantHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(MagicMod.MODID)
public class MagicMod {
    public static final String MODID = "elementalmagic";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MagicMod(IEventBus modEventBus) {
        // Регистрация всех реестров мода: предметы, блоки, сущности, биом-модификаторы и т. д.
        ModRegistry.register(modEventBus);

        // Обработчики зачарований: регенерация/макс. мана брони и усиление заклинаний мечей
        NeoForge.EVENT_BUS.register(ManaEnchantHandler.class);

        LOGGER.info("Elemental Magic loaded");
    }
}
