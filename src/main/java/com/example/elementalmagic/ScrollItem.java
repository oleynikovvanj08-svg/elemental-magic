package com.example.elementalmagic;

import com.example.elementalmagic.magic.Element;
import com.example.elementalmagic.magic.Spell;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;

public class ScrollItem extends Item {
    /** Количество уровней свитков. */
    public static final int TIERS = 5;
    private static final String[] TIER_NAMES = {"I", "II", "III", "IV", "V"};

    public final Spell spell;

    public ScrollItem(Spell spell, Properties props) {
        super(props);
        this.spell = spell;
    }

    /** Стихия заклинания на свитке. */
    public Element element() {
        return spell.element();
    }

    public static Rarity rarity(Spell spell) {
        if (spell.element().bossOnly || spell.tier() >= 5) return Rarity.EPIC;
        if (spell.tier() >= 3) return Rarity.RARE;
        if (spell.tier() == 2) return Rarity.UNCOMMON;
        return Rarity.COMMON;
    }

    public int xpCost() {
        return spell.tier() * 2;
    }

    /**
     * Случайный свиток стихии нужного уровня (1–5).
     * Если свитков такого уровня нет, берётся любой свиток этой стихии.
     * Возвращает пустой стак, если у стихии нет ни одного свитка.
     */
    public static ItemStack randomOfTier(Element element, int tier, RandomSource random) {
        List<DeferredItem<ScrollItem>> all = ModRegistry.SCROLLS.get(element);
        if (all == null || all.isEmpty()) return ItemStack.EMPTY;

        List<DeferredItem<ScrollItem>> matching = new ArrayList<>();
        for (DeferredItem<ScrollItem> item : all) {
            if (item.get().spell.tier() == tier) matching.add(item);
        }
        List<DeferredItem<ScrollItem>> pool = matching.isEmpty() ? all : matching;
        return new ItemStack(pool.get(random.nextInt(pool.size())).get());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatableWithFallback(getDescriptionId(),
                "Свиток %s уровня: %s", TIER_NAMES[spell.tier() - 1], spell.name());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.scroll_single",
                "Записывает заклинание: %s", spell.name()));
        tooltip.add(Component.literal("  ").append(spell.school().displayName())
                .append(Component.literal(" • Уровень " + spell.tier()).withStyle(ChatFormatting.GRAY)));
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.scroll_cost",
                "Запись: %s ур. опыта", xpCost()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.scroll_hint_any",
                "Любой гримуар или посох в другой руке, ПКМ по магическому столу").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.scroll_bonus",
                "Урон: +10% в гримуаре своей стихии, +5% в чужом").withStyle(ChatFormatting.DARK_GRAY));
        if (spell.element().opposite() != null) {
            tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.scroll_penalty",
                    "-5% в гримуаре стихии: %s", spell.element().opposite().displayName()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (spell.element().bossOnly) {
            tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.boss_only",
                    "Добывается только с боссов").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return spell.tier() >= 4;
    }
}
