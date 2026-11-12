package com.example.elementalmagic;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = MagicMod.MODID)
public final class ManaEvents {
    /** Сколько маны падает с моба: от MIN_DROP до MAX_DROP. */
    private static final int MIN_DROP = 1;
    private static final int MAX_DROP = 2;

    /** Мана падает с любого моба, которого убил игрок. */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Mob)) return;
        if (!(event.getSource().getEntity() instanceof Player)) return;

        Level level = entity.level();
        if (level.isClientSide) return;

        int count = MIN_DROP + level.random.nextInt(MAX_DROP - MIN_DROP + 1);
        ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(),
                new ItemStack(ModRegistry.MANA_ITEM.get(), count));
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }

    /** Рецепты варки зелий восстановления маны. */
    @SubscribeEvent
    public static void onRegisterBrewing(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();
        builder.addMix(Potions.AWKWARD, ModRegistry.MANA_ITEM.get(), ModRegistry.MANA_POTION);
        builder.addMix(ModRegistry.MANA_POTION, Items.GLOWSTONE_DUST, ModRegistry.STRONG_MANA_POTION);
    }

    private ManaEvents() {}
}
