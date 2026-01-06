package com.muzmod.gui;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
import com.muzmod.state.IState;
import com.muzmod.state.impl.SafeState;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * MuzMod Modern GUI v2.0
 * Clean, minimal design
 */
public class MuzModGuiModern extends GuiScreen {
    
    // Colors
    private static final int BG_DARK = 0xE0101010;
    private static final int BG_PANEL = 0xFF1A1A1A;
    private static final int BG_HEADER = 0xFF202020;
    private static final int BG_BUTTON = 0xFF252525;
    private static final int BG_BUTTON_HOVER = 0xFF303030;
    private static final int BG_FIELD = 0xFF151515;
    
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
    private static final int GUI_WIDTH = 400;
    private static final int GUI_HEIGHT = 280;
    
    // Current tab
    private int currentTab = 0;
    private String[] tabNames = {"Genel", "Maden", "AFK", "Ayarlar"};
    
    // Text fields
    private List<GuiTextField> fields = new ArrayList<>();
    private GuiTextField fieldMiningWarp, fieldMiningStart, fieldMiningEnd;
    private GuiTextField fieldAfkWarp, fieldAfkStart, fieldAfkEnd;
    private GuiTextField fieldWalkDist, fieldDetectRadius, fieldRepairThreshold, fieldTimeOffset;
    
    // Animation
    private long openTime;
    
    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        openTime = System.currentTimeMillis();
        
        guiX = (width - GUI_WIDTH) / 2;
        guiY = (height - GUI_HEIGHT) / 2;
        
        initFields();
    }
    
    private void initFields() {
        fields.clear();
        ModConfig config = MuzMod.instance.getConfig();
        
        int fieldX = guiX + 120;
        int fieldY = guiY + 70;
        
        // Mining fields
        fieldMiningWarp = createField(fieldX, fieldY, 150, config.getMiningWarpCommand(), 50);
        fieldMiningStart = createField(fieldX, fieldY + 25, 60, 
            String.format("%02d:%02d", config.getMiningStartHour(), config.getMiningStartMinute()), 5);
        fieldMiningEnd = createField(fieldX + 80, fieldY + 25, 60, 
            String.format("%02d:%02d", config.getMiningEndHour(), config.getMiningEndMinute()), 5);
        fieldWalkDist = createField(fieldX, fieldY + 50, 50, 
            String.valueOf(config.getInitialWalkDistance()), 4);
        fieldDetectRadius = createField(fieldX, fieldY + 75, 50, 
            String.valueOf((int)config.getPlayerDetectionRadius()), 4);
        
        // AFK fields
        fieldAfkWarp = createField(fieldX, fieldY, 150, config.getAfkWarpCommand(), 50);
        fieldAfkStart = createField(fieldX, fieldY + 25, 60, 
            String.format("%02d:%02d", config.getAfkStartHour(), config.getAfkStartMinute()), 5);
        fieldAfkEnd = createField(fieldX + 80, fieldY + 25, 60, 
            String.format("%02d:%02d", config.getAfkEndHour(), config.getAfkEndMinute()), 5);
        
        // Settings fields
        fieldRepairThreshold = createField(fieldX, fieldY, 50, 
            String.valueOf(config.getRepairDurabilityThreshold()), 5);
        fieldTimeOffset = createField(fieldX, fieldY + 25, 50, 
            String.valueOf(config.getTimeOffsetHours()), 3);
    }
    
    private GuiTextField createField(int x, int y, int w, String text, int maxLen) {
        GuiTextField field = new GuiTextField(fields.size(), fontRendererObj, x, y, w, 16);
        field.setMaxStringLength(maxLen);
        field.setText(text);
        field.setEnableBackgroundDrawing(false);
        fields.add(field);
        return field;
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Dark overlay
        drawRect(0, 0, width, height, 0xA0000000);
        
        // Main panel shadow
        drawRect(guiX + 3, guiY + 3, guiX + GUI_WIDTH + 3, guiY + GUI_HEIGHT + 3, 0x40000000);
        
        // Main panel background
        drawRect(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, BG_PANEL);
        
        // Header
        drawGradientRect(guiX, guiY, guiX + GUI_WIDTH, guiY + 30, BG_HEADER, BG_PANEL);
        
        // Title
        drawString(fontRendererObj, "§d§lMUZ§f§lMOD §7v2.0", guiX + 10, guiY + 10, TEXT_WHITE);
        
        // Time
        ModConfig config = MuzMod.instance.getConfig();
        Calendar cal = Calendar.getInstance();
        int hour = (cal.get(Calendar.HOUR_OF_DAY) + config.getTimeOffsetHours() + 24) % 24;
        String timeStr = String.format("§e%02d:%02d §7UTC", hour, cal.get(Calendar.MINUTE));
        drawString(fontRendererObj, timeStr, guiX + GUI_WIDTH - 70, guiY + 10, TEXT_WHITE);
        
        // Header line
        drawGradientRectH(guiX, guiY + 29, guiX + GUI_WIDTH, guiY + 31, ACCENT_PURPLE, ACCENT_CYAN);
        
        // Tabs
        drawTabs(mouseX, mouseY);
        
        // Content
        int contentY = guiY + 65;
        
        switch (currentTab) {
            case 0: drawGeneralTab(contentY, mouseX, mouseY); break;
            case 1: drawMiningTab(contentY); break;
            case 2: drawAfkTab(contentY); break;
            case 3: drawSettingsTab(contentY, mouseX, mouseY); break;
        }
        
        // Bottom bar
        drawRect(guiX, guiY + GUI_HEIGHT - 35, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, BG_HEADER);
        drawGradientRectH(guiX, guiY + GUI_HEIGHT - 36, guiX + GUI_WIDTH, guiY + GUI_HEIGHT - 35, ACCENT_PURPLE, ACCENT_CYAN);
        
        // Save/Close buttons
        drawButton(guiX + GUI_WIDTH - 160, guiY + GUI_HEIGHT - 28, 70, 20, "§aKaydet", ACCENT_GREEN, mouseX, mouseY);
        drawButton(guiX + GUI_WIDTH - 80, guiY + GUI_HEIGHT - 28, 70, 20, "§cKapat", ACCENT_RED, mouseX, mouseY);
        
        // Footer text
        drawString(fontRendererObj, "§8github.com/Crefax/MuzClient", guiX + 10, guiY + GUI_HEIGHT - 23, TEXT_DARK);
        
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
        int tabY = guiY + 35;
        
        for (int i = 0; i < tabNames.length; i++) {
            int tabX = guiX + i * tabWidth;
            boolean hovered = mouseX >= tabX && mouseX < tabX + tabWidth && mouseY >= tabY && mouseY < tabY + 25;
            boolean selected = currentTab == i;
            
            // Tab background
            if (selected) {
                drawRect(tabX, tabY, tabX + tabWidth, tabY + 25, BG_PANEL);
                // Accent underline
                int[] colors = {ACCENT_PURPLE, ACCENT_ORANGE, ACCENT_BLUE, ACCENT_YELLOW};
                drawRect(tabX + 5, tabY + 22, tabX + tabWidth - 5, tabY + 24, colors[i]);
            } else if (hovered) {
                drawRect(tabX, tabY, tabX + tabWidth, tabY + 25, BG_BUTTON_HOVER);
            } else {
                drawRect(tabX, tabY, tabX + tabWidth, tabY + 25, BG_HEADER);
            }
            
            // Tab text
            String text = tabNames[i];
            int textX = tabX + (tabWidth - fontRendererObj.getStringWidth(text)) / 2;
            drawString(fontRendererObj, text, textX, tabY + 8, selected ? TEXT_WHITE : TEXT_GRAY);
        }
    }
    
    private void drawGeneralTab(int y, int mouseX, int mouseY) {
        // Bot toggle
        boolean botOn = MuzMod.instance.isBotEnabled();
        String botText = botOn ? "§a● Bot Aktif" : "§c○ Bot Kapalı";
        int botColor = botOn ? ACCENT_GREEN : ACCENT_RED;
        drawButton(guiX + 20, y, 150, 24, botText, botColor, mouseX, mouseY);
        
        // State buttons
        drawButton(guiX + 20, y + 35, 80, 22, "Idle", BG_BUTTON, mouseX, mouseY);
        drawButton(guiX + 110, y + 35, 80, 22, "§6Mining", ACCENT_ORANGE, mouseX, mouseY);
        drawButton(guiX + 20, y + 62, 80, 22, "§bAFK", ACCENT_BLUE, mouseX, mouseY);
        drawButton(guiX + 110, y + 62, 80, 22, "§eRepair", ACCENT_YELLOW, mouseX, mouseY);
        
        // Current state
        IState state = MuzMod.instance.getStateManager().getCurrentState();
        String stateName = state != null ? state.getName() : "Idle";
        String status = state != null ? state.getStatus() : "Bekleniyor...";
        
        drawString(fontRendererObj, "§7Durum: §f" + stateName, guiX + 20, y + 100, TEXT_WHITE);
        drawString(fontRendererObj, "§8" + truncate(status, 35), guiX + 20, y + 115, TEXT_DARK);
        
        // Pickaxe info
        drawRect(guiX + 210, y, guiX + GUI_WIDTH - 20, y + 135, BG_HEADER);
        drawString(fontRendererObj, "§eKazma Durumu", guiX + 220, y + 8, ACCENT_YELLOW);
        
        if (mc.thePlayer != null) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held != null && held.getItem() instanceof ItemPickaxe) {
                int max = held.getMaxDamage();
                int damage = held.getItemDamage();
                int remaining = max - damage;
                float percent = (float) remaining / max;
                
                drawString(fontRendererObj, "§f" + truncate(held.getDisplayName(), 20), guiX + 220, y + 28, TEXT_WHITE);
                
                // Progress bar
                int barX = guiX + 220;
                int barY = y + 48;
                int barW = 150;
                int barH = 10;
                
                drawRect(barX, barY, barX + barW, barY + barH, BG_FIELD);
                int fillW = (int) (barW * percent);
                int barColor = percent > 0.5f ? ACCENT_GREEN : (percent > 0.25f ? ACCENT_YELLOW : ACCENT_RED);
                drawRect(barX, barY, barX + fillW, barY + barH, barColor);
                
                drawString(fontRendererObj, "§7" + remaining + " / " + max, barX, y + 65, TEXT_GRAY);
                drawString(fontRendererObj, String.format("§f%.1f%%", percent * 100), barX + 100, y + 65, TEXT_WHITE);
            } else {
                drawString(fontRendererObj, "§cKazma bulunamadı", guiX + 220, y + 40, ACCENT_RED);
            }
        }
    }
    
    private void drawMiningTab(int y) {
        drawString(fontRendererObj, "§7Warp Komutu:", guiX + 20, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldMiningWarp);
        fieldMiningWarp.drawTextBox();
        
        drawString(fontRendererObj, "§7Başlangıç:", guiX + 20, y + 28, TEXT_GRAY);
        drawFieldBackground(fieldMiningStart);
        fieldMiningStart.drawTextBox();
        
        drawString(fontRendererObj, "§7Bitiş:", guiX + 200, y + 28, TEXT_GRAY);
        drawFieldBackground(fieldMiningEnd);
        fieldMiningEnd.drawTextBox();
        
        drawString(fontRendererObj, "§7Yürüme Mesafesi:", guiX + 20, y + 53, TEXT_GRAY);
        drawFieldBackground(fieldWalkDist);
        fieldWalkDist.drawTextBox();
        drawString(fontRendererObj, "§8blok", guiX + 175, y + 53, TEXT_DARK);
        
        drawString(fontRendererObj, "§7Tespit Yarıçapı:", guiX + 20, y + 78, TEXT_GRAY);
        drawFieldBackground(fieldDetectRadius);
        fieldDetectRadius.drawTextBox();
        drawString(fontRendererObj, "§8blok", guiX + 175, y + 78, TEXT_DARK);
        
        // Toggles
        ModConfig config = MuzMod.instance.getConfig();
        drawToggle(guiX + 20, y + 105, "Blok Kilidi", config.isBlockLockEnabled());
        drawToggle(guiX + 20, y + 125, "Anında Kaç", config.isInstantFlee());
    }
    
    private void drawAfkTab(int y) {
        drawString(fontRendererObj, "§7Warp Komutu:", guiX + 20, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldAfkWarp);
        fieldAfkWarp.drawTextBox();
        
        drawString(fontRendererObj, "§7Başlangıç:", guiX + 20, y + 28, TEXT_GRAY);
        drawFieldBackground(fieldAfkStart);
        fieldAfkStart.drawTextBox();
        
        drawString(fontRendererObj, "§7Bitiş:", guiX + 200, y + 28, TEXT_GRAY);
        drawFieldBackground(fieldAfkEnd);
        fieldAfkEnd.drawTextBox();
        
        // Info
        drawString(fontRendererObj, "§8AFK modunda bot warp komutunu", guiX + 20, y + 60, TEXT_DARK);
        drawString(fontRendererObj, "§8çalıştırıp bekler.", guiX + 20, y + 72, TEXT_DARK);
    }
    
    private void drawSettingsTab(int y, int mouseX, int mouseY) {
        drawString(fontRendererObj, "§7Tamir Eşiği:", guiX + 20, y + 3, TEXT_GRAY);
        drawFieldBackground(fieldRepairThreshold);
        fieldRepairThreshold.drawTextBox();
        drawString(fontRendererObj, "§8durability", guiX + 175, y + 3, TEXT_DARK);
        
        drawString(fontRendererObj, "§7Saat Ofseti:", guiX + 20, y + 28, TEXT_GRAY);
        drawFieldBackground(fieldTimeOffset);
        fieldTimeOffset.drawTextBox();
        drawString(fontRendererObj, "§8saat (UTC)", guiX + 175, y + 28, TEXT_DARK);
        
        // Strafe toggle
        ModConfig config = MuzMod.instance.getConfig();
        drawToggle(guiX + 20, y + 55, "Strafe Anti-AFK", config.isStrafeEnabled());
        
        // Info
        drawString(fontRendererObj, "§8Strafe: Kazarken A-D ile hareket", guiX + 20, y + 85, TEXT_DARK);
    }
    
    private void drawFieldBackground(GuiTextField field) {
        drawRect(field.xPosition - 3, field.yPosition - 3, 
                field.xPosition + field.width + 3, field.yPosition + field.height + 1, BG_FIELD);
        if (field.isFocused()) {
            drawRect(field.xPosition - 3, field.yPosition + field.height, 
                    field.xPosition + field.width + 3, field.yPosition + field.height + 1, ACCENT_CYAN);
        }
    }
    
    private void drawButton(int x, int y, int w, int h, String text, int accentColor, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        
        int bg = hovered ? BG_BUTTON_HOVER : BG_BUTTON;
        drawRect(x, y, x + w, y + h, bg);
        drawRect(x, y + h - 2, x + w, y + h, accentColor);
        
        if (hovered) {
            // Glow effect
            drawRect(x, y, x + w, y + 1, accentColor & 0x40FFFFFF);
        }
        
        int textX = x + (w - fontRendererObj.getStringWidth(text)) / 2;
        int textY = y + (h - 8) / 2;
        drawString(fontRendererObj, text, textX, textY, TEXT_WHITE);
    }
    
    private void drawToggle(int x, int y, String label, boolean enabled) {
        drawString(fontRendererObj, (enabled ? "§a✓ " : "§c✗ ") + label, x, y, enabled ? ACCENT_GREEN : TEXT_GRAY);
    }
    
    private void drawGradientRectH(int left, int top, int right, int bottom, int colorLeft, int colorRight) {
        // Horizontal gradient using multiple rects (simple approximation)
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
        
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        // Tab clicks
        int tabWidth = GUI_WIDTH / tabNames.length;
        int tabY = guiY + 35;
        for (int i = 0; i < tabNames.length; i++) {
            int tabX = guiX + i * tabWidth;
            if (mouseX >= tabX && mouseX < tabX + tabWidth && mouseY >= tabY && mouseY < tabY + 25) {
                currentTab = i;
                return;
            }
        }
        
        ModConfig config = MuzMod.instance.getConfig();
        int y = guiY + 65;
        
        // General tab buttons
        if (currentTab == 0) {
            if (isInside(mouseX, mouseY, guiX + 20, y, 150, 24)) {
                MuzMod.instance.toggleBot();
            } else if (isInside(mouseX, mouseY, guiX + 20, y + 35, 80, 22)) {
                MuzMod.instance.getStateManager().forceState("idle");
            } else if (isInside(mouseX, mouseY, guiX + 110, y + 35, 80, 22)) {
                MuzMod.instance.getStateManager().forceState("mining");
            } else if (isInside(mouseX, mouseY, guiX + 20, y + 62, 80, 22)) {
                MuzMod.instance.getStateManager().forceState("afk");
            } else if (isInside(mouseX, mouseY, guiX + 110, y + 62, 80, 22)) {
                MuzMod.instance.getStateManager().forceState("repair");
            }
        }
        
        // Mining tab toggles
        if (currentTab == 1) {
            if (isInside(mouseX, mouseY, guiX + 20, y + 105, 150, 15)) {
                config.setBlockLockEnabled(!config.isBlockLockEnabled());
            } else if (isInside(mouseX, mouseY, guiX + 20, y + 125, 150, 15)) {
                config.setInstantFlee(!config.isInstantFlee());
            }
        }
        
        // Settings tab toggle
        if (currentTab == 3) {
            if (isInside(mouseX, mouseY, guiX + 20, y + 55, 150, 15)) {
                config.setStrafeEnabled(!config.isStrafeEnabled());
            }
        }
        
        // Save/Close buttons
        if (isInside(mouseX, mouseY, guiX + GUI_WIDTH - 160, guiY + GUI_HEIGHT - 28, 70, 20)) {
            saveSettings();
        } else if (isInside(mouseX, mouseY, guiX + GUI_WIDTH - 80, guiY + GUI_HEIGHT - 28, 70, 20)) {
            mc.displayGuiScreen(null);
        }
        
        // Text field clicks based on current tab
        if (currentTab == 1) {
            fieldMiningWarp.mouseClicked(mouseX, mouseY, mouseButton);
            fieldMiningStart.mouseClicked(mouseX, mouseY, mouseButton);
            fieldMiningEnd.mouseClicked(mouseX, mouseY, mouseButton);
            fieldWalkDist.mouseClicked(mouseX, mouseY, mouseButton);
            fieldDetectRadius.mouseClicked(mouseX, mouseY, mouseButton);
        } else if (currentTab == 2) {
            fieldAfkWarp.mouseClicked(mouseX, mouseY, mouseButton);
            fieldAfkStart.mouseClicked(mouseX, mouseY, mouseButton);
            fieldAfkEnd.mouseClicked(mouseX, mouseY, mouseButton);
        } else if (currentTab == 3) {
            fieldRepairThreshold.mouseClicked(mouseX, mouseY, mouseButton);
            fieldTimeOffset.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }
    
    private boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
    
    private void saveSettings() {
        ModConfig config = MuzMod.instance.getConfig();
        
        try {
            // Mining
            config.setMiningWarpCommand(fieldMiningWarp.getText());
            parseTime(fieldMiningStart.getText(), true, true);
            parseTime(fieldMiningEnd.getText(), true, false);
            config.setInitialWalkDistance(Integer.parseInt(fieldWalkDist.getText()));
            config.setPlayerDetectionRadius(Double.parseDouble(fieldDetectRadius.getText()));
            
            // AFK
            config.setAfkWarpCommand(fieldAfkWarp.getText());
            parseTime(fieldAfkStart.getText(), false, true);
            parseTime(fieldAfkEnd.getText(), false, false);
            
            // Settings
            config.setRepairDurabilityThreshold(Integer.parseInt(fieldRepairThreshold.getText()));
            config.setTimeOffsetHours(Integer.parseInt(fieldTimeOffset.getText()));
            
            config.save();
            MuzMod.LOGGER.info("[GUI] Settings saved");
        } catch (Exception e) {
            MuzMod.LOGGER.error("[GUI] Save error: " + e.getMessage());
        }
    }
    
    private void parseTime(String text, boolean mining, boolean start) {
        ModConfig config = MuzMod.instance.getConfig();
        String[] parts = text.split(":");
        if (parts.length == 2) {
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (mining) {
                if (start) config.setMiningStartTime(h, m);
                else config.setMiningEndTime(h, m);
            } else {
                if (start) config.setAfkStartTime(h, m);
                else config.setAfkEndTime(h, m);
            }
        }
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        
        // Route to active tab's fields
        if (currentTab == 1) {
            fieldMiningWarp.textboxKeyTyped(typedChar, keyCode);
            fieldMiningStart.textboxKeyTyped(typedChar, keyCode);
            fieldMiningEnd.textboxKeyTyped(typedChar, keyCode);
            fieldWalkDist.textboxKeyTyped(typedChar, keyCode);
            fieldDetectRadius.textboxKeyTyped(typedChar, keyCode);
        } else if (currentTab == 2) {
            fieldAfkWarp.textboxKeyTyped(typedChar, keyCode);
            fieldAfkStart.textboxKeyTyped(typedChar, keyCode);
            fieldAfkEnd.textboxKeyTyped(typedChar, keyCode);
        } else if (currentTab == 3) {
            fieldRepairThreshold.textboxKeyTyped(typedChar, keyCode);
            fieldTimeOffset.textboxKeyTyped(typedChar, keyCode);
        }
    }
    
    @Override
    public void updateScreen() {
        if (currentTab == 1) {
            fieldMiningWarp.updateCursorCounter();
            fieldMiningStart.updateCursorCounter();
            fieldMiningEnd.updateCursorCounter();
            fieldWalkDist.updateCursorCounter();
            fieldDetectRadius.updateCursorCounter();
        } else if (currentTab == 2) {
            fieldAfkWarp.updateCursorCounter();
            fieldAfkStart.updateCursorCounter();
            fieldAfkEnd.updateCursorCounter();
        } else if (currentTab == 3) {
            fieldRepairThreshold.updateCursorCounter();
            fieldTimeOffset.updateCursorCounter();
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
