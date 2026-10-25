package dev.zedkodex.modehud.entity;

import dev.zedkodex.modehud.ModeHud;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class Motorcycle extends Entity {
    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(Motorcycle.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ENGINE = SynchedEntityData.defineId(Motorcycle.class, EntityDataSerializers.BOOLEAN);

    public static final int MAX_FUEL = 6400;
    public static final int COAL_FUEL = 1600;

    private static final double MAX_SPEED = 0.55;
    private static final double MAX_REVERSE = 0.16;

    private double speed;
    private float fuelAcc;

    public float lean;
    public float leanO;
    public float pitch;
    public float pitchO;
    public float wheel;
    public float wheelO;

    public Motorcycle(EntityType<? extends Motorcycle> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FUEL, 0);
        builder.define(ENGINE, false);
    }

    public int getFuel() {
        return entityData.get(FUEL);
    }

    public void setFuel(int v) {
        entityData.set(FUEL, Mth.clamp(v, 0, MAX_FUEL));
    }

    public boolean isEngineOn() {
        return entityData.get(ENGINE);
    }

    private void setEngine(boolean on) {
        entityData.set(ENGINE, on);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setFuel(tag.getInt("Fuel"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Fuel", getFuel());
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModeHud.MOTORCYCLE_ITEM.get());
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity l ? l : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float scale) {
        return new Vec3(0.0, 1.0, 0.0);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
            if (getFuel() >= MAX_FUEL - 100) {
                if (!level().isClientSide) player.displayClientMessage(Component.translatable("message.modehud.tank_full"), true);
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (!level().isClientSide) {
                setFuel(getFuel() + COAL_FUEL);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level().playSound(null, blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.NEUTRAL, 1.0f, 0.9f);
                player.displayClientMessage(Component.translatable("message.modehud.fuel", getFuel() * 100 / MAX_FUEL), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level().isClientSide) return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (level().isClientSide) return;
        if (getFuel() > 0) {
            setEngine(true);
            level().playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 0.8f, 0.7f);
        } else if (passenger instanceof Player p) {
            p.displayClientMessage(Component.translatable("message.modehud.need_coal"), true);
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide) setEngine(false);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || isRemoved()) return false;
        if (!level().isClientSide && source.getEntity() instanceof Player p) {
            if (!p.getAbilities().instabuild) spawnAtLocation(new ItemStack(ModeHud.MOTORCYCLE_ITEM.get()));
            discard();
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        leanO = lean;
        pitchO = pitch;
        wheelO = wheel;

        LivingEntity rider = getControllingPassenger();
        boolean authority = rider != null ? isControlledByLocalInstance() : !level().isClientSide;
        double px = getX();
        double pz = getZ();

        if (authority) {
            drive(rider);
        } else if (rider != null) {
            setDeltaMovement(Vec3.ZERO);
        }

        if (!level().isClientSide) serverEngine(rider);
        visuals(px, pz);
    }

    private void drive(LivingEntity rider) {
        float throttle = 0f;
        float steer = 0f;
        if (rider != null) {
            throttle = rider.zza;
            steer = rider.xxa;
        }
        boolean on = isEngineOn() && getFuel() > 0;

        if (on) {
            if (throttle > 0) speed += 0.028 * throttle;
            else if (throttle < 0) speed += (speed > 0 ? 0.05 : 0.012) * throttle;
        }
        if (onGround()) speed *= on && throttle != 0 ? 0.988 : 0.965;
        else speed *= 0.995;
        if (Math.abs(speed) < 0.003 && throttle == 0) speed = 0;
        speed = Mth.clamp(speed, -MAX_REVERSE, MAX_SPEED);

        double rate = Math.min(1.0, Math.abs(speed) / 0.12) * (1.0 - 0.45 * Math.abs(speed) / MAX_SPEED);
        setYRot(getYRot() - (float) (steer * 4.2 * rate) * (speed >= 0 ? 1f : -1f));

        double yaw = Math.toRadians(getYRot());
        double vy = (getDeltaMovement().y - 0.08) * 0.98;
        if (onGround() && vy < 0) vy = -0.01;
        Vec3 v = new Vec3(-Math.sin(yaw) * speed, vy, Math.cos(yaw) * speed);
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        if (horizontalCollision) speed *= 0.5;
    }

    private void serverEngine(LivingEntity rider) {
        if (rider == null || !isEngineOn()) return;
        fuelAcc += Math.abs(rider.zza) > 0.01f ? 1f : 0.25f;
        while (fuelAcc >= 1f) {
            fuelAcc -= 1f;
            setFuel(getFuel() - 1);
        }
        if (getFuel() <= 0) {
            setEngine(false);
            if (rider instanceof ServerPlayer p) p.displayClientMessage(Component.translatable("message.modehud.out_of_fuel"), true);
            return;
        }
        if (tickCount % 4 == 0) {
            float pitch = 0.55f + (float) Math.abs(speed) * 0.5f;
            level().playSound(null, blockPosition(), SoundEvents.PISTON_CONTRACT, SoundSource.NEUTRAL, 0.12f, pitch);
        }
    }

    private double groundY(double x, double z) {
        BlockPos start = BlockPos.containing(x, getY() + 1.0, z);
        for (int i = 0; i < 4; i++) {
            BlockPos pos = start.below(i);
            BlockState state = level().getBlockState(pos);
            if (!state.getCollisionShape(level(), pos).isEmpty()) {
                return pos.getY() + state.getCollisionShape(level(), pos).max(Direction.Axis.Y);
            }
        }
        return getY() - 1.0;
    }

    private void visuals(double px, double pz) {
        double moved = Math.hypot(getX() - px, getZ() - pz);
        float dyaw = Mth.wrapDegrees(getYRot() - yRotO);
        float leanTarget = Mth.clamp(dyaw * (float) (moved * 6.0), -24f, 24f);
        lean += (leanTarget - lean) * 0.25f;

        double yaw = Math.toRadians(getYRot());
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double front = groundY(getX() + fx * 0.8, getZ() + fz * 0.8);
        double rear = groundY(getX() - fx * 0.8, getZ() - fz * 0.8);
        float pitchTarget = Mth.clamp((float) Math.toDegrees(Math.atan2(front - rear, 1.6)), -35f, 35f);
        pitch += (pitchTarget - pitch) * 0.3f;

        wheel += (float) (moved / 0.4375) * (speed >= 0 || level().isClientSide ? 1f : -1f);

        if (level().isClientSide && isEngineOn() && tickCount % 3 == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX() - fx * 1.0 + 0.2, getY() + 0.35, getZ() - fz * 1.0, 0.0, 0.02, 0.0);
        }
    }
}
