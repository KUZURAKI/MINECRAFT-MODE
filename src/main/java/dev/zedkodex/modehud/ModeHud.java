package dev.zedkodex.modehud;

import java.util.function.Supplier;

import dev.zedkodex.modehud.data.BackpackData;
import dev.zedkodex.modehud.data.Vitals;
import dev.zedkodex.modehud.entity.Bomb;
import dev.zedkodex.modehud.entity.Bomber;
import dev.zedkodex.modehud.entity.Car;
import dev.zedkodex.modehud.entity.Fighter;
import dev.zedkodex.modehud.entity.Missile;
import dev.zedkodex.modehud.entity.Motorcycle;
import dev.zedkodex.modehud.entity.Tarantula;
import dev.zedkodex.modehud.entity.WebShot;
import dev.zedkodex.modehud.item.BackpackItem;
import dev.zedkodex.modehud.item.MedItem;
import dev.zedkodex.modehud.item.VehicleItem;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
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
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MODID);

    public static final DeferredItem<Item> PILLS = ITEMS.register("pills",
            () -> new MedItem(new Item.Properties().stacksTo(16), MedItem.Kind.PILLS));
    public static final DeferredItem<Item> BANDAGE = ITEMS.register("bandage",
            () -> new MedItem(new Item.Properties().stacksTo(16), MedItem.Kind.BANDAGE));
    public static final DeferredItem<Item> BACKPACK_ITEM = ITEMS.register("backpack",
            () -> new BackpackItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<EntityType<Motorcycle>> MOTORCYCLE = ENTITIES.register("motorcycle",
            () -> EntityType.Builder.<Motorcycle>of(Motorcycle::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(10).updateInterval(1).build("motorcycle"));
    public static final Supplier<EntityType<Car>> CAR = ENTITIES.register("car",
            () -> EntityType.Builder.<Car>of(Car::new, MobCategory.MISC)
                    .sized(2.0F, 1.4F).clientTrackingRange(10).updateInterval(1).build("car"));
    public static final Supplier<EntityType<Fighter>> FIGHTER = ENTITIES.register("fighter",
            () -> EntityType.Builder.<Fighter>of(Fighter::new, MobCategory.MISC)
                    .sized(2.4F, 1.3F).clientTrackingRange(20).updateInterval(1).build("fighter"));
    public static final Supplier<EntityType<Bomber>> BOMBER = ENTITIES.register("bomber",
            () -> EntityType.Builder.<Bomber>of(Bomber::new, MobCategory.MISC)
                    .sized(4.5F, 1.6F).clientTrackingRange(24).updateInterval(1).build("bomber"));
    public static final Supplier<EntityType<Missile>> MISSILE = ENTITIES.register("missile",
            () -> EntityType.Builder.<Missile>of(Missile::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(16).updateInterval(1).build("missile"));
    public static final Supplier<EntityType<Bomb>> BOMB = ENTITIES.register("bomb",
            () -> EntityType.Builder.<Bomb>of(Bomb::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1).build("bomb"));
    public static final Supplier<EntityType<Tarantula>> TARANTULA = ENTITIES.register("tarantula",
            () -> EntityType.Builder.<Tarantula>of(Tarantula::new, MobCategory.MONSTER)
                    .sized(2.8F, 1.6F).clientTrackingRange(10).build("tarantula"));
    public static final Supplier<EntityType<WebShot>> WEB_SHOT = ENTITIES.register("web_shot",
            () -> EntityType.Builder.<WebShot>of(WebShot::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(8).updateInterval(2).build("web_shot"));

    public static final DeferredItem<Item> MOTORCYCLE_ITEM = ITEMS.register("motorcycle",
            () -> new VehicleItem(new Item.Properties().stacksTo(1), MOTORCYCLE));
    public static final DeferredItem<Item> CAR_ITEM = ITEMS.register("car",
            () -> new VehicleItem(new Item.Properties().stacksTo(1), CAR));
    public static final DeferredItem<Item> FIGHTER_ITEM = ITEMS.register("fighter",
            () -> new VehicleItem(new Item.Properties().stacksTo(1), FIGHTER));
    public static final DeferredItem<Item> BOMBER_ITEM = ITEMS.register("bomber",
            () -> new VehicleItem(new Item.Properties().stacksTo(1), BOMBER));
    public static final DeferredItem<Item> TARANTULA_EGG = ITEMS.register("tarantula_spawn_egg",
            () -> new DeferredSpawnEggItem(TARANTULA, 0x2A2220, 0xB02020, new Item.Properties()));

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
                        out.accept(MOTORCYCLE_ITEM.get());
                        out.accept(CAR_ITEM.get());
                        out.accept(FIGHTER_ITEM.get());
                        out.accept(BOMBER_ITEM.get());
                        out.accept(TARANTULA_EGG.get());
                        for (Item i : ModBlocks.allItems()) out.accept(i);
                    })
                    .build());

    public ModeHud(IEventBus bus, ModContainer container) {
        ITEMS.register(bus);
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.ITEMS.register(bus);
        TABS.register(bus);
        ATTACHMENTS.register(bus);
        ENTITIES.register(bus);
        FEATURES.register(bus);
        bus.addListener(ModeHud::registerPayloads);
        bus.addListener(ModeHud::attributes);
        bus.addListener(ModeHud::spawnPlacements);
        container.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(TARANTULA.get(), Tarantula.attributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TARANTULA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Tarantula::checkSpawn, RegisterSpawnPlacementsEvent.Operation.OR);
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
        reg.playToServer(dev.zedkodex.modehud.net.GripPayload.TYPE, dev.zedkodex.modehud.net.GripPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) dev.zedkodex.modehud.server.Grip.handle(sp, payload);
                }));
        reg.playToServer(ActionPayload.TYPE, ActionPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) Actions.handle(sp, payload.action());
                }));
    }
}
