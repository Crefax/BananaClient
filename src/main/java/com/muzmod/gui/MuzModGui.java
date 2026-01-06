package com.muzmod.gui;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
import com.muzmod.state.IState;
import com.muzmod.state.impl.MiningState;
import com.muzmod.state.impl.SafeState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * MuzMod Modern GUI v1.8.0
 * 
 * Temiz, modern ve okunabilir tasarım
 */
public class MuzModGui extends GuiScreen {
    
    // === COLORS ===
    private static final int BG_DARK = 0xF0101418;
    private static final int CARD_BG = 0xFF1A1D23;
    private static final int CARD_HEADER = 0xFF252A31;
    private static final int ACCENT_ORANGE = 0xFFFF9500;
    private static final int ACCENT_GREEN = 0xFF30D158;
    private static final int ACCENT_RED = 0xFFFF453A;
    private static final int ACCENT_BLUE = 0xFF0A84FF;
    private static final int ACCENT_YELLOW = 0xFFFFD60A;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFF8E8E93;
    private static final int TEXT_DARK = 0xFF48484A;
    
    // Layout
    private static final int CARD_WIDTH = 220;
    private static final int ROW_HEIGHT = 18;
    
    // Animation
    private long openTime;
    
    // Controls
    private GuiButton btnToggleBot, btnSave, btnClose;
    private GuiButton btnForceIdle, btnForceMining, btnForceAfk, btnForceRepair;
    private GuiButton btnBlockLock, btnInstantFlee, btnStrafeEnabled;
    
    // Text Fields - Mining
    private GuiTextField txtMiningWarp, txtMiningStart, txtMiningEnd;
    // Text Fields - AFK
    private GuiTextField txtAfkWarp, txtAfkStart, txtAfkEnd;
    // Text Fields - Settings
    private GuiTextField txtWalkDist, txtTimeOffset;
    private GuiTextField txtDetectRadius, txtRepairThreshold, txtRepairDelay;
    private GuiTextField txtStrafeInterval, txtStrafeDuration;
    
    private List<GuiTextField> allFields = new ArrayList<>();
    
    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        openTime = System.currentTimeMillis();
        allFields.clear();
        buttonList.clear();
        
        ModConfig config = MuzMod.instance.getConfig();
        
        int centerX = width / 2;
        int startY = 25;
        int leftX = centerX - CARD_WIDTH - 10;
        int rightX = centerX + 10;
        
        // === TOP BAR ===
        boolean botOn = MuzMod.instance.isBotEnabled();
        btnToggleBot = addBtn(0, leftX, startY, 90, 18, 
            botOn ? "§a● Bot Aktif" : "§c○ Bot Kapalı");
        
        btnForceIdle = addBtn(1, leftX + 95, startY, 45, 18, "§7Idle");
        btnForceMining = addBtn(2, leftX + 145, startY, 50, 18, "§6Maden");
        btnForceAfk = addBtn(3, leftX + 200, startY, 40, 18, "§bAFK");
        btnForceRepair = addBtn(4, rightX, startY, 50, 18, "§eTamir");
        
        txtTimeOffset = addField(rightX + 180, startY + 2, 30, 
            String.valueOf(config.getTimeOffsetHours()), 3);
        
        int cardY = startY + 28;
        
        // === LEFT: MADEN KARTI ===
        txtMiningWarp = addField(leftX + 60, cardY + 28, 150, config.getMiningWarpCommand(), 50);
        String mStart = String.format("%02d:%02d", config.getMiningStartHour(), config.getMiningStartMinute());
        String mEnd = String.format("%02d:%02d", config.getMiningEndHour(), config.getMiningEndMinute());
        txtMiningStart = addField(leftX + 60, cardY + 48, 50, mStart, 5);
        txtMiningEnd = addField(leftX + 130, cardY + 48, 50, mEnd, 5);
        txtWalkDist = addField(leftX + 100, cardY + 68, 40, String.valueOf(config.getInitialWalkDistance()), 3);
        txtDetectRadius = addField(leftX + 100, cardY + 88, 40, String.valueOf((int)config.getPlayerDetectionRadius()), 3);
        btnBlockLock = addBtn(10, leftX + 10, cardY + 108, 200, 16,
            config.isBlockLockEnabled() ? "§a✓ Blok Kilidi Açık" : "§7✗ Blok Kilidi Kapalı");
        btnInstantFlee = addBtn(11, leftX + 10, cardY + 128, 200, 16,
            config.isInstantFlee() ? "§a✓ Anında Kaç Açık" : "§7✗ Anında Kaç Kapalı");
        
        // === RIGHT: AFK KARTI ===
        txtAfkWarp = addField(rightX + 60, cardY + 28, 150, config.getAfkWarpCommand(), 50);
        String aStart = String.format("%02d:%02d", config.getAfkStartHour(), config.getAfkStartMinute());
        String aEnd = String.format("%02d:%02d", config.getAfkEndHour(), config.getAfkEndMinute());
        txtAfkStart = addField(rightX + 60, cardY + 48, 50, aStart, 5);
        txtAfkEnd = addField(rightX + 130, cardY + 48, 50, aEnd, 5);
        
        // Repair Settings
        txtRepairThreshold = addField(rightX + 120, cardY + 68, 40, 
            String.valueOf(config.getRepairDurabilityThreshold()), 4);
        txtRepairDelay = addField(rightX + 120, cardY + 88, 40, 
            String.format("%.1f", config.getRepairClickDelay()), 4);
        
        // Strafe Settings  
        txtStrafeInterval = addField(rightX + 120, cardY + 108, 40,
            String.valueOf(config.getStrafeInterval() / 1000), 4);
        txtStrafeDuration = addField(rightX + 120, cardY + 128, 40,
            String.valueOf(config.getStrafeDuration()), 4);
        btnStrafeEnabled = addBtn(12, rightX + 10, cardY + 148, 200, 16,
            config.isStrafeEnabled() ? "§a✓ Strafe Anti-AFK Açık" : "§7✗ Strafe Anti-AFK Kapalı");
        
        // === BOTTOM BUTTONS ===
        int bottomY = height - 30;
        btnSave = addBtn(100, centerX - 95, bottomY, 90, 20, "§a✓ Kaydet");
        btnClose = addBtn(101, centerX + 5, bottomY, 90, 20, "§c✗ Kapat");
    }
    
    private GuiButton addBtn(int id, int x, int y, int w, int h, String text) {
        GuiButton btn = new GuiButton(id, x, y, w, h, text);
        buttonList.add(btn);
        return btn;
    }
    
    private GuiTextField addField(int x, int y, int w, String text, int maxLen) {
        GuiTextField field = new GuiTextField(allFields.size(), fontRendererObj, x, y, w, 14);
        field.setMaxStringLength(maxLen);
        field.setText(text);
        allFields.add(field);
        return field;
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Animation
        long elapsed = System.currentTimeMillis() - openTime;
        float anim = Math.min(1f, elapsed / 150f);
        
        // Background
        drawRect(0, 0, width, height, applyAlpha(BG_DARK, anim));
        
        GlStateManager.pushMatrix();
        float scale = 0.95f + 0.05f * anim;
        GlStateManager.translate(width / 2f, height / 2f, 0);
        GlStateManager.scale(scale, scale, 1);
        GlStateManager.translate(-width / 2f, -height / 2f, 0);
        
        ModConfig config = MuzMod.instance.getConfig();
        int centerX = width / 2;
        int startY = 25;
        int leftX = centerX - CARD_WIDTH - 10;
        int rightX = centerX + 10;
        int cardY = startY + 28;
        
        // === HEADER ===
        drawRect(leftX - 5, startY - 5, rightX + CARD_WIDTH + 5, startY + 20, CARD_HEADER);
        drawString(fontRendererObj, "§6§lMUZMOD §7v1.8.0", leftX + 240, startY + 3, TEXT_WHITE);
        
        // Current State & Time
        IState state = MuzMod.instance.getStateManager().getCurrentState();
        String stateName = state != null ? state.getName() : "Idle";
        int stateColor = getStateColor(stateName);
        
        Calendar cal = Calendar.getInstance();
        int hour = (cal.get(Calendar.HOUR_OF_DAY) + config.getTimeOffsetHours() + 24) % 24;
        String timeStr = String.format("%02d:%02d", hour, cal.get(Calendar.MINUTE));
        
        drawString(fontRendererObj, "§7UTC", rightX + 155, startY + 3, TEXT_GRAY);
        
        // === LEFT CARD: MADEN ===
        drawCard(leftX, cardY, CARD_WIDTH, 155, "§6⛏ MADEN AYARLARI", ACCENT_ORANGE);
        drawLabel("Warp:", leftX + 12, cardY + 30);
        drawLabel("Başla:", leftX + 12, cardY + 50);
        drawLabel("Bitir:", leftX + 115, cardY + 50);
        drawLabel("Yürüme:", leftX + 12, cardY + 70);
        drawString(fontRendererObj, "§8blok", leftX + 145, cardY + 70, TEXT_DARK);
        drawLabel("Tespit R:", leftX + 12, cardY + 90);
        drawString(fontRendererObj, "§8blok", leftX + 145, cardY + 90, TEXT_DARK);
        
        // === RIGHT CARD: AFK & TAMİR ===
        drawCard(rightX, cardY, CARD_WIDTH, 175, "§b⏸ AFK & TAMİR", ACCENT_BLUE);
        drawLabel("Warp:", rightX + 12, cardY + 30);
        drawLabel("Başla:", rightX + 12, cardY + 50);
        drawLabel("Bitir:", rightX + 115, cardY + 50);
        
        drawString(fontRendererObj, "§7─── Tamir ───", rightX + 65, cardY + 68, TEXT_DARK);
        drawLabel("Dur. Eşik:", rightX + 12, cardY + 70);
        drawLabel("Tık Arası:", rightX + 12, cardY + 90);
        drawString(fontRendererObj, "§8sn", rightX + 165, cardY + 90, TEXT_DARK);
        
        drawString(fontRendererObj, "§7─── Strafe ───", rightX + 65, cardY + 108, TEXT_DARK);
        drawLabel("Aralık:", rightX + 12, cardY + 110);
        drawString(fontRendererObj, "§8sn", rightX + 165, cardY + 110, TEXT_DARK);
        drawLabel("Süre:", rightX + 12, cardY + 130);
        drawString(fontRendererObj, "§8ms", rightX + 165, cardY + 130, TEXT_DARK);
        
        // === STATUS BAR ===
        int statusY = cardY + 185;
        drawRect(leftX - 5, statusY, rightX + CARD_WIDTH + 5, statusY + 25, CARD_HEADER);
        
        String statusText = state != null ? state.getStatus() : "";
        drawString(fontRendererObj, "§7Durum: ", leftX + 5, statusY + 8, TEXT_GRAY);
        drawString(fontRendererObj, stateName, leftX + 50, statusY + 8, stateColor);
        
        if (statusText != null && !statusText.isEmpty()) {
            drawString(fontRendererObj, "§8» §f" + statusText, leftX + 100, statusY + 8, TEXT_WHITE);
        }
        
        drawString(fontRendererObj, "§e" + timeStr, rightX + CARD_WIDTH - 35, statusY + 8, ACCENT_YELLOW);
        
        // Safe State warning
        if (state instanceof SafeState) {
            SafeState safe = (SafeState) state;
            int warnY = statusY - 20;
            drawRect(leftX - 5, warnY, rightX + CARD_WIDTH + 5, warnY + 18, 0xFFFF0000);
            drawCenteredString(fontRendererObj, "§c§l⚠ " + safe.getReason().getMessage() + " §7[RSHIFT ile çık]", 
                centerX, warnY + 5, TEXT_WHITE);
        }
        
        GlStateManager.popMatrix();
        
        // Draw fields
        for (GuiTextField field : allFields) {
            field.drawTextBox();
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private void drawCard(int x, int y, int w, int h, String title, int accent) {
        // Background
        drawRect(x, y, x + w, y + h, CARD_BG);
        // Header
        drawRect(x, y, x + w, y + 20, CARD_HEADER);
        // Accent line
        drawRect(x, y + 19, x + w, y + 21, accent);
        // Title
        drawString(fontRendererObj, title, x + 10, y + 6, TEXT_WHITE);
    }
    
    private void drawLabel(String text, int x, int y) {
        drawString(fontRendererObj, text, x, y, TEXT_GRAY);
    }
    
    private int getStateColor(String name) {
        switch (name) {
            case "Mining": return ACCENT_ORANGE;
            case "Tamir": return ACCENT_ORANGE;
            case "AFK": return ACCENT_BLUE;
            case "Güvenli": return ACCENT_RED;
            default: return TEXT_GRAY;
        }
    }
    
    private int applyAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        ModConfig config = MuzMod.instance.getConfig();
        
        switch (button.id) {
            case 0: // Toggle Bot
                MuzMod.instance.toggleBot();
                btnToggleBot.displayString = MuzMod.instance.isBotEnabled() ? 
                    "§a● Bot Aktif" : "§c○ Bot Kapalı";
                break;
            case 1: // Idle
                MuzMod.instance.getStateManager().forceState("idle");
                break;
            case 2: // Mining
                MuzMod.instance.setBotEnabled(true);
                MuzMod.instance.getStateManager().forceState("mining");
                break;
            case 3: // AFK
                MuzMod.instance.setBotEnabled(true);
                MuzMod.instance.getStateManager().forceState("afk");
                break;
            case 4: // Repair (Debug)
                MuzMod.instance.setBotEnabled(true);
                MuzMod.instance.getStateManager().forceState("repair");
                break;
            case 10: // Block Lock
                config.setBlockLockEnabled(!config.isBlockLockEnabled());
                btnBlockLock.displayString = config.isBlockLockEnabled() ? 
                    "§a✓ Blok Kilidi Açık" : "§7✗ Blok Kilidi Kapalı";
                break;
            case 11: // Instant Flee
                config.setInstantFlee(!config.isInstantFlee());
                btnInstantFlee.displayString = config.isInstantFlee() ? 
                    "§a✓ Anında Kaç Açık" : "§7✗ Anında Kaç Kapalı";
                break;
            case 12: // Strafe
                config.setStrafeEnabled(!config.isStrafeEnabled());
                btnStrafeEnabled.displayString = config.isStrafeEnabled() ? 
                    "§a✓ Strafe Anti-AFK Açık" : "§7✗ Strafe Anti-AFK Kapalı";
                break;
            case 100: // Save
                saveConfig();
                mc.displayGuiScreen(null);
                break;
            case 101: // Close
                mc.displayGuiScreen(null);
                break;
        }
    }
    
    private void saveConfig() {
        ModConfig config = MuzMod.instance.getConfig();
        
        try {
            // Mining
            config.setMiningWarpCommand(txtMiningWarp.getText());
            parseTime(txtMiningStart.getText(), (h, m) -> config.setMiningStartTime(h, m));
            parseTime(txtMiningEnd.getText(), (h, m) -> config.setMiningEndTime(h, m));
            config.setInitialWalkDistance((int) parseDouble(txtWalkDist.getText(), 10));
            config.setPlayerDetectionRadius(parseDouble(txtDetectRadius.getText(), 15));
            
            // AFK
            config.setAfkWarpCommand(txtAfkWarp.getText());
            parseTime(txtAfkStart.getText(), (h, m) -> config.setAfkStartTime(h, m));
            parseTime(txtAfkEnd.getText(), (h, m) -> config.setAfkEndTime(h, m));
            
            // Repair
            config.setRepairDurabilityThreshold(parseInt(txtRepairThreshold.getText(), 100));
            config.setRepairClickDelay(parseFloat(txtRepairDelay.getText(), 2.0f));
            
            // Strafe
            config.setStrafeInterval(parseInt(txtStrafeInterval.getText(), 30) * 1000);
            config.setStrafeDuration(parseInt(txtStrafeDuration.getText(), 200));
            
            // Time offset
            config.setTimeOffsetHours(parseInt(txtTimeOffset.getText(), 0));
            
            config.save();
            MuzMod.LOGGER.info("[GUI] Config kaydedildi");
        } catch (Exception e) {
            MuzMod.LOGGER.error("[GUI] Config kaydetme hatası: " + e.getMessage());
        }
    }
    
    private void parseTime(String text, TimeConsumer consumer) {
        try {
            String[] parts = text.split(":");
            if (parts.length == 2) {
                int h = Integer.parseInt(parts[0].trim());
                int m = Integer.parseInt(parts[1].trim());
                consumer.accept(h, m);
            }
        } catch (Exception ignored) {}
    }
    
    private int parseInt(String text, int def) {
        try { return Integer.parseInt(text.trim()); } 
        catch (Exception e) { return def; }
    }
    
    private float parseFloat(String text, float def) {
        try { return Float.parseFloat(text.trim().replace(",", ".")); } 
        catch (Exception e) { return def; }
    }
    
    private double parseDouble(String text, double def) {
        try { return Double.parseDouble(text.trim().replace(",", ".")); } 
        catch (Exception e) { return def; }
    }
    
    @FunctionalInterface
    interface TimeConsumer {
        void accept(int hour, int minute);
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        
        for (GuiTextField field : allFields) {
            field.textboxKeyTyped(typedChar, keyCode);
        }
        
        super.keyTyped(typedChar, keyCode);
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for (GuiTextField field : allFields) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
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
