package com.example.elementalmagic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

public class CurioPredicates {
    public static final ResourceLocation RING_VALIDATOR = ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, "ring_validator");
    public static final ResourceLocation AMULET_VALIDATOR = ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, "amulet_validator");

    public static void register() {
        // Предикат для колец: проверяет, что предмет — это AccessoryItem с kind == RING
        CuriosApi.registerCurioPredicates(RING_VALIDATOR, (SlotResult slotResult) -> {
            ItemStack stack = slotResult.stack();
            return stack.getItem() instanceof AccessoryItem accessory && accessory.kind == AccessoryItem.Kind.RING;
        });

        // Предикат для амулетов: проверяет, что предмет — это AccessoryItem с kind == AMULET
        CuriosApi.registerCurioPredicates(AMULET_VALIDATOR, (SlotResult slotResult) -> {
            ItemStack stack = slotResult.stack();
            return stack.getItem() instanceof AccessoryItem accessory && accessory.kind == AccessoryItem.Kind.AMULET;
        });
    }
}
