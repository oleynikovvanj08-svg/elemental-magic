package com.example.elementalmagic;

import com.example.elementalmagic.magic.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = MagicMod.MODID)
public final class ManaEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
                if (p.level().isClientSide || p.tickCount % 10 != 0) return;

                 // Восстановление маны
        int mana = Mana.get(p);
        int max = Mana.max(p);
                if (mana < max) Mana.set(p, mana + Mana.regen(p));

                 // Бонус полного комплекта брони
                for (Element e : Element.values()) {
            if (Mana.armorPieces(p, e) == 4) {
                p.addEffect(new MobEffectInstance(setBonus(e), 300, 0, true, false, true));
            }
        }

                 // Показ маны, когда в руке гримуар или посох
                        boolean holding = isMagicItem(p.getMainHandItem().getItem()) || isMagicItem(p.getOffhandItem().getItem());
        if (holding) {
                        p.displayClientMessage(Component.literal("✦ Мана: " + Mana.get(p) + " / " + max)
                    .withStyle(ChatFormatting.AQUA), true);
        }
    }

    private static boolean isMagicItem(Object item) {
                return item instanceof SpellbookItem || item instanceof StaffItem;
    }

            private static Holder<MobEffect> setBonus(Element e) {
        return switch (e) {
            case LIGHTNING -> MobEffects.MOVEMENT_SPEED;
            case LIGHT -> MobEffects.NIGHT_VISION;
            case DARKNESS -> MobEffects.DAMAGE_BOOST;
            case WATER -> MobEffects.WATER_BREATHING;
            case EARTH -> MobEffects.DAMAGE_RESISTANCE;
            case AIR -> MobEffects.JUMP;
            case FIRE -> MobEffects.FIRE_RESISTANCE;
            case NATURE -> MobEffects.LUCK;
            case ASH -> MobEffects.FIRE_RESISTANCE;
            case EXPLOSION -> MobEffects.DAMAGE_RESISTANCE;
            case ELEMENTAL -> MobEffects.DAMAGE_BOOST;
            case TIME -> MobEffects.DIG_SPEED;
            case STARS -> MobEffects.SLOW_FALLING;
            case DARK_NATURE -> MobEffects.DAMAGE_BOOST;
            case SHADOW -> MobEffects.INVISIBILITY;
        };
    }

    private ManaEvents() {}
}
