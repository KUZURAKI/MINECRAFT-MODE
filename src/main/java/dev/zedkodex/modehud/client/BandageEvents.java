package dev.zedkodex.modehud.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.zedkodex.modehud.Config;
import dev.zedkodex.modehud.ModeHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

@EventBusSubscriber(modid = ModeHud.MODID, value = Dist.CLIENT)
public class BandageEvents {
    private static final float REVOLUTIONS = 6f;

    @SubscribeEvent
    static void onArmPose(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof AbstractClientPlayer p
                && event.getRenderer().getModel() instanceof PlayerModel<?> model
                && BandageState.wrapping(p)) {
            model.leftArmPose = HumanoidModel.ArmPose.BLOCK;
        }
    }

    @SubscribeEvent
    static void onMove(MovementInputUpdateEvent event) {
        if (event.getEntity() instanceof AbstractClientPlayer p && BandageState.wrapping(p)) {
            float k = Config.CLIENT.wrapSpeed.get().floatValue() / 0.2f;
            event.getInput().leftImpulse *= k;
            event.getInput().forwardImpulse *= k;
        }
    }

    @SubscribeEvent
    static void onHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !BandageState.wrapping(p)) return;
        event.setCanceled(true);

        PoseStack pose = event.getPoseStack();
        PlayerRenderer renderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(p);
        float progress = BandageState.progress(p, event.getPartialTick());
        float angle = progress * REVOLUTIONS * (float) Math.PI * 2f;
        float along = (progress * 3f) % 1f;

        pose.pushPose();
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            pose.mulPose(Axis.ZP.rotationDegrees(-14f));
            pose.mulPose(Axis.XP.rotationDegrees(-10f));
            place(pose, 1f, -0.54f, 0.24f, 0.05f);
            renderer.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), p);
            if (BandageModel.shared != null) {
                pose.pushPose();
                renderer.getModel().rightArm.translateAndRotate(pose);
                BandageModel.shared.render(pose, event.getMultiBufferSource(), event.getPackedLight(), BandageState.rings(p), true);
                pose.popPose();
            }
        } else {
            place(pose, -1f, 0.80f, 0.32f, 0f);
            net.minecraft.client.model.geom.ModelPart arm = renderer.getModel().leftArm;
            float hx = arm.x / 16f;
            float hy = (arm.y + 1f) / 16f;
            pose.translate(hx, hy, 0f);
            pose.mulPose(Axis.XP.rotationDegrees(18f * (float) Math.cos(angle) + (along - 0.5f) * 10f));
            pose.mulPose(Axis.ZP.rotationDegrees(18f * (float) Math.sin(angle)));
            pose.mulPose(Axis.YP.rotationDegrees((float) Math.toDegrees(angle) * 0.3f));
            pose.translate(-hx, -hy, 0f);
            renderer.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), p);
        }
        pose.popPose();
    }

    private static void place(PoseStack pose, float side, float dx, float dy, float dz) {
        pose.translate(side * 0.64000005F + dx, -0.6F + dy, -0.71999997F + dz);
        pose.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
        pose.translate(side * -1.0F, 3.6F, 3.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
        pose.mulPose(Axis.XP.rotationDegrees(200.0F));
        pose.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
        pose.translate(side * 5.6F, 0.0F, 0.0F);
    }
}
