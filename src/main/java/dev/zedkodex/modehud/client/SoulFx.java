package dev.zedkodex.modehud.client;

import net.minecraft.client.gui.GuiGraphics;

public final class SoulFx {
    private SoulFx() {}

    private static final int SOUL = 0x8FE3FF;

    public static void render(GuiGraphics g, long now, float raw, float k, int w, int h, float bodyX, float bodyY, float panelX, float panelY) {
        if (k > 0.01f) abyss(g, w, h, k);
        if (k > 0.01f) thread(g, now, w, h, bodyX, bodyY, k);
        if (k > 0.01f) motes(g, now, bodyX, bodyY, k);
        soul(g, raw, bodyX, bodyY, panelX, panelY);
    }

    private static void abyss(GuiGraphics g, int w, int h, float k) {
        int n = 32;
        for (int i = 0; i < n; i++) {
            float f = i / (float) (n - 1);
            float a = (0.08f + 0.5f * f * f) * k;
            g.fill(i * w / n, 0, (i + 1) * w / n, h, Gfx.rgba(0x02040A, a));
        }
        int hb = (int) (h * 0.22f);
        g.fillGradient(0, 0, w, hb, Gfx.rgba(0x000000, 0.5f * k), Gfx.rgba(0x000000, 0f));
        g.fillGradient(0, h - hb, w, h, Gfx.rgba(0x000000, 0f), Gfx.rgba(0x000000, 0.5f * k));
    }

    private static void thread(GuiGraphics g, long now, int w, int h, float bx, float by, float k) {
        float sx = w * 0.5f;
        float sy = h + 6f;
        float cx = (sx + bx) / 2f + 30f;
        float cy = (sy + by) / 2f + 40f;
        int n = 240;
        float time = now / 1000f;
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float u = 1f - t;
            float x = u * u * sx + 2 * u * t * cx + t * t * bx;
            float y = u * u * sy + 2 * u * t * cy + t * t * by;
            float wob = (float) Math.sin(t * 9f - time * 3f) * 3f * (float) Math.sin(t * Math.PI);
            x += wob;
            float reach = Gfx.clamp(k * 1.4f - t * 0.4f);
            if (reach <= 0f) break;
            float pulse = 0.7f + 0.3f * (float) Math.sin(time * 4f - t * 12f);
            float a = k * pulse * (0.55f + 0.45f * t);
            int th = 2 + (int) (3 * (1f - t));
            g.fill((int) x - th - 3, (int) y - th - 3, (int) x + th + 4, (int) y + th + 4, Gfx.rgba(0x2A0638, a * 0.1f));
            g.fill((int) x - th - 1, (int) y - th - 1, (int) x + th + 2, (int) y + th + 2, Gfx.rgba(0x12001C, a * 0.35f));
            g.fill((int) x - th / 2, (int) y - th / 2, (int) x + th / 2 + 1, (int) y + th / 2 + 1, Gfx.rgba(0x000000, a * 0.95f));
        }
        for (int i = 0; i < 6; i++) {
            float t = (float) ((time * 0.35 + i / 6.0) % 1.0);
            float u = t;
            float v = 1f - u;
            float x = v * v * bx + 2 * v * u * cx + u * u * sx;
            float y = v * v * by + 2 * v * u * cy + u * u * sy;
            float a = (float) Math.sin(t * Math.PI) * k;
            g.fill((int) x - 2, (int) y - 2, (int) x + 4, (int) y + 4, Gfx.rgba(0x5A1478, a * 0.5f));
            g.fill((int) x - 1, (int) y - 1, (int) x + 3, (int) y + 3, Gfx.rgba(0x000000, a));
        }
        g.fill((int) bx - 9, (int) by - 9, (int) bx + 9, (int) by + 9, Gfx.rgba(0x12001C, 0.35f * k));
        g.fill((int) bx - 5, (int) by - 5, (int) bx + 5, (int) by + 5, Gfx.rgba(0x000000, 0.8f * k));
    }

    private static void motes(GuiGraphics g, long now, float bx, float by, float k) {
        for (int i = 0; i < 16; i++) {
            float speed = 2200f + Gfx.hash(i * 13 + 5) % 1800;
            float t = (float) (((now + Gfx.hash(i * 7 + 1) % 4000) / speed) % 1.0);
            float sway = (float) Math.sin(t * 6.28f * 1.5f + i) * 6f;
            float x = bx + ((Gfx.hash(i * 29 + 3) % 40) - 20) + sway;
            float y = by + 30 - t * 110f;
            float a = (float) Math.sin(t * Math.PI) * 0.7f * k;
            int u = 1 + (i % 3 == 0 ? 1 : 0);
            g.fill((int) x, (int) y, (int) x + u, (int) y + u, Gfx.rgba(SOUL, a));
        }
    }

    private static void glow(GuiGraphics g, float x, float y, float r, float a) {
        int steps = 5;
        for (int i = steps; i >= 1; i--) {
            float rr = r * i / steps;
            float al = a * (0.12f + 0.5f * (1f - i / (float) steps));
            g.fill((int) (x - rr), (int) (y - rr), (int) (x + rr), (int) (y + rr), Gfx.rgba(SOUL, al));
        }
        g.fill((int) (x - r * 0.25f), (int) (y - r * 0.25f), (int) (x + r * 0.25f), (int) (y + r * 0.25f), Gfx.rgba(0xFFFFFF, a));
    }

    private static float[] path(float s, float bx, float by, float px, float py) {
        float e = 1f - (float) Math.pow(1f - s, 2.4);
        float cx = (bx + px) / 2f;
        float cy = Math.min(by, py) - 90f;
        float u = 1f - e;
        return new float[]{u * u * bx + 2 * u * e * cx + e * e * px, u * u * by + 2 * u * e * cy + e * e * py};
    }

    private static void soul(GuiGraphics g, float raw, float bx, float by, float px, float py) {
        float s = Gfx.clamp(raw / 0.6f);
        if (s <= 0f || s >= 1f) return;
        float a = (float) Math.sin(s * Math.PI);

        float ring = s * 90f;
        float ra = (1f - s) * 0.6f;
        for (int i = 0; i < 48; i++) {
            double ang = i / 48.0 * Math.PI * 2;
            int x = (int) (bx + Math.cos(ang) * ring);
            int y = (int) (by + Math.sin(ang) * ring * 0.45f);
            g.fill(x, y, x + 2, y + 2, Gfx.rgba(SOUL, ra));
        }

        for (int j = 14; j >= 1; j--) {
            float st = s - j * 0.025f;
            if (st <= 0f) continue;
            float[] p = path(st, bx, by, px, py);
            float f = 1f - j / 15f;
            int u = Math.max(1, (int) (4 * f));
            g.fill((int) p[0], (int) p[1], (int) p[0] + u, (int) p[1] + u, Gfx.rgba(SOUL, a * f * 0.7f));
        }
        float[] p = path(s, bx, by, px, py);
        glow(g, p[0], p[1], 9f, a);
    }
}
