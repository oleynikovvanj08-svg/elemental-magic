package com.example.elementalmagic.magic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Spells {
    public static final List<Spell> ALL = new ArrayList<>();
    private static final Map<String, Spell> BY_ID = new HashMap<>();

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
    }

    static void reg(String id, School school, int mana, int cooldownTicks, Spell.CastAction action) {
        Spell spell = new Spell(id, school, mana, cooldownTicks, action);
        ALL.add(spell);
        BY_ID.put(id, spell);
    }

    public static List<Spell> byElement(Element element) {
        return ALL.stream().filter(s -> s.school().element == element).toList();
    }

    /** Заклинание по id или null, если такого нет. */
    public static Spell byId(String id) {
        return BY_ID.get(id);
    }

    /** Порядковый номер заклинания в общем списке. Нужен для сортировки внутри книги. */
    public static int order(String id) {
        Spell s = BY_ID.get(id);
        return s == null ? Integer.MAX_VALUE : ALL.indexOf(s);
    }

    private Spells() {}
}
