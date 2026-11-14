package com.example.elementalmagic.magic;

import com.example.elementalmagic.Mana;
import com.example.elementalmagic.ModRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Действия зачарований: макс. мана и регенерация для брони, урон заклинаний для мечей. */
public final class ManaEnchantHandler {

    /** +20 к максимуму маны за каждый уровень на каждом предмете брони. */
    private static final int MAX_MANA_PER_LEVEL = 20;
    /** +1 маны в секунду за каждый уровень на каждом предмете брони. */
    private static final int REGEN_PER_LEVEL = 1;
    /** +20% урона заклинаний за каждый уровень закалки маной (I = +20%, IV = +80%). */
    private static final float TEMPERING_SPELL_BONUS_PER_LEVEL = 0.2f;

    // ----- Броня: восстановление маны раз в секунду -----
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount % 20 != 0) return; // раз в секунду

        int regenLevels = sumLevels(player, ModEnchantments.MANA_RECOVERY,
                player.level().registryAccess());
        if (regenLevels <= 0) return;

        int current = player.getData(ModRegistry.MANA);
        int max = maxMana(player);
        if (current < max) {
            player.setData(ModRegistry.MANA,
                    Math.min(current + regenLevels * REGEN_PER_LEVEL, max));
        }
    }

    // ----- Мечи: закалка маной усиливает магический урон -----
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // Усиливаем только магический урон, а не обычные удары оружием
        if (!event.getSource().is(DamageTypes.MAGIC)
                && !event.getSource().is(DamageTypes.INDIRECT_MAGIC)) return;

        if (event.getSource().getEntity() instanceof Player player) {
            ItemStack weapon = player.getMainHandItem();
            int lvl = ModEnchantments.levelOn(weapon, ModEnchantments.MANA_TEMPERING,
                    player.level().registryAccess());
            if (lvl > 0) {
                float bonus = 1.0f + lvl * TEMPERING_SPELL_BONUS_PER_LEVEL;
                event.setAmount(event.getAmount() * bonus);
            }
        }
    }

    /** Бонус к максимуму маны от «Увеличения маны» на всей надетой броне. */
    public static int bonusMaxMana(Player player) {
        return sumLevels(player, ModEnchantments.MANA_BOOST,
                player.level().registryAccess()) * MAX_MANA_PER_LEVEL;
    }

    /** Максимум маны с учётом зачарований.
     *  ВАЖНО: если ваш максимум зависит от амулетов — замените Mana.BASE_MAX
     *  на вызов вашего метода расчёта максимума, бонус прибавится сверху. */
    public static int maxMana(Player player) {
        return Mana.BASE_MAX + bonusMaxMana(player);
    }

    private static int sumLevels(Player player, ResourceKey<Enchantment> enchant,
                                 HolderLookup.Provider registries) {
        int total = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            total += ModEnchantments.levelOn(player.getItemBySlot(slot), enchant, registries);
        }
        return total;
    }

    private ManaEnchantHandler() {}
}
