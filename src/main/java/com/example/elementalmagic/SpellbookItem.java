package com.example.elementalmagic;

import com.example.elementalmagic.magic.Element;
import com.example.elementalmagic.magic.Spell;
import com.example.elementalmagic.magic.Spells;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellbookItem extends Item {
    public final Element element;

    public SpellbookItem(Element element, Properties props) {
        super(props);
        this.element = element;
    }

    public static List<Spell> allSpells(Element element) {
        List<Spell> list = Spells.byElement(element);
        return list == null ? List.of() : list;
    }

    public static List<Integer> learned(ItemStack stack) {
        return stack.getOrDefault(ModRegistry.LEARNED_SPELLS.get(), List.of(0));
    }

    private List<Spell> spells(ItemStack stack) {
        List<Spell> all = allSpells(element);
        List<Spell> out = new ArrayList<>();
        for (int i : learned(stack)) {
            if (i >= 0 && i < all.size()) out.add(all.get(i));
        }
        return out;
    }

    private int index(ItemStack stack, int size) {
        return Math.floorMod(stack.getOrDefault(ModRegistry.SPELL_INDEX.get(), 0), size);
    }

    /** Возвращает оставшийся кулдаун заклинания в тиках (0 если готово). */
    private int cooldownLeft(ItemStack stack, Spell spell, long currentTick) {
        Map<String, Long> cds = stack.getOrDefault(ModRegistry.SPELL_COOLDOWNS.get(), Map.of());
        long ready = cds.getOrDefault(spell.id(), 0L);
        return (int) Math.max(0, ready - currentTick);
    }

    /** Ставит кулдаун для заклинания. */
    private void setCooldown(ItemStack stack, Spell spell, long currentTick) {
        Map<String, Long> cds = new HashMap<>(stack.getOrDefault(ModRegistry.SPELL_COOLDOWNS.get(), Map.of()));
        cds.put(spell.id(), currentTick + spell.cooldown());
        stack.set(ModRegistry.SPELL_COOLDOWNS.get(), cds);
    }

    public void cycle(Player player, ItemStack stack, int dir) {
        List<Spell> list = spells(stack);
        if (list.isEmpty()) {
            player.displayClientMessage(Component.translatableWithFallback("msg.elementalmagic.empty_book",
                    "В книге нет заклинаний. Запиши их со свитков на магическом столе")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (list.size() == 1) {
            player.displayClientMessage(Component.translatableWithFallback("msg.elementalmagic.only_one",
                    "Записано только одно заклинание. Запиши другие со свитков на магическом столе")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int idx = Math.floorMod(index(stack, list.size()) + dir, list.size());
        stack.set(ModRegistry.SPELL_INDEX.get(), idx);
        Spell spell = list.get(idx);
        long tick = player.level().getGameTime();
        int cdLeft = cooldownLeft(stack, spell, tick);
        
        Component msg = Component.translatable("msg.elementalmagic.selected", spell.name())
                .append(Component.literal(" (" + (idx + 1) + "/" + list.size() + ")")
                        .withStyle(ChatFormatting.DARK_GRAY));
        if (cdLeft > 0) {
            msg = msg.append(Component.literal(" [" + String.format("%.1f", cdLeft / 20.0) + " с]")
                    .withStyle(ChatFormatting.RED));
        }
        player.displayClientMessage(msg, true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        List<Spell> list = spells(stack);
        if (list.isEmpty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatableWithFallback("msg.elementalmagic.empty_book",
                        "В книге нет заклинаний. Запиши их со свитков на магическом столе")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.pass(stack);
        }
        int idx = index(stack, list.size());

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) cycle(player, stack, 1);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (level instanceof ServerLevel serverLevel) {
            Spell spell = list.get(idx);
            long tick = serverLevel.getGameTime();
            int cdLeft = cooldownLeft(stack, spell, tick);

            if (cdLeft > 0) {
                player.displayClientMessage(Component.translatable("msg.elementalmagic.on_cooldown",
                        spell.name(), String.format("%.1f", cdLeft / 20.0))
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }

            int cost = Mana.costFor(player, spell);
            int mana = Mana.get(player);
            boolean free = player.getAbilities().instabuild;

            if (!free && mana < cost) {
                player.displayClientMessage(Component.translatable("msg.elementalmagic.no_mana")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            if (spell.action().cast(serverLevel, player)) {
                if (!free) Mana.set(player, mana - cost);
                setCooldown(stack, spell, tick);
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
        List<Spell> list = spells(stack);
        int total = allSpells(element).size();
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.learned",
                "Записано заклинаний: %s из %s", list.size(), total).withStyle(ChatFormatting.GRAY));
        if (list.isEmpty()) return;
        Spell s = list.get(index(stack, list.size()));
        tooltip.add(Component.translatable("tooltip.elementalmagic.selected", s.name()));
        tooltip.add(Component.literal("  ").append(s.school().displayName())
                .append(Component.literal(" • " + s.manaCost() + " маны • "
                        + (s.cooldown() / 20.0) + " с").withStyle(ChatFormatting.DARK_GRAY)));

        if (ctx.level() != null) {
            int cdLeft = cooldownLeft(stack, s, ctx.level().getGameTime());
            if (cdLeft > 0) {
                tooltip.add(Component.literal("  Перезарядка: " + String.format("%.1f", cdLeft / 20.0) + " с")
                        .withStyle(ChatFormatting.RED));
            }
        }

        tooltip.add(Component.translatable("tooltip.elementalmagic.hint").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Z / X: сменить заклинание").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
