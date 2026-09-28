package com.alganaut.hominid.entity.mellified;

import com.alganaut.hominid.entity.goal.AttackTurtleEggGoal;

import com.alganaut.hominid.entity.goal.MellifiedSwellGoal;
import com.alganaut.hominid.registry.sound.HominidSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nullable;
import java.util.Collection;

public class Mellified extends Monster {
    public static final EntityDataAccessor<Integer> DATA_SWELL_DIR;
    public int idleAnimationTimeout = 0;
    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState idleAnimationState = new AnimationState();
    public int oldSwell;
    public int swell;
    public int maxSwell = 60;
    public boolean hasExploded = false;

    public Mellified(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 35.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.DROWNED_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.DROWNED_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DROWNED_DEATH;
    }

    private boolean isMoving() {
        return this.getDeltaMovement().horizontalDistance() > 0.01F;
    }

    protected boolean isSunSensitive() {
        return false;
    }
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new AttackTurtleEggGoal(this, 1.0, 3));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1, false));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.0F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
        if (!hasMellifiedExploded())
        {
            this.goalSelector.addGoal(3, new MellifiedSwellGoal(this));
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SWELL_DIR, -1);
        super.defineSynchedData(builder);
    }

    public void setSwellDir(int state) {
        this.entityData.set(DATA_SWELL_DIR, state);
    }

    private void setupAnimationStates() {
        if (this.getDeltaMovement().horizontalDistance() <= 0.001F) {
            if (this.idleAnimationTimeout <= 0) {
                this.idleAnimationTimeout = 250;
                this.idleAnimationState.start(this.tickCount);
            } else {
                --this.idleAnimationTimeout;
            }
        } else {
            this.idleAnimationTimeout = 0;
            this.idleAnimationState.stop();
        }
    }
    @Override
    public void tick() {
        if (this.level().isClientSide()) {
            this.setupAnimationStates();
        }
        if (this.isAlive() && !hasMellifiedExploded()) {
            this.oldSwell = this.swell;
            int i = this.getSwellDir();
            if (i > 0 && this.swell == 0) {
                this.gameEvent(GameEvent.PRIME_FUSE);
            }
            this.swell += i;
            if (this.swell < 0) {
                this.swell = 0;
            }

            if (this.swell >= this.maxSwell) {
                this.swell = this.maxSwell;
                this.hasExploded = true;
                this.attackAnimationState.start(this.tickCount);
                this.explodeMellifiedHead();
            }
        }
        super.tick();
    }

    public int getSwellDir() {
        return this.entityData.get(DATA_SWELL_DIR);
    }

    @Override
    public void handleEntityEvent(byte state) {
        if (state == 60){
            this.attackAnimationState.stop();
            this.attackAnimationState.startIfStopped(this.tickCount);
        }
        else super.handleEntityEvent(state);
    }

    private void spawnHoneyParticles(LivingEntity entity) {
        if(!this.level().isClientSide){
            ServerLevel serverLevel = (ServerLevel) entity.level();

            for (int i = 0; i < 40; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 0.8;
                double offsetY = this.random.nextDouble() * 0.5 + 0.5;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.8;
                double velocityX = (this.random.nextDouble() - 0.5) * 0.2;
                double velocityY = this.random.nextDouble() * 0.3 + 0.2;
                double velocityZ = (this.random.nextDouble() - 0.5) * 0.2;

                serverLevel.sendParticles(ParticleTypes.FALLING_HONEY,
                        entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ,
                        1, velocityX, velocityY, velocityZ, 0);
            }
        }
    }

    public void explodeMellifiedHead() {
        if (!this.level().isClientSide) {
            this.attackAnimationState.stop();
            this.spawnLingeringCloud();
            this.swell = 0;
            this.hasExploded = true;
        }
    }

    private void spawnLingeringCloud() {
            AreaEffectCloud sporeCloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
            sporeCloud.setRadius(5F);
            sporeCloud.setDuration(100);
            sporeCloud.setOwner(this);
            sporeCloud.setParticle(ParticleTypes.CLOUD);
            sporeCloud.setRadiusPerTick(-sporeCloud.getRadius() / (float)sporeCloud.getDuration());
            sporeCloud.addEffect(new MobEffectInstance (MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
            this.level().addFreshEntity(sporeCloud);
    }

    public boolean hasMellifiedExploded() {
        return this.hasExploded;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return level.getBlockState(pos).isAir() ? 10.0F : 0.0F;
    }

    public boolean isInvertedHealAndHarm() {
        return true;
    }

    static {
        DATA_SWELL_DIR = SynchedEntityData.defineId(Mellified.class, EntityDataSerializers.INT);
    }
}