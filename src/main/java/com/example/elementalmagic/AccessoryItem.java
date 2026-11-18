package com.example.elementalmagic;

import com.example.elementalmagic.magic.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class AccessoryItem extends Item implements ICurioItem {
    public enum Kind { AMULET, RING }

    public final Element element;
    public final Kind kind;

    public AccessoryItem(Element element, Kind kind, Properties props) {
        super(props);
        this.element = element;
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        String slot = kind == Kind.AMULET ? "слот для амулетов" : "слот для колец";
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.accessory_slot",
                "Работает только в %s", slot).withStyle(ChatFormatting.GRAY));

        int mana = kind == Kind.AMULET ? 50 : 15;
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.accessory_mana",
                "Максимум маны: +%s", mana).withStyle(ChatFormatting.AQUA));

        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.accessory_damage_own",
                "Урон заклинаний стихии %s: +20%%", element.displayName()).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.accessory_damage_other",
                "Урон остальных заклинаний: +10%%").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // ===== Реализация ICurioItem =====
    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        // Проверяем, что предмет пытаются надеть в правильный слот
        String identifier = slotContext.identifier();
        return switch (kind) {
            case AMULET -> identifier.equals("amulet");
            case RING -> identifier.equals("ring");
        };
    }
}
