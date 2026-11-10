package com.example.elementalmagic.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum Element {
    LIGHTNING("lightning", 0x7FDBFF),
    LIGHT("light", 0xFFF3A0),
    DARKNESS("darkness", 0x8A2BE2),
    WATER("water", 0x1E90FF),
    EARTH("earth", 0x8B5A2B),
    AIR("air", 0xC8F7F7),
    FIRE("fire", 0xFF4500);

    public final String id;
    public final int color;

    Element(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public MutableComponent displayName() {
        return Component.translatable("element.elementalmagic." + id)
                .withStyle(s -> s.withColor(color));
    }
}