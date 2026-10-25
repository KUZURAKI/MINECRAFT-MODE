package dev.zedkodex.modehud.item;

import dev.zedkodex.modehud.ModeHud;
import dev.zedkodex.modehud.entity.Motorcycle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public class MotorcycleItem extends Item {
    public MotorcycleItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        Motorcycle bike = ModeHud.MOTORCYCLE.get().create(level);
        if (bike == null) return InteractionResult.FAIL;
        Player player = ctx.getPlayer();
        Vec3 at = Vec3.atBottomCenterOf(pos);
        bike.moveTo(at.x, at.y, at.z, player != null ? player.getYRot() : 0f, 0f);
        if (!level.noCollision(bike, bike.getBoundingBox())) return InteractionResult.FAIL;
        level.addFreshEntity(bike);
        if (player == null || !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
