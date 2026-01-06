package com.muzmod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Modern GUI Utilities
 * Rounded corners, gradients, shadows, glow effects
 */
public class ModernGuiUtils {
    
    // === MODERN COLOR PALETTE ===
    public static final int BG_PRIMARY = 0xFF0D0D0D;      // Ana arka plan - çok koyu
    public static final int BG_SECONDARY = 0xFF151515;    // İkincil arka plan
    public static final int BG_TERTIARY = 0xFF1E1E1E;     // Üçüncül arka plan
    public static final int BG_CARD = 0xFF1A1A1A;         // Kart arka planı
    public static final int BG_CARD_HOVER = 0xFF222222;   // Hover durumu
    
    public static final int ACCENT_PURPLE = 0xFF9B59B6;   // Ana accent
    public static final int ACCENT_PINK = 0xFFE91E63;     // Pembe accent
    public static final int ACCENT_CYAN = 0xFF00BCD4;     // Cyan accent
    public static final int ACCENT_GREEN = 0xFF4CAF50;    // Yeşil (aktif)
    public static final int ACCENT_RED = 0xFFF44336;      // Kırmızı (hata)
    public static final int ACCENT_ORANGE = 0xFFFF9800;   // Turuncu (maden)
    public static final int ACCENT_BLUE = 0xFF2196F3;     // Mavi (AFK)
    public static final int ACCENT_YELLOW = 0xFFFFEB3B;   // Sarı (uyarı)
    
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;    // Ana metin
    public static final int TEXT_SECONDARY = 0xFFB0B0B0;  // İkincil metin
    public static final int TEXT_TERTIARY = 0xFF707070;   // Üçüncül metin
    public static final int TEXT_DISABLED = 0xFF505050;   // Devre dışı
    
    public static final int BORDER_COLOR = 0xFF2A2A2A;    // Kenar rengi
    public static final int BORDER_ACCENT = 0xFF3A3A3A;   // Accent kenar
    
    // === DRAWING METHODS ===
    
    /**
     * Draw a rectangle with alpha blending
     */
    public static void drawRect(int x, int y, int width, int height, int color) {
        Gui.drawRect(x, y, x + width, y + height, color);
    }
    
    /**
     * Draw rounded rectangle (simulated with multiple rects)
     */
    public static void drawRoundedRect(int x, int y, int width, int height, int radius, int color) {
        // Main body
        drawRect(x + radius, y, width - radius * 2, height, color);
        drawRect(x, y + radius, width, height - radius * 2, color);
        
        // Corners (circles approximated)
        drawCircle(x + radius, y + radius, radius, color);
        drawCircle(x + width - radius, y + radius, radius, color);
        drawCircle(x + radius, y + height - radius, radius, color);
        drawCircle(x + width - radius, y + height - radius, radius, color);
    }
    
    /**
     * Draw filled circle
     */
    public static void drawCircle(int centerX, int centerY, int radius, int color) {
        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        
        worldrenderer.pos(centerX, centerY, 0).endVertex();
        for (int i = 0; i <= 360; i += 10) {
            double rad = Math.toRadians(i);
            worldrenderer.pos(centerX + Math.cos(rad) * radius, centerY + Math.sin(rad) * radius, 0).endVertex();
        }
        
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
    
    /**
     * Draw vertical gradient
     */
    public static void drawGradientV(int x, int y, int width, int height, int colorTop, int colorBottom) {
        float alphaTop = (float) (colorTop >> 24 & 255) / 255.0F;
        float redTop = (float) (colorTop >> 16 & 255) / 255.0F;
        float greenTop = (float) (colorTop >> 8 & 255) / 255.0F;
        float blueTop = (float) (colorTop & 255) / 255.0F;
        
        float alphaBot = (float) (colorBottom >> 24 & 255) / 255.0F;
        float redBot = (float) (colorBottom >> 16 & 255) / 255.0F;
        float greenBot = (float) (colorBottom >> 8 & 255) / 255.0F;
        float blueBot = (float) (colorBottom & 255) / 255.0F;
        
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(x + width, y, 0).color(redTop, greenTop, blueTop, alphaTop).endVertex();
        worldrenderer.pos(x, y, 0).color(redTop, greenTop, blueTop, alphaTop).endVertex();
        worldrenderer.pos(x, y + height, 0).color(redBot, greenBot, blueBot, alphaBot).endVertex();
        worldrenderer.pos(x + width, y + height, 0).color(redBot, greenBot, blueBot, alphaBot).endVertex();
        tessellator.draw();
        
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
    
    /**
     * Draw horizontal gradient
     */
    public static void drawGradientH(int x, int y, int width, int height, int colorLeft, int colorRight) {
        float alphaL = (float) (colorLeft >> 24 & 255) / 255.0F;
        float redL = (float) (colorLeft >> 16 & 255) / 255.0F;
        float greenL = (float) (colorLeft >> 8 & 255) / 255.0F;
        float blueL = (float) (colorLeft & 255) / 255.0F;
        
        float alphaR = (float) (colorRight >> 24 & 255) / 255.0F;
        float redR = (float) (colorRight >> 16 & 255) / 255.0F;
        float greenR = (float) (colorRight >> 8 & 255) / 255.0F;
        float blueR = (float) (colorRight & 255) / 255.0F;
        
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(x, y, 0).color(redL, greenL, blueL, alphaL).endVertex();
        worldrenderer.pos(x, y + height, 0).color(redL, greenL, blueL, alphaL).endVertex();
        worldrenderer.pos(x + width, y + height, 0).color(redR, greenR, blueR, alphaR).endVertex();
        worldrenderer.pos(x + width, y, 0).color(redR, greenR, blueR, alphaR).endVertex();
        tessellator.draw();
        
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
    
    /**
     * Draw shadow under element
     */
    public static void drawShadow(int x, int y, int width, int height, int shadowSize) {
        // Bottom shadow
        drawGradientV(x, y + height, width, shadowSize, 0x60000000, 0x00000000);
        // Right shadow
        drawGradientH(x + width, y, shadowSize, height, 0x60000000, 0x00000000);
        // Corner shadow
        drawGradientV(x + width, y + height, shadowSize, shadowSize, 0x40000000, 0x00000000);
    }
    
    /**
     * Draw glow effect around element
     */
    public static void drawGlow(int x, int y, int width, int height, int color, int glowSize) {
        int glowColor = (color & 0x00FFFFFF) | 0x30000000; // 30% alpha
        int transparent = (color & 0x00FFFFFF) | 0x00000000;
        
        // Top glow
        drawGradientV(x, y - glowSize, width, glowSize, transparent, glowColor);
        // Bottom glow
        drawGradientV(x, y + height, width, glowSize, glowColor, transparent);
        // Left glow
        drawGradientH(x - glowSize, y, glowSize, height, transparent, glowColor);
        // Right glow
        drawGradientH(x + width, y, glowSize, height, glowColor, transparent);
    }
    
    /**
     * Draw outline
     */
    public static void drawOutline(int x, int y, int width, int height, int thickness, int color) {
        drawRect(x, y, width, thickness, color); // Top
        drawRect(x, y + height - thickness, width, thickness, color); // Bottom
        drawRect(x, y, thickness, height, color); // Left
        drawRect(x + width - thickness, y, thickness, height, color); // Right
    }
    
    /**
     * Draw modern button
     */
    public static void drawModernButton(int x, int y, int width, int height, String text, 
            int bgColor, int accentColor, boolean hovered, FontRenderer font) {
        int bg = hovered ? lighten(bgColor, 20) : bgColor;
        
        // Shadow
        if (hovered) {
            drawShadow(x, y, width, height, 4);
        }
        
        // Background
        drawRect(x, y, width, height, bg);
        
        // Accent line at bottom
        drawRect(x, y + height - 2, width, 2, accentColor);
        
        // Hover glow
        if (hovered) {
            drawGlow(x, y, width, height, accentColor, 3);
        }
        
        // Text
        int textX = x + (width - font.getStringWidth(text)) / 2;
        int textY = y + (height - 8) / 2;
        font.drawStringWithShadow(text, textX, textY, hovered ? TEXT_PRIMARY : TEXT_SECONDARY);
    }
    
    /**
     * Draw modern card/panel
     */
    public static void drawModernCard(int x, int y, int width, int height, String title, 
            int accentColor, FontRenderer font) {
        // Shadow
        drawShadow(x, y, width, height, 6);
        
        // Main background
        drawRect(x, y, width, height, BG_CARD);
        
        // Header gradient
        drawGradientV(x, y, width, 24, BG_TERTIARY, BG_CARD);
        
        // Accent line
        drawRect(x, y + 23, width, 1, accentColor);
        
        // Glow on accent line
        drawGradientV(x, y + 24, width, 4, (accentColor & 0x00FFFFFF) | 0x30000000, 0x00000000);
        
        // Border
        drawOutline(x, y, width, height, 1, BORDER_COLOR);
        
        // Title
        font.drawStringWithShadow(title, x + 10, y + 7, TEXT_PRIMARY);
    }
    
    /**
     * Draw toggle switch
     */
    public static void drawToggle(int x, int y, boolean enabled, boolean hovered) {
        int width = 32;
        int height = 16;
        
        int bgColor = enabled ? ACCENT_GREEN : BG_TERTIARY;
        int knobX = enabled ? x + width - 14 : x + 2;
        
        // Track
        drawRoundedRect(x, y, width, height, 8, bgColor);
        
        // Knob
        int knobColor = hovered ? 0xFFFFFFFF : 0xFFE0E0E0;
        drawCircle(knobX + 6, y + 8, 6, knobColor);
        
        if (hovered) {
            drawGlow(x, y, width, height, enabled ? ACCENT_GREEN : ACCENT_PURPLE, 2);
        }
    }
    
    /**
     * Draw progress bar
     */
    public static void drawProgressBar(int x, int y, int width, int height, float progress, int color) {
        // Background
        drawRect(x, y, width, height, BG_SECONDARY);
        
        // Progress
        int progressWidth = (int) (width * Math.min(1f, Math.max(0f, progress)));
        if (progressWidth > 0) {
            drawGradientH(x, y, progressWidth, height, color, lighten(color, 30));
        }
        
        // Border
        drawOutline(x, y, width, height, 1, BORDER_COLOR);
    }
    
    /**
     * Lighten a color
     */
    public static int lighten(int color, int amount) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((color >> 8) & 0xFF) + amount);
        int b = Math.min(255, (color & 0xFF) + amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Darken a color
     */
    public static int darken(int color, int amount) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, ((color >> 16) & 0xFF) - amount);
        int g = Math.max(0, ((color >> 8) & 0xFF) - amount);
        int b = Math.max(0, (color & 0xFF) - amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Apply alpha to color
     */
    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
    
    /**
     * Interpolate between two colors
     */
    public static int lerpColor(int color1, int color2, float t) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Get rainbow color based on time
     */
    public static int getRainbow(float speed, float saturation, float brightness) {
        float hue = (System.currentTimeMillis() % (long)(1000 / speed)) / (1000f / speed);
        return java.awt.Color.HSBtoRGB(hue, saturation, brightness);
    }
}
