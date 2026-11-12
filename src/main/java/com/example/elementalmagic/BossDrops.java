package com.example.elementalmagic;

import com.example.elementalmagic.magic.Element;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = MagicMod.MODID)
public final class BossDrops {

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        LivingEntity boss = event.getEntity();
        Level level = boss.level();
        if (level.isClientSide) return;

        Element school = schoolFor(boss.getType());
        if (school == null) return;

        Entity killer = event.getSource().getEntity();
        Entity dropAt = killer instanceof Player ? killer : boss;
        RandomSource r = boss.getRandom();

        boolean dragon = boss.getType() == EntityType.ENDER_DRAGON;
        if (dragon || r.nextFloat() < 0.35f) {
            drop(level, dropAt, new ItemStack(ModRegistry.BOOKS.get(school).get()));
        }

        int count = 1 + r.nextInt(3);
        for (int i = 0; i < count; i++) {
            // Из двух случайных уровней берётся меньший, поэтому высокие уровни реже
            int tier = 1 + Math.min(r.nextInt(ScrollItem.TIERS), r.nextInt(ScrollItem.TIERS));
            ItemStack scroll = ScrollItem.randomOfTier(school, tier, r);
            if (!scroll.isEmpty()) drop(level, dropAt, scroll);
        }
    }

    /** Какая редкая школа падает с какого босса. */
    private static Element schoolFor(EntityType<?> type) {
        if (type == EntityType.ENDER_DRAGON) return Element.STARS;
        if (type == EntityType.WITHER) return Element.DARK_NATURE;
        if (type == EntityType.WARDEN) return Element.SHADOW;
        if (type == EntityType.ELDER_GUARDIAN) return Element.TIME;
        return null;
    }

    private static void drop(Level level, Entity at, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, at.getX(), at.getY() + 0.5, at.getZ(), stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    private BossDrops() {}
}
