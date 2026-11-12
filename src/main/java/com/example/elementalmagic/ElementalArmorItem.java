package com.example.elementalmagic;

import com.example.elementalmagic.magic.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Supplier;

public class ElementalArmorItem extends ArmorItem {
    public final Element element;

    public ElementalArmorItem(Element element, Supplier<ArmorMaterial> material, Type type, Properties props) {
        super(material, type, props);
        this.element = element;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.armor_element",
                "Стихия: %s", element.displayName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.armor_mana",
                "Максимум маны: +150").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.armor_damage",
                "Урон заклинаний стихии %s: +5%%", element.displayName()).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatableWithFallback("tooltip.elementalmagic.armor_damage_all",
                "Урон остальных заклинаний: +5%%").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return element.bossOnly;
    }
}
