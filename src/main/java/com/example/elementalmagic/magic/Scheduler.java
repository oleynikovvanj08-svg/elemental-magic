package com.example.elementalmagic.magic;

import com.example.elementalmagic.MagicMod;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/** Отложенные и повторяющиеся задачи на серверных тиках (громовое облако, граната, Захват и т.д.). */
@EventBusSubscriber(modid = MagicMod.MODID)
public final class Scheduler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private record Task(long runAt, Runnable action) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static long now = 0;

    /** Выполнить через delay тиков. */
    public static void later(int delay, Runnable action) {
        TASKS.add(new Task(now + Math.max(0, delay), action));
    }

    /** Выполнить times раз с интервалом interval тиков. Первый раз на ближайшем тике. */
    public static void repeat(int interval, int times, Runnable action) {
        for (int i = 0; i < times; i++) later(i * interval, action);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        now++;
        if (TASKS.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        TASKS.removeIf(t -> {
            if (t.runAt() <= now) {
                due.add(t);
                return true;
            }
            return false;
        });
        for (Task t : due) {
            try {
                t.action().run();
            } catch (Exception ex) {
                LOGGER.error("[ElementalMagic] Ошибка в отложенной задаче заклинания", ex);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TASKS.clear();
        now = 0;
    }

    private Scheduler() {}
}
