package com.example.elementalmagic;

import com.example.elementalmagic.magic.Element;
import com.example.elementalmagic.magic.Spell;
import com.example.elementalmagic.magic.Spells;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

    /** Все заклинания школы. Если их ещё нет, пустой список. */
    public static List<Spell> allSpells(Element element) {
        List<Spell> list = Spells.byElement(element);
        return list == null ? List.of() : list;
    }

    /** Старый формат: номера заклинаний своей школы. Новая книга знает только первое. */
    public static List<Integer> learned(ItemStack stack) {
        return stack.getOrDefault(ModRegistry.LEARNED_SPELLS.get(), List.of(0));
    }

    /** id записанных заклинаний. Старые книги переводятся из номеров автоматически. */
    public static List<String> learnedIds(ItemStack stack, Element bookElement) {
        List<String> ids = stack.get(ModRegistry.LEARNED_IDS.get());
        if (ids != null) return ids;
        List<Spell> own = allSpells(bookElement);
        List<String> out = new ArrayList<>();
        for (int i : learned(stack)) {
            if (i >= 0 && i < own.size()) out.add(own.get(i).id());
        }
        return out;
    }

    public static void setLearnedIds(ItemStack stack, List<String> ids) {
        stack.set(ModRegistry.LEARNED_IDS.get(), List.copyOf(ids));
    }

    /** Записанные заклинания в порядке, в котором они лежат в книге. */
    public List<Spell> spells(ItemStack stack) {
        List<Spell> out = new ArrayList<>();
        for (String id : learnedIds(stack, element)) {
            Spell s = Spells.byId(id);
            if (s != null) out.add(s);
        }
        return out;
    }

    public int selectedIndex(ItemStack stack, int size) {
        if (size <= 0) return 0;
        return Math.floorMod(stack.getOrDefault(ModRegistry.SPELL_INDEX.get(), 0), size);
    }

    /** Оставшийся кулдаун заклинания в тиках (0, если готово). */
    public static int cooldownLeft(ItemStack stack, Spell spell, long currentTick) {
        Map<String, Long> cds = stack.getOrDefault(ModRegistry.SPELL_COOLDOWNS.get(), Map.of());
        long ready = cds.getOrDefault(spell.id(), 0L);
        return (int) Math.max(0, ready - currentTick);
    }

    private static void setCooldown(ItemStack stack, Spell spell, long currentTick) {
        Map<String, Long> cds = new HashMap<>(stack.getOrDefault(ModRegistry.SPELL_COOLDOWNS.get(), Map.of()));
        cds.put(spell.id(), currentTick + spell.cooldown());
        stack.set(ModRegistry.SPELL_COOLDOWNS.get(), cds);
    }

    /** Стоимость маны с учётом бонусов игрока. */
    public static int manaCost(Player player, Spell spell) {
        return Mana.costFor(player, spell);
    }

    /** Множитель в виде текста: 1.10 → "+10%", 0.95 → "-5%". */
    public static String percent(float multiplier) {
        int pct = Math.round((multiplier - 1f) * 100);
        return (pct >= 0 ? "+" : "") + pct + "%";
    }

    private static String seconds(int ticks) {
        return String.format("%.1f", ticks / 20.0);
    }

    /** Переключает заклинание: dir = 1 следующее, dir = -1 предыдущее. Вызывать только на сервере. */
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
        int idx = Math.floorMod(selectedIndex(stack, list.size()) + dir, list.size());
        stack.set(ModRegistry.SPELL_INDEX.get(), idx);
        Spell spell = list.get(idx);
        int cdLeft = cooldownLeft(stack, spell, player.level().getGameTime());

        MutableComponent msg = Component.translatable("msg.elementalmagic.selected", spell.name())
                .append(Component.literal(" (" + (idx + 1) + "/" + list.size() + ")")
                        .withStyle(ChatFormatting.DARK_GRAY));
        if (cdLeft > 0) {
            msg.append(Component.literal(" [" + seconds(cdLeft) + " с]").withStyle(ChatFormatting.RED));
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
        int idx = selectedIndex(stack, list.size());

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) cycle(player, stack, 1);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (level instanceof ServerLevel serverLevel) {
            Spell spell = list.get(idx);
            long tick = serverLevel.getGameTime();
            int cdLeft = cooldownLeft(stack, spell, tick);

            if (cdLeft > 0) {
                player.displayClientMessage(spell.name()
                        .append(Component.literal(" перезаряжается (" + seconds(cdLeft) + " с)")
                                .withStyle(ChatFormatting.RED)), true);
                return InteractionResultHolder.fail(stack);
            }

            int cost = manaCost(player, spell);
            int mana = Mana.get(player);
            boolean free = player.getAbilities().instabuild;

            if (!free && mana < cost) {
                player.displayClientMessage(Component.translatable("msg.elementalmagic.no_mana")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            if (spell.castFrom(serverLevel, player, element)) {
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
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.learned_count",
                "Записано заклинаний: %s", list.size()).withStyle(ChatFormatting.GRAY));
        if (list.isEmpty()) return;

        Spell s = list.get(selectedIndex(stack, list.size()));
        tooltip.add(Component.translatable("tooltip.elementalmagic.selected", s.name()));
        tooltip.add(Component.literal("  ").append(s.school().displayName())
                .append(Component.literal(" • " + s.manaCost() + " маны • "
                        + s.cooldownSeconds() + " с").withStyle(ChatFormatting.DARK_GRAY)));
        if (s.baseDamage() > 0) {
            float mult = element.bookMultiplier(s.element());
            tooltip.add(Component.literal("  Урон: " + String.format("%.1f", s.damageIn(element))
                    + " (" + percent(mult) + ")").withStyle(ChatFormatting.DARK_GRAY));
        }

        if (ctx.level() != null) {
            int cdLeft = cooldownLeft(stack, s, ctx.level().getGameTime());
            if (cdLeft > 0) {
                tooltip.add(Component.literal("  Перезарядка: " + seconds(cdLeft) + " с")
                        .withStyle(ChatFormatting.RED));
            }
        }

        tooltip.add(Component.translatable("tooltip.elementalmagic.hint").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Z / X: сменить заклинание • V: открыть гримуар")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // ===== Статические методы для книг и посохов =====

    /** Заклинания из любого носителя (книга или посох). */
    public static List<Spell> spellsOf(ItemStack stack, Element element) {
        if (stack.getItem() instanceof SpellbookItem book) return book.spells(stack);
        if (stack.getItem() instanceof StaffItem) {
            List<Spell> out = new ArrayList<>();
            for (String id : learnedIds(stack, element)) {
                Spell s = Spells.byId(id);
                if (s != null) out.add(s);
            }
            return out;
        }
        return List.of();
    }

    public static int selectedIndexOf(ItemStack stack, int size) {
        if (size <= 0) return 0;
        return Math.floorMod(stack.getOrDefault(ModRegistry.SPELL_INDEX.get(), 0), size);
    }

    public static void setCooldownOf(ItemStack stack, Spell spell, long currentTick) {
        Map<String, Long> cds = new HashMap<>(stack.getOrDefault(ModRegistry.SPELL_COOLDOWNS.get(), Map.of()));
        cds.put(spell.id(), currentTick + spell.cooldown());
        stack.set(ModRegistry.SPELL_COOLDOWNS.get(), cds);
    }

    public static void cycleItem(Player player, ItemStack stack, Element element, int dir) {
        List<Spell> list = spellsOf(stack, element);
        if (list.isEmpty()) {
            player.displayClientMessage(Component.translatableWithFallback("msg.elementalmagic.empty_book",
                    "В носителе нет заклинаний. Запиши их со свитков на магическом столе")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (list.size() == 1) {
            player.displayClientMessage(Component.translatableWithFallback("msg.elementalmagic.only_one",
                    "Записано только одно заклинание. Запиши другие со свитков на магическом столе")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int idx = Math.floorMod(selectedIndexOf(stack, list.size()) + dir, list.size());
        stack.set(ModRegistry.SPELL_INDEX.get(), idx);
        Spell spell = list.get(idx);
        int cdLeft = cooldownLeft(stack, spell, player.level().getGameTime());

        MutableComponent msg = Component.translatable("msg.elementalmagic.selected", spell.name())
                .append(Component.literal(" (" + (idx + 1) + "/" + list.size() + ")")
                        .withStyle(ChatFormatting.DARK_GRAY));
        if (cdLeft > 0) {
            msg.append(Component.literal(" [" + seconds(cdLeft) + " с]")
                    .withStyle(ChatFormatting.RED));
        }
        player.displayClientMessage(msg, true);
    }
}
