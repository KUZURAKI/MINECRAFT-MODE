package dev.zedkodex.modehud.client;

import dev.zedkodex.modehud.ModeHud;
import dev.zedkodex.modehud.net.ActionPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ModeHud.MODID, value = Dist.CLIENT)
public class BackpackKeys {
    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) return;
        while (ModKeys.OPEN_BACKPACK.consumeClick()) PacketDistributor.sendToServer(new ActionPayload(ActionPayload.OPEN_BACKPACK));
        while (ModKeys.TAKE_OFF_BACKPACK.consumeClick()) PacketDistributor.sendToServer(new ActionPayload(ActionPayload.TAKE_OFF_BACKPACK));
    }
}
