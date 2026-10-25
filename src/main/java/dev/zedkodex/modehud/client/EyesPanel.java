package dev.zedkodex.modehud.client;

import net.minecraft.client.gui.GuiGraphics;

public final class EyesPanel {
    private EyesPanel() {}

    private static final long DELAY = 1200;
    private static final long OPEN = 4600;
    public static final long CLOSE = 750;

    private static final float[] TIMES = {0f, 0.18f, 0.30f, 0.42f, 0.52f, 0.72f, 1f};
    private static final float[] RX = {55f, 58f, 56f, 62f, 58f, 80f, 130f};
    private static final float[] RY = {0.1f, 16f, 3f, 22f, 6f, 55f, 200f};
    private static final int BANDS = 8;

    private static boolean running;
    private static long startMs;
    private static long closeMs;

    public static void start(long now) {
        running = true;
        startMs = now;
        closeMs = 0;
    }

    public static void close(long now) {
        running = false;
        closeMs = now;
    }

    public static boolean isClosed(long now) {
        return closeMs > 0 && now - closeMs >= CLOSE + 200;
    }

    public static void reset() {
        running = false;
        closeMs = 0;
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
        int w = g.guiWidth();
        int h = g.guiHeight();

        if (closeMs > 0) {
            long el = now - closeMs;
            if (el >= CLOSE) {
                g.fill(0, 0, w, h, 0xFF000000);
                return;
            }
            float t = el / (float) CLOSE;
            float k = (1f - t) * (1f - t) * (1f - t);
            drawEye(g, w, h, (55f + 75f * k) / 100f * w, Math.max(0.5f, (0.1f + 200f * k) / 100f * h));
            return;
        }

        if (!running) return;
        long el = now - startMs;
        if (el > DELAY + OPEN) {
            running = false;
            return;
        }
        if (el < DELAY) {
            g.fill(0, 0, w, h, 0xFF000000);
            return;
        }
        float t = (el - DELAY) / (float) OPEN;
        drawEye(g, w, h, sample(RX, t) / 100f * w, Math.max(0.5f, sample(RY, t) / 100f * h));
    }

    private static void drawEye(GuiGraphics g, int w, int h, float rx, float ry) {
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
    }
}
