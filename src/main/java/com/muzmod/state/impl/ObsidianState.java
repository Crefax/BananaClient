package com.muzmod.state.impl;

import com.muzmod.MuzMod;
import com.muzmod.state.AbstractState;
import com.muzmod.util.InputSimulator;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * Obsidian Mining State v3.1.0
 * 
 * Bot obsidyenlerin ÜSTÜNDE yürür (Y=5) ve ayağının altındaki
 * Y=4'teki obsidyenleri kazarak ilerler.
 * 
 * Features:
 * - Player walks ON TOP of obsidian (Y=5, standing on Y=4 obsidian)
 * - Mines obsidian blocks at Y=4 (under/in front of player)
 * - RED marker for current target
 * - YELLOW marker for next target  
 * - Always moving forward while mining
 * - Auto turns to side with more obsidian
 */
public class ObsidianState extends AbstractState {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();
    
    // Obsidian is at Y=4, player stands on it at Y=5
    private static final int OBSIDIAN_Y = 4;
    private static final int PLAYER_Y = 5;
    
    // Current phase
    private ObsidianPhase phase = ObsidianPhase.INIT;
    
    // Target blocks
    private BlockPos redTarget = null;    // Current target (RED)
    private BlockPos yellowTarget = null; // Next target (YELLOW)
    
    // Current block being mined
    private BlockPos currentMiningBlock = null;
    
    // Direction
    private float currentYaw = 0;
    private int dirX = 0;  // Direction vector X
    private int dirZ = 0;  // Direction vector Z
    
    // Timing
    private long lastMineTime = 0;
    
    // Debug info
    private String debugInfo = "";
    
    private enum ObsidianPhase {
        INIT,           // Initialize
        CLIMB_UP,       // Climb up when Y < 4
        FIND_TARGET,    // Find target ahead
        MINING,         // Mining forward
        TURNING,        // Turning to new direction
        DONE,
        FAILED
    }
    
    public ObsidianState() {
        this.status = "Obsidian ready";
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        phase = ObsidianPhase.INIT;
        redTarget = null;
        yellowTarget = null;
        currentMiningBlock = null;
        currentYaw = 0;
        dirX = 0;
        dirZ = 0;
        lastMineTime = 0;
        
        MinecraftForge.EVENT_BUS.register(this);
        MuzMod.LOGGER.info("[Obsidian] State enabled - v3.1.0");
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        InputSimulator.releaseAll();
        MinecraftForge.EVENT_BUS.unregister(this);
        redTarget = null;
        yellowTarget = null;
        currentMiningBlock = null;
        MuzMod.LOGGER.info("[Obsidian] State disabled");
    }
    
    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        long now = System.currentTimeMillis();
        
        // Check Y level - if below 4, need to climb up
        int playerY = (int) Math.floor(mc.thePlayer.posY);
        if (playerY < OBSIDIAN_Y && phase != ObsidianPhase.CLIMB_UP && phase != ObsidianPhase.INIT) {
            MuzMod.LOGGER.info("[Obsidian] Y=" + playerY + " is below " + OBSIDIAN_Y + ", switching to CLIMB_UP");
            phase = ObsidianPhase.CLIMB_UP;
        }
        
        switch (phase) {
            case INIT:
                handleInit();
                break;
            case CLIMB_UP:
                handleClimbUp();
                break;
            case FIND_TARGET:
                handleFindTarget();
                break;
            case MINING:
                handleMining(now);
                break;
            case TURNING:
                handleTurning();
                break;
            case DONE:
            case FAILED:
                InputSimulator.releaseAll();
                break;
        }
    }
    
    /**
     * Initialize - set direction based on player facing
     */
    private void handleInit() {
        debugInfo = "Initializing...";
        setStatus(debugInfo);
        
        // Check Y level first
        int playerY = (int) Math.floor(mc.thePlayer.posY);
        if (playerY < OBSIDIAN_Y) {
            MuzMod.LOGGER.info("[Obsidian] Init - Y=" + playerY + " too low, climbing up first");
            phase = ObsidianPhase.CLIMB_UP;
            return;
        }
        
        // Get current facing direction and snap to nearest 90 degrees
        currentYaw = Math.round(mc.thePlayer.rotationYaw / 90f) * 90f;
        mc.thePlayer.rotationYaw = currentYaw;
        
        // Calculate direction vector
        updateDirectionVector();
        
        MuzMod.LOGGER.info("[Obsidian] Init - Yaw: " + currentYaw + ", Dir: (" + dirX + ", " + dirZ + ")");
        
        phase = ObsidianPhase.FIND_TARGET;
    }
    
    /**
     * Climb up when Y is below obsidian level
     * Mine obsidian above and jump to get to Y=4+
     */
    private void handleClimbUp() {
        int playerY = (int) Math.floor(mc.thePlayer.posY);
        
        debugInfo = "Climbing up... Y=" + playerY;
        setStatus(debugInfo);
        
        // Check if we're at the right level now
        if (playerY >= OBSIDIAN_Y) {
            MuzMod.LOGGER.info("[Obsidian] Reached Y=" + playerY + ", resuming mining");
            InputSimulator.releaseAll();
            phase = ObsidianPhase.FIND_TARGET;
            return;
        }
        
        int playerX = (int) Math.floor(mc.thePlayer.posX);
        int playerZ = (int) Math.floor(mc.thePlayer.posZ);
        
        // Look for obsidian above us to mine
        BlockPos above1 = new BlockPos(playerX, playerY + 1, playerZ);
        BlockPos above2 = new BlockPos(playerX, playerY + 2, playerZ);
        
        // Also check blocks around above us
        BlockPos[] checkPositions = {
            above1, above2,
            above1.north(), above1.south(), above1.east(), above1.west(),
            above2.north(), above2.south(), above2.east(), above2.west()
        };
        
        BlockPos toMine = null;
        for (BlockPos pos : checkPositions) {
            if (mc.theWorld.getBlockState(pos).getBlock() == Blocks.obsidian) {
                toMine = pos;
                break;
            }
        }
        
        if (toMine != null) {
            currentMiningBlock = toMine;
            
            // Look UP at the block
            lookAtBlock(toMine);
            
            // Mine it
            InputSimulator.holdKey(mc.gameSettings.keyBindAttack, true);
        } else {
            currentMiningBlock = null;
            InputSimulator.releaseKey(mc.gameSettings.keyBindAttack);
            
            // Look up
            mc.thePlayer.rotationPitch = -60;
        }
        
        // Always try to jump to get higher
        InputSimulator.holdKey(mc.gameSettings.keyBindJump, true);
        
        // Move around a bit to find a way up
        InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
    }
    
    /**
     * Update direction vector from yaw
     */
    private void updateDirectionVector() {
        // Normalize yaw to 0-360
        float yaw = currentYaw;
        while (yaw < 0) yaw += 360;
        while (yaw >= 360) yaw -= 360;
        
        // Convert to direction vector
        // 0/360 = South (+Z), 90 = West (-X), 180 = North (-Z), 270 = East (+X)
        if (yaw >= 315 || yaw < 45) {
            dirX = 0; dirZ = 1;  // South
        } else if (yaw >= 45 && yaw < 135) {
            dirX = -1; dirZ = 0; // West
        } else if (yaw >= 135 && yaw < 225) {
            dirX = 0; dirZ = -1; // North
        } else {
            dirX = 1; dirZ = 0;  // East
        }
    }
    
    /**
     * Find target - count obsidian ahead and set red target
     */
    private void handleFindTarget() {
        debugInfo = "Finding target...";
        setStatus(debugInfo);
        
        int playerX = (int) Math.floor(mc.thePlayer.posX);
        int playerZ = (int) Math.floor(mc.thePlayer.posZ);
        
        // Count obsidian ahead at Y=4
        int obsidianCount = countObsidianAhead(playerX, playerZ);
        
        MuzMod.LOGGER.info("[Obsidian] Found " + obsidianCount + " obsidian ahead");
        
        if (obsidianCount < 2) {
            // Try turning
            int leftCount = countObsidianInDirection(playerX, playerZ, -90);
            int rightCount = countObsidianInDirection(playerX, playerZ, 90);
            
            MuzMod.LOGGER.info("[Obsidian] Left: " + leftCount + ", Right: " + rightCount);
            
            if (leftCount >= 2 || rightCount >= 2) {
                if (leftCount > rightCount) {
                    currentYaw -= 90;
                } else {
                    currentYaw += 90;
                }
                normalizeYaw();
                updateDirectionVector();
                mc.thePlayer.rotationYaw = currentYaw;
                MuzMod.LOGGER.info("[Obsidian] Turned to " + currentYaw);
                return;
            } else {
                MuzMod.LOGGER.info("[Obsidian] No obsidian found, done");
                phase = ObsidianPhase.DONE;
                return;
            }
        }
        
        // Pick target distance: between (count-10) and count
        int minDist = Math.max(2, obsidianCount - 10);
        int targetDist = minDist + random.nextInt(Math.min(11, obsidianCount - minDist + 1));
        
        // Set red target
        redTarget = new BlockPos(playerX + dirX * targetDist, OBSIDIAN_Y, playerZ + dirZ * targetDist);
        
        MuzMod.LOGGER.info("[Obsidian] Red target: " + redTarget + " (dist: " + targetDist + ")");
        
        // Calculate yellow target (preview next direction)
        calculateYellowTarget();
        
        phase = ObsidianPhase.MINING;
    }
    
    /**
     * Calculate yellow target for next turn preview
     */
    private void calculateYellowTarget() {
        if (redTarget == null) {
            yellowTarget = null;
            return;
        }
        
        int leftCount = countObsidianInDirection(redTarget.getX(), redTarget.getZ(), -90);
        int rightCount = countObsidianInDirection(redTarget.getX(), redTarget.getZ(), 90);
        
        int nextCount = Math.max(leftCount, rightCount);
        if (nextCount >= 2) {
            // Get next direction
            float nextYaw = currentYaw + (leftCount > rightCount ? -90 : 90);
            float tempYaw = currentYaw;
            currentYaw = nextYaw;
            normalizeYaw();
            updateDirectionVector();
            
            int minDist = Math.max(2, nextCount - 10);
            int targetDist = minDist + random.nextInt(Math.min(11, nextCount - minDist + 1));
            
            yellowTarget = new BlockPos(
                redTarget.getX() + dirX * targetDist,
                OBSIDIAN_Y,
                redTarget.getZ() + dirZ * targetDist
            );
            
            // Restore direction
            currentYaw = tempYaw;
            normalizeYaw();
            updateDirectionVector();
            
            MuzMod.LOGGER.info("[Obsidian] Yellow target: " + yellowTarget);
        } else {
            yellowTarget = null;
        }
    }
    
    /**
     * Mining - walk forward while mining obsidian under/ahead
     */
    private void handleMining(long now) {
        if (redTarget == null) {
            phase = ObsidianPhase.FIND_TARGET;
            return;
        }
        
        int playerX = (int) Math.floor(mc.thePlayer.posX);
        int playerZ = (int) Math.floor(mc.thePlayer.posZ);
        
        // Check if reached target
        double distToTarget = Math.sqrt(
            Math.pow(playerX - redTarget.getX(), 2) +
            Math.pow(playerZ - redTarget.getZ(), 2)
        );
        
        if (distToTarget < 1.5) {
            MuzMod.LOGGER.info("[Obsidian] Reached red target!");
            InputSimulator.releaseAll();
            
            // Yellow becomes red
            redTarget = yellowTarget;
            yellowTarget = null;
            
            phase = ObsidianPhase.TURNING;
            return;
        }
        
        debugInfo = String.format("Mining... dist: %.1f", distToTarget);
        setStatus(debugInfo);
        
        // Find obsidian to mine - check in front at Y=4 (under player level)
        BlockPos toMine = findObsidianToMine(playerX, playerZ);
        
        if (toMine != null) {
            currentMiningBlock = toMine;
            
            // Look at the block
            lookAtBlock(toMine);
            
            // Mine it
            InputSimulator.holdKey(mc.gameSettings.keyBindAttack, true);
            
            // Also walk forward
            InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
        } else {
            // No obsidian to mine, just walk
            currentMiningBlock = null;
            InputSimulator.releaseKey(mc.gameSettings.keyBindAttack);
            InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
            
            // Look forward
            mc.thePlayer.rotationYaw = currentYaw;
            mc.thePlayer.rotationPitch = 45; // Look down slightly
        }
    }
    
    /**
     * Find obsidian block to mine in front of player at Y=4
     */
    private BlockPos findObsidianToMine(int playerX, int playerZ) {
        // Check positions in front at Y=4 (obsidian level)
        for (int dist = 0; dist <= 2; dist++) {
            int checkX = playerX + dirX * dist;
            int checkZ = playerZ + dirZ * dist;
            
            BlockPos checkPos = new BlockPos(checkX, OBSIDIAN_Y, checkZ);
            Block block = mc.theWorld.getBlockState(checkPos).getBlock();
            
            if (block == Blocks.obsidian) {
                return checkPos;
            }
        }
        
        // Also check directly below player
        BlockPos below = new BlockPos(playerX, OBSIDIAN_Y, playerZ);
        if (mc.theWorld.getBlockState(below).getBlock() == Blocks.obsidian) {
            return below;
        }
        
        return null;
    }
    
    /**
     * Look at a block position
     */
    private void lookAtBlock(BlockPos pos) {
        double dx = pos.getX() + 0.5 - mc.thePlayer.posX;
        double dy = pos.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = pos.getZ() + 0.5 - mc.thePlayer.posZ;
        
        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        
        // Clamp pitch
        targetPitch = Math.max(-90, Math.min(90, targetPitch));
        
        // Smooth rotation
        float yawDiff = targetYaw - mc.thePlayer.rotationYaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        mc.thePlayer.rotationYaw += yawDiff * 0.5f;
        mc.thePlayer.rotationPitch += (targetPitch - mc.thePlayer.rotationPitch) * 0.5f;
    }
    
    /**
     * Handle turning to new direction
     */
    private void handleTurning() {
        debugInfo = "Turning...";
        setStatus(debugInfo);
        
        InputSimulator.releaseAll();
        
        int playerX = (int) Math.floor(mc.thePlayer.posX);
        int playerZ = (int) Math.floor(mc.thePlayer.posZ);
        
        // Check sides
        int leftCount = countObsidianInDirection(playerX, playerZ, -90);
        int rightCount = countObsidianInDirection(playerX, playerZ, 90);
        
        MuzMod.LOGGER.info("[Obsidian] Turn check - Left: " + leftCount + ", Right: " + rightCount);
        
        if (leftCount < 2 && rightCount < 2) {
            // Check behind
            int behindCount = countObsidianInDirection(playerX, playerZ, 180);
            if (behindCount >= 2) {
                currentYaw += 180;
                MuzMod.LOGGER.info("[Obsidian] Turning back");
            } else {
                MuzMod.LOGGER.info("[Obsidian] No obsidian anywhere, done");
                phase = ObsidianPhase.DONE;
                return;
            }
        } else if (leftCount > rightCount) {
            currentYaw -= 90;
            MuzMod.LOGGER.info("[Obsidian] Turning left");
        } else {
            currentYaw += 90;
            MuzMod.LOGGER.info("[Obsidian] Turning right");
        }
        
        normalizeYaw();
        updateDirectionVector();
        mc.thePlayer.rotationYaw = currentYaw;
        
        phase = ObsidianPhase.FIND_TARGET;
    }
    
    /**
     * Count obsidian ahead in current direction at Y=4
     */
    private int countObsidianAhead(int startX, int startZ) {
        int count = 0;
        
        for (int i = 0; i <= 100; i++) {
            int x = startX + dirX * i;
            int z = startZ + dirZ * i;
            
            BlockPos checkPos = new BlockPos(x, OBSIDIAN_Y, z);
            Block block = mc.theWorld.getBlockState(checkPos).getBlock();
            
            if (block == Blocks.obsidian) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Count obsidian in a direction (relative to current direction)
     */
    private int countObsidianInDirection(int startX, int startZ, float yawOffset) {
        // Save current direction
        int oldDirX = dirX;
        int oldDirZ = dirZ;
        float oldYaw = currentYaw;
        
        // Rotate direction
        currentYaw += yawOffset;
        normalizeYaw();
        updateDirectionVector();
        
        int count = countObsidianAhead(startX, startZ);
        
        // Restore direction
        dirX = oldDirX;
        dirZ = oldDirZ;
        currentYaw = oldYaw;
        
        return count;
    }
    
    /**
     * Normalize yaw to -180 to 180
     */
    private void normalizeYaw() {
        while (currentYaw > 180) currentYaw -= 360;
        while (currentYaw < -180) currentYaw += 360;
    }
    
    // ==================== RENDERING ====================
    
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!enabled || mc.thePlayer == null) return;
        
        // Render red target
        if (redTarget != null) {
            renderBlock(redTarget, 1.0f, 0.0f, 0.0f, 0.6f); // RED
        }
        
        // Render yellow target
        if (yellowTarget != null) {
            renderBlock(yellowTarget, 1.0f, 1.0f, 0.0f, 0.4f); // YELLOW
        }
        
        // Render current mining block
        if (currentMiningBlock != null) {
            renderBlock(currentMiningBlock, 0.0f, 1.0f, 0.0f, 0.5f); // GREEN
        }
    }
    
    /**
     * Render a highlighted block
     */
    private void renderBlock(BlockPos pos, float r, float g, float b, float alpha) {
        double x = pos.getX() - mc.getRenderManager().viewerPosX;
        double y = pos.getY() - mc.getRenderManager().viewerPosY;
        double z = pos.getZ() - mc.getRenderManager().viewerPosZ;
        
        // Convert to 0-255 int values
        int ri = (int)(r * 255);
        int gi = (int)(g * 255);
        int bi = (int)(b * 255);
        int ai = (int)(alpha * 255);
        int ao = 255; // Full opacity for outline
        
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        
        // Draw filled box
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        
        // Bottom
        wr.pos(x, y, z).color(ri, gi, bi, ai).endVertex();
        wr.pos(x + 1, y, z).color(ri, gi, bi, ai).endVertex();
        wr.pos(x + 1, y, z + 1).color(ri, gi, bi, ai).endVertex();
        wr.pos(x, y, z + 1).color(ri, gi, bi, ai).endVertex();
        
        // Top
        wr.pos(x, y + 1, z).color(ri, gi, bi, ai).endVertex();
        wr.pos(x, y + 1, z + 1).color(ri, gi, bi, ai).endVertex();
        wr.pos(x + 1, y + 1, z + 1).color(ri, gi, bi, ai).endVertex();
        wr.pos(x + 1, y + 1, z).color(ri, gi, bi, ai).endVertex();
        
        // North
        wr.pos(x, y, z).color(ri, gi, bi, ai).endVertex();
        wr.pos(x, y + 1, z).color(ri, gi, bi, ai).endVertex();
        wr.pos(x + 1, y + 1, z).color(ri, gi, bi, ai).endVertex();
        wr.pos(x + 1, y, z).color(ri, gi, bi, ai).endVertex();
        
        // South
        wr.pos(x, y, z + 1).color(ri, gi, bi, ai).endVertex();
        wr.pos(x + 1, y, z + 1).color(ri, gi, bi, ai).endVertex();
        wr.pos(x + 1, y + 1, z + 1).color(ri, gi, bi, ai).endVertex();
        wr.pos(x, y + 1, z + 1).color(ri, gi, bi, ai).endVertex();
        
        // West
        wr.pos(x, y, z).color(ri, gi, bi, ai).endVertex();
        wr.pos(x, y, z + 1).color(ri, gi, bi, ai).endVertex();
        wr.pos(x, y + 1, z + 1).color(ri, gi, bi, ai).endVertex();
        wr.pos(x, y + 1, z).color(ri, gi, bi, ai).endVertex();
        
        // East
        wr.pos(x + 1, y, z).color(ri, gi, bi, ai).endVertex();
        wr.pos(x + 1, y + 1, z).color(ri, gi, bi, ai).endVertex();
        wr.pos(x + 1, y + 1, z + 1).color(ri, gi, bi, ai).endVertex();
        wr.pos(x + 1, y, z + 1).color(ri, gi, bi, ai).endVertex();
        
        tessellator.draw();
        
        // Draw outline
        GL11.glLineWidth(3.0f);
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        
        wr.pos(x, y, z).color(ri, gi, bi, ao).endVertex();
        wr.pos(x + 1, y, z).color(ri, gi, bi, ao).endVertex();
        wr.pos(x + 1, y, z + 1).color(ri, gi, bi, ao).endVertex();
        wr.pos(x, y, z + 1).color(ri, gi, bi, ao).endVertex();
        wr.pos(x, y, z).color(ri, gi, bi, ao).endVertex();
        
        wr.pos(x, y + 1, z).color(ri, gi, bi, ao).endVertex();
        wr.pos(x + 1, y + 1, z).color(ri, gi, bi, ao).endVertex();
        wr.pos(x + 1, y + 1, z + 1).color(ri, gi, bi, ao).endVertex();
        wr.pos(x, y + 1, z + 1).color(ri, gi, bi, ao).endVertex();
        wr.pos(x, y + 1, z).color(ri, gi, bi, ao).endVertex();
        
        tessellator.draw();
        
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
    
    // ==================== INTERFACE METHODS ====================
    
    @Override
    public String getName() {
        return "Obsidian";
    }
    
    @Override
    public boolean shouldActivate() {
        return false; // Manual activation only
    }
    
    @Override
    public int getPriority() {
        return 5;
    }
}
