package dev.zedkodex.modehud.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.zedkodex.modehud.Config;
import dev.zedkodex.modehud.ModeHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = ModeHud.MODID, value = Dist.CLIENT)
public final class Footprints {
    private Footprints() {}

    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "textures/misc/footprint.png");
    private static final double STRIDE = 0.75;

    private record Print(double x, double y, double z, float yaw, boolean right, int color, long born, int life) {}

    private static final List<Print> prints = new ArrayList<>();
    private static Vec3 last;
    private static boolean side;

    private static int material(BlockState s) {
        if (s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW)) return 0x7C8FA8;
        if (s.is(BlockTags.SAND)) return s.is(Blocks.RED_SAND) ? 0x7A4A2A : 0x8A7440;
        if (s.is(Blocks.MUD)) return 0x241A12;
        if (s.is(Blocks.CLAY)) return 0x6A7280;
        if (s.is(Blocks.GRAVEL)) return 0x4A4642;
        if (s.is(Blocks.SOUL_SAND) || s.is(Blocks.SOUL_SOIL)) return 0x1E1612;
        if (s.is(Blocks.PODZOL) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.DIRT) || s.is(Blocks.ROOTED_DIRT)) return 0x2E1F12;
        if (s.is(Blocks.MYCELIUM)) return 0x3A3040;
        if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT_PATH) || s.is(Blocks.FARMLAND)) return 0x20300F;
        return -1;
    }

    private static int lifeOf(BlockState s) {
        if (s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW)) return 1400;
        if (s.is(BlockTags.SAND)) return 500;
        if (s.is(Blocks.GRASS_BLOCK)) return 260;
        return 800;
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        ClientLevel lv = mc.level;
        if (p == null || lv == null) {
            prints.clear();
            last = null;
            return;
        }
        long now = lv.getGameTime();
        prints.removeIf(pr -> now - pr.born() > pr.life());
        if (!Config.CLIENT.weatherEffects.get() || p.isCreative() || p.isSpectator() || HudState.dead) return;

        BlockPos head = BlockPos.containing(p.getEyePosition());
        if (!p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER) && lv.getBiome(head).value().coldEnoughToSnow(head)) {
            long t = now % 80;
            if (t < 14 && t % 2 == 0) {
                Vec3 look = p.getLookAngle();
                Vec3 at = p.getEyePosition().add(look.scale(0.32)).add(0, -0.17, 0);
                lv.addParticle(ParticleTypes.CLOUD, at.x, at.y, at.z, look.x * 0.025, 0.012, look.z * 0.025);
            }
        }

        if (p.isPassenger() || !p.onGround() || p.isInWater() || p.isSwimming() || p.isFallFlying() || p.isShiftKeyDown()) return;
        Vec3 pos = p.position();
        if (last == null) {
            last = pos;
            return;
        }
        double dx = pos.x - last.x;
        double dz = pos.z - last.z;
        if (dx * dx + dz * dz < STRIDE * STRIDE) return;
        last = pos;
        side = !side;
        float yaw = (float) Math.atan2(-dx, dz);
        double off = side ? 0.14 : -0.14;
        double fx = pos.x - Math.cos(yaw) * off;
        double fz = pos.z - Math.sin(yaw) * off;
        BlockPos bp = BlockPos.containing(fx, p.getY() - 0.2, fz);
        BlockState st = lv.getBlockState(bp);
        if (st.getShape(lv, bp).isEmpty()) {
            bp = bp.below();
            st = lv.getBlockState(bp);
        }
        int col = material(st);
        if (col < 0) return;
        double top = bp.getY() + st.getShape(lv, bp).max(Direction.Axis.Y);
        if (Math.abs(top - p.getY()) > 0.35) return;
        if (prints.size() > 90) prints.remove(0);
        prints.add(new Print(fx, top + 0.012, fz, yaw, side, col, now, lifeOf(st)));
    }

    @SubscribeEvent
    static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || prints.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel lv = mc.level;
        if (lv == null) return;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource bs = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityTranslucent(TEX);
        VertexConsumer vc = bs.getBuffer(type);
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double now = lv.getGameTime() + pt;
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        org.joml.Matrix4f m = ps.last().pose();
        for (Print pr : prints) {
            double age = now - pr.born();
            float a = (float) Math.min(1.0, (pr.life() - age) / 80.0) * Math.min(1f, (float) age / 2f + 0.3f) * 0.6f;
            if (a <= 0.01f) continue;
            float fx = (float) -Math.sin(pr.yaw());
            float fz = (float) Math.cos(pr.yaw());
            float rx = -fz;
            float rz = fx;
            float hl = 0.29f;
            float hw = 0.16f;
            int light = LevelRenderer.getLightColor(lv, BlockPos.containing(pr.x(), pr.y() + 0.1, pr.z()));
            int r = pr.color() >> 16 & 255;
            int gC = pr.color() >> 8 & 255;
            int b = pr.color() & 255;
            int al = (int) (a * 255);
            float u0 = pr.right() ? 1f : 0f;
            float u1 = 1f - u0;
            quad(vc, m, pr, fx, fz, rx, rz, hl, hw, u0, u1, r, gC, b, al, light);
        }
        ps.popPose();
        bs.endBatch(type);
    }

    private static void quad(VertexConsumer vc, org.joml.Matrix4f m, Print pr, float fx, float fz, float rx, float rz, float hl, float hw,
                             float u0, float u1, int r, int g, int b, int a, int light) {
        float x = (float) pr.x();
        float y = (float) pr.y();
        float z = (float) pr.z();
        vertex(vc, m, x + fx * hl + rx * hw, y, z + fz * hl + rz * hw, u0, 0f, r, g, b, a, light);
        vertex(vc, m, x + fx * hl - rx * hw, y, z + fz * hl - rz * hw, u1, 0f, r, g, b, a, light);
        vertex(vc, m, x - fx * hl - rx * hw, y, z - fz * hl - rz * hw, u1, 1f, r, g, b, a, light);
        vertex(vc, m, x - fx * hl + rx * hw, y, z - fz * hl + rz * hw, u0, 1f, r, g, b, a, light);
    }

    private static void vertex(VertexConsumer vc, org.joml.Matrix4f m, float x, float y, float z, float u, float v, int r, int g, int b, int a, int light) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0f, 1f, 0f);
    }
}
