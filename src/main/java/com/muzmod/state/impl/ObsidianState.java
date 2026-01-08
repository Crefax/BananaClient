package com.muzmod.state.impl;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
import com.muzmod.state.AbstractState;
import com.muzmod.util.InputSimulator;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.*;

/**
 * Obsidian Mining State v2.0.0
 * 
 * Dynamic snake-like obsidian mining system.
 * 
 * Features:
 * - Warp to atolye
 * - Two-step initial movement (forward then side)
 * - Dynamic distance calculation based on available obsidian
 * - Snake pattern mining (always moving forward, occasional turns)
 * - Never stays in one place mining around
 */
public class ObsidianState extends AbstractState {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();
    private final ModConfig config = MuzMod.instance.getConfig();
    
    // Phases
    private ObsidianPhase phase = ObsidianPhase.WARPING;
    private long phaseStartTime = 0;
    private long lastActionTime = 0;
    
    // Warp settings
    private static final long WARP_DELAY = 3000;
    
    // Two-step initial movement
    private int firstWalkTarget = 0;      // First walk distance (forward)
    private int secondWalkTarget = 0;     // Second walk distance (side)
    private double walkedDistance = 0;
    private BlockPos walkStartPos = null;
    private boolean firstWalkDone = false;
    private boolean secondWalkDone = false;
    
    // Mining direction (yaw)
    private float miningYaw = 0;          // Current mining direction
    private int blocksMinedInRow = 0;     // Blocks mined in current direction
    private int targetBlocksInRow = 0;    // How many blocks to mine before turning
    
    // Current target obsidian
    private BlockPos currentTarget = null;
    private int obsidianY = -1;           // Y level of obsidian layer
    
    // Stuck detection
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;
    private long stuckCheckTime = 0;
    private BlockPos lastPosition = null;
    private int stuckCounter = 0;
    
    // Debug
    private String debugInfo = "";
    
    private enum ObsidianPhase {
        WARPING,           // Teleporting to atolye
        WAITING_WARP,      // Waiting for warp
        FIRST_WALK,        // Walking forward (dynamic distance)
        SECOND_WALK,       // Walking to side (dynamic distance)
        FIND_OBSIDIAN,     // Finding obsidian level
        MINING,            // Mining obsidian (snake pattern)
        WALKING_FORWARD,   // Walking forward on obsidian
        TURNING,           // Turning 90 degrees
        DONE,
        FAILED
    }
    
    public ObsidianState() {
        this.status = "Obsidian ready";
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        phase = ObsidianPhase.WARPING;
        phaseStartTime = System.currentTimeMillis();
        lastActionTime = System.currentTimeMillis();
        retryCount = 0;
        
        firstWalkTarget = 0;
        secondWalkTarget = 0;
        walkedDistance = 0;
        walkStartPos = null;
        firstWalkDone = false;
        secondWalkDone = false;
        
        miningYaw = 0;
        blocksMinedInRow = 0;
        targetBlocksInRow = 0;
        currentTarget = null;
        obsidianY = -1;
        
        stuckCounter = 0;
        lastPosition = null;
        
        InputSimulator.releaseAll();
        setStatus("Obsidian starting");
        
        MuzMod.LOGGER.info("[Obsidian] =============================");
        MuzMod.LOGGER.info("[Obsidian] OBSIDIAN STATE v2.0 STARTED");
        MuzMod.LOGGER.info("[Obsidian] Dynamic snake mining enabled");
        MuzMod.LOGGER.info("[Obsidian] =============================");
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        InputSimulator.releaseAll();
        MuzMod.LOGGER.info("[Obsidian] OBSIDIAN STATE STOPPED");
    }
    
    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        long now = System.currentTimeMillis();
        checkStuck(now);
        
        switch (phase) {
            case WARPING:
                handleWarping();
                break;
            case WAITING_WARP:
                handleWaitingWarp(now);
                break;
            case FIRST_WALK:
                handleFirstWalk();
                break;
            case SECOND_WALK:
                handleSecondWalk();
                break;
            case FIND_OBSIDIAN:
                handleFindObsidian();
                break;
            case MINING:
                handleMining();
                break;
            case WALKING_FORWARD:
                handleWalkingForward();
                break;
            case TURNING:
                handleTurning();
                break;
            case DONE:
                setStatus("Completed");
                MuzMod.instance.getStateManager().forceState("idle");
                break;
            case FAILED:
                setStatus("Failed!");
                MuzMod.instance.getStateManager().forceState("idle");
                break;
        }
    }
    
    // ==================== PHASE HANDLERS ====================
    
    private void handleWarping() {
        debugInfo = "Warping...";
        setStatus(debugInfo);
        
        String warpCommand = config.getObsidianWarpCommand();
        mc.thePlayer.sendChatMessage(warpCommand);
        MuzMod.LOGGER.info("[Obsidian] Warp sent: " + warpCommand);
        
        setPhase(ObsidianPhase.WAITING_WARP);
    }
    
    private void handleWaitingWarp(long now) {
        debugInfo = "Waiting for warp...";
        setStatus(debugInfo);
        
        if (now - phaseStartTime >= WARP_DELAY) {
            MuzMod.LOGGER.info("[Obsidian] Warp complete, calculating first walk...");
            setPhase(ObsidianPhase.FIRST_WALK);
        }
    }
    
    /**
     * First walk - Go forward dynamically based on obsidian ahead
     */
    private void handleFirstWalk() {
        if (walkStartPos == null) {
            walkStartPos = mc.thePlayer.getPosition();
            
            // Calculate how much obsidian is ahead
            int obsidianAhead = countObsidianInDirection(0); // Forward (current facing)
            
            if (obsidianAhead < 5) {
                // Not enough obsidian ahead, skip first walk
                MuzMod.LOGGER.info("[Obsidian] Not enough obsidian ahead (" + obsidianAhead + "), skipping first walk");
                firstWalkDone = true;
                walkStartPos = null;
                setPhase(ObsidianPhase.SECOND_WALK);
                return;
            }
            
            // Walk 30-80% of available distance
            float ratio = 0.3f + random.nextFloat() * 0.5f;
            firstWalkTarget = Math.max(5, (int)(obsidianAhead * ratio));
            
            MuzMod.LOGGER.info("[Obsidian] First walk: " + firstWalkTarget + " blocks (obsidian ahead: " + obsidianAhead + ")");
            
            debugInfo = "First walk: " + firstWalkTarget + " blocks";
            setStatus(debugInfo);
        }
        
        // Calculate walked distance
        walkedDistance = Math.sqrt(
            Math.pow(mc.thePlayer.posX - walkStartPos.getX(), 2) +
            Math.pow(mc.thePlayer.posZ - walkStartPos.getZ(), 2)
        );
        
        debugInfo = String.format("Walking forward... %.1f/%d", walkedDistance, firstWalkTarget);
        setStatus(debugInfo);
        
        // Check if arrived
        if (walkedDistance >= firstWalkTarget) {
            InputSimulator.releaseAll();
            MuzMod.LOGGER.info("[Obsidian] First walk complete");
            firstWalkDone = true;
            walkStartPos = null;
            walkedDistance = 0;
            setPhase(ObsidianPhase.SECOND_WALK);
            return;
        }
        
        // Keep walking forward
        InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
        
        // Jump if needed
        if (shouldJump()) {
            InputSimulator.holdKey(mc.gameSettings.keyBindJump, true);
        } else {
            InputSimulator.releaseKey(mc.gameSettings.keyBindJump);
        }
    }
    
    /**
     * Second walk - Go to side (left or right based on config)
     */
    private void handleSecondWalk() {
        if (walkStartPos == null) {
            walkStartPos = mc.thePlayer.getPosition();
            
            // Turn to side direction first
            boolean goLeft = config.isObsidianGoLeft();
            float targetYaw = mc.thePlayer.rotationYaw + (goLeft ? -90 : 90);
            mc.thePlayer.rotationYaw = targetYaw;
            
            // Count obsidian in side direction
            int obsidianSide = countObsidianInDirection(goLeft ? -90 : 90);
            
            if (obsidianSide < 3) {
                // Not enough obsidian to side, go to mining
                MuzMod.LOGGER.info("[Obsidian] Not enough obsidian to side (" + obsidianSide + "), starting mining");
                secondWalkDone = true;
                walkStartPos = null;
                setPhase(ObsidianPhase.FIND_OBSIDIAN);
                return;
            }
            
            // Walk 30-80% of available distance
            float ratio = 0.3f + random.nextFloat() * 0.5f;
            secondWalkTarget = Math.max(3, (int)(obsidianSide * ratio));
            
            MuzMod.LOGGER.info("[Obsidian] Second walk: " + secondWalkTarget + " blocks (obsidian side: " + obsidianSide + ")");
            
            debugInfo = "Second walk: " + secondWalkTarget + " blocks";
            setStatus(debugInfo);
        }
        
        // Calculate walked distance
        walkedDistance = Math.sqrt(
            Math.pow(mc.thePlayer.posX - walkStartPos.getX(), 2) +
            Math.pow(mc.thePlayer.posZ - walkStartPos.getZ(), 2)
        );
        
        debugInfo = String.format("Walking to side... %.1f/%d", walkedDistance, secondWalkTarget);
        setStatus(debugInfo);
        
        // Check if arrived
        if (walkedDistance >= secondWalkTarget) {
            InputSimulator.releaseAll();
            MuzMod.LOGGER.info("[Obsidian] Second walk complete");
            secondWalkDone = true;
            walkStartPos = null;
            walkedDistance = 0;
            setPhase(ObsidianPhase.FIND_OBSIDIAN);
            return;
        }
        
        // Keep walking forward (we already turned)
        InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
        
        if (shouldJump()) {
            InputSimulator.holdKey(mc.gameSettings.keyBindJump, true);
        } else {
            InputSimulator.releaseKey(mc.gameSettings.keyBindJump);
        }
    }
    
    /**
     * Find obsidian layer and set up mining direction
     */
    private void handleFindObsidian() {
        debugInfo = "Finding obsidian...";
        setStatus(debugInfo);
        
        InputSimulator.releaseAll();
        
        BlockPos playerPos = mc.thePlayer.getPosition();
        
        // Search for obsidian below and around
        for (int y = 0; y >= -5; y--) {
            BlockPos checkPos = playerPos.add(0, y, 0);
            if (mc.theWorld.getBlockState(checkPos).getBlock() == Blocks.obsidian) {
                obsidianY = checkPos.getY();
                MuzMod.LOGGER.info("[Obsidian] Found obsidian at Y=" + obsidianY);
                break;
            }
        }
        
        if (obsidianY == -1) {
            // Check nearby
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    for (int y = 2; y >= -5; y--) {
                        BlockPos checkPos = playerPos.add(x, y, z);
                        if (mc.theWorld.getBlockState(checkPos).getBlock() == Blocks.obsidian) {
                            obsidianY = checkPos.getY();
                            MuzMod.LOGGER.info("[Obsidian] Found obsidian nearby at Y=" + obsidianY);
                            break;
                        }
                    }
                    if (obsidianY != -1) break;
                }
                if (obsidianY != -1) break;
            }
        }
        
        if (obsidianY == -1) {
            MuzMod.LOGGER.warn("[Obsidian] No obsidian found!");
            retryOrFail();
            return;
        }
        
        // Set initial mining direction (current yaw, rounded to 90)
        miningYaw = Math.round(mc.thePlayer.rotationYaw / 90) * 90;
        
        // Calculate how many blocks to mine in this direction
        targetBlocksInRow = calculateTargetBlocks();
        blocksMinedInRow = 0;
        
        MuzMod.LOGGER.info("[Obsidian] Mining direction: " + miningYaw + "°, target: " + targetBlocksInRow + " blocks");
        
        setPhase(ObsidianPhase.MINING);
    }
    
    /**
     * Main mining phase - snake pattern
     * Always mine forward, never stay in place
     */
    private void handleMining() {
        BlockPos playerPos = mc.thePlayer.getPosition();
        
        // Find obsidian to mine in front
        currentTarget = findObsidianAhead();
        
        if (currentTarget == null) {
            // No obsidian ahead, need to turn
            MuzMod.LOGGER.info("[Obsidian] No obsidian ahead, turning...");
            setPhase(ObsidianPhase.TURNING);
            return;
        }
        
        // Check if we should turn (mined enough in this direction)
        if (blocksMinedInRow >= targetBlocksInRow) {
            MuzMod.LOGGER.info("[Obsidian] Reached target blocks (" + blocksMinedInRow + "), turning...");
            setPhase(ObsidianPhase.TURNING);
            return;
        }
        
        // Look at target
        float[] rotation = getRotationToBlock(currentTarget);
        smoothRotateTo(rotation[0], rotation[1]);
        
        // Check if block is still obsidian
        Block targetBlock = mc.theWorld.getBlockState(currentTarget).getBlock();
        if (targetBlock != Blocks.obsidian) {
            // Block was mined, move forward
            blocksMinedInRow++;
            MuzMod.LOGGER.info("[Obsidian] Block mined, total in row: " + blocksMinedInRow);
            setPhase(ObsidianPhase.WALKING_FORWARD);
            return;
        }
        
        // Check rotation
        float yawDiff = Math.abs(mc.thePlayer.rotationYaw - rotation[0]);
        while (yawDiff > 180) yawDiff -= 360;
        yawDiff = Math.abs(yawDiff);
        
        if (yawDiff < 10) {
            // Mining
            InputSimulator.holdLeftClick(true);
            debugInfo = String.format("Mining... (%d/%d in row)", blocksMinedInRow, targetBlocksInRow);
            setStatus(debugInfo);
        } else {
            InputSimulator.releaseLeftClick();
            debugInfo = "Turning to target...";
            setStatus(debugInfo);
        }
    }
    
    /**
     * Walk forward after mining a block
     */
    private void handleWalkingForward() {
        debugInfo = "Walking forward...";
        setStatus(debugInfo);
        
        InputSimulator.releaseLeftClick();
        
        // Face mining direction
        smoothRotateTo(miningYaw, 0);
        
        float yawDiff = Math.abs(mc.thePlayer.rotationYaw - miningYaw);
        while (yawDiff > 180) yawDiff -= 360;
        yawDiff = Math.abs(yawDiff);
        
        if (yawDiff > 15) {
            // Still turning
            return;
        }
        
        // Check if we're on obsidian or close to next target
        BlockPos playerPos = mc.thePlayer.getPosition();
        BlockPos ahead = getBlockAhead(1);
        
        // If ahead has obsidian at mining level, go to mining
        BlockPos obsidianAhead = new BlockPos(ahead.getX(), obsidianY, ahead.getZ());
        if (mc.theWorld.getBlockState(obsidianAhead).getBlock() == Blocks.obsidian) {
            InputSimulator.releaseAll();
            setPhase(ObsidianPhase.MINING);
            return;
        }
        
        // Walk forward
        InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
        
        // Check if we walked far enough (simple distance check)
        if (walkStartPos == null) {
            walkStartPos = playerPos;
        }
        
        double walked = Math.sqrt(
            Math.pow(mc.thePlayer.posX - walkStartPos.getX(), 2) +
            Math.pow(mc.thePlayer.posZ - walkStartPos.getZ(), 2)
        );
        
        if (walked >= 1.0) {
            InputSimulator.releaseAll();
            walkStartPos = null;
            setPhase(ObsidianPhase.MINING);
        }
    }
    
    /**
     * Turn 90 degrees for snake pattern
     */
    private void handleTurning() {
        debugInfo = "Turning...";
        setStatus(debugInfo);
        
        InputSimulator.releaseAll();
        
        // Decide turn direction (prefer alternating)
        boolean turnLeft = (blocksMinedInRow % 2 == 0) ? random.nextBoolean() : !random.nextBoolean();
        
        // Check which side has more obsidian
        int leftObs = countObsidianInDirection(-90);
        int rightObs = countObsidianInDirection(90);
        
        if (leftObs > rightObs + 3) {
            turnLeft = true;
        } else if (rightObs > leftObs + 3) {
            turnLeft = false;
        }
        
        float targetYaw = miningYaw + (turnLeft ? -90 : 90);
        
        // Normalize
        while (targetYaw > 180) targetYaw -= 360;
        while (targetYaw < -180) targetYaw += 360;
        
        smoothRotateTo(targetYaw, 0);
        
        float yawDiff = Math.abs(mc.thePlayer.rotationYaw - targetYaw);
        while (yawDiff > 180) yawDiff -= 360;
        yawDiff = Math.abs(yawDiff);
        
        if (yawDiff < 10) {
            // Turn complete
            miningYaw = targetYaw;
            blocksMinedInRow = 0;
            targetBlocksInRow = calculateTargetBlocks();
            
            MuzMod.LOGGER.info("[Obsidian] Turned to " + miningYaw + "°, new target: " + targetBlocksInRow + " blocks");
            setPhase(ObsidianPhase.MINING);
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private void setPhase(ObsidianPhase newPhase) {
        phase = newPhase;
        phaseStartTime = System.currentTimeMillis();
        lastActionTime = System.currentTimeMillis();
        MuzMod.LOGGER.info("[Obsidian] -> Phase: " + newPhase);
    }
    
    /**
     * Count obsidian blocks in a direction (relative to current facing)
     * @param angleOffset 0=forward, 90=right, -90=left
     */
    private int countObsidianInDirection(float angleOffset) {
        if (mc.thePlayer == null || mc.theWorld == null) return 0;
        
        BlockPos playerPos = mc.thePlayer.getPosition();
        float checkYaw = mc.thePlayer.rotationYaw + angleOffset;
        double rad = Math.toRadians(checkYaw);
        
        int count = 0;
        
        // Check up to 50 blocks in that direction
        for (int dist = 1; dist <= 50; dist++) {
            int checkX = playerPos.getX() - (int)(Math.sin(rad) * dist);
            int checkZ = playerPos.getZ() + (int)(Math.cos(rad) * dist);
            
            // Check a few Y levels around player
            boolean foundObsidian = false;
            for (int y = -3; y <= 3; y++) {
                BlockPos checkPos = new BlockPos(checkX, playerPos.getY() + y, checkZ);
                if (mc.theWorld.getBlockState(checkPos).getBlock() == Blocks.obsidian) {
                    foundObsidian = true;
                    break;
                }
            }
            
            if (foundObsidian) {
                count++;
            } else {
                // Gap in obsidian, stop counting
                break;
            }
        }
        
        return count;
    }
    
    /**
     * Calculate how many blocks to mine in current direction
     * Based on available obsidian
     */
    private int calculateTargetBlocks() {
        int obsidianAhead = countObsidianInDirection(0);
        
        if (obsidianAhead < 3) {
            return obsidianAhead;
        }
        
        // Mine 40-90% of available, randomized
        float ratio = 0.4f + random.nextFloat() * 0.5f;
        int target = (int)(obsidianAhead * ratio);
        
        // Clamp between 3 and 20
        return Math.max(3, Math.min(20, target));
    }
    
    /**
     * Find obsidian block directly ahead to mine
     */
    private BlockPos findObsidianAhead() {
        BlockPos playerPos = mc.thePlayer.getPosition();
        double rad = Math.toRadians(miningYaw);
        
        // Check 1-4 blocks ahead at obsidian level
        for (int dist = 1; dist <= 4; dist++) {
            int checkX = playerPos.getX() - (int)(Math.sin(rad) * dist);
            int checkZ = playerPos.getZ() + (int)(Math.cos(rad) * dist);
            
            BlockPos checkPos = new BlockPos(checkX, obsidianY, checkZ);
            if (mc.theWorld.getBlockState(checkPos).getBlock() == Blocks.obsidian) {
                return checkPos;
            }
        }
        
        return null;
    }
    
    /**
     * Get block position ahead
     */
    private BlockPos getBlockAhead(int distance) {
        double rad = Math.toRadians(mc.thePlayer.rotationYaw);
        int x = mc.thePlayer.getPosition().getX() - (int)(Math.sin(rad) * distance);
        int z = mc.thePlayer.getPosition().getZ() + (int)(Math.cos(rad) * distance);
        return new BlockPos(x, mc.thePlayer.getPosition().getY(), z);
    }
    
    private boolean shouldJump() {
        if (!mc.thePlayer.onGround) return false;
        
        double rad = Math.toRadians(mc.thePlayer.rotationYaw);
        double checkX = mc.thePlayer.posX - Math.sin(rad) * 0.8;
        double checkZ = mc.thePlayer.posZ + Math.cos(rad) * 0.8;
        
        BlockPos ahead = new BlockPos(checkX, mc.thePlayer.posY, checkZ);
        BlockPos aboveAhead = ahead.up();
        
        Block blockAhead = mc.theWorld.getBlockState(ahead).getBlock();
        
        boolean blocked = !mc.theWorld.isAirBlock(ahead) && !blockAhead.isPassable(mc.theWorld, ahead);
        boolean spaceAbove = mc.theWorld.isAirBlock(aboveAhead) && mc.theWorld.isAirBlock(aboveAhead.up());
        
        return blocked && spaceAbove;
    }
    
    private void checkStuck(long now) {
        if (now - stuckCheckTime < 2000) return;
        stuckCheckTime = now;
        
        BlockPos currentPos = mc.thePlayer.getPosition();
        
        if (lastPosition != null && lastPosition.equals(currentPos)) {
            stuckCounter++;
            
            if (stuckCounter >= 5) {
                MuzMod.LOGGER.warn("[Obsidian] Stuck detected, turning...");
                stuckCounter = 0;
                setPhase(ObsidianPhase.TURNING);
            }
        } else {
            stuckCounter = 0;
        }
        
        lastPosition = currentPos;
    }
    
    private void retryOrFail() {
        retryCount++;
        if (retryCount >= MAX_RETRIES) {
            MuzMod.LOGGER.error("[Obsidian] Max retries exceeded!");
            setPhase(ObsidianPhase.FAILED);
        } else {
            MuzMod.LOGGER.info("[Obsidian] Retry " + retryCount + "/" + MAX_RETRIES);
            setPhase(ObsidianPhase.WARPING);
        }
    }
    
    private float[] getRotationToBlock(BlockPos pos) {
        double dx = pos.getX() + 0.5 - mc.thePlayer.posX;
        double dy = pos.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = pos.getZ() + 0.5 - mc.thePlayer.posZ;
        
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        
        return new float[]{yaw, pitch};
    }
    
    private void smoothRotateTo(float targetYaw, float targetPitch) {
        float currentYaw = mc.thePlayer.rotationYaw;
        float currentPitch = mc.thePlayer.rotationPitch;
        
        float yawDiff = targetYaw - currentYaw;
        float pitchDiff = targetPitch - currentPitch;
        
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        float rotSpeed = 15.0f;
        float yawStep = Math.signum(yawDiff) * Math.min(Math.abs(yawDiff), rotSpeed);
        float pitchStep = Math.signum(pitchDiff) * Math.min(Math.abs(pitchDiff), rotSpeed);
        
        mc.thePlayer.rotationYaw += yawStep;
        mc.thePlayer.rotationPitch += pitchStep;
        
        mc.thePlayer.rotationPitch = Math.max(-90, Math.min(90, mc.thePlayer.rotationPitch));
    }
    
    @Override
    public String getName() {
        return "Obsidian";
    }
    
    @Override
    public boolean shouldActivate() {
        return false;
    }
    
    @Override
    public int getPriority() {
        return 3;
    }
    
    public String getDebugInfo() { return debugInfo; }
    public ObsidianPhase getPhase() { return phase; }
    public int getBlocksMinedInRow() { return blocksMinedInRow; }
}
