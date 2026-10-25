package dev.zedkodex.modehud;

import java.util.function.Supplier;

import dev.zedkodex.modehud.data.Vitals;
import dev.zedkodex.modehud.item.MedItem;
import dev.zedkodex.modehud.net.ClientState;
import dev.zedkodex.modehud.net.PortalPayload;
import dev.zedkodex.modehud.net.VitalsPayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    public static final DeferredItem<Item> PILLS = ITEMS.register("pills",
            () -> new MedItem(new Item.Properties().stacksTo(16), MedItem.Kind.PILLS));
    public static final DeferredItem<Item> BANDAGE = ITEMS.register("bandage",
            () -> new MedItem(new Item.Properties().stacksTo(16), MedItem.Kind.BANDAGE));

    public static final Supplier<AttachmentType<Vitals>> VITALS = ATTACHMENTS.register("vitals",
            () -> AttachmentType.serializable(Vitals::new).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID))
                    .icon(() -> new ItemStack(PILLS.get()))
                    .displayItems((params, out) -> {
                        out.accept(PILLS.get());
                        out.accept(BANDAGE.get());
                    })
                    .build());

    public ModeHud(IEventBus bus, ModContainer container) {
        ITEMS.register(bus);
        TABS.register(bus);
        ATTACHMENTS.register(bus);
        bus.addListener(ModeHud::registerPayloads);
        container.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(VitalsPayload.TYPE, VitalsPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientState.apply(payload)));
        event.registrar("1").playToClient(PortalPayload.TYPE, PortalPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientState.portals.add(payload.kind())));
    }
}
