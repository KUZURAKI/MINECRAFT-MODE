package dev.zedkodex.modehud.client;

import dev.zedkodex.modehud.Config;
import dev.zedkodex.modehud.ModeHud;
import net.minecraft.Util;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = ModeHud.MODID, value = Dist.CLIENT)
public final class SoulView {
    private SoulView() {}

    private static final float DISTANCE = 2.2f;
    private static final float SCREEN_X = 0.2f;
    private static final float OPEN_MS = 1100f;
    private static final float CLOSE_MS = 800f;

    private static float raw;
    private static boolean target;
    private static long lastMs;
    private static CameraType saved;
    private static boolean fromFirst;
    private static boolean swapped;
    private static float saveBody, saveBodyO, saveHead, saveHeadO, savePitch, savePitchO;

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (!target && raw <= 0f) {
            saved = mc.options.getCameraType();
            fromFirst = saved.isFirstPerson();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        target = true;
    }

    public static void close() {
        target = false;
    }

    public static float raw() {
        return raw;
    }

    public static float progress() {
        return Gfx.smooth(raw);
    }

    private static void advance() {
        long now = Util.getMillis();
        float dt = lastMs == 0 ? 0f : Math.min(100, now - lastMs);
        lastMs = now;
        if (target) raw = Math.min(1f, raw + dt / OPEN_MS);
        else if (raw > 0f) {
            raw = Math.max(0f, raw - dt / CLOSE_MS);
            if (raw <= 0f && saved != null) {
                Minecraft.getInstance().options.setCameraType(saved);
                saved = null;
            }
        }
    }

    @SubscribeEvent
    static void onOpening(ScreenEvent.Opening event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getNewScreen() != null && event.getNewScreen().getClass() == InventoryScreen.class
                && mc.player != null && !mc.player.isCreative() && !mc.player.isSpectator()
                && Config.CLIENT.soulInventory.get() && HudState.active) {
            event.setNewScreen(new SoulInventoryScreen(mc.player));
        }
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        restore();
        if (target && !(Minecraft.getInstance().screen instanceof SoulInventoryScreen)) target = false;
        if (!target && raw <= 0f && saved != null) {
            Minecraft.getInstance().options.setCameraType(saved);
            saved = null;
        }
    }

    @SubscribeEvent
    static void onAngles(ViewportEvent.ComputeCameraAngles event) {
        advance();
        float e = progress();
        if (e <= 0f) return;
        event.setPitch(Mth.lerp(e, event.getPitch(), 5f));
    }

    @SubscribeEvent
    static void onDistance(CalculateDetachedCameraDistanceEvent event) {
        float e = progress();
        if (e <= 0f) return;
        Minecraft mc = Minecraft.getInstance();
        double aspect = (double) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        double tanV = Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
        double shift = DISTANCE * (0.5 - SCREEN_X) * 2.0 * tanV * aspect * e;
        net.minecraft.client.Camera cam = event.getCamera();
        org.joml.Vector3f left = cam.getLeftVector();
        net.minecraft.world.phys.Vec3 from = cam.getPosition();
        net.minecraft.world.phys.Vec3 to = from.add(-left.x * shift, -left.y * shift, -left.z * shift);
        if (mc.level != null) {
            net.minecraft.world.phys.HitResult hit = mc.level.clip(new net.minecraft.world.level.ClipContext(from, to,
                    net.minecraft.world.level.ClipContext.Block.VISUAL, net.minecraft.world.level.ClipContext.Fluid.NONE, cam.getEntity()));
            if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                net.minecraft.world.phys.Vec3 at = hit.getLocation();
                to = at.add(from.subtract(at).normalize().scale(0.2));
            }
        }
        cam.setPosition(to);
        event.setDistance(event.getDistance() * (1f - e) + DISTANCE * e);
    }

    @SubscribeEvent
    static void onPlayerPre(RenderPlayerEvent.Pre event) {
        if (fromFirst && event.getEntity() == Minecraft.getInstance().player && progress() < 0.22f && raw > 0f) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onArm(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof AbstractClientPlayer p && p == Minecraft.getInstance().player
                && event.getRenderer().getModel() instanceof PlayerModel<?> model && progress() > 0.3f && !swapped) {
            model.rightArmPose = HumanoidModel.ArmPose.BLOCK;
            float e = progress();
            saveBody = p.yBodyRot;
            saveBodyO = p.yBodyRotO;
            saveHead = p.yHeadRot;
            saveHeadO = p.yHeadRotO;
            savePitch = p.getXRot();
            savePitchO = p.xRotO;
            float body = Mth.rotLerp(event.getPartialTick(), p.yBodyRotO, p.yBodyRot);
            p.yBodyRot = p.yBodyRotO = body + 28f * e;
            p.yHeadRot = p.yHeadRotO = body + 62f * e;
            p.setXRot(Mth.lerp(e, savePitch, 6f));
            p.xRotO = p.getXRot();
            swapped = true;
        }
    }

    @SubscribeEvent
    static void onArmPost(RenderLivingEvent.Post<?, ?> event) {
        restore();
    }

    private static void restore() {
        net.minecraft.world.entity.player.Player p = Minecraft.getInstance().player;
        if (swapped && p != null) {
            p.yBodyRot = saveBody;
            p.yBodyRotO = saveBodyO;
            p.yHeadRot = saveHead;
            p.yHeadRotO = saveHeadO;
            p.setXRot(savePitch);
            p.xRotO = savePitchO;
            swapped = false;
        }
    }
}
