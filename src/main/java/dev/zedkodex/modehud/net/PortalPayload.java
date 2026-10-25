package dev.zedkodex.modehud.net;

import dev.zedkodex.modehud.ModeHud;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PortalPayload(int kind) implements CustomPacketPayload {
    public static final Type<PortalPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "portal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PortalPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PortalPayload::kind,
            PortalPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
