package com.example.elementalmagic.magic;

import java.util.HashMap;
import java.util.Map;

/** Базовый урон заклинаний для окна книги. 1 единица = половина сердца. */
public final class SpellInfo {
    private static final Map<String, Float> DAMAGE = new HashMap<>();

    static {
        // ===== Природа: Флора =====
        dmg("vine_grasp", 3);
        dmg("thorn_lash", 5);
        dmg("root_prison", 4);
        dmg("natures_wrath", 10);

        // ===== Природа: Звери =====
        dmg("bears_fury", 12);
        dmg("eagle_dive", 6);
    }

    private static void dmg(String id, float damage) {
        DAMAGE.put(id, damage);
    }

    public static float damage(String id) {
        return DAMAGE.getOrDefault(id, 0f);
    }

    private SpellInfo() {}
}
