package dev.zedkodex.modehud.entity;

import dev.zedkodex.modehud.Config;
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

public class Bomb extends Projectile implements ItemSupplier {
    private int life;

    public Bomb(EntityType<? extends Bomb> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.TNT);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement().add(0, -0.06, 0).scale(0.995);
        setDeltaMovement(v);
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            explode();
            return;
        }
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        if (!level().isClientSide && ++life > 400) explode();
    }

    private void explode() {
        if (level().isClientSide) return;
        Level.ExplosionInteraction mode = Config.COMMON.bombBreaksBlocks.get() ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.MOB;
        level().explode(this, getX(), getY(), getZ(), Config.COMMON.bombPower.get().floatValue(), mode);
        discard();
    }
}
