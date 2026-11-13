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
    ICE("ice", 0xA5F2F3, "Лёд", false),
    GRAVITY("gravity", 0x6A5ACD, "Гравитация", false),
    POISON("poison", 0x4CAF50, "Яд", false),
    SUMMONING("summoning", 0xC8A878, "Призыв", false),

    // ===== Школы, которые падают только с боссов =====
    TIME("time", 0xD4AF37, "Время", true),
    STARS("stars", 0xB0C4FF, "Звёзды", true),
    DARK_NATURE("dark_nature", 0x556B2F, "Тёмная природа", true),
    SHADOW("shadow", 0x4B4B6E, "Тени", true);

    /** Бонус к урону заклинаний своей стихии. */
    public static final float OWN_BONUS = 1.10f;
    /** Бонус к урону заклинаний чужой стихии. */
    public static final float OTHER_BONUS = 1.05f;
    /** Штраф к урону заклинаний противоположной стихии. */
    public static final float OPPOSITE_PENALTY = 0.95f;

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

    /** Противоположная стихия или null, если её нет. */
    public Element opposite() {
        return switch (this) {
            case DARKNESS -> LIGHT;
            case LIGHT -> DARKNESS;
            case FIRE -> WATER;
            case WATER -> FIRE;
            case NATURE -> DARK_NATURE;
            case DARK_NATURE -> NATURE;
            case ICE -> FIRE;
            case GRAVITY -> AIR;
            case POISON -> NATURE;
            default -> null;
        };
    }

    /**
     * Множитель урона для заклинания стихии spellElement,
     * если оно записано в гримуар этой стихии.
     */
    public float bookMultiplier(Element spellElement) {
        if (spellElement == this) return OWN_BONUS;
        if (spellElement == opposite()) return OPPOSITE_PENALTY;
        return OTHER_BONUS;
    }

    /** Поиск стихии по id, например "fire". Возвращает null, если не найдено. */
    public static Element byId(String id) {
        for (Element e : values()) {
            if (e.id.equals(id)) return e;
        }
        return null;
    }
}
