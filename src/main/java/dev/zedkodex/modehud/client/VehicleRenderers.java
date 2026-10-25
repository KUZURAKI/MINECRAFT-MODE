package dev.zedkodex.modehud.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.zedkodex.modehud.entity.Car;
import dev.zedkodex.modehud.entity.Plane;
import dev.zedkodex.modehud.entity.Tarantula;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class VehicleRenderers {
    private VehicleRenderers() {}

    public static class CarRenderer extends EntityRenderer<Car> {
        public CarRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
            shadowRadius = 1.2f;
        }

        @Override
        public void render(Car e, float yaw, float partial, PoseStack pose, MultiBufferSource buffer, int light) {
            PartsMesh mesh = PartsMesh.get("car");
            float wheel = Mth.lerp(partial, e.wheelO, e.wheel);
            float steer = Mth.lerp(partial, e.steerO, e.steer);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(180f - yaw));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, e.rollO, e.roll)));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial, e.pitchO, e.pitch)));
            mesh.render(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(mesh.texture)), light, (part, ps) -> {
                if (!part.name().startsWith("wheel")) return;
                float[] pv = part.pivot();
                ps.translate(pv[0], pv[1], pv[2]);
                if (part.name().startsWith("wheel_f")) ps.mulPose(Axis.YP.rotationDegrees(-steer));
                ps.mulPose(Axis.XP.rotation(-wheel));
                ps.translate(-pv[0], -pv[1], -pv[2]);
            });
            pose.popPose();
            super.render(e, yaw, partial, pose, buffer, light);
        }

        @Override
        public ResourceLocation getTextureLocation(Car e) {
            return PartsMesh.get("car").texture;
        }
    }

    public static class PlaneRenderer<T extends Plane> extends EntityRenderer<T> {
        private final String name;
        private final float pivotY;

        public PlaneRenderer(EntityRendererProvider.Context ctx, String name, float pivotY, float shadow) {
            super(ctx);
            this.name = name;
            this.pivotY = pivotY;
            this.shadowRadius = shadow;
        }

        @Override
        public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buffer, int light) {
            PartsMesh mesh = PartsMesh.get(name);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(180f - yaw));
            pose.translate(0f, pivotY, 0f);
            pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partial, e.xRotO, e.getXRot())));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, e.rollO, e.roll)));
            pose.translate(0f, -pivotY, 0f);
            mesh.render(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(mesh.texture)), light, null);
            pose.popPose();
            super.render(e, yaw, partial, pose, buffer, light);
        }

        @Override
        public ResourceLocation getTextureLocation(T e) {
            return PartsMesh.get(name).texture;
        }
    }

    public static class TarantulaRenderer extends EntityRenderer<Tarantula> {
        public TarantulaRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
            shadowRadius = 1.6f;
        }

        @Override
        public void render(Tarantula e, float yaw, float partial, PoseStack pose, MultiBufferSource buffer, int light) {
            RigMesh rig = RigMesh.get("tarantula");
            float speed = e.walkAnimation.speed(partial);
            String anim = "idle";
            float time = (e.tickCount + partial) / 20f;
            if (e.shootAnim > 0 || (e.getTarget() != null && e.isAggressive() && e.distanceToSqr(e.getTarget()) < 25)) {
                anim = "attack";
            } else if (speed > 0.04f) {
                anim = "walk";
                time = e.walkAnimation.position(partial) * rig.length("walk") / 7f;
            }
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(180f - Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            if (e.deathTime > 0) {
                pose.mulPose(Axis.ZP.rotationDegrees(Math.min(1f, (e.deathTime + partial) / 20f) * 90f));
            }
            rig.render(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(rig.texture)), light, anim, time);
            pose.popPose();
            super.render(e, yaw, partial, pose, buffer, light);
        }

        @Override
        public ResourceLocation getTextureLocation(Tarantula e) {
            return RigMesh.get("tarantula").texture;
        }
    }
}
