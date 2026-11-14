package com.example.elementalmagic.magic;

import com.example.elementalmagic.MagicMod;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Ключи и утилиты для зачарований мода. Сами зачарования — дата-паком (JSON). */
public final class ModEnchantments {

    public static final ResourceKey<Enchantment> MANA_BOOST = key("mana_boost");
    public static final ResourceKey<Enchantment> MANA_RECOVERY = key("mana_recovery");
    public static final ResourceKey<Enchantment> MANA_TEMPERING = key("mana_tempering");

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, name));
    }

    /** Уровень зачарования на предмете (0 — зачарования нет). */
    public static int levelOn(ItemStack stack, ResourceKey<Enchantment> enchant,
                              HolderLookup.Provider registries) {
        if (stack.isEmpty()) return 0;
        return registries.lookupOrThrow(Registries.ENCHANTMENT).get(enchant)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
                .orElse(0);
    }

    private ModEnchantments() {}
}
