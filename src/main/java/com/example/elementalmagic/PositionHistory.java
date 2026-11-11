package com.example.elementalmagic;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = MagicMod.MODID)
public final class PositionHistory {
    private static final Map<UUID, LinkedList<Vec3>> HISTORY = new HashMap<>();
    private static final int MAX_SIZE = 100;

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || p.tickCount % 5 != 0) return;
        LinkedList<Vec3> list = HISTORY.computeIfAbsent(p.getUUID(), k -> new LinkedList<>());
        list.addFirst(p.position());
        if (list.size() > MAX_SIZE) list.removeLast();
    }

    /** Позиция игрока N тиков назад (5 тиков = 1 запись). Если нет данных, null. */
    public static Vec3 get(Player p, int ticksAgo) {
        LinkedList<Vec3> list = HISTORY.get(p.getUUID());
        if (list == null) return null;
        int idx = ticksAgo / 5;
        return idx < list.size() ? list.get(idx) : null;
    }

    private PositionHistory() {}
}
