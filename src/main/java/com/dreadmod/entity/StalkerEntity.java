package com.dreadmod.entity;

import com.dreadmod.DreadUtil;
import com.dreadmod.FearManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Lo Stalker: figura alta e pallida che ti perseguita dal buio.
 * - Si muove SOLO quando nessun giocatore lo sta guardando (come un angelo piangente).
 * - Si teletrasporta alle tue spalle se resta troppo lontano senza essere visto.
 * - Il suo attacco ti acceca, ti rallenta e ti terrorizza.
 * - Scompare dopo circa 6 minuti. Non attacca chi porta un Amuleto Sacro.
 */
public class StalkerEntity extends HostileEntity {
    private static final int MAX_LIFETIME = 7200;

    private boolean watched = false;
    private int lifetime = 0;
    private int unwatchedTicks = 0;
    private int teleportCooldown = 200;

    public StalkerEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 30;
    }

    public static DefaultAttributeContainer.Builder createStalkerAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.36)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.5));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 32.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, 1, false, false,
                e -> !(e instanceof PlayerEntity p) || !DreadUtil.hasCharm(p)));
    }

    /** Se true, tutta l'IA viene sospesa: lo Stalker resta immobile come una statua. */
    @Override
    protected boolean isImmobile() {
        return watched || super.isImmobile();
    }

    public boolean isWatched() {
        return watched;
    }

    @Override
    public void tick() {
        super.tick();
        World world = this.getWorld();

        if (world.isClient) {
            if (this.random.nextInt(3) == 0) {
                world.addParticle(ParticleTypes.SMOKE, this.getParticleX(0.5), this.getRandomBodyY(),
                        this.getParticleZ(0.5), 0.0, 0.02, 0.0);
            }
            return;
        }

        lifetime++;
        updateWatched();

        if (lifetime > MAX_LIFETIME && !watched) {
            vanish();
            return;
        }

        LivingEntity target = this.getTarget();

        if (watched) {
            unwatchedTicks = 0;
            this.getNavigation().stop();
            this.setVelocity(0.0, Math.min(0.0, this.getVelocity().y), 0.0);
        } else {
            unwatchedTicks++;
        }

        if (teleportCooldown > 0) teleportCooldown--;

        // Troppo lontano e non visto: compare alle spalle del bersaglio.
        if (target != null && !watched && teleportCooldown == 0 && unwatchedTicks > 80
                && this.distanceTo(target) > 26.0f) {
            if (teleportBehind(target, 11.0 + this.random.nextInt(4))) {
                teleportCooldown = 300;
            }
        }
    }

    /** Un giocatore "guarda" lo Stalker se è nel suo campo visivo, con visuale libera e non è accecato. */
    private void updateWatched() {
        boolean seen = false;
        for (PlayerEntity p : this.getWorld().getPlayers()) {
            if (p.isSpectator() || p.hasStatusEffect(StatusEffects.BLINDNESS)) continue;
            if (this.squaredDistanceTo(p) > 64.0 * 64.0) continue;
            Vec3d to = this.getEyePos().subtract(p.getEyePos());
            double len = to.length();
            if (len < 0.5) {
                seen = true;
                break;
            }
            double dot = p.getRotationVec(1.0f).dotProduct(to.multiply(1.0 / len));
            if (dot > 0.55 && p.canSee(this)) {
                seen = true;
                break;
            }
        }
        watched = seen;
    }

    private boolean teleportBehind(LivingEntity target, double dist) {
        World world = this.getWorld();
        Vec3d look = target.getRotationVec(1.0f);
        Vec3d back = new Vec3d(-look.x, 0, -look.z).normalize();
        double oldX = getX(), oldY = getY(), oldZ = getZ();

        for (int i = 0; i < 14; i++) {
            double angle = (this.random.nextDouble() - 0.5) * 1.4;
            Vec3d dir = back.rotateY((float) angle);
            double x = target.getX() + dir.x * dist;
            double z = target.getZ() + dir.z * dist;
            for (int dy = 4; dy >= -6; dy--) {
                BlockPos bp = BlockPos.ofFloored(x, target.getY() + dy, z);
                if (!world.getBlockState(bp.down()).isSolidBlock(world, bp.down())) continue;
                if (!world.getBlockState(bp).isAir() || !world.getBlockState(bp.up()).isAir()
                        || !world.getBlockState(bp.up(2)).isAir()) continue;
                this.refreshPositionAndAngles(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5,
                        this.random.nextFloat() * 360f, 0f);
                if (world.isSpaceEmpty(this)) {
                    this.getNavigation().stop();
                    return true;
                }
                this.refreshPositionAndAngles(oldX, oldY, oldZ, this.getYaw(), 0f);
            }
        }
        return false;
    }

    private void vanish() {
        if (this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getBodyY(0.5), getZ(), 25, 0.3, 0.8, 0.3, 0.02);
        }
        this.discard();
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 80, 0));
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1));
            if (living instanceof ServerPlayerEntity sp) {
                FearManager.add(sp, 25);
                DreadUtil.playAtPlayer(sp, SoundEvents.ENTITY_ENDERMAN_SCREAM, 1.0f, 0.5f);
            }
            // Dopo l'attacco svanisce alle spalle della vittima per ricominciare la caccia.
            if (this.random.nextInt(100) < 55) {
                teleportBehind(living, 16.0);
            }
            teleportCooldown = 120;
        }
        return hit;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_ENDERMAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ENDERMAN_DEATH;
    }

    @Override
    public float getSoundPitch() {
        return 0.5f;
    }
}
