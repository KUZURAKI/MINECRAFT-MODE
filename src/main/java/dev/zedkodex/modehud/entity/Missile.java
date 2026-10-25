package dev.zedkodex.modehud.entity;

import dev.zedkodex.modehud.Config;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Missile extends Projectile implements ItemSupplier {
    private int life;

    public Missile(EntityType<? extends Missile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.FIREWORK_ROCKET);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            explode();
            return;
        }
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.FLAME, getX() - v.x * 0.5, getY() - v.y * 0.5, getZ() - v.z * 0.5, 0, 0, 0);
            level().addParticle(ParticleTypes.SMOKE, getX() - v.x, getY() - v.y, getZ() - v.z, 0, 0.01, 0);
        } else if (++life > 120) {
            explode();
        }
    }

    private void explode() {
        if (level().isClientSide) return;
        Level.ExplosionInteraction mode = Config.COMMON.missileBreaksBlocks.get() ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.MOB;
        level().explode(this, getX(), getY(), getZ(), Config.COMMON.missilePower.get().floatValue(), mode);
        discard();
    }
}
