package com.example.elementalmagic.magic;

import com.example.elementalmagic.MagicMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Обработчики событий для заклинаний: водяной щит, призванные существа, заморозка ИИ, Backtrack. */
@EventBusSubscriber(modid = MagicMod.MODID)
public final class MagicEvents {

    // ====================== Водяной щит ======================

    private static final Map<UUID, Long> WATER_SHIELD = new HashMap<>();

    /** Защищает от магического урона (заклинаний) до указанного игрового времени. */
    static void waterShield(LivingEntity e, long untilGameTime) {
        WATER_SHIELD.put(e.getUUID(), untilGameTime);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity e = event.getEntity();
        Long until = WATER_SHIELD.get(e.getUUID());
        if (until == null) return;
        if (e.level().getGameTime() > until) {
            WATER_SHIELD.remove(e.getUUID());
            return;
        }
        DamageSource src = event.getSource();
        if (src.is(DamageTypes.INDIRECT_MAGIC) || src.is(DamageTypes.MAGIC)) {
            event.setCanceled(true);
            if (e.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(),
                        25, 0.4, 0.6, 0.4, 0.1);
            }
        }
    }

    // ====================== Призванные существа ======================

    /** Призванные существа не дают лут и опыт. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity().getPersistentData().hasUUID(SpellUtil.OWNER_TAG)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onXp(LivingExperienceDropEvent event) {
        if (event.getEntity().getPersistentData().hasUUID(SpellUtil.OWNER_TAG)) event.setDroppedExperience(0);
    }

    // ====================== Заморозка ИИ (остановка времени, путы, Захват) ======================

    static final String FROZEN_TAG = "em_frozen";

    /** Отключает ИИ моба на указанное число тиков. Игроков не затрагивает (для них есть эффекты). */
    static void freezeAi(LivingEntity e, int ticks) {
        if (!(e instanceof Mob mob) || mob.isNoAi()) return;
        mob.setNoAi(true);
        mob.getPersistentData().putBoolean(FROZEN_TAG, true);
        Scheduler.later(ticks, () -> {
            if (!mob.isRemoved()) {
                mob.setNoAi(false);
                mob.getPersistentData().remove(FROZEN_TAG);
            }
        });
    }

    /** Если сервер остановился во время заморозки, при загрузке моба ИИ возвращается. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Mob mob && mob.getPersistentData().getBoolean(FROZEN_TAG)) {
            mob.setNoAi(false);
            mob.getPersistentData().remove(FROZEN_TAG);
        }
    }

    // ====================== История позиций для Backtrack ======================

    private record Snapshot(ResourceKey<Level> dim, Vec3 pos, float yRot, float xRot, float health) {}

    /** 10 секунд истории: максимальное время отката. */
    private static final int HISTORY_TICKS = 200;
    private static final Map<UUID, ArrayDeque<Snapshot>> HISTORY = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !sp.isAlive()) return;
        ArrayDeque<Snapshot> history = HISTORY.computeIfAbsent(sp.getUUID(), k -> new ArrayDeque<>());
        Snapshot last = history.peekLast();
        if (last != null && last.dim() != sp.level().dimension()) history.clear();
        history.addLast(new Snapshot(sp.level().dimension(), sp.position(), sp.getYRot(), sp.getXRot(), sp.getHealth()));
        while (history.size() > HISTORY_TICKS) history.removeFirst();
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        HISTORY.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HISTORY.remove(event.getEntity().getUUID());
        WATER_SHIELD.remove(event.getEntity().getUUID());
    }

    /**
     * Возвращает игрока на позицию seconds секунд назад (или в самую раннюю сохранённую).
     * Здоровье поднимается до того, что было тогда, но никогда не уменьшается.
     * Возвращает false, если истории меньше секунды.
     */
    static boolean rewind(ServerPlayer p, int seconds) {
        ArrayDeque<Snapshot> history = HISTORY.get(p.getUUID());
        if (history == null || history.size() < 20) return false;

        int index = Math.max(0, history.size() - 1 - seconds * 20);
        Snapshot target = null;
        int i = 0;
        for (Snapshot s : history) {
            if (i++ == index) {
                target = s;
                break;
            }
        }
        if (target == null || target.dim() != p.level().dimension()) return false;

        p.connection.teleport(target.pos().x, target.pos().y, target.pos().z, target.yRot(), target.xRot());
        p.resetFallDistance();
        p.clearFire();
        if (target.health() > p.getHealth()) p.setHealth(Math.min(target.health(), p.getMaxHealth()));

        // «Будущее», которого не было, из истории убираем
        while (history.size() > index + 1) history.removeLast();
        return true;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        HISTORY.clear();
        WATER_SHIELD.clear();
    }

    private MagicEvents() {}
}
