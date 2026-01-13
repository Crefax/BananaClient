package com.muzmod.gui.modern.components;

import com.muzmod.gui.modern.GuiRenderUtils;
import com.muzmod.gui.modern.GuiTheme;
import net.minecraft.client.Minecraft;

/**
 * Modern Slider Component
 * Draggable slider with value display and smooth animations
 */
public class ModernSlider {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    
    private int x, y;
    private int width, height;
    
    private String label;
    private String suffix = "";
    
    private double value;
    private double minValue;
    private double maxValue;
    private double step = 1.0;
    
    private boolean dragging = false;
    private boolean hovered = false;
    private boolean showValue = true;
    private boolean integerMode = false;
    
    // Animation
    private float hoverAnimation = 0f;
    
    // Track dimensions
    private static final int TRACK_HEIGHT = 4;
    private static final int KNOB_SIZE = 16;
    
    public ModernSlider(int x, int y, int width, int height, String label, double minValue, double maxValue, double initialValue) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.value = clamp(initialValue);
    }
    
    /**
     * Update animations and dragging
     */
    public void update(int mouseX, int mouseY) {
        hovered = isMouseOver(mouseX, mouseY) || isKnobHovered(mouseX, mouseY);
        
        float targetHover = hovered || dragging ? 1f : 0f;
        hoverAnimation += (targetHover - hoverAnimation) * GuiTheme.ANIMATION_SPEED;
        
        // Handle dragging
        if (dragging) {
            int trackStartX = x;
            int trackEndX = x + width - KNOB_SIZE;
            int trackWidth = trackEndX - trackStartX;
            
            double percent = (double)(mouseX - trackStartX - KNOB_SIZE / 2) / trackWidth;
            percent = Math.max(0, Math.min(1, percent));
            
            value = minValue + (maxValue - minValue) * percent;
            
            // Apply step
            if (step > 0) {
                value = Math.round(value / step) * step;
            }
            
            value = clamp(value);
        }
    }
    
    /**
     * Render the slider
     */
    public void render(int mouseX, int mouseY) {
        update(mouseX, mouseY);
        
        // Calculate positions
        int trackY = y + (height - TRACK_HEIGHT) / 2;
        int trackWidth = width - KNOB_SIZE;
        
        double percent = (value - minValue) / (maxValue - minValue);
        int knobX = x + (int)(percent * trackWidth);
        int knobY = y + (height - KNOB_SIZE) / 2;
        
        // Draw label
        if (label != null && !label.isEmpty()) {
            String displayText = label;
            if (showValue) {
                String valueText = formatValue();
                displayText = label + ": " + valueText + suffix;
            }
            GuiRenderUtils.drawText(displayText, x, y - 12, GuiTheme.TEXT_PRIMARY);
        }
        
        // Draw track background
        int trackBgColor = GuiTheme.SCROLLBAR_TRACK;
        GuiRenderUtils.drawRoundedRect(x, trackY, width, TRACK_HEIGHT, TRACK_HEIGHT / 2, trackBgColor);
        
        // Draw filled part
        int fillWidth = knobX - x + KNOB_SIZE / 2;
        if (fillWidth > 0) {
            int fillColor = GuiTheme.lerpColor(GuiTheme.ACCENT_PRIMARY, GuiTheme.ACCENT_SECONDARY, (float)percent);
            GuiRenderUtils.drawRoundedRect(x, trackY, fillWidth, TRACK_HEIGHT, TRACK_HEIGHT / 2, fillColor);
        }
        
        // Draw knob shadow
        if (hovered || dragging) {
            GuiRenderUtils.drawCircle(knobX + KNOB_SIZE / 2 + 1, knobY + KNOB_SIZE / 2 + 2, KNOB_SIZE / 2 + 2, GuiTheme.SHADOW_MEDIUM);
        }
        
        // Draw knob
        int knobColor = GuiTheme.lerpColor(GuiTheme.GLASS_PRIMARY, GuiTheme.SURFACE_PRIMARY, hoverAnimation);
        GuiRenderUtils.drawCircle(knobX + KNOB_SIZE / 2, knobY + KNOB_SIZE / 2, KNOB_SIZE / 2, knobColor);
        
        // Knob border
        int borderColor = GuiTheme.lerpColor(GuiTheme.BORDER_DEFAULT, GuiTheme.ACCENT_PRIMARY, hoverAnimation);
        // Draw a simple border ring
        GuiRenderUtils.drawCircle(knobX + KNOB_SIZE / 2, knobY + KNOB_SIZE / 2, KNOB_SIZE / 2, GuiTheme.withAlpha(borderColor, 0.5f));
        GuiRenderUtils.drawCircle(knobX + KNOB_SIZE / 2, knobY + KNOB_SIZE / 2, KNOB_SIZE / 2 - 1, knobColor);
    }
    
    /**
     * Handle mouse click
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            if (isKnobHovered(mouseX, mouseY) || isTrackHovered(mouseX, mouseY)) {
                dragging = true;
                
                // Jump to click position
                int trackStartX = x;
                int trackEndX = x + width - KNOB_SIZE;
                int trackWidth = trackEndX - trackStartX;
                
                double percent = (double)(mouseX - trackStartX - KNOB_SIZE / 2) / trackWidth;
                percent = Math.max(0, Math.min(1, percent));
                
                value = minValue + (maxValue - minValue) * percent;
                
                if (step > 0) {
                    value = Math.round(value / step) * step;
                }
                value = clamp(value);
                
                return true;
            }
        }
        return false;
    }
    
    /**
     * Handle mouse release
     */
    public void mouseReleased(int mouseX, int mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
    }
    
    /**
     * Check if mouse is over the slider area
     */
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    
    /**
     * Check if mouse is over the knob
     */
    private boolean isKnobHovered(int mouseX, int mouseY) {
        double percent = (value - minValue) / (maxValue - minValue);
        int trackWidth = width - KNOB_SIZE;
        int knobX = x + (int)(percent * trackWidth);
        int knobY = y + (height - KNOB_SIZE) / 2;
        
        int dx = mouseX - (knobX + KNOB_SIZE / 2);
        int dy = mouseY - (knobY + KNOB_SIZE / 2);
        return Math.sqrt(dx * dx + dy * dy) <= KNOB_SIZE / 2 + 2;
    }
    
    /**
     * Check if mouse is over the track
     */
    private boolean isTrackHovered(int mouseX, int mouseY) {
        int trackY = y + (height - TRACK_HEIGHT) / 2 - 4;
        return mouseX >= x && mouseX < x + width && mouseY >= trackY && mouseY < trackY + TRACK_HEIGHT + 8;
    }
    
    /**
     * Clamp value to range
     */
    private double clamp(double val) {
        return Math.max(minValue, Math.min(maxValue, val));
    }
    
    /**
     * Format value for display
     */
    private String formatValue() {
        if (integerMode) {
            return String.valueOf((int)value);
        } else if (step >= 1) {
            return String.valueOf((int)value);
        } else {
            return String.format("%.1f", value);
        }
    }
    
    // ==================== GETTERS/SETTERS ====================
    
    public double getValue() { return integerMode ? (int)value : value; }
    public int getIntValue() { return (int)value; }
    
    public void setValue(double value) {
        this.value = clamp(value);
    }
    
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
    
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    
    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
    
    public double getMinValue() { return minValue; }
    public void setMinValue(double minValue) { this.minValue = minValue; }
    
    public double getMaxValue() { return maxValue; }
    public void setMaxValue(double maxValue) { this.maxValue = maxValue; }
    
    public double getStep() { return step; }
    public void setStep(double step) { this.step = step; }
    
    public boolean isShowValue() { return showValue; }
    public void setShowValue(boolean showValue) { this.showValue = showValue; }
    
    public boolean isIntegerMode() { return integerMode; }
    public void setIntegerMode(boolean integerMode) { this.integerMode = integerMode; }
    
    public boolean isDragging() { return dragging; }
}
