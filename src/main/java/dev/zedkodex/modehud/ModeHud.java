package dev.zedkodex.modehud;

import java.util.function.Supplier;

import dev.zedkodex.modehud.data.BackpackData;
import dev.zedkodex.modehud.data.Vitals;
import dev.zedkodex.modehud.item.BackpackItem;
import dev.zedkodex.modehud.item.MedItem;
import dev.zedkodex.modehud.net.ActionPayload;
import dev.zedkodex.modehud.net.BackpackPayload;
import dev.zedkodex.modehud.net.BandagePayload;
import dev.zedkodex.modehud.net.ClientState;
import dev.zedkodex.modehud.net.PortalPayload;
import dev.zedkodex.modehud.net.VitalsPayload;
import dev.zedkodex.modehud.server.Actions;
import dev.zedkodex.modehud.world.ModBlocks;
import dev.zedkodex.modehud.world.SmoothTerrainFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(ModeHud.MODID)
public class ModeHud {
    public static final String MODID = "modehud";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MODID);

    public static final DeferredItem<Item> PILLS = ITEMS.register("pills",
            () -> new MedItem(new Item.Properties().stacksTo(16), MedItem.Kind.PILLS));
    public static final DeferredItem<Item> BANDAGE = ITEMS.register("bandage",
            () -> new MedItem(new Item.Properties().stacksTo(16), MedItem.Kind.BANDAGE));
    public static final DeferredItem<Item> BACKPACK_ITEM = ITEMS.register("backpack",
            () -> new BackpackItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<AttachmentType<Vitals>> VITALS = ATTACHMENTS.register("vitals",
            () -> AttachmentType.serializable(Vitals::new).build());
    public static final Supplier<AttachmentType<BackpackData>> BACKPACK = ATTACHMENTS.register("backpack",
            () -> AttachmentType.serializable(BackpackData::new).build());

    public static final Supplier<Feature<NoneFeatureConfiguration>> SMOOTH_TERRAIN = FEATURES.register("smooth_terrain",
            () -> new SmoothTerrainFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID))
                    .icon(() -> new ItemStack(PILLS.get()))
                    .displayItems((params, out) -> {
                        out.accept(PILLS.get());
                        out.accept(BANDAGE.get());
                        out.accept(BACKPACK_ITEM.get());
                        for (Item i : ModBlocks.allItems()) out.accept(i);
                    })
                    .build());

    public ModeHud(IEventBus bus, ModContainer container) {
        ITEMS.register(bus);
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.ITEMS.register(bus);
        TABS.register(bus);
        ATTACHMENTS.register(bus);
        FEATURES.register(bus);
        bus.addListener(ModeHud::registerPayloads);
        container.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var reg = event.registrar("1");
        reg.playToClient(VitalsPayload.TYPE, VitalsPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientState.apply(payload)));
        reg.playToClient(PortalPayload.TYPE, PortalPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientState.portals.add(payload.kind())));
        reg.playToClient(BandagePayload.TYPE, BandagePayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (payload.ticks() <= 0) ClientState.bandaged.remove(payload.entityId());
                    else ClientState.bandaged.put(payload.entityId(), System.currentTimeMillis() + payload.ticks() * 50L);
                }));
        reg.playToClient(BackpackPayload.TYPE, BackpackPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (payload.worn()) ClientState.backpacks.add(payload.entityId());
                    else ClientState.backpacks.remove(payload.entityId());
                }));
        reg.playToServer(ActionPayload.TYPE, ActionPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) Actions.handle(sp, payload.action());
                }));
    }
}
