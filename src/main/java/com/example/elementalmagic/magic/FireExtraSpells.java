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
 * Дополнительные заклинания огня. Вызывается из ExtraSpells.init().
 *
 * Метеоритный дождь: за 3 секунды на область падает град огненных шаров (каждый третий крупный,
 * как у гаста, остальные мелкие, как огненный заряд). Шары настоящие, но вместо ванильного
 * взрыва и пожара их попадание обрабатывает MeteorEvents: урон по площади и поджог врагов,
 * блоки не горят и не разрушаются.
 */
final class FireExtraSpells {
    /** Метки на снаряде: по ним MeteorEvents узнаёт метеор и берёт его параметры. */
    static final String METEOR_TAG = "em_meteor";
    static final String METEOR_DAMAGE = "em_meteor_damage";
    static final String METEOR_RADIUS = "em_meteor_radius";

    // ===== Настройки баланса =====
    private static final int METEOR_COUNT = 16;           // сколько шаров в дожде
    private static final int METEOR_INTERVAL_TICKS = 4;   // пауза между шарами
    private static final double AREA_RADIUS = 7.0;        // радиус зоны обстрела
    private static final float DEFAULT_DAMAGE = 4f;       // базовый урон мелкого шара (крупный x2)

    static void init() {
        reg("meteor_shower", FLAME, 55, 600, 4, (level, p) -> {
            Vec3 center = aimPoint(p, 30);
            if (center == null) return false;

            // Урон считается сейчас, пока действует множитель посоха, книги и уровня
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

        // Все метеоры летят в одну сторону под углом, как настоящий метеорный поток
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
        data.putFloat(METEOR_DAMAGE, heavy ? damage * 2f : damage);
        data.putFloat(METEOR_RADIUS, heavy ? 3.2f : 2.2f);
        level.addFreshEntity(fireball);
    }

    /** Точка прицеливания: существо, иначе блок. Null, если игрок смотрит в пустоту. */
    private static Vec3 aimPoint(Player p, double range) {
        LivingEntity t = target(p, range);
        if (t != null) return t.position();
        HitResult hit = p.pick(range, 0, false);
        return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
    }

    private FireExtraSpells() {}
}
