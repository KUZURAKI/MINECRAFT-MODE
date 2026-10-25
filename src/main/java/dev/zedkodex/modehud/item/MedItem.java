package dev.zedkodex.modehud.item;

import dev.zedkodex.modehud.Config;
import dev.zedkodex.modehud.ModeHud;
import dev.zedkodex.modehud.data.Vitals;
import dev.zedkodex.modehud.net.BandagePayload;
import dev.zedkodex.modehud.net.ClientState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class MedItem extends Item {
    public enum Kind { PILLS, BANDAGE }

    private final Kind kind;

    public MedItem(Properties props, Kind kind) {
        super(props);
        this.kind = kind;
    }

    private int wrapTicks() {
        return Config.COMMON.bandageWrapTicks.get();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return kind == Kind.PILLS ? UseAnim.EAT : UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return kind == Kind.PILLS ? 20 : wrapTicks() * 3;
    }

    private boolean canUse(Level level, Player player) {
        if (kind == Kind.PILLS) return player.getHealth() < player.getMaxHealth();
        int bleed = level.isClientSide ? ClientState.bleed : player.getData(ModeHud.VITALS).bleed;
        return bleed > 0 || player.getHealth() < player.getMaxHealth();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canUse(level, player)) return InteractionResultHolder.fail(stack);
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (kind != Kind.BANDAGE || level.isClientSide) return;
        int used = getUseDuration(stack, entity) - remaining;
        if (used % wrapTicks() == 0) {
            level.playSound(null, entity.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.8f, 1.3f);
        }
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
                v.healTicks = Config.COMMON.bandageHealTicks.get();
                v.healLeft = Config.COMMON.bandageHeal.get();
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new BandagePayload(p.getId(), v.healTicks));
            }
            v.fx(Vitals.FX_HEAL);
            if (!p.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }
}
