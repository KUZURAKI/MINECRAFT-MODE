package dev.zedkodex.modehud.client;

import dev.zedkodex.modehud.Config;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public final class HudRenderer {
    private HudRenderer() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || !HudState.active) return;
        long now = Util.getMillis();

        BloodPanel.render(g, now);

        float hs = Config.CLIENT.hudScale.get().floatValue();
        g.pose().pushPose();
        g.pose().scale(hs, hs, 1f);
        Gfx.vw = Math.round(g.guiWidth() / hs);
        Gfx.vh = Math.round(g.guiHeight() / hs);
        float soul = SoulView.progress();
        if (soul < 0.99f) {
            g.pose().pushPose();
            g.pose().translate(-soul * 340f, 0f, 0f);
            VitalsPanel.render(g, now);
            g.pose().popPose();
            HotbarPanel.render(g, now, p);
        }
        g.pose().popPose();

        float ts = Config.CLIENT.toastScale.get().floatValue();
        g.pose().pushPose();
        g.pose().scale(ts, ts, 1f);
        Gfx.vw = Math.round(g.guiWidth() / ts);
        Gfx.vh = Math.round(g.guiHeight() / ts);
        ToastPanel.render(g, now);
        g.pose().popPose();

        EyesPanel.render(g, now);
    }
}
