package dev.zedkodex.modehud.item;

import dev.zedkodex.modehud.ModeHud;
import dev.zedkodex.modehud.data.BackpackData;
import dev.zedkodex.modehud.server.Actions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BackpackItem extends Item {
    public BackpackItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(held);
        BackpackData data = player.getData(ModeHud.BACKPACK);
        if (data.worn()) {
            player.displayClientMessage(Component.translatable("message.modehud.backpack_worn"), true);
            return InteractionResultHolder.fail(held);
        }
        data.stack = held.copyWithCount(1);
        held.shrink(1);
        level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0f, 0.8f);
        if (player instanceof ServerPlayer sp) Actions.syncBackpack(sp);
        return InteractionResultHolder.consume(held);
    }
}
