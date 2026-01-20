package com.muzmod.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

/**
 * Simulates player input (key presses, mouse clicks)
 * Sadece KeyBinding sistemi kullanır - basit ve güvenilir
 */
public class InputSimulator {
    
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    private static boolean leftClickHeld = false;
    private static boolean rightClickHeld = false;
    
    /**
     * Sol tık basılı tut (kazma/saldırı)
     * KeyBinding state'i set eder - Minecraft kendi işini halleder
     */
    public static void holdLeftClick(boolean hold) {
        if (hold) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
            leftClickHeld = true;
        } else {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
            leftClickHeld = false;
        }
    }
    
    /**
     * Sol tık bırak
     */
    public static void releaseLeftClick() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        leftClickHeld = false;
    }
    
    /**
     * Sol tık basılı mı?
     */
    public static boolean isLeftClickHeld() {
        return leftClickHeld;
    }
    
    /**
     * Sağ tık basılı tut
     */
    public static void holdRightClick(boolean hold) {
        if (hold && !rightClickHeld) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
            rightClickHeld = true;
        } else if (!hold && rightClickHeld) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            rightClickHeld = false;
        }
    }
    
    /**
     * Sağ tık bırak
     */
    public static void releaseRightClick() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        rightClickHeld = false;
    }
    
    /**
     * Tuş basılı tut
     */
    public static void holdKey(KeyBinding keyBinding, boolean hold) {
        KeyBinding.setKeyBindState(keyBinding.getKeyCode(), hold);
    }
    
    /**
     * Tuşa bir kez bas
     */
    public static void pressKey(KeyBinding keyBinding) {
        KeyBinding.setKeyBindState(keyBinding.getKeyCode(), true);
        KeyBinding.onTick(keyBinding.getKeyCode());
    }
    
    /**
     * Tuşu bırak
     */
    public static void releaseKey(KeyBinding keyBinding) {
        KeyBinding.setKeyBindState(keyBinding.getKeyCode(), false);
    }
    
    /**
     * Hareket tuşlarını bırak
     */
    public static void releaseMovementKeys() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
    }
    
    /**
     * Tüm inputları bırak
     */
    public static void releaseAll() {
        releaseLeftClick();
        releaseRightClick();
        releaseMovementKeys();
    }
    
    /**
     * İleri yürü
     */
    public static void walkForward(boolean walk) {
        holdKey(mc.gameSettings.keyBindForward, walk);
    }
    
    /**
     * Geri yürü
     */
    public static void walkBackward(boolean walk) {
        holdKey(mc.gameSettings.keyBindBack, walk);
    }
    
    /**
     * Sola yürü
     */
    public static void strafeLeft(boolean strafe) {
        holdKey(mc.gameSettings.keyBindLeft, strafe);
    }
    
    /**
     * Sağa yürü
     */
    public static void strafeRight(boolean strafe) {
        holdKey(mc.gameSettings.keyBindRight, strafe);
    }
    
    /**
     * Zıpla
     */
    public static void jump() {
        pressKey(mc.gameSettings.keyBindJump);
    }
    
    /**
     * Eğil
     */
    public static void sneak(boolean sneak) {
        holdKey(mc.gameSettings.keyBindSneak, sneak);
    }
    
    /**
     * Koş
     */
    public static void sprint(boolean sprint) {
        holdKey(mc.gameSettings.keyBindSprint, sprint);
    }
    
    /**
     * A tuşu bas
     */
    public static void pressA() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), true);
    }
    
    /**
     * A tuşu bırak
     */
    public static void releaseA() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
    }
    
    /**
     * D tuşu bas
     */
    public static void pressD() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), true);
    }
    
    /**
     * D tuşu bırak
     */
    public static void releaseD() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
    }
}
