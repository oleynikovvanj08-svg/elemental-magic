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

import java.util.List;

public class SpellbookItem extends Item {
    public final Element element;

    public SpellbookItem(Element element, Properties props) {
        super(props);
        this.element = element;
    }

    private List<Spell> spells() {
        return Spells.byElement(element);
    }

    private int index(ItemStack stack, int size) {
        return Math.floorMod(stack.getOrDefault(ModRegistry.SPELL_INDEX.get(), 0), size);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        List<Spell> list = spells();
        if (list.isEmpty()) return InteractionResultHolder.pass(stack);
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
        List<Spell> list = spells();
        if (list.isEmpty()) return;
        Spell s = list.get(index(stack, list.size()));
        tooltip.add(Component.translatable("tooltip.elementalmagic.selected", s.name()));
        tooltip.add(Component.literal("  ").append(s.school().displayName())
                .append(Component.literal(" • " + s.manaCost() + " маны • "
                        + (s.cooldown() / 20.0) + " с").withStyle(ChatFormatting.DARK_GRAY)));
        tooltip.add(Component.translatable("tooltip.elementalmagic.count", list.size())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.elementalmagic.hint").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}