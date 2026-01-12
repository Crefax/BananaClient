package com.muzmod.state.impl;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
import com.muzmod.state.AbstractState;
import com.muzmod.util.AlertSystem;
import com.muzmod.util.InputSimulator;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Random;

/**
 * Obsidian Mining State v0.7.10
 * 
 * Makro tabanlı obsidyen kazma sistemi.
 * 
 * Çalışma mantığı:
 * - Sabit yaw/pitch pozisyonunda kalır (jitter ile)
 * - Aim kayarsa otomatik düzeltir
 * - Döndükçe +90 derece ile yeni pozisyon
 * - Oyuncu önüne gelirse veya yanlış blok kazılırsa DURDUR
 */
public class ObsidianState extends AbstractState {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();
    
    // Kilitli pozisyon
    private float lockedYaw = 0;
    private float lockedPitch = 55;
    
    // Yön (4 yön: 0=Güney, 1=Batı, 2=Kuzey, 3=Doğu)
    private int direction = 0;
    private static final float[] YAWS = {0, 90, 180, -90}; // Güney, Batı, Kuzey, Doğu
    
    // Phase
    private Phase phase = Phase.INIT;
    
    // Jitter
    private long lastJitterTime = 0;
    private float jitterYaw = 0;
    private float jitterPitch = 0;
    
    // Durum
    private String debugInfo = "";
    private long miningStartTime = 0;
    private BlockPos currentMiningBlock = null;
    
    // Dönüş
    private boolean isTurning = false;
    private float turnTargetYaw = 0;
    
    // Envanter kontrolü
    private long lastInventoryCheck = 0;
    private static final long INVENTORY_CHECK_INTERVAL = 2000;
    private boolean waitingForCevir = false;
    private long cevirCommandTime = 0;
    
    // Focus kontrolü
    private boolean hadFocus = true;
    private long focusRegainTime = 0;
    private static final long FOCUS_GRACE_PERIOD = 500;
    
    // Block check
    private long lastBlockCheck = 0;
    private static final long BLOCK_CHECK_INTERVAL = 200; // 200ms'de bir kontrol
    
    private enum Phase {
        INIT,
        MINING,
        TURNING,
        STOPPED
    }
    
    public ObsidianState() {
        this.status = "Obsidian ready";
    }
    
    @Override
    public String getName() {
        return "obsidian";
    }
    
    @Override
    public int getPriority() {
        return 5; // Normal priority
    }
    
    @Override
    public boolean shouldActivate() {
        return false; // Manuel aktivasyon
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        phase = Phase.INIT;
        isTurning = false;
        currentMiningBlock = null;
        miningStartTime = 0;
        waitingForCevir = false;
        
        // Başlangıç pozisyonunu kaydet
        if (mc.thePlayer != null) {
            lockedYaw = mc.thePlayer.rotationYaw;
            lockedPitch = mc.thePlayer.rotationPitch;
            
            // Yönü belirle (en yakın 90'ın katı)
            direction = getDirectionFromYaw(lockedYaw);
            lockedYaw = YAWS[direction];
            
            MuzMod.LOGGER.info("[Obsidian] Locked position - Yaw: " + lockedYaw + ", Pitch: " + lockedPitch + ", Direction: " + direction);
        }
        
        MinecraftForge.EVENT_BUS.register(this);
        MuzMod.LOGGER.info("[Obsidian] v0.7.10 enabled - Macro mode");
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        InputSimulator.releaseAll();
        MinecraftForge.EVENT_BUS.unregister(this);
        currentMiningBlock = null;
        MuzMod.LOGGER.info("[Obsidian] disabled");
    }
    
    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        ModConfig config = MuzMod.instance.getConfig();
        
        // Phase STOPPED ise hiçbir şey yapma
        if (phase == Phase.STOPPED) {
            setStatus("§cDURDURULDU");
            return;
        }
        
        // Kazma durability kontrolü
        if (RepairState.needsRepair(config)) {
            InputSimulator.releaseAll();
            MuzMod.LOGGER.info("[Obsidian] Kazma durability düşük, tamire gönderiliyor");
            MuzMod.instance.getStateManager().forceState("repair");
            return;
        }
        
        // Sell komutu bekleniyorsa
        if (waitingForCevir) {
            long elapsed = System.currentTimeMillis() - cevirCommandTime;
            int sellDelay = config.getObsidianSellDelay();
            if (elapsed >= sellDelay) {
                waitingForCevir = false;
                MuzMod.LOGGER.info("[Obsidian] Sell komutu bekleme bitti, devam ediliyor");
            } else {
                setStatus("Bekleniyor... " + ((sellDelay - elapsed) / 1000) + "s");
                return;
            }
        }
        
        // Envanter kontrolü
        if (phase == Phase.MINING && config.isObsidianSellEnabled()) {
            long now = System.currentTimeMillis();
            if (now - lastInventoryCheck >= INVENTORY_CHECK_INTERVAL) {
                lastInventoryCheck = now;
                if (isInventoryFull()) {
                    String sellCommand = config.getObsidianSellCommand();
                    MuzMod.LOGGER.info("[Obsidian] Envanter dolu! " + sellCommand + " yazılıyor...");
                    InputSimulator.releaseAll();
                    mc.thePlayer.sendChatMessage(sellCommand);
                    waitingForCevir = true;
                    cevirCommandTime = now;
                    setStatus(sellCommand + " gönderildi...");
                    return;
                }
            }
        }
        
        // Blok ve oyuncu kontrolü
        if (phase == Phase.MINING && System.currentTimeMillis() - lastBlockCheck >= BLOCK_CHECK_INTERVAL) {
            lastBlockCheck = System.currentTimeMillis();
            
            // Önüne oyuncu geldi mi?
            EntityPlayer blockingPlayer = getBlockingPlayer();
            if (blockingPlayer != null) {
                stopWithAlert("player", blockingPlayer.getName());
                return;
            }
            
            // Baktığı blok obsidyen mi?
            MovingObjectPosition mop = mc.objectMouseOver;
            if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                BlockPos pos = mop.getBlockPos();
                Block block = mc.theWorld.getBlockState(pos).getBlock();
                
                if (block != Blocks.obsidian && block != Blocks.air) {
                    String blockName = block.getLocalizedName();
                    stopWithAlert("block", blockName);
                    return;
                }
            }
        }
        
        // Aim düzeltme ve jitter
        handleAim(config);
        
        switch (phase) {
            case INIT:
                doInit();
                break;
            case MINING:
                doMining();
                break;
            case TURNING:
                doTurning();
                break;
            case STOPPED:
                // Hiçbir şey yapma
                break;
        }
    }
    
    /**
     * Uyarı ile durdur
     */
    private void stopWithAlert(String type, String name) {
        InputSimulator.releaseAll();
        phase = Phase.STOPPED;
        
        String playerName = mc.thePlayer.getName();
        
        if (type.equals("player")) {
            AlertSystem.alertPlayerBlocking(playerName, name);
            setStatus("§c" + name + " önünüze geçti!");
        } else {
            AlertSystem.alertWrongBlock(playerName, name);
            setStatus("§c" + name + " bloğu!");
        }
        
        // Bot'u durdur
        MuzMod.instance.setBotEnabled(false);
        MuzMod.LOGGER.warn("[Obsidian] STOPPED - " + type + ": " + name);
    }
    
    /**
     * Önüne oyuncu var mı kontrol et
     */
    private EntityPlayer getBlockingPlayer() {
        if (mc.thePlayer == null) return null;
        
        // Oyuncunun baktığı yönde 3 blok içinde oyuncu ara
        double range = 3.0;
        List<EntityPlayer> players = mc.theWorld.playerEntities;
        
        for (EntityPlayer player : players) {
            if (player == mc.thePlayer) continue;
            
            double dx = player.posX - mc.thePlayer.posX;
            double dy = player.posY - mc.thePlayer.posY;
            double dz = player.posZ - mc.thePlayer.posZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            
            if (dist <= range && Math.abs(dy) <= 2) {
                // Baktığımız yönde mi?
                double yaw = Math.toRadians(lockedYaw);
                double lookX = -Math.sin(yaw);
                double lookZ = Math.cos(yaw);
                
                // Dot product - aynı yönde mi?
                double dot = (dx * lookX + dz * lookZ) / (dist + 0.001);
                if (dot > 0.5) { // Önümüzde
                    return player;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Aim kontrolü - Kilitlenen pozisyona geri dön + Jitter
     */
    private void handleAim(ModConfig config) {
        // Focus kontrolü
        boolean currentFocus = mc.inGameHasFocus;
        
        if (!currentFocus) {
            hadFocus = false;
            return;
        }
        
        if (!hadFocus && currentFocus) {
            hadFocus = true;
            focusRegainTime = System.currentTimeMillis();
            return;
        }
        
        if (System.currentTimeMillis() - focusRegainTime < FOCUS_GRACE_PERIOD) {
            return;
        }
        
        // Dönüş modunda özel işlem
        if (isTurning) {
            float yawDiff = turnTargetYaw - mc.thePlayer.rotationYaw;
            while (yawDiff > 180) yawDiff -= 360;
            while (yawDiff < -180) yawDiff += 360;
            
            float turnSpeed = config.getObsidianTurnSpeed();
            if (Math.abs(yawDiff) > 1.0f) {
                mc.thePlayer.rotationYaw += yawDiff * turnSpeed;
            } else {
                mc.thePlayer.rotationYaw = turnTargetYaw;
                isTurning = false;
                lockedYaw = turnTargetYaw;
                phase = Phase.MINING;
                MuzMod.LOGGER.info("[Obsidian] Turn complete - New locked yaw: " + lockedYaw);
            }
            return;
        }
        
        // Jitter uygula
        long now = System.currentTimeMillis();
        int jitterInterval = config.getObsidianJitterInterval();
        
        if (now - lastJitterTime >= jitterInterval) {
            lastJitterTime = now;
            float jitterYawRange = config.getObsidianJitterYaw();
            float jitterPitchRange = config.getObsidianJitterPitch();
            
            jitterYaw = (random.nextFloat() - 0.5f) * 2 * jitterYawRange;
            jitterPitch = (random.nextFloat() - 0.5f) * 2 * jitterPitchRange;
        }
        
        // Hedef pozisyon (kilitli + jitter)
        float targetYaw = lockedYaw + jitterYaw;
        float targetPitch = lockedPitch + jitterPitch;
        
        // Pitch sınırları
        targetPitch = Math.max(-90, Math.min(90, targetPitch));
        
        // Yumuşak geçiş ile pozisyona git
        float aimSpeed = config.getObsidianAimSpeed();
        
        float yawDiff = targetYaw - mc.thePlayer.rotationYaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        float pitchDiff = targetPitch - mc.thePlayer.rotationPitch;
        
        if (Math.abs(yawDiff) > 0.1f) {
            mc.thePlayer.rotationYaw += yawDiff * aimSpeed;
        }
        
        if (Math.abs(pitchDiff) > 0.1f) {
            mc.thePlayer.rotationPitch += pitchDiff * aimSpeed;
        }
    }
    
    private void doInit() {
        setStatus("Başlatılıyor...");
        phase = Phase.MINING;
        miningStartTime = System.currentTimeMillis();
        MuzMod.LOGGER.info("[Obsidian] Init complete, starting mining");
    }
    
    private void doMining() {
        setStatus("Kazıyor... [Yaw: " + String.format("%.1f", lockedYaw) + "]");
        
        // Sürekli sol tık basılı tut
        if (!InputSimulator.isLeftClickHeld()) {
            InputSimulator.holdLeftClick(true);
        }
        
        // Baktığımız bloğu güncelle
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            currentMiningBlock = mop.getBlockPos();
        }
    }
    
    /**
     * 90 derece dönüş başlat
     */
    public void turn90() {
        if (phase == Phase.STOPPED) return;
        
        InputSimulator.releaseAll();
        
        // Sonraki yön
        direction = (direction + 1) % 4;
        turnTargetYaw = YAWS[direction];
        
        // Yaw farkını normalize et
        float diff = turnTargetYaw - mc.thePlayer.rotationYaw;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        turnTargetYaw = mc.thePlayer.rotationYaw + diff;
        
        isTurning = true;
        phase = Phase.TURNING;
        
        MuzMod.LOGGER.info("[Obsidian] Turning to direction " + direction + " (yaw: " + turnTargetYaw + ")");
    }
    
    private void doTurning() {
        setStatus("Dönüyor... [Hedef: " + String.format("%.1f", turnTargetYaw) + "]");
        // Aim handler'da işleniyor
    }
    
    /**
     * Yaw'dan yön belirle
     */
    private int getDirectionFromYaw(float yaw) {
        // Yaw'u normalize et
        while (yaw < 0) yaw += 360;
        while (yaw >= 360) yaw -= 360;
        
        // En yakın 90'ın katına yuvarla
        if (yaw >= 315 || yaw < 45) return 0;   // Güney (0)
        if (yaw >= 45 && yaw < 135) return 1;   // Batı (90)
        if (yaw >= 135 && yaw < 225) return 2;  // Kuzey (180)
        return 3; // Doğu (-90/270)
    }
    
    /**
     * Envanter dolu mu?
     */
    private boolean isInventoryFull() {
        for (int i = 0; i < 36; i++) {
            if (mc.thePlayer.inventory.getStackInSlot(i) == null) {
                return false;
            }
        }
        return true;
    }
    
    // ===================== RENDERING =====================
    
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isEnabled()) return;
        
        float partialTicks = event.partialTicks;
        
        // Baktığımız bloğu yeşil göster
        if (currentMiningBlock != null) {
            Block block = mc.theWorld.getBlockState(currentMiningBlock).getBlock();
            if (block == Blocks.obsidian) {
                renderBlockOutline(currentMiningBlock, 0, 1, 0, 0.8f, partialTicks); // Yeşil
            } else if (block != Blocks.air) {
                renderBlockOutline(currentMiningBlock, 1, 0, 0, 0.8f, partialTicks); // Kırmızı (yanlış blok)
            }
        }
    }
    
    private void renderBlockOutline(BlockPos pos, float r, float g, float b, float a, float partialTicks) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        
        // Kamera pozisyonunu partialTicks ile hesapla
        double camX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double camY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks;
        double camZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;
        
        AxisAlignedBB box = new AxisAlignedBB(
                x - camX, y - camY, z - camZ,
                x + 1 - camX, y + 1 - camY, z + 1 - camZ
        );
        
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0f);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        
        // Alt yüzey
        wr.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        
        tessellator.draw();
        
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        
        // Üst yüzey
        wr.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        
        tessellator.draw();
        
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        
        // Dikey çizgiler
        wr.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        
        wr.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        
        wr.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        
        wr.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        
        tessellator.draw();
        
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
    
    @Override
    public String getStatus() {
        if (phase == Phase.STOPPED) {
            return "§c" + status;
        }
        return status;
    }
}
