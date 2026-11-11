package com.example.elementalmagic.magic;

import java.util.ArrayList;
import java.util.List;

public final class Spells {
    public static final List<Spell> ALL = new ArrayList<>();

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
        ALL.add(new Spell(id, school, mana, cooldownTicks, action));
    }

    public static List<Spell> byElement(Element element) {
        return ALL.stream().filter(s -> s.school().element == element).toList();
    }

    private Spells() {}
}
