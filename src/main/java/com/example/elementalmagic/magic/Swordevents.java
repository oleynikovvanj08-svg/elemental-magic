package com.example.elementalmagic.magic;

import com.example.elementalmagic.ElementalSwordItem;
import com.example.elementalmagic.MagicMod;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** События мечей: пассивные эффекты, Шипастая кора, очистка баффов. */
@EventBusSubscriber(modid = MagicMod.MODID)
public final class SwordEvents {

    /** Стихия меча в руках игрока (основная рука важнее) или null. */
    private static Element heldSword(Player p) {
        if (p.getMainHandItem().getItem() instanceof ElementalSwordItem s) return s.element;
        if (p.getOffhandItem().getItem() instanceof ElementalSwordItem s) return s.element;
        return null;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide()) return;
        Element held = heldSword(p);
        if (held != null) SwordSpells.tickPassive(p, held);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player p && !p.level().isClientSide()) {
            SwordSpells.onPlayerDamaged(p, event.getSource(), event.getAmount());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SwordSpells.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SwordSpells.clearAll();
    }

    private SwordEvents() {}
}
