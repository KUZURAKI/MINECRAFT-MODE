package dev.zedkodex.modehud.client;

import dev.zedkodex.modehud.Config;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

public final class VitalsPanel {
    private VitalsPanel() {}

    private static final int HP = 0xFF4D4D, HP_D = 0x4A0D12;
    private static final int FOOD = 0xFFB23E, FOOD_D = 0x4A2C08;
    private static final int WATER = 0x4DB8FF, WATER_D = 0x0B2F4A;
    private static final int AIR = 0x7FE3FF, AIR_D = 0x0B3A4A;
    private static final int[] STAGE_RGB = {0, 0xFFB0A0, 0xFF6A6A, 0xFF2A3A};
    private static final String HUD_GLYPHS = "01#@$%&*+=<>/\\|{}[]?!";

    private static class BarState {
        final int[] last = new int[10];
        final long[] changed = new long[10];

        BarState() {
            java.util.Arrays.fill(last, -1);
        }
    }

    private static final BarState hpBar = new BarState();
    private static final BarState foodBar = new BarState();
    private static final BarState waterBar = new BarState();
    private static final BarState airBar = new BarState();

    private static float deadMix;
    private static float bleedAnim;
    private static float airAnim;
    private static long lastMs;

    public static void render(GuiGraphics g, long now) {
        if (!Config.CLIENT.hudEnabled.get()) return;
        long start = HudState.hudStartMs;
        if (now < start || HudState.hp.shown < 0) return;

        float dt = lastMs == 0 ? 16 : Math.min(100, now - lastMs);
        lastMs = now;
        float k = 1f - (float) Math.exp(-dt / 110f);
        deadMix += ((HudState.dead ? 1f : 0f) - deadMix) * k;
        bleedAnim += ((HudState.bleed > 0 && !HudState.dead ? 1f : 0f) - bleedAnim) * k;
        airAnim += ((HudState.airVisible ? 1f : 0f) - airAnim) * k;

        int ox = 12;
        int oy = 8;

        
        int hp = HudState.hp.shown;
        boolean low = hp <= 6 && !HudState.dead;
        boolean critical = hp <= 3 && !HudState.dead;
        int hpCy = oy + 14;

        bar(g, hpBar, ox + 15, hpCy, 9, 7, 2, hp, HP, HP_D, low, critical, HudState.hpFlashMs, HudState.hpFlashRgb, 600, now, start);

        float iconIn = Gfx.clamp((now - start - 100f) / 700f);
        float beat = 1f;
        if (low) {
            double period = critical ? 360.0 : 700.0;
            float ph = (float) ((now % (long) period) / period);
            beat = ph < 0.15f ? 1f + 0.2f * (ph / 0.15f)
                    : ph < 0.30f ? 1.2f - 0.2f * ((ph - 0.15f) / 0.15f)
                    : ph < 0.45f ? 1f + 0.12f * ((ph - 0.30f) / 0.15f)
                    : 1.12f - 0.12f * ((ph - 0.45f) / 0.55f);
        }
        float hpIconScale = (0.4f + 0.6f * Gfx.easeOutBack(iconIn)) * beat;
        Gfx.icon(g, "hp", ox + 9, hpCy, 20, -12f, hpIconScale, iconIn, 1f - 0.55f * deadMix);

        if (HudState.bleed > 0 && !HudState.dead) {
            int n = HudState.bleed;
            double dur = n == 1 ? 2000.0 : n == 2 ? 1200.0 : 700.0;
            for (int d = 0; d < n; d++) {
                float fr = (float) (((now / dur) + d * 0.25) % 1.0);
                int dx = ox + 5 + d * 5;
                int dy = hpCy + 6 + (int) (fr * 12);
                g.fill(dx, dy, dx + 2, dy + 3, Gfx.rgba(0xC01020, 1f - fr));
            }
        }

        if (bleedAnim > 0.02f) drawChip(g, now, ox, hpCy + 9, bleedAnim);

        int needCy = hpCy + 13 + Math.round(20 * bleedAnim);
        needRow(g, foodBar, "eat", ox, needCy, 0, HudState.food.shown, FOOD, FOOD_D, HudState.foodFlashMs, 10f, 1100, now, start);
        needRow(g, waterBar, "water", ox, needCy, 80, HudState.water.shown, WATER, WATER_D, HudState.waterFlashMs, 26f, 1400, now, start);

        if (airAnim > 0.02f) {
            int airCy = needCy + 15 + Math.round((1f - airAnim) * -5);
            needRowAlpha(g, airBar, "air", ox, airCy, 0, HudState.air.shown, AIR, AIR_D, HudState.airFlashMs, 10f, airAnim, now, start);
        }
    }

    private static void needRow(GuiGraphics g, BarState st, String icon, int ox, int cy, int dx, int value, int rgb, int dark,
                                long flashMs, float rot, int base, long now, long start) {
        boolean low = value <= 4 && !HudState.dead;
        bar(g, st, ox + 12 + dx, cy, 5, 5, 1, value, rgb, dark, low, value <= 2, flashMs, 0xFFFFFF, 700, now, start, base);
        float in = Gfx.clamp((now - start - (base - 500f)) / 700f);
        float shakeX = 0, shakeY = 0;
        if (low) {
            int step = (int) ((now / 100) % 4);
            shakeX = new float[]{0, -1, 1, -1}[step];
            shakeY = new float[]{0, 1, -1, -1}[step];
        }
        Gfx.icon(g, icon, ox + 7 + dx + shakeX, cy + shakeY, 15, rot, 0.4f + 0.6f * Gfx.easeOutBack(in), in, 1f - 0.55f * deadMix);
    }

    private static void needRowAlpha(GuiGraphics g, BarState st, String icon, int ox, int cy, int dx, int value, int rgb, int dark,
                                     long flashMs, float rot, float alpha, long now, long start) {
        boolean low = value <= 4 && !HudState.dead;
        barAlpha(g, st, ox + 12 + dx, cy, 5, 5, 1, value, rgb, dark, low, value <= 2, flashMs, 0xFFFFFF, 700, now, start, 0, alpha);
        float shakeX = 0, shakeY = 0;
        if (low) {
            int step = (int) ((now / 100) % 4);
            shakeX = new float[]{0, -1, 1, -1}[step];
            shakeY = new float[]{0, 1, -1, -1}[step];
        }
        Gfx.icon(g, icon, ox + 7 + dx + shakeX, cy + shakeY, 15, rot, 1f, alpha, 1f - 0.55f * deadMix);
    }

    private static void bar(GuiGraphics g, BarState st, int x, int cy, int cw, int ch, int gap, int value, int rgb, int dark,
                            boolean low, boolean critical, long flashMs, int flashRgb, long flashLen, long now, long start) {
        bar(g, st, x, cy, cw, ch, gap, value, rgb, dark, low, critical, flashMs, flashRgb, flashLen, now, start, 600);
    }

    private static void bar(GuiGraphics g, BarState st, int x, int cy, int cw, int ch, int gap, int value, int rgb, int dark,
                            boolean low, boolean critical, long flashMs, int flashRgb, long flashLen, long now, long start, int base) {
        barAlpha(g, st, x, cy, cw, ch, gap, value, rgb, dark, low, critical, flashMs, flashRgb, flashLen, now, start, base, 1f);
    }

    private static void barAlpha(GuiGraphics g, BarState st, int x, int cy, int cw, int ch, int gap, int value, int rgb, int dark,
                                 boolean low, boolean critical, long flashMs, int flashRgb, long flashLen, long now, long start,
                                 int base, float rowAlpha) {
        if (deadMix > 0.01f) {
            rgb = Gfx.mix(rgb, Gfx.gray(rgb), deadMix);
            dark = Gfx.mix(dark, Gfx.gray(dark), deadMix);
            flashMs = 0;
        }
        float flash = Gfx.clamp((flashMs - now) / (float) flashLen);
        double period = critical ? 400.0 : 800.0;
        float ph = (float) ((now % (long) period) / period);
        float pulse = low ? 0.5f - 0.5f * (float) Math.cos(ph * Math.PI * 2) : 0f;

        g.pose().pushPose();
        g.pose().translate(x, cy, 0);
        g.pose().mulPose(new Matrix4f().m10(-0.364f));

        for (int i = 0; i < 10; i++) {
            float intro = Gfx.clamp((now - start - base - i * 70f) / 350f) * rowAlpha;
            if (intro <= 0.01f) continue;

            int pts = Math.max(0, Math.min(2, value - i * 2));

            int lx = i * (cw + gap);
            int ly = -ch / 2;

            int outline = flash > 0 ? Gfx.mix(0x000000, flashRgb, flash) : low ? Gfx.mix(0x000000, rgb, pulse) : 0x000000;
            g.fill(lx - 1, ly - 1, lx + cw + 1, ly + ch + 1, Gfx.rgba(outline, intro));
            int bg = flash > 0 ? Gfx.mix(dark, flashRgb, flash * 0.7f) : dark;
            g.fill(lx, ly, lx + cw, ly + ch, Gfx.rgba(bg, intro));

            if (pts > 0) {
                int w = pts == 2 ? cw : (cw + 1) / 2;
                int col = low ? Gfx.mix(rgb, 0xFFFFFF, pulse * 0.55f) : rgb;
                if (flash > 0) col = Gfx.mix(col, flashRgb, flash * 0.8f);
                g.fill(lx, ly, lx + w, ly + ch, Gfx.rgba(col, intro));
                g.fill(lx, ly, lx + w, ly + 1, Gfx.rgba(0xFFFFFF, 0.45f * intro));
                g.fill(lx, ly + ch - Math.max(1, ch * 2 / 5), lx + w, ly + ch, Gfx.rgba(0x000000, 0.25f * intro));
            }
        }
        g.pose().popPose();
    }

    private static void drawChip(GuiGraphics g, long now, int ox, int y, float a) {
        int stage = HudState.bleedStage;
        int rgb = STAGE_RGB[stage];
        float shake = stage == 3 ? ((now / 200) % 2 == 0 ? 0f : 1f) : 0f;
        float cx = ox + 22 + shake;
        float cy = y + 11;
        Gfx.icon(g, "bleed" + stage, cx, cy, 17, 0f, 1f, a);
        Gfx.text(g, String.valueOf(stage), cx + 3, cy + 2, Gfx.rgba(0xFFFFFF, a), 0.8f, true);

        long el = now - HudState.bleedChangeMs;
        String label = Config.CLIENT.asciiEffects.get() ? Scramble.text("КРОВОТЕЧЕНИЕ", el, 0, 0.6f, "|!.~,", 5) : "КРОВОТЕЧЕНИЕ";
        Gfx.text(g, label, cx + 11, cy - 3, Gfx.rgba(rgb, a), 0.75f, true);
    }
}
