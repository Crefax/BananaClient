package com.muzmod.gui;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
import com.muzmod.duel.DuelAnalyzerState;
import com.muzmod.schedule.ScheduleEntry;
import com.muzmod.schedule.ScheduleManager;
import com.muzmod.state.IState;
import com.muzmod.state.StateManager;
import com.muzmod.state.impl.SafeState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * MuzMod Modern GUI v2.1
 * With Schedule Support
 */
public class MuzModGuiModern extends GuiScreen {
    
    // Colors
    private static final int BG_DARK = 0xE0101010;
    private static final int BG_PANEL = 0xFF1A1A1A;
    private static final int BG_HEADER = 0xFF202020;
    private static final int BG_BUTTON = 0xFF252525;
    private static final int BG_BUTTON_HOVER = 0xFF303030;
    private static final int BG_FIELD = 0xFF151515;
    private static final int BG_LIST_ITEM = 0xFF1E1E1E;
    private static final int BG_LIST_ITEM_HOVER = 0xFF282828;
    private static final int BG_LIST_ITEM_SELECTED = 0xFF2A2A40;
    
    private static final int ACCENT_PURPLE = 0xFF9B59B6;
    private static final int ACCENT_CYAN = 0xFF00BCD4;
    private static final int ACCENT_GREEN = 0xFF4CAF50;
    private static final int ACCENT_RED = 0xFFE53935;
    private static final int ACCENT_ORANGE = 0xFFFF9800;
    private static final int ACCENT_BLUE = 0xFF2196F3;
    private static final int ACCENT_YELLOW = 0xFFFFEB3B;
    
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int TEXT_DARK = 0xFF666666;
    
    // Layout
    private int guiX, guiY;
    private static final int GUI_WIDTH = 500;
    private static final int GUI_HEIGHT = 420;
    
    // Tabs
    private int currentTab = 0;
    private String[] tabNames = {"Genel", "Zamanlama", "Ayarlar"};
    
    // Settings sub-tabs
    private int settingsSubTab = 0; // 0=Genel, 1=Mining, 2=OX, 3=Obsidyen, 4=Duel, 5=Config
    private String[] settingsSubTabs = {"Genel", "Mining", "OX", "Obsidyen", "Duel", "Config"};
    
    // Mining settings scroll
    private int miningSettingsScrollOffset = 0;
    private static final int MINING_SETTINGS_SCROLL_MAX = 150; // Maksimum scroll miktarı
    
    // Obsidyen settings scroll
    private int obsidianSettingsScrollOffset = 0;
    private static final int OBSIDIAN_SETTINGS_SCROLL_MAX = 200; // Maksimum scroll miktarı
    
    // Schedule tab
    private int selectedDay = 0; // 0=Pzt, 6=Paz
    private long selectedEntryId = -1;
    private int scheduleScrollOffset = 0;
    private boolean addingNewEntry = false;
    
    // New entry fields
    private GuiTextField fieldStartTime, fieldEndTime, fieldWarpCommand;
    private int newEntryType = 0; // 0=Mining, 1=AFK, 2=Repair
    
    // Settings fields
    private GuiTextField fieldDefaultMiningWarp, fieldDefaultAfkWarp;
    private GuiTextField fieldRepairThreshold, fieldTimeOffset, fieldDetectRadius, fieldWalkDist;
    private GuiTextField fieldRepairClickDelay;
    
    // Mining settings fields
    private GuiTextField fieldInitialWalkMin, fieldInitialWalkMax, fieldWalkYawVar;
    private GuiTextField fieldSecondWalkMin, fieldSecondWalkMax, fieldSecondWalkAngle;
    private GuiTextField fieldStrafeInterval, fieldStrafeDuration;
    private GuiTextField fieldMaxDistFromCenter;
    
    // Mining Jitter fields
    private GuiTextField fieldMiningJitterYaw, fieldMiningJitterPitch, fieldMiningJitterInterval;
    
    // OX settings fields
    private GuiTextField fieldOxMinPlayers;
    
    // Obsidyen settings fields (jitter + aim hızı + sell)
    private GuiTextField fieldObsidianJitterYaw, fieldObsidianJitterPitch, fieldObsidianJitterInterval;
    private GuiTextField fieldObsidianAimSpeed, fieldObsidianTurnSpeed;
    private GuiTextField fieldObsidianSellCommand, fieldObsidianSellDelay;
    private GuiTextField fieldObsidianTargetMin, fieldObsidianTargetMax;
    private boolean obsidianSellEnabled = true;
    
    // Duel Analyzer fields
    private GuiTextField fieldDuelPlayer1, fieldDuelPlayer2;
    
    // Slider için drag state
    private boolean draggingAimSpeed = false;
    private boolean draggingTurnSpeed = false;
    
    private List<GuiTextField> allFields = new ArrayList<>();
    
    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        
        guiX = (width - GUI_WIDTH) / 2;
        guiY = (height - GUI_HEIGHT) / 2;
        
        initFields();
    }
    
    private void initFields() {
        allFields.clear();
        
        ModConfig config = MuzMod.instance.getConfig();
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        
        int baseX = guiX + 250;
        int baseY = guiY + 100;
        
        // New entry fields (Zamanlama tab)
        fieldStartTime = createField(baseX, baseY, 50, "10:00", 5);
        fieldEndTime = createField(baseX + 60, baseY, 50, "12:00", 5);
        fieldWarpCommand = createField(baseX, baseY + 25, 120, "/warp maden", 50);
        
        // Settings fields (Genel sub-tab)
        int setX = guiX + 130;
        int setY = guiY + 100;
        
        fieldDefaultMiningWarp = createField(setX, setY, 150, schedule.getDefaultMiningWarp(), 50);
        fieldDefaultAfkWarp = createField(setX, setY + 28, 150, schedule.getDefaultAfkWarp(), 50);
        fieldRepairThreshold = createField(setX, setY + 56, 50, String.valueOf(config.getRepairDurabilityThreshold()), 5);
        fieldTimeOffset = createField(setX, setY + 84, 50, String.valueOf(config.getTimeOffsetHours()), 4);
        fieldDetectRadius = createField(setX, setY + 112, 50, String.valueOf((int)config.getPlayerDetectionRadius()), 4);
        fieldRepairClickDelay = createField(setX, setY + 140, 50, String.valueOf(config.getRepairClickDelay()), 4);
        
        // Mining settings fields
        fieldInitialWalkMin = createField(setX, setY, 50, String.valueOf(config.getInitialWalkDistanceMin()), 4);
        fieldInitialWalkMax = createField(setX + 70, setY, 50, String.valueOf(config.getInitialWalkDistanceMax()), 4);
        fieldWalkYawVar = createField(setX, setY + 28, 50, String.valueOf((int)config.getWalkYawVariation()), 4);
        fieldMaxDistFromCenter = createField(setX, setY + 56, 50, String.valueOf(config.getMaxDistanceFromCenter()), 4);
        fieldSecondWalkMin = createField(setX, setY + 112, 50, String.valueOf(config.getSecondWalkDistanceMin()), 4);
        fieldSecondWalkMax = createField(setX + 70, setY + 112, 50, String.valueOf(config.getSecondWalkDistanceMax()), 4);
        fieldSecondWalkAngle = createField(setX, setY + 140, 50, String.valueOf((int)config.getSecondWalkAngleVariation()), 4);
        fieldStrafeInterval = createField(setX, setY + 196, 50, String.valueOf(config.getStrafeInterval() / 1000), 4);
        fieldStrafeDuration = createField(setX + 70, setY + 196, 50, String.valueOf(config.getStrafeDuration()), 4);
        
        // Mining Jitter fields
        fieldMiningJitterYaw = createField(setX, setY, 50, String.valueOf(config.getMiningJitterYaw()), 5);
        fieldMiningJitterPitch = createField(setX + 70, setY, 50, String.valueOf(config.getMiningJitterPitch()), 5);
        fieldMiningJitterInterval = createField(setX, setY + 28, 60, String.valueOf(config.getMiningJitterInterval()), 5);
        
        // OX settings fields
        fieldOxMinPlayers = createField(setX, setY, 50, String.valueOf(config.getOxMinPlayers()), 3);
        
        // Obsidyen settings fields (jitter + aim hızı + sell)
        fieldObsidianJitterYaw = createField(setX, setY, 50, String.valueOf(config.getObsidianJitterYaw()), 5);
        fieldObsidianJitterPitch = createField(setX + 70, setY, 50, String.valueOf(config.getObsidianJitterPitch()), 5);
        fieldObsidianJitterInterval = createField(setX, setY + 28, 60, String.valueOf(config.getObsidianJitterInterval()), 5);
        fieldObsidianAimSpeed = createField(setX, setY, 50, String.valueOf(config.getObsidianAimSpeed()), 5);
        fieldObsidianTurnSpeed = createField(setX, setY, 50, String.valueOf(config.getObsidianTurnSpeed()), 5);
        fieldObsidianSellCommand = createField(setX, setY, 120, config.getObsidianSellCommand(), 50);
        fieldObsidianSellDelay = createField(setX, setY, 50, String.valueOf(config.getObsidianSellDelay() / 1000.0), 5);
        fieldObsidianTargetMin = createField(setX, setY, 40, String.valueOf(config.getObsidianTargetMinOffset()), 3);
        fieldObsidianTargetMax = createField(setX, setY, 40, String.valueOf(config.getObsidianTargetMaxOffset()), 3);
        obsidianSellEnabled = config.isObsidianSellEnabled();
        
        // Duel Analyzer fields
        fieldDuelPlayer1 = createField(setX, setY, 100, "", 16);
        fieldDuelPlayer2 = createField(setX, setY + 25, 100, "", 16);
        
        // Legacy field for compatibility
        fieldWalkDist = fieldInitialWalkMin;
    }
    
    private GuiTextField createField(int x, int y, int w, String text, int maxLen) {
        GuiTextField field = new GuiTextField(allFields.size(), fontRendererObj, x, y, w, 16);
        field.setMaxStringLength(maxLen);
        field.setText(text);
        field.setEnableBackgroundDrawing(false);
        allFields.add(field);
        return field;
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Dark overlay
        drawRect(0, 0, width, height, 0xA0000000);
        
        // Main panel
        drawRect(guiX + 4, guiY + 4, guiX + GUI_WIDTH + 4, guiY + GUI_HEIGHT + 4, 0x40000000);
        drawRect(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, BG_PANEL);
        
        // Header
        drawGradientRect(guiX, guiY, guiX + GUI_WIDTH, guiY + 32, BG_HEADER, BG_PANEL);
        drawString(fontRendererObj, "§e§l" + MuzMod.CLIENT_NAME + " §7v" + MuzMod.VERSION, guiX + 12, guiY + 12, TEXT_WHITE);
        
        // Time & Day
        ModConfig config = MuzMod.instance.getConfig();
        Calendar cal = Calendar.getInstance();
        int hour = (cal.get(Calendar.HOUR_OF_DAY) + config.getTimeOffsetHours() + 24) % 24;
        int javaDow = cal.get(Calendar.DAY_OF_WEEK);
        int dayOfWeek = (javaDow == Calendar.SUNDAY) ? 6 : javaDow - 2;
        String dayName = ScheduleEntry.getDayShortName(dayOfWeek);
        String timeStr = String.format("§e%s %02d:%02d", dayName, hour, cal.get(Calendar.MINUTE));
        drawString(fontRendererObj, timeStr, guiX + GUI_WIDTH - 85, guiY + 12, TEXT_WHITE);
        
        // Header line
        drawGradientRectH(guiX, guiY + 31, guiX + GUI_WIDTH, guiY + 33, ACCENT_PURPLE, ACCENT_CYAN);
        
        // Tabs
        drawTabs(mouseX, mouseY);
        
        // Content
        switch (currentTab) {
            case 0: drawGeneralTab(mouseX, mouseY); break;
            case 1: drawScheduleTab(mouseX, mouseY); break;
            case 2: drawSettingsTab(mouseX, mouseY); break;
        }
        
        // Bottom bar
        drawRect(guiX, guiY + GUI_HEIGHT - 35, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, BG_HEADER);
        drawGradientRectH(guiX, guiY + GUI_HEIGHT - 36, guiX + GUI_WIDTH, guiY + GUI_HEIGHT - 35, ACCENT_PURPLE, ACCENT_CYAN);
        
        // Save/Close buttons
        drawButton(guiX + GUI_WIDTH - 160, guiY + GUI_HEIGHT - 28, 70, 20, "§aKaydet", ACCENT_GREEN, mouseX, mouseY);
        drawButton(guiX + GUI_WIDTH - 80, guiY + GUI_HEIGHT - 28, 70, 20, "§cKapat", ACCENT_RED, mouseX, mouseY);
        
        // Footer
        drawString(fontRendererObj, "§8" + MuzMod.GITHUB_URL, guiX + 10, guiY + GUI_HEIGHT - 23, TEXT_DARK);
        
        // Safe state warning
        IState state = MuzMod.instance.getStateManager().getCurrentState();
        if (state instanceof SafeState) {
            SafeState safe = (SafeState) state;
            drawRect(guiX, guiY + GUI_HEIGHT - 55, guiX + GUI_WIDTH, guiY + GUI_HEIGHT - 38, 0xAAFF0000);
            drawCenteredString(fontRendererObj, "§c§l⚠ " + safe.getReason().getMessage(), 
                guiX + GUI_WIDTH / 2, guiY + GUI_HEIGHT - 50, TEXT_WHITE);
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private void drawTabs(int mouseX, int mouseY) {
        int tabWidth = GUI_WIDTH / tabNames.length;
        int tabY = guiY + 38;
        
        for (int i = 0; i < tabNames.length; i++) {
            int tabX = guiX + i * tabWidth;
            boolean hovered = mouseX >= tabX && mouseX < tabX + tabWidth && mouseY >= tabY && mouseY < tabY + 26;
            boolean selected = currentTab == i;
            
            if (selected) {
                drawRect(tabX, tabY, tabX + tabWidth, tabY + 26, BG_PANEL);
                int[] colors = {ACCENT_PURPLE, ACCENT_CYAN, ACCENT_YELLOW};
                drawRect(tabX + 5, tabY + 23, tabX + tabWidth - 5, tabY + 25, colors[i]);
            } else if (hovered) {
                drawRect(tabX, tabY, tabX + tabWidth, tabY + 26, BG_BUTTON_HOVER);
            } else {
                drawRect(tabX, tabY, tabX + tabWidth, tabY + 26, BG_HEADER);
            }
            
            String text = tabNames[i];
            int textX = tabX + (tabWidth - fontRendererObj.getStringWidth(text)) / 2;
            drawString(fontRendererObj, text, textX, tabY + 9, selected ? TEXT_WHITE : TEXT_GRAY);
        }
    }
    
    private void drawGeneralTab(int mouseX, int mouseY) {
        int y = guiY + 75;
        
        // Bot toggle
        boolean botOn = MuzMod.instance.isBotEnabled();
        String botText = botOn ? "§a● Bot Aktif" : "§c○ Bot Kapalı";
        int botColor = botOn ? ACCENT_GREEN : ACCENT_RED;
        drawButton(guiX + 20, y, 140, 26, botText, botColor, mouseX, mouseY);
        
        // Schedule toggle
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        boolean schedOn = schedule.isScheduleEnabled();
        String schedText = schedOn ? "§a● Zamanlama Açık" : "§c○ Zamanlama Kapalı";
        drawButton(guiX + 170, y, 140, 26, schedText, schedOn ? ACCENT_CYAN : BG_BUTTON, mouseX, mouseY);
        
        // Auto AFK toggle
        boolean autoAfk = schedule.isAutoAfkWhenIdle();
        String afkText = autoAfk ? "§a✓ Boşta AFK" : "§c✗ Boşta AFK";
        drawButton(guiX + 320, y, 110, 26, afkText, autoAfk ? ACCENT_BLUE : BG_BUTTON, mouseX, mouseY);
        
        // HUD toggle
        y += 30;
        boolean hudOn = MuzMod.instance.getConfig().isShowOverlay();
        String hudText = hudOn ? "§a● HUD Açık" : "§c○ HUD Kapalı";
        drawButton(guiX + 20, y, 120, 22, hudText, hudOn ? ACCENT_GREEN : BG_BUTTON, mouseX, mouseY);
        
        // State buttons
        y += 30;
        drawString(fontRendererObj, "§7Manuel Durum:", guiX + 20, y, TEXT_GRAY);
        y += 14;
        drawButton(guiX + 20, y, 55, 22, "Idle", BG_BUTTON, mouseX, mouseY);
        drawButton(guiX + 80, y, 55, 22, "§6Mine", ACCENT_ORANGE, mouseX, mouseY);
        drawButton(guiX + 140, y, 55, 22, "§bAFK", ACCENT_BLUE, mouseX, mouseY);
        drawButton(guiX + 200, y, 55, 22, "§eRepair", ACCENT_YELLOW, mouseX, mouseY);
        drawButton(guiX + 260, y, 55, 22, "§dOX", ACCENT_PURPLE, mouseX, mouseY);
        drawButton(guiX + 320, y, 55, 22, "§8Obsid", 0xFF444444, mouseX, mouseY);
        
        // Current state info
        y += 35;
        IState state = MuzMod.instance.getStateManager().getCurrentState();
        String stateName = state != null ? state.getName() : "Idle";
        String status = state != null ? state.getStatus() : "Bekleniyor...";
        
        drawString(fontRendererObj, "§7Mevcut Durum: §f" + stateName, guiX + 20, y, TEXT_WHITE);
        drawString(fontRendererObj, "§8" + truncate(status, 50), guiX + 20, y + 14, TEXT_DARK);
        
        // Scheduled event info
        y += 35;
        Calendar cal = Calendar.getInstance();
        ModConfig config = MuzMod.instance.getConfig();
        int hour = (cal.get(Calendar.HOUR_OF_DAY) + config.getTimeOffsetHours() + 24) % 24;
        int minute = cal.get(Calendar.MINUTE);
        int javaDow = cal.get(Calendar.DAY_OF_WEEK);
        int dayOfWeek = (javaDow == Calendar.SUNDAY) ? 6 : javaDow - 2;
        
        ScheduleEntry currentEntry = schedule.getCurrentEntry(dayOfWeek, hour, minute);
        if (currentEntry != null) {
            drawString(fontRendererObj, "§7Aktif Etkinlik: §f" + currentEntry.getEventType().getDisplayName(), guiX + 20, y, TEXT_WHITE);
            drawString(fontRendererObj, "§8" + currentEntry.getTimeRange(), guiX + 20, y + 14, TEXT_DARK);
        } else {
            ScheduleEntry.EventType type = schedule.getCurrentScheduledType(dayOfWeek, hour, minute);
            drawString(fontRendererObj, "§7Zamanlanan: §f" + type.getDisplayName(), guiX + 20, y, TEXT_WHITE);
        }
        
        // Pickaxe info (right side)
        int px = guiX + 280;
        int py = guiY + 175;
        drawRect(px, py, guiX + GUI_WIDTH - 15, py + 90, BG_HEADER);
        drawString(fontRendererObj, "§eKazma Durumu", px + 8, py + 6, ACCENT_YELLOW);
        
        if (mc.thePlayer != null) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held != null && held.getItem() instanceof ItemPickaxe) {
                int max = held.getMaxDamage();
                int damage = held.getItemDamage();
                int remaining = max - damage;
                float percent = (float) remaining / max;
                
                drawString(fontRendererObj, "§f" + truncate(held.getDisplayName(), 18), px + 8, py + 22, TEXT_WHITE);
                
                // Progress bar
                int barX = px + 8;
                int barY = py + 40;
                int barW = 130;
                int barH = 8;
                
                drawRect(barX, barY, barX + barW, barY + barH, BG_FIELD);
                int fillW = (int) (barW * percent);
                int barColor = percent > 0.5f ? ACCENT_GREEN : (percent > 0.25f ? ACCENT_YELLOW : ACCENT_RED);
                drawRect(barX, barY, barX + fillW, barY + barH, barColor);
                
                drawString(fontRendererObj, "§7" + remaining + "/" + max, px + 8, py + 55, TEXT_GRAY);
                drawString(fontRendererObj, String.format("§f%.0f%%", percent * 100), px + 90, py + 55, TEXT_WHITE);
            } else {
                drawString(fontRendererObj, "§cKazma yok", px + 8, py + 35, ACCENT_RED);
            }
        }
    }
    
    private void drawScheduleTab(int mouseX, int mouseY) {
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        
        // Day selector
        int dayY = guiY + 70;
        String[] shortDays = {"Pzt", "Sal", "Car", "Per", "Cum", "Cmt", "Paz"};
        int dayWidth = 55;
        
        for (int i = 0; i < 7; i++) {
            int dx = guiX + 15 + i * dayWidth;
            boolean hovered = mouseX >= dx && mouseX < dx + dayWidth - 2 && mouseY >= dayY && mouseY < dayY + 20;
            boolean selected = selectedDay == i;
            
            int bg = selected ? ACCENT_PURPLE : (hovered ? BG_BUTTON_HOVER : BG_BUTTON);
            drawRect(dx, dayY, dx + dayWidth - 2, dayY + 20, bg);
            
            int entryCount = schedule.getEntryCountForDay(i);
            String dayText = shortDays[i] + (entryCount > 0 ? " §7(" + entryCount + ")" : "");
            int textW = fontRendererObj.getStringWidth(dayText);
            drawString(fontRendererObj, dayText, dx + (dayWidth - 2 - textW) / 2, dayY + 6, selected ? TEXT_WHITE : TEXT_GRAY);
        }
        
        // Entry list for selected day
        int listX = guiX + 15;
        int listY = guiY + 100;
        int listW = 220;
        int listH = 155;
        
        drawRect(listX, listY, listX + listW, listY + listH, BG_HEADER);
        drawString(fontRendererObj, "§f" + ScheduleEntry.getDayName(selectedDay) + " Etkinlikleri", listX + 5, listY + 5, TEXT_WHITE);
        
        List<ScheduleEntry> entries = schedule.getEntriesForDay(selectedDay);
        int itemH = 28;
        int visibleItems = 4;
        int itemY = listY + 22;
        
        for (int i = scheduleScrollOffset; i < Math.min(entries.size(), scheduleScrollOffset + visibleItems); i++) {
            ScheduleEntry entry = entries.get(i);
            boolean itemHover = mouseX >= listX + 3 && mouseX < listX + listW - 3 && mouseY >= itemY && mouseY < itemY + itemH - 2;
            boolean itemSelected = entry.getId() == selectedEntryId;
            
            int itemBg = itemSelected ? BG_LIST_ITEM_SELECTED : (itemHover ? BG_LIST_ITEM_HOVER : BG_LIST_ITEM);
            drawRect(listX + 3, itemY, listX + listW - 3, itemY + itemH - 2, itemBg);
            
            // Color indicator
            drawRect(listX + 3, itemY, listX + 6, itemY + itemH - 2, entry.getEventType().getColor());
            
            // Time & Type
            String timeStr = entry.getTimeRange();
            String typeStr = entry.getEventType().getDisplayName();
            drawString(fontRendererObj, "§f" + timeStr, listX + 10, itemY + 4, TEXT_WHITE);
            drawString(fontRendererObj, "§7" + typeStr, listX + 10, itemY + 15, TEXT_GRAY);
            
            // Enabled indicator
            if (!entry.isEnabled()) {
                drawString(fontRendererObj, "§c✗", listX + listW - 18, itemY + 9, ACCENT_RED);
            }
            
            itemY += itemH;
        }
        
        // Scroll indicators
        if (scheduleScrollOffset > 0) {
            drawString(fontRendererObj, "§7▲", listX + listW / 2, listY + 20, TEXT_GRAY);
        }
        if (entries.size() > scheduleScrollOffset + visibleItems) {
            drawString(fontRendererObj, "§7▼", listX + listW / 2, listY + listH - 12, TEXT_GRAY);
        }
        
        // Empty state
        if (entries.isEmpty()) {
            drawCenteredString(fontRendererObj, "§8Etkinlik yok", listX + listW / 2, listY + 70, TEXT_DARK);
        }
        
        // Right panel - Add/Edit entry
        int panelX = guiX + 245;
        int panelY = guiY + 100;
        int panelW = 190;
        
        drawRect(panelX, panelY, panelX + panelW, panelY + 155, BG_HEADER);
        
        if (addingNewEntry || selectedEntryId != -1) {
            String title = addingNewEntry ? "§aYeni Etkinlik" : "§eDuzenle";
            drawString(fontRendererObj, title, panelX + 8, panelY + 6, TEXT_WHITE);
            
            // Time inputs
            drawString(fontRendererObj, "§7Baslangic:", panelX + 8, panelY + 26, TEXT_GRAY);
            drawFieldBackground(fieldStartTime, panelX + 70, panelY + 24);
            fieldStartTime.xPosition = panelX + 70;
            fieldStartTime.yPosition = panelY + 24;
            fieldStartTime.drawTextBox();
            
            drawString(fontRendererObj, "§7Bitis:", panelX + 8, panelY + 46, TEXT_GRAY);
            drawFieldBackground(fieldEndTime, panelX + 70, panelY + 44);
            fieldEndTime.xPosition = panelX + 70;
            fieldEndTime.yPosition = panelY + 44;
            fieldEndTime.drawTextBox();
            
            // Event type selector
            drawString(fontRendererObj, "§7Tip:", panelX + 8, panelY + 68, TEXT_GRAY);
            String[] types = {"§6Maden", "§bAFK", "§eTamir", "§dOX"};
            int[] typeColors = {ACCENT_ORANGE, ACCENT_BLUE, ACCENT_YELLOW, ACCENT_PURPLE};
            for (int i = 0; i < 4; i++) {
                int tx = panelX + 8 + i * 45;
                boolean tHover = mouseX >= tx && mouseX < tx + 42 && mouseY >= panelY + 65 && mouseY < panelY + 82;
                boolean tSel = newEntryType == i;
                drawRect(tx, panelY + 65, tx + 42, panelY + 82, tSel ? typeColors[i] : (tHover ? BG_BUTTON_HOVER : BG_BUTTON));
                drawCenteredString(fontRendererObj, types[i], tx + 21, panelY + 69, TEXT_WHITE);
            }
            
            // Warp command
            drawString(fontRendererObj, "§7Warp (opsiyonel):", panelX + 8, panelY + 90, TEXT_GRAY);
            drawFieldBackground(fieldWarpCommand, panelX + 8, panelY + 103);
            fieldWarpCommand.xPosition = panelX + 8;
            fieldWarpCommand.yPosition = panelY + 103;
            fieldWarpCommand.width = panelW - 20;
            fieldWarpCommand.drawTextBox();
            
            // Action buttons
            if (addingNewEntry) {
                drawButton(panelX + 8, panelY + 128, 80, 20, "§aEkle", ACCENT_GREEN, mouseX, mouseY);
                drawButton(panelX + 100, panelY + 128, 80, 20, "§7Iptal", BG_BUTTON, mouseX, mouseY);
            } else {
                drawButton(panelX + 8, panelY + 128, 55, 20, "§aKaydet", ACCENT_GREEN, mouseX, mouseY);
                drawButton(panelX + 68, panelY + 128, 55, 20, "§cSil", ACCENT_RED, mouseX, mouseY);
                drawButton(panelX + 128, panelY + 128, 55, 20, "§7Iptal", BG_BUTTON, mouseX, mouseY);
            }
        } else {
            drawString(fontRendererObj, "§7Etkinlik Ekle", panelX + 8, panelY + 6, TEXT_GRAY);
            drawButton(panelX + 40, panelY + 60, 110, 28, "§a+ Yeni Etkinlik", ACCENT_GREEN, mouseX, mouseY);
            
            // Quick actions
            drawString(fontRendererObj, "§7Hizli Islemler:", panelX + 8, panelY + 105, TEXT_GRAY);
            drawButton(panelX + 8, panelY + 120, 85, 18, "§7Hafta Icine", BG_BUTTON, mouseX, mouseY);
            drawButton(panelX + 98, panelY + 120, 85, 18, "§7Hafta Sonuna", BG_BUTTON, mouseX, mouseY);
        }
    }
    
    private void drawSettingsTab(int mouseX, int mouseY) {
        ModConfig config = MuzMod.instance.getConfig();
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        
        // Sub-tab selector
        int subTabY = guiY + 68;
        int subTabWidth = 100;
        for (int i = 0; i < settingsSubTabs.length; i++) {
            int sx = guiX + 15 + i * subTabWidth;
            boolean hovered = mouseX >= sx && mouseX < sx + subTabWidth - 2 && mouseY >= subTabY && mouseY < subTabY + 18;
            boolean selected = settingsSubTab == i;
            
            int bg = selected ? ACCENT_PURPLE : (hovered ? BG_BUTTON_HOVER : BG_BUTTON);
            drawRect(sx, subTabY, sx + subTabWidth - 2, subTabY + 18, bg);
            
            String tabText = settingsSubTabs[i];
            int textW = fontRendererObj.getStringWidth(tabText);
            drawString(fontRendererObj, tabText, sx + (subTabWidth - 2 - textW) / 2, subTabY + 5, selected ? TEXT_WHITE : TEXT_GRAY);
        }
        
        int y = guiY + 95;
        int labelX = guiX + 20;
        int fieldX = guiX + 160;
        
        // Draw sub-tab content
        switch (settingsSubTab) {
            case 0: drawSettingsGeneral(mouseX, mouseY, config, schedule, y, labelX, fieldX); break;
            case 1: drawSettingsMining(mouseX, mouseY, config, y, labelX, fieldX); break;
            case 2: drawSettingsOX(mouseX, mouseY, config, y, labelX, fieldX); break;
            case 3: drawSettingsObsidyen(mouseX, mouseY, config, y - obsidianSettingsScrollOffset, labelX, fieldX); break;
            case 4: drawSettingsDuel(mouseX, mouseY, config, y, labelX, fieldX); break;
            case 5: drawSettingsConfig(mouseX, mouseY, y, labelX, fieldX); break;
        }
    }
    
    private void drawSettingsGeneral(int mouseX, int mouseY, ModConfig config, ScheduleManager schedule, int y, int labelX, int fieldX) {
        // Default warps
        drawString(fontRendererObj, "§7Maden Warp:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldDefaultMiningWarp, fieldX, y);
        fieldDefaultMiningWarp.xPosition = fieldX;
        fieldDefaultMiningWarp.yPosition = y;
        fieldDefaultMiningWarp.drawTextBox();
        
        y += 26;
        drawString(fontRendererObj, "§7AFK Warp:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldDefaultAfkWarp, fieldX, y);
        fieldDefaultAfkWarp.xPosition = fieldX;
        fieldDefaultAfkWarp.yPosition = y;
        fieldDefaultAfkWarp.drawTextBox();
        
        y += 26;
        drawString(fontRendererObj, "§7Tamir Esigi:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldRepairThreshold, fieldX, y);
        fieldRepairThreshold.xPosition = fieldX;
        fieldRepairThreshold.yPosition = y;
        fieldRepairThreshold.drawTextBox();
        drawString(fontRendererObj, "§8dur", fieldX + 55, y + 3, TEXT_DARK);
        
        y += 26;
        drawString(fontRendererObj, "§7Saat Ofseti:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldTimeOffset, fieldX, y);
        fieldTimeOffset.xPosition = fieldX;
        fieldTimeOffset.yPosition = y;
        fieldTimeOffset.drawTextBox();
        drawString(fontRendererObj, "§8saat", fieldX + 55, y + 3, TEXT_DARK);
        
        y += 26;
        drawString(fontRendererObj, "§7Tespit Yaricapi:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldDetectRadius, fieldX, y);
        fieldDetectRadius.xPosition = fieldX;
        fieldDetectRadius.yPosition = y;
        fieldDetectRadius.drawTextBox();
        drawString(fontRendererObj, "§8blok", fieldX + 55, y + 3, TEXT_DARK);
        
        y += 26;
        drawString(fontRendererObj, "§7Tamir Tiklama:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldRepairClickDelay, fieldX, y);
        fieldRepairClickDelay.xPosition = fieldX;
        fieldRepairClickDelay.yPosition = y;
        fieldRepairClickDelay.drawTextBox();
        drawString(fontRendererObj, "§8sn", fieldX + 55, y + 3, TEXT_DARK);
        
        // Toggles
        y += 30;
        drawToggle(labelX, y, "Blok Kilidi", config.isBlockLockEnabled(), mouseX, mouseY);
        drawToggle(labelX + 120, y, "Aninda Kac", config.isInstantFlee(), mouseX, mouseY);
        drawToggle(labelX + 240, y, "Strafe", config.isStrafeEnabled(), mouseX, mouseY);
    }
    
    private void drawSettingsMining(int mouseX, int mouseY, ModConfig config, int y, int labelX, int fieldX) {
        // Scroll offset'i uygula
        int scrollY = y - miningSettingsScrollOffset;
        
        // Clipping area için başlangıç Y
        int clipTop = guiY + 90;
        int clipBottom = guiY + GUI_HEIGHT - 45;
        
        // Scroll göstergesi (sağ taraf)
        if (miningSettingsScrollOffset > 0 || MINING_SETTINGS_SCROLL_MAX > 0) {
            int scrollBarX = guiX + GUI_WIDTH - 20;
            int scrollBarY = clipTop;
            int scrollBarH = clipBottom - clipTop;
            
            // Scroll bar arka planı
            drawRect(scrollBarX, scrollBarY, scrollBarX + 6, scrollBarY + scrollBarH, BG_FIELD);
            
            // Scroll bar pozisyonu
            float scrollRatio = (float) miningSettingsScrollOffset / MINING_SETTINGS_SCROLL_MAX;
            int thumbH = 30;
            int thumbY = scrollBarY + (int) ((scrollBarH - thumbH) * scrollRatio);
            drawRect(scrollBarX, thumbY, scrollBarX + 6, thumbY + thumbH, ACCENT_CYAN);
            
            // Scroll ok göstergeleri
            if (miningSettingsScrollOffset > 0) {
                drawString(fontRendererObj, "§7▲", scrollBarX - 4, clipTop + 2, TEXT_GRAY);
            }
            if (miningSettingsScrollOffset < MINING_SETTINGS_SCROLL_MAX) {
                drawString(fontRendererObj, "§7▼", scrollBarX - 4, clipBottom - 12, TEXT_GRAY);
            }
        }
        
        // İlk Yürüyüş
        if (scrollY >= clipTop - 20 && scrollY < clipBottom) {
            drawString(fontRendererObj, "§6§lİlk Yürüyüş (South)", labelX, scrollY, ACCENT_ORANGE);
        }
        scrollY += 16;
        
        if (scrollY >= clipTop - 16 && scrollY < clipBottom) {
            drawString(fontRendererObj, "§7Min-Max Mesafe:", labelX, scrollY + 3, TEXT_GRAY);
            drawFieldBackground(fieldInitialWalkMin, fieldX, scrollY);
            fieldInitialWalkMin.xPosition = fieldX;
            fieldInitialWalkMin.yPosition = scrollY;
            fieldInitialWalkMin.drawTextBox();
            drawString(fontRendererObj, "§7-", fieldX + 55, scrollY + 3, TEXT_GRAY);
            drawFieldBackground(fieldInitialWalkMax, fieldX + 65, scrollY);
            fieldInitialWalkMax.xPosition = fieldX + 65;
            fieldInitialWalkMax.yPosition = scrollY;
            fieldInitialWalkMax.drawTextBox();
            drawString(fontRendererObj, "§8blok", fieldX + 125, scrollY + 3, TEXT_DARK);
        }
        
        scrollY += 22;
        if (scrollY >= clipTop - 16 && scrollY < clipBottom) {
            drawString(fontRendererObj, "§7Açı Varyasyonu:", labelX, scrollY + 3, TEXT_GRAY);
            drawFieldBackground(fieldWalkYawVar, fieldX, scrollY);
            fieldWalkYawVar.xPosition = fieldX;
            fieldWalkYawVar.yPosition = scrollY;
            fieldWalkYawVar.drawTextBox();
            drawString(fontRendererObj, "§8derece (+/-)", fieldX + 55, scrollY + 3, TEXT_DARK);
        }
        
        // Mining Center
        scrollY += 26;
        if (scrollY >= clipTop - 20 && scrollY < clipBottom) {
            drawString(fontRendererObj, "§a§lMining Merkezi", labelX, scrollY, ACCENT_GREEN);
        }
        scrollY += 16;
        
        if (scrollY >= clipTop - 16 && scrollY < clipBottom) {
            drawString(fontRendererObj, "§7Max Uzaklık:", labelX, scrollY + 3, TEXT_GRAY);
            drawFieldBackground(fieldMaxDistFromCenter, fieldX, scrollY);
            fieldMaxDistFromCenter.xPosition = fieldX;
            fieldMaxDistFromCenter.yPosition = scrollY;
            fieldMaxDistFromCenter.drawTextBox();
            drawString(fontRendererObj, "§8blok (merkezden)", fieldX + 55, scrollY + 3, TEXT_DARK);
        }
        
        // İkinci Yürüyüş
        scrollY += 26;
        if (scrollY >= clipTop - 20 && scrollY < clipBottom) {
            drawString(fontRendererObj, "§b§lİkinci Yürüyüş (East/West)", labelX, scrollY, ACCENT_CYAN);
        }
        scrollY += 16;
        
        boolean secondEnabled = config.isSecondWalkEnabled();
        if (scrollY >= clipTop - 16 && scrollY < clipBottom) {
            drawToggle(labelX, scrollY, "Aktif", secondEnabled, mouseX, mouseY);
        }
        
        scrollY += 18;
        if (scrollY >= clipTop - 16 && scrollY < clipBottom) {
            drawString(fontRendererObj, "§7Min-Max Mesafe:", labelX, scrollY + 3, TEXT_GRAY);
            drawFieldBackground(fieldSecondWalkMin, fieldX, scrollY);
            fieldSecondWalkMin.xPosition = fieldX;
            fieldSecondWalkMin.yPosition = scrollY;
            fieldSecondWalkMin.drawTextBox();
            drawString(fontRendererObj, "§7-", fieldX + 55, scrollY + 3, TEXT_GRAY);
            drawFieldBackground(fieldSecondWalkMax, fieldX + 65, scrollY);
            fieldSecondWalkMax.xPosition = fieldX + 65;
            fieldSecondWalkMax.yPosition = scrollY;
            fieldSecondWalkMax.drawTextBox();
            drawString(fontRendererObj, "§8blok", fieldX + 125, scrollY + 3, TEXT_DARK);
        }
        
        scrollY += 22;
        if (scrollY >= clipTop - 16 && scrollY < clipBottom) {
            drawString(fontRendererObj, "§7Açı Varyasyonu:", labelX, scrollY + 3, TEXT_GRAY);
            drawFieldBackground(fieldSecondWalkAngle, fieldX, scrollY);
            fieldSecondWalkAngle.xPosition = fieldX;
            fieldSecondWalkAngle.yPosition = scrollY;
            fieldSecondWalkAngle.drawTextBox();
            drawString(fontRendererObj, "§8derece (+/-)", fieldX + 55, scrollY + 3, TEXT_DARK);
        }
        
        scrollY += 22;
        boolean randomDir = config.isSecondWalkRandomDirection();
        if (scrollY >= clipTop - 16 && scrollY < clipBottom) {
            drawToggle(labelX, scrollY, "Rastgele Yön", randomDir, mouseX, mouseY);
            drawString(fontRendererObj, randomDir ? "§a(East/West)" : "§7(Sadece West)", labelX + 110, scrollY, TEXT_GRAY);
        }
        
        // Strafe Anti-AFK
        scrollY += 26;
        if (scrollY >= clipTop - 20 && scrollY < clipBottom) {
            drawString(fontRendererObj, "§e§lStrafe Anti-AFK", labelX, scrollY, ACCENT_YELLOW);
        }
        scrollY += 16;
        
        boolean strafeEnabled = config.isStrafeEnabled();
        if (scrollY >= clipTop - 16 && scrollY < clipBottom) {
            drawToggle(labelX, scrollY, "Aktif", strafeEnabled, mouseX, mouseY);
        }
        
        scrollY += 18;
        if (scrollY >= clipTop - 16 && scrollY < clipBottom) {
            drawString(fontRendererObj, "§7Aralık / Süre:", labelX, scrollY + 3, TEXT_GRAY);
            drawFieldBackground(fieldStrafeInterval, fieldX, scrollY);
            fieldStrafeInterval.xPosition = fieldX;
            fieldStrafeInterval.yPosition = scrollY;
            fieldStrafeInterval.drawTextBox();
            drawString(fontRendererObj, "§8sn", fieldX + 55, scrollY + 3, TEXT_DARK);
            drawFieldBackground(fieldStrafeDuration, fieldX + 80, scrollY);
            fieldStrafeDuration.xPosition = fieldX + 80;
            fieldStrafeDuration.yPosition = scrollY;
            fieldStrafeDuration.drawTextBox();
            drawString(fontRendererObj, "§8ms", fieldX + 140, scrollY + 3, TEXT_DARK);
        }
        
        // Mining Jitter Anti-AFK
        scrollY += 26;
        if (scrollY >= clipTop - 20 && scrollY < clipBottom) {
            drawString(fontRendererObj, "§c§lJitter Anti-AFK", labelX, scrollY, 0xFFFF5555);
        }
        scrollY += 16;
        
        if (scrollY >= clipTop - 16 && scrollY < clipBottom) {
            drawString(fontRendererObj, "§7Jitter (yaw/pitch):", labelX, scrollY + 3, TEXT_GRAY);
            drawFieldBackground(fieldMiningJitterYaw, fieldX, scrollY);
            fieldMiningJitterYaw.xPosition = fieldX;
            fieldMiningJitterYaw.yPosition = scrollY;
            fieldMiningJitterYaw.drawTextBox();
            drawString(fontRendererObj, "§8/", fieldX + 55, scrollY + 3, TEXT_DARK);
            drawFieldBackground(fieldMiningJitterPitch, fieldX + 65, scrollY);
            fieldMiningJitterPitch.xPosition = fieldX + 65;
            fieldMiningJitterPitch.yPosition = scrollY;
            fieldMiningJitterPitch.drawTextBox();
            drawString(fontRendererObj, "§8derece", fieldX + 125, scrollY + 3, TEXT_DARK);
        }
        
        scrollY += 22;
        if (scrollY >= clipTop - 16 && scrollY < clipBottom) {
            drawString(fontRendererObj, "§7Jitter Aralık:", labelX, scrollY + 3, TEXT_GRAY);
            drawFieldBackground(fieldMiningJitterInterval, fieldX, scrollY);
            fieldMiningJitterInterval.xPosition = fieldX;
            fieldMiningJitterInterval.yPosition = scrollY;
            fieldMiningJitterInterval.drawTextBox();
            drawString(fontRendererObj, "§8ms", fieldX + 65, scrollY + 3, TEXT_DARK);
        }
    }
    
    private void drawSettingsAFK(int mouseX, int mouseY, ModConfig config, ScheduleManager schedule, int y, int labelX, int fieldX) {
        drawString(fontRendererObj, "§b§lAFK Ayarları", labelX, y, ACCENT_BLUE);
        y += 20;
        
        drawString(fontRendererObj, "§7AFK Warp:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldDefaultAfkWarp, fieldX, y);
        fieldDefaultAfkWarp.xPosition = fieldX;
        fieldDefaultAfkWarp.yPosition = y;
        fieldDefaultAfkWarp.drawTextBox();
        
        y += 30;
        boolean autoAfk = schedule.isAutoAfkWhenIdle();
        drawToggle(labelX, y, "Boşta AFK", autoAfk, mouseX, mouseY);
        drawString(fontRendererObj, "§8(Etkinlik yokken)", labelX + 100, y, TEXT_DARK);
    }
    
    private void drawSettingsOX(int mouseX, int mouseY, ModConfig config, int y, int labelX, int fieldX) {
        drawString(fontRendererObj, "§d§lOX Event Ayarları", labelX, y, ACCENT_PURPLE);
        y += 20;
        
        // Direction labels and buttons
        String[] directions = {"Kuzey", "Dogu", "Guney", "Bati"};
        float[] yawValues = {180f, -90f, 0f, 90f};
        
        // Lime direction
        drawString(fontRendererObj, "§aYeşil Taraf:", labelX, y + 3, ACCENT_GREEN);
        float limeYaw = config.getOxLimeYaw();
        int limeDir = getDirectionIndex(limeYaw);
        for (int i = 0; i < 4; i++) {
            int bx = fieldX + i * 50;
            boolean sel = (i == limeDir);
            boolean hov = mouseX >= bx && mouseX < bx + 48 && mouseY >= y && mouseY < y + 18;
            drawRect(bx, y, bx + 48, y + 18, sel ? ACCENT_GREEN : (hov ? BG_BUTTON_HOVER : BG_BUTTON));
            drawCenteredString(fontRendererObj, directions[i], bx + 24, y + 5, TEXT_WHITE);
        }
        
        y += 28;
        // Red direction
        drawString(fontRendererObj, "§cKırmızı Taraf:", labelX, y + 3, ACCENT_RED);
        float redYaw = config.getOxRedYaw();
        int redDir = getDirectionIndex(redYaw);
        for (int i = 0; i < 4; i++) {
            int bx = fieldX + i * 50;
            boolean sel = (i == redDir);
            boolean hov = mouseX >= bx && mouseX < bx + 48 && mouseY >= y && mouseY < y + 18;
            drawRect(bx, y, bx + 48, y + 18, sel ? ACCENT_RED : (hov ? BG_BUTTON_HOVER : BG_BUTTON));
            drawCenteredString(fontRendererObj, directions[i], bx + 24, y + 5, TEXT_WHITE);
        }
        
        y += 35;
        // Minimum oyuncu sayısı
        drawString(fontRendererObj, "§7Min. Oyuncu:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldOxMinPlayers, fieldX, y);
        fieldOxMinPlayers.xPosition = fieldX;
        fieldOxMinPlayers.yPosition = y;
        fieldOxMinPlayers.drawTextBox();
        drawString(fontRendererObj, "§8kişi (OX başlaması için)", fieldX + 55, y + 3, TEXT_DARK);
        
        y += 28;
        drawString(fontRendererObj, "§8Yön Açıklaması:", labelX, y, TEXT_DARK);
        y += 12;
        drawString(fontRendererObj, "§8Kuzey=180° Doğu=-90° Güney=0° Batı=90°", labelX, y, TEXT_DARK);
    }
    
    private void drawSettingsObsidyen(int mouseX, int mouseY, ModConfig config, int y, int labelX, int fieldX) {
        drawString(fontRendererObj, "§5§lObsidyen Ayarları", labelX, y, ACCENT_PURPLE);
        y += 20;
        
        // Jitter Yaw/Pitch
        drawString(fontRendererObj, "§7Jitter (AFK):", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldObsidianJitterYaw, fieldX, y);
        fieldObsidianJitterYaw.xPosition = fieldX;
        fieldObsidianJitterYaw.yPosition = y;
        fieldObsidianJitterYaw.drawTextBox();
        drawString(fontRendererObj, "§8yaw", fieldX + 55, y + 3, TEXT_DARK);
        drawFieldBackground(fieldObsidianJitterPitch, fieldX + 90, y);
        fieldObsidianJitterPitch.xPosition = fieldX + 90;
        fieldObsidianJitterPitch.yPosition = y;
        fieldObsidianJitterPitch.drawTextBox();
        drawString(fontRendererObj, "§8pitch", fieldX + 145, y + 3, TEXT_DARK);
        
        y += 26;
        // Jitter Interval
        drawString(fontRendererObj, "§7Jitter Aralık:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldObsidianJitterInterval, fieldX, y);
        fieldObsidianJitterInterval.xPosition = fieldX;
        fieldObsidianJitterInterval.yPosition = y;
        fieldObsidianJitterInterval.drawTextBox();
        drawString(fontRendererObj, "§8ms", fieldX + 65, y + 3, TEXT_DARK);
        
        y += 30;
        // === AIM HIZI AYARLARI ===
        drawString(fontRendererObj, "§d§lAim Hızı Ayarları", labelX, y, ACCENT_PURPLE);
        y += 20;
        
        // Normal Aim Hızı - Slider + Field
        drawString(fontRendererObj, "§7Normal Aim:", labelX, y + 3, TEXT_GRAY);
        drawSliderWithField(fieldX, y, 100, config.getObsidianAimSpeed(), 0.01f, 1.0f, 
            fieldObsidianAimSpeed, mouseX, mouseY, "aimSpeed");
        y += 26;
        
        // Dönüş Aim Hızı - Slider + Field
        drawString(fontRendererObj, "§7Dönüş Aim:", labelX, y + 3, TEXT_GRAY);
        drawSliderWithField(fieldX, y, 100, config.getObsidianTurnSpeed(), 0.01f, 1.0f, 
            fieldObsidianTurnSpeed, mouseX, mouseY, "turnSpeed");
        
        y += 30;
        // Açıklama
        drawString(fontRendererObj, "§8Açıklama:", labelX, y, TEXT_DARK);
        y += 12;
        drawString(fontRendererObj, "§8Normal Aim: Mining sırasında aim hızı", labelX, y, TEXT_DARK);
        y += 12;
        drawString(fontRendererObj, "§8Dönüş Aim: Hedef noktaya varınca dönüş hızı", labelX, y, TEXT_DARK);
        y += 12;
        drawString(fontRendererObj, "§8(0.01=yavaş, 1.0=anında)", labelX, y, TEXT_DARK);
        
        y += 25;
        // === SELL AYARLARI ===
        drawString(fontRendererObj, "§d§lEnvanter Satış Ayarları", labelX, y, ACCENT_PURPLE);
        y += 20;
        
        // Sell Enabled Toggle
        drawString(fontRendererObj, "§7Otomatik Satış:", labelX, y + 3, TEXT_GRAY);
        String sellText = obsidianSellEnabled ? "§a✓ Açık" : "§c✗ Kapalı";
        int sellBtnColor = obsidianSellEnabled ? 0xFF2D5A27 : 0xFF5A2727;
        drawRect(fieldX, y, fieldX + 60, y + 16, sellBtnColor);
        drawCenteredString(fontRendererObj, sellText, fieldX + 30, y + 4, TEXT_WHITE);
        
        y += 24;
        // Sell Command
        drawString(fontRendererObj, "§7Satış Komutu:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldObsidianSellCommand, fieldX, y);
        fieldObsidianSellCommand.xPosition = fieldX;
        fieldObsidianSellCommand.yPosition = y;
        fieldObsidianSellCommand.drawTextBox();
        
        y += 24;
        // Sell Delay
        drawString(fontRendererObj, "§7Bekleme Süresi:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldObsidianSellDelay, fieldX, y);
        fieldObsidianSellDelay.xPosition = fieldX;
        fieldObsidianSellDelay.yPosition = y;
        fieldObsidianSellDelay.drawTextBox();
        drawString(fontRendererObj, "§8saniye", fieldX + 55, y + 3, TEXT_DARK);
        
        y += 28;
        // Target Distance Settings - Başlık
        drawString(fontRendererObj, "§e§lHedef Mesafe Ayarları", labelX, y + 3, TEXT_WHITE);
        
        y += 20;
        // Min Offset (İlk Blok)
        drawString(fontRendererObj, "§7İlk Blok Offset:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldObsidianTargetMin, fieldX, y);
        fieldObsidianTargetMin.xPosition = fieldX;
        fieldObsidianTargetMin.yPosition = y;
        fieldObsidianTargetMin.drawTextBox();
        drawString(fontRendererObj, "§8(son obsidyene yakınlık)", fieldX + 45, y + 3, TEXT_DARK);
        
        y += 24;
        // Max Offset (Son Blok)
        drawString(fontRendererObj, "§7Son Blok Offset:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldObsidianTargetMax, fieldX, y);
        fieldObsidianTargetMax.xPosition = fieldX;
        fieldObsidianTargetMax.yPosition = y;
        fieldObsidianTargetMax.drawTextBox();
        drawString(fontRendererObj, "§8(son obsidyene uzaklık)", fieldX + 45, y + 3, TEXT_DARK);
    }
    
    /**
     * Config Import/Export sekmesi
     */
    private void drawSettingsDuel(int mouseX, int mouseY, ModConfig config, int y, int labelX, int fieldX) {
        DuelAnalyzerState duelState = DuelAnalyzerState.getInstance();
        boolean isAnalyzing = duelState != null && duelState.isAnalyzing();
        
        // Başlık
        drawString(fontRendererObj, "§e§lDuel Analyzer", labelX, y, ACCENT_YELLOW);
        y += 24;
        
        // Durum
        String statusText = isAnalyzing ? "§aAnaliz Aktif" : "§7Pasif";
        drawString(fontRendererObj, "Durum: " + statusText, labelX, y, TEXT_WHITE);
        y += 20;
        
        // Eğer analiz aktifse, durumu göster
        if (isAnalyzing && duelState.getSession() != null) {
            drawString(fontRendererObj, "§7Süre: §f" + duelState.getSession().getSessionDurationFormatted(), labelX, y, TEXT_GRAY);
            y += 30;
            
            // Durdur butonu
            int stopBtnX = labelX;
            int stopBtnY = y;
            int btnWidth = 120;
            int btnHeight = 22;
            
            boolean hoverStop = mouseX >= stopBtnX && mouseX < stopBtnX + btnWidth && 
                               mouseY >= stopBtnY && mouseY < stopBtnY + btnHeight;
            drawRect(stopBtnX, stopBtnY, stopBtnX + btnWidth, stopBtnY + btnHeight, hoverStop ? ACCENT_RED : 0xFF8B0000);
            drawCenteredString(fontRendererObj, "§cAnalizi Durdur", stopBtnX + btnWidth/2, stopBtnY + 7, TEXT_WHITE);
            
            y += 40;
        } else {
            // Yeni analiz başlatma
            drawString(fontRendererObj, "§7Oyuncu 1:", labelX, y + 3, TEXT_GRAY);
            drawFieldBackground(fieldDuelPlayer1, fieldX, y);
            fieldDuelPlayer1.xPosition = fieldX;
            fieldDuelPlayer1.yPosition = y;
            fieldDuelPlayer1.drawTextBox();
            
            y += 28;
            drawString(fontRendererObj, "§7Oyuncu 2:", labelX, y + 3, TEXT_GRAY);
            drawFieldBackground(fieldDuelPlayer2, fieldX, y);
            fieldDuelPlayer2.xPosition = fieldX;
            fieldDuelPlayer2.yPosition = y;
            fieldDuelPlayer2.drawTextBox();
            
            y += 35;
            
            // Başlat butonu
            int startBtnX = labelX;
            int startBtnY = y;
            int btnWidth = 140;
            int btnHeight = 22;
            
            boolean canStart = fieldDuelPlayer1.getText().length() > 0 && fieldDuelPlayer2.getText().length() > 0;
            boolean hoverStart = mouseX >= startBtnX && mouseX < startBtnX + btnWidth && 
                                mouseY >= startBtnY && mouseY < startBtnY + btnHeight;
            int startColor = canStart ? (hoverStart ? ACCENT_GREEN : 0xFF2E7D32) : BG_BUTTON;
            
            drawRect(startBtnX, startBtnY, startBtnX + btnWidth, startBtnY + btnHeight, startColor);
            String startText = canStart ? "§a▶ Analizi Başlat" : "§8▶ Analizi Başlat";
            drawCenteredString(fontRendererObj, startText, startBtnX + btnWidth/2, startBtnY + 7, TEXT_WHITE);
            
            y += 35;
        }
        
        // HUD Ayarları
        drawRect(labelX, y, guiX + GUI_WIDTH - 25, y + 1, 0xFF444444);
        y += 15;
        
        drawString(fontRendererObj, "§b§lHUD Ayarları", labelX, y, ACCENT_CYAN);
        y += 20;
        
        // HUD toggle
        boolean hudEnabled = config.isDuelHudEnabled();
        drawToggle(labelX, y, "HUD Göster", hudEnabled, mouseX, mouseY);
        y += 25;
        
        // HUD pozisyonu
        drawString(fontRendererObj, "§7HUD Pozisyonu: §f" + config.getDuelHudX() + ", " + config.getDuelHudY(), labelX, y, TEXT_GRAY);
        y += 14;
        drawString(fontRendererObj, "§8(HUD'u sürükleyerek değiştirebilirsin)", labelX, y, TEXT_DARK);
        y += 30;
        
        // Bilgi
        drawRect(labelX, y, guiX + GUI_WIDTH - 25, y + 1, 0xFF444444);
        y += 15;
        
        drawString(fontRendererObj, "§7Özellikler:", labelX, y, TEXT_GRAY);
        y += 14;
        drawString(fontRendererObj, "§8• Hit sayısı takibi", labelX + 10, y, TEXT_DARK);
        y += 12;
        drawString(fontRendererObj, "§8• Altın/Encli elma tüketimi", labelX + 10, y, TEXT_DARK);
        y += 12;
        drawString(fontRendererObj, "§8• Kırılan zırh takibi", labelX + 10, y, TEXT_DARK);
        y += 12;
        drawString(fontRendererObj, "§8• Kılıç bilgisi (lore dahil)", labelX + 10, y, TEXT_DARK);
        y += 12;
        drawString(fontRendererObj, "§8• Ölümde JSON kayıt", labelX + 10, y, TEXT_DARK);
    }

    private void drawSettingsConfig(int mouseX, int mouseY, int y, int labelX, int fieldX) {
        String playerName = MuzMod.instance.getCurrentPlayerName();
        
        // Başlık
        drawString(fontRendererObj, "§6§lConfig Yönetimi", labelX, y, ACCENT_YELLOW);
        y += 24;
        
        // Mevcut hesap bilgisi
        drawString(fontRendererObj, "§7Aktif Hesap:", labelX, y + 3, TEXT_GRAY);
        String displayName = playerName != null ? "§a" + playerName : "§cBağlı değil";
        drawString(fontRendererObj, displayName, fieldX, y + 3, TEXT_WHITE);
        y += 20;
        
        // Config dosyası bilgisi
        drawString(fontRendererObj, "§7Config Dosyası:", labelX, y + 3, TEXT_GRAY);
        String configName = playerName != null ? "§fconfig_" + playerName + ".cfg" : "§8-";
        drawString(fontRendererObj, configName, fieldX, y + 3, TEXT_WHITE);
        y += 30;
        
        // Ana Divider
        drawRect(labelX, y, guiX + GUI_WIDTH - 25, y + 1, 0xFF444444);
        y += 10;
        
        // Main Config başlık
        drawString(fontRendererObj, "§e§lMain Config (Şablon)", labelX, y, ACCENT_YELLOW);
        y += 20;
        
        // Açıklama
        drawString(fontRendererObj, "§8Main Config: Tüm hesaplar için ortak şablon", labelX, y, TEXT_DARK);
        y += 14;
        drawString(fontRendererObj, "§8config.cfg dosyasıdır.", labelX, y, TEXT_DARK);
        y += 24;
        
        // Import butonu
        int btnWidth = 140;
        int btnHeight = 22;
        int importBtnX = labelX;
        int importBtnY = y;
        
        boolean hoverImport = mouseX >= importBtnX && mouseX < importBtnX + btnWidth && 
                              mouseY >= importBtnY && mouseY < importBtnY + btnHeight;
        int importColor = hoverImport ? 0xFF3D7A37 : 0xFF2D5A27;
        
        drawRect(importBtnX, importBtnY, importBtnX + btnWidth, importBtnY + btnHeight, importColor);
        drawRect(importBtnX, importBtnY, importBtnX + btnWidth, importBtnY + 1, 0xFF4D8A47);
        drawCenteredString(fontRendererObj, "§a⬇ Main'den Import", importBtnX + btnWidth/2, importBtnY + 7, TEXT_WHITE);
        
        y += 28;
        drawString(fontRendererObj, "§8Main Config'i bu hesaba uygula", labelX, y, TEXT_DARK);
        y += 24;
        
        // Export butonu
        int exportBtnX = labelX;
        int exportBtnY = y;
        
        boolean hoverExport = mouseX >= exportBtnX && mouseX < exportBtnX + btnWidth && 
                              mouseY >= exportBtnY && mouseY < exportBtnY + btnHeight;
        int exportColor = hoverExport ? 0xFF7A5A37 : 0xFF5A4027;
        
        drawRect(exportBtnX, exportBtnY, exportBtnX + btnWidth, exportBtnY + btnHeight, exportColor);
        drawRect(exportBtnX, exportBtnY, exportBtnX + btnWidth, exportBtnY + 1, 0xFF8A6A47);
        drawCenteredString(fontRendererObj, "§6⬆ Main'e Export", exportBtnX + btnWidth/2, exportBtnY + 7, TEXT_WHITE);
        
        y += 28;
        drawString(fontRendererObj, "§8Bu hesabın ayarlarını Main'e kaydet", labelX, y, TEXT_DARK);
        y += 30;
        
        // Divider
        drawRect(labelX, y, guiX + GUI_WIDTH - 25, y + 1, 0xFF444444);
        y += 15;
        
        // Bilgi
        drawString(fontRendererObj, "§7Nasıl Kullanılır:", labelX, y, TEXT_GRAY);
        y += 16;
        drawString(fontRendererObj, "§81. Bir hesapta ayarları yap", labelX + 10, y, TEXT_DARK);
        y += 12;
        drawString(fontRendererObj, "§82. Export ile Main'e kaydet", labelX + 10, y, TEXT_DARK);
        y += 12;
        drawString(fontRendererObj, "§83. Diğer hesaplarda Import yap", labelX + 10, y, TEXT_DARK);
    }

    /**
     * Slider + TextField kombinasyonu çizer
     */
    private void drawSliderWithField(int x, int y, int sliderWidth, float currentValue, float min, float max, 
            GuiTextField field, int mouseX, int mouseY, String sliderId) {
        // Slider background
        drawRect(x, y + 2, x + sliderWidth, y + 14, 0xFF333333);
        drawRect(x + 1, y + 3, x + sliderWidth - 1, y + 13, 0xFF1A1A1A);
        
        // Slider fill
        float percent = (currentValue - min) / (max - min);
        int fillWidth = (int) ((sliderWidth - 2) * percent);
        drawRect(x + 1, y + 3, x + 1 + fillWidth, y + 13, 0xFF9944FF);
        
        // Slider handle
        int handleX = x + 1 + fillWidth - 3;
        drawRect(handleX, y + 1, handleX + 6, y + 15, 0xFFFFFFFF);
        
        // Field (sağ tarafta)
        int fieldX = x + sliderWidth + 10;
        drawFieldBackground(field, fieldX, y);
        field.xPosition = fieldX;
        field.yPosition = y;
        field.drawTextBox();
    }
    
    private int getDirectionIndex(float yaw) {
        // Normalize yaw to 0-360
        while (yaw < 0) yaw += 360;
        while (yaw >= 360) yaw -= 360;
        
        // 0=South, 90=West, 180=North, 270=East
        if (yaw >= 135 && yaw < 225) return 0; // North (180)
        if (yaw >= 225 && yaw < 315) return 1; // East (270/-90)
        if (yaw >= 315 || yaw < 45) return 2;  // South (0)
        return 3; // West (90)
    }
    
    private void drawFieldBackground(GuiTextField field, int x, int y) {
        drawRect(x - 3, y - 2, x + field.width + 3, y + 15, BG_FIELD);
        if (field.isFocused()) {
            drawRect(x - 3, y + 14, x + field.width + 3, y + 15, ACCENT_CYAN);
        }
    }
    
    private void drawButton(int x, int y, int w, int h, String text, int accentColor, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        int bg = hovered ? BG_BUTTON_HOVER : BG_BUTTON;
        drawRect(x, y, x + w, y + h, bg);
        drawRect(x, y + h - 2, x + w, y + h, accentColor);
        int textX = x + (w - fontRendererObj.getStringWidth(text)) / 2;
        int textY = y + (h - 8) / 2;
        drawString(fontRendererObj, text, textX, textY, TEXT_WHITE);
    }
    
    private void drawToggle(int x, int y, String label, boolean enabled, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + 100 && mouseY >= y && mouseY < y + 15;
        String text = (enabled ? "§a+ " : "§c- ") + label;
        if (hovered) {
            drawRect(x - 2, y - 2, x + 100, y + 12, BG_BUTTON_HOVER);
        }
        drawString(fontRendererObj, text, x, y, enabled ? ACCENT_GREEN : TEXT_GRAY);
    }
    
    private void drawGradientRectH(int left, int top, int right, int bottom, int colorLeft, int colorRight) {
        int w = right - left;
        for (int i = 0; i < w; i++) {
            float ratio = (float) i / w;
            int color = lerpColor(colorLeft, colorRight, ratio);
            drawRect(left + i, top, left + i + 1, bottom, color);
        }
    }
    
    private int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF, a2 = (c2 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF, r2 = (c2 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF, g2 = (c2 >> 8) & 0xFF;
        int b1 = c1 & 0xFF, b2 = c2 & 0xFF;
        return ((int)(a1 + (a2-a1)*t) << 24) | ((int)(r1 + (r2-r1)*t) << 16) | 
               ((int)(g1 + (g2-g1)*t) << 8) | (int)(b1 + (b2-b1)*t);
    }
    
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 3) + "...";
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        // Tab clicks
        int tabWidth = GUI_WIDTH / tabNames.length;
        int tabY = guiY + 38;
        for (int i = 0; i < tabNames.length; i++) {
            int tabX = guiX + i * tabWidth;
            if (mouseX >= tabX && mouseX < tabX + tabWidth && mouseY >= tabY && mouseY < tabY + 26) {
                currentTab = i;
                addingNewEntry = false;
                selectedEntryId = -1;
                return;
            }
        }
        
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        ModConfig config = MuzMod.instance.getConfig();
        
        // General tab
        if (currentTab == 0) {
            int y = guiY + 75;
            
            // Bot toggle
            if (isInside(mouseX, mouseY, guiX + 20, y, 140, 26)) {
                MuzMod.instance.toggleBot();
            }
            // Schedule toggle
            else if (isInside(mouseX, mouseY, guiX + 170, y, 140, 26)) {
                schedule.setScheduleEnabled(!schedule.isScheduleEnabled());
                MuzMod.instance.getStateManager().setUseScheduleBasedTransition(schedule.isScheduleEnabled());
            }
            // Auto AFK toggle
            else if (isInside(mouseX, mouseY, guiX + 320, y, 110, 26)) {
                schedule.setAutoAfkWhenIdle(!schedule.isAutoAfkWhenIdle());
            }
            
            // HUD toggle
            y += 30;
            if (isInside(mouseX, mouseY, guiX + 20, y, 120, 22)) {
                config.setShowOverlay(!config.isShowOverlay());
            }
            
            // State buttons - "Manuel Durum:" label'dan sonra y += 14 var
            y += 30; // Label satırı
            y += 14; // Butonlar label'dan 14 piksel aşağıda
            if (isInside(mouseX, mouseY, guiX + 20, y, 55, 22)) {
                MuzMod.instance.getStateManager().forceState("idle");
            } else if (isInside(mouseX, mouseY, guiX + 80, y, 55, 22)) {
                MuzMod.instance.getStateManager().forceState("mining");
            } else if (isInside(mouseX, mouseY, guiX + 140, y, 55, 22)) {
                MuzMod.instance.getStateManager().forceState("afk");
            } else if (isInside(mouseX, mouseY, guiX + 200, y, 55, 22)) {
                MuzMod.instance.getStateManager().forceState("repair");
            } else if (isInside(mouseX, mouseY, guiX + 260, y, 55, 22)) {
                MuzMod.instance.getStateManager().forceState("ox");
            } else if (isInside(mouseX, mouseY, guiX + 320, y, 55, 22)) {
                MuzMod.instance.getStateManager().forceState("obsidian");
            }
        }
        
        // Schedule tab
        if (currentTab == 1) {
            // Day selector
            int dayY = guiY + 70;
            int dayWidth = 55;
            for (int i = 0; i < 7; i++) {
                int dx = guiX + 15 + i * dayWidth;
                if (mouseX >= dx && mouseX < dx + dayWidth - 2 && mouseY >= dayY && mouseY < dayY + 20) {
                    selectedDay = i;
                    selectedEntryId = -1;
                    scheduleScrollOffset = 0;
                    return;
                }
            }
            
            // Entry list clicks
            int listX = guiX + 15;
            int listY = guiY + 100;
            int listW = 220;
            int itemH = 28;
            int itemY = listY + 22;
            
            List<ScheduleEntry> entries = schedule.getEntriesForDay(selectedDay);
            for (int i = scheduleScrollOffset; i < Math.min(entries.size(), scheduleScrollOffset + 4); i++) {
                if (mouseX >= listX + 3 && mouseX < listX + listW - 3 && mouseY >= itemY && mouseY < itemY + itemH - 2) {
                    ScheduleEntry entry = entries.get(i);
                    selectedEntryId = entry.getId();
                    addingNewEntry = false;
                    
                    // Load entry data to fields
                    fieldStartTime.setText(String.format("%02d:%02d", entry.getStartHour(), entry.getStartMinute()));
                    fieldEndTime.setText(String.format("%02d:%02d", entry.getEndHour(), entry.getEndMinute()));
                    fieldWarpCommand.setText(entry.getCustomWarpCommand() != null ? entry.getCustomWarpCommand() : "");
                    newEntryType = entry.getEventType().ordinal();
                    return;
                }
                itemY += itemH;
            }
            
            // Right panel buttons
            int panelX = guiX + 245;
            int panelY = guiY + 100;
            
            if (addingNewEntry || selectedEntryId != -1) {
                // Type selector
                for (int i = 0; i < 4; i++) {
                    int tx = panelX + 8 + i * 45;
                    if (mouseX >= tx && mouseX < tx + 42 && mouseY >= panelY + 65 && mouseY < panelY + 82) {
                        newEntryType = i;
                        return;
                    }
                }
                
                // Action buttons
                if (addingNewEntry) {
                    // Add button
                    if (isInside(mouseX, mouseY, panelX + 8, panelY + 128, 80, 20)) {
                        addNewEntry();
                    }
                    // Cancel button
                    else if (isInside(mouseX, mouseY, panelX + 100, panelY + 128, 80, 20)) {
                        addingNewEntry = false;
                    }
                } else {
                    // Save button
                    if (isInside(mouseX, mouseY, panelX + 8, panelY + 128, 55, 20)) {
                        saveSelectedEntry();
                    }
                    // Delete button
                    else if (isInside(mouseX, mouseY, panelX + 68, panelY + 128, 55, 20)) {
                        schedule.removeEntry(selectedEntryId);
                        selectedEntryId = -1;
                    }
                    // Cancel button
                    else if (isInside(mouseX, mouseY, panelX + 128, panelY + 128, 55, 20)) {
                        selectedEntryId = -1;
                    }
                }
                
                // Field clicks
                fieldStartTime.mouseClicked(mouseX, mouseY, mouseButton);
                fieldEndTime.mouseClicked(mouseX, mouseY, mouseButton);
                fieldWarpCommand.mouseClicked(mouseX, mouseY, mouseButton);
            } else {
                // New entry button
                if (isInside(mouseX, mouseY, panelX + 40, panelY + 60, 110, 28)) {
                    addingNewEntry = true;
                    fieldStartTime.setText("10:00");
                    fieldEndTime.setText("12:00");
                    fieldWarpCommand.setText("");
                    newEntryType = 0;
                }
                // Copy to weekdays
                else if (isInside(mouseX, mouseY, panelX + 8, panelY + 120, 85, 18)) {
                    if (selectedDay >= 0 && selectedDay <= 4) {
                        // Seçili gün zaten hafta içi - bu günü diğer hafta içi günlerine kopyala
                        schedule.applyToWeekdays(selectedDay);
                    } else {
                        // Hafta sonu seçili - hafta sonundan hafta içine kopyala
                        schedule.applyToWeekdays(selectedDay);
                    }
                    MuzMod.LOGGER.info("[GUI] Hafta içine kopyalandı: kaynak gün=" + selectedDay);
                }
                // Copy to weekends
                else if (isInside(mouseX, mouseY, panelX + 98, panelY + 120, 85, 18)) {
                    if (selectedDay >= 5 && selectedDay <= 6) {
                        // Seçili gün zaten hafta sonu - bu günü diğer hafta sonu gününe kopyala
                        schedule.applyToWeekends(selectedDay);
                    } else {
                        // Hafta içi seçili - hafta içinden hafta sonuna kopyala
                        schedule.applyToWeekends(selectedDay);
                    }
                    MuzMod.LOGGER.info("[GUI] Hafta sonuna kopyalandı: kaynak gün=" + selectedDay);
                }
            }
        }
        
        // Settings tab
        if (currentTab == 2) {
            // Sub-tab clicks
            int subTabY = guiY + 68;
            int subTabWidth = 100;
            for (int i = 0; i < settingsSubTabs.length; i++) {
                int sx = guiX + 15 + i * subTabWidth;
                if (mouseX >= sx && mouseX < sx + subTabWidth - 2 && mouseY >= subTabY && mouseY < subTabY + 18) {
                    settingsSubTab = i;
                    return;
                }
            }
            
            int y = guiY + 95;
            int labelX = guiX + 20;
            int fieldX = guiX + 140;
            
            // Handle clicks based on sub-tab
            if (settingsSubTab == 0) {
                // Genel toggles (after fields)
                int toggleY = y + 26 * 6 + 4;
                if (isInside(mouseX, mouseY, labelX - 2, toggleY - 2, 102, 14)) {
                    config.setBlockLockEnabled(!config.isBlockLockEnabled());
                } else if (isInside(mouseX, mouseY, labelX + 118, toggleY - 2, 102, 14)) {
                    config.setInstantFlee(!config.isInstantFlee());
                } else if (isInside(mouseX, mouseY, labelX + 238, toggleY - 2, 102, 14)) {
                    config.setStrafeEnabled(!config.isStrafeEnabled());
                }
                
                // Field clicks
                fieldDefaultMiningWarp.mouseClicked(mouseX, mouseY, mouseButton);
                fieldDefaultAfkWarp.mouseClicked(mouseX, mouseY, mouseButton);
                fieldRepairThreshold.mouseClicked(mouseX, mouseY, mouseButton);
                fieldTimeOffset.mouseClicked(mouseX, mouseY, mouseButton);
                fieldDetectRadius.mouseClicked(mouseX, mouseY, mouseButton);
                fieldRepairClickDelay.mouseClicked(mouseX, mouseY, mouseButton);
            }
            else if (settingsSubTab == 1) {
                // Mining sub-tab - scroll offset'i hesaba kat
                // Y pozisyonları scroll offset'e göre ayarlanır
                int scrollY = y - miningSettingsScrollOffset;
                
                // İkinci Yürüyüş Aktif toggle: scrollY + 16 + 22 + 26 + 16 + 26 + 16 = scrollY + 122
                int secondToggleY = scrollY + 122;
                if (isInside(mouseX, mouseY, labelX - 2, secondToggleY - 2, 70, 14)) {
                    config.setSecondWalkEnabled(!config.isSecondWalkEnabled());
                }
                
                // Rastgele Yön toggle: secondToggleY + 18 + 22 + 22 = scrollY + 184
                int randomDirY = scrollY + 184;
                if (isInside(mouseX, mouseY, labelX - 2, randomDirY - 2, 110, 14)) {
                    config.setSecondWalkRandomDirection(!config.isSecondWalkRandomDirection());
                }
                
                // Strafe Aktif toggle: randomDirY + 26 + 16 = scrollY + 226
                int strafeToggleY = scrollY + 226;
                if (isInside(mouseX, mouseY, labelX - 2, strafeToggleY - 2, 70, 14)) {
                    config.setStrafeEnabled(!config.isStrafeEnabled());
                }
                
                // Field clicks - her field kendi pozisyonunda, drawSettingsMining'de güncelleniyor
                fieldInitialWalkMin.mouseClicked(mouseX, mouseY, mouseButton);
                fieldInitialWalkMax.mouseClicked(mouseX, mouseY, mouseButton);
                fieldWalkYawVar.mouseClicked(mouseX, mouseY, mouseButton);
                fieldMaxDistFromCenter.mouseClicked(mouseX, mouseY, mouseButton);
                fieldSecondWalkMin.mouseClicked(mouseX, mouseY, mouseButton);
                fieldSecondWalkMax.mouseClicked(mouseX, mouseY, mouseButton);
                fieldSecondWalkAngle.mouseClicked(mouseX, mouseY, mouseButton);
                fieldStrafeInterval.mouseClicked(mouseX, mouseY, mouseButton);
                fieldStrafeDuration.mouseClicked(mouseX, mouseY, mouseButton);
                fieldMiningJitterYaw.mouseClicked(mouseX, mouseY, mouseButton);
                fieldMiningJitterPitch.mouseClicked(mouseX, mouseY, mouseButton);
                fieldMiningJitterInterval.mouseClicked(mouseX, mouseY, mouseButton);
            }
            else if (settingsSubTab == 2) {
                // OX sub-tab
                int oxDirY = y + 20;
                float[] yawValues = {180f, -90f, 0f, 90f}; // Kuzey, Doğu, Güney, Batı
                
                // Yeşil yön butonları
                for (int i = 0; i < 4; i++) {
                    int bx = fieldX + i * 50;
                    if (mouseX >= bx && mouseX < bx + 48 && mouseY >= oxDirY && mouseY < oxDirY + 18) {
                        config.setOxLimeYaw(yawValues[i]);
                    }
                }
                
                // Kırmızı yön butonları
                oxDirY += 28;
                for (int i = 0; i < 4; i++) {
                    int bx = fieldX + i * 50;
                    if (mouseX >= bx && mouseX < bx + 48 && mouseY >= oxDirY && mouseY < oxDirY + 18) {
                        config.setOxRedYaw(yawValues[i]);
                    }
                }
                
                // OX field click
                fieldOxMinPlayers.mouseClicked(mouseX, mouseY, mouseButton);
            }
            
            // Obsidyen settings clicks
            if (settingsSubTab == 3) {
                // Field clicks (jitter + aim hızı + sell + target offset)
                fieldObsidianJitterYaw.mouseClicked(mouseX, mouseY, mouseButton);
                fieldObsidianJitterPitch.mouseClicked(mouseX, mouseY, mouseButton);
                fieldObsidianJitterInterval.mouseClicked(mouseX, mouseY, mouseButton);
                fieldObsidianAimSpeed.mouseClicked(mouseX, mouseY, mouseButton);
                fieldObsidianTurnSpeed.mouseClicked(mouseX, mouseY, mouseButton);
                fieldObsidianSellCommand.mouseClicked(mouseX, mouseY, mouseButton);
                fieldObsidianSellDelay.mouseClicked(mouseX, mouseY, mouseButton);
                fieldObsidianTargetMin.mouseClicked(mouseX, mouseY, mouseButton);
                fieldObsidianTargetMax.mouseClicked(mouseX, mouseY, mouseButton);
                
                // Sell toggle click - pozisyonu hesapla (scroll offset dahil)
                // Draw'daki sıraya göre hesapla
                int contentY = guiY + 95 - obsidianSettingsScrollOffset;
                int sellToggleY = contentY + 20 + 26 + 30 + 20 + 26 + 30 + 12 + 12 + 12 + 25 + 20; // draw sırasına göre
                int sellFieldX = guiX + 160;
                
                // Sell enabled toggle butonu
                if (mouseX >= sellFieldX && mouseX < sellFieldX + 60 && mouseY >= sellToggleY && mouseY < sellToggleY + 16) {
                    obsidianSellEnabled = !obsidianSellEnabled;
                    config.setObsidianSellEnabled(obsidianSellEnabled);
                }
                
                // Slider clicks - doğrudan slider alanı kontrol (fieldObsidian... pozisyonlarından hesapla)
                // Slider fieldX'in solunda, field sağında
                int sliderX = fieldObsidianAimSpeed.xPosition - 110; // slider width + gap
                
                // Normal Aim slider
                int aimSliderY = fieldObsidianAimSpeed.yPosition;
                if (isInside(mouseX, mouseY, sliderX, aimSliderY, 100, 16)) {
                    draggingAimSpeed = true;
                    updateSliderValue(mouseX, sliderX, 100, 0.01f, 1.0f, "aimSpeed");
                }
                
                // Turn Aim slider
                int turnSliderY = fieldObsidianTurnSpeed.yPosition;
                if (isInside(mouseX, mouseY, sliderX, turnSliderY, 100, 16)) {
                    draggingTurnSpeed = true;
                    updateSliderValue(mouseX, sliderX, 100, 0.01f, 1.0f, "turnSpeed");
                }
            }
            // Duel sub-tab
            else if (settingsSubTab == 4) {
                StateManager sm = MuzMod.instance.getStateManager();
                DuelAnalyzerState duelState = sm.getDuelAnalyzerState();
                
                int btnX = guiX + 25;
                int btnWidth = 120;
                int btnHeight = 20;
                
                if (duelState.isAnalyzing()) {
                    // Stop Analysis butonu - y pozisyonu: 95 + 25(başlık) + 25(durum) + 16(süre) + 8
                    int stopBtnY = guiY + 95 + 25 + 25 + 16 + 8;
                    if (mouseX >= btnX && mouseX < btnX + btnWidth && 
                        mouseY >= stopBtnY && mouseY < stopBtnY + btnHeight) {
                        duelState.stopAnalysis();
                    }
                } else {
                    // Start Analysis butonu - y pozisyonu: 95 + 25(başlık) + 25(durum) + 25(player1) + 25(player2) + 8
                    int startBtnY = guiY + 95 + 25 + 25 + 25 + 25 + 8;
                    if (mouseX >= btnX && mouseX < btnX + btnWidth && 
                        mouseY >= startBtnY && mouseY < startBtnY + btnHeight) {
                        String p1 = fieldDuelPlayer1.getText().trim();
                        String p2 = fieldDuelPlayer2.getText().trim();
                        if (!p1.isEmpty() && !p2.isEmpty()) {
                            duelState.startAnalysis(p1, p2);
                        }
                    }
                }
                
                // HUD Toggle - y pozisyonu hesabı
                int hudSectionY = guiY + 95 + 25 + 25 + (duelState.isAnalyzing() ? 16 + 8 + 25 : 25 + 25 + 8 + 25) + 30;
                int hudToggleBtnY = hudSectionY + 25;
                if (mouseX >= btnX && mouseX < btnX + 80 && 
                    mouseY >= hudToggleBtnY && mouseY < hudToggleBtnY + btnHeight) {
                    config.setDuelHudEnabled(!config.isDuelHudEnabled());
                }
            }
            // Config sub-tab
            else if (settingsSubTab == 5) {
                int cfgLabelX = guiX + 25;
                int btnWidth = 140;
                int btnHeight = 22;
                
                // Import butonu pozisyonu (y hesabı: başlık+24, hesap+20, config+30, divider+10, main başlık+20, açıklama1+14, açıklama2+24)
                int importBtnY = guiY + 95 + 24 + 20 + 30 + 10 + 20 + 14 + 24;
                
                if (mouseX >= cfgLabelX && mouseX < cfgLabelX + btnWidth && 
                    mouseY >= importBtnY && mouseY < importBtnY + btnHeight) {
                    // Import from Main Config
                    importFromMainConfig();
                }
                
                // Export butonu pozisyonu (import + 28 + açıklama + 24)
                int exportBtnY = importBtnY + 28 + 24;
                
                if (mouseX >= cfgLabelX && mouseX < cfgLabelX + btnWidth && 
                    mouseY >= exportBtnY && mouseY < exportBtnY + btnHeight) {
                    // Export to Main Config
                    exportToMainConfig();
                }
            }
        }
        
        // Save/Close buttons
        if (isInside(mouseX, mouseY, guiX + GUI_WIDTH - 160, guiY + GUI_HEIGHT - 28, 70, 20)) {
            saveAllSettings();
        } else if (isInside(mouseX, mouseY, guiX + GUI_WIDTH - 80, guiY + GUI_HEIGHT - 28, 70, 20)) {
            mc.displayGuiScreen(null);
        }
    }
    
    private void addNewEntry() {
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        
        int[] start = parseTime(fieldStartTime.getText());
        int[] end = parseTime(fieldEndTime.getText());
        
        if (start != null && end != null) {
            ScheduleEntry.EventType type = ScheduleEntry.EventType.values()[newEntryType];
            ScheduleEntry entry = new ScheduleEntry(selectedDay, start[0], start[1], end[0], end[1], type);
            entry.setCustomWarpCommand(fieldWarpCommand.getText());
            schedule.addEntry(entry);
            addingNewEntry = false;
        }
    }
    
    private void saveSelectedEntry() {
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        ScheduleEntry entry = schedule.getEntryById(selectedEntryId);
        
        if (entry != null) {
            int[] start = parseTime(fieldStartTime.getText());
            int[] end = parseTime(fieldEndTime.getText());
            
            if (start != null && end != null) {
                entry.setStartHour(start[0]);
                entry.setStartMinute(start[1]);
                entry.setEndHour(end[0]);
                entry.setEndMinute(end[1]);
                entry.setEventType(ScheduleEntry.EventType.values()[newEntryType]);
                entry.setCustomWarpCommand(fieldWarpCommand.getText());
                schedule.updateEntry(entry);
            }
        }
        selectedEntryId = -1;
    }
    
    private int[] parseTime(String text) {
        try {
            String[] parts = text.split(":");
            if (parts.length == 2) {
                int h = Integer.parseInt(parts[0].trim());
                int m = Integer.parseInt(parts[1].trim());
                if (h >= 0 && h < 24 && m >= 0 && m < 60) {
                    return new int[]{h, m};
                }
            }
        } catch (Exception e) {}
        return null;
    }
    
    /**
     * Main Config'den (config.cfg) mevcut hesaba import et
     */
    private void importFromMainConfig() {
        try {
            File clientDir = MuzMod.instance.getClientDir();
            File mainConfigFile = new File(clientDir, "config.cfg");
            
            if (!mainConfigFile.exists()) {
                MuzMod.LOGGER.warn("[Config] Main config dosyası bulunamadı!");
                return;
            }
            
            String playerName = MuzMod.instance.getCurrentPlayerName();
            if (playerName == null) {
                MuzMod.LOGGER.warn("[Config] Aktif oyuncu yok!");
                return;
            }
            
            // Main config'i oku
            File playerConfigFile = new File(clientDir, "config_" + playerName + ".cfg");
            
            // Dosyayı kopyala
            java.nio.file.Files.copy(
                mainConfigFile.toPath(), 
                playerConfigFile.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            
            // Config'i yeniden yükle (force reload)
            MuzMod.instance.loadConfigForPlayer(playerName, true);
            
            // GUI field'larını güncelle
            refreshFieldsFromConfig();
            
            MuzMod.LOGGER.info("[Config] Main config imported for " + playerName);
        } catch (Exception e) {
            MuzMod.LOGGER.error("[Config] Import error: " + e.getMessage());
        }
    }
    
    /**
     * Mevcut hesabın config'ini Main Config'e (config.cfg) export et
     */
    private void exportToMainConfig() {
        try {
            // Önce mevcut ayarları kaydet
            saveAllSettings();
            
            File clientDir = MuzMod.instance.getClientDir();
            String playerName = MuzMod.instance.getCurrentPlayerName();
            
            if (playerName == null) {
                MuzMod.LOGGER.warn("[Config] Aktif oyuncu yok!");
                return;
            }
            
            File playerConfigFile = new File(clientDir, "config_" + playerName + ".cfg");
            File mainConfigFile = new File(clientDir, "config.cfg");
            
            if (!playerConfigFile.exists()) {
                MuzMod.LOGGER.warn("[Config] Player config dosyası bulunamadı!");
                return;
            }
            
            // Dosyayı kopyala
            java.nio.file.Files.copy(
                playerConfigFile.toPath(), 
                mainConfigFile.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            
            MuzMod.LOGGER.info("[Config] Config exported to main for " + playerName);
        } catch (Exception e) {
            MuzMod.LOGGER.error("[Config] Export error: " + e.getMessage());
        }
    }
    
    /**
     * GUI field'larını config'den yeniden yükle
     */
    private void refreshFieldsFromConfig() {
        ModConfig config = MuzMod.instance.getConfig();
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        
        // Genel
        fieldDefaultMiningWarp.setText(schedule.getDefaultMiningWarp());
        fieldDefaultAfkWarp.setText(schedule.getDefaultAfkWarp());
        fieldRepairThreshold.setText(String.valueOf(config.getRepairDurabilityThreshold()));
        fieldTimeOffset.setText(String.valueOf(config.getTimeOffsetHours()));
        fieldDetectRadius.setText(String.valueOf(config.getPlayerDetectionRadius()));
        fieldRepairClickDelay.setText(String.valueOf(config.getRepairClickDelay()));
        
        // Mining
        fieldInitialWalkMin.setText(String.valueOf(config.getInitialWalkDistanceMin()));
        fieldInitialWalkMax.setText(String.valueOf(config.getInitialWalkDistanceMax()));
        fieldWalkYawVar.setText(String.valueOf(config.getWalkYawVariation()));
        fieldMaxDistFromCenter.setText(String.valueOf(config.getMaxDistanceFromCenter()));
        fieldSecondWalkMin.setText(String.valueOf(config.getSecondWalkDistanceMin()));
        fieldSecondWalkMax.setText(String.valueOf(config.getSecondWalkDistanceMax()));
        fieldSecondWalkAngle.setText(String.valueOf(config.getSecondWalkAngleVariation()));
        fieldStrafeInterval.setText(String.valueOf(config.getStrafeInterval() / 1000));
        fieldStrafeDuration.setText(String.valueOf(config.getStrafeDuration()));
        fieldMiningJitterYaw.setText(String.valueOf(config.getMiningJitterYaw()));
        fieldMiningJitterPitch.setText(String.valueOf(config.getMiningJitterPitch()));
        fieldMiningJitterInterval.setText(String.valueOf(config.getMiningJitterInterval()));
        
        // OX
        fieldOxMinPlayers.setText(String.valueOf(config.getOxMinPlayers()));
        
        // Obsidyen
        fieldObsidianJitterYaw.setText(String.valueOf(config.getObsidianJitterYaw()));
        fieldObsidianJitterPitch.setText(String.valueOf(config.getObsidianJitterPitch()));
        fieldObsidianJitterInterval.setText(String.valueOf(config.getObsidianJitterInterval()));
        fieldObsidianAimSpeed.setText(String.valueOf(config.getObsidianAimSpeed()));
        fieldObsidianTurnSpeed.setText(String.valueOf(config.getObsidianTurnSpeed()));
        fieldObsidianSellCommand.setText(config.getObsidianSellCommand());
        fieldObsidianSellDelay.setText(String.valueOf(config.getObsidianSellDelay() / 1000.0));
        fieldObsidianTargetMin.setText(String.valueOf(config.getObsidianTargetMinOffset()));
        fieldObsidianTargetMax.setText(String.valueOf(config.getObsidianTargetMaxOffset()));
        obsidianSellEnabled = config.isObsidianSellEnabled();
    }
    
    private void saveAllSettings() {
        ModConfig config = MuzMod.instance.getConfig();
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        
        try {
            // Genel ayarlar
            schedule.setDefaultMiningWarp(fieldDefaultMiningWarp.getText());
            schedule.setDefaultAfkWarp(fieldDefaultAfkWarp.getText());
            
            config.setRepairDurabilityThreshold(Integer.parseInt(fieldRepairThreshold.getText()));
            config.setTimeOffsetHours(Integer.parseInt(fieldTimeOffset.getText()));
            config.setPlayerDetectionRadius(Double.parseDouble(fieldDetectRadius.getText()));
            config.setRepairClickDelay(Float.parseFloat(fieldRepairClickDelay.getText()));
            
            // Mining ayarları
            config.setInitialWalkDistanceMin(Integer.parseInt(fieldInitialWalkMin.getText()));
            config.setInitialWalkDistanceMax(Integer.parseInt(fieldInitialWalkMax.getText()));
            config.setWalkYawVariation(Float.parseFloat(fieldWalkYawVar.getText()));
            config.setMaxDistanceFromCenter(Integer.parseInt(fieldMaxDistFromCenter.getText()));
            config.setSecondWalkDistanceMin(Integer.parseInt(fieldSecondWalkMin.getText()));
            config.setSecondWalkDistanceMax(Integer.parseInt(fieldSecondWalkMax.getText()));
            config.setSecondWalkAngleVariation(Float.parseFloat(fieldSecondWalkAngle.getText()));
            
            // Strafe ayarları
            config.setStrafeInterval(Long.parseLong(fieldStrafeInterval.getText()) * 1000);
            config.setStrafeDuration(Long.parseLong(fieldStrafeDuration.getText()));
            
            // Mining Jitter ayarları
            config.setMiningJitterYaw(Float.parseFloat(fieldMiningJitterYaw.getText()));
            config.setMiningJitterPitch(Float.parseFloat(fieldMiningJitterPitch.getText()));
            config.setMiningJitterInterval(Integer.parseInt(fieldMiningJitterInterval.getText()));
            
            // OX ayarları
            config.setOxMinPlayers(Integer.parseInt(fieldOxMinPlayers.getText()));
            
            // Obsidyen jitter ayarları
            config.setObsidianJitterYaw(Float.parseFloat(fieldObsidianJitterYaw.getText()));
            config.setObsidianJitterPitch(Float.parseFloat(fieldObsidianJitterPitch.getText()));
            config.setObsidianJitterInterval(Integer.parseInt(fieldObsidianJitterInterval.getText()));
            
            // Obsidyen aim hızı ayarları
            config.setObsidianAimSpeed(Float.parseFloat(fieldObsidianAimSpeed.getText()));
            config.setObsidianTurnSpeed(Float.parseFloat(fieldObsidianTurnSpeed.getText()));
            
            // Obsidyen sell ayarları
            config.setObsidianSellEnabled(obsidianSellEnabled);
            config.setObsidianSellCommand(fieldObsidianSellCommand.getText());
            config.setObsidianSellDelay((int)(Float.parseFloat(fieldObsidianSellDelay.getText()) * 1000));
            
            // Obsidyen hedef mesafe ayarları
            config.setObsidianTargetMinOffset(Integer.parseInt(fieldObsidianTargetMin.getText()));
            config.setObsidianTargetMaxOffset(Integer.parseInt(fieldObsidianTargetMax.getText()));
            
            config.save();
            schedule.save();
            
            MuzMod.LOGGER.info("[GUI] All settings saved");
        } catch (Exception e) {
            MuzMod.LOGGER.error("[GUI] Save error: " + e.getMessage());
        }
    }
    
    private boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
    
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        
        int scroll = Mouse.getEventDWheel();
        if (scroll == 0) return;
        
        // Scroll wheel for schedule list
        if (currentTab == 1) {
            ScheduleManager schedule = MuzMod.instance.getScheduleManager();
            List<ScheduleEntry> entries = schedule.getEntriesForDay(selectedDay);
            int maxScroll = Math.max(0, entries.size() - 4);
            
            if (scroll > 0 && scheduleScrollOffset > 0) {
                scheduleScrollOffset--;
            } else if (scroll < 0 && scheduleScrollOffset < maxScroll) {
                scheduleScrollOffset++;
            }
        }
        
        // Scroll wheel for Mining settings
        if (currentTab == 2 && settingsSubTab == 1) {
            int scrollStep = 15; // Her scroll için 15 piksel
            
            if (scroll > 0 && miningSettingsScrollOffset > 0) {
                miningSettingsScrollOffset = Math.max(0, miningSettingsScrollOffset - scrollStep);
            } else if (scroll < 0 && miningSettingsScrollOffset < MINING_SETTINGS_SCROLL_MAX) {
                miningSettingsScrollOffset = Math.min(MINING_SETTINGS_SCROLL_MAX, miningSettingsScrollOffset + scrollStep);
            }
        }
        
        // Scroll wheel for Obsidyen settings
        if (currentTab == 2 && settingsSubTab == 3) {
            int scrollStep = 20; // Her scroll için 20 piksel
            
            if (scroll > 0 && obsidianSettingsScrollOffset > 0) {
                obsidianSettingsScrollOffset = Math.max(0, obsidianSettingsScrollOffset - scrollStep);
            } else if (scroll < 0 && obsidianSettingsScrollOffset < OBSIDIAN_SETTINGS_SCROLL_MAX) {
                obsidianSettingsScrollOffset = Math.min(OBSIDIAN_SETTINGS_SCROLL_MAX, obsidianSettingsScrollOffset + scrollStep);
            }
        }
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (addingNewEntry) {
                addingNewEntry = false;
            } else if (selectedEntryId != -1) {
                selectedEntryId = -1;
            } else {
                mc.displayGuiScreen(null);
            }
            return;
        }
        
        // Route to active fields
        if (currentTab == 1 && (addingNewEntry || selectedEntryId != -1)) {
            fieldStartTime.textboxKeyTyped(typedChar, keyCode);
            fieldEndTime.textboxKeyTyped(typedChar, keyCode);
            fieldWarpCommand.textboxKeyTyped(typedChar, keyCode);
        } else if (currentTab == 2) {
            // Sub-tab based field handling
            if (settingsSubTab == 0) {
                fieldDefaultMiningWarp.textboxKeyTyped(typedChar, keyCode);
                fieldDefaultAfkWarp.textboxKeyTyped(typedChar, keyCode);
                fieldRepairThreshold.textboxKeyTyped(typedChar, keyCode);
                fieldTimeOffset.textboxKeyTyped(typedChar, keyCode);
                fieldDetectRadius.textboxKeyTyped(typedChar, keyCode);
                fieldRepairClickDelay.textboxKeyTyped(typedChar, keyCode);
            } else if (settingsSubTab == 1) {
                fieldInitialWalkMin.textboxKeyTyped(typedChar, keyCode);
                fieldInitialWalkMax.textboxKeyTyped(typedChar, keyCode);
                fieldWalkYawVar.textboxKeyTyped(typedChar, keyCode);
                fieldMaxDistFromCenter.textboxKeyTyped(typedChar, keyCode);
                fieldSecondWalkMin.textboxKeyTyped(typedChar, keyCode);
                fieldSecondWalkMax.textboxKeyTyped(typedChar, keyCode);
                fieldSecondWalkAngle.textboxKeyTyped(typedChar, keyCode);
                fieldStrafeInterval.textboxKeyTyped(typedChar, keyCode);
                fieldStrafeDuration.textboxKeyTyped(typedChar, keyCode);
                fieldMiningJitterYaw.textboxKeyTyped(typedChar, keyCode);
                fieldMiningJitterPitch.textboxKeyTyped(typedChar, keyCode);
                fieldMiningJitterInterval.textboxKeyTyped(typedChar, keyCode);
            } else if (settingsSubTab == 2) {
                fieldOxMinPlayers.textboxKeyTyped(typedChar, keyCode);
            } else if (settingsSubTab == 3) {
                fieldObsidianJitterYaw.textboxKeyTyped(typedChar, keyCode);
                fieldObsidianJitterPitch.textboxKeyTyped(typedChar, keyCode);
                fieldObsidianJitterInterval.textboxKeyTyped(typedChar, keyCode);
                fieldObsidianAimSpeed.textboxKeyTyped(typedChar, keyCode);
                fieldObsidianTurnSpeed.textboxKeyTyped(typedChar, keyCode);
                fieldObsidianSellCommand.textboxKeyTyped(typedChar, keyCode);
                fieldObsidianSellDelay.textboxKeyTyped(typedChar, keyCode);
                fieldObsidianTargetMin.textboxKeyTyped(typedChar, keyCode);
                fieldObsidianTargetMax.textboxKeyTyped(typedChar, keyCode);
            } else if (settingsSubTab == 4) {
                fieldDuelPlayer1.textboxKeyTyped(typedChar, keyCode);
                fieldDuelPlayer2.textboxKeyTyped(typedChar, keyCode);
            }
        }
    }
    
    @Override
    public void updateScreen() {
        for (GuiTextField field : allFields) {
            field.updateCursorCounter();
        }
    }
    
    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        // Slider drag bırakıldığında
        draggingAimSpeed = false;
        draggingTurnSpeed = false;
    }
    
    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        
        // Slider drag güncelleme
        if (settingsSubTab == 3) {
            int sliderX = fieldObsidianAimSpeed.xPosition - 110;
            if (draggingAimSpeed) {
                updateSliderValue(mouseX, sliderX, 100, 0.01f, 1.0f, "aimSpeed");
            }
            if (draggingTurnSpeed) {
                updateSliderValue(mouseX, sliderX, 100, 0.01f, 1.0f, "turnSpeed");
            }
        }
    }
    
    /**
     * Slider değerini güncelle ve field'a yaz
     */
    private void updateSliderValue(int mouseX, int sliderX, int sliderWidth, float min, float max, String sliderId) {
        ModConfig config = MuzMod.instance.getConfig();
        
        // Mouse pozisyonundan değer hesapla
        float percent = (float)(mouseX - sliderX) / sliderWidth;
        percent = Math.max(0, Math.min(1, percent));
        float value = min + percent * (max - min);
        
        // 2 decimal precision
        value = Math.round(value * 100) / 100.0f;
        
        // Config'e kaydet ve field'ı güncelle
        if (sliderId.equals("aimSpeed")) {
            config.setObsidianAimSpeed(value);
            fieldObsidianAimSpeed.setText(String.valueOf(value));
        } else if (sliderId.equals("turnSpeed")) {
            config.setObsidianTurnSpeed(value);
            fieldObsidianTurnSpeed.setText(String.valueOf(value));
        }
    }
    
    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
