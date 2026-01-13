package com.muzmod.gui.modern.components;

import com.muzmod.gui.modern.GuiRenderUtils;
import com.muzmod.gui.modern.GuiTheme;
import net.minecraft.client.Minecraft;

/**
 * Modern Toggle Switch Component
 * Animated on/off switch with smooth transitions
 */
public class ModernToggle {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    
    private int x, y;
    private int width = 44;
    private int height = 22;
    
    private boolean enabled;
    private String label;
    
    // Animation
    private float animationProgress = 0f;
    private float targetProgress = 0f;
    private boolean animating = false;
    
    // Hover state
    private boolean hovered = false;
    
    public ModernToggle(int x, int y, String label, boolean initialState) {
        this.x = x;
        this.y = y;
        this.label = label;
        this.enabled = initialState;
        this.animationProgress = initialState ? 1f : 0f;
        this.targetProgress = animationProgress;
    }
    
    /**
     * Update animation
     */
    public void update() {
        if (animationProgress != targetProgress) {
            float diff = targetProgress - animationProgress;
            animationProgress += diff * GuiTheme.ANIMATION_SPEED * 2;
            
            if (Math.abs(diff) < 0.01f) {
                animationProgress = targetProgress;
            }
        }
    }
    
    /**
     * Render the toggle
     */
    public void render(int mouseX, int mouseY) {
        update();
        
        // Check hover
        hovered = isMouseOver(mouseX, mouseY);
        
        // Calculate colors based on state
        int bgColor = GuiTheme.lerpColor(GuiTheme.TOGGLE_OFF_BG, GuiTheme.TOGGLE_ON_BG, animationProgress);
        int knobColor = GuiTheme.lerpColor(GuiTheme.TOGGLE_OFF_KNOB, GuiTheme.TOGGLE_ON_KNOB, animationProgress);
        
        // Add hover effect
        if (hovered) {
            bgColor = GuiTheme.brighten(bgColor, 0.1f);
        }
        
        // Draw track (background)
        int trackRadius = height / 2;
        GuiRenderUtils.drawRoundedRect(x, y, width, height, trackRadius, bgColor);
        
        // Draw border
        GuiRenderUtils.drawOutline(x, y, width, height, GuiTheme.withAlpha(0xFFFFFF, 0.1f));
        
        // Calculate knob position
        int knobSize = height - 4;
        int knobMinX = x + 2;
        int knobMaxX = x + width - knobSize - 2;
        int knobX = (int) (knobMinX + (knobMaxX - knobMinX) * animationProgress);
        int knobY = y + 2;
        
        // Draw knob shadow
        GuiRenderUtils.drawCircle(knobX + knobSize / 2 + 1, knobY + knobSize / 2 + 1, knobSize / 2, GuiTheme.SHADOW_MEDIUM);
        
        // Draw knob
        GuiRenderUtils.drawCircle(knobX + knobSize / 2, knobY + knobSize / 2, knobSize / 2, knobColor);
        
        // Draw label
        if (label != null && !label.isEmpty()) {
            int labelColor = enabled ? GuiTheme.TEXT_PRIMARY : GuiTheme.TEXT_MUTED;
            GuiRenderUtils.drawText(label, x + width + 10, y + (height - 8) / 2, labelColor);
        }
    }
    
    /**
     * Handle mouse click
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            toggle();
            return true;
        }
        return false;
    }
    
    /**
     * Toggle the state
     */
    public void toggle() {
        enabled = !enabled;
        targetProgress = enabled ? 1f : 0f;
    }
    
    /**
     * Check if mouse is over toggle
     */
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    
    // ==================== GETTERS/SETTERS ====================
    
    public boolean isEnabled() { return enabled; }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.targetProgress = enabled ? 1f : 0f;
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    
    public int getTotalWidth() {
        if (label != null && !label.isEmpty()) {
            return width + 10 + GuiRenderUtils.getTextWidth(label);
        }
        return width;
    }
}
