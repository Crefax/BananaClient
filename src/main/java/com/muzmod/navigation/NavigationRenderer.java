package com.muzmod.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Navigation Renderer
 * 
 * Yol ve hedef görselleştirme sistemi.
 * - Hedef bloğu kırmızı kutu olarak gösterir
 * - Yol çizgisi çizer
 * - Waypoint'leri gösterir
 */
public class NavigationRenderer {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    
    // Render verileri
    private List<BlockPos> path = null;
    private BlockPos target = null;
    private int currentWaypointIndex = 0;
    
    // Renkler (RGBA)
    private static final float[] COLOR_TARGET = {1.0f, 0.2f, 0.2f, 0.8f};      // Kırmızı - hedef
    private static final float[] COLOR_PATH = {0.2f, 1.0f, 0.4f, 0.6f};        // Yeşil - yol çizgisi
    private static final float[] COLOR_WAYPOINT = {1.0f, 1.0f, 0.2f, 0.5f};    // Sarı - waypoint
    private static final float[] COLOR_CURRENT = {0.2f, 0.6f, 1.0f, 0.8f};     // Mavi - mevcut waypoint
    
    // Ayarlar
    private boolean renderEnabled = true;
    private float lineWidth = 3.0f;
    
    public NavigationRenderer() {}
    
    /**
     * Yolu ayarla
     */
    public void setPath(List<BlockPos> path) {
        this.path = path;
        this.currentWaypointIndex = 0;
    }
    
    /**
     * Hedefi ayarla
     */
    public void setTarget(BlockPos target) {
        this.target = target;
    }
    
    /**
     * Mevcut waypoint indeksini ayarla
     */
    public void setCurrentWaypointIndex(int index) {
        this.currentWaypointIndex = index;
    }
    
    /**
     * Temizle
     */
    public void clear() {
        this.path = null;
        this.target = null;
        this.currentWaypointIndex = 0;
    }
    
    /**
     * Güncelle (her tick)
     */
    public void update() {
        // Şu an için boş - animasyonlar eklenebilir
    }
    
    /**
     * Render (RenderWorldLastEvent'te çağrılmalı)
     */
    public void render(float partialTicks) {
        if (!renderEnabled) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        EntityPlayer player = mc.thePlayer;
        
        // Kamera pozisyonunu al
        double renderX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double renderY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double renderZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        
        // OpenGL ayarları
        GlStateManager.pushMatrix();
        GlStateManager.translate(-renderX, -renderY, -renderZ);
        
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableDepth();
        GL11.glLineWidth(lineWidth);
        
        // Yolu çiz
        if (path != null && !path.isEmpty()) {
            renderPath(player);
        }
        
        // Hedefi çiz
        if (target != null) {
            renderTargetBox(target);
        }
        
        // OpenGL ayarlarını geri al
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
    
    /**
     * Yol çizgisi çiz
     */
    private void renderPath(EntityPlayer player) {
        if (path == null || path.size() < 2) return;
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        
        // Yol çizgisi
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        
        // Oyuncudan başla
        wr.pos(player.posX, player.posY + 0.5, player.posZ)
          .color(COLOR_PATH[0], COLOR_PATH[1], COLOR_PATH[2], COLOR_PATH[3])
          .endVertex();
        
        for (int i = currentWaypointIndex; i < path.size(); i++) {
            BlockPos pos = path.get(i);
            float alpha = (i == path.size() - 1) ? 1.0f : 0.6f; // Son nokta daha parlak
            
            wr.pos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
              .color(COLOR_PATH[0], COLOR_PATH[1], COLOR_PATH[2], alpha)
              .endVertex();
        }
        
        tessellator.draw();
        
        // Waypoint kutuları
        for (int i = 0; i < path.size(); i++) {
            BlockPos pos = path.get(i);
            
            if (i == currentWaypointIndex) {
                // Mevcut waypoint - mavi
                renderSmallBox(pos, COLOR_CURRENT, 0.3);
            } else if (i > currentWaypointIndex) {
                // Gelecek waypoint - sarı
                renderSmallBox(pos, COLOR_WAYPOINT, 0.2);
            }
            // Geçmiş waypoint'ler çizilmez
        }
    }
    
    /**
     * Hedef kutusu çiz (kırmızı)
     */
    private void renderTargetBox(BlockPos pos) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        
        float r = COLOR_TARGET[0];
        float g = COLOR_TARGET[1];
        float b = COLOR_TARGET[2];
        float a = COLOR_TARGET[3];
        
        // Dış çerçeve (wireframe)
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        
        // Alt yüz
        wr.pos(x, y, z).color(r, g, b, a).endVertex();
        wr.pos(x + 1, y, z).color(r, g, b, a).endVertex();
        wr.pos(x + 1, y, z + 1).color(r, g, b, a).endVertex();
        wr.pos(x, y, z + 1).color(r, g, b, a).endVertex();
        wr.pos(x, y, z).color(r, g, b, a).endVertex();
        
        tessellator.draw();
        
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        
        // Üst yüz
        wr.pos(x, y + 1, z).color(r, g, b, a).endVertex();
        wr.pos(x + 1, y + 1, z).color(r, g, b, a).endVertex();
        wr.pos(x + 1, y + 1, z + 1).color(r, g, b, a).endVertex();
        wr.pos(x, y + 1, z + 1).color(r, g, b, a).endVertex();
        wr.pos(x, y + 1, z).color(r, g, b, a).endVertex();
        
        tessellator.draw();
        
        // Dikey çizgiler
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        
        wr.pos(x, y, z).color(r, g, b, a).endVertex();
        wr.pos(x, y + 1, z).color(r, g, b, a).endVertex();
        
        wr.pos(x + 1, y, z).color(r, g, b, a).endVertex();
        wr.pos(x + 1, y + 1, z).color(r, g, b, a).endVertex();
        
        wr.pos(x + 1, y, z + 1).color(r, g, b, a).endVertex();
        wr.pos(x + 1, y + 1, z + 1).color(r, g, b, a).endVertex();
        
        wr.pos(x, y, z + 1).color(r, g, b, a).endVertex();
        wr.pos(x, y + 1, z + 1).color(r, g, b, a).endVertex();
        
        tessellator.draw();
        
        // İç dolgu (yarı saydam)
        GlStateManager.depthMask(false);
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        
        float fillAlpha = 0.3f;
        
        // Alt yüz
        wr.pos(x, y, z).color(r, g, b, fillAlpha).endVertex();
        wr.pos(x + 1, y, z).color(r, g, b, fillAlpha).endVertex();
        wr.pos(x + 1, y, z + 1).color(r, g, b, fillAlpha).endVertex();
        wr.pos(x, y, z + 1).color(r, g, b, fillAlpha).endVertex();
        
        // Üst yüz
        wr.pos(x, y + 1, z).color(r, g, b, fillAlpha).endVertex();
        wr.pos(x, y + 1, z + 1).color(r, g, b, fillAlpha).endVertex();
        wr.pos(x + 1, y + 1, z + 1).color(r, g, b, fillAlpha).endVertex();
        wr.pos(x + 1, y + 1, z).color(r, g, b, fillAlpha).endVertex();
        
        tessellator.draw();
        GlStateManager.depthMask(true);
    }
    
    /**
     * Küçük kutu çiz (waypoint)
     */
    private void renderSmallBox(BlockPos pos, float[] color, double size) {
        double offset = (1.0 - size) / 2.0;
        double x = pos.getX() + offset;
        double y = pos.getY() + offset;
        double z = pos.getZ() + offset;
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        
        float r = color[0];
        float g = color[1];
        float b = color[2];
        float a = color[3];
        
        // Wireframe
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        
        wr.pos(x, y, z).color(r, g, b, a).endVertex();
        wr.pos(x + size, y, z).color(r, g, b, a).endVertex();
        wr.pos(x + size, y, z + size).color(r, g, b, a).endVertex();
        wr.pos(x, y, z + size).color(r, g, b, a).endVertex();
        wr.pos(x, y, z).color(r, g, b, a).endVertex();
        
        wr.pos(x, y + size, z).color(r, g, b, a).endVertex();
        wr.pos(x + size, y + size, z).color(r, g, b, a).endVertex();
        wr.pos(x + size, y + size, z + size).color(r, g, b, a).endVertex();
        wr.pos(x, y + size, z + size).color(r, g, b, a).endVertex();
        wr.pos(x, y + size, z).color(r, g, b, a).endVertex();
        
        tessellator.draw();
    }
    
    // ==================== GETTERS/SETTERS ====================
    
    public boolean isRenderEnabled() {
        return renderEnabled;
    }
    
    public void setRenderEnabled(boolean enabled) {
        this.renderEnabled = enabled;
    }
    
    public float getLineWidth() {
        return lineWidth;
    }
    
    public void setLineWidth(float width) {
        this.lineWidth = width;
    }
    
    public boolean hasPath() {
        return path != null && !path.isEmpty();
    }
    
    public boolean hasTarget() {
        return target != null;
    }
}
