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
    private static final float PANEL_START = 0.25f;
    private static final int CARD = 20;
    private static final int PITCH = 23;
    private static final float TILT = (float) Math.toRadians(15.0);
    private static final int W = 216;
    private static final int H = 223;
    private static final float CX = W / 2f;
    private static final float CY = H / 2f;
    private static final float DEPTH_SCALE = 0.05f;
    private static final float LAYER = 0.5f;

    private final Player player;
    private final int[] origX;
    private final int[] origY;
    private int rawX;
    private int rawY;
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
        s.x = cardX + 2;
        s.y = cardY + 2;
    }

    private void arrange() {
        put(0, 187, 40);
        put(1, 116, 29);
        put(2, 116 + PITCH, 29);
        put(3, 116, 29 + PITCH);
        put(4, 116 + PITCH, 29 + PITCH);
        for (int i = 0; i < 4; i++) put(5 + i, 76, 6 + i * PITCH);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) put(9 + r * 9 + c, 6 + c * PITCH, 113 + r * PITCH);
        }
        for (int c = 0; c < 9; c++) put(36 + c, 6 + c * PITCH, 197);
        if (menu.slots.size() > 45) put(45, 6, 75);
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
        SoulView.open();
    }

    private float visibility() {
        return Gfx.smooth(Gfx.clamp((SoulView.raw() - PANEL_START) / (1f - PANEL_START)));
    }

    @Override
    public void onClose() {
        if (closing) return;
        closing = true;
        SoulView.close();
    }

    private void layout(float v) {
        scale = Math.min(height * 0.8f / H, width * 0.44f / W);
        pivotX = width * 0.66f + (1f - v) * width * 0.65f;
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
        float v = visibility();
        if (closing && SoulView.raw() <= PANEL_START) {
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
        g.fill(x0, y0, x1, y1, Gfx.rgba(0x000000, 0.32f * v));
    }

    private void layer(GuiGraphics g, int x0, int y0, int x1, int y1, int argb) {
        g.pose().translate(0f, 0f, LAYER);
        g.fill(x0, y0, x1, y1, argb);
    }

    private void card(GuiGraphics g, int x, int y, float v, boolean result, boolean active, boolean hover) {
        g.pose().pushPose();
        layer(g, x - 1, y - 1, x + CARD + 1, y + CARD + 1, Gfx.rgba(0x000000, 0.9f * v));
        layer(g, x, y, x + CARD, y + CARD, Gfx.rgba(0x12161F, 0.6f * v));
        layer(g, x, y, x + CARD, y + 1, Gfx.rgba(0xFFFFFF, 0.28f * v));
        layer(g, x, y, x + 1, y + CARD, Gfx.rgba(0xFFFFFF, 0.28f * v));
        layer(g, x, y + CARD - 1, x + CARD, y + CARD, Gfx.rgba(0x000000, 0.35f * v));
        layer(g, x + CARD - 1, y, x + CARD, y + CARD, Gfx.rgba(0x000000, 0.35f * v));
        if (result) layer(g, x + 1, y + 1, x + CARD - 1, y + CARD - 1, Gfx.rgba(ACCENT, 0.12f * v));
        if (active) layer(g, x + 1, y + 1, x + CARD - 1, y + CARD - 1, Gfx.rgba(0xFFFFFF, 0.14f * v));
        if (hover) layer(g, x + 1, y + 1, x + CARD - 1, y + CARD - 1, Gfx.rgba(0xFFFFFF, 0.1f * v));
        g.pose().popPose();
    }

    private void stat(GuiGraphics g, String icon, float x, float y, String text, float v) {
        Gfx.icon(g, icon, x + 5, y + 5, 11, 0f, 1f, v);
        Gfx.text(g, text, x + 13, y + 1, Gfx.rgba(0xFFFFFF, 0.92f * v), 0.9f, false);
    }

    private void drawPanel(GuiGraphics g, float v) {
        g.pose().pushPose();
        box(g, 0, 0, 102, 101, v);
        box(g, 108, 0, W, 101, v);
        box(g, 0, 107, W, 185, v);
        box(g, 0, 191, W, H, v);

        g.pose().translate(0f, 0f, 2f);
        int selected = player.getInventory().selected;
        for (Slot s : menu.slots) {
            boolean active = s.index == 36 + selected;
            int x = s.x - 2;
            int y = s.y - 2;
            card(g, x, y, v, s.index == 0, active, s == hoveredSlot);
            g.pose().pushPose();
            g.pose().translate(0f, 0f, 6f);
            if (s.index >= 36 && s.index <= 44) {
                Gfx.text(g, String.valueOf(s.index - 35), x + 2, y + 2, Gfx.rgba(0xFFFFFF, 0.55f * v), 0.5f, true);
            }
            if (active) {
                int dx = x + CARD / 2 - 1;
                int dy = y + CARD + 2;
                layer(g, dx - 1, dy - 1, dx + 4, dy + 4, Gfx.rgba(0x000000, v));
                layer(g, dx, dy, dx + 3, dy + 3, Gfx.rgba(0xFFFFFF, v));
            }
            g.pose().popPose();
        }

        g.pose().translate(0f, 0f, 8f);
        float hp = HudState.hp.shown >= 0 ? HudState.hp.shown : player.getHealth();
        int water = HudState.water.shown >= 0 ? HudState.water.shown : 20;
        stat(g, "lvl", 6f, 6f, "LVL " + player.experienceLevel, v);
        layer(g, 7, 19, 68, 21, Gfx.rgba(0x000000, 0.6f * v));
        layer(g, 7, 19, 7 + (int) (61 * player.experienceProgress), 21, Gfx.rgba(ACCENT, 0.9f * v));
        stat(g, "hp", 6f, 25f, (int) Math.ceil(hp) + "/" + (int) player.getMaxHealth(), v);
        stat(g, "eat", 6f, 38f, String.valueOf(player.getFoodData().getFoodLevel()), v);
        stat(g, "water", 6f, 51f, String.valueOf(water), v);
        Gfx.text(g, "->", 167, 45, Gfx.rgba(ACCENT, 0.85f * v), 1.1f, false);
        g.pose().popPose();
    }
}
