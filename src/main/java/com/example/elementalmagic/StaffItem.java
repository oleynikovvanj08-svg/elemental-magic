package com.example.elementalmagic;

import com.example.elementalmagic.magic.Element;
import com.example.elementalmagic.magic.Spell;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class StaffItem extends Item {
    /** Бонус посоха к урону заклинаний своей стихии. */
    public static final float OWN_BONUS = 1.20f;
    /** Бонус посоха к урону заклинаний чужой стихии. */
    public static final float OTHER_BONUS = 1.10f;
    /** Штраф посоха к урону заклинаний противоположной стихии. */
    public static final float OPPOSITE_PENALTY = 0.90f;

    public final Element element;

    public StaffItem(Element element, Properties props) {
        super(props);
        this.element = element;
    }

    /** Множитель урона посоха выше, чем у книги. */
    public float multiplier(Element spellElement) {
        if (spellElement == element) return OWN_BONUS;
        if (spellElement == element.opposite()) return OPPOSITE_PENALTY;
        return OTHER_BONUS;
    }

    private static String seconds(int ticks) {
        return String.format("%.1f", ticks / 20.0);
    }

    public void cycle(Player player, ItemStack stack, int dir) {
        SpellbookItem.cycleItem(player, stack, element, dir);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        List<Spell> list = SpellbookItem.spellsOf(stack, element);
        if (list.isEmpty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatableWithFallback("msg.elementalmagic.empty_staff",
                        "В посохе нет заклинаний. Запиши их со свитков на магическом столе")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.pass(stack);
        }
        int idx = SpellbookItem.selectedIndexOf(stack, list.size());

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) cycle(player, stack, 1);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (level instanceof ServerLevel serverLevel) {
            Spell spell = list.get(idx);
            long tick = serverLevel.getGameTime();
            int cdLeft = SpellbookItem.cooldownLeft(stack, spell, tick);

            if (cdLeft > 0) {
                player.displayClientMessage(spell.name()
                        .append(Component.literal(" перезаряжается (" + seconds(cdLeft) + " с)")
                                .withStyle(ChatFormatting.RED)), true);
                return InteractionResultHolder.fail(stack);
            }

            int cost = SpellbookItem.manaCost(player, spell);
            int mana = Mana.get(player);
            boolean free = player.getAbilities().instabuild;

            if (!free && mana < cost) {
                player.displayClientMessage(Component.translatable("msg.elementalmagic.no_mana")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }

            float mult = multiplier(spell.element());
            if (spell.castWith(serverLevel, player, mult)) {
                if (!free) Mana.set(player, mana - cost);
                SpellbookItem.setCooldownOf(stack, spell, tick);
                level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                        SoundSource.PLAYERS, 0.8f, 1.1f);
            } else {
                player.displayClientMessage(Component.translatable("msg.elementalmagic.no_target")
                        .withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.elementalmagic.element", element.displayName()));
        List<Spell> list = SpellbookItem.spellsOf(stack, element);
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.learned_count",
                "Записано заклинаний: %s", list.size()).withStyle(ChatFormatting.GRAY));
        if (list.isEmpty()) return;

        Spell s = list.get(SpellbookItem.selectedIndexOf(stack, list.size()));
        tooltip.add(Component.translatable("tooltip.elementalmagic.selected", s.name()));
        tooltip.add(Component.literal("  ").append(s.school().displayName())
                .append(Component.literal(" • " + s.manaCost() + " маны • "
                        + s.cooldownSeconds() + " с").withStyle(ChatFormatting.DARK_GRAY)));
        if (s.baseDamage() > 0) {
            float mult = multiplier(s.element());
            tooltip.add(Component.literal("  Урон: " + String.format("%.1f", s.baseDamage() * mult)
                    + " (" + SpellbookItem.percent(mult) + ")").withStyle(ChatFormatting.DARK_GRAY));
        }

        if (ctx.level() != null) {
            int cdLeft = SpellbookItem.cooldownLeft(stack, s, ctx.level().getGameTime());
            if (cdLeft > 0) {
                tooltip.add(Component.literal("  Перезарядка: " + seconds(cdLeft) + " с")
                        .withStyle(ChatFormatting.RED));
            }
        }

        tooltip.add(Component.translatable("tooltip.elementalmagic.hint").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Z / X: сменить заклинание • V: открыть")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Бонус к урону: +20% своя школа, +10% чужая, -10% противоположная")
                .withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
