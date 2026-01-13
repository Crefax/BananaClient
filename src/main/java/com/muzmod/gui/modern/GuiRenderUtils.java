package com.muzmod.gui.modern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Modern GUI Rendering Utilities
 * Provides glass effects, rounded rectangles, shadows, etc.
 */
public class GuiRenderUtils extends Gui {
    
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    // ==================== BASIC SHAPES ====================
    
    /**
     * Draw a filled rectangle with optional alpha
     */
    public static void drawRect(int x, int y, int width, int height, int color) {
        Gui.drawRect(x, y, x + width, y + height, color);
    }
    
    /**
     * Draw a rectangle outline
     */
    public static void drawOutline(int x, int y, int width, int height, int color) {
        drawHLine(x, x + width - 1, y, color);
        drawHLine(x, x + width - 1, y + height - 1, color);
        drawVLine(x, y, y + height - 1, color);
        drawVLine(x + width - 1, y, y + height - 1, color);
    }
    
    public static void drawHLine(int x1, int x2, int y, int color) {
        Gui.drawRect(x1, y, x2 + 1, y + 1, color);
    }
    
    public static void drawVLine(int x, int y1, int y2, int color) {
        Gui.drawRect(x, y1, x + 1, y2 + 1, color);
    }
    
    // ==================== ROUNDED RECTANGLES ====================
    
    /**
     * Draw a rounded rectangle (simulated with layered rectangles)
     */
    public static void drawRoundedRect(int x, int y, int width, int height, int radius, int color) {
        // Simplified rounded rect using overlapping rectangles
        // Main body
        drawRect(x + radius, y, width - radius * 2, height, color);
        drawRect(x, y + radius, width, height - radius * 2, color);
        
        // Corners (simplified as small squares for performance)
        drawRect(x + 1, y + 1, radius - 1, radius - 1, color);
        drawRect(x + width - radius, y + 1, radius - 1, radius - 1, color);
        drawRect(x + 1, y + height - radius, radius - 1, radius - 1, color);
        drawRect(x + width - radius, y + height - radius, radius - 1, radius - 1, color);
    }
    
    /**
     * Draw a rounded rectangle with border
     */
    public static void drawRoundedRectWithBorder(int x, int y, int width, int height, int radius, 
                                                   int fillColor, int borderColor) {
        // Draw fill
        drawRoundedRect(x, y, width, height, radius, fillColor);
        
        // Draw border (simplified)
        drawHLine(x + radius, x + width - radius - 1, y, borderColor);
        drawHLine(x + radius, x + width - radius - 1, y + height - 1, borderColor);
        drawVLine(x, y + radius, y + height - radius - 1, borderColor);
        drawVLine(x + width - 1, y + radius, y + height - radius - 1, borderColor);
    }
    
    // ==================== GLASS EFFECTS ====================
    
    /**
     * Draw a glass panel with blur effect simulation
     */
    public static void drawGlassPanel(int x, int y, int width, int height, int glassColor, int borderColor) {
        // Background blur simulation (layered semi-transparent rectangles)
        drawRect(x, y, width, height, GuiTheme.withAlpha(0x000000, 0.4f));
        drawRect(x, y, width, height, glassColor);
        
        // Top highlight
        drawRect(x, y, width, 1, GuiTheme.withAlpha(0xFFFFFF, 0.1f));
        
        // Border
        drawOutline(x, y, width, height, borderColor);
    }
    
    /**
     * Draw a frosted glass panel (more opaque)
     */
    public static void drawFrostedPanel(int x, int y, int width, int height) {
        // Multiple layers for frosted effect
        drawRect(x, y, width, height, GuiTheme.BLUR_PANEL);
        drawRect(x, y, width, height, GuiTheme.GLASS_MEDIUM);
        
        // Subtle inner glow
        drawRect(x + 1, y + 1, width - 2, 1, GuiTheme.withAlpha(0xFFFFFF, 0.05f));
        
        // Border
        drawOutline(x, y, width, height, GuiTheme.BORDER_DEFAULT);
    }
    
    // ==================== GRADIENTS ====================
    
    /**
     * Draw a horizontal gradient rectangle
     */
    public static void drawGradientRectH(int x, int y, int width, int height, int colorLeft, int colorRight) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        
        float aL = ((colorLeft >> 24) & 0xFF) / 255.0f;
        float rL = ((colorLeft >> 16) & 0xFF) / 255.0f;
        float gL = ((colorLeft >> 8) & 0xFF) / 255.0f;
        float bL = (colorLeft & 0xFF) / 255.0f;
        
        float aR = ((colorRight >> 24) & 0xFF) / 255.0f;
        float rR = ((colorRight >> 16) & 0xFF) / 255.0f;
        float gR = ((colorRight >> 8) & 0xFF) / 255.0f;
        float bR = (colorRight & 0xFF) / 255.0f;
        
        wr.pos(x + width, y, 0).color(rR, gR, bR, aR).endVertex();
        wr.pos(x, y, 0).color(rL, gL, bL, aL).endVertex();
        wr.pos(x, y + height, 0).color(rL, gL, bL, aL).endVertex();
        wr.pos(x + width, y + height, 0).color(rR, gR, bR, aR).endVertex();
        
        tessellator.draw();
        
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
    
    /**
     * Draw a vertical gradient rectangle
     */
    public static void drawGradientRectV(int x, int y, int width, int height, int colorTop, int colorBottom) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        
        float aT = ((colorTop >> 24) & 0xFF) / 255.0f;
        float rT = ((colorTop >> 16) & 0xFF) / 255.0f;
        float gT = ((colorTop >> 8) & 0xFF) / 255.0f;
        float bT = (colorTop & 0xFF) / 255.0f;
        
        float aB = ((colorBottom >> 24) & 0xFF) / 255.0f;
        float rB = ((colorBottom >> 16) & 0xFF) / 255.0f;
        float gB = ((colorBottom >> 8) & 0xFF) / 255.0f;
        float bB = (colorBottom & 0xFF) / 255.0f;
        
        wr.pos(x + width, y, 0).color(rT, gT, bT, aT).endVertex();
        wr.pos(x, y, 0).color(rT, gT, bT, aT).endVertex();
        wr.pos(x, y + height, 0).color(rB, gB, bB, aB).endVertex();
        wr.pos(x + width, y + height, 0).color(rB, gB, bB, aB).endVertex();
        
        tessellator.draw();
        
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
    
    // ==================== SHADOWS ====================
    
    /**
     * Draw a drop shadow under an element
     */
    public static void drawShadow(int x, int y, int width, int height, int shadowSize) {
        // Bottom shadow
        for (int i = 0; i < shadowSize; i++) {
            int alpha = (int)(30 * (1 - (float)i / shadowSize));
            drawRect(x + i, y + height + i, width - i, 1, GuiTheme.withAlpha(0x000000, alpha / 255f));
        }
        
        // Right shadow
        for (int i = 0; i < shadowSize; i++) {
            int alpha = (int)(30 * (1 - (float)i / shadowSize));
            drawRect(x + width + i, y + i, 1, height - i, GuiTheme.withAlpha(0x000000, alpha / 255f));
        }
    }
    
    // ==================== SCISSOR (CLIPPING) ====================
    
    /**
     * Enable scissor test for clipping
     */
    public static void enableScissor(int x, int y, int width, int height) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            x * scale,
            (sr.getScaledHeight() - y - height) * scale,
            width * scale,
            height * scale
        );
    }
    
    /**
     * Disable scissor test
     */
    public static void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
    
    // ==================== CIRCLE ====================
    
    /**
     * Draw a filled circle
     */
    public static void drawCircle(int centerX, int centerY, int radius, int color) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        
        wr.pos(centerX, centerY, 0).color(r, g, b, a).endVertex();
        
        int segments = 32;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            double px = centerX + Math.cos(angle) * radius;
            double py = centerY + Math.sin(angle) * radius;
            wr.pos(px, py, 0).color(r, g, b, a).endVertex();
        }
        
        tessellator.draw();
        
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }
    
    // ==================== TEXT UTILITIES ====================
    
    /**
     * Draw centered text
     */
    public static void drawCenteredText(String text, int x, int y, int color) {
        int width = mc.fontRendererObj.getStringWidth(text);
        mc.fontRendererObj.drawStringWithShadow(text, x - width / 2, y, color);
    }
    
    /**
     * Draw text with shadow
     */
    public static void drawText(String text, int x, int y, int color) {
        mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
    }
    
    /**
     * Get text width
     */
    public static int getTextWidth(String text) {
        return mc.fontRendererObj.getStringWidth(text);
    }
    
    /**
     * Get font height
     */
    public static int getFontHeight() {
        return mc.fontRendererObj.FONT_HEIGHT;
    }
}
