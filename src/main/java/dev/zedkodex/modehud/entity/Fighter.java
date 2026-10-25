package dev.zedkodex.modehud.entity;

import java.util.List;

import dev.zedkodex.modehud.Config;
import dev.zedkodex.modehud.ModeHud;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

public class Fighter extends Plane {
    private boolean leftGun;
    private boolean leftPylon;

    public Fighter(EntityType<? extends Fighter> type, Level level) {
        super(type, level);
    }

    @Override
    protected int maxFuel() {
        return 12800;
    }

    @Override
    protected int coalFuel() {
        return 1600;
    }

    @Override
    protected Item dropItem() {
        return ModeHud.FIGHTER_ITEM.get();
    }

    @Override
    protected int seats() {
        return 1;
    }

    @Override
    protected Vec3 seatLocal(int index) {
        return new Vec3(0.0, 0.95, -2.0);
    }

    @Override
    protected double maxSpeed() {
        return 1.7;
    }

    @Override
    protected double takeoffSpeed() {
        return 0.8;
    }

    @Override
    protected double stallSpeed() {
        return 0.55;
    }

    @Override
    protected float pitchRate() {
        return 1.8f;
    }

    @Override
    protected float yawRate() {
        return 2.4f;
    }

    @Override
    public void primary(ServerPlayer rider) {
        if (cooldownA > 0 || !isEngineOn() || !(level() instanceof ServerLevel sl)) return;
        cooldownA = 2;
        leftGun = !leftGun;
        Vec3 from = pointToWorld(leftGun ? -0.65 : 0.65, 1.0, -4.3);
        Vec3 dir = forward();
        Vec3 to = from.add(dir.scale(90));

        BlockHitResult block = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        AABB box = new AABB(from, end).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(sl, this, from, end, box,
                e -> e != this && !hasPassenger(e) && e.isPickable() && !(e instanceof Plane));
        if (hit != null) {
            end = hit.getLocation();
            Entity target = hit.getEntity();
            target.hurt(sl.damageSources().thrown(this, rider), Config.COMMON.planeGunDamage.get().floatValue());
        } else if (block.getType() != HitResult.Type.MISS) {
            sl.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 4, 0.1, 0.1, 0.1, 0.1);
        }
        double len = end.subtract(from).length();
        for (double d = 2; d < len; d += 3) {
            Vec3 p = from.add(dir.scale(d));
            sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        sl.playSound(null, from.x, from.y, from.z, SoundEvents.DISPENSER_LAUNCH, SoundSource.NEUTRAL, 0.6f, 1.8f);
    }

    @Override
    public void secondary(ServerPlayer rider) {
        if (cooldownB > 0 || !isEngineOn() || !(level() instanceof ServerLevel sl)) return;
        cooldownB = 40;
        leftPylon = !leftPylon;
        Vec3 at = pointToWorld(leftPylon ? -2.1 : 2.1, 0.6, 0.9);
        Missile m = ModeHud.MISSILE.get().create(sl);
        if (m == null) return;
        m.setOwner(rider);
        m.setPos(at);
        Vec3 dir = forward();
        m.setDeltaMovement(dir.scale(Math.max(1.8, speed + 1.2)));
        m.setYRot(getYRot());
        m.setXRot(getXRot());
        sl.addFreshEntity(m);
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 1.2f, 0.7f);
    }
}
