package dev.zedkodex.modehud.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.zedkodex.modehud.net.ClientState;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

public class BackpackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public BackpackLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!ClientState.backpacks.contains(player.getId()) || player.isInvisible()) return;
        PartsMesh mesh = PartsMesh.get("backpack");
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        pose.translate(0.0, 0.7, 0.14);
        pose.scale(1.0f, -1.0f, 1.0f);
        mesh.render(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(mesh.texture)), light, null);
        pose.popPose();
    }
}
