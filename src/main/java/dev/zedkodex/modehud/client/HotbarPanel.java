package dev.zedkodex.modehud.client;

import dev.zedkodex.modehud.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class HotbarPanel {
    private HotbarPanel() {}

    private static final int CARD = 20;
    private static final int GAP = 3;

    private static float shown;
    private static final float[] raise = new float[9];
    private static long lastMs;

    public static void render(GuiGraphics g, long now, Player p) {
        if (!Config.CLIENT.hudEnabled.get() || now < HudState.hudStartMs) return;
        float dt = lastMs == 0 ? 16 : Math.min(100, now - lastMs);
        lastMs = now;

        boolean show = !HudState.dead && !HudState.spectator && (now - HudState.barShowMs < 2000 || (Minecraft.getInstance().screen != null && !(Minecraft.getInstance().screen instanceof SoulInventoryScreen)));
        float k = 1f - (float) Math.exp(-dt / (show ? 90f : 160f));
        shown += ((show ? 1f : 0f) - shown) * k;
        if (shown < 0.01f) return;

        Inventory inv = p.getInventory();
        int w = Gfx.vw;
        int h = Gfx.vh;
        int total = 9 * CARD + 8 * GAP;
        int x0 = (w - total) / 2;
        float baseY = h - 30 + (1f - shown) * 60f;
        float kr = 1f - (float) Math.exp(-dt / 70f);

        for (int i = 0; i < 9; i++) {
            boolean act = i == inv.selected;
            raise[i] += ((act ? 1f : 0f) - raise[i]) * kr;
            int x = x0 + i * (CARD + GAP);
            int y = Math.round(baseY - raise[i] * 6f);
            card(g, inv.getItem(i), x, y, i + 1, act, raise[i], shown);
        }

        ItemStack off = p.getOffhandItem();
        if (!off.isEmpty()) card(g, off, x0 - CARD - 8, Math.round(baseY), 0, false, 0f, shown);
    }

    private static void card(GuiGraphics g, ItemStack s, int x, int y, int key, boolean act, float up, float a) {
        g.fill(x - 1, y - 1, x + CARD + 1, y + CARD + 1, Gfx.rgba(0x000000, 0.9f * a));
        g.fill(x, y, x + CARD, y + CARD, Gfx.rgba(0x12161F, 0.6f * a));
        g.fill(x, y, x + CARD, y + 1, Gfx.rgba(0xFFFFFF, 0.28f * a));
        g.fill(x, y, x + 1, y + CARD, Gfx.rgba(0xFFFFFF, 0.28f * a));
        g.fill(x, y + CARD - 1, x + CARD, y + CARD, Gfx.rgba(0x000000, 0.35f * a));
        g.fill(x + CARD - 1, y, x + CARD, y + CARD, Gfx.rgba(0x000000, 0.35f * a));
        if (act) g.fill(x + 1, y + 1, x + CARD - 1, y + CARD - 1, Gfx.rgba(0xFFFFFF, 0.14f * a));

        Minecraft mc = Minecraft.getInstance();
        if (!s.isEmpty()) {
            g.renderItem(s, x + 2, y + 2);
            if (!act) g.fill(x + 1, y + 1, x + CARD - 1, y + CARD - 1, Gfx.rgba(0x20242C, 0.5f * a * (1f - up)));
            g.renderItemDecorations(mc.font, s, x + 2, y + 2);
        }
        if (key > 0) Gfx.text(g, String.valueOf(key), x + 2, y + 2, Gfx.rgba(0xFFFFFF, 0.55f * a), 0.5f, true);
        if (act) {
            int dx = x + CARD / 2 - 1;
            int dy = y + CARD + 3;
            g.fill(dx - 1, dy - 1, dx + 4, dy + 4, Gfx.rgba(0x000000, a));
            g.fill(dx, dy, dx + 3, dy + 3, Gfx.rgba(0xFFFFFF, a));
        }
    }
}
