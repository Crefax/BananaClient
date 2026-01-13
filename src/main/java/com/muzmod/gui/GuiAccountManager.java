package com.muzmod.gui;

import com.muzmod.MuzMod;
import com.muzmod.account.AccountManager;
import com.muzmod.account.AltAccount;
import com.muzmod.account.AltManager;
import com.muzmod.account.ProxyManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/**
 * Account Manager GUI - Alt hesap listesi ve proxy desteği
 */
public class GuiAccountManager extends GuiScreen {
    
    private GuiScreen parentScreen;
    
    // Colors
    private static final int BG_PANEL = 0xFF1A1A1A;
    private static final int BG_HEADER = 0xFF202020;
    private static final int BG_FIELD = 0xFF151515;
    private static final int BG_LIST_ITEM = 0xFF252525;
    private static final int BG_LIST_HOVER = 0xFF303030;
    private static final int BG_LIST_SELECTED = 0xFF3A3A3A;
    private static final int ACCENT_PURPLE = 0xFF9B59B6;
    private static final int ACCENT_CYAN = 0xFF00BCD4;
    private static final int ACCENT_GREEN = 0xFF4CAF50;
    private static final int ACCENT_RED = 0xFFE53935;
    private static final int ACCENT_ORANGE = 0xFFFF9800;
    private static final int ACCENT_YELLOW = 0xFFFFEB3B;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int TEXT_DARK = 0xFF666666;
    
    // Layout
    private int guiX, guiY;
    private static final int GUI_WIDTH = 400;
    private static final int GUI_HEIGHT = 300;
    
    // Alt list
    private int selectedAltIndex = -1;
    private int listScrollOffset = 0;
    private static final int LIST_ITEM_HEIGHT = 28;
    private static final int VISIBLE_ITEMS = 6;
    
    // View mode: 0 = list, 1 = add/edit form
    private int viewMode = 0;
    private boolean editingExisting = false;
    
    // Form fields
    private GuiTextField usernameField;
    private GuiTextField proxyHostField;
    private GuiTextField proxyPortField;
    private GuiTextField proxyUserField;
    private GuiTextField proxyPassField;
    private boolean proxyEnabled = false;
    
    // Quick login field
    private GuiTextField quickLoginField;
    
    // Status
    private String statusMessage = "";
    private int statusColor = TEXT_GRAY;
    private long statusTime = 0;
    
    public GuiAccountManager(GuiScreen parent) {
        this.parentScreen = parent;
    }
    
    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        
        guiX = (width - GUI_WIDTH) / 2;
        guiY = (height - GUI_HEIGHT) / 2;
        
        // Quick login field (list view)
        quickLoginField = new GuiTextField(10, fontRendererObj, guiX + 15, guiY + 238, 120, 16);
        quickLoginField.setMaxStringLength(16);
        quickLoginField.setEnableBackgroundDrawing(false);
        
        // Form fields
        usernameField = new GuiTextField(0, fontRendererObj, guiX + 180, guiY + 70, 180, 16);
        usernameField.setMaxStringLength(16);
        usernameField.setEnableBackgroundDrawing(false);
        
        proxyHostField = new GuiTextField(1, fontRendererObj, guiX + 180, guiY + 130, 120, 16);
        proxyHostField.setMaxStringLength(64);
        proxyHostField.setEnableBackgroundDrawing(false);
        
        proxyPortField = new GuiTextField(2, fontRendererObj, guiX + 310, guiY + 130, 50, 16);
        proxyPortField.setMaxStringLength(5);
        proxyPortField.setEnableBackgroundDrawing(false);
        proxyPortField.setText("1080");
        
        proxyUserField = new GuiTextField(3, fontRendererObj, guiX + 180, guiY + 180, 180, 16);
        proxyUserField.setMaxStringLength(64);
        proxyUserField.setEnableBackgroundDrawing(false);
        
        proxyPassField = new GuiTextField(4, fontRendererObj, guiX + 180, guiY + 210, 180, 16);
        proxyPassField.setMaxStringLength(64);
        proxyPassField.setEnableBackgroundDrawing(false);
        
        updateButtons();
    }
    
    private void updateButtons() {
        buttonList.clear();
        
        if (viewMode == 0) {
            // List view buttons
            buttonList.add(new GuiButton(1, guiX + 270, guiY + 45, 115, 20, "§a+ Hesap Ekle"));
            buttonList.add(new GuiButton(2, guiX + 270, guiY + 70, 115, 20, "§cSil"));
            buttonList.add(new GuiButton(3, guiX + 270, guiY + 95, 115, 20, "§eDuzenle"));
            buttonList.add(new GuiButton(4, guiX + 270, guiY + 130, 115, 20, "§bGiris Yap"));
            buttonList.add(new GuiButton(5, guiX + 270, guiY + 155, 115, 20, "§6Orijinale Don"));
            buttonList.add(new GuiButton(6, guiX + 145, guiY + 235, 60, 20, "§aGiris"));
            buttonList.add(new GuiButton(0, guiX + 270, guiY + 235, 115, 20, "§7Kapat"));
        } else {
            // Form view buttons
            buttonList.add(new GuiButton(10, guiX + 40, guiY + 250, 100, 20, editingExisting ? "§aKaydet" : "§aEkle"));
            buttonList.add(new GuiButton(11, guiX + 150, guiY + 250, 100, 20, "§7Iptal"));
            buttonList.add(new GuiButton(12, guiX + 260, guiY + 250, 100, 20, "§eProxy Test"));
        }
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Dark background
        drawRect(0, 0, width, height, 0xC0000000);
        
        // Panel shadow
        drawRect(guiX + 4, guiY + 4, guiX + GUI_WIDTH + 4, guiY + GUI_HEIGHT + 4, 0x40000000);
        
        // Main panel
        drawRect(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, BG_PANEL);
        
        // Header
        drawRect(guiX, guiY, guiX + GUI_WIDTH, guiY + 35, BG_HEADER);
        drawGradientRectH(guiX, guiY + 34, guiX + GUI_WIDTH, guiY + 36, ACCENT_PURPLE, ACCENT_CYAN);
        
        // Title
        drawCenteredString(fontRendererObj, "§d§lAccount Manager", guiX + GUI_WIDTH / 2, guiY + 8, TEXT_WHITE);
        
        // Current account info
        AccountManager am = AccountManager.getInstance();
        String currentInfo = "§7Aktif: §f" + am.getCurrentUsername();
        if (!am.isUsingOriginal()) {
            currentInfo += " §8(Offline)";
        }
        
        // Proxy status
        ProxyManager pm = ProxyManager.getInstance();
        if (pm.isProxyEnabled()) {
            currentInfo += " §d[Proxy: " + pm.getProxyString() + "]";
        }
        
        drawCenteredString(fontRendererObj, currentInfo, guiX + GUI_WIDTH / 2, guiY + 21, TEXT_WHITE);
        
        if (viewMode == 0) {
            drawListView(mouseX, mouseY);
        } else {
            drawFormView(mouseX, mouseY);
        }
        
        // Status message (fade out after 3 seconds)
        if (!statusMessage.isEmpty() && System.currentTimeMillis() - statusTime < 3000) {
            drawCenteredString(fontRendererObj, statusMessage, guiX + GUI_WIDTH / 2, guiY + GUI_HEIGHT - 12, statusColor);
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private void drawListView(int mouseX, int mouseY) {
        AltManager altManager = AltManager.getInstance();
        List<AltAccount> accounts = altManager.getAccounts();
        
        // Alt list panel
        int listX = guiX + 10;
        int listY = guiY + 45;
        int listW = 250;
        int listH = VISIBLE_ITEMS * LIST_ITEM_HEIGHT;
        
        drawRect(listX, listY, listX + listW, listY + listH, BG_FIELD);
        
        // List header
        drawString(fontRendererObj, "§7Hesaplar (" + accounts.size() + ")", listX + 5, listY - 10, TEXT_GRAY);
        
        // Draw accounts
        int y = listY + 2;
        for (int i = listScrollOffset; i < Math.min(accounts.size(), listScrollOffset + VISIBLE_ITEMS); i++) {
            AltAccount acc = accounts.get(i);
            
            // Background
            int bgColor = BG_LIST_ITEM;
            if (i == selectedAltIndex) {
                bgColor = BG_LIST_SELECTED;
            } else if (mouseX >= listX + 2 && mouseX < listX + listW - 2 && mouseY >= y && mouseY < y + LIST_ITEM_HEIGHT - 2) {
                bgColor = BG_LIST_HOVER;
            }
            drawRect(listX + 2, y, listX + listW - 2, y + LIST_ITEM_HEIGHT - 2, bgColor);
            
            // Username
            String name = acc.getUsername();
            if (acc.isProxyEnabled()) {
                name = "§d[P] §f" + name;
            }
            drawString(fontRendererObj, name, listX + 8, y + 5, TEXT_WHITE);
            
            // Proxy info
            if (acc.isProxyEnabled()) {
                drawString(fontRendererObj, "§8" + acc.getProxyString(), listX + 8, y + 16, TEXT_DARK);
            } else {
                drawString(fontRendererObj, "§8Proxy Yok", listX + 8, y + 16, TEXT_DARK);
            }
            
            y += LIST_ITEM_HEIGHT;
        }
        
        // Scroll indicator
        if (accounts.size() > VISIBLE_ITEMS) {
            int scrollBarH = listH - 10;
            int scrollThumbH = Math.max(20, scrollBarH * VISIBLE_ITEMS / accounts.size());
            int scrollThumbY = listY + 5 + (scrollBarH - scrollThumbH) * listScrollOffset / Math.max(1, accounts.size() - VISIBLE_ITEMS);
            
            drawRect(listX + listW - 6, listY + 5, listX + listW - 2, listY + listH - 5, 0xFF333333);
            drawRect(listX + listW - 6, scrollThumbY, listX + listW - 2, scrollThumbY + scrollThumbH, ACCENT_PURPLE);
        }
        
        // Quick login section
        drawRect(guiX + 10, guiY + 230, guiX + 210, guiY + 260, BG_FIELD);
        drawString(fontRendererObj, "§7Hizli Giris:", guiX + 15, guiY + 220, TEXT_GRAY);
        
        // Quick login field background
        drawRect(guiX + 12, guiY + 235, guiX + 140, guiY + 257, BG_LIST_ITEM);
        quickLoginField.drawTextBox();
        
        // Info text
        drawString(fontRendererObj, "§8Orijinal: " + AccountManager.getInstance().getOriginalUsername(), guiX + 10, guiY + 268, TEXT_DARK);
    }
    
    private void drawFormView(int mouseX, int mouseY) {
        // Form title
        String formTitle = editingExisting ? "§e§lHesap Duzenle" : "§a§lYeni Hesap Ekle";
        drawCenteredString(fontRendererObj, formTitle, guiX + GUI_WIDTH / 2, guiY + 45, TEXT_WHITE);
        
        // Username section
        drawString(fontRendererObj, "§7Kullanici Adi:", guiX + 40, guiY + 72, TEXT_GRAY);
        drawRect(guiX + 175, guiY + 67, guiX + 365, guiY + 89, BG_FIELD);
        usernameField.drawTextBox();
        
        // Proxy toggle
        int toggleX = guiX + 40;
        int toggleY = guiY + 100;
        drawString(fontRendererObj, "§7Proxy Kullan:", toggleX, toggleY + 2, TEXT_GRAY);
        
        int checkX = guiX + 175;
        drawRect(checkX, toggleY, checkX + 20, toggleY + 20, BG_FIELD);
        if (proxyEnabled) {
            drawRect(checkX + 2, toggleY + 2, checkX + 18, toggleY + 18, ACCENT_GREEN);
            drawCenteredString(fontRendererObj, "§l✓", checkX + 10, toggleY + 6, TEXT_WHITE);
        }
        
        // Proxy fields (only if enabled)
        if (proxyEnabled) {
            // Host & Port
            drawString(fontRendererObj, "§7Host:", guiX + 40, guiY + 132, TEXT_GRAY);
            drawRect(guiX + 175, guiY + 127, guiX + 300, guiY + 149, BG_FIELD);
            proxyHostField.drawTextBox();
            
            drawString(fontRendererObj, "§7Port:", guiX + 305, guiY + 132, TEXT_GRAY);
            drawRect(guiX + 305, guiY + 127, guiX + 365, guiY + 149, BG_FIELD);
            proxyPortField.drawTextBox();
            
            // Optional auth
            drawString(fontRendererObj, "§8(Opsiyonel)", guiX + 40, guiY + 160, TEXT_DARK);
            
            drawString(fontRendererObj, "§7Kullanici:", guiX + 40, guiY + 182, TEXT_GRAY);
            drawRect(guiX + 175, guiY + 177, guiX + 365, guiY + 199, BG_FIELD);
            proxyUserField.drawTextBox();
            
            drawString(fontRendererObj, "§7Sifre:", guiX + 40, guiY + 212, TEXT_GRAY);
            drawRect(guiX + 175, guiY + 207, guiX + 365, guiY + 229, BG_FIELD);
            proxyPassField.drawTextBox();
        } else {
            drawString(fontRendererObj, "§8Proxy devre disi. Direkt baglanilacak.", guiX + 40, guiY + 135, TEXT_DARK);
        }
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (viewMode == 0) {
            handleListAction(button.id);
        } else {
            handleFormAction(button.id);
        }
    }
    
    private void handleListAction(int buttonId) {
        AltManager altManager = AltManager.getInstance();
        
        switch (buttonId) {
            case 0: // Kapat
                mc.displayGuiScreen(parentScreen);
                break;
                
            case 1: // Hesap Ekle
                viewMode = 1;
                editingExisting = false;
                resetFormFields();
                updateButtons();
                break;
                
            case 2: // Sil
                if (selectedAltIndex >= 0 && selectedAltIndex < altManager.getAccountCount()) {
                    String name = altManager.getAccount(selectedAltIndex).getUsername();
                    altManager.removeAccount(selectedAltIndex);
                    selectedAltIndex = -1;
                    setStatus("§c" + name + " silindi!", ACCENT_RED);
                } else {
                    setStatus("§cOnce bir hesap secin!", ACCENT_RED);
                }
                break;
                
            case 3: // Düzenle
                if (selectedAltIndex >= 0 && selectedAltIndex < altManager.getAccountCount()) {
                    AltAccount acc = altManager.getAccount(selectedAltIndex);
                    viewMode = 1;
                    editingExisting = true;
                    loadAccountToForm(acc);
                    updateButtons();
                } else {
                    setStatus("§cOnce bir hesap secin!", ACCENT_RED);
                }
                break;
                
            case 4: // Giriş Yap
                if (selectedAltIndex >= 0 && selectedAltIndex < altManager.getAccountCount()) {
                    AltAccount acc = altManager.getAccount(selectedAltIndex);
                    if (altManager.login(acc)) {
                        setStatus("§a" + acc.getUsername() + " ile giris yapildi!", ACCENT_GREEN);
                    } else {
                        setStatus("§cGiris basarisiz!", ACCENT_RED);
                    }
                } else {
                    setStatus("§cOnce bir hesap secin!", ACCENT_RED);
                }
                break;
                
            case 5: // Orijinale Dön
                if (AccountManager.getInstance().isUsingOriginal()) {
                    setStatus("§eZaten orijinal hesaptasiniz!", ACCENT_ORANGE);
                } else {
                    AccountManager.getInstance().restoreOriginal();
                    ProxyManager.getInstance().disableProxy();
                    altManager.clearActiveAccount();
                    setStatus("§aOrijinal hesaba donuldu!", ACCENT_GREEN);
                }
                break;
                
            case 6: // Hızlı Giriş
                String quickName = quickLoginField.getText().trim();
                if (!quickName.isEmpty()) {
                    // Proxy'siz hızlı giriş
                    ProxyManager.getInstance().disableProxy();
                    if (AccountManager.getInstance().loginOffline(quickName)) {
                        setStatus("§a" + quickName + " ile giris yapildi!", ACCENT_GREEN);
                        quickLoginField.setText("");
                    } else {
                        setStatus("§cGecersiz kullanici adi!", ACCENT_RED);
                    }
                }
                break;
        }
    }
    
    private void handleFormAction(int buttonId) {
        AltManager altManager = AltManager.getInstance();
        
        switch (buttonId) {
            case 10: // Ekle/Kaydet
                String username = usernameField.getText().trim();
                if (username.isEmpty() || username.length() > 16) {
                    setStatus("§cGecersiz kullanici adi!", ACCENT_RED);
                    return;
                }
                
                AltAccount newAcc = new AltAccount(username);
                
                if (proxyEnabled) {
                    String host = proxyHostField.getText().trim();
                    String portStr = proxyPortField.getText().trim();
                    
                    if (host.isEmpty()) {
                        setStatus("§cProxy host bos olamaz!", ACCENT_RED);
                        return;
                    }
                    
                    int port;
                    try {
                        port = Integer.parseInt(portStr);
                        if (port < 1 || port > 65535) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        setStatus("§cGecersiz port numarasi!", ACCENT_RED);
                        return;
                    }
                    
                    newAcc.setProxyEnabled(true);
                    newAcc.setProxyHost(host);
                    newAcc.setProxyPort(port);
                    newAcc.setProxyUsername(proxyUserField.getText().trim());
                    newAcc.setProxyPassword(proxyPassField.getText().trim());
                }
                
                altManager.addAccount(newAcc);
                setStatus(editingExisting ? "§aHesap guncellendi!" : "§aHesap eklendi!", ACCENT_GREEN);
                
                viewMode = 0;
                updateButtons();
                break;
                
            case 11: // İptal
                viewMode = 0;
                updateButtons();
                break;
                
            case 12: // Proxy Test
                if (!proxyEnabled) {
                    setStatus("§cProxy devre disi!", ACCENT_ORANGE);
                    return;
                }
                
                String testHost = proxyHostField.getText().trim();
                String testPortStr = proxyPortField.getText().trim();
                
                if (testHost.isEmpty()) {
                    setStatus("§cHost bos!", ACCENT_RED);
                    return;
                }
                
                int testPort;
                try {
                    testPort = Integer.parseInt(testPortStr);
                } catch (NumberFormatException e) {
                    setStatus("§cGecersiz port!", ACCENT_RED);
                    return;
                }
                
                setStatus("§eProxy test ediliyor...", ACCENT_YELLOW);
                
                // Test in background
                final int finalTestPort = testPort;
                new Thread(() -> {
                    ProxyManager pm = ProxyManager.getInstance();
                    pm.setProxy(testHost, finalTestPort, proxyUserField.getText().trim(), proxyPassField.getText().trim());
                    
                    if (pm.testConnection()) {
                        setStatus("§aProxy calisiyor!", ACCENT_GREEN);
                    } else {
                        setStatus("§cProxy baglantisi basarisiz!", ACCENT_RED);
                    }
                }).start();
                break;
        }
    }
    
    private void resetFormFields() {
        usernameField.setText("");
        proxyEnabled = false;
        proxyHostField.setText("");
        proxyPortField.setText("1080");
        proxyUserField.setText("");
        proxyPassField.setText("");
        usernameField.setFocused(true);
    }
    
    private void loadAccountToForm(AltAccount acc) {
        usernameField.setText(acc.getUsername());
        proxyEnabled = acc.isProxyEnabled();
        
        if (acc.isProxyEnabled()) {
            proxyHostField.setText(acc.getProxyHost() != null ? acc.getProxyHost() : "");
            proxyPortField.setText(String.valueOf(acc.getProxyPort()));
            proxyUserField.setText(acc.getProxyUsername() != null ? acc.getProxyUsername() : "");
            proxyPassField.setText(acc.getProxyPassword() != null ? acc.getProxyPassword() : "");
        } else {
            proxyHostField.setText("");
            proxyPortField.setText("1080");
            proxyUserField.setText("");
            proxyPassField.setText("");
        }
        
        usernameField.setFocused(true);
    }
    
    private void setStatus(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
        this.statusTime = System.currentTimeMillis();
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (viewMode == 1) {
                viewMode = 0;
                updateButtons();
            } else {
                mc.displayGuiScreen(parentScreen);
            }
            return;
        }
        
        if (viewMode == 0) {
            quickLoginField.textboxKeyTyped(typedChar, keyCode);
            
            // Enter for quick login
            if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) && quickLoginField.isFocused()) {
                handleListAction(6);
            }
        } else {
            usernameField.textboxKeyTyped(typedChar, keyCode);
            if (proxyEnabled) {
                proxyHostField.textboxKeyTyped(typedChar, keyCode);
                proxyPortField.textboxKeyTyped(typedChar, keyCode);
                proxyUserField.textboxKeyTyped(typedChar, keyCode);
                proxyPassField.textboxKeyTyped(typedChar, keyCode);
            }
            
            // Enter to save
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                handleFormAction(10);
            }
        }
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        if (viewMode == 0) {
            quickLoginField.mouseClicked(mouseX, mouseY, mouseButton);
            
            // List click
            AltManager altManager = AltManager.getInstance();
            int listX = guiX + 10;
            int listY = guiY + 45;
            int listW = 250;
            
            if (mouseX >= listX && mouseX < listX + listW) {
                int clickedIndex = listScrollOffset + (mouseY - listY - 2) / LIST_ITEM_HEIGHT;
                if (clickedIndex >= 0 && clickedIndex < altManager.getAccountCount()) {
                    if (selectedAltIndex == clickedIndex && mouseButton == 0) {
                        // Double click - login
                        handleListAction(4);
                    } else {
                        selectedAltIndex = clickedIndex;
                    }
                }
            }
        } else {
            usernameField.mouseClicked(mouseX, mouseY, mouseButton);
            proxyHostField.mouseClicked(mouseX, mouseY, mouseButton);
            proxyPortField.mouseClicked(mouseX, mouseY, mouseButton);
            proxyUserField.mouseClicked(mouseX, mouseY, mouseButton);
            proxyPassField.mouseClicked(mouseX, mouseY, mouseButton);
            
            // Proxy toggle click
            int checkX = guiX + 175;
            int toggleY = guiY + 100;
            if (mouseX >= checkX && mouseX < checkX + 20 && mouseY >= toggleY && mouseY < toggleY + 20) {
                proxyEnabled = !proxyEnabled;
            }
        }
    }
    
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0 && viewMode == 0) {
            AltManager altManager = AltManager.getInstance();
            int maxScroll = Math.max(0, altManager.getAccountCount() - VISIBLE_ITEMS);
            
            if (scroll > 0) {
                listScrollOffset = Math.max(0, listScrollOffset - 1);
            } else {
                listScrollOffset = Math.min(maxScroll, listScrollOffset + 1);
            }
        }
    }
    
    @Override
    public void updateScreen() {
        if (viewMode == 0) {
            quickLoginField.updateCursorCounter();
        } else {
            usernameField.updateCursorCounter();
            proxyHostField.updateCursorCounter();
            proxyPortField.updateCursorCounter();
            proxyUserField.updateCursorCounter();
            proxyPassField.updateCursorCounter();
        }
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
