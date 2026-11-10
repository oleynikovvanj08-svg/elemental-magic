package com.example.elementalmagic;

import com.example.elementalmagic.magic.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ElementalArmorItem extends ArmorItem {
    public final Element element;

    public ElementalArmorItem(Element element, Holder<ArmorMaterial> material, Type type, Properties props) {
        super(material, type, props);
        this.element = element;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.elementalmagic.element", element.displayName()));
        tooltip.add(Component.translatable("tooltip.elementalmagic.armor_piece").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.elementalmagic.armor_set." + element.id)
                .withStyle(ChatFormatting.GOLD));
    }
}