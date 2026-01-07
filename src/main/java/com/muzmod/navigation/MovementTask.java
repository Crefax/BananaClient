package com.muzmod.navigation;

import net.minecraft.util.BlockPos;

/**
 * Hareket görevi
 */
public class MovementTask {
    
    public enum Type {
        GO_TO,          // Koordinata git
        GO_FORWARD,     // İleri git
        GO_DIRECTION,   // Yöne git
        ROTATE,         // Dön
        FOLLOW_PATH,    // Yolu takip et
        WAIT            // Bekle
    }
    
    private final Type type;
    private final BlockPos target;
    private final float targetYaw;
    private final float targetPitch;
    private final int distance;
    private final Direction direction;
    private final long waitTime;
    
    private long startTime;
    private boolean started = false;
    private boolean completed = false;
    private boolean failed = false;
    
    // GO_TO constructor
    public MovementTask(Type type, BlockPos target) {
        this.type = type;
        this.target = target;
        this.targetYaw = 0;
        this.targetPitch = 0;
        this.distance = 0;
        this.direction = null;
        this.waitTime = 0;
    }
    
    // ROTATE constructor
    public MovementTask(float yaw, float pitch) {
        this.type = Type.ROTATE;
        this.target = null;
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        this.distance = 0;
        this.direction = null;
        this.waitTime = 0;
    }
    
    // GO_DIRECTION constructor
    public MovementTask(Direction direction, int distance) {
        this.type = Type.GO_DIRECTION;
        this.target = null;
        this.targetYaw = 0;
        this.targetPitch = 0;
        this.distance = distance;
        this.direction = direction;
        this.waitTime = 0;
    }
    
    // GO_FORWARD constructor
    public MovementTask(int distance) {
        this.type = Type.GO_FORWARD;
        this.target = null;
        this.targetYaw = 0;
        this.targetPitch = 0;
        this.distance = distance;
        this.direction = Direction.FORWARD;
        this.waitTime = 0;
    }
    
    // WAIT constructor
    public MovementTask(long waitTimeMs) {
        this.type = Type.WAIT;
        this.target = null;
        this.targetYaw = 0;
        this.targetPitch = 0;
        this.distance = 0;
        this.direction = null;
        this.waitTime = waitTimeMs;
    }
    
    public void start() {
        this.startTime = System.currentTimeMillis();
        this.started = true;
    }
    
    public void complete() {
        this.completed = true;
    }
    
    public void fail() {
        this.failed = true;
    }
    
    // Getters
    public Type getType() { return type; }
    public BlockPos getTarget() { return target; }
    public float getTargetYaw() { return targetYaw; }
    public float getTargetPitch() { return targetPitch; }
    public int getDistance() { return distance; }
    public Direction getDirection() { return direction; }
    public long getWaitTime() { return waitTime; }
    public long getStartTime() { return startTime; }
    public boolean isStarted() { return started; }
    public boolean isCompleted() { return completed; }
    public boolean isFailed() { return failed; }
    
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    @Override
    public String toString() {
        switch (type) {
            case GO_TO:
                return "GoTo(" + target.getX() + "," + target.getY() + "," + target.getZ() + ")";
            case GO_FORWARD:
                return "GoForward(" + distance + ")";
            case GO_DIRECTION:
                return "Go" + direction + "(" + distance + ")";
            case ROTATE:
                return "Rotate(" + targetYaw + "," + targetPitch + ")";
            case WAIT:
                return "Wait(" + waitTime + "ms)";
            default:
                return type.toString();
        }
    }
}
