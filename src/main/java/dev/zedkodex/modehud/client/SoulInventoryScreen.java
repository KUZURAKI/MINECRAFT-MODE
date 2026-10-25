package dev.zedkodex.modehud.client;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

public class SoulInventoryScreen extends AbstractContainerScreen<InventoryMenu> {
    private static final int ACCENT = 0x8FE3FF;
    private static final float OPEN_MS = 900f;
    private static final float OPEN_DELAY = 250f;
    private static final float CLOSE_MS = 320f;
    private static final float TILT = (float) Math.toRadians(15.0);
    private static final int W = 188;
    private static final int H = 196;
    private static final float CX = W / 2f;
    private static final float CY = H / 2f;
    private static final float DEPTH_SCALE = 0.01f;

    private final Player player;
    private final int[] origX;
    private final int[] origY;
    private int rawX;
    private int rawY;
    private long openMs;
    private long closeMs;
    private boolean closing;
    private float scale = 1f;
    private float pivotX;
    private float pivotY;
    private float angle;
    private Matrix4f savedProj;
    private VertexSorting savedSort;
    private Matrix4f perspProj;

    public SoulInventoryScreen(Player player) {
        super(player.inventoryMenu, player.getInventory(), Component.translatable("container.crafting"));
        this.player = player;
        imageWidth = W;
        imageHeight = H;
        int n = menu.slots.size();
        origX = new int[n];
        origY = new int[n];
        for (int i = 0; i < n; i++) {
            origX[i] = menu.slots.get(i).x;
            origY[i] = menu.slots.get(i).y;
        }
        arrange();
    }

    private void put(int index, int cardX, int cardY) {
        Slot s = menu.slots.get(index);
        s.x = cardX + 1;
        s.y = cardY + 1;
    }

    private void arrange() {
        put(0, 165, 35);
        put(1, 105, 25);
        put(2, 125, 25);
        put(3, 105, 45);
        put(4, 125, 45);
        for (int i = 0; i < 4; i++) put(5 + i, 71, 5 + i * 20);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) put(9 + r * 9 + c, 5 + c * 20, 99 + r * 20);
        }
        for (int c = 0; c < 9; c++) put(36 + c, 5 + c * 20, 173);
        if (menu.slots.size() > 45) put(45, 5, 65);
    }

    @Override
    public void removed() {
        for (int i = 0; i < origX.length; i++) {
            Slot s = menu.slots.get(i);
            s.x = origX[i];
            s.y = origY[i];
        }
        super.removed();
    }

    @Override
    protected void init() {
        super.init();
        leftPos = 0;
        topPos = 0;
        if (openMs == 0) openMs = Util.getMillis();
        SoulView.open();
    }

    private float visibility(long now) {
        if (closing) return 1f - Gfx.smooth(Gfx.clamp((now - closeMs) / CLOSE_MS));
        return Gfx.smooth(Gfx.clamp((now - openMs - OPEN_DELAY) / OPEN_MS));
    }

    @Override
    public void onClose() {
        if (closing) return;
        closing = true;
        closeMs = Util.getMillis();
        SoulView.close();
    }

    private void layout(float v) {
        scale = Math.min(height * 0.8f / H, width * 0.44f / W);
        pivotX = width * 0.74f + (1f - v) * width * 0.65f;
        pivotY = height * 0.5f;
        angle = TILT * v;
    }

    private float depth() {
        return 0.8f * W * scale;
    }

    private Matrix4f pose() {
        return new Matrix4f().translate(pivotX, pivotY, 0f).rotateY(-angle).scale(scale, scale, DEPTH_SCALE).translate(-CX, -CY, 0f);
    }

    private Matrix4f perspective() {
        Matrix4f mv = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f w = new Matrix4f();
        w.m23(-1f / depth());
        Matrix4f persp = new Matrix4f().translate(pivotX, pivotY, 0f).mul(w).translate(-pivotX, -pivotY, 0f);
        return new Matrix4f(savedProj).mul(mv).mul(persp).mul(new Matrix4f(mv).invert());
    }

    private double[] unproject(double mx, double my) {
        double u = mx - pivotX;
        double v = my - pivotY;
        double s = Math.sin(angle);
        double c = Math.cos(angle);
        double d = depth();
        double qx = u / (scale * (c + u * s / d));
        double qy = v * (1.0 - scale * qx * s / d) / scale;
        return new double[]{CX + qx, CY + qy};
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        double[] m = unproject(x, y);
        return !closing && super.mouseClicked(m[0], m[1], button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        double[] m = unproject(x, y);
        return !closing && super.mouseReleased(m[0], m[1], button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        double[] m = unproject(x, y);
        return !closing && super.mouseDragged(m[0], m[1], button, dx / scale, dy / scale);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        double[] m = unproject(x, y);
        return super.mouseScrolled(m[0], m[1], sx, sy);
    }

    @Override
    public void mouseMoved(double x, double y) {
        double[] m = unproject(x, y);
        super.mouseMoved(m[0], m[1]);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = Util.getMillis();
        float v = visibility(now);
        if (closing && v <= 0f) {
            super.onClose();
            return;
        }
        layout(v);
        rawX = mx;
        rawY = my;
        double[] m = unproject(mx, my);

        g.flush();
        savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        savedSort = RenderSystem.getVertexSorting();
        perspProj = perspective();
        RenderSystem.setProjectionMatrix(perspProj, savedSort);
        g.pose().pushPose();
        g.pose().mulPose(pose());
        drawPanel(g, v);
        super.render(g, (int) Math.floor(m[0]), (int) Math.floor(m[1]), pt);
        g.pose().popPose();
        g.flush();
        RenderSystem.setProjectionMatrix(savedProj, savedSort);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int x, int y) {
        g.flush();
        RenderSystem.setProjectionMatrix(savedProj, savedSort);
        g.pose().pushPose();
        g.pose().last().pose().identity();
        super.renderTooltip(g, rawX, rawY);
        g.pose().popPose();
        g.flush();
        RenderSystem.setProjectionMatrix(perspProj, savedSort);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
    }

    private void box(GuiGraphics g, int x0, int y0, int x1, int y1, float v) {
        g.fill(x0 + 2, y0, x1 - 2, y1, Gfx.rgba(0x070A10, 0.86f * v));
        g.fill(x0, y0 + 2, x0 + 2, y1 - 2, Gfx.rgba(0x070A10, 0.86f * v));
        g.fill(x1 - 2, y0 + 2, x1, y1 - 2, Gfx.rgba(0x070A10, 0.86f * v));
        g.fill(x0 + 1, y0 + 1, x0 + 2, y0 + 2, Gfx.rgba(0x070A10, 0.86f * v));
        g.fill(x1 - 2, y0 + 1, x1 - 1, y0 + 2, Gfx.rgba(0x070A10, 0.86f * v));
        g.fill(x0 + 1, y1 - 2, x0 + 2, y1 - 1, Gfx.rgba(0x070A10, 0.86f * v));
        g.fill(x1 - 2, y1 - 2, x1 - 1, y1 - 1, Gfx.rgba(0x070A10, 0.86f * v));
        g.fill(x0 + 2, y0, x1 - 2, y0 + 1, Gfx.rgba(ACCENT, 0.45f * v));
        g.fill(x0 + 2, y1 - 1, x1 - 2, y1, Gfx.rgba(ACCENT, 0.12f * v));
    }

    private void card(GuiGraphics g, int x, int y, float v, boolean result, boolean active, boolean hover) {
        int o = hover ? 1 : 0;
        g.fill(x - o, y - o, x + 18 + o, y + 18 + o, Gfx.rgba(0x000000, 0.9f * v));
        g.fill(x + 1 - o, y + 1 - o, x + 17 + o, y + 17 + o, Gfx.rgba(result ? 0x1B2E40 : 0x12161F, 0.75f * v));
        g.fill(x + 1 - o, y + 1 - o, x + 17 + o, y + 2 - o, Gfx.rgba(0xFFFFFF, 0.28f * v));
        g.fill(x + 1 - o, y + 1 - o, x + 2 - o, y + 17 + o, Gfx.rgba(0xFFFFFF, 0.28f * v));
        g.fill(x + 1 - o, y + 16 + o, x + 17 + o, y + 17 + o, Gfx.rgba(0x000000, 0.35f * v));
        g.fill(x + 16 + o, y + 1 - o, x + 17 + o, y + 17 + o, Gfx.rgba(0x000000, 0.35f * v));
        if (result) g.fill(x + 1, y + 1, x + 17, y + 17, Gfx.rgba(ACCENT, 0.12f * v));
        if (active || hover) g.fill(x + 1, y + 1, x + 17, y + 17, Gfx.rgba(0xFFFFFF, (hover ? 0.1f : 0.16f) * v));
    }

    private void stat(GuiGraphics g, String icon, float x, float y, String text, float v) {
        Gfx.icon(g, icon, x + 5, y + 5, 11, 0f, 1f, v);
        Gfx.text(g, text, x + 13, y + 1, Gfx.rgba(0xFFFFFF, 0.92f * v), 0.9f, false);
    }

    private void drawPanel(GuiGraphics g, float v) {
        g.pose().pushPose();
        box(g, 0, 0, 94, 88, v);
        box(g, 100, 0, 188, 88, v);
        box(g, 0, 94, 188, 162, v);
        box(g, 0, 168, 188, 196, v);

        g.pose().translate(0f, 0f, 1f);
        int selected = player.getInventory().selected;
        for (Slot s : menu.slots) {
            card(g, s.x - 1, s.y - 1, v, s.index == 0, s.index == 36 + selected, s == hoveredSlot);
        }

        g.pose().translate(0f, 0f, 1f);
        float hp = HudState.hp.shown >= 0 ? HudState.hp.shown : player.getHealth();
        int water = HudState.water.shown >= 0 ? HudState.water.shown : 20;
        stat(g, "lvl", 5f, 5f, "LVL " + player.experienceLevel, v);
        g.fill(6, 17, 64, 19, Gfx.rgba(0x000000, 0.6f * v));
        g.fill(6, 17, 6 + (int) (58 * player.experienceProgress), 19, Gfx.rgba(ACCENT, 0.9f * v));
        stat(g, "hp", 5f, 23f, (int) Math.ceil(hp) + "/" + (int) player.getMaxHealth(), v);
        stat(g, "eat", 5f, 35f, String.valueOf(player.getFoodData().getFoodLevel()), v);
        stat(g, "water", 5f, 47f, String.valueOf(water), v);
        Gfx.text(g, "->", 149, 40, Gfx.rgba(ACCENT, 0.85f * v), 1.1f, false);
        g.pose().popPose();
    }
}
