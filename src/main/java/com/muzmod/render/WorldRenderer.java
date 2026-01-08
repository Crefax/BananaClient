package com.muzmod.render;

import com.muzmod.MuzMod;
import com.muzmod.navigation.NavigationManager;
import com.muzmod.state.impl.MiningState;
import com.muzmod.util.PlayerDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

import java.util.Set;

/**
 * Renders in-world elements (ore highlights, player detection circle, navigation path)
 */
public class WorldRenderer {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    
    public void render(float partialTicks) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MuzMod.instance.isBotEnabled()) return;
        
        // HUD kapalıysa world render da yapma
        if (!MuzMod.instance.getConfig().isShowOverlay()) return;
        
        // Get render position
        double renderX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double renderY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks;
        double renderZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;
        
        // Render player detection radius
        renderDetectionRadius(renderX, renderY, renderZ, partialTicks);
        
        // Render navigation path
        NavigationManager.getInstance().getRenderer().render(partialTicks);
        
        // Render ore highlights if mining
        if (MuzMod.instance.getStateManager().getCurrentState() instanceof MiningState) {
            MiningState miningState = (MiningState) MuzMod.instance.getStateManager().getCurrentState();
            renderOreHighlights(miningState, renderX, renderY, renderZ);
        }
    }
    
    private void renderDetectionRadius(double renderX, double renderY, double renderZ, float partialTicks) {
        double radius = MuzMod.instance.getConfig().getPlayerDetectionRadius();
        
        // Check if players are nearby for color
        boolean playerNearby = PlayerDetector.isPlayerNearby(mc.thePlayer, radius);
        
        // Setup GL state
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GL11.glLineWidth(2.0f);
        
        Tessellator tessellator = Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        
        // Translate to player position (at feet level)
        GlStateManager.translate(-renderX, -renderY, -renderZ);
        GlStateManager.translate(mc.thePlayer.posX, mc.thePlayer.posY + 0.01, mc.thePlayer.posZ);
        
        // Set color based on nearby players
        float r, g, b, a;
        if (playerNearby) {
            r = 1.0f; g = 0.2f; b = 0.2f; a = 0.6f; // Red
        } else {
            r = 0.2f; g = 1.0f; b = 0.2f; a = 0.4f; // Green
        }
        
        // Draw circle
        worldRenderer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        int segments = 64;
        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            worldRenderer.pos(x, 0, z).color(r, g, b, a).endVertex();
        }
        tessellator.draw();
        
        // Draw filled circle (semi-transparent)
        worldRenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(0, 0, 0).color(r, g, b, a * 0.2f).endVertex();
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            worldRenderer.pos(x, 0, z).color(r, g, b, a * 0.2f).endVertex();
        }
        tessellator.draw();
        
        // Restore GL state
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
    
    private void renderOreHighlights(MiningState miningState, double renderX, double renderY, double renderZ) {
        Set<BlockPos> markedOres = miningState.getMarkedOres();
        BlockPos currentTarget = miningState.getCurrentTarget();
        
        if (markedOres.isEmpty()) return;
        
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0f);
        
        for (BlockPos pos : markedOres) {
            boolean isTarget = pos.equals(currentTarget);
            renderBlockHighlight(pos, renderX, renderY, renderZ, isTarget);
        }
        
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
    
    private void renderBlockHighlight(BlockPos pos, double renderX, double renderY, double renderZ, boolean isTarget) {
        double x = pos.getX() - renderX;
        double y = pos.getY() - renderY;
        double z = pos.getZ() - renderZ;
        
        float r, g, b, a;
        if (isTarget) {
            r = 1.0f; g = 1.0f; b = 0.0f; a = 0.8f; // Yellow for current target
        } else {
            r = 0.0f; g = 1.0f; b = 1.0f; a = 0.5f; // Cyan for other ores
        }
        
        Tessellator tessellator = Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        
        double expand = isTarget ? 0.005 : 0.002;
        double minX = x - expand;
        double minY = y - expand;
        double minZ = z - expand;
        double maxX = x + 1 + expand;
        double maxY = y + 1 + expand;
        double maxZ = z + 1 + expand;
        
        // Draw box outline
        worldRenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        
        // Bottom face
        worldRenderer.pos(minX, minY, minZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(maxX, minY, minZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(maxX, minY, minZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(minX, minY, maxZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(minX, minY, maxZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(minX, minY, minZ).color(r, g, b, a).endVertex();
        
        // Top face
        worldRenderer.pos(minX, maxY, minZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(minX, maxY, minZ).color(r, g, b, a).endVertex();
        
        // Vertical edges
        worldRenderer.pos(minX, minY, minZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(minX, maxY, minZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(maxX, minY, minZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(minX, minY, maxZ).color(r, g, b, a).endVertex();
        worldRenderer.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();
        
        tessellator.draw();
        
        // Draw filled faces for target block
        if (isTarget) {
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            float fillAlpha = 0.15f;
            
            // Top face
            worldRenderer.pos(minX, maxY, minZ).color(r, g, b, fillAlpha).endVertex();
            worldRenderer.pos(maxX, maxY, minZ).color(r, g, b, fillAlpha).endVertex();
            worldRenderer.pos(maxX, maxY, maxZ).color(r, g, b, fillAlpha).endVertex();
            worldRenderer.pos(minX, maxY, maxZ).color(r, g, b, fillAlpha).endVertex();
            
            // Bottom face
            worldRenderer.pos(minX, minY, minZ).color(r, g, b, fillAlpha).endVertex();
            worldRenderer.pos(minX, minY, maxZ).color(r, g, b, fillAlpha).endVertex();
            worldRenderer.pos(maxX, minY, maxZ).color(r, g, b, fillAlpha).endVertex();
            worldRenderer.pos(maxX, minY, minZ).color(r, g, b, fillAlpha).endVertex();
            
            tessellator.draw();
        }
    }
}
