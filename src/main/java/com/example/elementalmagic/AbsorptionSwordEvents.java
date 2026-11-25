package com.example.elementalmagic;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Эффекты Меча поглощения: уменьшенный магический урон и постепенное высасывание маны. */
@EventBusSubscriber(modid = MagicMod.MODID)
public final class AbsorptionSwordEvents {

    /** Меч в любой руке. */
    private static boolean holdsSword(Player p) {
        return p.getMainHandItem().getItem() instanceof AbsorptionSwordItem
                || p.getOffhandItem().getItem() instanceof AbsorptionSwordItem;
    }

    /** Магический урон (заклинания, зелья, ведьмы и т.п.) снижается наполовину. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !holdsSword(player)) return;
        DamageSource source = event.getSource();
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) {
            event.setAmount(event.getAmount() * AbsorptionSwordItem.MAGIC_DAMAGE_FACTOR);
        }
    }

    /** Раз в DRAIN_INTERVAL тиков меч забирает DRAIN_AMOUNT маны. В творческом режиме не забирает. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.getAbilities().instabuild || !holdsSword(player)) return;
        if (player.tickCount % AbsorptionSwordItem.DRAIN_INTERVAL != 0) return;
        int mana = Mana.get(player);
        if (mana > 0) {
            Mana.set(player, Math.max(0, mana - AbsorptionSwordItem.DRAIN_AMOUNT));
        }
    }

    private AbsorptionSwordEvents() {}
}
