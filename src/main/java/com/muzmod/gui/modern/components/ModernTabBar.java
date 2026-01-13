package com.muzmod.gui.modern.components;

import com.muzmod.gui.modern.GuiRenderUtils;
import com.muzmod.gui.modern.GuiTheme;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern Tab Bar Component
 * Horizontal tab navigation with animated indicator
 */
public class ModernTabBar {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    
    private int x, y;
    private int width, height;
    
    private List<Tab> tabs = new ArrayList<>();
    private int selectedIndex = 0;
    
    // Animation
    private float indicatorX = 0;
    private float indicatorWidth = 0;
    
    // Callback
    private Consumer<Integer> onTabChange;
    
    private static final int TAB_PADDING = 16;
    private static final int INDICATOR_HEIGHT = 3;
    
    public static class Tab {
        public String title;
        public String icon; // Optional icon identifier
        public boolean enabled = true;
        
        // Calculated bounds
        int x, width;
        
        public Tab(String title) {
            this.title = title;
        }
        
        public Tab(String title, String icon) {
            this.title = title;
            this.icon = icon;
        }
    }
    
    public ModernTabBar(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    /**
     * Add a tab
     */
    public void addTab(String title) {
        tabs.add(new Tab(title));
        recalculateTabBounds();
    }
    
    /**
     * Add a tab with icon
     */
    public void addTab(String title, String icon) {
        tabs.add(new Tab(title, icon));
        recalculateTabBounds();
    }
    
    /**
     * Recalculate tab positions
     */
    private void recalculateTabBounds() {
        if (tabs.isEmpty()) return;
        
        int totalTextWidth = 0;
        for (Tab tab : tabs) {
            totalTextWidth += GuiRenderUtils.getTextWidth(tab.title) + TAB_PADDING * 2;
        }
        
        // Distribute tabs evenly if they fit, otherwise use calculated widths
        int currentX = x;
        
        if (totalTextWidth <= width) {
            // Equal distribution
            int tabWidth = width / tabs.size();
            for (Tab tab : tabs) {
                tab.x = currentX;
                tab.width = tabWidth;
                currentX += tabWidth;
            }
        } else {
            // Calculated widths
            for (Tab tab : tabs) {
                tab.width = GuiRenderUtils.getTextWidth(tab.title) + TAB_PADDING * 2;
                tab.x = currentX;
                currentX += tab.width;
            }
        }
        
        // Initialize indicator position
        if (!tabs.isEmpty() && selectedIndex < tabs.size()) {
            Tab selectedTab = tabs.get(selectedIndex);
            indicatorX = selectedTab.x;
            indicatorWidth = selectedTab.width;
        }
    }
    
    /**
     * Update animations
     */
    public void update() {
        if (tabs.isEmpty() || selectedIndex >= tabs.size()) return;
        
        Tab selectedTab = tabs.get(selectedIndex);
        
        // Animate indicator
        indicatorX += (selectedTab.x - indicatorX) * GuiTheme.ANIMATION_SPEED;
        indicatorWidth += (selectedTab.width - indicatorWidth) * GuiTheme.ANIMATION_SPEED;
    }
    
    /**
     * Render the tab bar
     */
    public void render(int mouseX, int mouseY) {
        update();
        
        // Draw background
        GuiRenderUtils.drawRoundedRect(x, y, width, height, GuiTheme.BORDER_RADIUS_MEDIUM, GuiTheme.GLASS_SECONDARY);
        
        // Draw tabs
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            boolean isSelected = (i == selectedIndex);
            boolean isHovered = tab.enabled && isTabHovered(tab, mouseX, mouseY);
            
            // Tab text color
            int textColor;
            if (!tab.enabled) {
                textColor = GuiTheme.TEXT_DISABLED;
            } else if (isSelected) {
                textColor = GuiTheme.ACCENT_PRIMARY;
            } else if (isHovered) {
                textColor = GuiTheme.TEXT_PRIMARY;
            } else {
                textColor = GuiTheme.TEXT_SECONDARY;
            }
            
            // Draw hover background
            if (isHovered && !isSelected) {
                GuiRenderUtils.drawRoundedRect(tab.x + 2, y + 2, tab.width - 4, height - 4 - INDICATOR_HEIGHT, 
                                               GuiTheme.BORDER_RADIUS_SMALL, GuiTheme.withAlpha(0xFFFFFF, 0.05f));
            }
            
            // Draw tab title centered
            int textWidth = GuiRenderUtils.getTextWidth(tab.title);
            int textX = tab.x + (tab.width - textWidth) / 2;
            int textY = y + (height - 8 - INDICATOR_HEIGHT) / 2;
            GuiRenderUtils.drawText(tab.title, textX, textY, textColor);
        }
        
        // Draw indicator
        int indicatorY = y + height - INDICATOR_HEIGHT;
        GuiRenderUtils.drawRoundedRect((int)indicatorX + 4, indicatorY, (int)indicatorWidth - 8, INDICATOR_HEIGHT, 
                                       INDICATOR_HEIGHT / 2, GuiTheme.ACCENT_PRIMARY);
    }
    
    /**
     * Handle mouse click
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0 || !isMouseOver(mouseX, mouseY)) return false;
        
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            if (tab.enabled && isTabHovered(tab, mouseX, mouseY)) {
                setSelectedIndex(i);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a tab is hovered
     */
    private boolean isTabHovered(Tab tab, int mouseX, int mouseY) {
        return mouseX >= tab.x && mouseX < tab.x + tab.width && mouseY >= y && mouseY < y + height;
    }
    
    /**
     * Check if mouse is over tab bar
     */
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    
    // ==================== GETTERS/SETTERS ====================
    
    public int getSelectedIndex() { return selectedIndex; }
    
    public void setSelectedIndex(int index) {
        if (index >= 0 && index < tabs.size() && tabs.get(index).enabled) {
            int oldIndex = selectedIndex;
            selectedIndex = index;
            
            if (onTabChange != null && oldIndex != index) {
                onTabChange.accept(index);
            }
        }
    }
    
    public Tab getSelectedTab() {
        return selectedIndex < tabs.size() ? tabs.get(selectedIndex) : null;
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        recalculateTabBounds();
    }
    
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        recalculateTabBounds();
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    
    public List<Tab> getTabs() { return tabs; }
    
    public void setOnTabChange(Consumer<Integer> onTabChange) {
        this.onTabChange = onTabChange;
    }
    
    public void setTabEnabled(int index, boolean enabled) {
        if (index >= 0 && index < tabs.size()) {
            tabs.get(index).enabled = enabled;
        }
    }
}
