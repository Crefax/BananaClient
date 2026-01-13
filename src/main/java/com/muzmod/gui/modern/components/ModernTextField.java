package com.muzmod.gui.modern.components;

import com.muzmod.gui.modern.GuiRenderUtils;
import com.muzmod.gui.modern.GuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/**
 * Modern Text Field Component
 * Glass effect with focus animation and placeholder support
 */
public class ModernTextField {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    
    private int x, y;
    private int width, height;
    
    private String text = "";
    private String placeholder = "";
    private int maxLength = 100;
    
    private boolean focused = false;
    private boolean hovered = false;
    private boolean numbersOnly = false;
    private boolean allowDecimals = false;
    private boolean allowNegative = false;
    
    // Cursor
    private int cursorPosition = 0;
    private int selectionStart = 0;
    private long cursorBlinkTime = 0;
    
    // Animation
    private float focusAnimation = 0f;
    
    // Padding
    private static final int PADDING_X = 10;
    private static final int PADDING_Y = 6;
    
    public ModernTextField(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public ModernTextField(int x, int y, int width, int height, String placeholder) {
        this(x, y, width, height);
        this.placeholder = placeholder;
    }
    
    /**
     * Update animations
     */
    public void update(int mouseX, int mouseY) {
        hovered = isMouseOver(mouseX, mouseY);
        
        float targetFocus = focused ? 1f : 0f;
        focusAnimation += (targetFocus - focusAnimation) * GuiTheme.ANIMATION_SPEED;
    }
    
    /**
     * Render the text field
     */
    public void render(int mouseX, int mouseY) {
        update(mouseX, mouseY);
        
        // Background color
        int bgColor = GuiTheme.lerpColor(GuiTheme.INPUT_DEFAULT, GuiTheme.INPUT_FOCUSED, focusAnimation);
        if (hovered && !focused) {
            bgColor = GuiTheme.INPUT_HOVER;
        }
        
        // Border color
        int borderColor = GuiTheme.lerpColor(GuiTheme.BORDER_DEFAULT, GuiTheme.ACCENT_PRIMARY, focusAnimation);
        
        // Draw background
        GuiRenderUtils.drawRoundedRect(x, y, width, height, GuiTheme.BORDER_RADIUS_SMALL, bgColor);
        
        // Draw border
        GuiRenderUtils.drawRoundedRectWithBorder(x, y, width, height, GuiTheme.BORDER_RADIUS_SMALL, 0x00000000, borderColor);
        
        // Enable scissor for text clipping
        GuiRenderUtils.enableScissor(x + PADDING_X, y, width - PADDING_X * 2, height);
        
        // Calculate text position
        int textY = y + (height - 8) / 2;
        int textX = x + PADDING_X;
        
        if (text.isEmpty() && !focused) {
            // Draw placeholder
            GuiRenderUtils.drawText(placeholder, textX, textY, GuiTheme.INPUT_PLACEHOLDER);
        } else {
            // Draw text
            GuiRenderUtils.drawText(text, textX, textY, GuiTheme.INPUT_TEXT);
            
            // Draw cursor
            if (focused && (System.currentTimeMillis() - cursorBlinkTime) % 1000 < 500) {
                String textBeforeCursor = text.substring(0, Math.min(cursorPosition, text.length()));
                int cursorX = textX + GuiRenderUtils.getTextWidth(textBeforeCursor);
                GuiRenderUtils.drawRect(cursorX, textY - 1, 1, 10, GuiTheme.ACCENT_PRIMARY);
            }
        }
        
        GuiRenderUtils.disableScissor();
    }
    
    /**
     * Handle mouse click
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            boolean wasClickedOn = isMouseOver(mouseX, mouseY);
            
            if (wasClickedOn) {
                focused = true;
                cursorBlinkTime = System.currentTimeMillis();
                
                // Calculate cursor position from click
                int relativeX = mouseX - x - PADDING_X;
                cursorPosition = getCharIndexFromX(relativeX);
                selectionStart = cursorPosition;
                
                return true;
            } else {
                focused = false;
            }
        }
        return false;
    }
    
    /**
     * Handle key press
     */
    public boolean keyTyped(char typedChar, int keyCode) {
        if (!focused) return false;
        
        cursorBlinkTime = System.currentTimeMillis();
        
        switch (keyCode) {
            case Keyboard.KEY_BACK:
                if (text.length() > 0 && cursorPosition > 0) {
                    text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
                    cursorPosition--;
                }
                return true;
                
            case Keyboard.KEY_DELETE:
                if (cursorPosition < text.length()) {
                    text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
                }
                return true;
                
            case Keyboard.KEY_LEFT:
                if (cursorPosition > 0) cursorPosition--;
                return true;
                
            case Keyboard.KEY_RIGHT:
                if (cursorPosition < text.length()) cursorPosition++;
                return true;
                
            case Keyboard.KEY_HOME:
                cursorPosition = 0;
                return true;
                
            case Keyboard.KEY_END:
                cursorPosition = text.length();
                return true;
                
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                focused = false;
                return true;
                
            case Keyboard.KEY_ESCAPE:
                focused = false;
                return true;
                
            case Keyboard.KEY_V:
                if (GuiScreen.isCtrlKeyDown()) {
                    String clipboard = GuiScreen.getClipboardString();
                    if (clipboard != null) {
                        insertText(clipboard);
                    }
                    return true;
                }
                break;
                
            case Keyboard.KEY_C:
                if (GuiScreen.isCtrlKeyDown()) {
                    GuiScreen.setClipboardString(text);
                    return true;
                }
                break;
                
            case Keyboard.KEY_A:
                if (GuiScreen.isCtrlKeyDown()) {
                    cursorPosition = text.length();
                    selectionStart = 0;
                    return true;
                }
                break;
        }
        
        // Regular character input
        if (isValidChar(typedChar)) {
            insertText(String.valueOf(typedChar));
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if character is valid for input
     */
    private boolean isValidChar(char c) {
        if (c < 32) return false; // Control characters
        
        if (numbersOnly) {
            if (Character.isDigit(c)) return true;
            if (allowDecimals && c == '.' && !text.contains(".")) return true;
            if (allowNegative && c == '-' && cursorPosition == 0 && !text.contains("-")) return true;
            return false;
        }
        
        return true;
    }
    
    /**
     * Insert text at cursor position
     */
    private void insertText(String str) {
        // Filter invalid characters for numbers only mode
        if (numbersOnly) {
            StringBuilder filtered = new StringBuilder();
            for (char c : str.toCharArray()) {
                if (isValidChar(c)) {
                    filtered.append(c);
                }
            }
            str = filtered.toString();
        }
        
        if (text.length() + str.length() <= maxLength) {
            text = text.substring(0, cursorPosition) + str + text.substring(cursorPosition);
            cursorPosition += str.length();
        }
    }
    
    /**
     * Get character index from X position
     */
    private int getCharIndexFromX(int x) {
        if (text.isEmpty()) return 0;
        
        int totalWidth = 0;
        for (int i = 0; i < text.length(); i++) {
            int charWidth = GuiRenderUtils.getTextWidth(String.valueOf(text.charAt(i)));
            if (totalWidth + charWidth / 2 >= x) {
                return i;
            }
            totalWidth += charWidth;
        }
        return text.length();
    }
    
    /**
     * Check if mouse is over field
     */
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    
    // ==================== GETTERS/SETTERS ====================
    
    public String getText() { return text; }
    public void setText(String text) {
        this.text = text;
        this.cursorPosition = Math.min(cursorPosition, text.length());
    }
    
    public int getIntValue() {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    public double getDoubleValue() {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    
    public String getPlaceholder() { return placeholder; }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }
    
    public int getMaxLength() { return maxLength; }
    public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
    
    public boolean isFocused() { return focused; }
    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) {
            cursorBlinkTime = System.currentTimeMillis();
        }
    }
    
    public void setNumbersOnly(boolean numbersOnly) { this.numbersOnly = numbersOnly; }
    public void setAllowDecimals(boolean allowDecimals) { this.allowDecimals = allowDecimals; }
    public void setAllowNegative(boolean allowNegative) { this.allowNegative = allowNegative; }
}
