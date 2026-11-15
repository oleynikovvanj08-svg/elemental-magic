package com.example.elementalmagic.boss;

import com.example.elementalmagic.magic.Element;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
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

import java.util.List;

/**
 * Основа для боссов стихий. Здесь общее: босс-бар, цели, дальний импульс с отбрасыванием
 * и охота за скрывшимся игроком (телепорт рядом и удар сквозь стены).
 * Наследнику остаётся описать стихию, цвет полоски и свои способности в tickAbilities.
 */
public abstract class AbstractBoss extends Monster {
    // ===== Дальний импульс =====
    private static final int BOLT_CHARGE_TICKS = 14;
    private static final double BOLT_MIN_DISTANCE = 5;
    private static final double BOLT_MAX_DISTANCE = 28;
    private static final double BOLT_RANGE = 32;

    // ===== Охота за скрывшимся игроком =====
    private static final int HIDE_TICKS = 60;
    private static final int HUNT_COOLDOWN_TICKS = 100;
    private static final double HUNT_MAX_DISTANCE = 48;
    private static final int HUNT_SEARCH_RADIUS = 6;

    private final ServerBossEvent bossEvent;

    private int boltCooldown = 60;
    private int chargeTicks = 0;
    private Vec3 aimPoint;
    private int hiddenTicks = 0;
    private int huntCooldown = 0;

    protected AbstractBoss(EntityType<? extends AbstractBoss> type, Level level) {
        super(type, level);
        this.xpReward = 100;
        this.bossEvent = new ServerBossEvent(getDisplayName(), barColor(), BossEvent.BossBarOverlay.NOTCHED_10);
    }

    // ===== Что задаёт наследник =====

    /** Стихия босса: от неё зависит добыча. */
    protected abstract Element bossElement();

    /** Цвет полоски здоровья. */
    protected abstract BossEvent.BossBarColor barColor();

    /** Способности босса. Вызывается каждый тик, пока у босса есть живая цель. */
    protected abstract void tickAbilities(ServerLevel level, LivingEntity target, boolean enraged);

    protected float boltDamage() { return 10f; }

    protected double boltKnockback() { return 1.8; }

    protected ParticleOptions boltParticle() { return ParticleTypes.END_ROD; }

    /** Дополнительный эффект на игрока, которого задел импульс (поджог, яд и т.п.). */
    protected void onBoltHit(Player player) {}

    protected int boltCooldownTicks(boolean enraged) { return enraged ? 50 : 80; }

    protected static AttributeSupplier.Builder baseAttributes(double health, double damage, double speed, double armor) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    // ===== Цели и поведение =====

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

        // Дальний импульс: зарядка, затем выстрел по запомненной точке
        if (chargeTicks > 0) {
            chargeTicks--;
            if (aimPoint != null) {
                Vec3 from = getEyePosition();
                particleLine(serverLevel, from, beamEnd(serverLevel, from), ParticleTypes.ENCHANT, 1.0);
            }
            if (chargeTicks == 0) fireBolt(serverLevel);
        } else if (--boltCooldown <= 0) {
            double dist = distanceToSqr(target);
            if (dist > BOLT_MIN_DISTANCE * BOLT_MIN_DISTANCE
                    && dist < BOLT_MAX_DISTANCE * BOLT_MAX_DISTANCE
                    && hasLineOfSight(target)) {
                aimPoint = target.getEyePosition();
                chargeTicks = BOLT_CHARGE_TICKS;
                boltCooldown = boltCooldownTicks(enraged);
                playSound(SoundEvents.BEACON_ACTIVATE, 2f, 1.6f);
            }
        }

        tickAbilities(serverLevel, target, enraged);
    }

    // ===== Дальний импульс =====

    private Vec3 beamEnd(ServerLevel level, Vec3 from) {
        Vec3 dir = aimPoint.subtract(from);
        if (dir.lengthSqr() < 1.0E-4) dir = new Vec3(0, 0, 1);
        Vec3 end = from.add(dir.normalize().scale(BOLT_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(from, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    private void fireBolt(ServerLevel level) {
        if (aimPoint == null) return;
        Vec3 from = getEyePosition();
        Vec3 end = beamEnd(level, from);
        Vec3 flat = new Vec3(end.x - from.x, 0, end.z - from.z);

        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(from, end).inflate(2), AbstractBoss::valid)) {
            if (p.getBoundingBox().inflate(0.6).clip(from, end).isEmpty()) continue;
            p.hurt(damageSources().indirectMagic(this, this), boltDamage());
            onBoltHit(p);
            if (flat.lengthSqr() > 1.0E-4) {
                p.setDeltaMovement(flat.normalize().scale(boltKnockback()).add(0, 0.5, 0));
                p.hurtMarked = true;
            }
        }

        particleLine(level, from, end, boltParticle(), 0.4);
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 1.5f, 1.4f);
        aimPoint = null;
    }

    // ===== Охота на скрывшегося =====

    private void huntTarget(ServerLevel level, LivingEntity target) {
        Vec3 spot = findFreeSpotNear(level, target.position());
        if (spot != null) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 40, 0.4, 0.8, 0.4, 0.05);
            teleportTo(spot.x, spot.y, spot.z);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 40, 0.4, 0.8, 0.4, 0.05);
            playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.5f);
        }
        if (distanceToSqr(target) < 49) shockwave(level, 8f);
    }

    /** Ближайшее к центру место, где босс помещается (3 блока высоты) и есть пол. */
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
                        if (!level.getBlockState(BlockPos.containing(pos).below()).blocksMotion()) continue;
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    // ===== Помощники для наследников =====

    protected static boolean valid(Player p) {
        return p.isAlive() && !p.isSpectator() && !p.isCreative();
    }

    protected List<Player> playersNear(ServerLevel level, double radius) {
        return level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius), AbstractBoss::valid);
    }

    /** Волна вокруг босса: урон и отбрасывание игроков в радиусе 7 блоков сквозь стены. */
    protected void shockwave(ServerLevel level, float damage) {
        for (Player p : playersNear(level, 7)) {
            p.hurt(damageSources().indirectMagic(this, this), damage);
            Vec3 away = p.position().subtract(position());
            away = new Vec3(away.x, 0, away.z);
            if (away.lengthSqr() > 1.0E-4) {
                p.setDeltaMovement(away.normalize().scale(1.2).add(0, 0.4, 0));
                p.hurtMarked = true;
            }
        }
        ring(level, 7, boltParticle());
        ring(level, 4, boltParticle());
        playSound(SoundEvents.WITHER_BREAK_BLOCK, 1.5f, 0.6f);
    }

    protected void ring(ServerLevel level, double radius, ParticleOptions type) {
        ringAt(level, position(), radius, type);
    }

    protected static void ringAt(ServerLevel level, Vec3 center, double radius, ParticleOptions type) {
        int n = Math.max(8, (int) (radius * 10));
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            level.sendParticles(type, center.x + Math.cos(a) * radius, center.y + 0.3,
                    center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    protected static void particleLine(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions type, double step) {
        Vec3 delta = to.subtract(from);
        int n = Math.max(1, (int) (delta.length() / step));
        for (int i = 0; i <= n; i++) {
            Vec3 pt = from.add(delta.scale(i / (double) n));
            level.sendParticles(type, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
        }
    }

    // ===== Добыча =====

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        for (ItemStack stack : BossLoot.roll(bossElement(), random)) {
            spawnAtLocation(stack);
        }
    }
}
