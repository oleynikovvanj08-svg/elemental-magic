package com.example.elementalmagic.boss;

import com.example.elementalmagic.magic.Element;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Хранитель времени: босс стихии «Время».
 * Способности:
 *  1. Временной импульс: дальний луч с отбрасыванием. Бьёт, как только есть прямая видимость,
 *     поэтому от босса нельзя спрятаться в башне или отстроиться вверх.
 *  2. Замедление времени: игроки рядом получают замедление и усталость.
 *  3. Временной скачок: появляется за спиной цели и бьёт.
 *  4. Разлом (когда здоровья меньше половины): волна урона вокруг босса с отбрасыванием.
 *  5. Охота: если цель не видна 3 секунды (спряталась в комнате, замуровалась), босс телепортируется
 *     на ближайшее свободное место рядом с ней и бьёт Разломом сквозь стены.
 */
public class TimeKeeperEntity extends Monster {
    // ===== Настройки баланса =====
    private static final float BOLT_DAMAGE = 10f;
    private static final double BOLT_KNOCKBACK = 1.8;
    private static final int BOLT_CHARGE_TICKS = 14;        // время зарядки (даёт шанс увернуться)
    private static final double BOLT_MIN_DISTANCE = 5;      // ближе босс бьёт в ближнем бою
    private static final double BOLT_MAX_DISTANCE = 28;
    private static final double BOLT_RANGE = 32;

    private static final int HIDE_TICKS = 60;               // сколько цель может прятаться до телепорта
    private static final int HUNT_COOLDOWN_TICKS = 100;
    private static final double HUNT_MAX_DISTANCE = 48;
    private static final int HUNT_SEARCH_RADIUS = 6;

    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);

    private int slowCooldown = 100;
    private int blinkCooldown = 160;
    private int riftCooldown = 200;
    private int boltCooldown = 60;

    private int chargeTicks = 0;
    private Vec3 aimPoint;

    private int hiddenTicks = 0;
    private int huntCooldown = 0;

    public TimeKeeperEntity(EntityType<? extends TimeKeeperEntity> type, Level level) {
        super(type, level);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300)
                .add(Attributes.ATTACK_DAMAGE, 12)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // mustSee = false: босс не теряет игрока, спрятавшегося за стеной
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    // ===== Босс-бар =====

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    // ===== Поведение =====

    /** Босс не исчезает ни вдали от игроков, ни на мирной сложности. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || !(level() instanceof ServerLevel serverLevel)) {
            chargeTicks = 0;
            return;
        }
        boolean enraged = getHealth() < getMaxHealth() * 0.5f;

        // Охота: цель не видна несколько секунд, поэтому босс идёт к ней сам
        if (huntCooldown > 0) huntCooldown--;
        if (hasLineOfSight(target)) {
            hiddenTicks = 0;
        } else if (++hiddenTicks >= HIDE_TICKS && huntCooldown <= 0
                && distanceToSqr(target) < HUNT_MAX_DISTANCE * HUNT_MAX_DISTANCE) {
            huntTarget(serverLevel, target);
            hiddenTicks = 0;
            huntCooldown = HUNT_COOLDOWN_TICKS;
        }

        // Временной импульс: зарядка, затем выстрел по запомненной точке
        if (chargeTicks > 0) {
            chargeTicks--;
            telegraph(serverLevel);
            if (chargeTicks == 0) fireBolt(serverLevel);
        } else if (--boltCooldown <= 0) {
            double dist = distanceToSqr(target);
            if (dist > BOLT_MIN_DISTANCE * BOLT_MIN_DISTANCE
                    && dist < BOLT_MAX_DISTANCE * BOLT_MAX_DISTANCE
                    && hasLineOfSight(target)) {
                aimPoint = target.getEyePosition();
                chargeTicks = BOLT_CHARGE_TICKS;
                boltCooldown = enraged ? 50 : 80;
                playSound(SoundEvents.BEACON_ACTIVATE, 2f, 1.6f);
            }
        }

        if (--slowCooldown <= 0) {
            slowField(serverLevel);
            slowCooldown = enraged ? 160 : 240;
        }
        if (--blinkCooldown <= 0 && distanceToSqr(target) > 36) {
            blink(serverLevel, target);
            blinkCooldown = enraged ? 140 : 220;
        }
        if (enraged && --riftCooldown <= 0) {
            rift(serverLevel);
            riftCooldown = 200;
        }
    }

    // ===== Временной импульс =====

    /** Конец луча: по направлению к точке прицела, но не дальше первого блока. */
    private Vec3 beamEnd(ServerLevel level, Vec3 from) {
        Vec3 dir = aimPoint.subtract(from);
        if (dir.lengthSqr() < 1.0E-4) dir = new Vec3(0, 0, 1);
        Vec3 end = from.add(dir.normalize().scale(BOLT_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(from, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    /** Во время зарядки показывает линию будущего выстрела, чтобы можно было увернуться. */
    private void telegraph(ServerLevel level) {
        if (aimPoint == null) return;
        Vec3 from = getEyePosition();
        particleLine(level, from, beamEnd(level, from), ParticleTypes.ENCHANT, 1.0);
    }

    /** Выстрел: урон и сильное отбрасывание всем игрокам на линии. */
    private void fireBolt(ServerLevel level) {
        if (aimPoint == null) return;
        Vec3 from = getEyePosition();
        Vec3 end = beamEnd(level, from);
        Vec3 flat = new Vec3(end.x - from.x, 0, end.z - from.z);

        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(from, end).inflate(2),
                pl -> pl.isAlive() && !pl.isSpectator() && !pl.isCreative())) {
            if (p.getBoundingBox().inflate(0.6).clip(from, end).isEmpty()) continue;
            p.hurt(damageSources().indirectMagic(this, this), BOLT_DAMAGE);
            if (flat.lengthSqr() > 1.0E-4) {
                p.setDeltaMovement(flat.normalize().scale(BOLT_KNOCKBACK).add(0, 0.5, 0));
                p.hurtMarked = true;
            }
        }

        particleLine(level, from, end, ParticleTypes.END_ROD, 0.4);
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 1.5f, 1.4f);
        aimPoint = null;
    }

    private static void particleLine(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions type, double step) {
        Vec3 delta = to.subtract(from);
        int n = Math.max(1, (int) (delta.length() / step));
        for (int i = 0; i <= n; i++) {
            Vec3 pt = from.add(delta.scale(i / (double) n));
            level.sendParticles(type, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
        }
    }

    // ===== Охота на скрывшегося =====

    /** Телепорт к ближайшему свободному месту рядом с целью и удар сквозь стены. */
    private void huntTarget(ServerLevel level, LivingEntity target) {
        Vec3 spot = findFreeSpotNear(level, target.position());
        if (spot != null) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 40, 0.4, 0.8, 0.4, 0.05);
            teleportTo(spot.x, spot.y, spot.z);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 40, 0.4, 0.8, 0.4, 0.05);
            playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.5f);
        }
        // Разлом бьёт по площади и не зависит от стен: замурованному игроку тоже достанется
        if (distanceToSqr(target) < 49) rift(level);
    }

    /**
     * Ближайшее к центру место, где босс помещается (ему нужно 3 блока высоты) и есть пол.
     * Кольца ищутся от центра наружу, поэтому находится самое близкое.
     */
    private Vec3 findFreeSpotNear(ServerLevel level, Vec3 center) {
        double baseX = Math.floor(center.x) + 0.5;
        double baseY = Math.floor(center.y);
        double baseZ = Math.floor(center.z) + 0.5;
        for (int r = 1; r <= HUNT_SEARCH_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    for (int dy : new int[]{0, 1, -1}) {
                        Vec3 pos = new Vec3(baseX + dx, baseY + dy, baseZ + dz);
                        if (!level.noCollision(this, getBoundingBox().move(pos.subtract(position())))) continue;
                        if (!level.getBlockState(net.minecraft.core.BlockPos.containing(pos).below()).blocksMotion()) continue;
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    // ===== Остальные способности =====

    /** Замедление времени: игроки в радиусе 10 блоков замедлены на 4 секунды. */
    private void slowField(ServerLevel level) {
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(10),
                pl -> pl.isAlive() && !pl.isSpectator() && !pl.isCreative())) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 1));
        }
        ring(level, 10, ParticleTypes.ENCHANT);
        playSound(SoundEvents.BEACON_DEACTIVATE, 2f, 0.7f);
    }

    /** Временной скачок: появляется за спиной цели и бьёт. */
    private void blink(ServerLevel level, LivingEntity target) {
        Vec3 look = target.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        Vec3 dest = target.position().subtract(flat.scale(1.8));
        if (!level.noCollision(this, getBoundingBox().move(dest.subtract(position())))) return;

        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        teleportTo(dest.x, dest.y, dest.z);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.6f);
        doHurtTarget(target);
    }

    /** Разлом времени: урон и отбрасывание игроков в радиусе 7 блоков. */
    private void rift(ServerLevel level) {
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(7),
                pl -> pl.isAlive() && !pl.isSpectator())) {
            p.hurt(damageSources().indirectMagic(this, this), 8f);
            Vec3 away = p.position().subtract(position());
            away = new Vec3(away.x, 0, away.z);
            if (away.lengthSqr() > 1.0E-4) {
                p.setDeltaMovement(away.normalize().scale(1.2).add(0, 0.4, 0));
                p.hurtMarked = true;
            }
        }
        ring(level, 7, ParticleTypes.REVERSE_PORTAL);
        ring(level, 4, ParticleTypes.REVERSE_PORTAL);
        playSound(SoundEvents.WITHER_BREAK_BLOCK, 1.5f, 0.6f);
    }

    private void ring(ServerLevel level, double radius, ParticleOptions type) {
        int n = (int) (radius * 10);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            level.sendParticles(type, getX() + Math.cos(a) * radius, getY() + 0.3,
                    getZ() + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    // ===== Добыча =====

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        for (ItemStack stack : BossLoot.roll(Element.TIME, random)) {
            spawnAtLocation(stack);
        }
    }
}
