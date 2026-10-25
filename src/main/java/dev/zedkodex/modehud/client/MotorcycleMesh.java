package dev.zedkodex.modehud.client;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.zedkodex.modehud.ModeHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class MotorcycleMesh {
    private static final ResourceLocation DATA = ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "geo/motorcycle.json");

    private record Group(String name, float[] v, float[] n) {}

    private static MotorcycleMesh instance;

    private final List<Group> groups = new ArrayList<>();
    private final Set<String> steerGroups = new HashSet<>();
    private final float ground;
    private final Vector3f steerTop;
    private final Vector3f steerAxis;
    private final Vector3f frontAxle;
    private final Vector3f rearAxle;

    public static MotorcycleMesh get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static MotorcycleMesh load() {
        try (Reader r = Minecraft.getInstance().getResourceManager().getResourceOrThrow(DATA).openAsReader()) {
            return new MotorcycleMesh(JsonParser.parseReader(r).getAsJsonObject());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load " + DATA, e);
        }
    }

    private static Vector3f vec(JsonArray a) {
        return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
    }

    private MotorcycleMesh(JsonObject json) {
        ground = json.get("ground").getAsFloat();
        JsonObject steer = json.getAsJsonObject("steer");
        for (JsonElement e : steer.getAsJsonArray("groups")) steerGroups.add(e.getAsString());
        steerTop = vec(steer.getAsJsonArray("top"));
        frontAxle = vec(steer.getAsJsonArray("bottom"));
        steerAxis = new Vector3f(frontAxle).sub(steerTop).normalize();
        JsonObject wheels = json.getAsJsonObject("wheels");
        rearAxle = vec(wheels.getAsJsonArray("wheel1"));
        for (JsonElement e : json.getAsJsonArray("groups")) {
            JsonObject g = e.getAsJsonObject();
            JsonArray a = g.getAsJsonArray("v");
            float[] v = new float[a.size()];
            for (int i = 0; i < v.length; i++) v[i] = a.get(i).getAsFloat();
            groups.add(new Group(g.get("name").getAsString(), v, normals(v)));
        }
    }

    private static float[] normals(float[] v) {
        int tris = v.length / 15;
        float[] n = new float[tris * 3];
        for (int t = 0; t < tris; t++) {
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

    public float ground() {
        return ground;
    }

    public void render(PoseStack pose, VertexConsumer vc, int light, float wheelRot, float steerDeg, int color) {
        for (Group g : groups) {
            pose.pushPose();
            boolean steered = steerGroups.contains(g.name());
            if (steered) {
                pose.translate(steerTop.x, steerTop.y, steerTop.z);
                pose.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(steerDeg), steerAxis.x, steerAxis.y, steerAxis.z));
                pose.translate(-steerTop.x, -steerTop.y, -steerTop.z);
            }
            if (g.name().equals("wheel0")) spin(pose, frontAxle, wheelRot);
            else if (g.name().equals("wheel1")) spin(pose, rearAxle, wheelRot);
            emit(pose, vc, light, g, color);
            pose.popPose();
        }
    }

    private static void spin(PoseStack pose, Vector3f axle, float rot) {
        pose.translate(axle.x, axle.y, axle.z);
        pose.mulPose(Axis.XP.rotation(-rot));
        pose.translate(-axle.x, -axle.y, -axle.z);
    }

    private static void emit(PoseStack pose, VertexConsumer vc, int light, Group g, int color) {
        PoseStack.Pose last = pose.last();
        float[] v = g.v();
        float[] n = g.n();
        for (int t = 0; t < v.length / 15; t++) {
            int o = t * 15;
            for (int k = 0; k < 4; k++) {
                int i = o + Math.min(k, 2) * 5;
                vc.addVertex(last, v[i], v[i + 1], v[i + 2])
                        .setColor(color)
                        .setUv(v[i + 3], v[i + 4])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(last, n[t * 3], n[t * 3 + 1], n[t * 3 + 2]);
            }
        }
    }
}
