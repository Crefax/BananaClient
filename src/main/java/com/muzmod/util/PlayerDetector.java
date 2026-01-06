package com.muzmod.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for detecting nearby players
 */
public class PlayerDetector {
    
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    /**
     * Get all players within a certain radius of the given player
     * Excludes the player themselves
     */
    public static List<EntityPlayer> getNearbyPlayers(EntityPlayer player, double radius) {
        List<EntityPlayer> nearbyPlayers = new ArrayList<>();
        
        if (mc.theWorld == null) return nearbyPlayers;
        
        double radiusSq = radius * radius;
        
        for (EntityPlayer otherPlayer : mc.theWorld.playerEntities) {
            if (otherPlayer == player) continue;
            if (otherPlayer.isInvisible()) continue;
            
            double distSq = player.getDistanceSqToEntity(otherPlayer);
            if (distSq <= radiusSq) {
                nearbyPlayers.add(otherPlayer);
            }
        }
        
        return nearbyPlayers;
    }
    
    /**
     * Check if any player is within radius
     */
    public static boolean isPlayerNearby(EntityPlayer player, double radius) {
        return !getNearbyPlayers(player, radius).isEmpty();
    }
    
    /**
     * Get the closest player within radius
     */
    public static EntityPlayer getClosestPlayer(EntityPlayer player, double radius) {
        List<EntityPlayer> nearby = getNearbyPlayers(player, radius);
        
        if (nearby.isEmpty()) return null;
        
        EntityPlayer closest = null;
        double closestDist = Double.MAX_VALUE;
        
        for (EntityPlayer other : nearby) {
            double dist = player.getDistanceSqToEntity(other);
            if (dist < closestDist) {
                closestDist = dist;
                closest = other;
            }
        }
        
        return closest;
    }
    
    /**
     * Get direction to move away from nearest player
     * Returns an angle in degrees (yaw)
     */
    public static float getEscapeDirection(EntityPlayer player, double radius) {
        EntityPlayer closest = getClosestPlayer(player, radius);
        
        if (closest == null) return player.rotationYaw;
        
        // Calculate angle to closest player
        double dx = closest.posX - player.posX;
        double dz = closest.posZ - player.posZ;
        
        float angleToPlayer = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90;
        
        // Return opposite direction
        return angleToPlayer + 180;
    }
    
    /**
     * Check if a specific position is safe (no players nearby)
     */
    public static boolean isPositionSafe(BlockPos pos, double radius) {
        if (mc.theWorld == null || mc.thePlayer == null) return true;
        
        double radiusSq = radius * radius;
        
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;
            
            double distSq = player.getDistanceSq(pos);
            if (distSq <= radiusSq) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get names of nearby players
     */
    public static List<String> getNearbyPlayerNames(EntityPlayer player, double radius) {
        List<String> names = new ArrayList<>();
        
        for (EntityPlayer other : getNearbyPlayers(player, radius)) {
            names.add(other.getName());
        }
        
        return names;
    }
}
