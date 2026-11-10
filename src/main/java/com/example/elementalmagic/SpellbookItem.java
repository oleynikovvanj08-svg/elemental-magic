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
import java.util.List;

public class SpellbookItem extends Item {
    public final Element element;

    public SpellbookItem(Element element, Properties props) {
        super(props);
        this.element = element;
    }

    /** Все заклинания школы. Если у школы их ещё нет, вернёт пустой список. */
    public static List<Spell> allSpells(Element element) {
        List<Spell> list = Spells.byElement(element);
        return list == null ? List.of() : list;
    }

    /** Номера записанных заклинаний. Новая книга знает только первое заклинание школы. */
    public static List<Integer> learned(ItemStack stack) {
        return stack.getOrDefault(ModRegistry.LEARNED_SPELLS.get(), List.of(0));
    }

    /** Записанные в эту книгу заклинания по порядку. */
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

        // Shift + ПКМ: следующее заклинание
        if (player.isShiftKeyDown()) {
            idx = (idx + 1) % list.size();
            stack.set(ModRegistry.SPELL_INDEX.get(), idx);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(
                        "msg.elementalmagic.selected", list.get(idx).name()), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // ПКМ: каст
        if (level instanceof ServerLevel serverLevel) {
            Spell spell = list.get(idx);
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
                player.getCooldowns().addCooldown(this, spell.cooldown());
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
        tooltip.add(Component.translatable("tooltip.elementalmagic.hint").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
