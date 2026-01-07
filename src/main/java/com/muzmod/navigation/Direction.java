package com.muzmod.navigation;

/**
 * Yön enumları
 */
public enum Direction {
    NORTH,      // -Z
    SOUTH,      // +Z
    EAST,       // +X
    WEST,       // -X
    FORWARD,    // Oyuncunun baktığı yön
    BACKWARD,   // Oyuncunun arkası
    LEFT,       // Oyuncunun solu
    RIGHT;      // Oyuncunun sağı
    
    /**
     * Yaw açısından yön al
     */
    public static Direction fromYaw(float yaw) {
        // Normalize yaw to 0-360
        yaw = yaw % 360;
        if (yaw < 0) yaw += 360;
        
        if (yaw >= 315 || yaw < 45) {
            return SOUTH;
        } else if (yaw >= 45 && yaw < 135) {
            return WEST;
        } else if (yaw >= 135 && yaw < 225) {
            return NORTH;
        } else {
            return EAST;
        }
    }
    
    /**
     * Yönün yaw açısı
     */
    public float toYaw() {
        switch (this) {
            case NORTH: return 180;
            case SOUTH: return 0;
            case EAST: return -90;
            case WEST: return 90;
            default: return 0;
        }
    }
    
    /**
     * Karşı yön
     */
    public Direction opposite() {
        switch (this) {
            case NORTH: return SOUTH;
            case SOUTH: return NORTH;
            case EAST: return WEST;
            case WEST: return EAST;
            case FORWARD: return BACKWARD;
            case BACKWARD: return FORWARD;
            case LEFT: return RIGHT;
            case RIGHT: return LEFT;
            default: return this;
        }
    }
    
    /**
     * Sol taraf
     */
    public Direction left() {
        switch (this) {
            case NORTH: return WEST;
            case SOUTH: return EAST;
            case EAST: return NORTH;
            case WEST: return SOUTH;
            default: return LEFT;
        }
    }
    
    /**
     * Sağ taraf
     */
    public Direction right() {
        switch (this) {
            case NORTH: return EAST;
            case SOUTH: return WEST;
            case EAST: return SOUTH;
            case WEST: return NORTH;
            default: return RIGHT;
        }
    }
}
