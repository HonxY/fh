package com.jelly.FarmHelper.utils;

import com.jelly.FarmHelper.FarmHelper;
import com.jelly.FarmHelper.config.AngleEnum;
import com.jelly.FarmHelper.config.Config;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.*;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Utils {
    private static String lastDebug;
    private static String lastWebhook;
    private static long logMsgTime = 1000;
    private static long statusMsgTime = -1;

    public static void drawString(String text, int x, int y, float size, int color) {
        GlStateManager.scale(size, size, size);
        float mSize = (float) Math.pow(size, -1);
        Minecraft.getMinecraft().fontRendererObj.drawString(text, Math.round(x / size), Math.round(y / size), color);
        GlStateManager.scale(mSize, mSize, mSize);
    }

    public static void drawStringWithShadow(String text, int x, int y, float size, int color) {
        GlStateManager.scale(size, size, size);
        float mSize = (float) Math.pow(size, -1);
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, Math.round(x / size), Math.round(y / size), color);
        GlStateManager.scale(mSize, mSize, mSize);
    }

    public static String formatNumber(int number) {
        String s = Integer.toString(number);
        return String.format("%,d", number);
    }

    public static String formatNumber(float number) {
        String s = Integer.toString(Math.round(number));
        return String.format("%,d", Math.round(number));
    }

    public static void hardRotate(float yaw) {
        Minecraft mc = Minecraft.getMinecraft();
        if (Math.abs(mc.thePlayer.rotationYaw - yaw) < 0.2f) {
            mc.thePlayer.rotationYaw = yaw;
            return;
        }
        while (mc.thePlayer.rotationYaw > yaw) {
            mc.thePlayer.rotationYaw -= 0.1f;
        }
        while (mc.thePlayer.rotationYaw < yaw) {
            mc.thePlayer.rotationYaw += 0.1f;

        }
    }

    public static boolean hasSellItemInInventory() {
        for (Slot slot : Minecraft.getMinecraft().thePlayer.inventoryContainer.inventorySlots) {
            if (slot != null) {
                try {
                    switch (Config.CropType) {
                        case WHEAT:
                            if (slot.getStack().getDisplayName().contains("Tightly-Tied Hay Bale"))
                                return true;
                        case CARROT:
                            if (slot.getStack().getDisplayName().contains("Enchanted Carrot"))
                                return true;
                        case POTATO:
                            if (slot.getStack().getDisplayName().contains("Enchanted Baked Potato"))
                                return true;
                        case NETHERWART:
                            if (slot.getStack().getDisplayName().contains("Mutant Nether Wart"))
                                return true;
                    }
                } catch (Exception e) {

                }
            }
        }
        return false;
    }

    public static int getFirstSlotWithSellItem() {
        for (Slot slot : Minecraft.getMinecraft().thePlayer.inventoryContainer.inventorySlots) {
            if (slot != null) {
                if (slot.getStack() != null) {
                    try {
                        switch (Config.CropType) {
                            case WHEAT:
                                if (slot.getStack().getDisplayName().contains("Tightly-Tied Hay Bale"))
                                    return slot.slotNumber;
                            case CARROT:
                                if (slot.getStack().getDisplayName().contains("Enchanted Carrot"))
                                    return slot.slotNumber;
                            case POTATO:
                                if (slot.getStack().getDisplayName().contains("Enchanted Baked Potato"))
                                    return slot.slotNumber;
                            case NETHERWART:
                                if (slot.getStack().getDisplayName().contains("Mutant Nether Wart"))
                                    return slot.slotNumber;
                        }
                    } catch (Exception e) {

                    }
                }
            }
        }
        return 0;
    }
    
    public static void drawHorizontalLine(int startX, int endX, int y, int color) {
        if (endX < startX) {
            int i = startX;
            startX = endX;
            endX = i;
        }
        Gui.drawRect(startX, y, endX + 1, y + 1, color);
    }

    public static void drawVerticalLine(int x, int startY, int endY, int color) {
        if (endY < startY) {
            int i = startY;
            startY = endY;
            endY = i;
        }
        Gui.drawRect(x, startY + 1, x + 1, endY, color);
    }

    public static float get360RotationYaw() {
        return Minecraft.getMinecraft().thePlayer.rotationYaw > 0 ?
            (Minecraft.getMinecraft().thePlayer.rotationYaw % 360) :
            (Minecraft.getMinecraft().thePlayer.rotationYaw < 360f ? 360 - (-Minecraft.getMinecraft().thePlayer.rotationYaw % 360) : 360 + Minecraft.getMinecraft().thePlayer.rotationYaw);
    }

    public static float get360RotationYaw(float yaw) {
        return yaw > 0 ?
            (yaw % 360) :
            (yaw < 360f ? 360 - (-yaw % 360) : 360 + yaw);
    }

    static int getOppositeAngle(int angle) {
        return (angle < 180) ? angle + 180 : angle - 180;
    }

    static boolean shouldRotateClockwise(int targetYaw360, int initialYaw360) {
        return targetYaw360 - initialYaw360 > 0 ?
            targetYaw360 - initialYaw360 <= 180 : targetYaw360 - initialYaw360 < -180;
    }

    public static void smoothRotateTo(final int rotation360) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (get360RotationYaw() != rotation360) {
                    if (Math.abs(get360RotationYaw() - rotation360) < 1f) {
                        Minecraft.getMinecraft().thePlayer.rotationYaw = Math.round(Minecraft.getMinecraft().thePlayer.rotationYaw + Math.abs(get360RotationYaw() - rotation360));
                        return;
                    }
                    Minecraft.getMinecraft().thePlayer.rotationYaw += 0.3f + nextInt(3) / 10.0f;
                    try {
                        Thread.sleep(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public static void smoothRotateClockwise(final int rotationClockwise360) {
        new Thread(() -> {
            int targetYaw = (Math.round(get360RotationYaw()) + rotationClockwise360) % 360;
            while (get360RotationYaw() != targetYaw) {
                if (Math.abs(get360RotationYaw() - targetYaw) < 1f) {
                    Minecraft.getMinecraft().thePlayer.rotationYaw = Math.round(Minecraft.getMinecraft().thePlayer.rotationYaw + Math.abs(get360RotationYaw() - targetYaw));
                    return;
                }
                Minecraft.getMinecraft().thePlayer.rotationYaw += 0.3f + nextInt(3) / 10.0f;
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }

    public static void smoothRotateClockwise(final int rotationClockwise360, double speed) {
        new Thread(() -> {
            int targetYaw = (Math.round(get360RotationYaw()) + rotationClockwise360) % 360;
            while (get360RotationYaw() != targetYaw) {
                if (Math.abs(get360RotationYaw() - targetYaw) < 1f * speed) {
                    Minecraft.getMinecraft().thePlayer.rotationYaw = Math.round(Minecraft.getMinecraft().thePlayer.rotationYaw + Math.abs(get360RotationYaw() - targetYaw));
                    return;
                }
                Minecraft.getMinecraft().thePlayer.rotationYaw += (0.3f + nextInt(3) / 10.0f) * speed;
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }

    public static void sineRotateCW(final int rotationClockwise360, double speed) {
        new Thread(() -> {
            int targetYaw = (Math.round(get360RotationYaw()) + rotationClockwise360) % 360;
            while (get360RotationYaw() != targetYaw) {
                float difference = Math.abs(get360RotationYaw() - targetYaw);
                if (difference < 0.4f * speed) {
                    Minecraft.getMinecraft().thePlayer.rotationYaw = Math.round(Minecraft.getMinecraft().thePlayer.rotationYaw + difference);
                    return;
                }
                Minecraft.getMinecraft().thePlayer.rotationYaw += speed * 0.3 * ((difference / rotationClockwise360) + (Math.PI / 2));
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }

    public static void smoothRotateAnticlockwise(final int rotationAnticlockwise360) {
        new Thread(() -> {
            int targetYaw = Math.round(get360RotationYaw(get360RotationYaw() - rotationAnticlockwise360));
            while (get360RotationYaw() != targetYaw) {
                if (Math.abs(get360RotationYaw() - targetYaw) < 1f) {
                    Minecraft.getMinecraft().thePlayer.rotationYaw = Math.round(Minecraft.getMinecraft().thePlayer.rotationYaw - Math.abs(get360RotationYaw() - targetYaw));
                    return;
                }
                Minecraft.getMinecraft().thePlayer.rotationYaw -= 0.3f + nextInt(3) / 10.0f;
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void smoothRotateAnticlockwise(final int rotationAnticlockwise360, double speed) {
        new Thread(() -> {
            int targetYaw = Math.round(get360RotationYaw(get360RotationYaw() - rotationAnticlockwise360));
            while (get360RotationYaw() != targetYaw) {
                if (Math.abs(get360RotationYaw() - targetYaw) < 1f * speed) {
                    Minecraft.getMinecraft().thePlayer.rotationYaw = Math.round(Minecraft.getMinecraft().thePlayer.rotationYaw - Math.abs(get360RotationYaw() - targetYaw));
                    return;
                }
                Minecraft.getMinecraft().thePlayer.rotationYaw -= (0.3f + nextInt(3) / 10.0f) * speed;
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void sineRotateAWC(final int rotationAnticlockwise360, double speed) {
        new Thread(() -> {
            int targetYaw = Math.round(get360RotationYaw(get360RotationYaw() - rotationAnticlockwise360));
            while (get360RotationYaw() != targetYaw) {
                float difference = Math.abs(get360RotationYaw() - targetYaw);
                if (difference < 0.4f * speed) {
                    Minecraft.getMinecraft().thePlayer.rotationYaw = Math.round(Minecraft.getMinecraft().thePlayer.rotationYaw - difference);
                    return;
                }
                Minecraft.getMinecraft().thePlayer.rotationYaw -= 0.3f + nextInt(3) / 10.0f;
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static float getActualRotationYaw() { //f3
        return Minecraft.getMinecraft().thePlayer.rotationYaw > 0 ?
            (Minecraft.getMinecraft().thePlayer.rotationYaw % 360 > 180 ? -(180 - (Minecraft.getMinecraft().thePlayer.rotationYaw % 360 - 180)) : Minecraft.getMinecraft().thePlayer.rotationYaw % 360) :
            (-Minecraft.getMinecraft().thePlayer.rotationYaw % 360 > 180 ? (180 - (-Minecraft.getMinecraft().thePlayer.rotationYaw % 360 - 180)) : -(-Minecraft.getMinecraft().thePlayer.rotationYaw % 360));
    }

    public static float getActualRotationYaw(int yaw) { //f3
        return yaw > 0 ?
            (yaw % 360 > 180 ? -(180 - (yaw % 360 - 180)) : yaw % 360) :
            (-yaw % 360 > 180 ? (180 - (-yaw % 360 - 180)) : -(-yaw % 360));
    }

    public static int nextInt(int upperbound) {
        Random r = new Random();
        return r.nextInt(upperbound);
    }

    public static int getUnitX() {
        double modYaw = (Minecraft.getMinecraft().thePlayer.rotationYaw % 360 + 360) % 360;
        if (modYaw < 45 || modYaw > 315) {
            return 0;
        } else if (modYaw < 135) {
            return -1;
        } else if (modYaw < 225) {
            return 0;
        } else {
            return 1;
        }
    }

    public static int getUnitZ() {
        double modYaw = (Minecraft.getMinecraft().thePlayer.rotationYaw % 360 + 360) % 360;
        if (modYaw < 45 || modYaw > 315) {
            return 1;
        } else if (modYaw < 135) {
            return 0;
        } else if (modYaw < 225) {
            return -1;
        } else {
            return 0;
        }
    }

    // Base
    public static Block getBelowBlock() {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX,
                mc.thePlayer.posY - 1,
                mc.thePlayer.posZ
            )).getBlock());
    }

    public static Block getFrontBlock() {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + getUnitX(),
                mc.thePlayer.posY,
                mc.thePlayer.posZ + getUnitZ()
            )).getBlock());
    }

    public static Block getBackBlock() {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + getUnitX() * -1,
                mc.thePlayer.posY,
                mc.thePlayer.posZ + getUnitZ() * -1
            )).getBlock());
    }

    public static Block getRightBlock() {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + getUnitZ() * -1,
                mc.thePlayer.posY,
                mc.thePlayer.posZ + getUnitX()
            )).getBlock());
    }

    public static Block getLeftBlock() {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + getUnitZ(),
                mc.thePlayer.posY,
                mc.thePlayer.posZ + getUnitX() * -1
            )).getBlock());
    }

    // yOffset param
    public static Block getFrontBlock(double yOffset) {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + getUnitX(),
                mc.thePlayer.posY + yOffset,
                mc.thePlayer.posZ + getUnitZ()
            )).getBlock());
    }

    public static Block getBackBlock(double yOffset) {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + (getUnitX() * -1),
                mc.thePlayer.posY + yOffset,
                mc.thePlayer.posZ + (getUnitZ() * -1)
            )).getBlock());
    }

    public static Block getRightBlock(double yOffset) {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + (getUnitZ() * -1),
                mc.thePlayer.posY + yOffset,
                mc.thePlayer.posZ + getUnitX()
            )).getBlock());
    }

    public static Block getLeftBlock(double yOffset) {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + getUnitZ(),
                mc.thePlayer.posY + yOffset,
                mc.thePlayer.posZ + (getUnitX() * -1)
            )).getBlock());
    }

    // Multiple extension
    public static Block getFrontBlock(double yOffset, int multiple) {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + (getUnitX() * multiple),
                mc.thePlayer.posY + yOffset,
                mc.thePlayer.posZ + (getUnitZ() * multiple)
            )).getBlock());
    }

    public static Block getRightBlock(double yOffset, int multiple) {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + (getUnitZ() * -1 * multiple),
                mc.thePlayer.posY + yOffset,
                mc.thePlayer.posZ + (getUnitX() * multiple)
            )).getBlock());
    }

    public static Block getLeftBlock(double yOffset, int multiple) {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + (getUnitZ() * multiple),
                mc.thePlayer.posY + yOffset,
                mc.thePlayer.posZ + (getUnitX() * -1 * multiple)
            )).getBlock());
    }

    // Get block state of col block from right of row
    public static Block getRightColBlock(int multiple) {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + (getUnitZ() * -1 * multiple) + getUnitX(),
                mc.thePlayer.posY,
                mc.thePlayer.posZ + (getUnitX() * multiple) + getUnitZ()
            )).getBlock());
    }

    public static Block getLeftColBlock(int multiple) {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.theWorld.getBlockState(
            new BlockPos(
                mc.thePlayer.posX + (getUnitZ() * multiple) + getUnitX(),
                mc.thePlayer.posY,
                mc.thePlayer.posZ + (getUnitX() * -1 * multiple) + getUnitZ()
            )).getBlock());
    }

    public static void drawInfo(String title, String value, int y) {
        Utils.drawStringWithShadow(
            EnumChatFormatting.GRAY + "" + EnumChatFormatting.BOLD + title + EnumChatFormatting.DARK_GRAY + " » " + EnumChatFormatting.RED + value, 5, y, 1.1f, -1);
    }

    public static void sendLog(ChatComponentText chat) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(chat);
    }

    public static void scriptLog(String message) {
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.RED + EnumChatFormatting.BOLD + message
        ));
    }

    public static void scriptLog(String message, EnumChatFormatting color) {
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + color + EnumChatFormatting.BOLD + message
        ));
    }

    public static String getCropLog() {
        switch (Config.CropType) {
            case WHEAT:
                return EnumChatFormatting.GOLD + "WHEAT";
            case CARROT:
                return EnumChatFormatting.DARK_GREEN + "CARROT";
            case NETHERWART:
                return EnumChatFormatting.DARK_RED + "NETHERWART";
            case POTATO:
                return EnumChatFormatting.YELLOW + "POTATO";
            default:
                return "UNKNOWN";
        }
    }

    public static String smallURL() {
        if (Config.webhookUrl.length() > 10) {
            return Config.webhookUrl.substring(Config.webhookUrl.length() - 10);
        } else {
            return Config.webhookUrl;
        }
    }

    public static void configLog() {
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "Configuration"
        ));
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Crop - " + getCropLog()
        ));
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Resync - " + (Config.resync ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF")
        ));
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Jacob Failsafe - " + (Config.jacobFailsafe ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF")
        ));
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Jacob Score Threshold - " + EnumChatFormatting.DARK_AQUA + Config.jacobThreshold
        ));
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Webhook Logging - " + (Config.webhookLog ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF")
        ));
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Webhook URL - " + EnumChatFormatting.BLUE + smallURL()
        ));
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Status Cooldown - " + EnumChatFormatting.GOLD + Config.statusTime + "min"
        ));
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Debug Mode - " + (Config.debug ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF")
        ));
    }

//    public static void configLog() {
//        sendLog(new ChatComponentText(
//            EnumChatFormatting.BLUE + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.DARK_AQUA + "" + EnumChatFormatting.BOLD + "Configuration"
//        ));
//        sendLog(new ChatComponentText(
//            EnumChatFormatting.BLUE + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Resync - " + (Config.resync ? EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "ON" : EnumChatFormatting.RED + "" +  EnumChatFormatting.BOLD + "OFF")
//        ));
//        sendLog(new ChatComponentText(
//            EnumChatFormatting.BLUE + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Debug Mode - " + (Config.debug ? EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "ON" : EnumChatFormatting.RED + "" +  EnumChatFormatting.BOLD + "OFF")
//        ));
//        sendLog(new ChatComponentText(
//            EnumChatFormatting.BLUE + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Compact Debug - " + (Config.compactDebug ? EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "ON" : EnumChatFormatting.RED + "" +  EnumChatFormatting.BOLD + "OFF")
//        ));
//    }

    public static String getRuntimeFormat() {
        if (FarmHelper.startTime == 0) {
            return "0h 0m 0s";
        }
        long millis = System.currentTimeMillis() - FarmHelper.startTime;
        return String.format("%dh %dm %ds",
            TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
            TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    public static void webhookStatus() {
        if (statusMsgTime == -1) {
            statusMsgTime = System.currentTimeMillis();
        }
        long timeDiff = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - statusMsgTime);
        if (timeDiff > Config.statusTime) {
            FarmHelper.webhook.addEmbed(new DiscordWebhook.EmbedObject()
                .setTitle("Farm Helper")
                .setDescription("```" + "I'm still alive!" + "```")
                .setColor(Color.decode("#ff3b3b"))
                .setFooter("Jelly", "")
                .setThumbnail("https://crafatar.com/renders/head/" + Minecraft.getMinecraft().thePlayer.getUniqueID())
                .addField("Username", Minecraft.getMinecraft().thePlayer.getName(), true)
                .addField("Runtime", getRuntimeFormat(), true)
                //.addField("", "", false)
                .addField("Total Profit", FarmHelper.getProfit() != 0 ? "$" + formatNumber(FarmHelper.getProfit()) : "Not supported", false)
                .addField("Profit / hr", "$" + formatNumber((float) FarmHelper.getHourProfit(FarmHelper.getProfit())), false)
                //.addField("", "", false)
                .addField(FarmHelper.getHighTierName(), formatNumber(FarmHelper.getHighTierCount()), true)
                .addField("Counter", formatNumber(FarmHelper.currentCounter), true)
            );
            new Thread(() -> {
                try {
                    FarmHelper.webhook.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            statusMsgTime = System.currentTimeMillis();
        }
    }

    public static void webhookLog(String message) {
        long timeDiff = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - logMsgTime);
        Utils.debugFullLog("Last webhook message: " + timeDiff);
        if (Config.webhookLog && (timeDiff > 20 || lastWebhook != message)) {
            FarmHelper.webhook.addEmbed(new DiscordWebhook.EmbedObject()
                .setDescription("**Farm Helper Log** ```" + message + "```")
                .setColor(Color.decode("#741010"))
                .setFooter(Minecraft.getMinecraft().thePlayer.getName(), "")
            );
            new Thread(() -> {
                try {
                    FarmHelper.webhook.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            logMsgTime = System.currentTimeMillis();
        }
        lastWebhook = message;
    }

    public static void debugLog(String message) {
        if (Config.debug || lastDebug != message) {
            sendLog(new ChatComponentText(
                EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Log " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + message
            ));
        }
        lastDebug = message;
    }

    public static void debugFullLog(String message) {
        if (Config.debug) {
            debugLog(message);
        }
    }

    public static int angleToValue(AngleEnum c) {
        return !c.toString().replace("A", "").contains("N") ?
            Integer.parseInt(c.toString().replace("A", "")) :
            Integer.parseInt(c.toString().replace("A", "").replace("N", "")) * -1;
    }

    public static String getScoreboardDisplayName(int line) {
        try {
            Utils.debugFullLog(Minecraft.getMinecraft().theWorld.getScoreboard().getObjectiveInDisplaySlot(line).getDisplayName());
            return Minecraft.getMinecraft().theWorld.getScoreboard().getObjectiveInDisplaySlot(line).getDisplayName();
        } catch (Exception e) {
            Utils.debugLog("Error in getting scoreboard " + e);
            return "";
        }
    }

    public static List<String> getSidebarLines() {
        List<String> lines = new ArrayList<>();
        if (Minecraft.getMinecraft().theWorld == null) return lines;
        Scoreboard scoreboard = Minecraft.getMinecraft().theWorld.getScoreboard();
        if (scoreboard == null) return lines;

        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return lines;

        Collection<Score> scores = scoreboard.getSortedScores(objective);
        List<Score> list = scores.stream()
            .filter(input -> input != null && input.getPlayerName() != null && !input.getPlayerName()
                .startsWith("#"))
            .collect(Collectors.toList());

        if (list.size() > 15) {
            scores = Lists.newArrayList(Iterables.skip(list, scores.size() - 15));
        } else {
            scores = list;
        }

        for (Score score : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            lines.add(ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()));
        }

        return lines;
    }

    public static String cleanSB(String scoreboard) {
        char[] nvString = StringUtils.stripControlCodes(scoreboard).toCharArray();
        StringBuilder cleaned = new StringBuilder();

        for (char c : nvString) {
            if ((int) c > 20 && (int) c < 127) {
                cleaned.append(c);
            }
        }

        return cleaned.toString();
    }

    public static void ScheduleRunnable(Runnable r, int delay, TimeUnit tu) {
        ScheduledExecutorService eTemp = Executors.newScheduledThreadPool(1);
        eTemp.schedule(r, delay, tu);
        eTemp.shutdown();
    }

    public static void ExecuteRunnable(Runnable r) {
        ScheduledExecutorService eTemp = Executors.newScheduledThreadPool(1);
        eTemp.execute(r);
        eTemp.shutdown();
    }

    public static Block getBlockAround(int rightOffset, int frontOffset, int upOffset) {
        Minecraft mc = Minecraft.getMinecraft();
        double X = mc.thePlayer.posX;
        double Y = mc.thePlayer.posY;
        double Z = mc.thePlayer.posZ;

        return (mc.theWorld.getBlockState(
            new BlockPos(mc.thePlayer.getLookVec().zCoord * -1 * rightOffset + mc.thePlayer.getLookVec().xCoord * frontOffset + X, Y + upOffset,
                mc.thePlayer.getLookVec().xCoord * rightOffset + mc.thePlayer.getLookVec().zCoord * frontOffset + Z)).getBlock());

    }
}