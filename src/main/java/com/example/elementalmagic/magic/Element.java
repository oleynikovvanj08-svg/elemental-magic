package com.example.elementalmagic.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum Element {
    // ===== Обычные школы =====
    LIGHTNING("lightning", 0x7FDBFF, "Молния", false),
    LIGHT("light", 0xFFF3A0, "Свет", false),
    DARKNESS("darkness", 0x8A2BE2, "Тьма", false),
    WATER("water", 0x1E90FF, "Вода", false),
    EARTH("earth", 0x8B5A2B, "Земля", false),
    AIR("air", 0xC8F7F7, "Воздух", false),
    FIRE("fire", 0xFF4500, "Огонь", false),
    NATURE("nature", 0x3CB043, "Природа", false),
    ASH("ash", 0x9E9E9E, "Пепел", false),
    EXPLOSION("explosion", 0xFF8C00, "Взрывная магия", false),
    ELEMENTAL("elemental", 0x40E0D0, "Все стихии", false),

    // ===== Школы, которые падают только с боссов =====
    TIME("time", 0xD4AF37, "Время", true),
    STARS("stars", 0xB0C4FF, "Звёзды", true),
    DARK_NATURE("dark_nature", 0x556B2F, "Тёмная природа", true),
    SHADOW("shadow", 0x4B4B6E, "Тени", true);

    public final String id;
    public final int color;
    public final String ruName;
    public final boolean bossOnly;

    Element(String id, int color, String ruName, boolean bossOnly) {
        this.id = id;
        this.color = color;
        this.ruName = ruName;
        this.bossOnly = bossOnly;
    }

    public MutableComponent displayName() {
        return Component.translatableWithFallback("element.elementalmagic." + id, ruName)
                .withStyle(s -> s.withColor(color));
    }
}
