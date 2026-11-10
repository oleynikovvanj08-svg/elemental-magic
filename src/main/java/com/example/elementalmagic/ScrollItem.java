package com.example.elementalmagic;

import com.example.elementalmagic.magic.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ScrollItem extends Item {
    public static final int TIERS = 5;
    private static final String[] TIER_NAMES = {"ученика", "подмастерья", "адепта", "мастера", "архимага"};

    public final Element element;
    public final int tier;

    public ScrollItem(Element element, int tier, Properties props) {
        super(props);
        this.element = element;
        this.tier = tier;
    }

    public static Rarity rarity(Element e, int tier) {
        if (e.bossOnly || tier >= 5) return Rarity.EPIC;
        if (tier >= 3) return Rarity.RARE;
        if (tier == 2) return Rarity.UNCOMMON;
        return Rarity.COMMON;
    }

    /** Первый номер заклинания этого уровня (включительно). */
    public int from(int size) {
        return size * (tier - 1) / TIERS;
    }

    /** Граница заклинаний этого уровня (не включительно). */
    public int to(int size) {
        return size * tier / TIERS;
    }

    /** Сколько уровней опыта стоит запись. */
    public int xpCost() {
        return tier * 2;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatableWithFallback(getDescriptionId(),
                "Свиток " + TIER_NAMES[tier - 1] + ": %s", element.displayName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        int size = SpellbookItem.allSpells(element).size();
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.scroll_tier",
                "Уровень: %s из %s", tier, TIERS).withStyle(ChatFormatting.GRAY));
        if (to(size) > from(size)) {
            tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.scroll_range",
                    "Заклинания №%s–%s", from(size) + 1, to(size)).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.scroll_cost",
                "Запись: %s ур. опыта", xpCost()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.scroll_hint",
                "Книга этой школы в другой руке, ПКМ по магическому столу").withStyle(ChatFormatting.DARK_GRAY));
        if (element.bossOnly) {
            tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.boss_only",
                    "Добывается только с боссов").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return tier >= 4;
    }
}
