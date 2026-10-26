package dev.zedkodex.modehud.server;

import dev.zedkodex.modehud.ModeHud;
import dev.zedkodex.modehud.data.BackpackData;
import dev.zedkodex.modehud.net.ActionPayload;
import dev.zedkodex.modehud.net.BackpackPayload;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.network.PacketDistributor;

public final class Actions {
    private Actions() {}

    public static void handle(ServerPlayer p, int action) {
        switch (action) {
            case ActionPayload.OPEN_BACKPACK -> openBackpack(p);
            case ActionPayload.TAKE_OFF_BACKPACK -> takeOffBackpack(p);
            default -> {
            }
        }
    }

    public static void syncBackpack(ServerPlayer p) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new BackpackPayload(p.getId(), p.getData(ModeHud.BACKPACK).worn()));
    }

    private static void openBackpack(ServerPlayer p) {
        BackpackData data = p.getData(ModeHud.BACKPACK);
        if (!data.worn()) {
            p.displayClientMessage(Component.translatable("message.modehud.no_backpack"), true);
            return;
        }
        ItemStack bag = data.stack;
        SimpleContainer c = new SimpleContainer(27);
        bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(c.getItems());
        c.addListener(changed -> bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(c.getItems())));
        p.level().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
        p.openMenu(new SimpleMenuProvider((id, inv, player) -> ChestMenu.threeRows(id, inv, c), Component.translatable("item.modehud.backpack")));
    }

    private static void takeOffBackpack(ServerPlayer p) {
        BackpackData data = p.getData(ModeHud.BACKPACK);
        if (!data.worn()) return;
        ItemStack bag = data.stack;
        data.stack = ItemStack.EMPTY;
        if (!p.getInventory().add(bag)) p.drop(bag, false);
        p.level().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
        syncBackpack(p);
    }
}
