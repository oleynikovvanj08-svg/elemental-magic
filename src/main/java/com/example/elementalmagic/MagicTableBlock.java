package com.example.elementalmagic;

import com.example.elementalmagic.magic.Spell;
import com.example.elementalmagic.magic.Spells;
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
import java.util.Comparator;
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
        ItemStack carrier;
        Element element;

        if (stack.getItem() instanceof ScrollItem sc && 
            (other.getItem() instanceof SpellbookItem book || other.getItem() instanceof StaffItem staff)) {
            scroll = stack;
            carrier = other;
            element = other.getItem() instanceof SpellbookItem b ? b.element : ((StaffItem) other.getItem()).element;
        } else if (other.getItem() instanceof ScrollItem sc &&
                   (stack.getItem() instanceof SpellbookItem book || stack.getItem() instanceof StaffItem staff)) {
            scroll = other;
            carrier = stack;
            element = stack.getItem() instanceof SpellbookItem b ? b.element : ((StaffItem) stack.getItem()).element;
        } else {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) write(level, pos, player, scroll, book);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            msg(player, "msg.elementalmagic.table_hint_any",
                    "Возьми свиток в одну руку, любой гримуар в другую и нажми ПКМ по столу");
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void write(Level level, BlockPos pos, Player player, ItemStack scrollStack, ItemStack bookStack) {
        ScrollItem scroll = (ScrollItem) scrollStack.getItem();
        SpellbookItem book = (SpellbookItem) bookStack.getItem();

        List<Spell> pool = SpellbookItem.allSpells(scroll.element);
        int from = scroll.from(pool.size());
        int to = scroll.to(pool.size());
        if (from >= to) {
            msg(player, "msg.elementalmagic.no_spells", "В этой школе пока нет заклинаний такого уровня");
            return;
        }

        List<String> learned = new ArrayList<>(SpellbookItem.learnedIds(bookStack, book.element));
        Spell target = null;
        for (int i = from; i < to; i++) {
            if (!learned.contains(pool.get(i).id())) {
                target = pool.get(i);
                break;
            }
        }
        if (target == null) {
            msg(player, "msg.elementalmagic.all_learned", "Все заклинания этого уровня уже записаны");
            return;
        }

        boolean free = player.getAbilities().instabuild;
        int cost = scroll.xpCost();
        if (!free && player.experienceLevel < cost) {
            msg(player, "msg.elementalmagic.no_xp", "Нужно уровней опыта: %s", cost);
            return;
        }

        // Запоминаем выбранное заклинание, чтобы выбор не сбился после записи
        List<Spell> before = book.spells(bookStack);
        String selectedId = before.isEmpty() ? null : before.get(book.selectedIndex(bookStack, before.size())).id();

        learned.add(target.id());
        learned.sort(Comparator.comparingInt(Spells::order));
        SpellbookItem.setLearnedIds(bookStack, learned);
        if (selectedId != null) {
            bookStack.set(ModRegistry.SPELL_INDEX.get(), Math.max(0, learned.indexOf(selectedId)));
        }

        if (!free) {
            scrollStack.shrink(1);
            player.giveExperienceLevels(-cost);
        }

        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.5);
        }

        float mult = book.element.bookMultiplier(target.element());
        player.displayClientMessage(Component.translatableWithFallback("msg.elementalmagic.learned",
                "Записано заклинание: %s", target.name())
                .withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(" (урон " + SpellbookItem.percent(mult) + ")")
                        .withStyle(mult >= 1f ? ChatFormatting.GREEN : ChatFormatting.RED)), true);
    }

    private static void msg(Player player, String key, String fallback, Object... args) {
        player.displayClientMessage(Component.translatableWithFallback(key, fallback, args)
                .withStyle(ChatFormatting.GRAY), true);
    }
}
