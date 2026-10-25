package dev.zedkodex.modehud.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.zedkodex.modehud.ModeHud;
import dev.zedkodex.modehud.entity.Motorcycle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class MotorcycleRenderer extends EntityRenderer<Motorcycle> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "textures/entity/motorcycle.png");
    private static final ResourceLocation GLOW =
            ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "textures/entity/motorcycle_glow.png");

    public MotorcycleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.8f;
    }

    @Override
    public void render(Motorcycle entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffer, int light) {
        MotorcycleMesh mesh = MotorcycleMesh.get();
        float lean = Mth.lerp(partial, entity.leanO, entity.lean);
        float pitch = Mth.lerp(partial, entity.pitchO, entity.pitch);
        float wheel = Mth.lerp(partial, entity.wheelO, entity.wheel);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        pose.mulPose(Axis.ZP.rotationDegrees(lean));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.translate(0f, mesh.ground(), 0f);
        mesh.render(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, wheel, lean * 1.2f, 0xFFFFFFFF);
        mesh.render(pose, buffer.getBuffer(RenderType.eyes(GLOW)), LightTexture.FULL_BRIGHT, wheel, lean * 1.2f, 0xFFFFFFFF);
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Motorcycle entity) {
        return TEXTURE;
    }
}
