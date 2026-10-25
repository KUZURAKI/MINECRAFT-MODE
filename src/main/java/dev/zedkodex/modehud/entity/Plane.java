package dev.zedkodex.modehud.entity;

import dev.zedkodex.modehud.client.PlaneInput;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class Plane extends FuelVehicle {
    protected double speed;
    protected float throttle;
    protected int cooldownA;
    protected int cooldownB;
    protected double serverSpeed;

    public float roll;
    public float rollO;

    private double lastX;
    private double lastY;
    private double lastZ;

    protected Plane(EntityType<?> type, Level level) {
        super(type, level);
    }

    protected abstract double maxSpeed();

    protected abstract double takeoffSpeed();

    protected abstract double stallSpeed();

    protected abstract float pitchRate();

    protected abstract float yawRate();

    public abstract void primary(ServerPlayer rider);

    public abstract void secondary(ServerPlayer rider);

    public float throttle() {
        return throttle;
    }

    @Override
    protected float burnPerTick(LivingEntity rider) {
        return 0.25f + Math.max(0f, throttle) * 1.6f;
    }

    protected Vec3 forward() {
        double yaw = Math.toRadians(getYRot());
        double pitch = Math.toRadians(getXRot());
        double cp = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * cp, -Math.sin(pitch), Math.cos(yaw) * cp);
    }

    protected Vec3 pointToWorld(double lx, double ly, double lz) {
        return position().add(toWorld(new Vec3(lx, ly, lz)));
    }

    public double measuredSpeed() {
        return serverSpeed;
    }

    public void crash() {
        if (level().isClientSide || isRemoved()) return;
        level().explode(this, getX(), getY() + 0.5, getZ(), 3.0f, Level.ExplosionInteraction.NONE);
        ejectPassengers();
        discard();
    }

    public boolean mayExit() {
        return onGround() || getY() - groundY(getX(), getZ(), getY()) < 2.5;
    }

    @Override
    public void tick() {
        super.tick();
        rollO = roll;
        if (cooldownA > 0) cooldownA--;
        if (cooldownB > 0) cooldownB--;

        LivingEntity rider = getControllingPassenger();
        boolean authority = rider != null ? isControlledByLocalInstance() : !level().isClientSide;
        if (authority) fly(rider);
        else if (rider != null) setDeltaMovement(Vec3.ZERO);

        if (!level().isClientSide) {
            serverSpeed = Math.sqrt((getX() - lastX) * (getX() - lastX) + (getY() - lastY) * (getY() - lastY) + (getZ() - lastZ) * (getZ() - lastZ));
            serverEngine(rider);
            if (rider != null && isEngineOn() && tickCount % 4 == 0) {
                level().playSound(null, blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.NEUTRAL, 0.5f, 0.6f + (float) Math.abs(speed) * 0.4f);
            }
        }
        lastX = getX();
        lastY = getY();
        lastZ = getZ();

        float dyaw = Mth.wrapDegrees(getYRot() - yRotO);
        roll += (Mth.clamp(dyaw * 14f, -45f, 45f) - roll) * 0.18f;
        if (!Float.isFinite(roll)) roll = 0f;

        if (level().isClientSide && isEngineOn() && tickCount % 2 == 0) {
            Vec3 back = pointToWorld(0, 0.6, 4.0);
            level().addParticle(ParticleTypes.FLAME, back.x, back.y, back.z, 0.0, 0.0, 0.0);
            level().addParticle(ParticleTypes.SMOKE, back.x, back.y, back.z, 0.0, 0.02, 0.0);
        }
    }

    private void fly(LivingEntity rider) {
        boolean on = isEngineOn() && getFuel() > 0;
        float pitchIn = 0f;
        float yawIn = 0f;
        boolean thrust = false;
        boolean brake = false;
        if (rider != null) {
            pitchIn = rider.zza;
            yawIn = rider.xxa;
            if (level().isClientSide) {
                thrust = PlaneInput.thrust();
                brake = PlaneInput.brake();
            }
        }

        if (on) {
            if (thrust) throttle = Math.min(1f, throttle + 0.015f);
            if (brake) throttle = Math.max(-0.25f, throttle - 0.03f);
        } else {
            throttle = Math.max(0f, throttle - 0.02f);
        }
        double target = throttle >= 0 ? throttle * maxSpeed() : throttle * maxSpeed() * 0.5;
        speed += (target - speed) * (onGround() ? 0.025 : 0.018);
        if (!on) speed *= 0.985;
        if (Math.abs(speed) < 0.002 && target == 0) speed = 0;
        speed = Mth.clamp(speed, -0.22, maxSpeed());

        boolean airborne = !onGround();
        float xr = getXRot();
        if (pitchIn != 0f && (airborne || speed >= takeoffSpeed())) {
            xr += pitchIn * pitchRate();
        } else if (!airborne) {
            xr = xr + (0f - xr) * 0.3f;
        } else {
            xr = xr + (0f - xr) * 0.01f;
        }
        if (airborne && speed < stallSpeed()) xr += (float) ((stallSpeed() - speed) / stallSpeed()) * 2.8f;
        xr = Mth.clamp(xr, -55f, 55f);
        setXRot(xr);

        double steerFactor = airborne ? Math.min(1.0, speed / (maxSpeed() * 0.5)) : Math.min(1.0, speed / 0.25);
        if (speed < 0) steerFactor = -Math.min(1.0, Math.abs(speed) / 0.15);
        setYRot(getYRot() - (float) (yawIn * yawRate() * steerFactor));

        Vec3 dir = forward();
        double vy = dir.y * speed;
        if (!airborne) {
            if (dir.y > 0 && speed < takeoffSpeed()) vy = 0;
            if (dir.y <= 0) vy = -0.08;
        } else if (speed < stallSpeed()) {
            vy -= (stallSpeed() - speed) / stallSpeed() * 0.22;
        }
        if (getY() > level().getMaxBuildHeight() - 6 && vy > 0) vy = 0;

        Vec3 v = new Vec3(dir.x * speed, vy, dir.z * speed);
        setDeltaMovement(v);
        move(MoverType.SELF, v);

        if ((horizontalCollision || verticalCollision && v.y < -0.5) && speed > 0.55 && level().isClientSide) {
            PlaneInput.crash();
            speed = 0;
        } else if (horizontalCollision) {
            speed *= 0.4;
        }
    }
}
