package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

import java.util.List;

public final class GravitySpells {

    static void init() {
        // 1. Смена гравитации
        Spells.reg("gravity_shift", School.GRAVITY, 20, 200, (level, player) -> {
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 0));
            level.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 1, 0.5, 0.02);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.5f);
            return true;
        });

        // 2. Гравитационный колодец (6 секунд)
        Spells.reg("gravity_well", School.GRAVITY, 35, 300, (level, player) -> {
            Vec3 point = lookPoint(player, 8);
            int pulses = 12;
            for (int i = 0; i < pulses; i++) {
                int delay = i * 10;
                level.getServer().tell(new TickTask(level.getServer().getTickCount() + delay, () -> {
                    if (!player.isAlive()) return;
                    List<Entity> targets = level.getEntities(player,
                            new AABB(point, point).inflate(6), e -> e instanceof LivingEntity && e.isAlive());
                    for (Entity e : targets) {
                        Vec3 pull = point.subtract(e.position()).normalize().scale(0.8);
                        e.setDeltaMovement(e.getDeltaMovement().add(pull));
                        e.hurt(player.damageSources().indirectMagic(player, player), 1.0f);
                    }
                    level.sendParticles(ParticleTypes.ENCHANT, point.x, point.y, point.z, 20, 2, 2, 2, 0.3);
                }));
            }
            level.playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.6f);
            return true;
        });

        // 3. Шаг луны
        Spells.reg("moon_step", School.GRAVITY, 25, 600, (level, player) -> {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 8));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0));
            player.setDeltaMovement(player.getDeltaMovement().add(0, 1.5, 0));
            level.sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY(), player.getZ(), 30, 0.5, 0.2, 0.5, 0.05);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.8f);
            return true;
        });

        // 4. Сокрушение
        Spells.reg("crush", School.GRAVITY, 40, 300, (level, player) -> {
            List<Entity> targets = level.getEntities(player,
                    player.getBoundingBox().inflate(5), e -> e instanceof LivingEntity && e.isAlive());
            if (targets.isEmpty()) return false;
            for (Entity e : targets) {
                LivingEntity living = (LivingEntity) e;
                living.hurt(player.damageSources().indirectMagic(player, player), 6.0f);
                living.setDeltaMovement(living.getDeltaMovement().add(0, -1.5, 0));
            }
            level.sendParticles(ParticleTypes.GUST,
                    player.getX(), player.getY() + 0.5, player.getZ(), 6, 2, 0.5, 2, 0.1);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 0.7f);
            return true;
        });

        // 5. Чёрная дыра
        Spells.reg("black_hole", School.GRAVITY, 60, 800, (level, player) -> {
            Vec3 point = lookPoint(player, 12);
            List<Entity> targets = level.getEntities(player,
                    new AABB(point, point).inflate(8), e -> e instanceof LivingEntity && e.isAlive());
            if (targets.isEmpty()) return false;
            for (Entity e : targets) {
                Vec3 pull = point.subtract(e.position()).normalize().scale(1.5);
                e.setDeltaMovement(e.getDeltaMovement().add(pull));
                e.hurt(player.damageSources().indirectMagic(player, player), 5.0f);
            }
            level.sendParticles(ParticleTypes.PORTAL, point.x, point.y, point.z, 150, 3, 3, 3, 0.5);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 0.5f);
            return true;
        });

        // 6. Гравитационный пресс
        Spells.reg("gravity_press", School.GRAVITY, 45, 400, 4, (level, player) -> {
            LivingEntity target = nearest(player, 12);
            if (target == null) return false;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 6));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            target.setDeltaMovement(target.getDeltaMovement().add(0, -1.0, 0));
            target.hurt(player.damageSources().indirectMagic(player, player), 4.0f);
            level.sendParticles(ParticleTypes.GUST,
                    target.getX(), target.getY() + 0.5, target.getZ(), 8, 0.8, 0.5, 0.8, 0.05);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 0.6f);
            return true;
        });

        // 7. Гравитационный коллапс
        Spells.reg("gravity_collapse", School.GRAVITY, 60, 800, 5, (level, player) -> {
            Vec3 point = lookPoint(player, 10);
            List<Entity> targets = level.getEntities(player,
                    new AABB(point, point).inflate(5), e -> e instanceof LivingEntity && e.isAlive());
            if (targets.isEmpty()) return false;

            final int impactTick = level.getServer().getTickCount() + 60;
            for (Entity e : targets) {
                LivingEntity living = (LivingEntity) e;
                living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 0));
                final int entityId = living.getId();
                level.getServer().tell(new TickTask(impactTick, () -> {
                    if (level.getEntity(entityId) instanceof LivingEntity victim && victim.isAlive()) {
                        victim.setDeltaMovement(victim.getDeltaMovement().add(0, -2.5, 0));
                        victim.hurt(level.damageSources().magic(), 12.0f);
                        level.sendParticles(ParticleTypes.GUST,
                                victim.getX(), victim.getY(), victim.getZ(), 10, 1, 0.5, 1, 0.1);
                        level.playSound(null, victim.blockPosition(),
                                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
                    }
                }));
            }
            level.sendParticles(ParticleTypes.ENCHANT, point.x, point.y, point.z, 60, 2, 1, 2, 0.2);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.4f);
            return true;
        });

        // 8. Гравитационный толчок (новое)
        Spells.reg("gravity_push", School.GRAVITY, 25, 160, (level, player) -> {
            List<Entity> targets = level.getEntities(player,
                    player.getBoundingBox().inflate(6), e -> e instanceof LivingEntity && e.isAlive());
            if (targets.isEmpty()) return false;
            for (Entity e : targets) {
                Vec3 push = e.position().subtract(player.position()).normalize().scale(1.8);
                e.setDeltaMovement(e.getDeltaMovement().add(push));
                e.hurt(player.damageSources().indirectMagic(player, player), 4.0f);
            }
            level.sendParticles(ParticleTypes.GUST,
                    player.getX(), player.getY() + 0.5, player.getZ(), 40, 3, 0.5, 3, 0.1);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
            return true;
        });

        // 9. Притяжение (новое)
        Spells.reg("gravity_attraction", School.GRAVITY, 30, 240, (level, player) -> {
            List<Entity> targets = level.getEntities(player,
                    player.getBoundingBox().inflate(8), e -> e instanceof LivingEntity && e.isAlive());
            if (targets.isEmpty()) return false;
            for (Entity e : targets) {
                Vec3 pull = player.position().subtract(e.position()).normalize().scale(1.2);
                e.setDeltaMovement(e.getDeltaMovement().add(pull));
                e.hurt(player.damageSources().indirectMagic(player, player), 3.0f);
            }
            level.sendParticles(ParticleTypes.PORTAL,
                    player.getX(), player.getY() + 1, player.getZ(), 60, 3, 1, 3, 0.3);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.8f);
            return true;
        });

        // 10. Невесомость (новое)
        Spells.reg("weightlessness", School.GRAVITY, 20, 300, (level, player) -> {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 100, 3));
            player.setDeltaMovement(player.getDeltaMovement().add(0, 0.5, 0));
            level.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 1, 0.5, 0.02);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.5f);
            return true;
        });

        // 11. Гравитационная волна (новое)
        Spells.reg("gravity_wave", School.GRAVITY, 35, 360, (level, player) -> {
            Vec3 look = player.getViewVector(1.0f);
            Vec3 start = player.position().add(look.scale(2));
            List<Entity> targets = level.getEntities(player,
                    new AABB(start, start).inflate(5), e -> e instanceof LivingEntity && e.isAlive());
            if (targets.isEmpty()) return false;
            for (Entity e : targets) {
                LivingEntity living = (LivingEntity) e;
                living.hurt(player.damageSources().indirectMagic(player, player), 5.0f);
                living.setDeltaMovement(living.getDeltaMovement().add(0, 1.2, 0));
            }
            level.sendParticles(ParticleTypes.GUST, start.x, start.y, start.z, 80, 3, 1, 3, 0.15);
            level.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 30, 2, 1, 2, 0.1);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 0.8f);
            return true;
        });

        // 12. Сфера гравитации (новое)
        Spells.reg("gravity_sphere", School.GRAVITY, 60, 800, (level, player) -> {
            Vec3 point = lookPoint(player, 10);
            int pulses = 8; // 8 импульсов по 10 тиков = 80 тиков (4 секунды)
            for (int i = 0; i < pulses; i++) {
                int delay = i * 10;
                level.getServer().tell(new TickTask(level.getServer().getTickCount() + delay, () -> {
                    List<Entity> targets = level.getEntities(player,
                            new AABB(point, point).inflate(4), e -> e instanceof LivingEntity && e.isAlive());
                    for (Entity e : targets) {
                        LivingEntity living = (LivingEntity) e;
                        living.hurt(player.damageSources().indirectMagic(player, player), 3.0f);
                        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 10));
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 5));
                    }
                    level.sendParticles(ParticleTypes.PORTAL, point.x, point.y, point.z, 30, 2, 2, 2, 0.2);
                }));
            }
            level.playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 0.4f);
            return true;
        });
    }

    // ===== Вспомогательные методы =====

    private static Vec3 lookPoint(Player player, double maxDist) {
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getViewVector(1.0f);
        BlockHitResult block = player.level().clip(new ClipContext(
                start, start.add(dir.scale(maxDist)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double dist = block.getType() == HitResult.Type.MISS
                ? maxDist : block.getLocation().distanceTo(start);
        return start.add(dir.scale(dist));
    }

    private static LivingEntity nearest(Player player, double range) {
        List<Entity> list = player.level().getEntities(player,
                player.getBoundingBox().inflate(range),
                e -> e instanceof LivingEntity && e.isAlive());
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : list) {
            double d = e.distanceToSqr(player);
            if (d < bestD) { bestD = d; best = (LivingEntity) e; }
        }
        return best;
    }

    private GravitySpells() {}
}
