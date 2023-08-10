package com.jelly.farmhelper.hud;

import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.features.BanwaveChecker;
import com.jelly.farmhelper.macros.VerticalCropMacroNew;
import com.jelly.farmhelper.utils.StatusUtils;

import java.awt.*;
import java.util.List;

public class DebugHUD extends TextHud {
    public DebugHUD() {
        super(true, 1f, 10f, 1, true, true, 1, 5, 5, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 127));
    }

    @Switch(
        name = "Rainbow Text", category = "HUD",
        description = "Rainbow text for the debug HUD"
    )
    public static boolean rainbowStatusText = false;
    public static String currentState = "";
    public static String newState = "";
    public static boolean rotating = false;

    @Override
    protected void getLines(List<String> lines, boolean example) {

        if (rainbowStatusText) {
            Color chroma = Color.getHSBColor((float) ((System.currentTimeMillis() / 10) % 500) / 500, 1, 1);
            color.setFromOneColor(new OneColor(chroma.getRed(), chroma.getGreen(), chroma.getBlue(), 255));
        } else {
            Color chroma = Color.WHITE;
            color.setFromOneColor(new OneColor(chroma.getRed(), chroma.getGreen(), chroma.getBlue(), 255));

        }
        if (FarmHelper.config.debugMode) {
            lines.add("currentState: " + currentState);
            lines.add("newState: " + newState);
            lines.add("rotating: " + rotating);
        }
    }
}