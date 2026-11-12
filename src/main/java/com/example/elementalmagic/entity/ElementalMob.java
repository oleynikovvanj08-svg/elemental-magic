package com.example.elementalmagic.entity;

import com.example.elementalmagic.ModRegistry;
import com.example.elementalmagic.magic.Element;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Моб стихии. Один класс для всех 45 мобов: стихия и вид передаются при регистрации. */
public class ElementalMob extends Monster implements RangedAttackMob {

    public enum Kind {
        //    id       материал   здоровье урон скорость броня отбрас. масштаб ширина высота опыт цвет пятен яйца
        GOLEM("golem", "shard",   40,      7,   0.23,    6,    0.6,    1.3f,   0.9f,  2.5f,  10,  0x333333),
        MAGE ("mage",  "crystal", 24,      4,   0.25,    2,    0.0,    1.0f,   0.6f,  1.95f, 8,   0x1A1A40),
        WISP ("wisp",  "core",    16,      4,   0.34,    0,    0.0,    0.7f,   0.45f, 1.4f,  6,   0xFFFFFF);

        public final String id;
        public final String material;
        public final double health, damage, speed, armor, knockbackResistance;
        public final float scale, width, height;
        public final int xp;
        public final int eggColor;

        Kind(String id, String material, double health, double damage, double speed, double armor,
             double knockbackResistance, float scale, float width, float height, int xp, int eggColor) {
            this.id = id;
            this.material = material;
            this.health = health;
            this.damage = damage;
            this.speed = speed;
            this.armor = armor;
            this.knockbackResistance = knockbackResistance;
            this.scale = scale;
            this.width = width;
            this.height = height;
            this.xp = xp;
            this.eggColor = eggColor;
        }
    }

    public final Element element;
    public final Kind kind;

    public ElementalMob(EntityType<? extends ElementalMob> type, Level level, Element element, Kind kind) {
        super(type, level);
        this.element = element;
        this.kind = kind;
        this.xpReward = kind.xp;
        // Цели добавляются здесь, потому что registerGoals() вызывается до того, как заданы element и kind
        if (!level.isClientSide) addGoals();
    }

    public static AttributeSupplier.Builder createAttributes(Element element, Kind kind) {
        double m = element.bossOnly ? 1.5 : 1.0;
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, kind.health * m)
                .add(Attributes.ATTACK_DAMAGE, kind.damage * m)
                .add(Attributes.MOVEMENT_SPEED, kind.speed)
                .add(Attributes.ARMOR, kind.armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, kind.knockbackResistance)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    /** Название моба без ru_ru.json. */
    @Override
    protected Component getTypeName() {
        if (element == null || kind == null) return super.getTypeName();
        return Component.translatableWithFallback(getType().getDescriptionId(), ModEntities.mobName(element, kind));
    }

    private void addGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if (kind == Kind.MAGE) {
            goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 14f));
        } else {
            goalSelector.addGoal(2, new MeleeAttackGoal(this, kind == Kind.WISP ? 1.2 : 1.0, false));
        }
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, data);
        if (kind == Kind.MAGE) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModRegistry.STAVES.get(element).get()));
            setDropChance(EquipmentSlot.MAINHAND, 0f); // посох не выпадает
        }
        return result;
    }

    // ---------- Атаки ----------

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) applyElementEffect(living);
        return hit;
    }

    /** Магический удар мага на расстоянии. */
    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!(level() instanceof ServerLevel server) || !hasLineOfSight(target)) return;

        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        target.hurt(damageSources().indirectMagic(this, this), damage);
        applyElementEffect(target);

        Vec3 from = getEyePosition();
        Vec3 to = target.position().add(0, target.getBbHeight() / 2, 0);
        Vec3 step = to.subtract(from);
        int n = Math.max(1, (int) (step.length() * 2));
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.add(step.scale(i / (double) n));
            server.sendParticles(dust(), p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        playSound(SoundEvents.EVOKER_CAST_SPELL, 1f, 1.2f);
    }

    /** Эффект стихии при попадании. */
    private void applyElementEffect(LivingEntity t) {
        switch (element) {
            case LIGHTNING -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            case LIGHT -> t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            case DARKNESS -> t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            case WATER -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            case EARTH -> t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0));
            case AIR -> t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0));
            case FIRE -> t.igniteForSeconds(4);
            case NATURE -> t.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            case ASH -> t.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 0));
            case EXPLOSION -> t.knockback(1.2, getX() - t.getX(), getZ() - t.getZ());
            case ELEMENTAL -> {
                switch (random.nextInt(3)) {
                    case 0 -> t.igniteForSeconds(3);
                    case 1 -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                    default -> t.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                }
            }
            case TIME -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            case STARS -> t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 1));
            case DARK_NATURE -> t.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            case SHADOW -> t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
        }
    }

    // ---------- Внешний вид и звуки ----------

    private DustParticleOptions dust() {
        int c = element.color;
        return new DustParticleOptions(new Vector3f(
                ((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f), 1.0f);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Частицы цвета стихии вокруг моба
        if (level().isClientSide && random.nextInt(3) == 0) {
            level().addParticle(dust(), getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0, 0);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return switch (kind) {
            case GOLEM -> null;
            case MAGE -> SoundEvents.EVOKER_AMBIENT;
            case WISP -> SoundEvents.VEX_AMBIENT;
        };
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return switch (kind) {
            case GOLEM -> SoundEvents.IRON_GOLEM_HURT;
            case MAGE -> SoundEvents.EVOKER_HURT;
            case WISP -> SoundEvents.VEX_HURT;
        };
    }

    @Override
    protected SoundEvent getDeathSound() {
        return switch (kind) {
            case GOLEM -> SoundEvents.IRON_GOLEM_DEATH;
            case MAGE -> SoundEvents.EVOKER_DEATH;
            case WISP -> SoundEvents.VEX_DEATH;
        };
    }
}
