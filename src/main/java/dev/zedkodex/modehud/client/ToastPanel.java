package dev.zedkodex.modehud.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import dev.zedkodex.modehud.Config;
import dev.zedkodex.modehud.client.Places.Place;
import dev.zedkodex.modehud.client.Places.Realm;
import net.minecraft.client.gui.GuiGraphics;

public final class ToastPanel {
    private ToastPanel() {}

    private static final long DURATION = 5400;
    private static final int CELL_W = 6;
    private static final int CELL_H = 7;

    private static final Deque<Place> queue = new ArrayDeque<>();
    private static Place last;
    private static Place current;
    private static long startMs;
    private static long nextAllowed;
    private static long lastStep;
    private static AsciiField field;
    private static int fieldCols;

    public static void forget() {
        last = null;
    }

    public static void reset() {
        queue.clear();
        last = null;
        current = null;
        field = null;
    }

    public static void enqueue(Place p) {
        if (p == null || p == last) return;
        if (queue.size() >= 4) queue.pollFirst();
        queue.addLast(p);
        last = p;
    }

    public static void render(GuiGraphics g, long now) {
        if (!Config.CLIENT.notifications.get()) return;
        if (current != null && now - startMs > DURATION) {
            current = null;
            nextAllowed = now + 350;
        }
        if (current == null) {
            if (queue.isEmpty() || now < HudState.toastGateMs || now < nextAllowed) return;
            current = queue.pollFirst();
            startMs = now;
            field = null;
        }

        Place p = current;
        int w = Gfx.vw;
        int h = Gfx.vh;
        long el = now - startMs;
        float t = el / (float) DURATION;
        float op = t < 0.06f ? t / 0.06f : t < 0.84f ? 1f : Math.max(0f, (1f - t) / 0.16f);
        if (op <= 0.01f) return;

        int pool = p.realm() == Realm.NETHER ? 0x4A0800 : p.realm() == Realm.END ? 0x1C0A38 : 0x000000;
        g.fillGradient(0, 0, w, 120, Gfx.rgba(pool, 0.7f * op), Gfx.rgba(pool, 0f));

        if (p.flash() != 0) {
            float fa = el < 270 ? el / 270f : Math.max(0f, 1f - (el - 270) / 1530f);
            g.fill(0, 0, w, h, Gfx.rgba(p.flash(), 0.5f * fa));
        }

        boolean ascii = Config.CLIENT.asciiEffects.get();
        if (ascii) drawField(g, p, now, w, op);

        float titleScale = Math.min(2f, (w - 20f) / (Gfx.width(p.name())));
        float cx = w / 2f;
        float sway = p.motion().equals("sway") ? (float) Math.sin(now / 2600.0 * Math.PI * 2) * 2f : 0f;
        float pulse = p.motion().equals("pulse") ? 1f + 0.03f * (float) Math.sin(now / 2200.0 * Math.PI * 2) : 1f;
        float flick = p.motion().equals("flicker") ? flicker(now) : 1f;

        String label = ascii ? Scramble.text(p.label(), el, 0, 0.9f, p.glyphs(), 1) : p.label();
        Gfx.textCentered(g, label, cx, 14, Gfx.rgba(p.color(), 0.85f * op), 0.75f, true);

        String name = ascii ? Scramble.text(p.name(), el, 0, 0.4f, p.glyphs(), 2) : p.name();
        float s = titleScale * pulse;
        Gfx.textCentered(g, name, cx + sway, 26, Gfx.rgba(0xFFFFFF, op * flick), s, true);

        float infoScale = 0.625f;
        int perLine = Math.max(10, (int) (Math.min(w - 40, 280) / (8 * infoScale)));
        List<String> lines = wrap(p.info(), perLine);
        int total = p.info().length();
        String info = ascii ? Scramble.text(p.info(), el, 14, 2.5f, p.glyphs(), 3) : p.info();
        int idx = 0;
        float y = 26 + 8 * titleScale + 8;
        for (String line : lines) {
            String shown = info.substring(idx, Math.min(total, idx + line.length()));
            idx += line.length();
            while (idx < total && info.charAt(idx) == ' ') idx++;
            Gfx.textCentered(g, shown, cx, y, Gfx.rgba(p.color(), op), infoScale, true);
            y += 8 * infoScale + 3;
        }
    }

    private static float flicker(long now) {
        float ph = (now % 1400) / 1400f;
        if (ph < 0.08f) return 0.6f;
        if (ph > 0.52f && ph < 0.60f) return 0.85f;
        if (ph > 0.73f && ph < 0.78f) return 0.7f;
        return 1f;
    }

    private static List<String> wrap(String s, int perLine) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            int end = Math.min(s.length(), i + perLine);
            if (end < s.length()) {
                int sp = s.lastIndexOf(' ', end);
                if (sp > i) end = sp;
            }
            out.add(s.substring(i, end));
            i = end;
            while (i < s.length() && s.charAt(i) == ' ') i++;
        }
        return out;
    }

    private static void drawField(GuiGraphics g, Place p, long now, int w, float op) {
        int cols = Math.max(8, Math.min(70, (w - 16) / CELL_W));
        if (field == null || fieldCols != cols) {
            field = new AsciiField(cols, 14, p.glyphs(), p.flow(), p.density());
            fieldCols = cols;
            lastStep = now;
        }
        while (now - lastStep >= p.speed()) {
            field.step();
            lastStep += p.speed();
        }

        float shimmer = 0.5f + 0.3f * (0.5f + 0.5f * (float) Math.sin(now / 2200.0 * Math.PI * 2));
        float x0 = (w - cols * CELL_W) / 2f;
        float y0 = 6;
        for (int r = 0; r < field.rows; r++) {
            for (int c = 0; c < cols; c++) {
                char ch = field.g[r * cols + c];
                if (ch == ' ') continue;
                float nx = (c + 0.5f) / cols * 2f - 1f;
                float ny = (r + 0.5f) / field.rows * 2f - 1f;
                float d = (float) Math.sqrt(nx * nx + ny * ny) / 1.414f;
                float m = Gfx.clamp(1f - (d - 0.15f) / 0.53f);
                float a = m * shimmer * op;
                if (a < 0.06f) continue;
                Gfx.text(g, String.valueOf(ch), x0 + c * CELL_W, y0 + r * CELL_H, Gfx.rgba(p.color(), a), 0.75f, false);
            }
        }
    }
}
