package com.example.elementalmagic.boss;

import com.example.elementalmagic.magic.Element;
import com.example.elementalmagic.magic.Scheduler;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Владыка магмы: босс стихии «Лава». Не горит в огне и лаве.
 * Способности:
 *  1. Извержение: под ногами игроков вспыхивают метки, через секунду из земли бьёт огонь.
 *  2. Магматическое кольцо: расходящаяся волна огня, от неё можно уйти прыжком.
 *  Дальний импульс поджигает.
 */
public class MagmaLordEntity extends AbstractBoss {
    private int eruptionCooldown = 100;
    private int ringCooldown = 220;

    public MagmaLordEntity(EntityType<? extends MagmaLordEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return baseAttributes(350, 14, 0.28, 10);
    }

    @Override
    protected Element bossElement() {
        return Element.LAVA;
    }

    @Override
    protected BossEvent.BossBarColor barColor() {
        return BossEvent.BossBarColor.RED;
    }

    @Override
    protected ParticleOptions boltParticle() {
        return ParticleTypes.FLAME;
    }

    @Override
    protected void onBoltHit(Player player) {
        player.igniteForSeconds(5);
    }

    @Override
    protected void tickAbilities(ServerLevel level, LivingEntity target, boolean enraged) {
        if (--eruptionCooldown <= 0) {
            eruption(level, enraged ? 6 : 3);
            eruptionCooldown = enraged ? 140 : 200;
        }
        if (--ringCooldown <= 0) {
            magmaRing(level);
            ringCooldown = enraged ? 180 : 260;
        }
    }

    /** Извержение: первая метка точно под игроком, остальные рядом. */
    private void eruption(ServerLevel level, int count) {
        List<Player> players = playersNear(level, 24);
        if (players.isEmpty()) return;
        for (int i = 0; i < count; i++) {
            Player p = players.get(random.nextInt(players.size()));
            double ox = i == 0 ? 0 : (random.nextDouble() - 0.5) * 8;
            double oz = i == 0 ? 0 : (random.nextDouble() - 0.5) * 8;
            Vec3 at = new Vec3(p.getX() + ox, p.getY(), p.getZ() + oz);
            ringAt(level, at, 1.8, ParticleTypes.FLAME);
            Scheduler.later(20, () -> {
                if (!isAlive()) return;
                for (Player victim : level.getEntitiesOfClass(Player.class, new AABB(at, at).inflate(2.2),
                        AbstractBoss::valid)) {
                    victim.hurt(damageSources().indirectMagic(this, this), 8f);
                    victim.igniteForSeconds(6);
                    victim.setDeltaMovement(0, 0.9, 0);
                    victim.hurtMarked = true;
                }
                level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.5, at.z, 40, 0.8, 0.8, 0.8, 0.1);
                level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1.0, at.z, 40, 0.6, 1.2, 0.6, 0.05);
                playSound(SoundEvents.BLAZE_SHOOT, 1.5f, 0.6f);
            });
        }
    }

    /** Кольцо огня расходится от босса: бьёт тех, кто стоит на земле. Прыжок спасает. */
    private void magmaRing(ServerLevel level) {
        Vec3 c = position();
        for (int i = 1; i <= 6; i++) {
            double r = 2 + i * 1.8;
            Scheduler.later(i * 4, () -> {
                if (!isAlive()) return;
                ringAt(level, c, r, ParticleTypes.FLAME);
                ringAt(level, c, r, ParticleTypes.LAVA);
                for (Player p : level.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r + 1.5),
                        AbstractBoss::valid)) {
                    double d = Math.hypot(p.getX() - c.x, p.getZ() - c.z);
                    if (Math.abs(d - r) <= 1.2 && p.onGround()) {
                        p.hurt(damageSources().indirectMagic(this, this), 6f);
                        p.igniteForSeconds(3);
                        p.setDeltaMovement(p.getDeltaMovement().add(0, 0.7, 0));
                        p.hurtMarked = true;
                    }
                }
            });
        }
        playSound(SoundEvents.LAVA_EXTINGUISH, 2f, 0.6f);
    }
}
