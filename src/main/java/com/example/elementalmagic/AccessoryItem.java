package com.example.elementalmagic;

import com.example.elementalmagic.magic.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class AccessoryItem extends Item {
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
        tooltip.add(Component.translatable("tooltip.elementalmagic.element", element.displayName()));
        String key = kind == Kind.AMULET ? "tooltip.elementalmagic.amulet" : "tooltip.elementalmagic.ring";
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.elementalmagic.in_inventory").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}