package com.muzmod.gui;

import com.muzmod.MuzMod;
import com.muzmod.account.AccountManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * Account Manager GUI
 * Offline login and restore functionality
 */
public class GuiAccountManager extends GuiScreen {
    
    private GuiScreen parentScreen;
    
    // Colors
    private static final int BG_PANEL = 0xFF1A1A1A;
    private static final int BG_HEADER = 0xFF202020;
    private static final int BG_FIELD = 0xFF151515;
    private static final int ACCENT_PURPLE = 0xFF9B59B6;
    private static final int ACCENT_CYAN = 0xFF00BCD4;
    private static final int ACCENT_GREEN = 0xFF4CAF50;
    private static final int ACCENT_RED = 0xFFE53935;
    private static final int ACCENT_ORANGE = 0xFFFF9800;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int TEXT_DARK = 0xFF666666;
    
    // Layout
    private int guiX, guiY;
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 180;
    
    // Components
    private GuiTextField usernameField;
    private String statusMessage = "";
    private int statusColor = TEXT_GRAY;
    
    public GuiAccountManager(GuiScreen parent) {
        this.parentScreen = parent;
    }
    
    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        
        guiX = (width - GUI_WIDTH) / 2;
        guiY = (height - GUI_HEIGHT) / 2;
        
        // Username field
        usernameField = new GuiTextField(0, fontRendererObj, guiX + 20, guiY + 65, GUI_WIDTH - 40, 20);
        usernameField.setMaxStringLength(16);
        usernameField.setFocused(true);
        usernameField.setEnableBackgroundDrawing(false);
        
        // Buttons
        buttonList.clear();
        buttonList.add(new GuiButton(1, guiX + 20, guiY + 100, 115, 20, "§aGiris Yap"));
        buttonList.add(new GuiButton(2, guiX + 145, guiY + 100, 115, 20, "§eRestore"));
        buttonList.add(new GuiButton(0, guiX + 20, guiY + 130, GUI_WIDTH - 40, 20, "§7Geri"));
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Dark background
        drawRect(0, 0, width, height, 0xC0000000);
        
        // Panel shadow
        drawRect(guiX + 3, guiY + 3, guiX + GUI_WIDTH + 3, guiY + GUI_HEIGHT + 3, 0x40000000);
        
        // Main panel
        drawRect(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, BG_PANEL);
        
        // Header
        drawRect(guiX, guiY, guiX + GUI_WIDTH, guiY + 30, BG_HEADER);
        drawGradientRectH(guiX, guiY + 29, guiX + GUI_WIDTH, guiY + 31, ACCENT_PURPLE, ACCENT_CYAN);
        
        // Title
        drawCenteredString(fontRendererObj, "§d§lAccount Manager", guiX + GUI_WIDTH / 2, guiY + 10, TEXT_WHITE);
        
        // Current account info
        AccountManager am = AccountManager.getInstance();
        String currentInfo = "§7Mevcut: §f" + am.getCurrentUsername();
        if (!am.isUsingOriginal()) {
            currentInfo += " §8(Offline)";
        }
        drawCenteredString(fontRendererObj, currentInfo, guiX + GUI_WIDTH / 2, guiY + 38, TEXT_WHITE);
        
        // Username label
        drawString(fontRendererObj, "§7Kullanici Adi:", guiX + 20, guiY + 53, TEXT_GRAY);
        
        // Username field background
        drawRect(guiX + 17, guiY + 62, guiX + GUI_WIDTH - 17, guiY + 88, BG_FIELD);
        if (usernameField.isFocused()) {
            drawRect(guiX + 17, guiY + 86, guiX + GUI_WIDTH - 17, guiY + 88, ACCENT_CYAN);
        }
        usernameField.drawTextBox();
        
        // Status message
        if (!statusMessage.isEmpty()) {
            drawCenteredString(fontRendererObj, statusMessage, guiX + GUI_WIDTH / 2, guiY + GUI_HEIGHT - 18, statusColor);
        }
        
        // Original account info
        String originalInfo = "§8Orijinal: " + am.getOriginalUsername();
        drawCenteredString(fontRendererObj, originalInfo, guiX + GUI_WIDTH / 2, guiY + GUI_HEIGHT - 8, TEXT_DARK);
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        AccountManager am = AccountManager.getInstance();
        
        switch (button.id) {
            case 0: // Geri
                mc.displayGuiScreen(parentScreen);
                break;
                
            case 1: // Giriş Yap
                String username = usernameField.getText().trim();
                if (username.isEmpty()) {
                    setStatus("§cKullanici adi bos olamaz!", ACCENT_RED);
                } else if (username.length() < 1 || username.length() > 16) {
                    setStatus("§cKullanici adi 1-16 karakter olmali!", ACCENT_RED);
                } else if (am.loginOffline(username)) {
                    setStatus("§aBasariyla giris yapildi: " + username, ACCENT_GREEN);
                    usernameField.setText("");
                } else {
                    setStatus("§cGiris basarisiz! Gecersiz karakter.", ACCENT_RED);
                }
                break;
                
            case 2: // Restore
                if (am.isUsingOriginal()) {
                    setStatus("§eZaten orijinal hesaptasiniz!", ACCENT_ORANGE);
                } else if (am.restoreOriginal()) {
                    setStatus("§aOrijinal hesaba donuldu!", ACCENT_GREEN);
                } else {
                    setStatus("§cRestore basarisiz!", ACCENT_RED);
                }
                break;
        }
    }
    
    private void setStatus(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parentScreen);
            return;
        }
        
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            // Enter ile giriş yap
            try {
                actionPerformed(buttonList.get(0)); // Giriş Yap butonu
            } catch (Exception e) {}
            return;
        }
        
        usernameField.textboxKeyTyped(typedChar, keyCode);
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        usernameField.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    @Override
    public void updateScreen() {
        usernameField.updateCursorCounter();
    }
    
    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
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
    
    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
