package dev.zedkodex.modehud.net;

import dev.zedkodex.modehud.ModeHud;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VitalsPayload(float water, int bleed, int fxKind, int fxN, boolean journey) implements CustomPacketPayload {
    public static final Type<VitalsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "vitals"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VitalsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, VitalsPayload::water,
            ByteBufCodecs.VAR_INT, VitalsPayload::bleed,
            ByteBufCodecs.VAR_INT, VitalsPayload::fxKind,
            ByteBufCodecs.VAR_INT, VitalsPayload::fxN,
            ByteBufCodecs.BOOL, VitalsPayload::journey,
            VitalsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
