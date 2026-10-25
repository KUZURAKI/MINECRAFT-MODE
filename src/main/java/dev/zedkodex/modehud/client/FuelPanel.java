package dev.zedkodex.modehud.client;

import dev.zedkodex.modehud.entity.FuelVehicle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public final class FuelPanel {
    private FuelPanel() {}

    public static void render(GuiGraphics g, long now, Player p) {
        if (!(p.getVehicle() instanceof FuelVehicle bike) || HudState.dead) return;
        float fuel = bike.fuelFraction();
        boolean on = bike.isEngineOn();
        int w = 70;
        int x = Gfx.vw - w - 14;
        int y = Gfx.vh - 22;
        boolean low = fuel < 0.15f;
        int col = !on ? 0x8A8F9A : low ? (now / 300 % 2 == 0 ? 0xFF4D4D : 0x7A1A1A) : 0xFFB23E;

        Gfx.text(g, on ? "ДВИГАТЕЛЬ" : "ВЫКЛЮЧЕН", x, y - 9, Gfx.rgba(col, 1f), 0.7f, true);
        g.fill(x - 1, y - 1, x + w + 1, y + 7, 0xFF000000);
        g.fill(x, y, x + w, y + 6, Gfx.rgba(0x4A2C08, 0.9f));
        g.fill(x, y, x + (int) (w * fuel), y + 6, Gfx.rgba(col, 1f));
        g.fill(x, y, x + (int) (w * fuel), y + 1, Gfx.rgba(0xFFFFFF, 0.4f));
    }
}
