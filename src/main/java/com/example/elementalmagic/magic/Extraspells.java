package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.example.elementalmagic.magic.School.*;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

/**
 * Новые заклинания всех стихий. Вызывается из Spells после расчёта автоматических уровней,
 * поэтому уровни старых заклинаний не сдвигаются. У всех заклинаний здесь уровень задан явно.
 * Урон берётся из SpellInfo (метод d), чтобы число в описании и в бою совпадало.
 */
final class ExtraSpells {

    /** Секунды отката Backtrack для уровней I..V. */
    private static final int[] BACKTRACK_SECONDS = {3, 4, 5, 7, 10};

    static void init() {
        lightning();
        light();
        darkness();
        water();
        earth();
        air();
        fire();
        nature();
        ash();
        explosion();
        elemental();
        time();
        stars();
        darkNature();
        shadow();
    }

    private static float d(String id) {
        return SpellInfo.damage(id);
    }

    /** Точка прицеливания: существо, иначе блок, на который смотрит игрок. Null, если смотрит в пустоту. */
    private static Vec3 aimPoint(Player p, double range) {
        LivingEntity t = target(p, range);
        if (t != null) return t.position();
        HitResult hit = p.pick(range, 0, false);
        return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
    }

    // ============================== МОЛНИЯ ==============================
    private static void lightning() {
        reg("chain_lightning", STORM, 40, 100, 3, (level, p) -> {
            LivingEntity t = target(p, 25);
            if (t == null) return false;
            Set<LivingEntity> hit = new HashSet<>();
            LivingEntity cur = t;
            float dmg = d("chain_lightning");
            Vec3 from = p.getEyePosition();
            for (int i = 0; i < 5 && cur != null; i++) {
                hit.add(cur);
                hurt(p, cur, dmg);
                dmg *= 0.75f;
                line(level, from, mid(cur), ParticleTypes.ELECTRIC_SPARK);
                from = mid(cur);
                LivingEntity next = null;
                double best = Double.MAX_VALUE;
                for (LivingEntity e : around(level, p, cur.position(), 8)) {
                    if (hit.contains(e)) continue;
                    double dist = e.distanceToSqr(cur);
                    if (dist < best) {
                        best = dist;
                        next = e;
                    }
                }
                cur = next;
            }
            sound(level, t, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5f);
            return true;
        });

        reg("thunder_cloud", STORM, 45, 200, 4, (level, p) -> {
            Vec3 pos = aimPoint(p, 30);
            if (pos == null) return false;
            float dmg = d("thunder_cloud");
            for (int i = 0; i < 3; i++) {
                Scheduler.later(i * 20, bound(() -> {
                    burst(level, pos.add(0, 8, 0), ParticleTypes.CLOUD, 60, 4);
                    burst(level, pos.add(0, 8, 0), ParticleTypes.LARGE_SMOKE, 30, 3);
                    lightningVisual(level, pos);
                    for (LivingEntity e : around(level, p, pos, 3.5)) hurt(p, e, dmg);
                    burst(level, pos.add(0, 1, 0), ParticleTypes.ELECTRIC_SPARK, 30, 2);
                }));
            }
            return true;
        });
    }

    // ============================== СВЕТ ==============================
    private static void light() {
        reg("holy_bolt", HOLY, 15, 30, 1, (level, p) -> {
            LivingEntity t = targetAny(p, 25);
            if (t == null) return false;
            line(level, p.getEyePosition(), mid(t), ParticleTypes.END_ROD);
            if (t instanceof Enemy) {
                hurt(p, t, d("holy_bolt"));
            } else {
                t.heal(6f);
                burst(level, mid(t), ParticleTypes.HAPPY_VILLAGER, 15, 1);
            }
            sound(level, t, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f);
            return true;
        });

        reg("purification", HOLY, 30, 200, 3, (level, p) -> {
            for (LivingEntity e : friends(level, p, 6)) {
                cleanse(e);
                e.heal(6f);
                burst(level, mid(e), ParticleTypes.HAPPY_VILLAGER, 12, 1);
            }
            for (LivingEntity e : around(level, p, 8)) {
                if (!isUndead(e)) continue;
                hurt(p, e, d("purification"));
                e.igniteForSeconds(4);
            }
            ring(level, p.position(), 6, ParticleTypes.END_ROD);
            sound(level, p, SoundEvents.BEACON_ACTIVATE, 1.5f);
            return true;
        });
    }

    // ============================== ТЬМА ==============================
    private static void darkness() {
        reg("blood_pact", NECROMANCY, 30, 120, 3, (level, p) -> {
            if (p.getHealth() <= 6f) return false;
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            p.setHealth(p.getHealth() - 4f);
            burst(level, mid(p), ParticleTypes.DAMAGE_INDICATOR, 8, 1);
            float dealt = scaled(d("blood_pact"));
            hurt(p, t, d("blood_pact"));
            p.heal(dealt / 2f);
            line(level, mid(t), p.getEyePosition(), ParticleTypes.SOUL);
            burst(level, mid(t), ParticleTypes.DAMAGE_INDICATOR, 15, 1);
            sound(level, t, SoundEvents.SOUL_ESCAPE, 0.8f);
            return true;
        });

        reg("dark_touch", NECROMANCY, 12, 30, 1, (level, p) -> {
            LivingEntity t = target(p, 3.5);
            if (t == null) return false;
            float mult = level.isNight() ? 1.6f : 1f;
            hurt(p, t, d("dark_touch") * mult);
            burst(level, mid(t), ParticleTypes.SMOKE, 25, 1);
            burst(level, mid(t), ParticleTypes.SOUL, 10, 1);
            sound(level, t, SoundEvents.SOUL_ESCAPE, 1f);
            return true;
        });

        reg("shadow_bind", SHADOW, 25, 120, 2, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            int ticks = 80;
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, ticks, 6);
            effect(t, MobEffects.JUMP, ticks, 128);
            MagicEvents.freezeAi(t, ticks);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.SQUID_INK);
            burst(level, t.position().add(0, 0.1, 0), ParticleTypes.SQUID_INK, 30, 1);
            sound(level, t, SoundEvents.WARDEN_HEARTBEAT, 1f);
            return true;
        });
    }

    // ============================== ВОДА ==============================
    private static void water() {
        reg("tidal_wave", TIDE, 25, 80, 2, (level, p) -> {
            List<LivingEntity> list = beam(level, p, 12, 2.5);
            if (list.isEmpty()) return false;
            Vec3 look = p.getLookAngle();
            for (LivingEntity e : list) {
                hurt(p, e, d("tidal_wave"));
                push(e, look.scale(1.5).add(0, 0.35, 0));
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
                e.clearFire();
            }
            p.clearFire();
            line(level, p.getEyePosition(), beamEnd(p, 12), ParticleTypes.SPLASH);
            burst(level, beamEnd(p, 12), ParticleTypes.BUBBLE_POP, 60, 3);
            sound(level, p, SoundEvents.PLAYER_SPLASH, 0.7f);
            return true;
        });

        reg("water_shield", TIDE, 35, 400, 3, (level, p) -> {
            long until = level.getGameTime() + 200;
            for (LivingEntity e : friends(level, p, 6)) {
                MagicEvents.waterShield(e, until);
                effect(e, MobEffects.DAMAGE_RESISTANCE, 200, 1);
                e.clearFire();
            }
            ring(level, p.position(), 6, ParticleTypes.SPLASH);
            burst(level, mid(p), ParticleTypes.BUBBLE_POP, 60, 3);
            sound(level, p, SoundEvents.PLAYER_SPLASH, 1.3f);
            return true;
        });

        reg("suffocation", TIDE, 40, 300, 4, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null || t.canBreatheUnderwater()) return false;
            float dmg = d("suffocation");
            Scheduler.repeat(10, 8, bound(() -> {
                if (!t.isAlive()) return;
                t.setAirSupply(0);
                t.hurt(level.damageSources().drown(), scaled(dmg));
                burst(level, t.position().add(0, t.getBbHeight(), 0), ParticleTypes.BUBBLE, 12, 0.6);
            }));
            sound(level, t, SoundEvents.PLAYER_SPLASH, 0.6f);
            return true;
        });
    }

    // ============================== ЗЕМЛЯ ==============================
    private static void earth() {
        reg("stone_spike", STONE, 20, 60, 2, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, d("stone_spike"));
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 60, 2);
            Vec3 motion = t.getDeltaMovement();
            push(t, new Vec3(motion.x, 1.1, motion.z));
            burst(level, t.position(), blockParticle(Blocks.STONE), 50, 1.2);
            sound(level, t, SoundEvents.STONE_BREAK, 0.7f);
            return true;
        });

        reg("stone_bullet", STONE, 18, 30, 1, (level, p) -> {
            LivingEntity t = target(p, 40);
            if (t == null) return false;
            hurt(p, t, d("stone_bullet"));
            line(level, p.getEyePosition(), mid(t), blockParticle(Blocks.STONE));
            burst(level, mid(t), blockParticle(Blocks.COBBLESTONE), 20, 1);
            sound(level, p, SoundEvents.STONE_BREAK, 1.6f);
            return true;
        });

        reg("quicksand", STONE, 25, 160, 2, (level, p) -> {
            Vec3 center = aimPoint(p, 20);
            if (center == null) return false;
            Scheduler.repeat(10, 10, bound(() -> {
                for (LivingEntity e : enemies(level, p, center, 5)) {
                    effect(e, MobEffects.MOVEMENT_SLOWDOWN, 30, 3);
                    Vec3 m = e.getDeltaMovement();
                    e.setDeltaMovement(m.x * 0.5, m.y, m.z * 0.5);
                }
                level.sendParticles(blockParticle(Blocks.SAND), center.x, center.y + 0.1, center.z,
                        30, 2.5, 0.1, 2.5, 0.02);
            }));
            sound(level, p, SoundEvents.SAND_BREAK, 0.6f);
            return true;
        });

        reg("earthquake", STONE, 55, 600, 5, (level, p) -> {
            Scheduler.repeat(10, 5, bound(() -> {
                for (LivingEntity e : enemies(level, p, 9)) {
                    hurt(p, e, d("earthquake"));
                    effect(e, MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
                    if (e.onGround()) push(e, e.getDeltaMovement().add(0, 0.55, 0));
                }
                ring(level, p.position(), 9, blockParticle(Blocks.DIRT));
                burst(level, p.position(), blockParticle(Blocks.STONE), 50, 9);
                sound(level, p, SoundEvents.STONE_BREAK, 0.5f);
            }));
            return true;
        });
    }

    // ============================== ВОЗДУХ ==============================
    private static void air() {
        reg("gust", WIND, 12, 80, 1, (level, p) -> {
            Vec3 look = p.getLookAngle();
            push(p, new Vec3(look.x * 0.4, 1.2, look.z * 0.4));
            p.resetFallDistance();
            effect(p, MobEffects.SLOW_FALLING, 160, 0);
            for (LivingEntity e : enemies(level, p, 4)) {
                hurt(p, e, d("gust"));
                pushFrom(e, p.position(), 1.0, 0.3);
            }
            burst(level, p.position(), ParticleTypes.CLOUD, 40, 2);
            sound(level, p, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f);
            return true;
        });

        reg("repulsion", WIND, 25, 60, 2, (level, p) -> {
            List<LivingEntity> list = enemies(level, p, 7);
            if (list.isEmpty()) return false;
            for (LivingEntity e : list) {
                hurt(p, e, d("repulsion"));
                pushFrom(e, p.position(), 1.6, 0.4);
            }
            ring(level, p.position(), 7, ParticleTypes.CLOUD);
            sound(level, p, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f);
            return true;
        });

        reg("whirlwind", WIND, 30, 80, 3, (level, p) -> {
            List<LivingEntity> list = beam(level, p, 12, 2.5);
            if (list.isEmpty()) return false;
            Vec3 look = p.getLookAngle();
            for (LivingEntity e : list) {
                hurt(p, e, d("whirlwind"));
                push(e, look.scale(1.8).add(0, 0.3, 0));
            }
            line(level, p.getEyePosition(), beamEnd(p, 12), ParticleTypes.CLOUD);
            sound(level, p, SoundEvents.PLAYER_ATTACK_SWEEP, 1f);
            return true;
        });
    }

    // ============================== ОГОНЬ ==============================
    private static void fire() {
        reg("sacrificial_flame", FLAME, 30, 120, 3, (level, p) -> {
            if (p.getHealth() <= 6f) return false;
            LivingEntity t = target(p, 22);
            if (t == null) return false;
            p.setHealth(p.getHealth() - 4f);
            burst(level, mid(p), ParticleTypes.DAMAGE_INDICATOR, 8, 1);
            float ratio = p.getHealth() / p.getMaxHealth();
            float bonus = 1f + (1f - ratio) * 2.5f;
            for (LivingEntity e : around(level, p, t.position(), 3)) {
                hurt(p, e, d("sacrificial_flame") * bonus);
                e.igniteForSeconds(6);
            }
            line(level, p.getEyePosition(), mid(t), ParticleTypes.FLAME);
            burst(level, mid(t), ParticleTypes.FLAME, 50, 2);
            sound(level, p, SoundEvents.BLAZE_SHOOT, 0.8f);
            return true;
        });

        reg("fire_breath", FLAME, 35, 160, 4, (level, p) -> {
            float dmg = d("fire_breath");
            Scheduler.repeat(5, 8, bound(() -> {
                if (!p.isAlive()) return;
                for (LivingEntity e : beam(level, p, 9, 1.2)) {
                    hurt(p, e, dmg);
                    e.igniteForSeconds(5);
                }
                Vec3 end = beamEnd(p, 9);
                line(level, p.getEyePosition().add(p.getLookAngle()), end, ParticleTypes.FLAME);
                burst(level, end, ParticleTypes.FLAME, 15, 1.5);
                sound(level, p, SoundEvents.FIRE_AMBIENT, 1.5f);
            }));
            return true;
        });
    }

    // ============================== ПРИРОДА ==============================
    private static void nature() {
        reg("living_roots", FLORA, 22, 140, 2, (level, p) -> {
            LivingEntity t = target(p, 18);
            if (t == null) return false;
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 100, 6);
            effect(t, MobEffects.JUMP, 100, 128);
            effect(t, MobEffects.POISON, 100, 0);
            hurt(p, t, d("living_roots"));
            effect(p, MobEffects.REGENERATION, 80, 0);
            burst(level, t.position().add(0, 0.5, 0), ParticleTypes.HAPPY_VILLAGER, 40, 1.5);
            burst(level, t.position().add(0, 0.2, 0), blockParticle(Blocks.OAK_LEAVES), 30, 1.2);
            sound(level, t, SoundEvents.GRASS_BREAK, 0.8f);
            return true;
        });

        reg("summon_horse", BEAST, 30, 600, 2, (level, p) -> {
            Horse horse = EntityType.HORSE.create(level);
            if (horse == null) return false;
            horse.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0f);
            horse.finalizeSpawn(level, level.getCurrentDifficultyAt(p.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            horse.setTamed(true);
            horse.setOwnerUUID(p.getUUID());
            // Если IDE ругается на сигнатуру, в 1.21.1 это equipSaddle(ItemStack, SoundSource)
            horse.equipSaddle(new ItemStack(Items.SADDLE), SoundSource.PLAYERS);
            horse.setPersistenceRequired();
            level.addFreshEntity(horse);
            Minions.track(level, p, "horse", horse, 6000, 1);
            p.startRiding(horse);
            burst(level, horse.position().add(0, 1, 0), ParticleTypes.HAPPY_VILLAGER, 30, 1.5);
            sound(level, p, SoundEvents.HORSE_AMBIENT, 1f);
            return true;
        });

        reg("summon_wolves", BEAST, 40, 600, 3, (level, p) -> {
            for (int i = 0; i < 3; i++) {
                Wolf wolf = EntityType.WOLF.create(level);
                if (wolf == null) continue;
                double a = i * Math.PI * 2 / 3;
                wolf.moveTo(p.getX() + Math.cos(a) * 1.5, p.getY(), p.getZ() + Math.sin(a) * 1.5, p.getYRot(), 0f);
                wolf.finalizeSpawn(level, level.getCurrentDifficultyAt(p.blockPosition()),
                        MobSpawnType.MOB_SUMMONED, null);
                wolf.tame(p);
                var health = wolf.getAttribute(Attributes.MAX_HEALTH);
                if (health != null) health.setBaseValue(30);
                var attack = wolf.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attack != null) attack.setBaseValue(6);
                wolf.setHealth(wolf.getMaxHealth());
                level.addFreshEntity(wolf);
                Minions.track(level, p, "wolves", wolf, 1200, 3);
                burst(level, wolf.position().add(0, 0.5, 0), ParticleTypes.CLOUD, 15, 1);
            }
            sound(level, p, SoundEvents.WOLF_HOWL, 1f);
            return true;
        });
    }

    // ============================== ПЕПЕЛ ==============================
    private static void ash() {
        reg("ash_step", SMOKE, 20, 160, 2, (level, p) -> {
            effect(p, MobEffects.INVISIBILITY, 120, 0);
            effect(p, MobEffects.MOVEMENT_SPEED, 120, 1);
            for (LivingEntity e : enemies(level, p, 5)) effect(e, MobEffects.BLINDNESS, 40, 0);
            burst(level, mid(p), ParticleTypes.ASH, 120, 3);
            burst(level, mid(p), ParticleTypes.LARGE_SMOKE, 40, 2);
            sound(level, p, SoundEvents.FIRECHARGE_USE, 0.6f);
            return true;
        });

        reg("ash_squall", SMOKE, 30, 200, 3, (level, p) -> {
            if (enemies(level, p, 10).isEmpty()) return false;
            Scheduler.repeat(10, 4, bound(() -> {
                for (LivingEntity e : enemies(level, p, 10)) {
                    effect(e, MobEffects.BLINDNESS, 60, 0);
                    effect(e, MobEffects.MOVEMENT_SLOWDOWN, 60, 2);
                }
                burst(level, mid(p), ParticleTypes.ASH, 250, 10);
                burst(level, mid(p).add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 60, 8);
            }));
            sound(level, p, SoundEvents.FIRECHARGE_USE, 0.5f);
            return true;
        });
    }

    // ============================== ВЗРЫВЫ ==============================
    private static void explosion() {
        reg("rocket_jump", SHOCKWAVE, 20, 80, 2, (level, p) -> {
            Vec3 look = p.getLookAngle();
            Vec3 at = p.position();
            push(p, new Vec3(look.x * 1.2, 1.1, look.z * 1.2));
            p.resetFallDistance();
            effect(p, MobEffects.SLOW_FALLING, 120, 0);
            for (LivingEntity e : enemies(level, p, at, 4)) {
                hurt(p, e, d("rocket_jump"));
                pushFrom(e, at, 1.4, 0.4);
            }
            burst(level, at, ParticleTypes.EXPLOSION, 3, 1.5);
            burst(level, at, ParticleTypes.FLAME, 30, 2);
            sound(level, p, SoundEvents.GENERIC_EXPLODE, 1.2f);
            return true;
        });

        reg("grenade", DETONATION, 25, 100, 2, (level, p) -> {
            ItemEntity grenade = new ItemEntity(level, p.getX(), p.getEyeY() - 0.2, p.getZ(),
                    new ItemStack(Items.FIRE_CHARGE));
            grenade.setDeltaMovement(p.getLookAngle().scale(1.1).add(0, 0.2, 0));
            grenade.setNeverPickUp();
            level.addFreshEntity(grenade);
            Vec3[] last = {grenade.position()};
            float dmg = d("grenade");

            // Дымный след, пока граната летит
            Scheduler.repeat(2, 19, () -> {
                if (grenade.isRemoved()) return;
                last[0] = grenade.position();
                level.sendParticles(ParticleTypes.SMOKE, grenade.getX(), grenade.getY() + 0.2, grenade.getZ(),
                        2, 0.05, 0.05, 0.05, 0.01);
            });

            // Взрыв через 2 секунды
            Scheduler.later(40, bound(() -> {
                Vec3 c = grenade.isRemoved() ? last[0] : grenade.position();
                grenade.discard();
                for (LivingEntity e : around(level, p, c, 4.5)) {
                    hurt(p, e, dmg);
                    pushFrom(e, c, 1.2, 0.4);
                }
                burst(level, c, ParticleTypes.EXPLOSION_EMITTER, 1, 0);
                burst(level, c, ParticleTypes.FLAME, 40, 3);
                sound(level, grenade, SoundEvents.GENERIC_EXPLODE, 1f);
            }));
            sound(level, p, SoundEvents.FIRECHARGE_USE, 1f);
            return true;
        });
    }

    // ============================== ВСЕ СТИХИИ ==============================
    private static void elemental() {
        reg("elemental_resonance", FUSION, 50, 200, 4, (level, p) -> {
            LivingEntity t = target(p, 25);
            if (t == null) return false;
            hurt(p, t, d("elemental_resonance"));
            t.igniteForSeconds(4);                              // огонь
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 100, 1);    // вода и лёд
            effect(t, MobEffects.WEAKNESS, 100, 0);             // земля
            effect(t, MobEffects.POISON, 100, 0);               // природа
            line(level, p.getEyePosition(), mid(t), ParticleTypes.END_ROD);
            burst(level, mid(t), ParticleTypes.FLAME, 20, 1.5);
            burst(level, mid(t), ParticleTypes.SNOWFLAKE, 20, 1.5);
            burst(level, mid(t), ParticleTypes.ELECTRIC_SPARK, 20, 1.5);
            burst(level, mid(t), ParticleTypes.HAPPY_VILLAGER, 20, 1.5);
            sound(level, t, SoundEvents.EVOKER_CAST_SPELL, 1f);
            return true;
        });
    }

    // ============================== ВРЕМЯ ==============================
    private static void time() {
        reg("time_stop", STASIS, 80, 1200, 5, (level, p) -> {
            List<LivingEntity> list = around(level, p, 12);
            if (list.isEmpty()) return false;
            int ticks = 80;
            for (LivingEntity e : list) {
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, ticks, 6);
                effect(e, MobEffects.JUMP, ticks, 128);
                effect(e, MobEffects.DIG_SLOWDOWN, ticks, 4);
                effect(e, MobEffects.WEAKNESS, ticks, 3);
                MagicEvents.freezeAi(e, ticks);
                burst(level, mid(e), ParticleTypes.ENCHANT, 15, 1);
            }
            effect(p, MobEffects.MOVEMENT_SPEED, ticks, 1);
            effect(p, MobEffects.DIG_SPEED, ticks, 2);
            ring(level, p.position(), 12, ParticleTypes.END_ROD);
            sound(level, p, SoundEvents.BEACON_DEACTIVATE, 0.6f);
            return true;
        });

        // Время отката зависит от уровня заклинания: I=3, II=4, III=5, IV=7, V=10 секунд.
        // Чтобы изменить время, поменяй число уровня (пятый параметр) в reg ниже.
        reg("backtrack", STASIS, 35, 400, 3, (level, p) -> {
            if (!(p instanceof ServerPlayer sp)) return false;
            int tier = Spells.tierOf("backtrack");
            int seconds = BACKTRACK_SECONDS[Mth.clamp(tier, 1, 5) - 1];
            Vec3 from = sp.position().add(0, 1, 0);
            if (!MagicEvents.rewind(sp, seconds)) return false;
            burst(level, from, ParticleTypes.REVERSE_PORTAL, 60, 1.5);
            burst(level, sp.position().add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 60, 1.5);
            sound(level, sp, SoundEvents.ENDERMAN_TELEPORT, 0.6f);
            return true;
        });
    }

    // ============================== ЗВЁЗДЫ ==============================
    private static void stars() {
        reg("constellation", CONSTELLATION, 35, 100, 3, (level, p) -> {
            List<LivingEntity> pool = new ArrayList<>(enemies(level, p, 22));
            pool.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
            LivingEntity primary = target(p, 25);
            if (primary != null) {
                pool.remove(primary);
                pool.add(0, primary);
            }
            if (pool.isEmpty()) return false;
            float dmg = d("constellation");
            for (int i = 0; i < 5; i++) {
                LivingEntity t = pool.get(i % pool.size());
                Scheduler.later(i * 3, bound(() -> {
                    if (!t.isAlive()) return;
                    hurt(p, t, dmg);
                    line(level, p.getEyePosition(), mid(t), ParticleTypes.END_ROD);
                    burst(level, mid(t), ParticleTypes.FIREWORK, 12, 1);
                    sound(level, t, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6f);
                }));
            }
            return true;
        });

        reg("starfall", COSMOS, 55, 400, 4, (level, p) -> {
            List<LivingEntity> pool = new ArrayList<>(enemies(level, p, 20));
            LivingEntity primary = target(p, 25);
            if (primary != null && !pool.contains(primary)) pool.add(primary);
            if (pool.isEmpty()) return false;
            float dmg = d("starfall");
            for (int i = 0; i < 5; i++) {
                LivingEntity t = pool.get(level.random.nextInt(pool.size()));
                Scheduler.later(i * 6, () -> {
                    Vec3 at = t.position();
                    line(level, at.add(0, 12, 0), at, ParticleTypes.END_ROD);
                });
                Scheduler.later(i * 6 + 8, bound(() -> {
                    Vec3 at = t.position();
                    for (LivingEntity e : around(level, p, at, 3)) hurt(p, e, dmg);
                    burst(level, at.add(0, 0.5, 0), ParticleTypes.FIREWORK, 40, 2);
                    sound(level, t, SoundEvents.FIREWORK_ROCKET_BLAST, 1f);
                }));
            }
            return true;
        });

        reg("supernova", COSMOS, 90, 1800, 5, (level, p) -> {
            float dmg = d("supernova");
            // Зарядка: расширяющиеся кольца 2 секунды
            for (int i = 1; i <= 8; i++) {
                double r = i * 2.5;
                Scheduler.later(i * 5, () -> {
                    ring(level, p.position(), r, ParticleTypes.END_ROD);
                    burst(level, mid(p), ParticleTypes.END_ROD, 25, 3);
                });
            }
            sound(level, p, SoundEvents.BEACON_ACTIVATE, 0.5f);
            // Взрыв: бьёт всех, кроме тебя, питомцев и призванных существ. Блоки не ломает.
            Scheduler.later(45, bound(() -> {
                Vec3 c = p.position();
                for (LivingEntity e : around(level, p, c, 20)) {
                    hurt(p, e, dmg);
                    pushFrom(e, c, 1.6, 0.7);
                }
                burst(level, c.add(0, 1, 0), ParticleTypes.FLASH, 3, 1);
                burst(level, c.add(0, 2, 0), ParticleTypes.EXPLOSION_EMITTER, 12, 14);
                burst(level, c.add(0, 2, 0), ParticleTypes.END_ROD, 400, 20);
                ring(level, c, 20, ParticleTypes.END_ROD);
                sound(level, p, SoundEvents.GENERIC_EXPLODE, 0.5f);
            }));
            return true;
        });
    }

    // ============================== ТЁМНАЯ ПРИРОДА ==============================
    private static void darkNature() {
        reg("plague_cloud", ROT, 40, 300, 3, (level, p) -> {
            Vec3 center = p.position();
            float dmg = d("plague_cloud");
            Scheduler.repeat(20, 4, bound(() -> {
                for (LivingEntity e : enemies(level, p, center, 8)) {
                    hurt(p, e, dmg);
                    effect(e, MobEffects.WITHER, 60, 0);
                    effect(e, MobEffects.HUNGER, 100, 1);
                    effect(e, MobEffects.POISON, 60, 0);
                }
                burst(level, center.add(0, 1, 0), ParticleTypes.SNEEZE, 80, 8);
                ring(level, center, 8, ParticleTypes.SPORE_BLOSSOM_AIR);
            }));
            sound(level, p, SoundEvents.EVOKER_CAST_SPELL, 0.6f);
            return true;
        });

        reg("summon_undead", ROT, 50, 600, 4, (level, p) -> {
            for (int i = 0; i < 4; i++) {
                if (i % 2 == 0) {
                    Minions.combat(level, p, EntityType.ZOMBIE, 900, "undead", 4, false);
                } else {
                    Minions.combat(level, p, EntityType.SKELETON, 900, "undead", 4, false);
                }
            }
            sound(level, p, SoundEvents.ZOMBIE_AMBIENT, 0.7f);
            return true;
        });

        reg("poison_roots", THORN, 35, 200, 3, (level, p) -> {
            Vec3 center = aimPoint(p, 22);
            if (center == null) return false;
            float dmg = d("poison_roots");
            Scheduler.repeat(10, 6, bound(() -> {
                for (LivingEntity e : enemies(level, p, center, 5)) {
                    hurt(p, e, dmg);
                    effect(e, MobEffects.POISON, 60, 1);
                    effect(e, MobEffects.MOVEMENT_SLOWDOWN, 30, 5);
                    effect(e, MobEffects.JUMP, 30, 128);
                }
                ring(level, center, 5, ParticleTypes.SPORE_BLOSSOM_AIR);
                burst(level, center.add(0, 0.5, 0), blockParticle(Blocks.MANGROVE_ROOTS), 30, 4);
            }));
            sound(level, p, SoundEvents.GRASS_BREAK, 0.6f);
            return true;
        });
    }

    // ============================== ТЕНИ ==============================
    private static void shadow() {
        reg("shadow_step", STEALTH, 25, 160, 2, (level, p) -> {
            LivingEntity t = target(p, 25);
            if (t == null) return false;

            Vec3 facing = t.getLookAngle();
            Vec3 flat = new Vec3(facing.x, 0, facing.z);
            if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(0, 0, 1);
            flat = flat.normalize();

            Vec3 dest = null;
            for (double dist : new double[]{1.6, 1.1, 0.8}) {
                Vec3 c = t.position().subtract(flat.scale(dist));
                if (level.noCollision(p, p.getBoundingBox().move(c.subtract(p.position())))) {
                    dest = c;
                    break;
                }
            }
            if (dest == null) return false;

            Vec3 from = p.position();
            double dx = t.getX() - dest.x;
            double dz = t.getZ() - dest.z;
            float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
            if (p instanceof ServerPlayer sp) {
                sp.connection.teleport(dest.x, dest.y, dest.z, yaw, 0f);
            } else {
                teleport(p, dest);
            }
            p.resetFallDistance();
            hurt(p, t, d("shadow_step"));
            burst(level, from.add(0, 1, 0), ParticleTypes.SQUID_INK, 30, 1);
            burst(level, dest.add(0, 1, 0), ParticleTypes.SQUID_INK, 30, 1);
            sound(level, p, SoundEvents.ENDERMAN_TELEPORT, 1.2f);
            return true;
        });

        reg("shadow_grip", STEALTH, 45, 400, 4, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            int ticks = 120;
            MagicEvents.freezeAi(t, ticks);     // моб сам не действует, только повторяет за тобой
            Vec3[] last = {p.position()};
            boolean[] done = {false};
            for (int i = 1; i <= ticks; i++) {
                Scheduler.later(i, () -> gripTick(level, p, t, last, done));
            }
            line(level, p.getEyePosition(), mid(t), ParticleTypes.SQUID_INK);
            sound(level, t, SoundEvents.WARDEN_HEARTBEAT, 1.2f);
            return true;
        });

        reg("summon_shadows", STEALTH, 70, 900, 5, (level, p) -> {
            for (int i = 0; i < 3; i++) {
                Minions.combat(level, p, EntityType.ZOMBIE, 900, "shadows", 3, true);
            }
            burst(level, p.position().add(0, 1, 0), ParticleTypes.SQUID_INK, 60, 3);
            sound(level, p, SoundEvents.ENDERMAN_TELEPORT, 0.5f);
            return true;
        });
    }

    /** Один тик Захвата: цель повторяет горизонтальное движение хозяина (и прыжок). */
    private static void gripTick(ServerLevel level, Player p, LivingEntity t, Vec3[] last, boolean[] done) {
        if (done[0]) return;
        if (!p.isAlive() || !t.isAlive() || t.level() != p.level() || p.distanceToSqr(t) > 40 * 40) {
            done[0] = true;
            return;
        }
        Vec3 now = p.position();
        Vec3 delta = now.subtract(last[0]);
        last[0] = now;

        boolean jumped = delta.y > 0.2 && t.onGround();
        if (Math.abs(delta.x) > 1.0E-4 || Math.abs(delta.z) > 1.0E-4) {
            if (t instanceof ServerPlayer target) {
                // Игроков двигаем телепортом, иначе клиент вернёт их на место
                var box = target.getBoundingBox().move(delta.x, 0, delta.z);
                if (level.noCollision(target, box)) {
                    target.connection.teleport(target.getX() + delta.x, target.getY(), target.getZ() + delta.z,
                            target.getYRot(), target.getXRot());
                }
            } else {
                t.move(MoverType.SELF, new Vec3(delta.x, 0, delta.z));
            }
        }
        if (jumped) {
            Vec3 m = t.getDeltaMovement();
            push(t, new Vec3(m.x, 0.42, m.z));
        }
        if (t.tickCount % 2 == 0) {
            burst(level, t.position().add(0, 0.1, 0), ParticleTypes.SQUID_INK, 3, 0.8);
        }
    }

    private ExtraSpells() {}
}
