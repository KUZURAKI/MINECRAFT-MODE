package dev.zedkodex.modehud.client;

import dev.zedkodex.modehud.ModeHud;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.GrassColor;
import dev.zedkodex.modehud.world.ModBlocks;

@EventBusSubscriber(modid = ModeHud.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    static void onLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BandageModel.LAYER, BandageModel::createBodyLayer);
    }

    @SubscribeEvent
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        BandageModel model = new BandageModel(event.getEntityModels().bakeLayer(BandageModel.LAYER));
        BandageModel.shared = model;
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new BandageLayer(renderer, model));
            }
        }
    }

    @SubscribeEvent
    static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "weather"),
                (g, delta) -> WeatherPanel.render(g, net.minecraft.Util.getMillis()));
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "hud"), HudRenderer::render);
    }

    @SubscribeEvent
    static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level != null && pos != null ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.getDefaultColor(),
                ModBlocks.SLABS.get("grass_slab").get());
    }

    @SubscribeEvent
    static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 0 ? GrassColor.getDefaultColor() : -1, ModBlocks.SLABS.get("grass_slab").get());
    }
}
