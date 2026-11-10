package com.example.elementalmagic;

import com.example.elementalmagic.magic.Element;
import com.example.elementalmagic.magic.Spell;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class Mana {
    public static final int BASE_MAX = 100;

    public static int get(Player p) {
        return Math.min(p.getData(ModRegistry.MANA), max(p));
    }

    public static void set(Player p, int value) {
        p.setData(ModRegistry.MANA, Mth.clamp(value, 0, max(p)));
    }

    /** Максимум маны с учётом брони и аксессуаров. */
    public static int max(Player p) {
        int max = BASE_MAX;
        for (ItemStack s : p.getArmorSlots()) {
            if (s.getItem() instanceof ElementalArmorItem) max += 10;
        }
        if (hasAccessory(p, AccessoryItem.Kind.AMULET, null)) max += 50;
        if (hasAccessory(p, AccessoryItem.Kind.RING, null)) max += 15;
        return max;
    }

    /** Сколько маны восстанавливается за полсекунды. */
    public static int regen(Player p) {
        return hasAccessory(p, AccessoryItem.Kind.RING, null) ? 2 : 1;
    }

    /** Цена заклинания с учётом скидок. */
    public static int costFor(Player p, Spell spell) {
        Element e = spell.school().element;
        double mult = 1.0;
        mult -= 0.10 * armorPieces(p, e);
        if (holdsStaff(p, e)) mult -= 0.20;
        if (hasAccessory(p, AccessoryItem.Kind.AMULET, e)) mult -= 0.15;
        mult = Math.max(0.3, mult);
        return (int) Math.ceil(spell.manaCost() * mult);
    }

    public static int armorPieces(Player p, Element e) {
        int count = 0;
        for (ItemStack s : p.getArmorSlots()) {
            if (s.getItem() instanceof ElementalArmorItem a && a.element == e) count++;
        }
        return count;
    }

    public static boolean holdsStaff(Player p, Element e) {
        return (p.getMainHandItem().getItem() instanceof StaffItem s1 && s1.element == e)
                || (p.getOffhandItem().getItem() instanceof StaffItem s2 && s2.element == e);
    }

    /** element = null значит «любой стихии». */
    public static boolean hasAccessory(Player p, AccessoryItem.Kind kind, Element element) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof AccessoryItem acc
                    && acc.kind == kind && (element == null || acc.element == element)) {
                return true;
            }
        }
        return false;
    }

    private Mana() {}
}