package dev.zedkodex.modehud.item;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public class VehicleItem extends Item {
    private final Supplier<? extends EntityType<? extends Entity>> type;

    public VehicleItem(Properties props, Supplier<? extends EntityType<? extends Entity>> type) {
        super(props);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        Entity e = type.get().create(level);
        if (e == null) return InteractionResult.FAIL;
        Player player = ctx.getPlayer();
        Vec3 at = Vec3.atBottomCenterOf(pos);
        e.moveTo(at.x, at.y, at.z, player != null ? player.getYRot() : 0f, 0f);
        if (!level.noCollision(e, e.getBoundingBox())) return InteractionResult.FAIL;
        level.addFreshEntity(e);
        if (player == null || !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
