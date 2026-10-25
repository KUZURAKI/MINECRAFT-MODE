package dev.zedkodex.modehud.client;

import dev.zedkodex.modehud.Config;
import dev.zedkodex.modehud.ModeHud;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.minecraft.client.gui.screens.DeathScreen;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = ModeHud.MODID, value = Dist.CLIENT)
public class ClientGameEvents {
    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        HudState.tick();
    }

    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HudState.reset();
    }

    @SubscribeEvent
    static void onScreen(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof DeathScreen && !HudState.releasing && Config.CLIENT.eyesIntro.get()
                && HudState.active) {
            HudState.pendingDeath = event.getNewScreen();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onLayer(RenderGuiLayerEvent.Pre event) {
        if (!Config.CLIENT.hudEnabled.get()) return;
        ResourceLocation n = event.getName();
        if ((n.equals(VanillaGuiLayers.HOTBAR) && !HudState.spectator)
                || n.equals(VanillaGuiLayers.PLAYER_HEALTH)
                || n.equals(VanillaGuiLayers.FOOD_LEVEL)
                || n.equals(VanillaGuiLayers.AIR_LEVEL)
                || n.equals(VanillaGuiLayers.EXPERIENCE_BAR)
                || n.equals(VanillaGuiLayers.EXPERIENCE_LEVEL)
                || n.equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) {
            event.setCanceled(true);
        }
    }
}
