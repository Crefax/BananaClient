package com.muzmod.gui.modern;

/**
 * Modern GUI Theme - Apple LiquidGlass inspired design
 * Centralized color palette and styling constants
 */
public class GuiTheme {
    
    // ==================== GLASS EFFECTS ====================
    public static final int GLASS_PRIMARY = 0x80FFFFFF;      // White glass overlay
    public static final int GLASS_DARK = 0x90000000;         // Dark glass (main panels)
    public static final int GLASS_MEDIUM = 0x70101010;       // Medium dark glass
    public static final int GLASS_LIGHT = 0x50FFFFFF;        // Light glass highlight
    public static final int GLASS_ACCENT = 0x60FFFFFF;       // Accent glass
    public static final int GLASS_SECONDARY = 0x40000000;    // Secondary glass
    
    // ==================== BLUR BACKGROUND ====================
    public static final int BLUR_OVERLAY = 0xC0000000;       // Background blur overlay
    public static final int BLUR_PANEL = 0xB0101010;         // Panel blur
    
    // ==================== ACCENT COLORS ====================
    public static final int ACCENT_PRIMARY = 0xFF6366F1;     // Indigo (primary)
    public static final int ACCENT_SECONDARY = 0xFF8B5CF6;   // Violet (secondary)
    public static final int ACCENT_GRADIENT_START = 0xFF6366F1;
    public static final int ACCENT_GRADIENT_END = 0xFF8B5CF6;
    
    public static final int ACCENT_SUCCESS = 0xFF22C55E;     // Green
    public static final int ACCENT_WARNING = 0xFFF59E0B;     // Amber
    public static final int ACCENT_DANGER = 0xFFEF4444;      // Red
    public static final int ACCENT_INFO = 0xFF3B82F6;        // Blue
    public static final int ACCENT_CYAN = 0xFF06B6D4;        // Cyan
    
    // ==================== TEXT COLORS ====================
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;       // White
    public static final int TEXT_SECONDARY = 0xFFD1D5DB;     // Light gray
    public static final int TEXT_MUTED = 0xFF9CA3AF;         // Muted gray
    public static final int TEXT_DISABLED = 0xFF6B7280;      // Disabled gray
    public static final int TEXT_DARK = 0xFF374151;          // Dark text
    
    // ==================== SURFACE COLORS ====================
    public static final int SURFACE_PRIMARY = 0xE01F2937;    // Dark blue-gray
    public static final int SURFACE_SECONDARY = 0xE0374151;  // Medium gray
    public static final int SURFACE_ELEVATED = 0xE04B5563;   // Elevated surface
    public static final int SURFACE_CARD = 0xD01F2937;       // Card background
    
    // ==================== BORDER COLORS ====================
    public static final int BORDER_DEFAULT = 0x30FFFFFF;     // Subtle white border
    public static final int BORDER_HOVER = 0x50FFFFFF;       // Hover border
    public static final int BORDER_FOCUS = 0xFF6366F1;       // Focus border (accent)
    public static final int BORDER_DIVIDER = 0x20FFFFFF;     // Divider line
    public static final int BORDER_MUTED = 0x15FFFFFF;       // Muted border
    
    // ==================== BUTTON STATES ====================
    public static final int BTN_DEFAULT = 0x40FFFFFF;        // Default button
    public static final int BTN_HOVER = 0x50FFFFFF;          // Hover state
    public static final int BTN_ACTIVE = 0x60FFFFFF;         // Active/pressed
    public static final int BTN_DISABLED = 0x20FFFFFF;       // Disabled
    
    // Aliases for components
    public static final int BUTTON_DEFAULT = BTN_DEFAULT;
    public static final int BUTTON_HOVER = BTN_HOVER;
    public static final int BUTTON_ACTIVE = BTN_ACTIVE;
    public static final int BUTTON_DISABLED = BTN_DISABLED;
    
    public static final int BTN_PRIMARY = 0xFF6366F1;        // Primary button
    public static final int BTN_PRIMARY_HOVER = 0xFF818CF8;  // Primary hover
    public static final int BTN_SUCCESS = 0xFF22C55E;        // Success button
    public static final int BTN_DANGER = 0xFFEF4444;         // Danger button
    
    // Semantic color aliases
    public static final int SUCCESS = ACCENT_SUCCESS;
    public static final int WARNING = ACCENT_WARNING;
    public static final int DANGER = ACCENT_DANGER;
    public static final int INFO = ACCENT_INFO;
    
    // ==================== INPUT FIELD ====================
    public static final int INPUT_BG = 0x40000000;           // Input background
    public static final int INPUT_BG_FOCUS = 0x50000000;     // Focused input
    public static final int INPUT_BORDER = 0x30FFFFFF;       // Input border
    public static final int INPUT_BORDER_FOCUS = 0xFF6366F1; // Focused border
    
    // Aliases for components
    public static final int INPUT_DEFAULT = INPUT_BG;
    public static final int INPUT_HOVER = 0x45000000;
    public static final int INPUT_FOCUSED = INPUT_BG_FOCUS;
    public static final int INPUT_PLACEHOLDER = TEXT_MUTED;
    public static final int INPUT_TEXT = TEXT_PRIMARY;
    
    // ==================== TOGGLE/SWITCH ====================
    public static final int TOGGLE_OFF_BG = 0xFFDC2626;      // Toggle off background (RED)
    public static final int TOGGLE_OFF_KNOB = 0xFFFFFFFF;    // Toggle off knob (white)
    public static final int TOGGLE_ON_BG = 0xFF22C55E;       // Toggle on background (GREEN)
    public static final int TOGGLE_ON_KNOB = 0xFFFFFFFF;     // Toggle on knob (white)
    
    // ==================== SCROLLBAR ====================
    public static final int SCROLL_TRACK = 0x20FFFFFF;       // Scroll track
    public static final int SCROLL_THUMB = 0x40FFFFFF;       // Scroll thumb
    public static final int SCROLL_THUMB_HOVER = 0x60FFFFFF; // Scroll thumb hover
    
    // Aliases for components
    public static final int SCROLLBAR_TRACK = SCROLL_TRACK;
    public static final int SCROLLBAR_THUMB = SCROLL_THUMB;
    public static final int SCROLLBAR_THUMB_HOVER = SCROLL_THUMB_HOVER;
    public static final int SCROLLBAR_THUMB_ACTIVE = 0x80FFFFFF;
    
    // ==================== SHADOWS ====================
    public static final int SHADOW_LIGHT = 0x20000000;       // Light shadow
    public static final int SHADOW_MEDIUM = 0x40000000;      // Medium shadow
    public static final int SHADOW_DARK = 0x60000000;        // Dark shadow
    
    // ==================== DIMENSIONS ====================
    public static final int CORNER_RADIUS = 8;               // Default corner radius
    public static final int CORNER_RADIUS_SM = 4;            // Small radius
    public static final int CORNER_RADIUS_LG = 12;           // Large radius
    public static final int CORNER_RADIUS_XL = 16;           // Extra large
    
    // Aliases for components
    public static final int BORDER_RADIUS_SMALL = CORNER_RADIUS_SM;
    public static final int BORDER_RADIUS_MEDIUM = CORNER_RADIUS;
    public static final int BORDER_RADIUS_LARGE = CORNER_RADIUS_LG;
    
    public static final int PADDING_XS = 4;
    public static final int PADDING_SM = 8;
    public static final int PADDING_MD = 12;
    public static final int PADDING_LG = 16;
    public static final int PADDING_XL = 24;
    
    public static final int SPACING_XS = 4;
    public static final int SPACING_SM = 8;
    public static final int SPACING_MD = 12;
    public static final int SPACING_LG = 16;
    public static final int SPACING_XL = 24;
    
    // ==================== ANIMATION ====================
    public static final float ANIMATION_SPEED = 0.15f;       // Animation interpolation speed
    public static final int ANIMATION_DURATION_MS = 200;     // Animation duration
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Interpolate between two colors
     */
    public static int lerpColor(int from, int to, float progress) {
        int fa = (from >> 24) & 0xFF;
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        
        int ta = (to >> 24) & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        
        int a = (int) (fa + (ta - fa) * progress);
        int r = (int) (fr + (tr - fr) * progress);
        int g = (int) (fg + (tg - fg) * progress);
        int b = (int) (fb + (tb - fb) * progress);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Get color with modified alpha
     */
    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
    
    /**
     * Get color with modified alpha (float 0-1)
     */
    public static int withAlpha(int color, float alpha) {
        return withAlpha(color, (int)(alpha * 255));
    }
    
    /**
     * Brighten a color
     */
    public static int brighten(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int)(((color >> 16) & 0xFF) * (1 + factor)));
        int g = Math.min(255, (int)(((color >> 8) & 0xFF) * (1 + factor)));
        int b = Math.min(255, (int)((color & 0xFF) * (1 + factor)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Darken a color
     */
    public static int darken(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int)(((color >> 16) & 0xFF) * (1 - factor));
        int g = (int)(((color >> 8) & 0xFF) * (1 - factor));
        int b = (int)((color & 0xFF) * (1 - factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
