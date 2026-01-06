package com.muzmod.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

/**
 * Utility class for rotation calculations and smooth camera movement
 */
public class RotationUtils {
    
    /**
     * Get yaw and pitch to look at a block position
     * @return float[]{yaw, pitch}
     */
    public static float[] getRotationsToBlock(Entity entity, BlockPos pos) {
        return getRotationsToPosition(entity, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
    
    /**
     * Get yaw and pitch to look at a position
     * @return float[]{yaw, pitch}
     */
    public static float[] getRotationsToPosition(Entity entity, double x, double y, double z) {
        double dx = x - entity.posX;
        double dy = y - (entity.posY + entity.getEyeHeight());
        double dz = z - entity.posZ;
        
        double dist = Math.sqrt(dx * dx + dz * dz);
        
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(Math.atan2(dy, dist) * 180.0 / Math.PI);
        
        return new float[]{yaw, pitch};
    }
    
    /**
     * Get yaw and pitch to look at an entity
     * @return float[]{yaw, pitch}
     */
    public static float[] getRotationsToEntity(Entity from, Entity to) {
        return getRotationsToPosition(from, to.posX, to.posY + to.getEyeHeight() / 2, to.posZ);
    }
    
    /**
     * Smoothly interpolate rotation towards target
     * @param entity The entity to rotate
     * @param targetYaw Target yaw angle
     * @param targetPitch Target pitch angle
     * @param speed Interpolation speed (0.0 - 1.0)
     */
    public static void smoothLookAt(EntityPlayer entity, float targetYaw, float targetPitch, float speed) {
        float currentYaw = entity.rotationYaw;
        float currentPitch = entity.rotationPitch;
        
        // Calculate shortest rotation path for yaw
        float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;
        
        // Apply smooth interpolation
        float newYaw = currentYaw + yawDiff * speed;
        float newPitch = currentPitch + pitchDiff * speed;
        
        // Clamp pitch
        newPitch = MathHelper.clamp_float(newPitch, -90.0f, 90.0f);
        
        entity.rotationYaw = newYaw;
        entity.rotationPitch = newPitch;
    }
    
    /**
     * Instantly look at target (no smoothing)
     */
    public static void lookAt(EntityPlayer entity, float yaw, float pitch) {
        entity.rotationYaw = yaw;
        entity.rotationPitch = MathHelper.clamp_float(pitch, -90.0f, 90.0f);
    }
    
    /**
     * Instantly look at a block
     */
    public static void lookAtBlock(EntityPlayer entity, BlockPos pos) {
        float[] rotations = getRotationsToBlock(entity, pos);
        lookAt(entity, rotations[0], rotations[1]);
    }
    
    /**
     * Smoothly look at a block
     */
    public static void smoothLookAtBlock(EntityPlayer entity, BlockPos pos, float speed) {
        float[] rotations = getRotationsToBlock(entity, pos);
        smoothLookAt(entity, rotations[0], rotations[1], speed);
    }
    
    /**
     * Get the distance between two angles
     */
    public static float getAngleDistance(float angle1, float angle2) {
        return Math.abs(MathHelper.wrapAngleTo180_float(angle1 - angle2));
    }
    
    /**
     * Check if entity is looking at a position (within threshold)
     */
    public static boolean isLookingAt(Entity entity, double x, double y, double z, float threshold) {
        float[] targetRotations = getRotationsToPosition(entity, x, y, z);
        
        float yawDiff = getAngleDistance(entity.rotationYaw, targetRotations[0]);
        float pitchDiff = Math.abs(entity.rotationPitch - targetRotations[1]);
        
        return yawDiff <= threshold && pitchDiff <= threshold;
    }
    
    /**
     * Add random variation to rotation for more natural movement
     */
    public static float[] addRandomVariation(float yaw, float pitch, float maxVariation) {
        float yawVar = (float) ((Math.random() - 0.5) * maxVariation * 2);
        float pitchVar = (float) ((Math.random() - 0.5) * maxVariation * 2);
        
        return new float[]{yaw + yawVar, pitch + pitchVar};
    }
}
