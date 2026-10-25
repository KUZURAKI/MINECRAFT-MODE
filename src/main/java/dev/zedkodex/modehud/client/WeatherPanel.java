package dev.zedkodex.modehud.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dev.zedkodex.modehud.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class WeatherPanel {
    private WeatherPanel() {}

    private enum Kind { DROP, DUST, FLAKE, MOTE, LEAF, BUBBLE }

    private static final Random R = new Random();
    private static final List<P> ps = new ArrayList<>();
    private static long lastMs;
    private static float intensity;
    private static float acc;
    private static float ambAcc;

    private static class P {
        Kind kind;
        float x, y, y0, vx, vy, age, life, hold, dist, vmax, accel, spin, phase;
        int u, color;
        float[] mid;
        boolean sliding, stopped;
    }

    public static void reset() {
        ps.clear();
        intensity = 0;
    }

    public static void render(GuiGraphics g, long now) {
        if (!Config.CLIENT.weatherEffects.get()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        ClientLevel lv = mc.level;
        if (p == null || lv == null) return;
        float dt = lastMs == 0 ? 0.016f : Math.min(0.1f, (now - lastMs) / 1000f);
        lastMs = now;
        int w = g.guiWidth();
        int h = g.guiHeight();

        boolean live = !HudState.dead && !p.isCreative() && !p.isSpectator();
        boolean under = live && p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
        BlockPos eye = BlockPos.containing(p.getEyePosition());
        String biome = lv.getBiome(eye).unwrapKey().map(k -> k.location().getPath()).orElse("");
        boolean nether = lv.dimension() == Level.NETHER;
        boolean end = lv.dimension() == Level.END;
        boolean sky = lv.dimension() == Level.OVERWORLD && lv.canSeeSky(eye) && !p.isUnderWater();

        float rain = lv.getRainLevel(1f);
        float thunder = lv.getThunderLevel(1f);
        Biome.Precipitation pr = lv.getBiome(eye).value().getPrecipitationAt(eye);
        boolean arid = biome.contains("desert") || biome.contains("badlands");
        boolean wet = live && sky && rain > 0.02f;
        float target = wet ? Math.max(rain, 0.05f) * (0.75f + 0.25f * thunder) : 0f;
        intensity += (target - intensity) * (1f - (float) Math.exp(-dt / 1.2f));

        if (live && ps.size() < 90) {
            if (wet && intensity > 0.03f) {
                acc += dt * (1.5f + intensity * 14f + thunder * 8f);
                while (acc >= 1f) {
                    acc -= 1f;
                    ps.add(arid ? dust(w, h, 0xD9B77A, 0xB08A52) : pr == Biome.Precipitation.SNOW ? flake(w, h) : drop(w, h, intensity));
                }
            } else if (under) {
                ambAcc += dt * 6f;
                while (ambAcc >= 1f) {
                    ambAcc -= 1f;
                    ps.add(bubble(w, h));
                }
            } else if (!wet) {
                float rate = nether ? 4.5f : end ? 4f : sky ? 1.3f : 0f;
                ambAcc += dt * rate;
                while (ambAcc >= 1f) {
                    ambAcc -= 1f;
                    ps.add(ambient(biome, nether, end, w, h));
                }
            }
        }

        for (int i = ps.size() - 1; i >= 0; i--) {
            P q = ps.get(i);
            q.age += dt;
            if (q.age >= q.life) {
                ps.remove(i);
                continue;
            }
            update(q, dt);
            draw(g, q);
        }
    }

    private static P drop(int w, int h, float k) {
        P q = new P();
        q.kind = Kind.DROP;
        if (R.nextFloat() < 0.72f) {
            float e = R.nextFloat();
            e = e * e * 0.32f;
            q.x = (R.nextBoolean() ? e : 1f - e) * w;
        } else {
            q.x = 4 + R.nextFloat() * (w - 8);
        }
        q.y = q.y0 = 4 + R.nextFloat() * (h * 0.8f);
        q.u = 2;
        int rad = 1 + R.nextInt(3);
        q.dist = (20 + R.nextFloat() * 120) * (0.6f + rad * 0.3f);
        q.hold = 0.3f + R.nextFloat() * (3.5f - 2.5f * k);
        q.vmax = 25 + R.nextFloat() * 90;
        q.accel = 30 + R.nextFloat() * 90;
        q.color = rad;
        q.life = q.hold + 6f + R.nextFloat() * 5f;
        q.sliding = R.nextFloat() < 0.8f;
        q.vx = (R.nextFloat() - 0.5f) * 3f;
        return q;
    }

    private static P bubble(int w, int h) {
        P q = mote(w, h, pick(0xBFE8FF, 0x8FD0F0, 0xFFFFFF), -(22 + R.nextFloat() * 30), 7, 3.5f);
        q.kind = Kind.BUBBLE;
        q.u = R.nextFloat() < 0.3f ? 3 : 2;
        q.y = h * (0.35f + R.nextFloat() * 0.65f);
        return q;
    }

    private static P dust(int w, int h, int c1, int c2) {
        P q = new P();
        q.kind = Kind.DUST;
        q.u = 2 + R.nextInt(2);
        q.x = 4 + R.nextFloat() * (w - 8);
        q.y = 4 + R.nextFloat() * (h - 8);
        q.life = 1.6f + R.nextFloat() * 3.4f;
        q.color = R.nextBoolean() ? c1 : c2;
        int n = 3 + R.nextInt(7);
        q.mid = new float[n * 2];
        for (int i = 0; i < n; i++) {
            q.mid[i * 2] = R.nextInt(5) - 2;
            q.mid[i * 2 + 1] = R.nextInt(5) - 2;
        }
        return q;
    }

    private static P flake(int w, int h) {
        P q = dust(w, h, 0xFFFFFF, 0xDDE8F5);
        q.kind = Kind.FLAKE;
        q.life = 2.5f + R.nextFloat() * 3.5f;
        return q;
    }

    private static P mote(int w, int h, int color, float vy, float vx, float life) {
        P q = new P();
        q.kind = Kind.MOTE;
        q.u = 2;
        if (R.nextFloat() < 0.75f) {
            float e = R.nextFloat() * 0.16f;
            q.x = (R.nextBoolean() ? e : 1f - e) * w;
            q.y = R.nextFloat() * h;
        } else {
            q.x = R.nextFloat() * w;
            q.y = R.nextBoolean() ? R.nextFloat() * h * 0.1f : h - R.nextFloat() * h * 0.1f;
        }
        q.color = color;
        q.vy = vy * (0.6f + R.nextFloat() * 0.8f);
        q.vx = vx * (0.6f + R.nextFloat() * 0.8f);
        q.life = life * (0.6f + R.nextFloat() * 0.8f);
        q.phase = R.nextFloat() * 6.28f;
        q.spin = 0.8f + R.nextFloat() * 2f;
        return q;
    }

    private static int pick(int... c) {
        return c[R.nextInt(c.length)];
    }

    private static P ambient(String b, boolean nether, boolean end, int w, int h) {
        if (nether) {
            if (b.contains("crimson")) return mote(w, h, pick(0xFF3A3A, 0xC41E3A, 0x7A0610), -10, 6, 4.5f);
            if (b.contains("warped")) return mote(w, h, pick(0x2DE0C8, 0x1AA89A, 0x7AF5E0), -10, 6, 4.5f);
            if (b.contains("soul")) return mote(w, h, pick(0x6CE8F5, 0x3FB5D0, 0xB8F6FF), -14, 4, 5f);
            if (b.contains("basalt")) return mote(w, h, pick(0x8A8A8A, 0x5A5A5A, 0xB0B0B0), 14, 6, 5f);
            return mote(w, h, pick(0xFF9A2A, 0xFF6A1A, 0xFFD060), -18, 8, 3.5f);
        }
        if (end) return mote(w, h, pick(0xC77DFF, 0x9B5DE5, 0xE8D5FF, 0xFFFFFF), -6, 5, 5.5f);
        boolean leaf = true;
        P q;
        if (b.contains("cherry")) q = mote(w, h, pick(0xFFB7D5, 0xFF9EC4, 0xFFD0E4), 16, 9, 7f);
        else if (b.contains("desert") || b.contains("badlands")) {
            q = mote(w, h, pick(0xD9B77A, 0xB08A52), 6, 22, 4f);
            leaf = false;
        } else if (b.contains("snowy") || b.contains("frozen") || b.contains("ice") || b.contains("grove") || b.contains("peaks") || b.contains("slopes")) {
            q = mote(w, h, pick(0xFFFFFF, 0xDDE8F5), 14, 6, 6f);
            leaf = false;
        } else if (b.contains("jungle") || b.contains("bamboo")) q = mote(w, h, pick(0x2E8B2E, 0x4CAF50, 0x1E6B1E), 18, 8, 6f);
        else if (b.contains("swamp") || b.contains("mangrove")) {
            q = mote(w, h, pick(0xC8E86A, 0xA0D040), -6, 5, 6f);
            leaf = false;
        } else if (b.contains("dark_forest")) q = mote(w, h, pick(0x4A6B2A, 0x6B4A22), 15, 8, 6f);
        else if (b.contains("birch")) q = mote(w, h, pick(0xE8D870, 0xB8C84A), 15, 8, 6f);
        else if (b.contains("forest") || b.contains("taiga")) q = mote(w, h, pick(0x5FA83A, 0x8BC34A, 0xC78A2A), 15, 8, 6f);
        else if (b.contains("ocean") || b.contains("beach") || b.contains("river")) {
            q = mote(w, h, pick(0xFFFFFF, 0xCFE8F5), 10, 14, 4f);
            leaf = false;
        } else if (b.contains("mushroom")) {
            q = mote(w, h, pick(0xB070C8, 0xE0A0E8), -6, 5, 6f);
            leaf = false;
        } else {
            q = mote(w, h, pick(0xF5E96A, 0xFFFFFF, 0xA8D86A), 12, 10, 6f);
            leaf = false;
        }
        if (leaf) q.kind = Kind.LEAF;
        return q;
    }

    private static void update(P q, float dt) {
        switch (q.kind) {
            case DROP -> {
                if (q.sliding && !q.stopped && q.age > q.hold) {
                    q.vy = Math.min(q.vmax, q.vy + q.accel * dt);
                    q.y += q.vy * dt;
                    q.x += q.vx * dt * (float) Math.sin(q.age * 3f);
                    if (q.y - q.y0 >= q.dist) {
                        q.stopped = true;
                        q.life = Math.min(q.life, q.age + 2.5f);
                    }
                }
            }
            case MOTE, LEAF, BUBBLE -> {
                q.x += (q.vx + (float) Math.sin(q.age * q.spin + q.phase) * Math.abs(q.vx) * 0.8f) * dt;
                q.y += q.vy * dt;
            }
            default -> {
            }
        }
    }

    private static float fade(P q) {
        float in = Gfx.clamp(q.age / (q.kind == Kind.DROP ? 0.25f : 0.5f));
        float out = Gfx.clamp((q.life - q.age) / (q.kind == Kind.DROP ? 2f : 0.9f));
        return Math.min(in, out);
    }

    private static void draw(GuiGraphics g, P q) {
        float a = fade(q);
        if (a <= 0.01f) return;
        int x = (int) q.x;
        int y = (int) q.y;
        switch (q.kind) {
            case DROP -> {
                int rad = q.color;
                int u = q.u;
                float rx = rad * 0.75f + 0.4f;
                float ry = rad * 1.05f + 0.6f;
                int tail = (int) (q.y - q.y0);
                if (tail > 2) {
                    int tw = Math.max(1, u - 1);
                    g.fillGradient(x - tw / 2, (int) q.y0, x - tw / 2 + tw, y, Gfx.rgba(0xA9BDCB, 0f), Gfx.rgba(0xA9BDCB, a * 0.3f));
                }
                int ex = (int) Math.ceil(rx);
                int ey = (int) Math.ceil(ry);
                for (int dy = -ey; dy <= ey; dy++) {
                    for (int dx = -ex; dx <= ex; dx++) {
                        double d = (dx * dx) / (double) (rx * rx) + (dy * dy) / (double) (ry * ry);
                        if (d > 1.0) continue;
                        boolean low = dy > 0 && d > 0.55;
                        int col = low ? 0x4F6578 : 0xAFC3D2;
                        float al = low ? 0.55f : d > 0.6 ? 0.34f : 0.2f;
                        g.fill(x + dx * u, y + dy * u, x + dx * u + u, y + dy * u + u, Gfx.rgba(col, a * al));
                    }
                }
                g.fill(x - u, y - ey * u + u, x, y - ey * u + 2 * u, Gfx.rgba(0xFFFFFF, a * 0.85f));
            }
            case BUBBLE -> {
                int u = q.u;
                int c = Gfx.rgba(q.color, a * 0.7f);
                g.fill(x + u, y, x + 2 * u, y + u, c);
                g.fill(x + u, y + 2 * u, x + 2 * u, y + 3 * u, c);
                g.fill(x, y + u, x + u, y + 2 * u, c);
                g.fill(x + 2 * u, y + u, x + 3 * u, y + 2 * u, c);
                g.fill(x + u, y + u, x + 2 * u, y + 2 * u, Gfx.rgba(q.color, a * 0.15f));
            }
            case DUST, FLAKE -> {
                for (int i = 0; i < q.mid.length; i += 2) {
                    int px = x + (int) q.mid[i] * q.u;
                    int py = y + (int) q.mid[i + 1] * q.u;
                    g.fill(px, py, px + q.u, py + q.u, Gfx.rgba(q.color, a * 0.75f));
                }
            }
            case MOTE -> {
                g.fill(x, y, x + q.u, y + q.u, Gfx.rgba(q.color, a * 0.85f));
                g.fill(x - 1, y - 1, x + q.u + 1, y + q.u + 1, Gfx.rgba(q.color, a * 0.18f));
            }
            case LEAF -> {
                boolean flip = Math.sin(q.age * q.spin * 2f + q.phase) > 0;
                int u = q.u;
                int c = Gfx.rgba(q.color, a * 0.9f);
                if (flip) {
                    g.fill(x, y, x + u * 2, y + u, c);
                    g.fill(x + u, y + u, x + u * 2, y + u * 2, c);
                } else {
                    g.fill(x, y + u, x + u * 2, y + u * 2, c);
                    g.fill(x, y, x + u, y + u, c);
                }
            }
        }
    }
}
