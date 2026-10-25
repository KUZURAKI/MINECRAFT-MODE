package dev.zedkodex.modehud.entity;

import dev.zedkodex.modehud.ModeHud;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Bomber extends Plane {
    public Bomber(EntityType<? extends Bomber> type, Level level) {
        super(type, level);
    }

    @Override
    protected int maxFuel() {
        return 19200;
    }

    @Override
    protected int coalFuel() {
        return 1600;
    }

    @Override
    protected Item dropItem() {
        return ModeHud.BOMBER_ITEM.get();
    }

    @Override
    protected int seats() {
        return 1;
    }

    @Override
    protected Vec3 seatLocal(int index) {
        return new Vec3(0.0, 1.45, -3.8);
    }

    @Override
    protected double maxSpeed() {
        return 1.2;
    }

    @Override
    protected double takeoffSpeed() {
        return 0.65;
    }

    @Override
    protected double stallSpeed() {
        return 0.45;
    }

    @Override
    protected float pitchRate() {
        return 1.1f;
    }

    @Override
    protected float yawRate() {
        return 1.6f;
    }

    @Override
    public void primary(ServerPlayer rider) {
        secondary(rider);
    }

    @Override
    public void secondary(ServerPlayer rider) {
        if (cooldownB > 0 || !isEngineOn() || !(level() instanceof ServerLevel sl)) return;
        cooldownB = 12;
        Vec3 at = pointToWorld(0, -0.2, -2.0);
        Bomb b = ModeHud.BOMB.get().create(sl);
        if (b == null) return;
        b.setOwner(rider);
        b.setPos(at);
        b.setDeltaMovement(getDeltaMovement().scale(0.9));
        sl.addFreshEntity(b);
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.PISTON_EXTEND, SoundSource.NEUTRAL, 1.0f, 0.6f);
    }
}
