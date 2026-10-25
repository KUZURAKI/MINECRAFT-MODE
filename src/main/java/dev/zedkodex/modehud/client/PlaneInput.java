package dev.zedkodex.modehud.client;

import dev.zedkodex.modehud.net.ActionPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlaneInput {
    private PlaneInput() {}

    public static boolean thrust() {
        return Minecraft.getInstance().options.keyShift.isDown();
    }

    public static boolean brake() {
        return Minecraft.getInstance().options.keySprint.isDown();
    }

    public static void crash() {
        PacketDistributor.sendToServer(new ActionPayload(ActionPayload.CRASH));
    }
}
