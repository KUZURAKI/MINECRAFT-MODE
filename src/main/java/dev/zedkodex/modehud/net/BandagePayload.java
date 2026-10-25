package dev.zedkodex.modehud.net;

import dev.zedkodex.modehud.ModeHud;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BandagePayload(int entityId, int ticks) implements CustomPacketPayload {
    public static final Type<BandagePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "bandage"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BandagePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BandagePayload::entityId,
            ByteBufCodecs.VAR_INT, BandagePayload::ticks,
            BandagePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
