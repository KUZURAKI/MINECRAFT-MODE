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

public class Motorcycle extends FuelVehicle {
    private static final double MAX_SPEED = 0.55;
    private static final double MAX_REVERSE = 0.16;

    private double speed;

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
    protected int maxFuel() {
        return 6400;
    }

    @Override
    protected int coalFuel() {
        return 1600;
    }

    @Override
    protected Item dropItem() {
        return ModeHud.MOTORCYCLE_ITEM.get();
    }

    @Override
    protected int seats() {
        return 1;
    }

    @Override
    protected Vec3 seatLocal(int index) {
        return new Vec3(0.0, 1.0, 0.0);
    }

    @Override
    protected float burnPerTick(LivingEntity rider) {
        return Math.abs(rider.zza) > 0.01f ? 1f : 0.25f;
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

        if (authority) drive(rider);
        else if (rider != null) setDeltaMovement(Vec3.ZERO);

        if (!level().isClientSide) {
            serverEngine(rider);
            if (rider != null && isEngineOn() && tickCount % 4 == 0) {
                level().playSound(null, blockPosition(), SoundEvents.PISTON_CONTRACT, SoundSource.NEUTRAL, 0.12f,
                        0.55f + (float) Math.abs(speed) * 0.5f);
            }
        }
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

    private void visuals(double px, double pz) {
        double moved = Math.hypot(getX() - px, getZ() - pz);
        float dyaw = Mth.wrapDegrees(getYRot() - yRotO);
        float leanTarget = Mth.clamp(dyaw * (float) (moved * 6.0), -24f, 24f);
        lean = finite(lean + (leanTarget - lean) * 0.25f);

        double yaw = Math.toRadians(getYRot());
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double front = groundY(getX() + fx * 0.8, getZ() + fz * 0.8, getY() + 1.0);
        double rear = groundY(getX() - fx * 0.8, getZ() - fz * 0.8, getY() + 1.0);
        float pitchTarget = Mth.clamp((float) Math.toDegrees(Math.atan2(front - rear, 1.6)), -35f, 35f);
        pitch = finite(pitch + (pitchTarget - pitch) * 0.3f);

        wheel = finite(wheel + (float) (moved / 0.4375) * (speed >= 0 || level().isClientSide ? 1f : -1f));

        if (level().isClientSide && isEngineOn() && tickCount % 3 == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX() - fx * 1.0 + 0.2, getY() + 0.35, getZ() - fz * 1.0, 0.0, 0.02, 0.0);
        }
    }

    private static float finite(float v) {
        return Float.isFinite(v) ? v : 0f;
    }
}
