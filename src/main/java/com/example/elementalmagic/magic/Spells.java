package com.example.elementalmagic.magic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Spells {
    public static final List<Spell> ALL = new ArrayList<>();
    private static final Map<String, Spell> BY_ID = new HashMap<>();
    /** Уровни, рассчитанные по порядку заклинания внутри школы. */
    private static final Map<String, Integer> AUTO_TIER = new HashMap<>();

    static {
        // Обычные школы
        LightningSpells.init();
        LightSpells.init();
        DarknessSpells.init();
        WaterSpells.init();
        EarthSpells.init();
        AirSpells.init();
        FireSpells.init();
        NatureSpells.init();
        AshSpells.init();
        ExplosionSpells.init();
        ElementalSpells.init();

        // Школы с боссов
        TimeSpells.init();
        StarsSpells.init();
        DarkNatureSpells.init();
        ShadowSpells.init();

        // Автоматические уровни: школа делится на 5 равных частей
        for (Element e : Element.values()) {
            List<Spell> list = byElement(e);
            int n = list.size();
            for (int i = 0; i < n; i++) {
                AUTO_TIER.put(list.get(i).id(), Math.min(5, i * 5 / n + 1));
            }
        }
    }

    /** Регистрация без уровня: уровень берётся из SpellInfo или рассчитывается автоматически. */
    static void reg(String id, School school, int mana, int cooldownTicks, Spell.CastAction action) {
        Spell spell = new Spell(id, school, mana, cooldownTicks, action);
        ALL.add(spell);
        BY_ID.put(id, spell);
    }

    /** Регистрация с явным уровнем от 1 до 5. */
    static void reg(String id, School school, int mana, int cooldownTicks, int tier, Spell.CastAction action) {
        SpellInfo.setTier(id, tier);
        reg(id, school, mana, cooldownTicks, action);
    }

    public static List<Spell> byElement(Element element) {
        return ALL.stream().filter(s -> s.school().element == element).toList();
    }

    /** Заклинание по id или null, если такого нет. */
    public static Spell byId(String id) {
        return BY_ID.get(id);
    }

    /** Уровень заклинания: сначала из SpellInfo, иначе автоматический. */
    public static int tierOf(String id) {
        int explicit = SpellInfo.explicitTier(id);
        if (explicit > 0) return explicit;
        return AUTO_TIER.getOrDefault(id, 1);
    }

    /** Порядковый номер заклинания в общем списке. Нужен для сортировки внутри книги. */
    public static int order(String id) {
        Spell s = BY_ID.get(id);
        return s == null ? Integer.MAX_VALUE : ALL.indexOf(s);
    }

    private Spells() {}
}
