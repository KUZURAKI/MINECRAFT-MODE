package dev.zedkodex.modehud.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ModKeys {
    private ModKeys() {}

    public static final String CATEGORY = "key.categories.modehud";

    public static final KeyMapping OPEN_BACKPACK = new KeyMapping("key.modehud.open_backpack", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping TAKE_OFF_BACKPACK = new KeyMapping("key.modehud.take_off_backpack", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);
}
