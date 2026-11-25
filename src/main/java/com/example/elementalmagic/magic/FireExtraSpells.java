package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static com.example.elementalmagic.magic.School.FLAME;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

/**
 * Дополнительные заклинания огня.
 *
 * Метеоритный дождь: за 3 секунды на область падает град огненных шаров.
 * Каждый третий шар крупный (как у гаста), остальные мелкие.
 * Урон наносится по области в момент падения, независимо от попадания снаряда.
 */
final class FireExtraSpells {
    static final String METEOR_TAG = "em_meteor";
    static final String METEOR_DAMAGE = "em_meteor_damage";
    static final String METEOR_RADIUS = "em_meteor_radius";

    // ===== Настройки баланса =====
    private static final int METEOR_COUNT = 30;           // сколько шаров в дожде (было 16)
    private static final int METEOR_INTERVAL_TICKS = 3;   // пауза между шарами (было 4)
    private static final double AREA_RADIUS = 12.0;       // радиус зоны обстрела (было 7)
    private static final float DEFAULT_DAMAGE = 8f;       // базовый урон мелкого шара (было 4)

    static void init() {
        reg("meteor_shower", FLAME, 55, 600, 4, (level, p) -> {
            Vec3 center = aimPoint(p, 35); // чуть дальше прицеливание (было 30)
            if (center == null) return false;

            float base = SpellInfo.damage("meteor_shower");
            final float damage = scaled(base > 0 ? base : DEFAULT_DAMAGE);

            // Обозначаем зону обстрела
            ring(level, center, AREA_RADIUS, ParticleTypes.FLAME);
            ring(level, center, AREA_RADIUS * 0.5, ParticleTypes.LAVA);
            sound(level, p, SoundEvents.BLAZE_SHOOT, 0.6f);

            for (int i = 0; i < METEOR_COUNT; i++) {
                final boolean heavy = i % 3 == 0;
                Scheduler.later(10 + i * METEOR_INTERVAL_TICKS, () -> spawnMeteor(level, p, center, damage, heavy));
            }
            return true;
        });
    }

    private static void spawnMeteor(ServerLevel level, Player owner, Vec3 center, float damage, boolean heavy) {
        RandomSource random = level.random;
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = Math.sqrt(random.nextDouble()) * AREA_RADIUS;
        Vec3 target = new Vec3(center.x + Math.cos(angle) * dist, center.y, center.z + Math.sin(angle) * dist);

        // 1. Наносим урон по области СРАЗУ (чтобы не зависеть от MeteorEvents)
        float finalDamage = heavy ? damage * 2f : damage;
        float radius = heavy ? 5.0f : 3.5f;
        for (LivingEntity e : enemies(level, owner, target, radius)) {
            hurt(owner, e, finalDamage);
            e.igniteForSeconds(heavy ? 6 : 4);
            burst(level, mid(e), ParticleTypes.FLAME, 10, 1);
        }
        // Визуал взрыва
        burst(level, target, ParticleTypes.EXPLOSION_EMITTER, 1, 0);
        burst(level, target, ParticleTypes.LAVA, heavy ? 30 : 15, heavy ? 2 : 1);
        burst(level, target, ParticleTypes.FLAME, heavy ? 40 : 20, heavy ? 2 : 1);
        sound(level, owner, SoundEvents.GENERIC_EXPLODE.value(), heavy ? 1.5f : 1.0f);

        // 2. Спавним снаряд для визуала (он упадёт и исчезнет, но урон уже нанесён)
        Vec3 direction = new Vec3(0.35, -1.0, 0.2).normalize();
        Vec3 start = target.subtract(direction.scale(28));
        Vec3 motion = direction.scale(1.3);

        Fireball fireball;
        if (heavy) {
            fireball = new LargeFireball(level, owner, motion, 1);
        } else {
            fireball = new SmallFireball(level, owner, motion);
        }
        fireball.setPos(start.x, start.y, start.z);
        fireball.setDeltaMovement(motion);

        var data = fireball.getPersistentData();
        data.putBoolean(METEOR_TAG, true);
        data.putFloat(METEOR_DAMAGE, finalDamage);
        data.putFloat(METEOR_RADIUS, radius);
        level.addFreshEntity(fireball);
    }

    private static Vec3 aimPoint(Player p, double range) {
        LivingEntity t = target(p, range);
        if (t != null) return t.position();
        HitResult hit = p.pick(range, 0, false);
        return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
    }

    private FireExtraSpells() {}
}
