package com.muzmod.rotation;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
import net.minecraft.client.Minecraft;

import java.util.Random;

/**
 * Anti-AFK Rotation System v1.4.0
 * 
 * Generates random smooth rotations while mining to appear more human-like
 * and prevent AFK detection.
 * 
 * Features:
 * - Configurable min/max rotation angles (0.1 to 100+ degrees)
 * - Smooth interpolated movement (not instant)
 * - Configurable smooth speed
 * - Random interval timing
 * - Alternating left/right preference
 */
public class AntiAfkRotation {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();
    private final SmoothRotation smoother = new SmoothRotation();
    
    // Timing
    private long lastRotationTime = 0;
    private long nextRotationDelay = 0;
    
    // State
    private boolean turnLeft = true;
    
    // Pitch limits (30-60 derece arası)
    private static final float MIN_PITCH = 30f;
    private static final float MAX_PITCH = 60f;
    
    /**
     * Update anti-AFK - call every tick while mining
     * 
     * @param config ModConfig to read settings from
     * @return true if a rotation is in progress
     */
    public boolean update(ModConfig config) {
        if (mc.thePlayer == null) return false;
        
        // Continue any in-progress rotation
        if (smoother.isRotating()) {
            smoother.update();
            return true;
        }
        
        // Check if it's time for new rotation
        long now = System.currentTimeMillis();
        if (now - lastRotationTime < nextRotationDelay) {
            return false;
        }
        
        // Start new rotation
        startNewRotation(config);
        lastRotationTime = now;
        
        // Random delay for next rotation
        long baseInterval = config.getAntiAfkInterval();
        nextRotationDelay = baseInterval + random.nextInt((int)(baseInterval * 0.5));
        
        return true;
    }
    
    /**
     * Start a new random rotation
     */
    private void startNewRotation(ModConfig config) {
        // Get config values
        float yawMin = config.getAntiAfkYawMin();
        float yawMax = config.getAntiAfkYawMax();
        float pitchMin = config.getAntiAfkPitchMin();
        float pitchMax = config.getAntiAfkPitchMax();
        long smoothSpeed = config.getAntiAfkSmoothSpeed();
        
        // Calculate random angles within range
        float yawAmount = yawMin + random.nextFloat() * (yawMax - yawMin);
        float pitchAmount = pitchMin + random.nextFloat() * (pitchMax - pitchMin);
        
        // Alternate direction
        float yawOffset;
        if (turnLeft) {
            yawOffset = -yawAmount;
        } else {
            yawOffset = yawAmount;
        }
        turnLeft = !turnLeft;
        
        // Random pitch direction
        float pitchOffset = (random.nextBoolean() ? 1 : -1) * pitchAmount;
        
        // Clamp final pitch (30-60 derece arası)
        float currentPitch = mc.thePlayer.rotationPitch;
        float targetPitch = currentPitch + pitchOffset;
        
        if (targetPitch < MIN_PITCH) {
            pitchOffset = MIN_PITCH - currentPitch;
        } else if (targetPitch > MAX_PITCH) {
            pitchOffset = MAX_PITCH - currentPitch;
        }
        
        // Start smooth rotation
        smoother.startRelativeRotation(yawOffset, pitchOffset, smoothSpeed);
        
        MuzMod.LOGGER.debug(String.format(
            "AntiAFK rotation: yaw=%.2f°, pitch=%.2f°, duration=%dms",
            yawOffset, pitchOffset, smoothSpeed
        ));
    }
    
    /**
     * Force an immediate rotation (for manual trigger)
     */
    public void forceRotation(ModConfig config) {
        lastRotationTime = 0;
        nextRotationDelay = 0;
        update(config);
    }
    
    /**
     * Check if currently rotating
     */
    public boolean isRotating() {
        return smoother.isRotating();
    }
    
    /**
     * Cancel current rotation
     */
    public void cancel() {
        smoother.cancel();
    }
    
    /**
     * Reset state (call when mining stops/starts)
     */
    public void reset() {
        smoother.cancel();
        lastRotationTime = 0;
        nextRotationDelay = 0;
        turnLeft = true;
    }
    
    /**
     * Get the underlying smooth rotator for advanced use
     */
    public SmoothRotation getSmoother() {
        return smoother;
    }
}
