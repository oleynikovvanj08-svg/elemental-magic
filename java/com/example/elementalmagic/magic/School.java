package com.example.elementalmagic.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum School {
    // Молния
    STORM(Element.LIGHTNING, "storm"),
    MAGNETISM(Element.LIGHTNING, "magnetism"),
    // Свет
    HOLY(Element.LIGHT, "holy"),
    SOLAR(Element.LIGHT, "solar"),
    // Тьма
    SHADOW(Element.DARKNESS, "shadow"),
    NECROMANCY(Element.DARKNESS, "necromancy"),
    // Вода
    ICE(Element.WATER, "ice"),
    TIDE(Element.WATER, "tide"),
    // Земля
    IRON(Element.EARTH, "iron"),
    STONE(Element.EARTH, "stone"),
    // Воздух
    SOUND(Element.AIR, "sound"),
    WIND(Element.AIR, "wind"),
    // Огонь
    LAVA(Element.FIRE, "lava"),
    FLAME(Element.FIRE, "flame");

    public final Element element;
    public final String id;

    School(Element element, String id) {
        this.element = element;
        this.id = id;
    }

    public MutableComponent displayName() {
        return Component.translatable("school.elementalmagic." + id)
                .withStyle(s -> s.withColor(element.color));
    }
}