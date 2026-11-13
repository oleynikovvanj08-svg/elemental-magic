package com.example.elementalmagic.magic;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.example.elementalmagic.magic.School.*;
import static com.example.elementalmagic.magic.SpellUtil.*;

/**
 * Заклинания магических мечей: по три на меч. Эти заклинания НЕ входят в общий список Spells.ALL,
 * поэтому для них нет свитков и их нельзя записать в посох или книгу.
 * Заклинания «Зыбучие пески» (меч земли) и «Backtrack» (меч времени) берутся из общего списка.
 *
 * Мечи есть у стихий: молния, вода, земля, воздух, огонь, природа, время, звёзды, тёмная природа.
 */
public final class SwordSpells {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Set<Element> SWORD_ELEMENTS = EnumSet.of(
            Element.LIGHTNING, Element.WATER, Element.EARTH, Element.AIR, Element.FIRE,
            Element.NATURE, Element.TIME, Element.STARS, Element.DARK_NATURE);

    private static final Map<Element, List<Spell>> BY_ELEMENT = new EnumMap<>(Element.class);
    private static final Map<String, Float> DAMAGE = new HashMap<>();
    private static boolean built = false;

    static {
        // Урон за одно попадание. Здесь же можно подкрутить баланс.
        DAMAGE.put("sword_storm_dash", 8f);
        DAMAGE.put("sword_static_charge", 2f);       // бонус к каждому удару меча, цепь бьёт вполовину
        DAMAGE.put("sword_storm_wrath", 12f);        // за каждую из 5 молний
        DAMAGE.put("sword_water_crescent", 5f);
        DAMAGE.put("sword_whirlpool", 3f);           // за каждый из 3 импульсов
        DAMAGE.put("sword_stone_strike", 8f);
        DAMAGE.put("sword_flame_slash", 7f);
        DAMAGE.put("sword_blazing_edge", 3f);        // бонус к каждому удару меча
        DAMAGE.put("sword_flame_whirl", 10f);
        DAMAGE.put("sword_wind_slash", 7f);
        DAMAGE.put("sword_thorn_thrust", 7f);
        DAMAGE.put("sword_orbit", 5f);               // за каждый импульс орбиты
        DAMAGE.put("sword_star_pull", 14f);          // финальный взрыв
        DAMAGE.put("sword_infecting_strike", 5f);
        DAMAGE.put("sword_infecting_pulse", 2f);     // за каждый импульс заражения
        DAMAGE.put("sword_life_devour", 10f);
        DAMAGE.put("sword_rot_shroud", 2f);          // за каждый импульс ауры
    }

    private static float d(String id) {
        return DAMAGE.getOrDefault(id, 0f);
    }

    // ====================== Публичный интерфейс ======================

    public static boolean hasSword(Element e) {
        return SWORD_ELEMENTS.contains(e);
    }

    /** Три заклинания меча этой стихии. Пустой список, если меча у стихии нет. */
    public static synchronized List<Spell> spellsOf(Element element) {
        ensureBuilt();
        return BY_ELEMENT.getOrDefault(element, List.of());
    }

    /** Урон заклинания меча с учётом уровня (для подсказки). 0, если урона нет. */
    public static float damageOf(Spell spell) {
        return d(spell.id()) * spell.tierMultiplier();
    }

    /** Описание пассивного эффекта меча или null, если пассивки нет. */
    public static String passiveText(Element e) {
        return switch (e) {
            case LIGHTNING -> "Скорость II, пока меч в руке";
            case WATER -> "Мгновенно тушит тебя при поджоге (лава всё равно ранит)";
            case EARTH -> "Сопротивление урону, пока меч в руке";
            case AIR -> "Медленное падение, пока меч в руке";
            case FIRE -> "Меч всегда поджигает врагов";
            case NATURE -> "Восстанавливает голод, пока меч в руке";
            default -> null;
        };
    }

    // ====================== Пассивные эффекты ======================

    /** Вызывается каждый тик для игрока, держащего меч этой стихии. */
    public static void tickPassive(Player p, Element element) {
        switch (element) {
            case LIGHTNING -> refresh(p, MobEffects.MOVEMENT_SPEED, 1);
            case WATER -> {
                if (p.isOnFire()) p.clearFire();
            }
            case EARTH -> refresh(p, MobEffects.DAMAGE_RESISTANCE, 0);
            case AIR -> refresh(p, MobEffects.SLOW_FALLING, 0);
            case NATURE -> {
                if (p.tickCount % 40 == 0 && p.getFoodData().needsFood()) p.getFoodData().eat(1, 0.2f);
            }
            default -> { }
        }
    }

    /** Невидимый эффект на 1.5 секунды, обновляется каждые полсекунды, пока меч в руке. */
    private static void refresh(Player p, Holder<MobEffect> effect, int amplifier) {
        if (p.tickCount % 10 != 0) return;
        p.addEffect(new MobEffectInstance(effect, 30, amplifier, true, false, false));
    }

    // ====================== Баффы (Статический заряд, Раскалённый клинок, Кора, Эхо) ======================

    private enum Buff { STATIC_CHARGE, BLAZING_EDGE, THORNY_BARK, ECHO }

    private record ActiveBuff(long until, float mult) {}

    private static final Map<UUID, Map<Buff, ActiveBuff>> BUFFS = new HashMap<>();

    /** mult запоминается в момент каста, чтобы бонусы посоха/уровня работали и позже. */
    private static void give(Player p, Buff buff, int ticks, float mult) {
        BUFFS.computeIfAbsent(p.getUUID(), k -> new EnumMap<>(Buff.class))
                .put(buff, new ActiveBuff(p.level().getGameTime() + ticks, mult));
    }

    private static ActiveBuff active(Player p, Buff buff) {
        Map<Buff, ActiveBuff> map = BUFFS.get(p.getUUID());
        if (map == null) return null;
        ActiveBuff a = map.get(buff);
        if (a == null) return null;
        if (p.level().getGameTime() > a.until()) {
            map.remove(buff);
            return null;
        }
        return a;
    }

    private static void removeBuff(Player p, Buff buff) {
        Map<Buff, ActiveBuff> map = BUFFS.get(p.getUUID());
        if (map != null) map.remove(buff);
    }

    static void clear(UUID id) {
        BUFFS.remove(id);
    }

    static void clearAll() {
        BUFFS.clear();
    }

    /** Дополнительный удар: сбрасывает неуязвимость после удара мечом, иначе бонус «съедается». */
    private static void bonusHit(Player p, LivingEntity target, float amount) {
        target.invulnerableTime = 0;
        hurt(p, target, amount);
    }

    /** Вызывается из ElementalSwordItem.hurtEnemy после обычного удара мечом. */
    public static void onMeleeHit(Player p, LivingEntity target, Element element) {
        if (!(p.level() instanceof ServerLevel level)) return;

        // Пассивка меча огня: всегда поджигает
        if (element == Element.FIRE) target.igniteForSeconds(5);

        if (element == Element.LIGHTNING) {
            ActiveBuff buff = active(p, Buff.STATIC_CHARGE);
            if (buff != null) {
                withDamageMultiplier(buff.mult(), () -> {
                    float bonus = d("sword_static_charge");
                    bonusHit(p, target, bonus);
                    burst(level, mid(target), ParticleTypes.ELECTRIC_SPARK, 12, 1);
                    LivingEntity next = null;
                    double best = Double.MAX_VALUE;
                    for (LivingEntity e : around(level, p, target.position(), 5)) {
                        if (e == target) continue;
                        double dist = e.distanceToSqr(target);
                        if (dist < best) {
                            best = dist;
                            next = e;
                        }
                    }
                    if (next != null) {
                        bonusHit(p, next, bonus * 0.5f);
                        line(level, mid(target), mid(next), ParticleTypes.ELECTRIC_SPARK);
                    }
                    return true;
                });
            }
        } else if (element == Element.FIRE) {
            ActiveBuff buff = active(p, Buff.BLAZING_EDGE);
            if (buff != null) {
                withDamageMultiplier(buff.mult(), () -> {
                    bonusHit(p, target, d("sword_blazing_edge"));
                    burst(level, mid(target), ParticleTypes.FLAME, 12, 1);
                    return true;
                });
            }
        } else if (element == Element.TIME) {
            ActiveBuff echo = active(p, Buff.ECHO);
            if (echo != null) {
                removeBuff(p, Buff.ECHO);
                float echoDamage = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
                Scheduler.later(20, () -> withDamageMultiplier(echo.mult(), () -> {
                    if (!target.isAlive()) return true;
                    bonusHit(p, target, echoDamage);
                    burst(level, mid(target), ParticleTypes.REVERSE_PORTAL, 25, 1);
                    sound(level, target, SoundEvents.ENDERMAN_TELEPORT, 1.5f);
                    return true;
                }));
            }
        }
    }

    /** Шипастая кора: часть урона от ближней атаки возвращается атакующему. */
    public static void onPlayerDamaged(Player p, DamageSource source, float amount) {
        ActiveBuff buff = active(p, Buff.THORNY_BARK);
        if (buff == null) return;
        if (source.is(DamageTypes.THORNS)) return;
        Entity direct = source.getDirectEntity();
        if (!(direct instanceof LivingEntity attacker) || attacker == p || direct != source.getEntity()) return;
        float reflect = Math.max(2f, amount * 0.4f);
        attacker.hurt(p.damageSources().thorns(p), reflect);
        if (p.level() instanceof ServerLevel level) {
            burst(level, mid(attacker), blockParticle(Blocks.OAK_LEAVES), 12, 1);
        }
    }

    // ====================== Вспомогательные функции ======================

    /** Горизонтальное направление взгляда или null, если игрок смотрит строго вверх или вниз. */
    private static Vec3 flatLook(Player p) {
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) return null;
        return flat.normalize();
    }

    /** Враги в коридоре перед игроком (длина и полуширина по горизонтали). */
    private static List<LivingEntity> corridor(ServerLevel level, Player p, double length, double halfWidth) {
        Vec3 dir = flatLook(p);
        if (dir == null) return List.of();
        Vec3 origin = p.position();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : enemies(level, p, origin.add(dir.scale(length / 2)), length / 2 + halfWidth)) {
            Vec3 rel = e.position().subtract(origin);
            double along = rel.x * dir.x + rel.z * dir.z;
            if (along < -0.5 || along > length) continue;
            double perpX = rel.x - dir.x * along;
            double perpZ = rel.z - dir.z * along;
            if (Math.sqrt(perpX * perpX + perpZ * perpZ) > halfWidth) continue;
            out.add(e);
        }
        return out;
    }

    /** Враги в секторе перед игроком. minDot = косинус половины угла (0.5 это 60 градусов в каждую сторону). */
    private static List<LivingEntity> cone(ServerLevel level, Player p, double range, double minDot) {
        Vec3 dir = flatLook(p);
        if (dir == null) return List.of();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : enemies(level, p, range)) {
            Vec3 rel = e.position().subtract(p.position());
            Vec3 flat = new Vec3(rel.x, 0, rel.z);
            if (flat.lengthSqr() < 1.0E-4 || flat.normalize().dot(dir) >= minDot) out.add(e);
        }
        return out;
    }

    /** Дуга частиц перед игроком. */
    private static void arc(ServerLevel level, Player p, double radius, double halfAngle, ParticleOptions type) {
        Vec3 dir = flatLook(p);
        if (dir == null) return;
        double base = Math.atan2(dir.z, dir.x);
        int n = 18;
        for (int i = 0; i <= n; i++) {
            double a = base - halfAngle + (2 * halfAngle) * i / n;
            level.sendParticles(type, p.getX() + Math.cos(a) * radius, p.getY() + 1.0,
                    p.getZ() + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** Точка прицеливания: существо, иначе блок. Null, если игрок смотрит в пустоту. */
    private static Vec3 aimPoint(Player p, double range) {
        LivingEntity t = target(p, range);
        if (t != null) return t.position();
        HitResult hit = p.pick(range, 0, false);
        return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
    }

    private static double distToSegment(Vec3 pt, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        if (len2 < 1.0E-6) return pt.distanceTo(a);
        double t = Mth.clamp(pt.subtract(a).dot(ab) / len2, 0, 1);
        return pt.distanceTo(a.add(ab.scale(t)));
    }

    private static Spell make(String id, School school, int mana, int cooldownTicks, int tier,
                              Spell.CastAction action) {
        SpellInfo.setTier(id, tier);
        return new Spell(id, school, mana, cooldownTicks, action);
    }

    private static Spell shared(String id) {
        Spell s = Spells.byId(id);
        if (s == null) LOGGER.warn("[ElementalMagic] Для меча не найдено общее заклинание '{}'", id);
        return s;
    }

    private static List<Spell> listOf(Spell... spells) {
        List<Spell> out = new ArrayList<>();
        for (Spell s : spells) if (s != null) out.add(s);
        return List.copyOf(out);
    }

    // ====================== Сами заклинания ======================

    private static void ensureBuilt() {
        if (built) return;
        built = true;

        // ---------- Молния: Грозовой клинок ----------
        Spell stormDash = make("sword_storm_dash", STORM, 20, 80, 2, (level, p) -> {
            Vec3 look = p.getLookAngle();
            Vec3 dir = new Vec3(look.x, Mth.clamp(look.y, -0.2, 0.3), look.z).normalize();
            Vec3 start = p.position();
            double dist = 0;
            for (double step = 7; step >= 2; step -= 1) {
                if (level.noCollision(p, p.getBoundingBox().move(dir.scale(step)))) {
                    dist = step;
                    break;
                }
            }
            if (dist < 2) return false;
            Vec3 end = start.add(dir.scale(dist));
            float dmg = d("sword_storm_dash");
            Vec3 a = start.add(0, 1, 0);
            Vec3 b = end.add(0, 1, 0);
            for (LivingEntity e : around(level, p, a.add(b).scale(0.5), dist / 2 + 2)) {
                if (distToSegment(mid(e), a, b) <= 1.8) {
                    hurt(p, e, dmg);
                    burst(level, mid(e), ParticleTypes.ELECTRIC_SPARK, 15, 1);
                }
            }
            if (p instanceof ServerPlayer sp) {
                sp.connection.teleport(end.x, end.y, end.z, sp.getYRot(), sp.getXRot());
            } else {
                teleport(p, end);
            }
            p.resetFallDistance();
            line(level, a, b, ParticleTypes.ELECTRIC_SPARK);
            sound(level, p, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.4f);
            return true;
        });

        Spell staticCharge = make("sword_static_charge", STORM, 25, 200, 3, (level, p) -> {
            give(p, Buff.STATIC_CHARGE, 160, scaled(1f));
            burst(level, mid(p), ParticleTypes.ELECTRIC_SPARK, 40, 1.5);
            ring(level, p.position(), 1.5, ParticleTypes.ELECTRIC_SPARK);
            sound(level, p, SoundEvents.BEACON_POWER_SELECT, 1.6f);
            return true;
        });

        Spell stormWrath = make("sword_storm_wrath", STORM, 50, 500, 5, (level, p) -> {
            if (enemies(level, p, 9).isEmpty()) return false;
            float dmg = d("sword_storm_wrath");
            Vec3 center = p.position();
            for (int i = 0; i < 5; i++) {
                double angle = i * Math.PI * 2 / 5;
                Vec3 pos = center.add(Math.cos(angle) * 4.5, 0, Math.sin(angle) * 4.5);
                Scheduler.later(i * 4, bound(() -> {
                    lightningVisual(level, pos);
                    for (LivingEntity e : around(level, p, pos, 3.5)) {
                        hurt(p, e, dmg);
                        effect(e, MobEffects.MOVEMENT_SLOWDOWN, 40, 6);
                        MagicEvents.freezeAi(e, 40);
                    }
                    burst(level, pos.add(0, 1, 0), ParticleTypes.ELECTRIC_SPARK, 30, 2);
                }));
            }
            ring(level, center, 4.5, ParticleTypes.ELECTRIC_SPARK);
            burst(level, center, blockParticle(Blocks.DIRT), 40, 3);
            sound(level, p, SoundEvents.STONE_BREAK, 0.5f);
            return true;
        });

        // ---------- Вода: Приливной клинок ----------
        Spell waterCrescent = make("sword_water_crescent", TIDE, 15, 40, 1, (level, p) -> {
            List<LivingEntity> list = cone(level, p, 6, 0.5);
            if (list.isEmpty()) return false;
            Vec3 dir = flatLook(p);
            for (LivingEntity e : list) {
                hurt(p, e, d("sword_water_crescent"));
                push(e, dir.scale(1.3).add(0, 0.25, 0));
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
                e.clearFire();
            }
            arc(level, p, 3, Math.PI / 3, ParticleTypes.SPLASH);
            arc(level, p, 5, Math.PI / 3, ParticleTypes.BUBBLE_POP);
            sound(level, p, SoundEvents.PLAYER_SPLASH, 1.2f);
            return true;
        });

        Spell whirlpool = make("sword_whirlpool", TIDE, 28, 160, 3, (level, p) -> {
            if (enemies(level, p, 7).isEmpty()) return false;
            float dmg = d("sword_whirlpool");
            Scheduler.repeat(10, 3, bound(() -> {
                Vec3 c = p.position();
                for (LivingEntity e : enemies(level, p, c, 7)) {
                    hurt(p, e, dmg);
                    pullTo(e, c, 1.0);
                }
                ring(level, c, 7, ParticleTypes.SPLASH);
                ring(level, c, 4, ParticleTypes.BUBBLE_POP);
                sound(level, p, SoundEvents.PLAYER_SPLASH, 0.6f);
            }));
            return true;
        });

        Spell healingTide = make("sword_healing_tide", TIDE, 35, 300, 4, (level, p) -> {
            for (LivingEntity e : friends(level, p, 8)) {
                cleanse(e);
                e.heal(8f);
                effect(e, MobEffects.REGENERATION, 100, 0);
                burst(level, mid(e), ParticleTypes.SPLASH, 20, 1);
            }
            ring(level, p.position(), 8, ParticleTypes.SPLASH);
            sound(level, p, SoundEvents.PLAYER_SPLASH, 1.4f);
            return true;
        });

        // ---------- Земля: Каменный клинок ----------
        Spell stoneStrike = make("sword_stone_strike", STONE, 18, 60, 2, (level, p) -> {
            List<LivingEntity> list = corridor(level, p, 8, 1.6);
            if (list.isEmpty()) return false;
            for (LivingEntity e : list) {
                hurt(p, e, d("sword_stone_strike"));
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
                push(e, e.getDeltaMovement().add(0, 0.9, 0));
            }
            Vec3 dir = flatLook(p);
            for (int s = 1; s <= 8; s++) {
                burst(level, p.position().add(dir.scale(s)).add(0, 0.2, 0), blockParticle(Blocks.STONE), 12, 1);
            }
            sound(level, p, SoundEvents.STONE_BREAK, 0.6f);
            return true;
        });

        Spell stoneSkin = make("sword_stone_skin", STONE, 25, 300, 3, (level, p) -> {
            effect(p, MobEffects.DAMAGE_RESISTANCE, 160, 2);
            effect(p, MobEffects.MOVEMENT_SLOWDOWN, 160, 0);
            burst(level, mid(p), blockParticle(Blocks.STONE), 50, 1.5);
            ring(level, p.position(), 1.5, blockParticle(Blocks.COBBLESTONE));
            sound(level, p, SoundEvents.STONE_BREAK, 0.5f);
            return true;
        });

        // ---------- Воздух: Клинок ветров ----------
        Spell windSlash = make("sword_wind_slash", WIND, 15, 40, 1, (level, p) -> {
            List<LivingEntity> list = beam(level, p, 14, 1.2);
            if (list.isEmpty()) return false;
            Vec3 look = p.getLookAngle();
            for (LivingEntity e : list) {
                hurt(p, e, d("sword_wind_slash"));
                push(e, look.scale(0.6).add(0, 0.2, 0));
            }
            Vec3 end = beamEnd(p, 14);
            line(level, p.getEyePosition().add(look), end, ParticleTypes.CLOUD);
            burst(level, end, ParticleTypes.SWEEP_ATTACK, 3, 1);
            sound(level, p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f);
            return true;
        });

        Spell windDash = make("sword_wind_dash", WIND, 15, 80, 2, (level, p) -> {
            Vec3 dir = flatLook(p);
            if (dir == null) return false;
            push(p, dir.scale(2.0).add(0, 0.2, 0));
            p.resetFallDistance();
            effect(p, MobEffects.MOVEMENT_SPEED, 40, 1);
            burst(level, mid(p), ParticleTypes.CLOUD, 30, 1.5);
            sound(level, p, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.3f);
            return true;
        });

        Spell repulsion = make("sword_repulsion", WIND, 25, 120, 3, (level, p) -> {
            List<LivingEntity> list = around(level, p, 8);
            if (list.isEmpty()) return false;
            for (LivingEntity e : list) pushFrom(e, p.position(), 1.8, 0.45);
            ring(level, p.position(), 8, ParticleTypes.CLOUD);
            ring(level, p.position(), 4, ParticleTypes.CLOUD);
            burst(level, mid(p), ParticleTypes.CLOUD, 40, 2);
            sound(level, p, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8f);
            return true;
        });

        // ---------- Огонь: Клинок пламени ----------
        Spell flameSlash = make("sword_flame_slash", FLAME, 15, 40, 1, (level, p) -> {
            List<LivingEntity> list = cone(level, p, 5, 0.34);
            if (list.isEmpty()) return false;
            for (LivingEntity e : list) {
                hurt(p, e, d("sword_flame_slash"));
                e.igniteForSeconds(5);
            }
            arc(level, p, 3, Math.toRadians(70), ParticleTypes.FLAME);
            arc(level, p, 4.5, Math.toRadians(70), ParticleTypes.FLAME);
            sound(level, p, SoundEvents.FIRECHARGE_USE, 1.2f);
            return true;
        });

        Spell blazingEdge = make("sword_blazing_edge", FLAME, 25, 240, 3, (level, p) -> {
            give(p, Buff.BLAZING_EDGE, 160, scaled(1f));
            burst(level, mid(p), ParticleTypes.FLAME, 40, 1.5);
            sound(level, p, SoundEvents.BLAZE_SHOOT, 0.8f);
            return true;
        });

        Spell flameWhirl = make("sword_flame_whirl", FLAME, 40, 200, 4, (level, p) -> {
            List<LivingEntity> list = enemies(level, p, 5);
            if (list.isEmpty()) return false;
            for (LivingEntity e : list) {
                hurt(p, e, d("sword_flame_whirl"));
                e.igniteForSeconds(6);
                pushFrom(e, p.position(), 0.8, 0.3);
            }
            ring(level, p.position(), 5, ParticleTypes.FLAME);
            ring(level, p.position(), 3, ParticleTypes.FLAME);
            burst(level, mid(p), ParticleTypes.FLAME, 60, 3);
            sound(level, p, SoundEvents.FIRECHARGE_USE, 0.8f);
            return true;
        });

        // ---------- Природа: Клинок рощи ----------
        Spell thornThrust = make("sword_thorn_thrust", FLORA, 15, 50, 1, (level, p) -> {
            List<LivingEntity> list = corridor(level, p, 7, 1.3);
            if (list.isEmpty()) return false;
            for (LivingEntity e : list) {
                hurt(p, e, d("sword_thorn_thrust"));
                effect(e, MobEffects.POISON, 80, 0);
            }
            Vec3 dir = flatLook(p);
            for (int s = 1; s <= 7; s++) {
                burst(level, p.position().add(dir.scale(s)).add(0, 0.3, 0),
                        blockParticle(Blocks.OAK_LEAVES), 6, 0.8);
            }
            line(level, p.getEyePosition(), p.position().add(dir.scale(7)).add(0, 1, 0), ParticleTypes.CRIT);
            sound(level, p, SoundEvents.GRASS_BREAK, 1f);
            return true;
        });

        Spell rootGrasp = make("sword_root_grasp", FLORA, 30, 240, 3, (level, p) -> {
            List<LivingEntity> list = enemies(level, p, 6);
            if (list.isEmpty()) return false;
            for (LivingEntity e : list) {
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 60, 6);
                effect(e, MobEffects.JUMP, 60, 128);
                burst(level, e.position().add(0, 0.3, 0), blockParticle(Blocks.MANGROVE_ROOTS), 25, 1);
            }
            ring(level, p.position(), 6, ParticleTypes.COMPOSTER);
            sound(level, p, SoundEvents.GRASS_BREAK, 0.7f);
            return true;
        });

        Spell thornyBark = make("sword_thorny_bark", FLORA, 30, 300, 3, (level, p) -> {
            give(p, Buff.THORNY_BARK, 160, scaled(1f));
            burst(level, mid(p), blockParticle(Blocks.OAK_LOG), 40, 1.5);
            ring(level, p.position(), 1.5, ParticleTypes.COMPOSTER);
            sound(level, p, SoundEvents.GRASS_BREAK, 0.6f);
            return true;
        });

        // ---------- Время: Клинок мгновений ----------
        Spell timeHaste = make("sword_time_haste", ACCELERATION, 30, 300, 3, (level, p) -> {
            effect(p, MobEffects.MOVEMENT_SPEED, 160, 2);
            effect(p, MobEffects.DIG_SPEED, 160, 2);
            burst(level, mid(p), ParticleTypes.ENCHANT, 40, 1.5);
            sound(level, p, SoundEvents.BEACON_POWER_SELECT, 1.2f);
            return true;
        });

        Spell timeEcho = make("sword_time_echo", STASIS, 30, 240, 4, (level, p) -> {
            give(p, Buff.ECHO, 200, scaled(1f));
            burst(level, mid(p), ParticleTypes.REVERSE_PORTAL, 40, 1.5);
            sound(level, p, SoundEvents.ENDERMAN_TELEPORT, 0.5f);
            return true;
        });

        // ---------- Звёзды: Звёздный клинок ----------
        Spell blinding = make("sword_blinding", CONSTELLATION, 35, 300, 3, (level, p) -> {
            List<LivingEntity> list = around(level, p, 8).stream()
                    .filter(e -> e instanceof Enemy || e instanceof Player).toList();
            if (list.isEmpty()) return false;
            for (LivingEntity e : list) {
                effect(e, MobEffects.BLINDNESS, 40, 0);
                effect(e, MobEffects.WEAKNESS, 40, 20);   // игроки не наносят урона
                MagicEvents.freezeAi(e, 40);              // мобы 2 секунды не действуют
                burst(level, mid(e), ParticleTypes.END_ROD, 20, 1);
            }
            burst(level, mid(p), ParticleTypes.FLASH, 3, 1);
            ring(level, p.position(), 8, ParticleTypes.END_ROD);
            sound(level, p, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1f);
            return true;
        });

        Spell orbit = make("sword_orbit", CONSTELLATION, 40, 400, 4, (level, p) -> {
            float dmg = d("sword_orbit");
            for (int i = 0; i < 60; i++) {
                int tick = i;
                Scheduler.later(tick * 2, bound(() -> {
                    if (!p.isAlive()) return;
                    double base = tick * 0.35;
                    Set<LivingEntity> hit = new HashSet<>();
                    for (int s = 0; s < 3; s++) {
                        double a = base + s * Math.PI * 2 / 3;
                        Vec3 pos = p.position().add(Math.cos(a) * 2.5, 1.2, Math.sin(a) * 2.5);
                        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0);
                        if (tick % 5 == 0) hit.addAll(around(level, p, pos, 1.6));
                    }
                    for (LivingEntity e : hit) hurt(p, e, dmg);
                }));
            }
            sound(level, p, SoundEvents.BEACON_ACTIVATE, 1.8f);
            return true;
        });

        Spell starPull = make("sword_star_pull", COSMOS, 50, 500, 5, (level, p) -> {
            Vec3 center = aimPoint(p, 20);
            if (center == null) return false;
            float dmg = d("sword_star_pull");
            for (int i = 0; i < 8; i++) {
                double r = 6 - i * 0.6;
                Scheduler.later(i * 5, bound(() -> {
                    for (LivingEntity e : enemies(level, p, center, 7)) pullTo(e, center, 0.8);
                    ring(level, center, r, ParticleTypes.END_ROD);
                    burst(level, center.add(0, 1, 0), ParticleTypes.END_ROD, 10, 1);
                }));
            }
            Scheduler.later(45, bound(() -> {
                for (LivingEntity e : around(level, p, center, 5)) {
                    hurt(p, e, dmg);
                    pushFrom(e, center, 1.2, 0.5);
                }
                burst(level, center.add(0, 1, 0), ParticleTypes.FLASH, 3, 1);
                burst(level, center.add(0, 1, 0), ParticleTypes.EXPLOSION_EMITTER, 1, 0);
                burst(level, center.add(0, 1, 0), ParticleTypes.END_ROD, 120, 5);
                sound(level, p, SoundEvents.GENERIC_EXPLODE, 1.4f);
            }));
            sound(level, p, SoundEvents.BEACON_ACTIVATE, 1.2f);
            return true;
        });

        // ---------- Тёмная природа: Клинок скверны ----------
        Spell infectingStrike = make("sword_infecting_strike", ROT, 25, 120, 2, (level, p) -> {
            LivingEntity t = target(p, 5);
            if (t == null) return false;
            Set<LivingEntity> infected = new LinkedHashSet<>();
            infected.add(t);
            hurt(p, t, d("sword_infecting_strike"));
            float pulse = d("sword_infecting_pulse");
            Scheduler.repeat(20, 4, bound(() -> {
                for (LivingEntity src : new ArrayList<>(infected)) {
                    if (!src.isAlive()) continue;
                    for (LivingEntity e : enemies(level, p, src.position(), 4)) {
                        if (infected.size() >= 6) break;
                        infected.add(e);
                    }
                }
                for (LivingEntity e : infected) {
                    if (!e.isAlive()) continue;
                    effect(e, MobEffects.POISON, 60, 0);
                    hurt(p, e, pulse);
                    burst(level, mid(e), ParticleTypes.SPORE_BLOSSOM_AIR, 12, 1);
                }
            }));
            burst(level, mid(t), ParticleTypes.SNEEZE, 25, 1);
            sound(level, t, SoundEvents.EVOKER_CAST_SPELL, 0.8f);
            return true;
        });

        Spell lifeDevour = make("sword_life_devour", ROT, 30, 160, 3, (level, p) -> {
            LivingEntity t = target(p, 6);
            if (t == null) return false;
            float dealt = scaled(d("sword_life_devour"));
            hurt(p, t, d("sword_life_devour"));
            p.heal(dealt * 0.5f);
            line(level, mid(t), p.getEyePosition(), ParticleTypes.SOUL);
            burst(level, mid(t), ParticleTypes.DAMAGE_INDICATOR, 15, 1);
            sound(level, t, SoundEvents.SOUL_ESCAPE, 0.8f);
            return true;
        });

        Spell rotShroud = make("sword_rot_shroud", ROT, 45, 400, 4, (level, p) -> {
            float dmg = d("sword_rot_shroud");
            Scheduler.repeat(10, 16, bound(() -> {
                if (!p.isAlive()) return;
                for (LivingEntity e : enemies(level, p, 4)) {
                    hurt(p, e, dmg);
                    effect(e, MobEffects.WITHER, 40, 0);
                }
                ring(level, p.position(), 4, ParticleTypes.SPORE_BLOSSOM_AIR);
                burst(level, mid(p), ParticleTypes.SNEEZE, 8, 3);
            }));
            sound(level, p, SoundEvents.EVOKER_CAST_SPELL, 0.6f);
            return true;
        });

        BY_ELEMENT.put(Element.LIGHTNING, listOf(stormDash, staticCharge, stormWrath));
        BY_ELEMENT.put(Element.WATER, listOf(waterCrescent, whirlpool, healingTide));
        BY_ELEMENT.put(Element.EARTH, listOf(stoneStrike, stoneSkin, shared("quicksand")));
        BY_ELEMENT.put(Element.AIR, listOf(windSlash, windDash, repulsion));
        BY_ELEMENT.put(Element.FIRE, listOf(flameSlash, blazingEdge, flameWhirl));
        BY_ELEMENT.put(Element.NATURE, listOf(thornThrust, rootGrasp, thornyBark));
        BY_ELEMENT.put(Element.TIME, listOf(shared("backtrack"), timeHaste, timeEcho));
        BY_ELEMENT.put(Element.STARS, listOf(blinding, orbit, starPull));
        BY_ELEMENT.put(Element.DARK_NATURE, listOf(infectingStrike, lifeDevour, rotShroud));
    }

    private SwordSpells() {}
}
