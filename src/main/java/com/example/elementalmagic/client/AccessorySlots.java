package com.example.elementalmagic.client;

import com.example.elementalmagic.AccessoryItem;
import com.example.elementalmagic.MagicMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/**
 * Добавляет 4 слота в инвентарь игрока: 2 для амулетов (выше шлема) и 2 для колец (правее сапог).
 * Предметы работают, только если лежат в этих слотах.
 */
@EventBusSubscriber(modid = MagicMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class AccessorySlots {
    /** Смещение амулетов: левее брони, выше шлема. */
    private static final int AMULET_X = -18, AMULET_Y = 8;
    /** Смещение колец: правее брони, ниже сапог. */
    private static final int RING_X = 18, RING_Y = 62;

    private static final ResourceLocation SLOT =
            ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, "textures/gui/accessory_slot.png");

    /** Инвентарь с 4 дополнительными слотами: [36..39] амулеты, [40..41] кольца. */
    private static ItemStack[] extraSlots = new ItemStack[4];

    static {
        for (int i = 0; i < extraSlots.length; i++) extraSlots[i] = ItemStack.EMPTY;
    }

    /** Возвращает предметы в двух слотах для амулетов. */
    public static Iterable<ItemStack> amulets(Player p) {
        return List.of(get(p, 0), get(p, 1));
    }

    /** Возвращает предметы в двух слотах для колец. */
    public static Iterable<ItemStack> rings(Player p) {
        return List.of(get(p, 2), get(p, 3));
    }

    private static ItemStack get(Player p, int slot) {
        if (p.level().isClientSide) return extraSlots[slot];
        // На сервере храним в последних 4 слотах инвентаря
        Inventory inv = p.getInventory();
        int i = inv.getContainerSize() - 4 + slot;
        return i < inv.getContainerSize() ? inv.getItem(i) : ItemStack.EMPTY;
    }

    private static void set(Player p, int slot, ItemStack stack) {
        if (p.level().isClientSide) {
            extraSlots[slot] = stack;
        } else {
            Inventory inv = p.getInventory();
            int i = inv.getContainerSize() - 4 + slot;
            if (i < inv.getContainerSize()) inv.setItem(i, stack);
        }
    }

    /** Синхронизация клиента с сервером каждый тик. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!p.level().isClientSide) return;
        Inventory inv = p.getInventory();
        for (int i = 0; i < 4; i++) {
            int idx = inv.getContainerSize() - 4 + i;
            if (idx < inv.getContainerSize()) extraSlots[i] = inv.getItem(idx);
        }
    }

    /** Рисуем слоты и предметы в них. */
    @SubscribeEvent
    public static void onRenderInventory(ContainerScreenEvent.Render.Background event) {
        if (!(event.getContainerScreen() instanceof InventoryScreen screen)) return;
        if (!(screen.getMenu() instanceof InventoryMenu)) return;

        GuiGraphics g = event.getGuiGraphics();
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();

        // Слоты для амулетов
        RenderSystem.setShaderTexture(0, SLOT);
        for (int i = 0; i < 2; i++) {
            int x = left + AMULET_X;
            int y = top + AMULET_Y + i * 18;
            g.blit(SLOT, x, y, 0, 0, 18, 18, 18, 18);
            ItemStack stack = extraSlots[i];
            if (!stack.isEmpty()) g.renderItem(stack, x + 1, y + 1);
        }

        // Слоты для колец
        for (int i = 0; i < 2; i++) {
            int x = left + RING_X;
            int y = top + RING_Y + i * 18;
            g.blit(SLOT, x, y, 0, 0, 18, 18, 18, 18);
            ItemStack stack = extraSlots[2 + i];
            if (!stack.isEmpty()) g.renderItem(stack, x + 1, y + 1);
        }
    }

    /** Обработка кликов: положить, взять, поменять местами. */
    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        if (!(screen.getMenu() instanceof InventoryMenu)) return;

        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        int mouseX = (int) event.getMouseX();
        int mouseY = (int) event.getMouseY();

        Integer slot = null;
        AccessoryItem.Kind kind = null;

        for (int i = 0; i < 2; i++) {
            int x = left + AMULET_X;
            int y = top + AMULET_Y + i * 18;
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                slot = i;
                kind = AccessoryItem.Kind.AMULET;
                break;
            }
        }
        if (slot == null) {
            for (int i = 0; i < 2; i++) {
                int x = left + RING_X;
                int y = top + RING_Y + i * 18;
                if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                    slot = 2 + i;
                    kind = AccessoryItem.Kind.RING;
                    break;
                }
            }
        }
        if (slot == null) return;

        Player p = Minecraft.getInstance().player;
        if (p == null) return;

        ItemStack cursor = screen.getMenu().getCarried();
        ItemStack inSlot = extraSlots[slot];

        if (!cursor.isEmpty() && !(cursor.getItem() instanceof AccessoryItem acc && acc.kind == kind)) {
            p.displayClientMessage(Component.translatableWithFallback("msg.elementalmagic.wrong_accessory",
                    "Этот предмет не подходит для этого слота").withStyle(net.minecraft.ChatFormatting.RED), true);
            event.setCanceled(true);
            return;
        }

        // ЛКМ: положить/взять, ПКМ: взять один
        if (event.getButton() == 0) {
            extraSlots[slot] = cursor;
            screen.getMenu().setCarried(inSlot);
            set(p, slot, cursor);
        } else if (event.getButton() == 1 && !inSlot.isEmpty()) {
            ItemStack taken = inSlot.split(1);
            screen.getMenu().setCarried(taken);
            set(p, slot, inSlot);
        }
        event.setCanceled(true);
    }

    /** Подсказка при наведении. */
    @SubscribeEvent
    public static void onRenderTooltip(ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof InventoryScreen screen)) return;
        int mouseX = event.getMouseX() - screen.getGuiLeft();
        int mouseY = event.getMouseY() - screen.getGuiTop();

        for (int i = 0; i < 2; i++) {
            int x = AMULET_X;
            int y = AMULET_Y + i * 18;
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                ItemStack stack = extraSlots[i];
                if (!stack.isEmpty()) {
                    event.getGuiGraphics().renderTooltip(screen.getMinecraft().font, stack, event.getMouseX(), event.getMouseY());
                }
                return;
            }
        }
        for (int i = 0; i < 2; i++) {
            int x = RING_X;
            int y = RING_Y + i * 18;
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                ItemStack stack = extraSlots[2 + i];
                if (!stack.isEmpty()) {
                    event.getGuiGraphics().renderTooltip(screen.getMinecraft().font, stack, event.getMouseX(), event.getMouseY());
                }
                return;
            }
        }
    }

    private AccessorySlots() {}
}
