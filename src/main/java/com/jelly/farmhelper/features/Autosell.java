package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.KeyBindUtils;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.utils.PlayerUtils;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Autosell {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static boolean enabled;
    private static int hoeSlot;
    private static int sackSlot;
    private static State currentState;

    private static final Integer[] NPCSellSlots = {11, 16, 21, 23};
    private static Integer[] NPCSellSlotCounts = {999, 999, 999, 999};

    private static boolean fullCheck;
    private static final Clock checkTimer = new Clock();
    private static int fullCount;
    private static int totalCount;

    private static final Clock sellClock = new Clock();
    private static final Clock waitAfterDisable = new Clock();

    private static boolean soldToBZ = false;
    private static boolean soldSacks = false;

    enum State {
        SELL_INVENTORY,
        SACKS
    }

    public static final Clock waitBeforeSellClock = new Clock();
    public static final Clock stuckClock = new Clock();

    public static boolean disableOnlyFlag = false;

    public static boolean isEnabled() {
        return (waitAfterDisable.isScheduled() && !waitAfterDisable.passed()) || enabled;
    }

    public static void enable() {
        enable(false);
    }

    public static void enable(boolean disableOnlyFlag) {
        if (FarmHelper.gameState.cookie == GameState.EffectState.OFF) {
            LogUtils.sendDebug("[AutoSell] You need a cookie for auto sell!");
            disableAndRestart();
            return;
        }
        LogUtils.sendDebug("[AutoSell] Started inventory sell");
        NPCSellSlotCounts = new Integer[]{999, 999, 999, 999};
        hoeSlot = mc.thePlayer.inventory.currentItem;
        sackSlot = getSack();
        sellClock.reset();
        currentState = State.SELL_INVENTORY;
        enabled = true;
        soldToBZ = false;
        soldSacks = false;
        waitBeforeSellClock.schedule((long) (1_500 + Math.random() * 1_000));
        stuckClock.schedule(10_000);
        Autosell.disableOnlyFlag = disableOnlyFlag;
        if (!disableOnlyFlag)
            MacroHandler.disableCurrentMacro();
    }

    public static void disableAndRestart() {
        LogUtils.sendDebug("[AutoSell] Finished");
        mc.thePlayer.closeScreen();
        mc.thePlayer.inventory.currentItem = hoeSlot;
        disableOnlyFlag = false;
        MacroHandler.enableCurrentMacro();
        waitAfterDisable.schedule(2_500);
        enabled = false;
    }

    public static void disableOnly() {
        if (!enabled) return;
        LogUtils.sendDebug("[AutoSell] Disabled");
        mc.thePlayer.closeScreen();
        mc.thePlayer.inventory.currentItem = hoeSlot;
        waitAfterDisable.schedule(2_500);
        enabled = false;
    }

    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || FarmHelper.tickCount % 5 != 0)
            return;

        if (!enabled && FarmHelper.config.enableAutoSell && MacroHandler.isMacroing && MacroHandler.currentMacro.enabled) {
            if (mc.thePlayer.inventory.getFirstEmptyStack() == -1 && !fullCheck) {
                LogUtils.sendDebug("[AutoSell] Started inventory full watch");
                fullCheck = true;
                checkTimer.schedule(TimeUnit.SECONDS.toMillis(FarmHelper.config.inventoryFullTime));
                fullCount = 1;
                totalCount = 1;
            } else if (fullCheck && !checkTimer.passed()) {
                if (mc.thePlayer.inventory.getFirstEmptyStack() == -1) {
                    fullCount++;
                }
                totalCount++;
            } else if (fullCheck && checkTimer.passed()) {
                fullCheck = false;
                if (((float) fullCount / totalCount) >= (FarmHelper.config.inventoryFullRatio / 100.0)) {
                    MacroHandler.disableCurrentMacro();
                    enable();
                }
            }
        }

        if (!enabled) return;

        if (stuckClock.isScheduled() && stuckClock.passed()) {
            LogUtils.sendDebug("[AutoSell] Stuck in sell menu, restarting...");
            disableOnly();
            mc.thePlayer.closeScreen();
            enable(disableOnlyFlag);
            return;
        }

        if (waitBeforeSellClock.isScheduled() && !waitBeforeSellClock.passed())
            return;

        switch (currentState) {
            case SELL_INVENTORY:
                if(sellClock.isScheduled() && !sellClock.passed())
                    return;

                if (mc.currentScreen == null) {
                    if (FarmHelper.config.autoSellMarketType) {
                        LogUtils.sendDebug("[AutoSell] Opening Trades menu");
                        mc.thePlayer.sendChatMessage("/trades");
                    } else {
                        LogUtils.sendDebug("[AutoSell] Opening BZ menu");
                        mc.thePlayer.sendChatMessage("/bz");
                    }
                    sellClock.schedule(250);
                } else if (PlayerUtils.getInventoryName() != null && FarmHelper.config.autoSellMarketType && PlayerUtils.getInventoryName().contains("Trades")) {
                    LogUtils.sendDebug("[AutoSell] Detected trade menu, selling item");
                    List<Slot> sellList = PlayerUtils.getInventorySlots();
                    sellList.removeIf(item -> !shouldSell(item.getStack()));
                    if (sellList.size() > 0) {
                        PlayerUtils.clickOpenContainerSlot(45 + sellList.get(0).slotNumber);
                        sellClock.schedule(250);
                    } else {
                        LogUtils.sendDebug("[AutoSell] Out of items to sell!");
                        if (sackContains() && sackSlot != -1) {
                            LogUtils.sendDebug("[AutoSell] Switching to sacks");
                            currentState = State.SACKS;
                        } else {
                            LogUtils.sendDebug("[AutoSell] Sacks are empty! Exiting");
                            if (disableOnlyFlag) {
                                disableOnly();
                                disableOnlyFlag = false;
                            } else {
                                disableAndRestart();
                            }
                        }
                    }
                } else if (PlayerUtils.getInventoryName() != null && !soldToBZ && !FarmHelper.config.autoSellMarketType && PlayerUtils.getInventoryName().contains("Bazaar")) {
                    if (mc.thePlayer.openContainer.getSlot(47) != null &&
                            mc.thePlayer.openContainer.getSlot(47).getStack() != null &&
                            mc.thePlayer.openContainer.getSlot(47).getStack().getDisplayName().contains("Sell Inventory Now")) {
                        if (PlayerUtils.getLore(mc.thePlayer.openContainer.getSlot(47).getStack()) != null && PlayerUtils.getLore(mc.thePlayer.openContainer.getSlot(47).getStack()).get(4).toString().contains("You don't have anything")) {
                            soldToBZ = true;
                            sellClock.schedule(250);
                            break;
                        }
                        LogUtils.sendDebug("[AutoSell] Detected Bazaar menu, selling item");
                        PlayerUtils.clickOpenContainerSlot(47);
                        sellClock.schedule(250);
                    }
                } else if (PlayerUtils.getInventoryName() != null && soldToBZ && !FarmHelper.config.autoSellMarketType && PlayerUtils.getInventoryName().contains("Bazaar")) {
                    if (mc.thePlayer.openContainer.getSlot(48) != null &&
                            mc.thePlayer.openContainer.getSlot(48).getStack() != null) {
                        if (mc.thePlayer.openContainer.getSlot(48).getStack().getItem() == Item.getItemFromBlock(Blocks.glass_pane) && mc.thePlayer.openContainer.getSlot(48).getStack().getDisplayName().isEmpty()) {
                            currentState = State.SELL_INVENTORY;
                            soldToBZ = false;
                            sellClock.schedule(250);
                            if (disableOnlyFlag) {
                                disableOnly();
                                disableOnlyFlag = false;
                            } else {
                                disableAndRestart();
                            }
                            break;
                        } else if (mc.thePlayer.openContainer.getSlot(48).getStack().getDisplayName().contains("Sell Sacks Now")) {
                            LogUtils.sendDebug("[AutoSell] Detected Bazaar menu, selling item");
                            PlayerUtils.clickOpenContainerSlot(48);
                            soldToBZ = false;
                            soldSacks = true;
                            sellClock.schedule(250);
                        } else {
                            LogUtils.sendDebug("[AutoSell] Unknown Bazaar menu");
                            mc.thePlayer.closeScreen();
                            if (disableOnlyFlag) {
                                disableOnly();
                                disableOnlyFlag = false;
                            } else {
                                disableAndRestart();
                            }
                        }
                    }
                } else if (PlayerUtils.getInventoryName() != null && !soldToBZ && !FarmHelper.config.autoSellMarketType && PlayerUtils.getInventoryName().contains("Are you sure?")) {
                    LogUtils.sendDebug("[AutoSell] Detected Bazaar confirmation, selling item");
                    PlayerUtils.clickOpenContainerSlot(11);
                    sellClock.schedule(250);
                    soldToBZ = true;
                } else if (PlayerUtils.getInventoryName() != null && soldToBZ && !FarmHelper.config.autoSellMarketType && PlayerUtils.getInventoryName().contains("Are you sure?")) {
                    mc.thePlayer.closeScreen();
                    if (soldSacks)
                        if (disableOnlyFlag) {
                            disableOnly();
                            disableOnlyFlag = false;
                        } else {
                            disableAndRestart();
                        }
                } else {
                    LogUtils.sendDebug("[AutoSell] Unknown menu " + PlayerUtils.getInventoryName());
                    mc.thePlayer.closeScreen();
                }
                break;

            case SACKS:
                if (sackSlot == -1) {
                    LogUtils.sendDebug("[AutoSell] No sack found! Exiting");
                    if (disableOnlyFlag) {
                        disableOnly();
                        disableOnlyFlag = false;
                    } else {
                        disableAndRestart();
                    }
                } else if (!sackSlotsFilled()) {
                    LogUtils.sendDebug("[AutoSell] Sack item slots not returned, waiting");
                } else if (mc.thePlayer.inventory.getFirstEmptyStack() == -1) {
                    LogUtils.sendDebug("[AutoSell] Full inventory! Selling");
                    countSack();
                    currentState = State.SELL_INVENTORY;
                } else if (PlayerUtils.getInventoryName() != null && PlayerUtils.getInventoryName().contains("Enchanted Agronomy Sack")) {
                    countSack();
                    if (sackContains()) {
                        LogUtils.sendDebug("[AutoSell] Found item, claiming");
                        for (int i = 0; i < NPCSellSlots.length; i++) {
                            if (NPCSellSlotCounts[i] > 0) {
                                PlayerUtils.clickOpenContainerSlot(NPCSellSlots[i]);
                                return;
                            }
                        }
                    } else {
                        LogUtils.sendDebug("[AutoSell] Sack is empty, selling");
                        countSack();
                        currentState = State.SELL_INVENTORY;
                    }
                } else if (PlayerUtils.getInventoryName() != null &&
                    (PlayerUtils.getInventoryName().contains("SkyBlock Menu") || PlayerUtils.getInventoryName().contains("Trades"))) {
                    LogUtils.sendDebug("[AutoSell] In menu, opening sack");
                    PlayerUtils.clickOpenContainerSlot(45 + sackSlot, 1);
                } else if (mc.currentScreen == null) {
                    LogUtils.sendDebug("[AutoSell] No menu, opening SB menu");
                    mc.thePlayer.inventory.currentItem = 8;
                    KeyBindUtils.rightClick();
                } else {
                    LogUtils.sendDebug("[AutoSell] Unknown menu " + PlayerUtils.getInventoryName());
                    mc.thePlayer.closeScreen();
                }
        }
    }

    @SubscribeEvent
    public void OnKeyPress(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
            if (enabled) {
                LogUtils.sendDebug("[AutoSell] Exiting sell");
                if (disableOnlyFlag) {
                    disableOnly();
                    disableOnlyFlag = false;
                } else {
                    disableAndRestart();
                }
            }
        }
    }

    private static boolean shouldSell(ItemStack itemStack) {
        String name = net.minecraft.util.StringUtils.stripControlCodes(itemStack.getDisplayName());
        return (name.startsWith("Brown Mushroom") || name.startsWith("Enchanted Brown Mushroom") || name.startsWith("Brown Mushroom Block") || name.startsWith("Brown Enchanted Mushroom Block") ||
            name.startsWith("Red Mushroom") || name.startsWith("Enchanted Red Mushroom") || name.startsWith("Red Mushroom Block") || name.startsWith("Red Enchanted Mushroom Block") ||
            name.startsWith("Nether Wart") || name.startsWith("Enchanted Nether Wart") || name.startsWith("Mutant Nether Wart") ||
            name.startsWith("Sugar Cane") || name.startsWith("Enchanted Sugar") || name.startsWith("Enchanted Sugar Cane") ||
            name.startsWith("Cropie") || name.startsWith("Squash") || name.startsWith("Fermento") ||
            name.startsWith("Stone") || name.contains("Iron Hoe"));
    }

    private static int getSack() {
        List<Slot> sellList = PlayerUtils.getInventorySlots();
        for (Slot slot : sellList) {
            String name = net.minecraft.util.StringUtils.stripControlCodes(slot.getStack().getDisplayName());
            if (name.contains("Large Enchanted Agronomy Sack")) {
                return slot.slotNumber;
            }
        }
        return -1;
    }

    private static void countSack() {
        // Count all items in sack NPC
        for (int i = 0; i < NPCSellSlots.length; i++) {
            NPCSellSlotCounts[i] = countSlotInSack(NPCSellSlots[i]);
        }
    }

    private static boolean sackContains() {
        return Arrays.stream(NPCSellSlotCounts).anyMatch(i -> i > 0);
    }

    public static int countSlotInSack(int slotID) {
        ItemStack stack = mc.thePlayer.openContainer.getSlot(slotID).getStack();
        NBTTagList list = stack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8);
        Pattern pattern = Pattern.compile("^([a-zA-Z]+): ([0-9]+)(.*)");
        for (int j = 0; j < list.tagCount(); j++) {
            Matcher matcher = pattern.matcher(StringUtils.stripControlCodes(list.getStringTagAt(j)));
            if (matcher.matches()) {
                LogUtils.sendDebug("Stored: " + matcher.group(2));
                return Integer.parseInt(matcher.group(2));
            }
        }
        return 0;
    }

    private static boolean sackSlotsFilled() {
        for (Integer slotID : NPCSellSlots) {
            Slot slot = mc.thePlayer.openContainer.getSlot(slotID);
            if (slot == null || slot.getStack() == null || slot.getStack().stackSize <= 0) {
                return false;
            }
        }
        return true;
    }
}
