package dev.zedkodex.modehud.item;

import dev.zedkodex.modehud.Config;
import dev.zedkodex.modehud.ModeHud;
import dev.zedkodex.modehud.data.Vitals;
import dev.zedkodex.modehud.net.ClientState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class MedItem extends Item {
    public enum Kind { PILLS, BANDAGE }

    private final Kind kind;

    public MedItem(Properties props, Kind kind) {
        super(props);
        this.kind = kind;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return kind == Kind.PILLS ? 20 : 32;
    }

    private boolean canUse(Level level, Player player) {
        if (kind == Kind.PILLS) return player.getHealth() < player.getMaxHealth();
        int bleed = level.isClientSide ? ClientState.bleed : player.getData(ModeHud.VITALS).bleed;
        return bleed > 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canUse(level, player)) return InteractionResultHolder.fail(stack);
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer p) {
            Vitals v = p.getData(ModeHud.VITALS);
            if (kind == Kind.PILLS) {
                p.heal(Config.COMMON.pillsHeal.get());
            } else {
                v.bleed = 0;
                v.clotTimer = 0;
                v.bleedTimer = 0;
            }
            v.fx(Vitals.FX_HEAL);
            if (!p.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }
}
