package dev.zedkodex.modehud.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.zedkodex.modehud.ModeHud;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class BandageModel {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "bandage"), "main");
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "textures/entity/bandage.png");

    public static BandageModel shared;

    private final ModelPart[] left = new ModelPart[3];
    private final ModelPart[] right = new ModelPart[3];

    public BandageModel(ModelPart root) {
        for (int i = 0; i < 3; i++) {
            left[i] = root.getChild("left" + i);
            right[i] = root.getChild("right" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < 3; i++) {
            float y = 3.0F + 2.0F * i;
            root.addOrReplaceChild("left" + i,
                    CubeListBuilder.create().texOffs(0, 6 * i).addBox(-1.0F, y, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.3F)),
                    PartPose.ZERO);
            root.addOrReplaceChild("right" + i,
                    CubeListBuilder.create().texOffs(0, 6 * i).addBox(-3.0F, y, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.3F)),
                    PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    public void render(PoseStack pose, MultiBufferSource buffer, int light, int count, boolean rightArm) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        ModelPart[] rings = rightArm ? right : left;
        for (int i = 0; i < Math.min(count, rings.length); i++) {
            rings[i].render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        }
    }
}
