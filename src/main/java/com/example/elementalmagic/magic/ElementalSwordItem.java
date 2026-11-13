package com.example.elementalmagic;

import com.example.elementalmagic.magic.Element;
import com.example.elementalmagic.magic.Spell;
import com.example.elementalmagic.magic.SwordSpells;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Магический меч. Три собственных заклинания (не записываются со свитков), пассивный эффект
 * у мечей обычных стихий и эффекты на удар. ПКМ колдует выбранное заклинание,
 * Shift + ПКМ переключает.
 */
public class ElementalSwordItem extends SwordItem {
    /** Бонус меча к урону заклинаний своей стихии. Чуть ниже, чем у посоха, ведь меч ещё и бьёт сам. */
    public static final float OWN_BONUS = 1.15f;
    public static final float OTHER_BONUS = 1.05f;
    public static final float OPPOSITE_PENALTY = 0.95f;

    /** Стихии, у которых есть меч. Должно совпадать с SwordSpells.SWORD_ELEMENTS. */
    public static final Set<Element> HAS_SWORD = EnumSet.of(
            Element.LIGHTNING, Element.WATER, Element.EARTH, Element.AIR, Element.FIRE,
            Element.NATURE, Element.TIME, Element.STARS, Element.DARK_NATURE);

    public final Element element;

    public ElementalSwordItem(Element element, Properties props) {
        super(Tiers.DIAMOND, props);
        this.element = element;
    }

    /** Свойства меча: урон как у алмазного (3 + бонус тира), скорость атаки как у обычного меча. */
    public static Item.Properties properties(Element e) {
        return new Item.Properties()
                .rarity(e.bossOnly ? Rarity.EPIC : Rarity.RARE)
                .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.4f));
    }

    public float multiplier(Element spellElement) {
        if (spellElement == element) return OWN_BONUS;
        if (spellElement == element.opposite()) return OPPOSITE_PENALTY;
        return OTHER_BONUS;
    }

    public List<Spell> spells() {
        return SwordSpells.spellsOf(element);
    }

    private static String seconds(int ticks) {
        return String.format("%.1f", ticks / 20.0);
    }

    /** Смена заклинания (для клавиш Z / X, как у посоха). */
    public void cycle(Player player, ItemStack stack, int dir) {
        SpellbookItem.cycleItem(player, stack, element, dir);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        List<Spell> list = spells();
        if (list.isEmpty()) return InteractionResultHolder.pass(stack);
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

            // Бонус меча × бонус снаряжения. Бонус уровня castWith добавляет сам
            float mult = multiplier(spell.element()) * Mana.damageMultiplier(player, spell.element());
            if (spell.castWith(serverLevel, player, mult)) {
                if (!free) Mana.set(player, mana - cost);
                SpellbookItem.setCooldownOf(stack, spell, tick);
                level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                        SoundSource.PLAYERS, 0.8f, 1.3f);
            } else {
                player.displayClientMessage(Component.translatable("msg.elementalmagic.no_target")
                        .withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Эффекты на удар: поджог у меча огня, Статический заряд, Раскалённый клинок, Эхо. */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (attacker instanceof Player player && !player.level().isClientSide) {
            SwordSpells.onMeleeHit(player, target, element);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.elementalmagic.element", element.displayName()));

        List<Spell> list = spells();
        if (!list.isEmpty()) {
            int idx = SpellbookItem.selectedIndexOf(stack, list.size());
            for (int i = 0; i < list.size(); i++) {
                tooltip.add(Component.literal(i == idx ? "▶ " : "   ").withStyle(ChatFormatting.GRAY)
                        .append(list.get(i).name()));
            }

            Spell s = list.get(idx);
            tooltip.add(Component.literal("  ").append(s.school().displayName())
                    .append(Component.literal(" • " + s.manaCost() + " маны • "
                            + s.cooldownSeconds() + " с").withStyle(ChatFormatting.DARK_GRAY)));
            float dmg = SwordSpells.damageOf(s);
            if (dmg > 0) {
                float mult = multiplier(s.element());
                tooltip.add(Component.literal("  Урон: " + String.format("%.1f", dmg * mult)
                        + " (" + SpellbookItem.percent(mult) + ", без снаряжения)")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            if (ctx.level() != null) {
                int cdLeft = SpellbookItem.cooldownLeft(stack, s, ctx.level().getGameTime());
                if (cdLeft > 0) {
                    tooltip.add(Component.literal("  Перезарядка: " + seconds(cdLeft) + " с")
                            .withStyle(ChatFormatting.RED));
                }
            }
        }

        String passive = SwordSpells.passiveText(element);
        if (passive != null) {
            tooltip.add(Component.literal("Пассивно: " + passive).withStyle(ChatFormatting.GREEN));
        }

        tooltip.add(Component.literal("ПКМ: заклинание • Shift + ПКМ или Z / X: сменить")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Бонус к урону: +15% своя школа, +5% чужая, -5% противоположная")
                .withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
