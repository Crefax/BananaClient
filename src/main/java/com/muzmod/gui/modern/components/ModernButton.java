package com.muzmod.gui.modern.components;

import com.muzmod.gui.modern.GuiRenderUtils;
import com.muzmod.gui.modern.GuiTheme;
import net.minecraft.client.Minecraft;

/**
 * Modern Button Component
 * Clean flat design with subtle hover effects
 */
public class ModernButton {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    
    private int x, y;
    private int width, height;
    private String text;
    
    private boolean enabled = true;
    private boolean hovered = false;
    private boolean pressed = false;
    
    // Animation
    private float hoverAnimation = 0f;
    private float pressAnimation = 0f;
    
    // Style
    private ButtonStyle style = ButtonStyle.PRIMARY;
    
    public enum ButtonStyle {
        PRIMARY,    // Accent color (indigo/purple)
        SECONDARY,  // Gray/neutral
        SUCCESS,    // Green
        DANGER,     // Red
        GHOST       // Transparent with border
    }
    
    public ModernButton(int x, int y, int width, int height, String text) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.text = text;
    }
    
    public ModernButton(int x, int y, int width, int height, String text, ButtonStyle style) {
        this(x, y, width, height, text);
        this.style = style;
    }
    
    /**
     * Update animations
     */
    public void update(int mouseX, int mouseY) {
        boolean wasHovered = hovered;
        hovered = enabled && isMouseOver(mouseX, mouseY);
        
        // Hover animation
        float targetHover = hovered ? 1f : 0f;
        hoverAnimation += (targetHover - hoverAnimation) * GuiTheme.ANIMATION_SPEED;
        
        // Press animation decay
        pressAnimation *= 0.85f;
    }
    
    /**
     * Render the button
     */
    public void render(int mouseX, int mouseY) {
        update(mouseX, mouseY);
        
        // Get colors based on style
        int bgColor, textColor;
        
        switch (style) {
            case PRIMARY:
                bgColor = hovered ? 0xFF5B5FD9 : 0xFF6366F1; // Indigo
                textColor = 0xFFFFFFFF;
                break;
            case SECONDARY:
                bgColor = hovered ? 0xFF4B5563 : 0xFF374151; // Gray
                textColor = 0xFFE5E7EB;
                break;
            case SUCCESS:
                bgColor = hovered ? 0xFF16A34A : 0xFF22C55E; // Green
                textColor = 0xFFFFFFFF;
                break;
            case DANGER:
                bgColor = hovered ? 0xFFDC2626 : 0xFFEF4444; // Red
                textColor = 0xFFFFFFFF;
                break;
            case GHOST:
            default:
                bgColor = hovered ? 0x30FFFFFF : 0x15FFFFFF; // Transparent
                textColor = 0xFFE5E7EB;
                break;
        }
        
        // Pressed state - darken
        if (pressed) {
            bgColor = GuiTheme.darken(bgColor, 0.15f);
        }
        
        // Disabled state
        if (!enabled) {
            bgColor = 0xFF2D2D2D;
            textColor = 0xFF666666;
        }
        
        // Draw button background with rounded corners
        int radius = 4;
        GuiRenderUtils.drawRoundedRect(x, y, width, height, radius, bgColor);
        
        // Draw subtle border
        if (style == ButtonStyle.GHOST || hovered) {
            int borderColor = hovered ? 0x40FFFFFF : 0x20FFFFFF;
            GuiRenderUtils.drawRoundedRectWithBorder(x, y, width, height, radius, 0x00000000, borderColor);
        }
        
        // Draw text centered
        int textX = x + (width - GuiRenderUtils.getTextWidth(text)) / 2;
        int textY = y + (height - 8) / 2;
        
        GuiRenderUtils.drawText(text, textX, textY, textColor);
    }
    
    /**
     * Handle mouse click
     * @return true if button was clicked
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && enabled && isMouseOver(mouseX, mouseY)) {
            pressed = true;
            pressAnimation = 1f;
            return true;
        }
        return false;
    }
    
    /**
     * Handle mouse release
     */
    public void mouseReleased(int mouseX, int mouseY, int button) {
        pressed = false;
    }
    
    /**
     * Check if mouse is over button
     */
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    
    // ==================== GETTERS/SETTERS ====================
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public ButtonStyle getStyle() { return style; }
    public void setStyle(ButtonStyle style) { this.style = style; }
}
