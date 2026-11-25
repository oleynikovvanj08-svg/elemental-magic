package com.example.elementalmagic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Меч поглощения. Мощный, но медленный: урон 12, скорость атаки 1.2 (топор 1.0, обычный меч 1.6).
 * Пока меч в руке: магический урон по владельцу снижен на 50%, но меч постепенно высасывает ману.
 * Логика урона и истощения маны в AbsorptionSwordEvents.
 */
public class AbsorptionSwordItem extends SwordItem {
    /** Во сколько раз уменьшается магический урон по владельцу (0.5 означает на 50%). */
    public static final float MAGIC_DAMAGE_FACTOR = 0.5f;
    /** Как часто меч забирает ману, в тиках (20 тиков это 1 секунда). */
    public static final int DRAIN_INTERVAL = 20;
    /** Сколько маны забирает за один раз. */
    public static final int DRAIN_AMOUNT = 3;

    public AbsorptionSwordItem(Properties props) {
        super(Tiers.NETHERITE, props);
    }

    /**
     * Урон 12: 1 (игрок) + 4 (незерит) + 7 (здесь). Скорость 4.0 - 2.8 = 1.2.
     * Чтобы изменить урон, поменяй число 7, чтобы изменить скорость, поменяй -2.8f.
     */
    public static Item.Properties properties() {
        return new Item.Properties()
                .rarity(Rarity.EPIC)
                .fireResistant()
                .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 7, -2.8f));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        int cutPercent = Math.round((1f - MAGIC_DAMAGE_FACTOR) * 100);
        tooltip.add(Component.literal("Поглощение: магический урон по тебе снижен на " + cutPercent + "%")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Но меч высасывает ману: "
                + String.format("%.1f", DRAIN_AMOUNT * 20f / DRAIN_INTERVAL) + " в секунду, пока он в руке")
                .withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
