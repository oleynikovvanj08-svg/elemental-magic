package com.example.elementalmagic.magic;

import java.util.HashMap;
import java.util.Map;

/** Урон и уровни заклинаний. 1 единица урона = половина сердца. */
public final class SpellInfo {
    private static final Map<String, Float> DAMAGE = new HashMap<>();
    private static final Map<String, Integer> TIER = new HashMap<>();

    /** Множитель урона по уровню: I, II, III, IV, V. */
    private static final float[] TIER_DAMAGE = {1.00f, 1.15f, 1.30f, 1.50f, 1.75f};

    static {
        // ===== Природа: Флора =====
        dmg("vine_grasp", 3);
        dmg("thorn_lash", 5);
        dmg("root_prison", 4);
        dmg("natures_wrath", 10);

        // ===== Природа: Звери =====
        dmg("bears_fury", 12);
        dmg("eagle_dive", 6);

        // ===== Новые заклинания (урон за одно попадание; ExtraSpells берёт числа отсюда) =====
        // Молния
        dmg("chain_lightning", 8);      // первый прыжок, дальше -25% за каждый
        dmg("thunder_cloud", 9);        // за каждый из 3 ударов
        // Свет
        dmg("holy_bolt", 8);            // только враждебным мобам
        dmg("purification", 6);         // только нежити
        // Тьма
        dmg("blood_pact", 14);
        dmg("dark_touch", 9);           // ночью x1.6
        // Вода
        dmg("tidal_wave", 6);
        dmg("suffocation", 2);          // за каждый из 8 тиков удушья
        // Земля
        dmg("stone_spike", 7);
        dmg("stone_bullet", 9);
        dmg("earthquake", 3);           // за каждый из 5 толчков
        // Воздух
        dmg("gust", 2);
        dmg("repulsion", 3);
        dmg("whirlwind", 5);
        // Огонь
        dmg("sacrificial_flame", 8);    // растёт при низком здоровье до x3.5
        dmg("fire_breath", 3);          // за каждый из 8 импульсов
        // Природа
        dmg("living_roots", 3);
        // Взрывы
        dmg("rocket_jump", 5);
        dmg("grenade", 14);
        // Все стихии
        dmg("elemental_resonance", 12);
        // Звёзды
        dmg("constellation", 5);        // за каждый из 5 снарядов
        dmg("starfall", 9);             // за каждую из 5 звёзд
        dmg("supernova", 30);
        // Тёмная природа
        dmg("plague_cloud", 2);         // за каждый из 4 импульсов
        dmg("poison_roots", 2);         // за каждый из 6 импульсов
        // Тени
        dmg("shadow_step", 14);         // удар в спину

        // ===== Уровни заклинаний =====
        // Если заклинания здесь нет, уровень ставится автоматически по порядку в школе.
        // У новых заклинаний уровень задан прямо в reg(...) в ExtraSpells.
        tier("vine_grasp", 1);
        tier("thorn_lash", 2);
        tier("healing_bloom", 2);
        tier("root_prison", 3);
        tier("natures_wrath", 5);
        tier("wolfs_howl", 2);
        tier("bears_fury", 3);
        tier("eagle_dive", 2);
        tier("pack_instinct", 4);
        tier("primal_rage", 5);
    }

    private static void dmg(String id, float damage) {
        DAMAGE.put(id, damage);
    }

    private static void tier(String id, int tier) {
        setTier(id, tier);
    }

    static void setTier(String id, int tier) {
        TIER.put(id, Math.max(1, Math.min(5, tier)));
    }

    public static float damage(String id) {
        return DAMAGE.getOrDefault(id, 0f);
    }

    /** Уровень, заданный вручную, или 0, если не задан. */
    public static int explicitTier(String id) {
        return TIER.getOrDefault(id, 0);
    }

    public static float tierMultiplier(int tier) {
        int t = Math.max(1, Math.min(5, tier));
        return TIER_DAMAGE[t - 1];
    }

    private SpellInfo() {}
}
