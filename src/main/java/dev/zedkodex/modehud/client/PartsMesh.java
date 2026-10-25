package dev.zedkodex.modehud.client;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.zedkodex.modehud.ModeHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

public final class PartsMesh {
    public record Part(String name, float[] pivot, float[] v, float[] n) {}

    public interface PartFn {
        void apply(Part part, PoseStack pose);
    }

    private static final Map<String, PartsMesh> CACHE = new HashMap<>();

    public final List<Part> parts = new ArrayList<>();
    public final Map<String, float[]> anchors = new HashMap<>();
    public final ResourceLocation texture;

    public static PartsMesh get(String name) {
        return CACHE.computeIfAbsent(name, PartsMesh::new);
    }

    public static void clear() {
        CACHE.clear();
    }

    private PartsMesh(String name) {
        texture = ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "textures/entity/" + name + ".png");
        ResourceLocation data = ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "geo/" + name + ".json");
        try (Reader r = Minecraft.getInstance().getResourceManager().getResourceOrThrow(data).openAsReader()) {
            JsonObject json = JsonParser.parseReader(r).getAsJsonObject();
            for (JsonElement e : json.getAsJsonArray("parts")) {
                JsonObject p = e.getAsJsonObject();
                float[] v = floats(p.getAsJsonArray("v"));
                parts.add(new Part(p.get("name").getAsString(), floats(p.getAsJsonArray("pivot")), v, normals(v)));
            }
            JsonObject a = json.getAsJsonObject("anchors");
            for (String k : a.keySet()) anchors.put(k, floats(a.getAsJsonArray(k)));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot load " + data, ex);
        }
    }

    private static float[] floats(JsonArray a) {
        float[] f = new float[a.size()];
        for (int i = 0; i < f.length; i++) f[i] = a.get(i).getAsFloat();
        return f;
    }

    private static float[] normals(float[] v) {
        float[] n = new float[v.length / 15 * 3];
        for (int t = 0; t < n.length / 3; t++) {
            int o = t * 15;
            Vector3f a = new Vector3f(v[o + 5] - v[o], v[o + 6] - v[o + 1], v[o + 7] - v[o + 2]);
            Vector3f b = new Vector3f(v[o + 10] - v[o], v[o + 11] - v[o + 1], v[o + 12] - v[o + 2]);
            Vector3f c = a.cross(b);
            if (c.lengthSquared() > 1e-12f) c.normalize();
            n[t * 3] = c.x;
            n[t * 3 + 1] = c.y;
            n[t * 3 + 2] = c.z;
        }
        return n;
    }

    public void render(PoseStack pose, VertexConsumer vc, int light, PartFn fn) {
        for (Part p : parts) {
            pose.pushPose();
            if (fn != null) fn.apply(p, pose);
            PoseStack.Pose last = pose.last();
            for (int t = 0; t < p.v().length / 15; t++) {
                int o = t * 15;
                for (int k = 0; k < 4; k++) {
                    int i = o + Math.min(k, 2) * 5;
                    vc.addVertex(last, p.v()[i], p.v()[i + 1], p.v()[i + 2])
                            .setColor(0xFFFFFFFF)
                            .setUv(p.v()[i + 3], p.v()[i + 4])
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(last, p.n()[t * 3], p.n()[t * 3 + 1], p.n()[t * 3 + 2]);
                }
            }
            pose.popPose();
        }
    }
}
