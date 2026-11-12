package com.example.elementalmagic.client;

import com.example.elementalmagic.Mana;
import com.example.elementalmagic.SpellbookItem;
import com.example.elementalmagic.StaffItem;
import com.example.elementalmagic.magic.Element;
import com.example.elementalmagic.magic.Spell;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Окно гримуара или посоха: список записанных заклинаний с характеристиками. */
public class SpellbookScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int LINE = 11;

    private final InteractionHand hand;
    private double scroll;
    private int contentHeight;

    public SpellbookScreen(InteractionHand hand) {
        super(Component.literal("Гримуар"));
        this.hand = hand;
    }

    private ItemStack carrierStack() {
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        return player == null ? ItemStack.EMPTY : player.getItemInHand(hand);
    }

    private int listTop() {
        return 44;
    }

    private int listBottom() {
        return height - 24;
    }

    @Override
    public void tick() {
        // Если носитель убрали из руки, окно закрывается
        Item item = carrierStack().getItem();
        if (!(item instanceof SpellbookItem || item instanceof StaffItem)) onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int visible = listBottom() - listTop();
        int max = Math.max(0, contentHeight - visible);
        scroll = Math.max(0, Math.min(max, scroll - scrollY * LINE * 2));
        return true;
    }

    private static String fmt(double value) {
        return String.format("%.1f", value);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        ItemStack stack = carrierStack();
        Item item = stack.getItem();
        Element element;
        String title;
        if (item instanceof SpellbookItem book) {
            element = book.element;
            title = "Гримуар: ";
        } else if (item instanceof StaffItem staff) {
            element = staff.element;
            title = "Посох: ";
        } else {
            return;
        }

        if (minecraft == null || minecraft.player == null || minecraft.level == null) return;
        LocalPlayer player = minecraft.player;

        int left = (width - PANEL_WIDTH) / 2;
        int right = left + PANEL_WIDTH;
        int top = listTop();
        int bottom = listBottom();

        g.fill(left - 8, top - 8, right + 8, bottom + 8, 0xD0100C18);
        g.drawCenteredString(font, Component.literal(title).append(element.displayName()),
                width / 2, 12, 0xFFFFFF);
        float gear = Mana.damageMultiplier(player, element);
        g.drawCenteredString(font, Component.literal("Бонус снаряжения к своей стихии: "
                        + SpellbookItem.percent(gear)).withStyle(ChatFormatting.GOLD),
                width / 2, 24, 0xFFFFFF);
        g.drawCenteredString(font, Component.literal("Колесо мыши: прокрутка • Esc: закрыть")
                .withStyle(ChatFormatting.DARK_GRAY), width / 2, height - 14, 0xFFFFFF);

        List<Spell> spells = SpellbookItem.spellsOf(stack, element);
        if (spells.isEmpty()) {
            g.drawCenteredString(font, Component.literal("Здесь нет заклинаний. Запиши их со свитков на магическом столе")
                    .withStyle(ChatFormatting.GRAY), width / 2, top + 10, 0xFFFFFF);
            contentHeight = 0;
            return;
        }

        int selected = SpellbookItem.selectedIndexOf(stack, spells.size());
        long now = minecraft.level.getGameTime();

        g.enableScissor(left - 8, top, right + 8, bottom);
        int y = top - (int) scroll;
        int total = 0;
        for (int i = 0; i < spells.size(); i++) {
            Spell spell = spells.get(i);
            List<FormattedCharSequence> desc = font.split(spell.description(), PANEL_WIDTH - 8);
            int h = LINE * (3 + desc.size()) + 8;

            if (y + h > top && y < bottom) {
                if (i == selected) g.fill(left - 4, y - 3, right + 4, y + h - 5, 0x30FFFFFF);
                drawSpell(g, player, stack, spell, i, i == selected, desc, left, y, now);
            }
            y += h;
            total += h;
        }
        g.disableScissor();
        contentHeight = total;
    }

    private void drawSpell(GuiGraphics g, LocalPlayer player, ItemStack stack, Spell spell,
                           int index, boolean selected, List<FormattedCharSequence> desc, int x, int y, long now) {
        // Строка 1: номер, название и уровень
        MutableComponent name = Component.literal((index + 1) + ". ").withStyle(ChatFormatting.GRAY)
                .append(spell.name())
                .append(Component.literal("  [ур. " + spell.tier() + "]").withStyle(ChatFormatting.DARK_GRAY));
        if (selected) name.append(Component.literal("  ◀ выбрано").withStyle(ChatFormatting.YELLOW));
        g.drawString(font, name, x, y, 0xFFFFFF);

        // Строка 2: школа и мана
        MutableComponent info = Component.literal("").append(spell.school().displayName())
                .append(Component.literal("  •  Мана: " + SpellbookItem.manaCost(player, spell))
                        .withStyle(ChatFormatting.AQUA));
        g.drawString(font, info, x, y + LINE, 0xFFFFFF);

        // Строка 3: урон и перезарядка
        float mult = SpellbookItem.carrierMultiplier(stack, spell.element())
                * Mana.damageMultiplier(player, spell.element());

        MutableComponent stats;
        if (spell.baseDamage() > 0) {
            stats = Component.literal("Урон: " + fmt(spell.baseDamage() * mult)).withStyle(ChatFormatting.RED)
                    .append(Component.literal(" (" + SpellbookItem.percent(mult) + ")")
                            .withStyle(mult >= 1f ? ChatFormatting.GREEN : ChatFormatting.DARK_RED));
        } else {
            stats = Component.literal("Урон: —").withStyle(ChatFormatting.DARK_GRAY);
        }
        stats.append(Component.literal("  •  Перезарядка: " + fmt(spell.cooldownSeconds()) + " с")
                .withStyle(ChatFormatting.GOLD));
        int cdLeft = SpellbookItem.cooldownLeft(stack, spell, now);
        if (cdLeft > 0) {
            stats.append(Component.literal(" (ещё " + fmt(cdLeft / 20.0) + " с)").withStyle(ChatFormatting.RED));
        }
        g.drawString(font, stats, x, y + LINE * 2, 0xFFFFFF);

        // Описание
        int dy = y + LINE * 3;
        for (FormattedCharSequence line : desc) {
            g.drawString(font, line, x + 4, dy, 0xAAAAAA);
            dy += LINE;
        }
    }
}
