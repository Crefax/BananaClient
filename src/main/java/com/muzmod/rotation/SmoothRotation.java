package com.muzmod.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

/**
 * Smooth Rotation Manager v1.4.0
 * 
 * Handles smooth, natural-looking rotations for anti-AFK and position adjustments.
 * All rotation changes are interpolated over time for a more human-like appearance.
 */
public class SmoothRotation {
    
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    // Current rotation state
    private boolean isRotating = false;
    private float startYaw;
    private float startPitch;
    private float targetYaw;
    private float targetPitch;
    private long rotationStartTime;
    private long rotationDuration;
    
    // Easing type
    public enum EasingType {
        LINEAR,
        EASE_IN_OUT,
        EASE_OUT,
        EASE_IN
    }
    
    private EasingType currentEasing = EasingType.EASE_IN_OUT;
    
    /**
     * Start a smooth rotation to target angles
     * 
     * @param targetYaw   Target yaw angle
     * @param targetPitch Target pitch angle  
     * @param durationMs  Duration in milliseconds (smooth speed)
     */
    public void startRotation(float targetYaw, float targetPitch, long durationMs) {
        if (mc.thePlayer == null) return;
        
        this.startYaw = mc.thePlayer.rotationYaw;
        this.startPitch = mc.thePlayer.rotationPitch;
        this.targetYaw = targetYaw;
        this.targetPitch = MathHelper.clamp_float(targetPitch, -90f, 90f);
        this.rotationStartTime = System.currentTimeMillis();
        this.rotationDuration = Math.max(50, durationMs); // Minimum 50ms
        this.isRotating = true;
    }
    
    /**
     * Start a relative rotation from current position
     * 
     * @param yawOffset   Yaw offset to add
     * @param pitchOffset Pitch offset to add
     * @param durationMs  Duration in milliseconds
     */
    public void startRelativeRotation(float yawOffset, float pitchOffset, long durationMs) {
        if (mc.thePlayer == null) return;
        
        float currentYaw = mc.thePlayer.rotationYaw;
        float currentPitch = mc.thePlayer.rotationPitch;
        
        startRotation(currentYaw + yawOffset, currentPitch + pitchOffset, durationMs);
    }
    
    /**
     * Update rotation - call this every tick
     * Returns true if still rotating, false if done
     */
    public boolean update() {
        if (!isRotating || mc.thePlayer == null) {
            return false;
        }
        
        long elapsed = System.currentTimeMillis() - rotationStartTime;
        float progress = Math.min(1.0f, (float) elapsed / rotationDuration);
        
        // Apply easing
        float easedProgress = applyEasing(progress, currentEasing);
        
        // Calculate shortest yaw path
        float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - startYaw);
        float pitchDiff = targetPitch - startPitch;
        
        // Interpolate
        float newYaw = startYaw + yawDiff * easedProgress;
        float newPitch = startPitch + pitchDiff * easedProgress;
        
        // Apply
        mc.thePlayer.rotationYaw = newYaw;
        mc.thePlayer.rotationPitch = MathHelper.clamp_float(newPitch, -90f, 90f);
        
        // Check if done
        if (progress >= 1.0f) {
            isRotating = false;
            return false;
        }
        
        return true;
    }
    
    /**
     * Apply easing function to progress
     */
    private float applyEasing(float t, EasingType type) {
        switch (type) {
            case LINEAR:
                return t;
                
            case EASE_IN:
                // Quadratic ease in
                return t * t;
                
            case EASE_OUT:
                // Quadratic ease out
                return t * (2 - t);
                
            case EASE_IN_OUT:
                // Smooth ease in-out (sine based)
                return t < 0.5f 
                    ? 2 * t * t 
                    : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
                
            default:
                return t;
        }
    }
    
    /**
     * Check if currently rotating
     */
    public boolean isRotating() {
        return isRotating;
    }
    
    /**
     * Cancel current rotation
     */
    public void cancel() {
        isRotating = false;
    }
    
    /**
     * Set easing type
     */
    public void setEasing(EasingType easing) {
        this.currentEasing = easing;
    }
    
    /**
     * Get remaining rotation time in ms
     */
    public long getRemainingTime() {
        if (!isRotating) return 0;
        long elapsed = System.currentTimeMillis() - rotationStartTime;
        return Math.max(0, rotationDuration - elapsed);
    }
    
    /**
     * Get current progress (0.0 - 1.0)
     */
    public float getProgress() {
        if (!isRotating) return 1.0f;
        long elapsed = System.currentTimeMillis() - rotationStartTime;
        return Math.min(1.0f, (float) elapsed / rotationDuration);
    }
}
