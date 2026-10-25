package dev.zedkodex.modehud.net;

import dev.zedkodex.modehud.ModeHud;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BackpackPayload(int entityId, boolean worn) implements CustomPacketPayload {
    public static final Type<BackpackPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "backpack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BackpackPayload::entityId,
            ByteBufCodecs.BOOL, BackpackPayload::worn,
            BackpackPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
