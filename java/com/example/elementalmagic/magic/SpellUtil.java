package com.example.elementalmagic.magic;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

final class SpellUtil {

    // ---------- Поиск целей ----------

    /** Существо, на которое смотрит игрок (сквозь стены не видит). */
    static LivingEntity target(Player p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle().scale(range);
        double maxDist = range * range;
        HitResult block = p.pick(range, 0, false);
        if (block.getType() != HitResult.Type.MISS) maxDist = block.getLocation().distanceToSqr(eye);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, eye, eye.add(look),
                p.getBoundingBox().expandTowards(look).inflate(1),
                e -> e instanceof LivingEntity le && le.isAlive() && !e.isSpectator() && !isAlly(p, le),
                maxDist);
        return hit != null ? (LivingEntity) hit.getEntity() : null;
    }

    /** Все существа вокруг точки, кроме самого игрока и его питомцев. */
    static List<LivingEntity> around(ServerLevel level, Player p, Vec3 center, double r) {
        return level.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(center, r * 2, r * 2, r * 2),
                e -> e != p && e.isAlive() && !isAlly(p, e) && e.distanceToSqr(center) <= r * r);
    }

    static List<LivingEntity> around(ServerLevel level, Player p, double r) {
        return around(level, p, p.position(), r);
    }

    /** Только враждебные мобы вокруг точки. */
    static List<LivingEntity> enemies(ServerLevel level, Player p, Vec3 center, double r) {
        return around(level, p, center, r).stream().filter(e -> e instanceof Enemy).toList();
    }

    static List<LivingEntity> enemies(ServerLevel level, Player p, double r) {
        return enemies(level, p, p.position(), r);
    }

    /** Сам игрок, другие игроки и питомцы игрока. */
    static List<LivingEntity> friends(ServerLevel level, Player p, double r) {
        return level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                e -> e.isAlive() && (e == p || e instanceof Player || isAlly(p, e)));
    }

    /** Все существа на линии взгляда (до первого блока). */
    static List<LivingEntity> beam(ServerLevel level, Player p, double range, double width) {
        Vec3 start = p.getEyePosition();
        Vec3 end = beamEnd(p, range);
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(width + 1),
                e -> e != p && e.isAlive() && !isAlly(p, e)
                        && e.getBoundingBox().inflate(width).clip(start, end).isPresent());
    }

    static Vec3 beamEnd(Player p, double range) {
        return p.pick(range, 0, false).getLocation();
    }

    static boolean isAlly(Player p, LivingEntity e) {
        return e instanceof TamableAnimal t && t.isOwnedBy(p);
    }

    static boolean isUndead(LivingEntity e) {
        return e.getType().is(EntityTypeTags.UNDEAD);
    }

    static Vec3 mid(Entity e) {
        return e.position().add(0, e.getBbHeight() / 2, 0);
    }

    // ---------- Действия ----------

    static void hurt(Player p, LivingEntity e, float amount) {
        e.hurt(p.damageSources().indirectMagic(p, p), amount);
    }

    static void effect(LivingEntity e, Holder<MobEffect> effect, int ticks, int amplifier) {
        e.addEffect(new MobEffectInstance(effect, ticks, amplifier));
    }

    /** Снимает все негативные эффекты и огонь. */
    static void cleanse(LivingEntity e) {
        for (MobEffectInstance inst : new ArrayList<>(e.getActiveEffects())) {
            if (!inst.getEffect().value().isBeneficial()) e.removeEffect(inst.getEffect());
        }
        e.clearFire();
    }

    static void push(Entity e, Vec3 motion) {
        e.setDeltaMovement(motion);
        e.hurtMarked = true;
    }

    static void pushFrom(Entity e, Vec3 from, double strength, double up) {
        Vec3 dir = e.position().subtract(from);
        dir = new Vec3(dir.x, 0, dir.z);
        if (dir.lengthSqr() < 1.0E-4) dir = new Vec3(0, 0, 1);
        push(e, dir.normalize().scale(strength).add(0, up, 0));
    }

    static void pullTo(Entity e, Vec3 to, double strength) {
        Vec3 dir = to.subtract(e.position());
        if (dir.lengthSqr() < 1.0E-4) return;
        push(e, dir.normalize().scale(strength).add(0, 0.2, 0));
    }

    static void lightning(ServerLevel level, Player p, Vec3 pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(pos);
        if (p instanceof ServerPlayer sp) bolt.setCause(sp);
        level.addFreshEntity(bolt);
    }

    static Vec3 teleportPos(Player p, double range) {
        HitResult hit = p.pick(range, 0, false);
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            return Vec3.atBottomCenterOf(bhr.getBlockPos().relative(bhr.getDirection()));
        }
        return hit.getLocation();
    }

    static void teleport(Player p, Vec3 pos) {
        p.teleportTo(pos.x, pos.y, pos.z);
        p.resetFallDistance();
    }

    /** Перематывает время вперёд до нужного момента суток (1000 = утро, 13000 = ночь). */
    static void setTimeOfDay(ServerLevel level, long target) {
        long now = level.getDayTime();
        long next = now - now % 24000 + target;
        if (next <= now) next += 24000;
        level.setDayTime(next);
    }

    // ---------- Эффекты частиц и звуки ----------

    static void burst(ServerLevel level, Vec3 pos, ParticleOptions type, int count, double spread) {
        level.sendParticles(type, pos.x, pos.y, pos.z, count, spread / 2, 0.5, spread / 2, 0.02);
    }

    static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions type) {
        Vec3 step = to.subtract(from);
        int n = Math.max(1, (int) (step.length() * 2));
        for (int i = 0; i <= n; i++) {
            Vec3 pt = from.add(step.scale(i / (double) n));
            level.sendParticles(type, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
        }
    }

    static void ring(ServerLevel level, Vec3 center, double r, ParticleOptions type) {
        int n = (int) (r * 12);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            level.sendParticles(type, center.x + Math.cos(a) * r, center.y + 0.2,
                    center.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    static void sound(ServerLevel level, Entity at, SoundEvent s, float pitch) {
        level.playSound(null, at.getX(), at.getY(), at.getZ(), s, SoundSource.PLAYERS, 1f, pitch);
    }

    private SpellUtil() {}
}