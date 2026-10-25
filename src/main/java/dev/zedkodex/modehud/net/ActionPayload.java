package dev.zedkodex.modehud.net;

import dev.zedkodex.modehud.ModeHud;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ActionPayload(int action) implements CustomPacketPayload {
    public static final int OPEN_BACKPACK = 0;
    public static final int TAKE_OFF_BACKPACK = 1;
    public static final int EXIT_VEHICLE = 2;
    public static final int PRIMARY = 3;
    public static final int SECONDARY = 4;
    public static final int CRASH = 5;
    public static final int WRAITH_ON = 6;
    public static final int WRAITH_OFF = 7;

    public static final Type<ActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ActionPayload::action,
            ActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
