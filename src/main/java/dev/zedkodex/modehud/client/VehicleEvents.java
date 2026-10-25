package dev.zedkodex.modehud.client;

import dev.zedkodex.modehud.ModeHud;
import dev.zedkodex.modehud.entity.Plane;
import dev.zedkodex.modehud.net.ActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ModeHud.MODID, value = Dist.CLIENT)
public class VehicleEvents {
    private static void send(int action) {
        PacketDistributor.sendToServer(new ActionPayload(action));
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        while (ModKeys.OPEN_BACKPACK.consumeClick()) send(ActionPayload.OPEN_BACKPACK);
        while (ModKeys.TAKE_OFF_BACKPACK.consumeClick()) send(ActionPayload.TAKE_OFF_BACKPACK);
        while (ModKeys.EXIT_VEHICLE.consumeClick()) send(ActionPayload.EXIT_VEHICLE);
        if (mc.screen == null && p.getVehicle() instanceof Plane && mc.options.keyAttack.isDown()) send(ActionPayload.PRIMARY);
    }

    @SubscribeEvent
    static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || !(p.getVehicle() instanceof Plane)) return;
        if (event.isUseItem()) send(ActionPayload.SECONDARY);
        if (event.isAttack() || event.isUseItem() || event.isPickBlock()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onMove(MovementInputUpdateEvent event) {
        if (event.getEntity().getVehicle() instanceof Plane) event.getInput().shiftKeyDown = false;
    }
}
