package com.muzmod.gui;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
import com.muzmod.state.IState;
import com.muzmod.state.impl.MiningState;
import com.muzmod.state.impl.SafeState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.muzmod.gui.ModernGuiUtils.*;

/**
 * MuzMod Modern GUI v2.0
 * 
 * Hile client tarzı modern, şık tasarım
 * - Gradient arka planlar
 * - Glow efektleri
 * - Hover animasyonları
 * - Modern butonlar
 */
public class MuzModGuiModern extends GuiScreen {
    
    // Layout
    private static final int GUI_WIDTH = 480;
    private static final int GUI_HEIGHT = 320;
    
    // Animation
    private long openTime;
    private float animProgress = 0f;
    
    // Tabs
    private int currentTab = 0;
    private static final String[] TAB_NAMES = {"⚙ Genel", "⛏ Maden", "⏸ AFK", "🔧 Tamir"};
    private static final int[] TAB_COLORS = {ACCENT_PURPLE, ACCENT_ORANGE, ACCENT_BLUE, ACCENT_YELLOW};
    
    // Buttons (custom handling)
    private List<ModernButton> buttons = new ArrayList<>();
    private List<GuiTextField> textFields = new ArrayList<>();
    
    // Dragging
    private boolean dragging = false;
    private int dragX, dragY;
    private int guiX, guiY;
    
    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        openTime = System.currentTimeMillis();
        animProgress = 0f;
        
        // Center GUI
        guiX = (width - GUI_WIDTH) / 2;
        guiY = (height - GUI_HEIGHT) / 2;
        
        setupButtons();
        setupTextFields();
    }
    
    private void setupButtons() {
        buttons.clear();
        ModConfig config = MuzMod.instance.getConfig();
        
        int baseY = guiY + 70;
        int leftCol = guiX + 20;
        int rightCol = guiX + 250;
        
        // Tab 0: Genel
        // Bot toggle
        boolean botOn = MuzMod.instance.isBotEnabled();
        buttons.add(new ModernButton(0, leftCol, baseY, 200, 28, 
            botOn ? "● Bot Aktif" : "○ Bot Kapalı", 
            botOn ? ACCENT_GREEN : ACCENT_RED));
        
        // Force state buttons
        buttons.add(new ModernButton(1, leftCol, baseY + 40, 95, 24, "Idle", BG_TERTIARY));
        buttons.add(new ModernButton(2, leftCol + 105, baseY + 40, 95, 24, "Mining", ACCENT_ORANGE));
        buttons.add(new ModernButton(3, leftCol, baseY + 70, 95, 24, "AFK", ACCENT_BLUE));
        buttons.add(new ModernButton(4, leftCol + 105, baseY + 70, 95, 24, "Repair", ACCENT_YELLOW));
        
        // Tab 1: Maden
        buttons.add(new ModernButton(10, rightCol, baseY, 200, 24,
            config.isBlockLockEnabled() ? "✓ Blok Kilidi" : "✗ Blok Kilidi",
            config.isBlockLockEnabled() ? ACCENT_GREEN : BG_TERTIARY));
        buttons.add(new ModernButton(11, rightCol, baseY + 30, 200, 24,
            config.isInstantFlee() ? "✓ Anında Kaç" : "✗ Anında Kaç",
            config.isInstantFlee() ? ACCENT_GREEN : BG_TERTIARY));
        
        // Tab 3: Tamir
        buttons.add(new ModernButton(20, rightCol, baseY + 60, 200, 24,
            config.isStrafeEnabled() ? "✓ Strafe Anti-AFK" : "✗ Strafe Anti-AFK",
            config.isStrafeEnabled() ? ACCENT_GREEN : BG_TERTIARY));
        
        // Save & Close
        buttons.add(new ModernButton(100, guiX + GUI_WIDTH - 180, guiY + GUI_HEIGHT - 40, 75, 28, "Kaydet", ACCENT_GREEN));
        buttons.add(new ModernButton(101, guiX + GUI_WIDTH - 95, guiY + GUI_HEIGHT - 40, 75, 28, "Kapat", ACCENT_RED));
    }
    
    private void setupTextFields() {
        textFields.clear();
        ModConfig config = MuzMod.instance.getConfig();
        
        int baseY = guiY + 110;
        int leftCol = guiX + 100;
        
        // Mining fields
        textFields.add(createField(leftCol, baseY, 120, config.getMiningWarpCommand(), 50)); // 0: mining warp
        textFields.add(createField(leftCol, baseY + 25, 50, 
            String.format("%02d:%02d", config.getMiningStartHour(), config.getMiningStartMinute()), 5)); // 1: mining start
        textFields.add(createField(leftCol + 60, baseY + 25, 50, 
            String.format("%02d:%02d", config.getMiningEndHour(), config.getMiningEndMinute()), 5)); // 2: mining end
        textFields.add(createField(leftCol, baseY + 50, 40, 
            String.valueOf(config.getInitialWalkDistance()), 3)); // 3: walk dist
        textFields.add(createField(leftCol, baseY + 75, 40, 
            String.valueOf((int)config.getPlayerDetectionRadius()), 3)); // 4: detect radius
        
        // AFK fields
        textFields.add(createField(leftCol + 150, baseY, 120, config.getAfkWarpCommand(), 50)); // 5: afk warp
        textFields.add(createField(leftCol + 150, baseY + 25, 50, 
            String.format("%02d:%02d", config.getAfkStartHour(), config.getAfkStartMinute()), 5)); // 6: afk start
        textFields.add(createField(leftCol + 210, baseY + 25, 50, 
            String.format("%02d:%02d", config.getAfkEndHour(), config.getAfkEndMinute()), 5)); // 7: afk end
        
        // Repair fields
        textFields.add(createField(leftCol + 150, baseY + 50, 40, 
            String.valueOf(config.getRepairDurabilityThreshold()), 4)); // 8: repair threshold
        textFields.add(createField(leftCol + 150, baseY + 75, 40, 
            String.format("%.1f", config.getRepairClickDelay()), 4)); // 9: repair delay
        
        // Time offset
        textFields.add(createField(guiX + GUI_WIDTH - 50, guiY + 35, 30, 
            String.valueOf(config.getTimeOffsetHours()), 3)); // 10: time offset
    }
    
    private GuiTextField createField(int x, int y, int w, String text, int maxLen) {
        GuiTextField field = new GuiTextField(textFields.size(), fontRendererObj, x, y, w, 16);
        field.setMaxStringLength(maxLen);
        field.setText(text);
        field.setEnableBackgroundDrawing(false);
        return field;
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Animation
        long elapsed = System.currentTimeMillis() - openTime;
        animProgress = Math.min(1f, elapsed / 200f);
        
        // Smooth easing
        float ease = 1f - (float) Math.pow(1 - animProgress, 3);
        
        // Dark overlay
        drawRect(0, 0, width, height, withAlpha(0x000000, (int)(180 * ease)));
        
        GlStateManager.pushMatrix();
        
        // Scale animation
        float scale = 0.8f + 0.2f * ease;
        GlStateManager.translate(guiX + GUI_WIDTH / 2f, guiY + GUI_HEIGHT / 2f, 0);
        GlStateManager.scale(scale, scale, 1);
        GlStateManager.translate(-(guiX + GUI_WIDTH / 2f), -(guiY + GUI_HEIGHT / 2f), 0);
        
        // Main window
        drawMainWindow(mouseX, mouseY);
        
        // Tabs
        drawTabs(mouseX, mouseY);
        
        // Content
        drawContent(mouseX, mouseY);
        
        // Status bar
        drawStatusBar();
        
        GlStateManager.popMatrix();
        
        // Draw text fields
        for (GuiTextField field : textFields) {
            // Custom field background
            drawRect(field.xPosition - 2, field.yPosition - 2, 
                field.width + 4, field.height + 4, BG_SECONDARY);
            drawOutline(field.xPosition - 2, field.yPosition - 2, 
                field.width + 4, field.height + 4, 1, 
                field.isFocused() ? ACCENT_PURPLE : BORDER_COLOR);
            field.drawTextBox();
        }
        
        // Draw buttons
        for (ModernButton btn : buttons) {
            btn.draw(mouseX, mouseY, fontRendererObj);
        }
    }
    
    private void drawMainWindow(int mouseX, int mouseY) {
        // Shadow
        drawShadow(guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 10);
        
        // Background gradient
        drawGradientV(guiX, guiY, GUI_WIDTH, GUI_HEIGHT, BG_PRIMARY, darken(BG_PRIMARY, 10));
        
        // Border
        drawOutline(guiX, guiY, GUI_WIDTH, GUI_HEIGHT, 1, BORDER_ACCENT);
        
        // Header
        drawGradientH(guiX, guiY, GUI_WIDTH, 50, 
            withAlpha(ACCENT_PURPLE, 40), withAlpha(ACCENT_CYAN, 40));
        
        // Title with glow effect
        String title = "§l§dMUZ§fMOD";
        int titleX = guiX + 15;
        int titleY = guiY + 12;
        
        // Title glow
        drawGlow(titleX - 5, titleY - 3, fontRendererObj.getStringWidth("MUZMOD") + 10, 14, ACCENT_PURPLE, 5);
        fontRendererObj.drawStringWithShadow(title, titleX, titleY, TEXT_PRIMARY);
        
        // Version
        fontRendererObj.drawString("§7v2.0", titleX + 65, titleY + 2, TEXT_TERTIARY);
        
        // Time display
        ModConfig config = MuzMod.instance.getConfig();
        Calendar cal = Calendar.getInstance();
        int hour = (cal.get(Calendar.HOUR_OF_DAY) + config.getTimeOffsetHours() + 24) % 24;
        String timeStr = String.format("%02d:%02d:%02d", hour, cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
        
        int timeX = guiX + GUI_WIDTH - 80;
        fontRendererObj.drawStringWithShadow("§e" + timeStr, timeX, guiY + 15, ACCENT_YELLOW);
        fontRendererObj.drawString("§8UTC", timeX + 50, guiY + 15, TEXT_TERTIARY);
        
        // Header separator with gradient
        drawGradientH(guiX, guiY + 49, GUI_WIDTH, 1, ACCENT_PURPLE, ACCENT_CYAN);
    }
    
    private void drawTabs(int mouseX, int mouseY) {
        int tabWidth = (GUI_WIDTH - 20) / TAB_NAMES.length;
        int tabY = guiY + 52;
        
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int tabX = guiX + 10 + i * tabWidth;
            boolean hovered = mouseX >= tabX && mouseX < tabX + tabWidth - 2 && 
                             mouseY >= tabY && mouseY < tabY + 22;
            boolean selected = currentTab == i;
            
            // Tab background
            int bgColor = selected ? TAB_COLORS[i] : (hovered ? BG_CARD_HOVER : BG_TERTIARY);
            drawRect(tabX, tabY, tabWidth - 2, 22, withAlpha(bgColor, selected ? 255 : 180));
            
            // Tab glow when selected
            if (selected) {
                drawGlow(tabX, tabY, tabWidth - 2, 22, TAB_COLORS[i], 3);
            }
            
            // Tab text
            String text = TAB_NAMES[i];
            int textX = tabX + (tabWidth - 2 - fontRendererObj.getStringWidth(text)) / 2;
            fontRendererObj.drawStringWithShadow(text, textX, tabY + 7, 
                selected ? TEXT_PRIMARY : TEXT_SECONDARY);
        }
    }
    
    private void drawContent(int mouseX, int mouseY) {
        int contentY = guiY + 80;
        int leftCol = guiX + 20;
        int rightCol = guiX + 250;
        
        switch (currentTab) {
            case 0: // Genel
                drawGeneralTab(leftCol, contentY, mouseX, mouseY);
                break;
            case 1: // Maden
                drawMiningTab(leftCol, contentY, mouseX, mouseY);
                break;
            case 2: // AFK
                drawAfkTab(leftCol, contentY, mouseX, mouseY);
                break;
            case 3: // Tamir
                drawRepairTab(leftCol, contentY, mouseX, mouseY);
                break;
        }
    }
    
    private void drawGeneralTab(int x, int y, int mouseX, int mouseY) {
        // Bot Status Card
        drawModernCard(x, y - 15, 210, 115, "§d◆ Bot Kontrolü", ACCENT_PURPLE, fontRendererObj);
        
        // Current state info
        IState state = MuzMod.instance.getStateManager().getCurrentState();
        String stateName = state != null ? state.getName() : "Idle";
        String statusText = state != null ? state.getStatus() : "";
        
        fontRendererObj.drawString("§7Mevcut Durum:", x + 10, y + 105, TEXT_SECONDARY);
        fontRendererObj.drawStringWithShadow("§f" + stateName, x + 90, y + 105, getStateColor(stateName));
        
        if (!statusText.isEmpty()) {
            fontRendererObj.drawString("§8" + truncate(statusText, 30), x + 10, y + 118, TEXT_TERTIARY);
        }
        
        // Pickaxe info
        drawPickaxeInfo(x + 230, y - 15);
    }
    
    private void drawMiningTab(int x, int y, int mouseX, int mouseY) {
        drawModernCard(x, y - 15, 430, 180, "§6◆ Maden Ayarları", ACCENT_ORANGE, fontRendererObj);
        
        fontRendererObj.drawString("§7Warp Komutu:", x + 10, y + 18, TEXT_SECONDARY);
        fontRendererObj.drawString("§7Başlangıç:", x + 10, y + 43, TEXT_SECONDARY);
        fontRendererObj.drawString("§7Bitiş:", x + 130, y + 43, TEXT_SECONDARY);
        fontRendererObj.drawString("§7Yürüme (blok):", x + 10, y + 68, TEXT_SECONDARY);
        fontRendererObj.drawString("§7Tespit R (blok):", x + 10, y + 93, TEXT_SECONDARY);
    }
    
    private void drawAfkTab(int x, int y, int mouseX, int mouseY) {
        drawModernCard(x, y - 15, 430, 180, "§b◆ AFK Ayarları", ACCENT_BLUE, fontRendererObj);
        
        fontRendererObj.drawString("§7Warp Komutu:", x + 160, y + 18, TEXT_SECONDARY);
        fontRendererObj.drawString("§7Başlangıç:", x + 160, y + 43, TEXT_SECONDARY);
        fontRendererObj.drawString("§7Bitiş:", x + 280, y + 43, TEXT_SECONDARY);
    }
    
    private void drawRepairTab(int x, int y, int mouseX, int mouseY) {
        drawModernCard(x, y - 15, 430, 180, "§e◆ Tamir Ayarları", ACCENT_YELLOW, fontRendererObj);
        
        fontRendererObj.drawString("§7Durability Eşiği:", x + 160, y + 68, TEXT_SECONDARY);
        fontRendererObj.drawString("§7Tıklama Arası (sn):", x + 160, y + 93, TEXT_SECONDARY);
    }
    
    private void drawPickaxeInfo(int x, int y) {
        drawModernCard(x, y, 200, 115, "§e◆ Kazma Durumu", ACCENT_YELLOW, fontRendererObj);
        
        if (mc.thePlayer != null) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held != null && held.getItem() instanceof ItemPickaxe) {
                int max = held.getMaxDamage();
                int damage = held.getItemDamage();
                int remaining = max - damage;
                float percent = (float) remaining / max;
                
                String name = held.getDisplayName();
                fontRendererObj.drawString("§f" + truncate(name, 25), x + 10, y + 30, TEXT_PRIMARY);
                
                // Progress bar
                int barColor = percent > 0.5f ? ACCENT_GREEN : (percent > 0.25f ? ACCENT_YELLOW : ACCENT_RED);
                drawProgressBar(x + 10, y + 50, 180, 12, percent, barColor);
                
                // Durability text
                String durText = remaining + " / " + max;
                fontRendererObj.drawString("§7" + durText, x + 10, y + 70, TEXT_SECONDARY);
                fontRendererObj.drawString(String.format("§f%.1f%%", percent * 100), x + 140, y + 70, TEXT_PRIMARY);
            } else {
                fontRendererObj.drawString("§cKazma yok!", x + 10, y + 45, ACCENT_RED);
            }
        }
    }
    
    private void drawStatusBar() {
        int barY = guiY + GUI_HEIGHT - 25;
        
        // Background
        drawGradientH(guiX, barY, GUI_WIDTH, 25, 
            withAlpha(ACCENT_PURPLE, 30), withAlpha(ACCENT_CYAN, 30));
        
        // Top border
        drawGradientH(guiX, barY, GUI_WIDTH, 1, ACCENT_PURPLE, ACCENT_CYAN);
        
        // Safe state warning
        IState state = MuzMod.instance.getStateManager().getCurrentState();
        if (state instanceof SafeState) {
            SafeState safe = (SafeState) state;
            // Pulsing warning
            float pulse = (float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.5 + 0.5);
            int warnColor = lerpColor(ACCENT_RED, ACCENT_YELLOW, pulse);
            
            drawRect(guiX + 10, barY + 5, GUI_WIDTH - 20, 15, withAlpha(warnColor, 60));
            fontRendererObj.drawStringWithShadow("§c⚠ " + safe.getReason().getMessage() + " §7[RSHIFT]", 
                guiX + 20, barY + 8, TEXT_PRIMARY);
        } else {
            fontRendererObj.drawString("§7Powered by §dMuzMod §7| §8github.com/Crefax/MuzClient", 
                guiX + 15, barY + 8, TEXT_TERTIARY);
        }
    }
    
    private int getStateColor(String name) {
        switch (name) {
            case "Mining": return ACCENT_ORANGE;
            case "Tamir": return ACCENT_YELLOW;
            case "AFK": return ACCENT_BLUE;
            case "Güvenli": return ACCENT_RED;
            default: return TEXT_SECONDARY;
        }
    }
    
    private String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        // Tab clicks
        int tabWidth = (GUI_WIDTH - 20) / TAB_NAMES.length;
        int tabY = guiY + 52;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int tabX = guiX + 10 + i * tabWidth;
            if (mouseX >= tabX && mouseX < tabX + tabWidth - 2 && 
                mouseY >= tabY && mouseY < tabY + 22) {
                currentTab = i;
                return;
            }
        }
        
        // Button clicks
        for (ModernButton btn : buttons) {
            if (btn.isHovered(mouseX, mouseY)) {
                handleButtonClick(btn.id);
                return;
            }
        }
        
        // Text field clicks
        for (GuiTextField field : textFields) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
        
        // Dragging header
        if (mouseY >= guiY && mouseY <= guiY + 50 && mouseX >= guiX && mouseX <= guiX + GUI_WIDTH) {
            dragging = true;
            dragX = mouseX - guiX;
            dragY = mouseY - guiY;
        }
    }
    
    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        dragging = false;
    }
    
    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (dragging) {
            guiX = mouseX - dragX;
            guiY = mouseY - dragY;
            setupButtons();
            setupTextFields();
        }
    }
    
    private void handleButtonClick(int id) {
        ModConfig config = MuzMod.instance.getConfig();
        
        switch (id) {
            case 0: // Toggle Bot
                MuzMod.instance.toggleBot();
                setupButtons();
                break;
            case 1: // Idle
                MuzMod.instance.getStateManager().forceState("idle");
                break;
            case 2: // Mining
                MuzMod.instance.getStateManager().forceState("mining");
                break;
            case 3: // AFK
                MuzMod.instance.getStateManager().forceState("afk");
                break;
            case 4: // Repair
                MuzMod.instance.getStateManager().forceState("repair");
                break;
            case 10: // Block Lock
                config.setBlockLockEnabled(!config.isBlockLockEnabled());
                setupButtons();
                break;
            case 11: // Instant Flee
                config.setInstantFlee(!config.isInstantFlee());
                setupButtons();
                break;
            case 20: // Strafe
                config.setStrafeEnabled(!config.isStrafeEnabled());
                setupButtons();
                break;
            case 100: // Save
                saveSettings();
                break;
            case 101: // Close
                mc.displayGuiScreen(null);
                break;
        }
    }
    
    private void saveSettings() {
        ModConfig config = MuzMod.instance.getConfig();
        
        try {
            config.setMiningWarpCommand(textFields.get(0).getText());
            parseAndSetTime(textFields.get(1).getText(), true, true);
            parseAndSetTime(textFields.get(2).getText(), true, false);
            config.setInitialWalkDistance(Integer.parseInt(textFields.get(3).getText()));
            config.setPlayerDetectionRadius(Double.parseDouble(textFields.get(4).getText()));
            
            config.setAfkWarpCommand(textFields.get(5).getText());
            parseAndSetTime(textFields.get(6).getText(), false, true);
            parseAndSetTime(textFields.get(7).getText(), false, false);
            
            config.setRepairDurabilityThreshold(Integer.parseInt(textFields.get(8).getText()));
            config.setRepairClickDelay(Float.parseFloat(textFields.get(9).getText()));
            
            config.setTimeOffsetHours(Integer.parseInt(textFields.get(10).getText()));
            
            config.save();
            MuzMod.LOGGER.info("[GUI] Ayarlar kaydedildi");
        } catch (Exception e) {
            MuzMod.LOGGER.error("[GUI] Kaydetme hatası: " + e.getMessage());
        }
    }
    
    private void parseAndSetTime(String text, boolean mining, boolean start) {
        ModConfig config = MuzMod.instance.getConfig();
        String[] parts = text.split(":");
        if (parts.length == 2) {
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (mining) {
                if (start) { config.setMiningStartTime(h, m); }
                else { config.setMiningEndTime(h, m); }
            } else {
                if (start) { config.setAfkStartTime(h, m); }
                else { config.setAfkEndTime(h, m); }
            }
        }
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        
        for (GuiTextField field : textFields) {
            field.textboxKeyTyped(typedChar, keyCode);
        }
    }
    
    @Override
    public void updateScreen() {
        for (GuiTextField field : textFields) {
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
    
    // === MODERN BUTTON CLASS ===
    private class ModernButton {
        int id, x, y, width, height;
        String text;
        int color;
        
        ModernButton(int id, int x, int y, int w, int h, String text, int color) {
            this.id = id; this.x = x; this.y = y; 
            this.width = w; this.height = h;
            this.text = text; this.color = color;
        }
        
        void draw(int mouseX, int mouseY, net.minecraft.client.gui.FontRenderer font) {
            boolean hovered = isHovered(mouseX, mouseY);
            drawModernButton(x, y, width, height, text, BG_CARD, color, hovered, font);
        }
        
        boolean isHovered(int mx, int my) {
            return mx >= x && mx < x + width && my >= y && my < y + height;
        }
    }
}
