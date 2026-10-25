package dev.zedkodex.modehud.client;

import dev.zedkodex.modehud.ModeHud;
import dev.zedkodex.modehud.net.ClientState;
import net.minecraft.client.player.AbstractClientPlayer;

public final class BandageState {
    private BandageState() {}

    public static boolean wrapping(AbstractClientPlayer p) {
        return p.isUsingItem() && p.getUseItem().is(ModeHud.BANDAGE.get());
    }

    public static float progress(AbstractClientPlayer p, float partial) {
        int total = Math.max(1, p.getUseItem().getUseDuration(p));
        return Math.min(1f, (p.getTicksUsingItem() + partial) / total);
    }

    public static int rings(AbstractClientPlayer p) {
        if (wrapping(p)) {
            int total = Math.max(3, p.getUseItem().getUseDuration(p));
            return Math.min(3, p.getTicksUsingItem() * 3 / total + 1);
        }
        Long end = ClientState.bandaged.get(p.getId());
        return end != null && System.currentTimeMillis() < end ? 3 : 0;
    }
}
