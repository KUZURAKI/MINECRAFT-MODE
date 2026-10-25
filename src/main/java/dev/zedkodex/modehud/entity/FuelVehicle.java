package dev.zedkodex.modehud.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class FuelVehicle extends Entity {
    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(FuelVehicle.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ENGINE = SynchedEntityData.defineId(FuelVehicle.class, EntityDataSerializers.BOOLEAN);

    private float fuelAcc;

    protected FuelVehicle(EntityType<?> type, Level level) {
        super(type, level);
    }

    protected abstract int maxFuel();

    protected abstract int coalFuel();

    protected abstract Item dropItem();

    protected abstract int seats();

    protected abstract Vec3 seatLocal(int index);

    protected abstract float burnPerTick(LivingEntity rider);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FUEL, 0);
        builder.define(ENGINE, false);
    }

    public int getFuel() {
        return entityData.get(FUEL);
    }

    public void setFuel(int v) {
        entityData.set(FUEL, Mth.clamp(v, 0, maxFuel()));
    }

    public float fuelFraction() {
        return getFuel() / (float) maxFuel();
    }

    public boolean isEngineOn() {
        return entityData.get(ENGINE);
    }

    protected void setEngine(boolean on) {
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
        return 1.1F;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(dropItem());
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity l ? l : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < seats();
    }

    protected Vec3 toWorld(Vec3 l) {
        double yaw = Math.toRadians(getYRot());
        return new Vec3(-l.x * Math.cos(yaw) + l.z * Math.sin(yaw), l.y, -l.x * Math.sin(yaw) - l.z * Math.cos(yaw));
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float scale) {
        int idx = Math.max(0, getPassengers().indexOf(passenger));
        return toWorld(seatLocal(Math.min(idx, seats() - 1)));
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
            if (getFuel() >= maxFuel() - 100) {
                if (!level().isClientSide) player.displayClientMessage(Component.translatable("message.modehud.tank_full"), true);
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (!level().isClientSide) {
                setFuel(getFuel() + coalFuel());
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level().playSound(null, blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.NEUTRAL, 1.0f, 0.9f);
                player.displayClientMessage(Component.translatable("message.modehud.fuel", getFuel() * 100 / maxFuel()), true);
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
        if (level().isClientSide || passenger != getControllingPassenger()) return;
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
        if (!level().isClientSide && getControllingPassenger() == null) setEngine(false);
    }

    protected void onWrecked(DamageSource source) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || isRemoved()) return false;
        if (!level().isClientSide && source.getEntity() instanceof Player p) {
            if (!p.getAbilities().instabuild) spawnAtLocation(new ItemStack(dropItem()));
            ejectPassengers();
            discard();
            return true;
        }
        return false;
    }

    protected void serverEngine(LivingEntity rider) {
        if (rider == null || !isEngineOn()) return;
        fuelAcc += burnPerTick(rider);
        while (fuelAcc >= 1f) {
            fuelAcc -= 1f;
            setFuel(getFuel() - 1);
        }
        if (getFuel() <= 0) {
            setEngine(false);
            if (rider instanceof ServerPlayer p) p.displayClientMessage(Component.translatable("message.modehud.out_of_fuel"), true);
        }
    }

    protected double groundY(double x, double z, double from) {
        BlockPos start = BlockPos.containing(x, from, z);
        for (int i = 0; i < 5; i++) {
            BlockPos pos = start.below(i);
            BlockState state = level().getBlockState(pos);
            if (!state.getCollisionShape(level(), pos).isEmpty()) {
                return pos.getY() + state.getCollisionShape(level(), pos).max(Direction.Axis.Y);
            }
        }
        return getY() - 2.0;
    }
}
