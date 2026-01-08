package com.muzmod.rotation;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
import com.muzmod.util.InputSimulator;
import com.muzmod.util.RotationUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

import java.util.Random;

/**
 * Position Adjuster v1.5.0
 * 
 * Handles position adjustments when mining gets stuck.
 * Uses smooth rotations and movement to find new ore positions.
 * Now turns towards mining center first when available.
 */
public class PositionAdjuster {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();
    private final SmoothRotation smoother = new SmoothRotation();
    
    // State
    private boolean isAdjusting = false;
    private long adjustStartTime = 0;
    private int adjustmentCount = 0;
    private boolean turnLeft = true;
    
    // Target
    private float targetYaw;
    private float targetPitch;
    
    // Config
    private static final float MIN_PITCH = 30f;  // Minimum 30 derece aşağı
    private static final float MAX_PITCH = 60f;  // Maximum 60 derece aşağı
    private static final long ADJUSTMENT_DURATION = 2000; // 2 seconds per adjustment (hızlandırıldı)
    
    // Callback interface for when ore is found
    public interface OreFoundCallback {
        void onOreFound(BlockPos pos, float yaw, float pitch);
    }
    
    private OreFoundCallback callback;
    
    /**
     * Start a position adjustment (backward compatibility - no mining center)
     * 
     * @param baseYaw   Base yaw to adjust from
     * @param basePitch Base pitch to adjust from
     * @param config    Mod config
     */
    public void startAdjustment(float baseYaw, float basePitch, ModConfig config) {
        startAdjustment(baseYaw, basePitch, config, null);
    }
    
    /**
     * Start a position adjustment with optional mining center
     * 
     * @param baseYaw       Base yaw to adjust from
     * @param basePitch     Base pitch to adjust from
     * @param config        Mod config
     * @param miningCenter  Mining center position (nullable) - önce buraya dönülür
     */
    public void startAdjustment(float baseYaw, float basePitch, ModConfig config, BlockPos miningCenter) {
        isAdjusting = true;
        adjustStartTime = System.currentTimeMillis();
        adjustmentCount++;
        
        // Get config values
        float yawMin = config.getAdjustYawMin();
        float yawMax = config.getAdjustYawMax();
        float pitchMin = config.getAdjustPitchMin();
        float pitchMax = config.getAdjustPitchMax();
        long smoothSpeed = config.getAdjustSmoothSpeed();
        
        float yawOffset;
        float pitchOffset;
        
        // Mining merkezi varsa, önce oraya doğru dön
        if (miningCenter != null && mc.thePlayer != null) {
            // Merkeze olan yönü hesapla
            float[] toCenter = RotationUtils.getRotationsToBlock(mc.thePlayer, miningCenter);
            float centerYaw = toCenter[0];
            
            // Mevcut yaw ile merkez yaw arasındaki fark
            float diffToCenter = centerYaw - mc.thePlayer.rotationYaw;
            while (diffToCenter > 180) diffToCenter -= 360;
            while (diffToCenter < -180) diffToCenter += 360;
            
            // Merkeze doğru dön, ama tam üzerine değil - hafif sapma ekle
            float deviation = 5 + random.nextFloat() * 15; // 5-20 derece sapma
            if (random.nextBoolean()) deviation = -deviation;
            
            targetYaw = centerYaw + deviation;
            yawOffset = targetYaw - baseYaw;
            
            // Pitch - hafif aşağı bak
            pitchOffset = -5 + random.nextFloat() * 10; // -5 to +5
            targetPitch = 40 + pitchOffset; // 35-45 arası
            
            MuzMod.LOGGER.info(String.format(
                "Position adjustment #%d: turning towards center (yaw=%.2f°, offset=%.2f°)",
                adjustmentCount, targetYaw, yawOffset
            ));
        } else {
            // Normal rastgele ayarlama (merkez yoksa)
            float yawAmount = yawMin + random.nextFloat() * (yawMax - yawMin);
            float pitchAmount = pitchMin + random.nextFloat() * (pitchMax - pitchMin);
            
            // Alternate direction
            if (turnLeft) {
                yawOffset = -yawAmount;
            } else {
                yawOffset = yawAmount;
            }
            turnLeft = !turnLeft;
            
            // Random pitch direction
            pitchOffset = (random.nextBoolean() ? 1 : -1) * pitchAmount;
            
            // Calculate target
            targetYaw = baseYaw + yawOffset;
            targetPitch = basePitch + pitchOffset;
            
            MuzMod.LOGGER.info(String.format(
                "Position adjustment #%d: yaw=%.2f° (offset %.2f°), pitch=%.2f° (offset %.2f°)",
                adjustmentCount, targetYaw, yawOffset, targetPitch, pitchOffset
            ));
        }
        
        // Clamp pitch (30-60 derece arası)
        if (targetPitch < MIN_PITCH) targetPitch = MIN_PITCH;
        if (targetPitch > MAX_PITCH) targetPitch = MAX_PITCH;
        
        // Start smooth rotation
        smoother.startRotation(targetYaw, targetPitch, smoothSpeed);
    }
    
    /**
     * Update adjustment - call every tick
     * 
     * @param config Mod config
     * @return AdjustmentResult indicating current state
     */
    public AdjustmentResult update(ModConfig config) {
        if (!isAdjusting || mc.thePlayer == null) {
            return AdjustmentResult.NOT_ADJUSTING;
        }
        
        // Update smooth rotation
        boolean stillRotating = smoother.update();
        
        // Calculate elapsed time
        long elapsed = System.currentTimeMillis() - adjustStartTime;
        
        // Move forward slowly while adjusting
        if (elapsed > 500) { // Wait a bit before moving
            InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
        }
        
        // Check for ore while adjusting
        if (config.shouldMineWhileMoving()) {
            BlockPos lookingAt = getLookingAtBlock();
            if (lookingAt != null) {
                Block block = mc.theWorld.getBlockState(lookingAt).getBlock();
                if (block == Blocks.quartz_ore) {
                    // Found ore!
                    InputSimulator.holdLeftClick(true);
                    InputSimulator.releaseMovementKeys();
                    
                    float foundYaw = mc.thePlayer.rotationYaw;
                    float foundPitch = mc.thePlayer.rotationPitch;
                    
                    if (callback != null) {
                        callback.onOreFound(lookingAt, foundYaw, foundPitch);
                    }
                    
                    isAdjusting = false;
                    return AdjustmentResult.ORE_FOUND;
                }
            }
        }
        
        // Check if adjustment is complete
        if (elapsed >= ADJUSTMENT_DURATION && !stillRotating) {
            InputSimulator.releaseAll();
            isAdjusting = false;
            return AdjustmentResult.COMPLETED;
        }
        
        return AdjustmentResult.IN_PROGRESS;
    }
    
    /**
     * Get block player is looking at
     */
    private BlockPos getLookingAtBlock() {
        if (mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null) {
            BlockPos pos = mc.objectMouseOver.getBlockPos();
            double dist = mc.thePlayer.getDistanceSq(pos);
            if (dist <= 25) {
                return pos;
            }
        }
        return null;
    }
    
    /**
     * Set callback for ore found event
     */
    public void setOreFoundCallback(OreFoundCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Check if currently adjusting
     */
    public boolean isAdjusting() {
        return isAdjusting;
    }
    
    /**
     * Get adjustment count
     */
    public int getAdjustmentCount() {
        return adjustmentCount;
    }
    
    /**
     * Get target yaw
     */
    public float getTargetYaw() {
        return targetYaw;
    }
    
    /**
     * Get target pitch
     */
    public float getTargetPitch() {
        return targetPitch;
    }
    
    /**
     * Get turn direction string
     */
    public String getTurnDirection() {
        return turnLeft ? "←" : "→";
    }
    
    /**
     * Cancel adjustment
     */
    public void cancel() {
        isAdjusting = false;
        smoother.cancel();
        InputSimulator.releaseAll();
    }
    
    /**
     * Reset state
     */
    public void reset() {
        cancel();
        adjustmentCount = 0;
        turnLeft = true;
    }
    
    /**
     * Result of adjustment update
     */
    public enum AdjustmentResult {
        NOT_ADJUSTING,
        IN_PROGRESS,
        ORE_FOUND,
        COMPLETED
    }
}
