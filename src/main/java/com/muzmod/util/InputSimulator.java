package com.muzmod.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovingObjectPosition;

/**
 * Simulates realistic player input (key presses, mouse clicks)
 * Uses KeyBinding system for realistic behavior
 * Also supports direct attack simulation when window not focused
 */
public class InputSimulator {
    
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    private static boolean leftClickHeld = false;
    private static boolean rightClickHeld = false;
    private static long lastSwingTime = 0;
    private static final long SWING_INTERVAL = 50; // 50ms aralıklarla swing
    
    /**
     * Hold left click (attack/mine)
     * Her çağrıda KeyBinding state'i zorla set eder
     * Focus yoksa bile direkt kazma işlemini yapar
     */
    public static void holdLeftClick(boolean hold) {
        KeyBinding attackKey = mc.gameSettings.keyBindAttack;
        KeyBinding.setKeyBindState(attackKey.getKeyCode(), hold);
        
        if (hold) {
            // onTick çağırarak Minecraft'a "tuş basıldı" sinyali ver
            KeyBinding.onTick(attackKey.getKeyCode());
            leftClickHeld = true;
            
            // Focus yoksa veya GUI'den yeni çıkıldıysa manuel kazma yap
            if (!mc.inGameHasFocus && mc.thePlayer != null && mc.theWorld != null) {
                forceAttack();
            }
        } else {
            leftClickHeld = false;
        }
    }
    
    /**
     * Focus olmasa bile kazma işlemini zorla başlat ve devam ettir
     * Her çağrıda agresif şekilde kazma yapar
     */
    public static void forceAttack() {
        if (mc.thePlayer == null || mc.theWorld == null || mc.playerController == null) return;
        
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            // Her tick'te kazma ilerlemesini zorla
            // onPlayerDamageBlock true dönerse kazma devam ediyor
            // false dönerse yeni blok veya kazma başlamadı
            boolean isHitting = mc.playerController.onPlayerDamageBlock(mop.getBlockPos(), mop.sideHit);
            
            // Her durumda clickBlock da çağır - kazma başlatmayı garantile
            if (!isHitting) {
                mc.playerController.clickBlock(mop.getBlockPos(), mop.sideHit);
            }
            
            // Swing animasyonu - her 50ms'de bir
            long now = System.currentTimeMillis();
            if (now - lastSwingTime >= SWING_INTERVAL) {
                mc.thePlayer.swingItem();
                lastSwingTime = now;
            }
        }
    }
    
    /**
     * Kazma işlemini yeniden başlat (GUI kapandıktan sonra)
     * Key binding'i sıfırlayıp tekrar aktifleştir
     */
    public static void restartMining() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        KeyBinding attackKey = mc.gameSettings.keyBindAttack;
        
        // Önce bırak
        KeyBinding.setKeyBindState(attackKey.getKeyCode(), false);
        
        // Sonra tekrar bas ve onTick çağır
        KeyBinding.setKeyBindState(attackKey.getKeyCode(), true);
        KeyBinding.onTick(attackKey.getKeyCode());
        
        leftClickHeld = true;
        
        // Ayrıca manuel olarak da kazma başlat
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            mc.playerController.clickBlock(mop.getBlockPos(), mop.sideHit);
            mc.thePlayer.swingItem();
        }
    }
    
    /**
     * Release left click
     */
    public static void releaseLeftClick() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        leftClickHeld = false;
    }
    
    /**
     * Check if left click is being held by us
     */
    public static boolean isLeftClickHeld() {
        return leftClickHeld;
    }
    
    /**
     * Hold right click (use/place)
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
     * Release right click
     */
    public static void releaseRightClick() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        rightClickHeld = false;
    }
    
    /**
     * Hold a specific key
     */
    public static void holdKey(KeyBinding keyBinding, boolean hold) {
        KeyBinding.setKeyBindState(keyBinding.getKeyCode(), hold);
    }
    
    /**
     * Press a key once
     */
    public static void pressKey(KeyBinding keyBinding) {
        KeyBinding.setKeyBindState(keyBinding.getKeyCode(), true);
        KeyBinding.onTick(keyBinding.getKeyCode());
    }
    
    /**
     * Release a specific key
     */
    public static void releaseKey(KeyBinding keyBinding) {
        KeyBinding.setKeyBindState(keyBinding.getKeyCode(), false);
    }
    
    /**
     * Release all movement keys
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
     * Release all inputs
     */
    public static void releaseAll() {
        releaseLeftClick();
        releaseRightClick();
        releaseMovementKeys();
    }
    
    /**
     * Simulate walking forward
     */
    public static void walkForward(boolean walk) {
        holdKey(mc.gameSettings.keyBindForward, walk);
    }
    
    /**
     * Simulate walking backward
     */
    public static void walkBackward(boolean walk) {
        holdKey(mc.gameSettings.keyBindBack, walk);
    }
    
    /**
     * Simulate strafing left
     */
    public static void strafeLeft(boolean strafe) {
        holdKey(mc.gameSettings.keyBindLeft, strafe);
    }
    
    /**
     * Simulate strafing right
     */
    public static void strafeRight(boolean strafe) {
        holdKey(mc.gameSettings.keyBindRight, strafe);
    }
    
    /**
     * Simulate jumping
     */
    public static void jump() {
        pressKey(mc.gameSettings.keyBindJump);
    }
    
    /**
     * Hold sneak
     */
    public static void sneak(boolean sneak) {
        holdKey(mc.gameSettings.keyBindSneak, sneak);
    }
    
    /**
     * Simulate sprint
     */
    public static void sprint(boolean sprint) {
        holdKey(mc.gameSettings.keyBindSprint, sprint);
    }
    
    /**
     * Press A key (strafe left)
     */
    public static void pressA() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), true);
    }
    
    /**
     * Release A key
     */
    public static void releaseA() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
    }
    
    /**
     * Press D key (strafe right)
     */
    public static void pressD() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), true);
    }
    
    /**
     * Release D key
     */
    public static void releaseD() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
    }
}
