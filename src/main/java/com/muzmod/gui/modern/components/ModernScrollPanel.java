package com.muzmod.gui.modern.components;

import com.muzmod.gui.modern.GuiRenderUtils;
import com.muzmod.gui.modern.GuiTheme;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern Scroll Panel Component
 * Container for scrollable content with smooth scrolling and modern scrollbar
 */
public class ModernScrollPanel {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    
    private int x, y;
    private int width, height;
    private int contentHeight = 0;
    
    // Scroll state
    private double scrollOffset = 0;
    private double targetScrollOffset = 0;
    private double scrollVelocity = 0;
    
    // Scrollbar
    private boolean scrollbarHovered = false;
    private boolean scrollbarDragging = false;
    private int dragStartY = 0;
    private double dragStartScroll = 0;
    
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_MARGIN = 4;
    private static final int SCROLL_SPEED = 20;
    private static final double SMOOTH_FACTOR = 0.2;
    
    // Child elements - these can be any component
    private List<ScrollableItem> items = new ArrayList<>();
    
    /**
     * Interface for scrollable items
     */
    public interface ScrollableItem {
        void render(int x, int y, int width, int mouseX, int mouseY, boolean hovered);
        int getHeight();
        boolean mouseClicked(int mouseX, int mouseY, int button);
        void mouseReleased(int mouseX, int mouseY, int button);
        boolean keyTyped(char typedChar, int keyCode);
    }
    
    public ModernScrollPanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    /**
     * Update scroll position with smoothing
     */
    public void update() {
        // Smooth scrolling
        double diff = targetScrollOffset - scrollOffset;
        scrollOffset += diff * SMOOTH_FACTOR;
        
        if (Math.abs(diff) < 0.5) {
            scrollOffset = targetScrollOffset;
        }
        
        // Clamp scroll
        clampScroll();
    }
    
    /**
     * Render the scroll panel
     */
    public void render(int mouseX, int mouseY) {
        update();
        
        // Draw panel background
        GuiRenderUtils.drawGlassPanel(x, y, width, height, GuiTheme.GLASS_MEDIUM, GuiTheme.BORDER_DEFAULT);
        
        // Enable scissor for content clipping
        GuiRenderUtils.enableScissor(x, y, width - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN * 2, height);
        
        // Render items
        int contentY = y - (int)scrollOffset;
        int contentWidth = width - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN * 3;
        
        for (ScrollableItem item : items) {
            int itemHeight = item.getHeight();
            
            // Only render if visible
            if (contentY + itemHeight > y && contentY < y + height) {
                boolean itemHovered = mouseX >= x && mouseX < x + contentWidth &&
                                      mouseY >= Math.max(y, contentY) && mouseY < Math.min(y + height, contentY + itemHeight);
                item.render(x + SCROLLBAR_MARGIN, contentY, contentWidth, mouseX, mouseY, itemHovered);
            }
            
            contentY += itemHeight;
        }
        
        GuiRenderUtils.disableScissor();
        
        // Render scrollbar if needed
        if (contentHeight > height) {
            renderScrollbar(mouseX, mouseY);
        }
    }
    
    /**
     * Render the scrollbar
     */
    private void renderScrollbar(int mouseX, int mouseY) {
        int scrollbarX = x + width - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
        int scrollbarTrackHeight = height - SCROLLBAR_MARGIN * 2;
        int scrollbarY = y + SCROLLBAR_MARGIN;
        
        // Draw track
        GuiRenderUtils.drawRoundedRect(scrollbarX, scrollbarY, SCROLLBAR_WIDTH, scrollbarTrackHeight, 
                                       SCROLLBAR_WIDTH / 2, GuiTheme.SCROLLBAR_TRACK);
        
        // Calculate thumb size and position
        double visibleRatio = (double)height / contentHeight;
        int thumbHeight = Math.max(20, (int)(scrollbarTrackHeight * visibleRatio));
        
        double scrollRatio = scrollOffset / getMaxScroll();
        int thumbY = scrollbarY + (int)((scrollbarTrackHeight - thumbHeight) * scrollRatio);
        
        // Check if thumb is hovered
        scrollbarHovered = mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH &&
                          mouseY >= thumbY && mouseY < thumbY + thumbHeight;
        
        // Draw thumb
        int thumbColor = scrollbarDragging ? GuiTheme.SCROLLBAR_THUMB_ACTIVE : 
                        (scrollbarHovered ? GuiTheme.SCROLLBAR_THUMB_HOVER : GuiTheme.SCROLLBAR_THUMB);
        GuiRenderUtils.drawRoundedRect(scrollbarX, thumbY, SCROLLBAR_WIDTH, thumbHeight, 
                                       SCROLLBAR_WIDTH / 2, thumbColor);
    }
    
    /**
     * Handle mouse scroll
     */
    public boolean handleMouseScroll(int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            targetScrollOffset -= Math.signum(dWheel) * SCROLL_SPEED;
            clampScroll();
            return true;
        }
        return false;
    }
    
    /**
     * Handle mouse click
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        
        // Check scrollbar click
        if (contentHeight > height) {
            int scrollbarX = x + width - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
            if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH) {
                scrollbarDragging = true;
                dragStartY = mouseY;
                dragStartScroll = scrollOffset;
                return true;
            }
        }
        
        // Check item clicks
        int contentY = y - (int)scrollOffset;
        int contentWidth = width - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN * 3;
        
        for (ScrollableItem item : items) {
            int itemHeight = item.getHeight();
            
            // Check if click is within this item's bounds (and visible)
            if (mouseY >= Math.max(y, contentY) && mouseY < Math.min(y + height, contentY + itemHeight) &&
                mouseX >= x && mouseX < x + contentWidth) {
                
                // Adjust mouseY relative to item
                if (item.mouseClicked(mouseX, mouseY - contentY + (int)scrollOffset, button)) {
                    return true;
                }
            }
            
            contentY += itemHeight;
        }
        
        return false;
    }
    
    /**
     * Handle mouse release
     */
    public void mouseReleased(int mouseX, int mouseY, int button) {
        scrollbarDragging = false;
        
        for (ScrollableItem item : items) {
            item.mouseReleased(mouseX, mouseY, button);
        }
    }
    
    /**
     * Handle mouse drag
     */
    public void mouseDragged(int mouseX, int mouseY, int button) {
        if (scrollbarDragging && button == 0) {
            int scrollbarTrackHeight = height - SCROLLBAR_MARGIN * 2;
            double visibleRatio = (double)height / contentHeight;
            int thumbHeight = Math.max(20, (int)(scrollbarTrackHeight * visibleRatio));
            
            double dragDistance = mouseY - dragStartY;
            double scrollRange = scrollbarTrackHeight - thumbHeight;
            
            if (scrollRange > 0) {
                double scrollPercent = dragDistance / scrollRange;
                targetScrollOffset = dragStartScroll + scrollPercent * getMaxScroll();
                scrollOffset = targetScrollOffset;
                clampScroll();
            }
        }
    }
    
    /**
     * Handle key press
     */
    public boolean keyTyped(char typedChar, int keyCode) {
        for (ScrollableItem item : items) {
            if (item.keyTyped(typedChar, keyCode)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if mouse is over panel
     */
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    
    /**
     * Clamp scroll offset to valid range
     */
    private void clampScroll() {
        double maxScroll = getMaxScroll();
        targetScrollOffset = Math.max(0, Math.min(maxScroll, targetScrollOffset));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
    }
    
    /**
     * Get maximum scroll offset
     */
    private double getMaxScroll() {
        return Math.max(0, contentHeight - height);
    }
    
    /**
     * Add an item to the panel
     */
    public void addItem(ScrollableItem item) {
        items.add(item);
        recalculateContentHeight();
    }
    
    /**
     * Remove an item from the panel
     */
    public void removeItem(ScrollableItem item) {
        items.remove(item);
        recalculateContentHeight();
    }
    
    /**
     * Clear all items
     */
    public void clearItems() {
        items.clear();
        contentHeight = 0;
        scrollOffset = 0;
        targetScrollOffset = 0;
    }
    
    /**
     * Recalculate total content height
     */
    public void recalculateContentHeight() {
        contentHeight = 0;
        for (ScrollableItem item : items) {
            contentHeight += item.getHeight();
        }
    }
    
    /**
     * Scroll to top
     */
    public void scrollToTop() {
        targetScrollOffset = 0;
    }
    
    /**
     * Scroll to bottom
     */
    public void scrollToBottom() {
        targetScrollOffset = getMaxScroll();
    }
    
    /**
     * Scroll to make an item visible
     */
    public void scrollToItem(int index) {
        if (index < 0 || index >= items.size()) return;
        
        int itemY = 0;
        for (int i = 0; i < index; i++) {
            itemY += items.get(i).getHeight();
        }
        
        int itemHeight = items.get(index).getHeight();
        
        // Scroll up if item is above visible area
        if (itemY < scrollOffset) {
            targetScrollOffset = itemY;
        }
        // Scroll down if item is below visible area
        else if (itemY + itemHeight > scrollOffset + height) {
            targetScrollOffset = itemY + itemHeight - height;
        }
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
    
    public int getContentHeight() { return contentHeight; }
    public void setContentHeight(int contentHeight) { this.contentHeight = contentHeight; }
    
    public double getScrollOffset() { return scrollOffset; }
    public void setScrollOffset(double scrollOffset) {
        this.scrollOffset = scrollOffset;
        this.targetScrollOffset = scrollOffset;
        clampScroll();
    }
    
    public List<ScrollableItem> getItems() { return items; }
    
    public int getContentWidth() {
        return width - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN * 3;
    }
}
