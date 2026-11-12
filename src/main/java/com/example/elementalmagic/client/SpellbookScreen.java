package com.example.elementalmagic.client;

import com.example.elementalmagic.SpellbookItem;
import com.example.elementalmagic.magic.Spell;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Окно гримуара: список записанных заклинаний с характеристиками. */
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

    private ItemStack bookStack() {
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        return player == null ? ItemStack.EMPTY : player.getItemInHand(hand);
    }

    private int listTop() {
        return 32;
    }

    private int listBottom() {
        return height - 24;
    }

    @Override
    public void tick() {
        // Если книгу убрали из руки, окно закрывается
        if (!(bookStack().getItem() instanceof SpellbookItem)) onClose();
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

        ItemStack stack = bookStack();
        if (!(stack.getItem() instanceof SpellbookItem book)) return;
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return;
        LocalPlayer player = minecraft.player;

        int left = (width - PANEL_WIDTH) / 2;
        int right = left + PANEL_WIDTH;
        int top = listTop();
        int bottom = listBottom();

        g.fill(left - 8, top - 8, right + 8, bottom + 8, 0xD0100C18);
        g.drawCenteredString(font, Component.literal("Гримуар: ").append(book.element.displayName()),
                width / 2, 12, 0xFFFFFF);
        g.drawCenteredString(font, Component.literal("Колесо мыши: прокрутка • Z / X: выбор • Esc: закрыть")
                .withStyle(ChatFormatting.DARK_GRAY), width / 2, height - 14, 0xFFFFFF);

        List<Spell> spells = book.spells(stack);
        if (spells.isEmpty()) {
            g.drawCenteredString(font, Component.literal("В книге нет заклинаний. Запиши их со свитков на магическом столе")
                    .withStyle(ChatFormatting.GRAY), width / 2, top + 10, 0xFFFFFF);
            contentHeight = 0;
            return;
        }

        int selected = book.selectedIndex(stack, spells.size());
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
                drawSpell(g, player, stack, book, spell, i, i == selected, desc, left, y, now);
            }
            y += h;
            total += h;
        }
        g.disableScissor();
        contentHeight = total;
    }

    private void drawSpell(GuiGraphics g, LocalPlayer player, ItemStack stack, SpellbookItem book, Spell spell,
                           int index, boolean selected, List<FormattedCharSequence> desc, int x, int y, long now) {
        // Строка 1: номер и название
        MutableComponent name = Component.literal((index + 1) + ". ").withStyle(ChatFormatting.GRAY)
                .append(spell.name());
        if (selected) name.append(Component.literal("  ◀ выбрано").withStyle(ChatFormatting.YELLOW));
        g.drawString(font, name, x, y, 0xFFFFFF);

        // Строка 2: школа и мана
        MutableComponent info = Component.literal("").append(spell.school().displayName())
                .append(Component.literal("  •  Мана: " + SpellbookItem.manaCost(player, spell))
                        .withStyle(ChatFormatting.AQUA));
        g.drawString(font, info, x, y + LINE, 0xFFFFFF);

        // Строка 3: урон и перезарядка
        float mult = book.element.bookMultiplier(spell.element());
        MutableComponent stats;
        if (spell.baseDamage() > 0) {
            stats = Component.literal("Урон: " + fmt(spell.damageIn(book.element))).withStyle(ChatFormatting.RED)
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
