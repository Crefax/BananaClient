package com.muzmod.state.impl;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
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
 * Obsidian Mining State v3.0.0
 * 
 * Simple and effective obsidian mining system.
 * 
 * Features:
 * - Works on 100x100 obsidian field at Y=4
 * - Counts obsidian ahead and picks random target (max-10 to max)
 * - RED marker for current target
 * - YELLOW marker for next target
 * - Smooth aim while mining forward
 * - Auto turns to side with more obsidian
 * - Stair mining if Y drops below 4
 */
public class ObsidianState extends AbstractState {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();
    
    // Target Y level for obsidian field
    private static final int TARGET_Y = 4;
    
    // Current phase
    private ObsidianPhase phase = ObsidianPhase.INIT;
    
    // Target blocks
    private BlockPos redTarget = null;    // Current target (RED)
    private BlockPos yellowTarget = null; // Next target (YELLOW)
    
    // Current mining target
    private BlockPos currentMiningBlock = null;
    
    // Direction (0=North/-Z, 90=East/+X, 180=South/+Z, 270=West/-X)
    private float currentYaw = 0;
    
    // Timing
    private long lastMineTime = 0;
    private static final long MINE_DELAY = 50; // ms between mining attempts
    
    // Stuck detection
    private BlockPos lastPos = null;
    private long stuckTime = 0;
    private static final long STUCK_THRESHOLD = 3000;
    
    // Debug info
    private String debugInfo = "";
    
    private enum ObsidianPhase {
        INIT,           // Initialize, find direction
        FIND_TARGET,    // Find red target ahead
        MINING,         // Mining towards red target
        TURNING,        // Turning to next direction
        STAIR_UP,       // Building stairs to get back to Y=4
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
        lastMineTime = 0;
        lastPos = null;
        stuckTime = 0;
        
        // Register for render events
        MinecraftForge.EVENT_BUS.register(this);
        
        MuzMod.LOGGER.info("[Obsidian] State enabled - v3.0.0");
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        InputSimulator.releaseAll();
        
        // Unregister render events
        MinecraftForge.EVENT_BUS.unregister(this);
        
        redTarget = null;
        yellowTarget = null;
        
        MuzMod.LOGGER.info("[Obsidian] State disabled");
    }
    
    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        long now = System.currentTimeMillis();
        
        // Check Y level
        int playerY = (int) Math.floor(mc.thePlayer.posY);
        if (playerY < TARGET_Y && phase != ObsidianPhase.STAIR_UP && phase != ObsidianPhase.INIT) {
            MuzMod.LOGGER.info("[Obsidian] Y dropped to " + playerY + ", switching to stair mode");
            phase = ObsidianPhase.STAIR_UP;
        }
        
        // Stuck detection
        BlockPos currentPos = mc.thePlayer.getPosition();
        if (lastPos != null && lastPos.equals(currentPos)) {
            if (stuckTime == 0) stuckTime = now;
            else if (now - stuckTime > STUCK_THRESHOLD && phase == ObsidianPhase.MINING) {
                MuzMod.LOGGER.info("[Obsidian] Stuck detected, jumping");
                InputSimulator.holdKey(mc.gameSettings.keyBindJump, true);
                try { Thread.sleep(100); } catch (Exception ignored) {}
                InputSimulator.releaseKey(mc.gameSettings.keyBindJump);
                stuckTime = 0;
            }
        } else {
            lastPos = currentPos;
            stuckTime = 0;
        }
        
        switch (phase) {
            case INIT:
                handleInit();
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
            case STAIR_UP:
                handleStairUp(now);
                break;
            case DONE:
            case FAILED:
                InputSimulator.releaseAll();
                break;
        }
    }
    
    /**
     * Initialize - check Y level and set initial direction
     */
    private void handleInit() {
        debugInfo = "Initializing...";
        setStatus(debugInfo);
        
        int playerY = (int) Math.floor(mc.thePlayer.posY);
        
        if (playerY != TARGET_Y) {
            MuzMod.LOGGER.warn("[Obsidian] Player Y=" + playerY + ", expected Y=" + TARGET_Y);
            // Continue anyway, will handle with stair mode if needed
        }
        
        // Use current facing direction
        currentYaw = mc.thePlayer.rotationYaw;
        // Normalize to nearest 90 degrees
        currentYaw = Math.round(currentYaw / 90f) * 90f;
        
        MuzMod.LOGGER.info("[Obsidian] Initial direction: " + currentYaw);
        
        phase = ObsidianPhase.FIND_TARGET;
    }
    
    /**
     * Find target ahead - count obsidian and pick random target
     */
    private void handleFindTarget() {
        debugInfo = "Finding target...";
        setStatus(debugInfo);
        
        // Count obsidian ahead
        int obsidianCount = countObsidianAhead();
        
        if (obsidianCount < 3) {
            MuzMod.LOGGER.info("[Obsidian] Not enough obsidian ahead (" + obsidianCount + "), trying to turn");
            
            // Try turning to find more obsidian
            int leftCount = countObsidianInDirection(currentYaw - 90);
            int rightCount = countObsidianInDirection(currentYaw + 90);
            
            if (leftCount > 3 || rightCount > 3) {
                // Turn to the side with more obsidian
                if (leftCount > rightCount) {
                    currentYaw -= 90;
                } else {
                    currentYaw += 90;
                }
                normalizeYaw();
                mc.thePlayer.rotationYaw = currentYaw;
                MuzMod.LOGGER.info("[Obsidian] Turned to " + currentYaw + " (left: " + leftCount + ", right: " + rightCount + ")");
                return; // Will re-check in next update
            } else {
                MuzMod.LOGGER.info("[Obsidian] No obsidian in any direction, done");
                phase = ObsidianPhase.DONE;
                return;
            }
        }
        
        // Pick random target between (max-10) and max
        int targetDistance = Math.max(3, obsidianCount - random.nextInt(Math.min(10, obsidianCount)));
        
        // Calculate target position
        redTarget = getBlockInDirection(targetDistance);
        
        MuzMod.LOGGER.info("[Obsidian] Red target set at " + redTarget + " (distance: " + targetDistance + ", total ahead: " + obsidianCount + ")");
        
        // Also calculate yellow target (next turn direction preview)
        calculateYellowTarget();
        
        phase = ObsidianPhase.MINING;
    }
    
    /**
     * Calculate yellow target (preview of next target after turning)
     */
    private void calculateYellowTarget() {
        int leftCount = countObsidianInDirection(currentYaw - 90);
        int rightCount = countObsidianInDirection(currentYaw + 90);
        
        float nextYaw = (leftCount > rightCount) ? currentYaw - 90 : currentYaw + 90;
        int nextCount = Math.max(leftCount, rightCount);
        
        if (nextCount > 3) {
            int nextDistance = Math.max(3, nextCount - random.nextInt(Math.min(10, nextCount)));
            yellowTarget = getBlockInDirectionFrom(redTarget, nextYaw, nextDistance);
            MuzMod.LOGGER.info("[Obsidian] Yellow target preview at " + yellowTarget);
        } else {
            yellowTarget = null;
        }
    }
    
    /**
     * Mining towards red target
     */
    private void handleMining(long now) {
        if (redTarget == null) {
            phase = ObsidianPhase.FIND_TARGET;
            return;
        }
        
        // Check if reached red target
        BlockPos playerPos = mc.thePlayer.getPosition();
        double distToTarget = Math.sqrt(
            Math.pow(playerPos.getX() - redTarget.getX(), 2) +
            Math.pow(playerPos.getZ() - redTarget.getZ(), 2)
        );
        
        if (distToTarget < 1.5) {
            MuzMod.LOGGER.info("[Obsidian] Reached red target!");
            InputSimulator.releaseAll();
            
            // Yellow becomes red
            redTarget = yellowTarget;
            yellowTarget = null;
            
            if (redTarget == null) {
                // Need to find new direction
                phase = ObsidianPhase.TURNING;
            } else {
                // Calculate new yellow
                phase = ObsidianPhase.TURNING;
            }
            return;
        }
        
        debugInfo = String.format("Mining... dist: %.1f", distToTarget);
        setStatus(debugInfo);
        
        // Always hold W to move forward
        InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
        
        // Find next block to mine
        BlockPos blockToMine = findBlockToMine();
        
        if (blockToMine != null) {
            currentMiningBlock = blockToMine;
            
            // Smooth aim to block
            smoothLookAt(blockToMine);
            
            // Mine
            if (now - lastMineTime >= MINE_DELAY) {
                InputSimulator.holdKey(mc.gameSettings.keyBindAttack, true);
                lastMineTime = now;
            }
        } else {
            // No block to mine, just walk
            InputSimulator.releaseKey(mc.gameSettings.keyBindAttack);
            currentMiningBlock = null;
            
            // Look forward
            mc.thePlayer.rotationYaw = currentYaw;
            mc.thePlayer.rotationPitch = 0;
        }
        
        // Jump if needed (obstacle or going up)
        if (shouldJump()) {
            InputSimulator.holdKey(mc.gameSettings.keyBindJump, true);
        } else {
            InputSimulator.releaseKey(mc.gameSettings.keyBindJump);
        }
    }
    
    /**
     * Turning to next direction
     */
    private void handleTurning() {
        debugInfo = "Turning...";
        setStatus(debugInfo);
        
        InputSimulator.releaseAll();
        
        // Check sides for obsidian
        int leftCount = countObsidianInDirection(currentYaw - 90);
        int rightCount = countObsidianInDirection(currentYaw + 90);
        
        MuzMod.LOGGER.info("[Obsidian] Turning - left: " + leftCount + ", right: " + rightCount);
        
        if (leftCount < 3 && rightCount < 3) {
            // Check behind
            int behindCount = countObsidianInDirection(currentYaw + 180);
            if (behindCount > 3) {
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
        mc.thePlayer.rotationYaw = currentYaw;
        
        phase = ObsidianPhase.FIND_TARGET;
    }
    
    /**
     * Stair up mode - mine stairs to get back to Y=4
     */
    private void handleStairUp(long now) {
        int playerY = (int) Math.floor(mc.thePlayer.posY);
        
        if (playerY >= TARGET_Y) {
            MuzMod.LOGGER.info("[Obsidian] Back at Y=" + TARGET_Y);
            phase = ObsidianPhase.FIND_TARGET;
            return;
        }
        
        debugInfo = "Stair up... Y=" + playerY;
        setStatus(debugInfo);
        
        // Look up and forward to mine stair pattern
        mc.thePlayer.rotationPitch = -45; // Look up
        
        // Find block above and ahead to mine
        BlockPos ahead = mc.thePlayer.getPosition().offset(getEnumFacing(), 1);
        BlockPos aheadUp = ahead.up();
        
        Block blockAhead = mc.theWorld.getBlockState(ahead).getBlock();
        Block blockAheadUp = mc.theWorld.getBlockState(aheadUp).getBlock();
        
        if (blockAheadUp == Blocks.obsidian || blockAhead == Blocks.obsidian) {
            // Mine the obsidian
            BlockPos toMine = (blockAheadUp == Blocks.obsidian) ? aheadUp : ahead;
            smoothLookAt(toMine);
            InputSimulator.holdKey(mc.gameSettings.keyBindAttack, true);
        } else {
            InputSimulator.releaseKey(mc.gameSettings.keyBindAttack);
        }
        
        // Move forward and jump
        InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
        InputSimulator.holdKey(mc.gameSettings.keyBindJump, true);
    }
    
    /**
     * Count obsidian blocks ahead in current direction
     */
    private int countObsidianAhead() {
        return countObsidianInDirection(currentYaw);
    }
    
    /**
     * Count obsidian blocks in a specific direction
     */
    private int countObsidianInDirection(float yaw) {
        int count = 0;
        BlockPos playerPos = mc.thePlayer.getPosition();
        
        // Get direction vector
        double radians = Math.toRadians(yaw);
        int dx = (int) Math.round(-Math.sin(radians));
        int dz = (int) Math.round(Math.cos(radians));
        
        // Count up to 100 blocks
        for (int i = 1; i <= 100; i++) {
            int x = playerPos.getX() + dx * i;
            int z = playerPos.getZ() + dz * i;
            
            // Check at Y=4 level (obsidian floor)
            BlockPos checkPos = new BlockPos(x, TARGET_Y, z);
            Block block = mc.theWorld.getBlockState(checkPos).getBlock();
            
            if (block == Blocks.obsidian) {
                count++;
            } else if (block != Blocks.air) {
                // Hit something that's not obsidian or air, stop counting
                break;
            }
        }
        
        return count;
    }
    
    /**
     * Get block position at distance in current direction
     */
    private BlockPos getBlockInDirection(int distance) {
        return getBlockInDirectionFrom(mc.thePlayer.getPosition(), currentYaw, distance);
    }
    
    /**
     * Get block position at distance in direction from a position
     */
    private BlockPos getBlockInDirectionFrom(BlockPos from, float yaw, int distance) {
        double radians = Math.toRadians(yaw);
        int dx = (int) Math.round(-Math.sin(radians));
        int dz = (int) Math.round(Math.cos(radians));
        
        return new BlockPos(
            from.getX() + dx * distance,
            TARGET_Y,
            from.getZ() + dz * distance
        );
    }
    
    /**
     * Find the next obsidian block to mine (in front of player)
     */
    private BlockPos findBlockToMine() {
        BlockPos playerPos = mc.thePlayer.getPosition();
        
        // Check blocks in front at various heights
        EnumFacing facing = getEnumFacing();
        
        for (int forward = 1; forward <= 2; forward++) {
            for (int y = 0; y <= 2; y++) {
                BlockPos checkPos = playerPos.offset(facing, forward).up(y);
                Block block = mc.theWorld.getBlockState(checkPos).getBlock();
                
                if (block == Blocks.obsidian) {
                    return checkPos;
                }
            }
        }
        
        // Check directly in front at player level
        BlockPos front = playerPos.offset(facing, 1);
        if (mc.theWorld.getBlockState(front).getBlock() == Blocks.obsidian) {
            return front;
        }
        
        // Check below in front (for walking on obsidian)
        BlockPos frontDown = front.down();
        if (mc.theWorld.getBlockState(frontDown).getBlock() == Blocks.obsidian) {
            return frontDown;
        }
        
        return null;
    }
    
    /**
     * Get EnumFacing from current yaw
     */
    private EnumFacing getEnumFacing() {
        float yaw = currentYaw;
        while (yaw < 0) yaw += 360;
        while (yaw >= 360) yaw -= 360;
        
        if (yaw >= 315 || yaw < 45) return EnumFacing.SOUTH;
        if (yaw >= 45 && yaw < 135) return EnumFacing.WEST;
        if (yaw >= 135 && yaw < 225) return EnumFacing.NORTH;
        return EnumFacing.EAST;
    }
    
    /**
     * Smooth look at a block position
     */
    private void smoothLookAt(BlockPos pos) {
        double dx = pos.getX() + 0.5 - mc.thePlayer.posX;
        double dy = pos.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = pos.getZ() + 0.5 - mc.thePlayer.posZ;
        
        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        
        // Smooth interpolation
        float yawDiff = targetYaw - mc.thePlayer.rotationYaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        float pitchDiff = targetPitch - mc.thePlayer.rotationPitch;
        
        // Faster smoothing for mining
        float smoothFactor = 0.4f;
        mc.thePlayer.rotationYaw += yawDiff * smoothFactor;
        mc.thePlayer.rotationPitch += pitchDiff * smoothFactor;
    }
    
    /**
     * Check if player should jump
     */
    private boolean shouldJump() {
        BlockPos playerPos = mc.thePlayer.getPosition();
        EnumFacing facing = getEnumFacing();
        BlockPos ahead = playerPos.offset(facing, 1);
        
        // Jump if there's a block at feet level
        Block blockAhead = mc.theWorld.getBlockState(ahead).getBlock();
        if (blockAhead != Blocks.air && blockAhead != Blocks.obsidian) {
            return true;
        }
        
        // Jump if there's obsidian at feet level (need to climb over)
        if (blockAhead == Blocks.obsidian) {
            return true;
        }
        
        return false;
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
            renderTargetBlock(redTarget, 1.0f, 0.0f, 0.0f, 0.5f); // Red
        }
        
        // Render yellow target
        if (yellowTarget != null) {
            renderTargetBlock(yellowTarget, 1.0f, 1.0f, 0.0f, 0.4f); // Yellow
        }
        
        // Render current mining block
        if (currentMiningBlock != null) {
            renderTargetBlock(currentMiningBlock, 0.0f, 1.0f, 0.0f, 0.3f); // Green
        }
    }
    
    /**
     * Render a highlighted block
     */
    private void renderTargetBlock(BlockPos pos, float r, float g, float b, float alpha) {
        double x = pos.getX() - mc.getRenderManager().viewerPosX;
        double y = pos.getY() - mc.getRenderManager().viewerPosY;
        double z = pos.getZ() - mc.getRenderManager().viewerPosZ;
        
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        
        // Draw filled box
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        
        // Bottom face
        worldRenderer.pos(x, y, z).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x + 1, y, z).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x + 1, y, z + 1).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x, y, z + 1).color(r, g, b, alpha).endVertex();
        
        // Top face
        worldRenderer.pos(x, y + 1, z).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x, y + 1, z + 1).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x + 1, y + 1, z + 1).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x + 1, y + 1, z).color(r, g, b, alpha).endVertex();
        
        // North face
        worldRenderer.pos(x, y, z).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x, y + 1, z).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x + 1, y + 1, z).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x + 1, y, z).color(r, g, b, alpha).endVertex();
        
        // South face
        worldRenderer.pos(x, y, z + 1).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x + 1, y, z + 1).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x + 1, y + 1, z + 1).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x, y + 1, z + 1).color(r, g, b, alpha).endVertex();
        
        // West face
        worldRenderer.pos(x, y, z).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x, y, z + 1).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x, y + 1, z + 1).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x, y + 1, z).color(r, g, b, alpha).endVertex();
        
        // East face
        worldRenderer.pos(x + 1, y, z).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x + 1, y + 1, z).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x + 1, y + 1, z + 1).color(r, g, b, alpha).endVertex();
        worldRenderer.pos(x + 1, y, z + 1).color(r, g, b, alpha).endVertex();
        
        tessellator.draw();
        
        // Draw outline
        GlStateManager.disableBlend();
        GL11.glLineWidth(2.0f);
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        
        worldRenderer.pos(x, y, z).color(r, g, b, 1.0f).endVertex();
        worldRenderer.pos(x + 1, y, z).color(r, g, b, 1.0f).endVertex();
        worldRenderer.pos(x + 1, y, z + 1).color(r, g, b, 1.0f).endVertex();
        worldRenderer.pos(x, y, z + 1).color(r, g, b, 1.0f).endVertex();
        worldRenderer.pos(x, y, z).color(r, g, b, 1.0f).endVertex();
        
        worldRenderer.pos(x, y + 1, z).color(r, g, b, 1.0f).endVertex();
        worldRenderer.pos(x + 1, y + 1, z).color(r, g, b, 1.0f).endVertex();
        worldRenderer.pos(x + 1, y + 1, z + 1).color(r, g, b, 1.0f).endVertex();
        worldRenderer.pos(x, y + 1, z + 1).color(r, g, b, 1.0f).endVertex();
        worldRenderer.pos(x, y + 1, z).color(r, g, b, 1.0f).endVertex();
        
        tessellator.draw();
        
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
    
    // ==================== INTERFACE METHODS ====================
    
    @Override
    public String getName() {
        return "Obsidian";
    }
    
    @Override
    public boolean shouldActivate() {
        // Obsidian mode is manually activated, no time-based activation
        return false;
    }
    
    @Override
    public int getPriority() {
        // Medium priority
        return 5;
    }
}
