package com.example.elementalmagic.boss;

import com.example.elementalmagic.magic.Element;
import com.example.elementalmagic.magic.Scheduler;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Владыка скверны: босс стихии «Тёмная природа».
 * Способности:
 *  1. Ядовитые корни: из земли под игроками рвутся корни, обездвиживают и отравляют.
 *  2. Чумное облако: облако гнили вокруг босса на 6 секунд.
 *  3. Высасывание жизни: забирает здоровье у ближайшего игрока и лечит себя.
 *  Дальний импульс отравляет.
 */
public class BlightLordEntity extends AbstractBoss {
    private static final ParticleOptions ROOT_PARTICLE =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState());

    private int rootsCooldown = 100;
    private int cloudCooldown = 260;
    private int drainCooldown = 160;

    public BlightLordEntity(EntityType<? extends BlightLordEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return baseAttributes(340, 13, 0.27, 9);
    }

    @Override
    protected Element bossElement() {
        return Element.DARK_NATURE;
    }

    @Override
    protected BossEvent.BossBarColor barColor() {
        return BossEvent.BossBarColor.GREEN;
    }

    @Override
    protected ParticleOptions boltParticle() {
        return ParticleTypes.SPORE_BLOSSOM_AIR;
    }

    @Override
    protected void onBoltHit(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
    }

    @Override
    protected void tickAbilities(ServerLevel level, LivingEntity target, boolean enraged) {
        if (--rootsCooldown <= 0) {
            roots(level, enraged ? 5 : 3);
            rootsCooldown = enraged ? 130 : 190;
        }
        if (--cloudCooldown <= 0) {
            plagueCloud(level);
            cloudCooldown = enraged ? 220 : 320;
        }
        if (--drainCooldown <= 0 && drain(level)) {
            drainCooldown = enraged ? 110 : 170;
        }
    }

    private void roots(ServerLevel level, int count) {
        List<Player> players = playersNear(level, 22);
        if (players.isEmpty()) return;
        for (int i = 0; i < count; i++) {
            Player p = players.get(random.nextInt(players.size()));
            double ox = i == 0 ? 0 : (random.nextDouble() - 0.5) * 7;
            double oz = i == 0 ? 0 : (random.nextDouble() - 0.5) * 7;
            Vec3 at = new Vec3(p.getX() + ox, p.getY(), p.getZ() + oz);
            ringAt(level, at, 1.6, ParticleTypes.SPORE_BLOSSOM_AIR);
            Scheduler.later(20, () -> {
                if (!isAlive()) return;
                for (Player victim : level.getEntitiesOfClass(Player.class, new AABB(at, at).inflate(1.9),
                        AbstractBoss::valid)) {
                    victim.hurt(damageSources().indirectMagic(this, this), 5f);
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 6));
                    victim.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
                }
                level.sendParticles(ROOT_PARTICLE, at.x, at.y + 0.5, at.z, 50, 0.7, 0.8, 0.7, 0.1);
                playSound(SoundEvents.GRASS_BREAK, 1.5f, 0.6f);
            });
        }
    }

    /** Облако держится на месте, где его создали, и пульсирует раз в секунду. */
    private void plagueCloud(ServerLevel level) {
        Vec3 c = position();
        for (int i = 0; i < 6; i++) {
            Scheduler.later(i * 20, () -> {
                if (!isAlive()) return;
                ringAt(level, c, 8, ParticleTypes.SPORE_BLOSSOM_AIR);
                level.sendParticles(ParticleTypes.SNEEZE, c.x, c.y + 1, c.z, 60, 4.0, 0.8, 4.0, 0.02);
                for (Player p : level.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(8),
                        AbstractBoss::valid)) {
                    p.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                    p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 1));
                    p.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                }
            });
        }
        playSound(SoundEvents.EVOKER_CAST_SPELL, 2f, 0.5f);
    }

    /** Забирает 6 здоровья у ближайшего видимого игрока и лечит босса на 12. */
    private boolean drain(ServerLevel level) {
        Player nearest = null;
        double best = 14 * 14;
        for (Player p : playersNear(level, 14)) {
            double d = distanceToSqr(p);
            if (d < best && hasLineOfSight(p)) {
                best = d;
                nearest = p;
            }
        }
        if (nearest == null) return false;
        nearest.hurt(damageSources().indirectMagic(this, this), 6f);
        heal(12f);
        particleLine(level, nearest.position().add(0, 1, 0), position().add(0, 2, 0), ParticleTypes.SOUL, 0.4);
        playSound(SoundEvents.SOUL_ESCAPE.value(), 1.5f, 0.7f);
        return true;
    }
}
