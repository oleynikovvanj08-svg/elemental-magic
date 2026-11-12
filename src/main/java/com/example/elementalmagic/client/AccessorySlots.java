package com.example.elementalmagic.client;

import com.example.elementalmagic.AccessoryItem;
import com.example.elementalmagic.MagicMod;
import com.example.elementalmagic.ModRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = MagicMod.MODID, value = Dist.CLIENT)
public final class AccessorySlots {
    private static final int AMULET_X = -18, AMULET_Y = 8;
    private static final int RING_X = 18, RING_Y = 62;

    private static final ResourceLocation SLOT =
            ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, "textures/gui/accessory_slot.png");

    public static Iterable<ItemStack> amulets(Player p) {
        if (p == null) return List.of(ItemStack.EMPTY, ItemStack.EMPTY);
        List<ItemStack> list = p.getData(ModRegistry.AMULET_SLOTS);
        return list.isEmpty() ? List.of(ItemStack.EMPTY, ItemStack.EMPTY) : list;
    }

    public static Iterable<ItemStack> rings(Player p) {
        if (p == null) return List.of(ItemStack.EMPTY, ItemStack.EMPTY);
        List<ItemStack> list = p.getData(ModRegistry.RING_SLOTS);
        return list.isEmpty() ? List.of(ItemStack.EMPTY, ItemStack.EMPTY) : list;
    }

    private static void set(Player p, AccessoryItem.Kind kind, int slot, ItemStack stack) {
        List<ItemStack> old = kind == AccessoryItem.Kind.AMULET
                ? p.getData(ModRegistry.AMULET_SLOTS)
                : p.getData(ModRegistry.RING_SLOTS);
        List<ItemStack> updated = new ArrayList<>(old);
        while (updated.size() <= slot) updated.add(ItemStack.EMPTY);
        updated.set(slot, stack);
        if (kind == AccessoryItem.Kind.AMULET) {
            p.setData(ModRegistry.AMULET_SLOTS, List.copyOf(updated));
        } else {
            p.setData(ModRegistry.RING_SLOTS, List.copyOf(updated));
        }
    }

    @SubscribeEvent
    public static void onRenderInventory(ContainerScreenEvent.Render.Background event) {
        if (!(event.getContainerScreen() instanceof InventoryScreen screen)) return;

        GuiGraphics g = event.getGuiGraphics();
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        List<ItemStack> amulets = new ArrayList<>();
        amulets(player).forEach(amulets::add);
        for (int i = 0; i < 2; i++) {
            int x = left + AMULET_X;
            int y = top + AMULET_Y + i * 18;
            RenderSystem.setShaderTexture(0, SLOT);
            g.blit(SLOT, x, y, 0, 0, 18, 18, 18, 18);
            ItemStack stack = i < amulets.size() ? amulets.get(i) : ItemStack.EMPTY;
            if (!stack.isEmpty()) g.renderItem(stack, x + 1, y + 1);
        }

        List<ItemStack> rings = new ArrayList<>();
        rings(player).forEach(rings::add);
        for (int i = 0; i < 2; i++) {
            int x = left + RING_X;
            int y = top + RING_Y + i * 18;
            RenderSystem.setShaderTexture(0, SLOT);
            g.blit(SLOT, x, y, 0, 0, 18, 18, 18, 18);
            ItemStack stack = i < rings.size() ? rings.get(i) : ItemStack.EMPTY;
            if (!stack.isEmpty()) g.renderItem(stack, x + 1, y + 1);
        }
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;

        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        int mouseX = (int) event.getMouseX();
        int mouseY = (int) event.getMouseY();

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

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
                    slot = i;
                    kind = AccessoryItem.Kind.RING;
                    break;
                }
            }
        }
        if (slot == null) return;

        ItemStack cursor = screen.getMenu().getCarried();
        List<ItemStack> current = kind == AccessoryItem.Kind.AMULET
                ? new ArrayList<>(player.getData(ModRegistry.AMULET_SLOTS))
                : new ArrayList<>(player.getData(ModRegistry.RING_SLOTS));
        while (current.size() <= slot) current.add(ItemStack.EMPTY);
        ItemStack inSlot = current.get(slot);

        if (!cursor.isEmpty() && !(cursor.getItem() instanceof AccessoryItem acc && acc.kind == kind)) {
            player.displayClientMessage(Component.translatableWithFallback("msg.elementalmagic.wrong_accessory",
                    "Этот предмет не подходит для этого слота").withStyle(net.minecraft.ChatFormatting.RED), true);
            event.setCanceled(true);
            return;
        }

        if (event.getButton() == 0) {
            set(player, kind, slot, cursor);
            screen.getMenu().setCarried(inSlot);
        } else if (event.getButton() == 1 && !inSlot.isEmpty()) {
            ItemStack taken = inSlot.split(1);
            screen.getMenu().setCarried(taken);
            set(player, kind, slot, inSlot);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderTooltip(ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof InventoryScreen screen)) return;
        int mouseX = event.getMouseX() - screen.getGuiLeft();
        int mouseY = event.getMouseY() - screen.getGuiTop();
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        List<ItemStack> amulets = new ArrayList<>();
        amulets(player).forEach(amulets::add);
        for (int i = 0; i < 2; i++) {
            int x = AMULET_X;
            int y = AMULET_Y + i * 18;
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                ItemStack stack = i < amulets.size() ? amulets.get(i) : ItemStack.EMPTY;
                if (!stack.isEmpty()) {
                    event.getGuiGraphics().renderTooltip(screen.getMinecraft().font, stack, event.getMouseX(), event.getMouseY());
                }
                return;
            }
        }

        List<ItemStack> rings = new ArrayList<>();
        rings(player).forEach(rings::add);
        for (int i = 0; i < 2; i++) {
            int x = RING_X;
            int y = RING_Y + i * 18;
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                ItemStack stack = i < rings.size() ? rings.get(i) : ItemStack.EMPTY;
                if (!stack.isEmpty()) {
                    event.getGuiGraphics().renderTooltip(screen.getMinecraft().font, stack, event.getMouseX(), event.getMouseY());
                }
                return;
            }
        }
    }

    private AccessorySlots() {}
}
