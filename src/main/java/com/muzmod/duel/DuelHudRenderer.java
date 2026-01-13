package com.muzmod.duel;

import com.muzmod.MuzMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Renders the Duel Analyzer HUD
 * Position is configurable and draggable
 */
public class DuelHudRenderer extends Gui {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    
    // HUD pozisyonu (config'den okunacak)
    private int hudX = 10;
    private int hudY = 100;
    
    // HUD boyutları
    private int hudWidth = 200;
    private int hudHeight = 180;
    
    // Renkler
    private static final int BG_COLOR = 0xAA000000;       // Yarı saydam siyah
    private static final int BORDER_COLOR = 0xFF444444;   // Gri kenarlık
    private static final int TITLE_COLOR = 0xFFFFAA00;    // Turuncu başlık
    private static final int PLAYER1_COLOR = 0xFF55FF55;  // Yeşil (Player 1)
    private static final int PLAYER2_COLOR = 0xFFFF5555;  // Kırmızı (Player 2)
    private static final int TEXT_COLOR = 0xFFFFFFFF;     // Beyaz
    private static final int VALUE_COLOR = 0xFFAAAA00;    // Sarı değerler
    
    public DuelHudRenderer() {
    }
    
    /**
     * Render the HUD
     */
    public void render() {
        DuelSession session = DuelAnalyzerState.getInstance().getSession();
        if (session == null || !session.isActive()) return;
        
        // HUD pozisyonunu config'den al
        hudX = MuzMod.instance.getConfig().getDuelHudX();
        hudY = MuzMod.instance.getConfig().getDuelHudY();
        
        FontRenderer fr = mc.fontRendererObj;
        
        DuelData p1 = session.getPlayer1Data();
        DuelData p2 = session.getPlayer2Data();
        
        if (p1 == null || p2 == null) return;
        
        // Arka plan
        drawRect(hudX, hudY, hudX + hudWidth, hudY + hudHeight, BG_COLOR);
        
        // Kenarlık
        drawHorizontalLine(hudX, hudX + hudWidth - 1, hudY, BORDER_COLOR);
        drawHorizontalLine(hudX, hudX + hudWidth - 1, hudY + hudHeight - 1, BORDER_COLOR);
        drawVerticalLine(hudX, hudY, hudY + hudHeight - 1, BORDER_COLOR);
        drawVerticalLine(hudX + hudWidth - 1, hudY, hudY + hudHeight - 1, BORDER_COLOR);
        
        int y = hudY + 5;
        int centerX = hudX + hudWidth / 2;
        
        // Başlık
        String title = "⚔ DUEL ANALYZER ⚔";
        int titleWidth = fr.getStringWidth(title);
        fr.drawStringWithShadow(title, centerX - titleWidth / 2, y, TITLE_COLOR);
        y += 12;
        
        // Süre
        String duration = "Süre: " + session.getSessionDurationFormatted();
        int durationWidth = fr.getStringWidth(duration);
        fr.drawStringWithShadow(duration, centerX - durationWidth / 2, y, TEXT_COLOR);
        y += 14;
        
        // Ayırıcı çizgi
        drawHorizontalLine(hudX + 5, hudX + hudWidth - 6, y, BORDER_COLOR);
        y += 5;
        
        // Oyuncu isimleri
        int col1X = hudX + 10;
        int col2X = hudX + hudWidth - 10;
        
        // Player 1 (sol)
        fr.drawStringWithShadow(p1.getPlayerName(), col1X, y, PLAYER1_COLOR);
        // Player 2 (sağ - sağa hizalı)
        int p2Width = fr.getStringWidth(p2.getPlayerName());
        fr.drawStringWithShadow(p2.getPlayerName(), col2X - p2Width, y, PLAYER2_COLOR);
        y += 14;
        
        // İstatistikler
        drawStatRow(fr, y, "Hit", p1.getHitsGiven(), p2.getHitsGiven(), col1X, col2X);
        y += 11;
        
        drawStatRow(fr, y, "Alınan Hit", p1.getHitsReceived(), p2.getHitsReceived(), col1X, col2X);
        y += 11;
        
        drawStatRow(fr, y, "Encli Elma", p1.getEnchantedApplesEaten(), p2.getEnchantedApplesEaten(), col1X, col2X);
        y += 11;
        
        drawStatRow(fr, y, "Altın Elma", p1.getGoldenApplesEaten(), p2.getGoldenApplesEaten(), col1X, col2X);
        y += 11;
        
        // Kırılan zırh
        drawStatRow(fr, y, "Kask", p1.getHelmetsLost(), p2.getHelmetsLost(), col1X, col2X);
        y += 11;
        
        drawStatRow(fr, y, "Göğüslük", p1.getChestplatesLost(), p2.getChestplatesLost(), col1X, col2X);
        y += 11;
        
        drawStatRow(fr, y, "Pantolon", p1.getLeggingsLost(), p2.getLeggingsLost(), col1X, col2X);
        y += 11;
        
        drawStatRow(fr, y, "Bot", p1.getBootsLost(), p2.getBootsLost(), col1X, col2X);
        y += 14;
        
        // Kılıç bilgisi
        drawHorizontalLine(hudX + 5, hudX + hudWidth - 6, y, BORDER_COLOR);
        y += 5;
        
        String sword1 = truncate(p1.getSwordInfo().getDisplayName(), 12);
        String sword2 = truncate(p2.getSwordInfo().getDisplayName(), 12);
        
        fr.drawStringWithShadow(sword1, col1X, y, PLAYER1_COLOR);
        int sword2Width = fr.getStringWidth(sword2);
        fr.drawStringWithShadow(sword2, col2X - sword2Width, y, PLAYER2_COLOR);
    }
    
    /**
     * Draw a stat row with label and values
     */
    private void drawStatRow(FontRenderer fr, int y, String label, int val1, int val2, int col1X, int col2X) {
        // Değer 1 (sol)
        String v1 = String.valueOf(val1);
        fr.drawStringWithShadow(v1, col1X, y, VALUE_COLOR);
        
        // Label (orta)
        int labelWidth = fr.getStringWidth(label);
        int centerX = (col1X + col2X) / 2;
        fr.drawStringWithShadow(label, centerX - labelWidth / 2, y, TEXT_COLOR);
        
        // Değer 2 (sağ)
        String v2 = String.valueOf(val2);
        int v2Width = fr.getStringWidth(v2);
        fr.drawStringWithShadow(v2, col2X - v2Width, y, VALUE_COLOR);
    }
    
    /**
     * Truncate string if too long
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "?";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 2) + "..";
    }
    
    /**
     * Check if mouse is over the HUD (for dragging)
     */
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= hudX && mouseX <= hudX + hudWidth &&
               mouseY >= hudY && mouseY <= hudY + hudHeight;
    }
    
    /**
     * Update HUD position (for dragging)
     */
    public void setPosition(int x, int y) {
        // Ekran sınırları içinde tut
        ScaledResolution sr = new ScaledResolution(mc);
        x = Math.max(0, Math.min(x, sr.getScaledWidth() - hudWidth));
        y = Math.max(0, Math.min(y, sr.getScaledHeight() - hudHeight));
        
        this.hudX = x;
        this.hudY = y;
        
        // Config'e kaydet
        MuzMod.instance.getConfig().setDuelHudX(x);
        MuzMod.instance.getConfig().setDuelHudY(y);
    }
    
    public int getHudX() { return hudX; }
    public int getHudY() { return hudY; }
    public int getHudWidth() { return hudWidth; }
    public int getHudHeight() { return hudHeight; }
}
