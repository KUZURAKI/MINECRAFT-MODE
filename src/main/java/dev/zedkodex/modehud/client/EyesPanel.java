package dev.zedkodex.modehud.client;

import dev.zedkodex.modehud.Config;
import net.minecraft.client.gui.GuiGraphics;

public final class EyesPanel {
    private EyesPanel() {}

    private static final long DELAY = 1200;
    private static final long OPEN = 4600;

    private static final float[] TIMES = {0f, 0.18f, 0.30f, 0.42f, 0.52f, 0.72f, 1f};
    private static final float[] RX = {55f, 58f, 56f, 62f, 58f, 80f, 130f};
    private static final float[] RY = {0.1f, 16f, 3f, 22f, 6f, 55f, 200f};
    private static final String GL = "01#@$%&*+=<>/|{}[]?!";
    private static final int BANDS = 8;

    private static boolean running;
    private static long startMs;

    public static void start(long now) {
        running = true;
        startMs = now;
    }

    public static void reset() {
        running = false;
    }

    private static float sample(float[] vals, float t) {
        for (int i = 1; i < TIMES.length; i++) {
            if (t <= TIMES[i]) {
                float u = (t - TIMES[i - 1]) / (TIMES[i] - TIMES[i - 1]);
                return vals[i - 1] + (vals[i] - vals[i - 1]) * Gfx.smooth(u);
            }
        }
        return vals[vals.length - 1];
    }

    public static void render(GuiGraphics g, long now) {
        if (!running) return;
        long el = now - startMs;
        if (el > DELAY + OPEN) {
            running = false;
            return;
        }
        int w = g.guiWidth();
        int h = g.guiHeight();
        if (el < DELAY) {
            g.fill(0, 0, w, h, 0xFF000000);
            return;
        }

        float t = (el - DELAY) / (float) OPEN;
        float rx = sample(RX, t) / 100f * w;
        float ry = Math.max(0.5f, sample(RY, t) / 100f * h);
        float cx = w / 2f;
        float cy = h / 2f;

        for (int y = 0; y < h; y += 2) {
            float dy = (y + 1 - cy) / ry;
            if (Math.abs(dy) >= 1f) {
                g.fill(0, y, w, y + 2, 0xFF000000);
                continue;
            }
            float prev = 0f;
            for (int j = 0; j <= BANDS; j++) {
                float k = 0.55f + 0.45f * j / BANDS;
                float hw = rx * (float) Math.sqrt(Math.max(0f, k * k - dy * dy));
                if (j > 0 && hw > prev) {
                    int col = Gfx.rgba(0x000000, (j - 0.5f) / BANDS);
                    g.fill((int) (cx - hw), y, (int) (cx - prev), y + 2, col);
                    g.fill((int) (cx + prev), y, (int) (cx + hw), y + 2, col);
                }
                prev = hw;
            }
            int edge = (int) prev;
            g.fill(0, y, (int) (cx - edge), y + 2, 0xFF000000);
            g.fill((int) (cx + edge), y, w, y + 2, 0xFF000000);
        }

        if (!Config.CLIENT.asciiEffects.get()) return;
        float fade = t < 0.1f ? t / 0.1f * 0.9f : t < 0.85f ? 0.9f : 0.9f * Math.max(0f, (1f - t) / 0.15f);
        if (fade <= 0.03f) return;
        int frame = (int) (el / 80);
        for (int y = 0; y < h; y += 4) {
            for (int x = 0; x < w; x += 3) {
                float k = (float) Math.sqrt(Math.pow((x - cx) / rx, 2) + Math.pow((y - cy) / ry, 2));
                float m = k < 0.45f ? 0f : k < 0.8f ? (k - 0.45f) / 0.35f : k < 1f ? 1f - 0.75f * (k - 0.8f) / 0.2f : 0.25f;
                float a = m * fade;
                if (a < 0.08f) continue;
                int hh = Gfx.hash(frame * 977 + x * 31 + y * 17);
                if (hh % 100 > 30) continue;
                Gfx.text(g, String.valueOf(GL.charAt(hh % GL.length())), x, y, Gfx.rgba(0xB8FFC8, a), 0.34f, false);
            }
        }
    }
}
