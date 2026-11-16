package com.example.elementalmagic;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Зачарования мода. Сами записи лежат в data/elementalmagic/enchantment/*.json. */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> MANA_BOOST =
            key("mana_boost");
    public static final ResourceKey<Enchantment> MANA_RECOVERY =
            key("mana_recovery");
    public static final ResourceKey<Enchantment> MANA_TEMPERING =
            key("mana_tempering");

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, name));
    }

    /** Получить Holder зачарования из реестра по ключу. */
    public static Holder<Enchantment> holder(HolderLookup.Provider registries,
                                             ResourceKey<Enchantment> key) {
        return registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /** Суммарный уровень зачарования на всех надетых частях брони. */
    public static int armorLevel(LivingEntity entity, ResourceKey<Enchantment> key) {
        HolderLookup.Provider registries = entity.registryAccess();
        Holder<Enchantment> holder = holder(registries, key);
        int total = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            total += level(stack, holder);
        }
        return total;
    }

    /** Уровень зачарования на конкретном предмете. */
    public static int level(ItemStack stack, Holder<Enchantment> enchantment) {
        if (stack.isEmpty()) return 0;
        ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        return enchants.getLevel(enchantment);
    }

    private ModEnchantments() {}
}
