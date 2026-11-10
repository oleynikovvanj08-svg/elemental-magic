package com.example.elementalmagic.magic;

import java.util.ArrayList;
import java.util.List;

public final class Spells {
    public static final List<Spell> ALL = new ArrayList<>();

    static {
        LightningSpells.init();
        LightSpells.init();
        DarknessSpells.init();
        WaterSpells.init();
        EarthSpells.init();
        AirSpells.init();
        FireSpells.init();
    }

    static void reg(String id, School school, int mana, int cooldownTicks, Spell.CastAction action) {
        ALL.add(new Spell(id, school, mana, cooldownTicks, action));
    }

    public static List<Spell> byElement(Element element) {
        return ALL.stream().filter(s -> s.school().element == element).toList();
    }

    private Spells() {}
}