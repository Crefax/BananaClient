package com.muzmod.gui;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
import com.muzmod.schedule.ScheduleEntry;
import com.muzmod.schedule.ScheduleManager;
import com.muzmod.state.IState;
import com.muzmod.state.impl.SafeState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

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
    private static final int GUI_WIDTH = 450;
    private static final int GUI_HEIGHT = 360;
    
    // Tabs
    private int currentTab = 0;
    private String[] tabNames = {"Genel", "Zamanlama", "Ayarlar"};
    
    // Settings sub-tabs
    private int settingsSubTab = 0; // 0=Genel, 1=Mining, 2=OX
    private String[] settingsSubTabs = {"Genel", "Mining", "OX"};
    
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
    
    // Mining settings fields
    private GuiTextField fieldInitialWalkMin, fieldInitialWalkMax, fieldWalkYawVar;
    private GuiTextField fieldSecondWalkMin, fieldSecondWalkMax, fieldSecondWalkAngle;
    private GuiTextField fieldStrafeInterval, fieldStrafeDuration;
    private GuiTextField fieldMaxDistFromCenter;
    
    // OX settings fields
    private GuiTextField fieldOxMinPlayers;
    
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
        
        // OX settings fields
        fieldOxMinPlayers = createField(setX, setY, 50, String.valueOf(config.getOxMinPlayers()), 3);
        
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
        drawButton(guiX + 20, y, 65, 22, "Idle", BG_BUTTON, mouseX, mouseY);
        drawButton(guiX + 90, y, 65, 22, "§6Mining", ACCENT_ORANGE, mouseX, mouseY);
        drawButton(guiX + 160, y, 65, 22, "§bAFK", ACCENT_BLUE, mouseX, mouseY);
        drawButton(guiX + 230, y, 65, 22, "§eRepair", ACCENT_YELLOW, mouseX, mouseY);
        drawButton(guiX + 300, y, 65, 22, "§dOX", ACCENT_PURPLE, mouseX, mouseY);
        
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
        int fieldX = guiX + 140;
        
        // Draw sub-tab content
        switch (settingsSubTab) {
            case 0: drawSettingsGeneral(mouseX, mouseY, config, schedule, y, labelX, fieldX); break;
            case 1: drawSettingsMining(mouseX, mouseY, config, y, labelX, fieldX); break;
            case 2: drawSettingsOX(mouseX, mouseY, config, y, labelX, fieldX); break;
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
        
        // Toggles
        y += 30;
        drawToggle(labelX, y, "Blok Kilidi", config.isBlockLockEnabled(), mouseX, mouseY);
        drawToggle(labelX + 120, y, "Aninda Kac", config.isInstantFlee(), mouseX, mouseY);
        drawToggle(labelX + 240, y, "Strafe", config.isStrafeEnabled(), mouseX, mouseY);
    }
    
    private void drawSettingsMining(int mouseX, int mouseY, ModConfig config, int y, int labelX, int fieldX) {
        // İlk Yürüyüş
        drawString(fontRendererObj, "§6§lİlk Yürüyüş (South)", labelX, y, ACCENT_ORANGE);
        y += 16;
        
        drawString(fontRendererObj, "§7Min-Max Mesafe:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldInitialWalkMin, fieldX, y);
        fieldInitialWalkMin.xPosition = fieldX;
        fieldInitialWalkMin.yPosition = y;
        fieldInitialWalkMin.drawTextBox();
        drawString(fontRendererObj, "§7-", fieldX + 55, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldInitialWalkMax, fieldX + 65, y);
        fieldInitialWalkMax.xPosition = fieldX + 65;
        fieldInitialWalkMax.yPosition = y;
        fieldInitialWalkMax.drawTextBox();
        drawString(fontRendererObj, "§8blok", fieldX + 125, y + 3, TEXT_DARK);
        
        y += 22;
        drawString(fontRendererObj, "§7Açı Varyasyonu:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldWalkYawVar, fieldX, y);
        fieldWalkYawVar.xPosition = fieldX;
        fieldWalkYawVar.yPosition = y;
        fieldWalkYawVar.drawTextBox();
        drawString(fontRendererObj, "§8derece (+/-)", fieldX + 55, y + 3, TEXT_DARK);
        
        // Mining Center
        y += 26;
        drawString(fontRendererObj, "§a§lMining Merkezi", labelX, y, ACCENT_GREEN);
        y += 16;
        
        drawString(fontRendererObj, "§7Max Uzaklık:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldMaxDistFromCenter, fieldX, y);
        fieldMaxDistFromCenter.xPosition = fieldX;
        fieldMaxDistFromCenter.yPosition = y;
        fieldMaxDistFromCenter.drawTextBox();
        drawString(fontRendererObj, "§8blok (merkezden)", fieldX + 55, y + 3, TEXT_DARK);
        
        // İkinci Yürüyüş
        y += 26;
        drawString(fontRendererObj, "§b§lİkinci Yürüyüş (East/West)", labelX, y, ACCENT_CYAN);
        y += 16;
        
        boolean secondEnabled = config.isSecondWalkEnabled();
        drawToggle(labelX, y, "Aktif", secondEnabled, mouseX, mouseY);
        
        y += 18;
        drawString(fontRendererObj, "§7Min-Max Mesafe:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldSecondWalkMin, fieldX, y);
        fieldSecondWalkMin.xPosition = fieldX;
        fieldSecondWalkMin.yPosition = y;
        fieldSecondWalkMin.drawTextBox();
        drawString(fontRendererObj, "§7-", fieldX + 55, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldSecondWalkMax, fieldX + 65, y);
        fieldSecondWalkMax.xPosition = fieldX + 65;
        fieldSecondWalkMax.yPosition = y;
        fieldSecondWalkMax.drawTextBox();
        drawString(fontRendererObj, "§8blok", fieldX + 125, y + 3, TEXT_DARK);
        
        y += 22;
        drawString(fontRendererObj, "§7Açı Varyasyonu:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldSecondWalkAngle, fieldX, y);
        fieldSecondWalkAngle.xPosition = fieldX;
        fieldSecondWalkAngle.yPosition = y;
        fieldSecondWalkAngle.drawTextBox();
        drawString(fontRendererObj, "§8derece (+/-)", fieldX + 55, y + 3, TEXT_DARK);
        
        y += 22;
        boolean randomDir = config.isSecondWalkRandomDirection();
        drawToggle(labelX, y, "Rastgele Yön", randomDir, mouseX, mouseY);
        drawString(fontRendererObj, randomDir ? "§a(East/West)" : "§7(Sadece West)", labelX + 110, y, TEXT_GRAY);
        
        // Strafe Anti-AFK
        y += 26;
        drawString(fontRendererObj, "§e§lStrafe Anti-AFK", labelX, y, ACCENT_YELLOW);
        y += 16;
        
        boolean strafeEnabled = config.isStrafeEnabled();
        drawToggle(labelX, y, "Aktif", strafeEnabled, mouseX, mouseY);
        
        y += 18;
        drawString(fontRendererObj, "§7Aralık / Süre:", labelX, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldStrafeInterval, fieldX, y);
        fieldStrafeInterval.xPosition = fieldX;
        fieldStrafeInterval.yPosition = y;
        fieldStrafeInterval.drawTextBox();
        drawString(fontRendererObj, "§8sn", fieldX + 55, y + 3, TEXT_DARK);
        drawFieldBackground(fieldStrafeDuration, fieldX + 80, y);
        fieldStrafeDuration.xPosition = fieldX + 80;
        fieldStrafeDuration.yPosition = y;
        fieldStrafeDuration.drawTextBox();
        drawString(fontRendererObj, "§8ms", fieldX + 140, y + 3, TEXT_DARK);
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
            
            // State buttons
            y += 30;
            if (isInside(mouseX, mouseY, guiX + 20, y, 65, 22)) {
                MuzMod.instance.getStateManager().forceState("idle");
            } else if (isInside(mouseX, mouseY, guiX + 90, y, 65, 22)) {
                MuzMod.instance.getStateManager().forceState("mining");
            } else if (isInside(mouseX, mouseY, guiX + 160, y, 65, 22)) {
                MuzMod.instance.getStateManager().forceState("afk");
            } else if (isInside(mouseX, mouseY, guiX + 230, y, 65, 22)) {
                MuzMod.instance.getStateManager().forceState("repair");
            } else if (isInside(mouseX, mouseY, guiX + 300, y, 65, 22)) {
                MuzMod.instance.getStateManager().forceState("ox");
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
                int toggleY = y + 26 * 5 + 4;
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
            }
            else if (settingsSubTab == 1) {
                // Mining sub-tab - y hesaplaması (güncellenmiş layout):
                // İlk Yürüyüş başlık: y
                // Min-Max Mesafe: y + 16
                // Açı Var: y + 16 + 22 = y + 38
                // Mining Merkezi başlık: y + 38 + 26 = y + 64
                // Max Uzaklık: y + 64 + 16 = y + 80
                // İkinci Yürüyüş başlık: y + 80 + 26 = y + 106
                // Aktif toggle: y + 106 + 16 = y + 122
                int secondToggleY = y + 122;
                if (isInside(mouseX, mouseY, labelX - 2, secondToggleY - 2, 70, 14)) {
                    config.setSecondWalkEnabled(!config.isSecondWalkEnabled());
                }
                
                // Min-Max: y + 122 + 18 = y + 140
                // Açı Var: y + 140 + 22 = y + 162
                // Rastgele Yön: y + 162 + 22 = y + 184
                int randomDirY = y + 184;
                if (isInside(mouseX, mouseY, labelX - 2, randomDirY - 2, 110, 14)) {
                    config.setSecondWalkRandomDirection(!config.isSecondWalkRandomDirection());
                }
                
                // Strafe başlık: y + 184 + 26 = y + 210
                // Aktif toggle: y + 210 + 16 = y + 226
                int strafeToggleY = y + 226;
                if (isInside(mouseX, mouseY, labelX - 2, strafeToggleY - 2, 70, 14)) {
                    config.setStrafeEnabled(!config.isStrafeEnabled());
                }
                
                // Field clicks
                fieldInitialWalkMin.mouseClicked(mouseX, mouseY, mouseButton);
                fieldInitialWalkMax.mouseClicked(mouseX, mouseY, mouseButton);
                fieldWalkYawVar.mouseClicked(mouseX, mouseY, mouseButton);
                fieldMaxDistFromCenter.mouseClicked(mouseX, mouseY, mouseButton);
                fieldSecondWalkMin.mouseClicked(mouseX, mouseY, mouseButton);
                fieldSecondWalkMax.mouseClicked(mouseX, mouseY, mouseButton);
                fieldSecondWalkAngle.mouseClicked(mouseX, mouseY, mouseButton);
                fieldStrafeInterval.mouseClicked(mouseX, mouseY, mouseButton);
                fieldStrafeDuration.mouseClicked(mouseX, mouseY, mouseButton);
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
            
            // OX ayarları
            config.setOxMinPlayers(Integer.parseInt(fieldOxMinPlayers.getText()));
            
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
        
        // Scroll wheel for schedule list
        if (currentTab == 1) {
            int scroll = Mouse.getEventDWheel();
            if (scroll != 0) {
                ScheduleManager schedule = MuzMod.instance.getScheduleManager();
                List<ScheduleEntry> entries = schedule.getEntriesForDay(selectedDay);
                int maxScroll = Math.max(0, entries.size() - 4);
                
                if (scroll > 0 && scheduleScrollOffset > 0) {
                    scheduleScrollOffset--;
                } else if (scroll < 0 && scheduleScrollOffset < maxScroll) {
                    scheduleScrollOffset++;
                }
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
            } else if (settingsSubTab == 2) {
                fieldOxMinPlayers.textboxKeyTyped(typedChar, keyCode);
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
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
