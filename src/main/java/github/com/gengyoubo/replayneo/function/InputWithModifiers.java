package de.johni0702.minecraft.gui.function;

import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public interface InputWithModifiers {
    int modifiers();

    default boolean hasCtrl() {
        return (modifiers() & _CTRL_MOD) != 0;
    }
    default boolean hasShift() {
        return (modifiers() & _SHIFT_MOD) != 0;
    }
    default boolean hasAlt() {
        return (modifiers() & _ALT_MOD) != 0;
    }

    int _CTRL_MOD = Util.getPlatform() == Util.OS.OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;
    int _SHIFT_MOD = GLFW.GLFW_MOD_SHIFT;
    int _ALT_MOD = GLFW.GLFW_MOD_ALT;

    static int currentModifiers() {
        int ctrl = Screen.hasControlDown() ? _CTRL_MOD : 0;
        int shift = Screen.hasShiftDown() ? _SHIFT_MOD : 0;
        int alt = Screen.hasAltDown() ? _ALT_MOD : 0;
        return ctrl | shift | alt;
    }
}
