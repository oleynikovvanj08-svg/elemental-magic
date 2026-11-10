package com.example.elementalmagic;

import com.example.elementalmagic.magic.Spell;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MagicTableBlock extends Block {
    public MagicTableBlock(Properties props) {
        super(props);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack other = player.getItemInHand(otherHand);

        ItemStack scroll;
        ItemStack book;
        if (stack.getItem() instanceof ScrollItem && other.getItem() instanceof SpellbookItem) {
            scroll = stack;
            book = other;
        } else if (stack.getItem() instanceof SpellbookItem && other.getItem() instanceof ScrollItem) {
            scroll = other;
            book = stack;
        } else {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) write(level, pos, player, scroll, book);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            msg(player, "msg.elementalmagic.table_hint",
                    "Возьми свиток в одну руку, книгу той же школы в другую и нажми ПКМ по столу");
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void write(Level level, BlockPos pos, Player player, ItemStack scrollStack, ItemStack bookStack) {
        ScrollItem scroll = (ScrollItem) scrollStack.getItem();
        SpellbookItem book = (SpellbookItem) bookStack.getItem();

        if (scroll.element != book.element) {
            msg(player, "msg.elementalmagic.wrong_school",
                    "Этот свиток для книги школы: %s", scroll.element.displayName());
            return;
        }

        List<Spell> all = SpellbookItem.allSpells(book.element);
        int from = scroll.from(all.size());
        int to = scroll.to(all.size());
        if (from >= to) {
            msg(player, "msg.elementalmagic.no_spells", "В этой школе пока нет заклинаний такого уровня");
            return;
        }

        List<Integer> learned = new ArrayList<>(SpellbookItem.learned(bookStack));
        int target = -1;
        for (int i = from; i < to; i++) {
            if (!learned.contains(i)) {
                target = i;
                break;
            }
        }
        if (target < 0) {
            msg(player, "msg.elementalmagic.all_learned", "Все заклинания этого уровня уже записаны");
            return;
        }

        boolean free = player.getAbilities().instabuild;
        int cost = scroll.xpCost();
        if (!free && player.experienceLevel < cost) {
            msg(player, "msg.elementalmagic.no_xp", "Нужно уровней опыта: %s", cost);
            return;
        }

        learned.add(target);
        Collections.sort(learned);
        bookStack.set(ModRegistry.LEARNED_SPELLS.get(), List.copyOf(learned));
        if (!free) {
            scrollStack.shrink(1);
            player.giveExperienceLevels(-cost);
        }

        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.5);
        }
        player.displayClientMessage(Component.translatableWithFallback("msg.elementalmagic.learned",
                "Записано заклинание: %s", all.get(target).name())
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    private static void msg(Player player, String key, String fallback, Object... args) {
        player.displayClientMessage(Component.translatableWithFallback(key, fallback, args)
                .withStyle(ChatFormatting.GRAY), true);
    }
}
