package dev.zedkodex.modehud.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dev.zedkodex.modehud.Config;
import net.minecraft.client.gui.GuiGraphics;

public final class BloodPanel {
    private BloodPanel() {}

    private static final int[] REDS = {0x7A0610, 0xA50E1A, 0xC4182A};
    private static final Random R = new Random();

    private static class Splat {
        int u;
        float x, y, delay, len, t;
        List<int[]> px = new ArrayList<>();
    }

    private static class Slot {
        int k;
        boolean on;
        long onMs;
        long offMs;
        float outDelay;
        Splat s;
    }

    private static final Slot[] slots = new Slot[14];
    private static float edge;
    private static float crit;
    private static long lastMs;
    private static AsciiField field;
    private static int fieldW;
    private static int fieldH;
    private static long fieldStep;

    static {
        for (int i = 0; i < slots.length; i++) slots[i] = new Slot();
    }

    public static void reset() {
        for (Slot s : slots) {
            s.k = 0;
            s.on = false;
        }
        edge = 0;
        crit = 0;
        field = null;
    }

    private static Splat make() {
        Splat s = new Splat();
        s.u = 2 + R.nextInt(3);
        int rad = 2 + R.nextInt(3);
        for (int dy = -rad - 3; dy <= rad + 3; dy++) {
            for (int dx = -rad - 3; dx <= rad + 3; dx++) {
                double d = Math.hypot(dx, dy);
                boolean keep = d <= rad ? R.nextDouble() < 1.1 - d / (rad + 1) : d <= rad + 3 && R.nextDouble() < 0.05;
                if (keep) s.px.add(new int[]{dx, dy, R.nextInt(3)});
            }
        }
        s.x = 3 + R.nextFloat() * 94;
        s.y = 3 + R.nextFloat() * 94;
        s.delay = R.nextFloat() * 0.5f;
        s.len = 35 + R.nextInt(75);
        s.t = 9 + R.nextFloat() * 9;
        return s;
    }

    public static void render(GuiGraphics g, long now) {
        if (!Config.CLIENT.bloodEffects.get()) return;
        float dt = lastMs == 0 ? 16 : Math.min(100, now - lastMs);
        lastMs = now;
        int w = g.guiWidth();
        int h = g.guiHeight();
        int hp = HudState.hpTarget;
        boolean low = hp <= 6 && !HudState.dead;
        boolean critical = hp <= 3 && !HudState.dead;

        float up = 1f - (float) Math.exp(-dt / 250f);
        float down = 1f - (float) Math.exp(-dt / 1000f);
        edge += ((low ? 1f : 0f) - edge) * (low ? up : down);
        crit += ((critical ? 1f : 0f) - crit) * (critical ? up : down);

        drawSplats(g, now, w, h, critical ? 14 : low ? 5 : 0);
        if (edge > 0.01f) drawEdges(g, now, w, h);
    }

    private static void drawSplats(GuiGraphics g, long now, int w, int h, int count) {
        for (int i = 0; i < slots.length; i++) {
            Slot sl = slots[i];
            boolean want = i < count;
            if (want && !sl.on) {
                sl.k++;
                sl.on = true;
                sl.onMs = now;
                sl.s = make();
                sl.outDelay = 0;
            } else if (!want && sl.on) {
                sl.on = false;
                sl.offMs = now;
                sl.outDelay = R.nextFloat() * 2600f;
            }
            if (sl.k == 0 || sl.s == null) continue;

            float alpha = Gfx.clamp((now - sl.onMs - sl.s.delay * 1000f) / 450f);
            if (!sl.on) {
                float fade = 1f - Gfx.clamp((now - sl.offMs - sl.outDelay) / 1600f);
                if (fade <= 0f) continue;
                alpha *= fade;
            }
            if (alpha <= 0.02f) continue;

            Splat s = sl.s;
            int px = (int) (s.x / 100f * w);
            int py = (int) (s.y / 100f * h);
            for (int[] c : s.px) {
                int x = px + c[0] * s.u;
                int y = py + c[1] * s.u;
                g.fill(x, y, x + s.u, y + s.u, Gfx.rgba(REDS[c[2]], alpha * 0.85f));
            }
            g.fill(px, py, px + s.u, py + s.u, Gfx.rgba(REDS[1], alpha * 0.85f));

            float p = Gfx.clamp((now - sl.onMs - (s.delay * 1000f + 800f)) / (s.t * 1000f));
            float ease = 1f - (float) Math.pow(1f - p, 2.2);
            int len = (int) (s.len * ease);
            if (len > 1) {
                int sw = Math.max(1, (int) (s.u * 0.6f));
                int sx = px + s.u / 2 - sw / 2;
                g.fill(sx, py + s.u, sx + sw, py + s.u + len, Gfx.rgba(0x8A0B16, alpha * 0.9f));
                g.fill(sx, py + s.u + len - 2, sx + sw, py + s.u + len, Gfx.rgba(0x5A050C, alpha));
            }
        }
    }

    private static void drawEdges(GuiGraphics g, long now, int w, int h) {
        double period = crit > 0.5f ? 700.0 : 1400.0;
        float pulse = 0.7f + 0.3f * (float) Math.sin(now / period * Math.PI * 2);
        float a = edge * (0.32f + 0.36f * crit) * pulse;
        int red = 0xD20014;

        int hb = (int) (h * 0.26f);
        int wb = (int) (w * 0.2f);
        g.fillGradient(0, 0, w, hb, Gfx.rgba(red, a), Gfx.rgba(red, 0f));
        g.fillGradient(0, h - hb, w, h, Gfx.rgba(red, 0f), Gfx.rgba(red, a));
        int n = 20;
        for (int i = 0; i < n; i++) {
            float f = 1f - i / (float) n;
            int col = Gfx.rgba(red, a * f * f);
            int x0 = i * wb / n;
            int x1 = (i + 1) * wb / n;
            g.fill(x0, 0, x1, h, col);
            g.fill(w - x1, 0, w - x0, h, col);
        }

        if (!Config.CLIENT.asciiEffects.get()) return;
        int cw = 7;
        int ch = 9;
        int cols = w / cw + 1;
        int rows = h / ch + 1;
        if (field == null || fieldW != w || fieldH != h) {
            field = new AsciiField(cols, rows, AsciiField.GLYPHS, AsciiField.Flow.NONE, 0.3f);
            fieldW = w;
            fieldH = h;
            fieldStep = now;
        }
        while (now - fieldStep >= 150) {
            field.step();
            fieldStep += 150;
        }
        float ta = edge * (0.4f + 0.25f * crit);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                char chr = field.g[r * cols + c];
                if (chr == ' ') continue;
                float nx = (c + 0.5f) / cols * 2f - 1f;
                float ny = (r + 0.5f) / rows * 2f - 1f;
                float m = Gfx.clamp((Math.max(Math.abs(nx), Math.abs(ny)) * 0.7f + Math.min(1f, (float) Math.sqrt(nx * nx + ny * ny)) * 0.3f - 0.5f) / 0.5f);
                float al = m * ta;
                if (al < 0.06f) continue;
                Gfx.text(g, String.valueOf(chr), c * cw, r * ch, Gfx.rgba(0xFF2A3A, al), 0.875f, false);
            }
        }
    }
}
