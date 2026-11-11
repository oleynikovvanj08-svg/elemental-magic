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
    FLAME(Element.FIRE, "flame"),
    // Природа
    FLORA(Element.NATURE, "flora"),
    BEAST(Element.NATURE, "beast"),
    // Пепел
    DECAY(Element.ASH, "decay"),
    SMOKE(Element.ASH, "smoke"),
    // Взрывная магия
    DETONATION(Element.EXPLOSION, "detonation"),
    SHOCKWAVE(Element.EXPLOSION, "shockwave"),
    // Все стихии
    FROST(Element.ELEMENTAL, "frost"),
    FUSION(Element.ELEMENTAL, "fusion"),
    // Время
    ACCELERATION(Element.TIME, "acceleration"),
    STASIS(Element.TIME, "stasis"),
    // Звёзды
    CONSTELLATION(Element.STARS, "constellation"),
    COSMOS(Element.STARS, "cosmos"),
    // Тёмная природа
    ROT(Element.DARK_NATURE, "rot"),
    THORN(Element.DARK_NATURE, "thorn"),
    // Тени
    STEALTH(Element.SHADOW, "stealth"),
    BLADE(Element.SHADOW, "blade");

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
