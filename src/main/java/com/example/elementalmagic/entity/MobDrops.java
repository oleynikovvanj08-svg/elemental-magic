package com.example.elementalmagic.entity;

import com.example.elementalmagic.MagicMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = MagicMod.MODID)
public final class MobDrops {

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ElementalMob mob)) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        Level level = mob.level();
        if (level.isClientSide) return;

        Item material = ModEntities.material(mob.element, mob.kind);
        if (material == null) return;

        int count = 1 + level.random.nextInt(2);
        int looting = lootingLevel(level, player);
        if (looting > 0) count += level.random.nextInt(looting + 1);

        ItemEntity drop = new ItemEntity(level, mob.getX(), mob.getY(), mob.getZ(), new ItemStack(material, count));
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }

    private static int lootingLevel(Level level, Player player) {
        Holder<Enchantment> looting = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.LOOTING);
        return EnchantmentHelper.getItemEnchantmentLevel(looting, player.getMainHandItem());
    }

    private MobDrops() {}
}
