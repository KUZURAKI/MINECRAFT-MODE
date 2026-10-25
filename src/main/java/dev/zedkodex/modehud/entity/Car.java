package dev.zedkodex.modehud.entity;

import dev.zedkodex.modehud.ModeHud;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Car extends FuelVehicle {
    private static final double MAX_SPEED = 0.8;
    private static final double MAX_REVERSE = 0.22;

    private double speed;

    public float steer;
    public float steerO;
    public float pitch;
    public float pitchO;
    public float roll;
    public float rollO;
    public float wheel;
    public float wheelO;

    public Car(EntityType<? extends Car> type, Level level) {
        super(type, level);
    }

    @Override
    protected int maxFuel() {
        return 9600;
    }

    @Override
    protected int coalFuel() {
        return 1800;
    }

    @Override
    protected Item dropItem() {
        return ModeHud.CAR_ITEM.get();
    }

    @Override
    protected int seats() {
        return 2;
    }

    @Override
    protected Vec3 seatLocal(int index) {
        return new Vec3(index == 0 ? -0.4725 : 0.4725, 0.42, -0.02);
    }

    @Override
    protected float burnPerTick(LivingEntity rider) {
        return Math.abs(rider.zza) > 0.01f ? 1f : 0.3f;
    }

    @Override
    public void tick() {
        super.tick();
        steerO = steer;
        pitchO = pitch;
        rollO = roll;
        wheelO = wheel;

        LivingEntity rider = getControllingPassenger();
        boolean authority = rider != null ? isControlledByLocalInstance() : !level().isClientSide;
        double px = getX();
        double pz = getZ();

        if (authority) drive(rider);
        else if (rider != null) setDeltaMovement(Vec3.ZERO);

        if (!level().isClientSide) {
            serverEngine(rider);
            if (rider != null && isEngineOn() && tickCount % 5 == 0) {
                level().playSound(null, blockPosition(), SoundEvents.PISTON_CONTRACT, SoundSource.NEUTRAL, 0.14f,
                        0.45f + (float) Math.abs(speed) * 0.4f);
            }
        }
        visuals(px, pz);
    }

    private void drive(LivingEntity rider) {
        float throttle = 0f;
        float input = 0f;
        if (rider != null) {
            throttle = rider.zza;
            input = rider.xxa;
        }
        boolean on = isEngineOn() && getFuel() > 0;

        if (on) {
            if (throttle > 0) speed += 0.032 * throttle;
            else if (throttle < 0) speed += (speed > 0.02 ? 0.06 : 0.02) * throttle;
        }
        speed *= onGround() ? (on && throttle != 0 ? 0.992 : 0.96) : 0.996;
        if (Math.abs(speed) < 0.003 && throttle == 0) speed = 0;
        speed = Mth.clamp(speed, -MAX_REVERSE, MAX_SPEED);

        double rate = Math.min(1.0, Math.abs(speed) / 0.1) * (1.0 - 0.4 * Math.abs(speed) / MAX_SPEED);
        setYRot(getYRot() - (float) (input * 3.4 * rate) * (speed >= 0 ? 1f : -1f));

        double yaw = Math.toRadians(getYRot());
        double vy = (getDeltaMovement().y - 0.08) * 0.98;
        if (onGround() && vy < 0) vy = -0.01;
        Vec3 v = new Vec3(-Math.sin(yaw) * speed, vy, Math.cos(yaw) * speed);
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        if (horizontalCollision) speed *= 0.5;
    }

    private void visuals(double px, double pz) {
        double moved = Math.hypot(getX() - px, getZ() - pz);
        float dyaw = Mth.wrapDegrees(getYRot() - yRotO);
        steer = fin(steer + (Mth.clamp(dyaw * 6f, -28f, 28f) - steer) * 0.3f);
        roll = fin(roll + (Mth.clamp(dyaw * (float) (moved * 2.2), -6f, 6f) - roll) * 0.2f);

        double yaw = Math.toRadians(getYRot());
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double front = groundY(getX() + fx * 1.8, getZ() + fz * 1.8, getY() + 1.2);
        double rear = groundY(getX() - fx * 1.8, getZ() - fz * 1.8, getY() + 1.2);
        float target = Mth.clamp((float) Math.toDegrees(Math.atan2(front - rear, 3.6)), -30f, 30f);
        pitch = fin(pitch + (target - pitch) * 0.25f);

        wheel = fin(wheel + (float) (moved / 0.5) * (speed >= 0 || level().isClientSide ? 1f : -1f));

        if (level().isClientSide && isEngineOn() && tickCount % 4 == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX() - fx * 2.2, getY() + 0.35, getZ() - fz * 2.2, 0.0, 0.02, 0.0);
        }
    }

    private static float fin(float v) {
        return Float.isFinite(v) ? v : 0f;
    }
}
