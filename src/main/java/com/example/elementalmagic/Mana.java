package com.example.elementalmagic;

import com.example.elementalmagic.client.AccessorySlots;
import com.example.elementalmagic.magic.Element;
import com.example.elementalmagic.magic.Spell;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class Mana {
    public static final int BASE_MAX = 100;
    public static final int REGEN_INTERVAL = 10;

    public static final float ARMOR_ALL = 0.05f;
    public static final float ARMOR_OWN = 0.05f;
    public static final float ACCESSORY_ALL = 0.10f;
    public static final float ACCESSORY_OWN = 0.20f;

    public static int get(Player p) {
        return Math.min(p.getData(ModRegistry.MANA), max(p));
    }

    public static void set(Player p, int value) {
        p.setData(ModRegistry.MANA, Mth.clamp(value, 0, max(p)));
    }

    public static int max(Player p) {
        int max = BASE_MAX;
        for (ItemStack s : p.getArmorSlots()) {
            if (s.getItem() instanceof ElementalArmorItem) max += 150;
        }
        for (ItemStack s : AccessorySlots.amulets(p)) {
            if (s.getItem() instanceof AccessoryItem) max += 50;
        }
        for (ItemStack s : AccessorySlots.rings(p)) {
            if (s.getItem() instanceof AccessoryItem) max += 15;
        }
        return max;
    }

    public static int regen(Player p) {
        boolean hasRing = false;
        for (ItemStack s : AccessorySlots.rings(p)) {
            if (s.getItem() instanceof AccessoryItem) {
                hasRing = true;
                break;
            }
        }
        return hasRing ? 2 : 1;
    }

    public static int costFor(Player p, Spell spell) {
        Element e = spell.school().element;
        double mult = 1.0;
        mult -= 0.10 * armorPieces(p, e);
        if (holdsStaff(p, e)) mult -= 0.20;
        if (hasAccessory(p, AccessoryItem.Kind.AMULET, e)) mult -= 0.15;
        mult = Math.max(0.3, mult);
        return (int) Math.ceil(spell.manaCost() * mult);
    }

    public static float damageMultiplier(Player p, Element spellElement) {
        float bonus = 0f;
        for (ItemStack s : p.getArmorSlots()) {
            if (s.getItem() instanceof ElementalArmorItem a) {
                bonus += a.element == spellElement ? ARMOR_OWN : ARMOR_ALL;
            }
        }
        for (ItemStack s : AccessorySlots.amulets(p)) {
            bonus += accessoryBonus(s, spellElement);
        }
        for (ItemStack s : AccessorySlots.rings(p)) {
            bonus += accessoryBonus(s, spellElement);
        }
        return 1f + bonus;
    }

    private static float accessoryBonus(ItemStack stack, Element spellElement) {
        if (!(stack.getItem() instanceof AccessoryItem acc)) return 0f;
        return acc.element == spellElement ? ACCESSORY_OWN : ACCESSORY_ALL;
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

    public static boolean hasAccessory(Player p, AccessoryItem.Kind kind, Element element) {
        Iterable<ItemStack> slots = kind == AccessoryItem.Kind.AMULET
                ? AccessorySlots.amulets(p)
                : AccessorySlots.rings(p);
        for (ItemStack s : slots) {
            if (s.getItem() instanceof AccessoryItem acc && acc.element == element) return true;
        }
        return false;
    }

    private Mana() {}
}
