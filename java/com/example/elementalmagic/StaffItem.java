package com.example.elementalmagic;

import com.example.elementalmagic.magic.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class StaffItem extends SwordItem {
    public final Element element;

    public StaffItem(Element element, Properties props) {
        super(Tiers.DIAMOND, props);
        this.element = element;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide) {
            switch (element) {
                case LIGHTNING -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                case LIGHT -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200));
                case DARKNESS -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60));
                case WATER -> target.setTicksFrozen(target.getTicksFrozen() + 100);
                case EARTH -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80));
                case AIR -> target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20));
                case FIRE -> target.igniteForSeconds(4);
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.elementalmagic.element", element.displayName()));
        tooltip.add(Component.translatable("tooltip.elementalmagic.staff_hit." + element.id)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.elementalmagic.staff_bonus").withStyle(ChatFormatting.GOLD));
    }
}