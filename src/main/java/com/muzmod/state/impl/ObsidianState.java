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
 * Obsidian Mining State v0.7.11
 * 
 * Makro tabanlı obsidyen kazma sistemi + Otomatik yön değiştirme.
 * 
 * Çalışma mantığı:
 * - Sabit yaw/pitch pozisyonunda kalır (jitter ile)
 * - Obsidyen sayımı yaparak hedef mesafe belirler
 * - Hedefe ulaşınca otomatik döner (sağ/sol obsidyen sayısına göre)
 * - Oyuncu önüne gelirse veya yanlış blok kazılırsa DURDUR
 */
public class ObsidianState extends AbstractState {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();
    
    // Başlangıç Y pozisyonu
    private int startY = -1;
    
    // Kilitli pozisyon
    private float lockedYaw = 0;
    private float lockedPitch = 55;
    
    // Yön (4 yön: 0=Güney, 1=Batı, 2=Kuzey, 3=Doğu)
    private int direction = 0;
    private static final int[] DX = {0, -1, 0, 1};  // Güney, Batı, Kuzey, Doğu
    private static final int[] DZ = {1, 0, -1, 0};
    private static final float[] YAWS = {0, 90, 180, -90};
    
    // Phase
    private Phase phase = Phase.INIT;
    
    // Hedefler
    private BlockPos redTarget = null;    // Mevcut hedef (KIRMIZI)
    private BlockPos yellowTarget = null; // Sonraki hedef (SARI)
    private BlockPos currentMiningBlock = null;
    
    // Jitter
    private long lastJitterTime = 0;
    private float jitterYaw = 0;
    private float jitterPitch = 0;
    
    // Durum
    private String debugInfo = "";
    private long miningStartTime = 0;
    
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
    private static final long BLOCK_CHECK_INTERVAL = 200;
    
    // Hedef kontrolü
    private long lastTargetCheck = 0;
    private static final long TARGET_CHECK_INTERVAL = 500; // 500ms'de bir hedef kontrolü
    
    private enum Phase {
        INIT,
        FIND_TARGET,
        MINING,
        TURNING,
        DONE,
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
        return 5;
    }
    
    @Override
    public boolean shouldActivate() {
        return false;
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        phase = Phase.INIT;
        isTurning = false;
        currentMiningBlock = null;
        redTarget = null;
        yellowTarget = null;
        miningStartTime = 0;
        waitingForCevir = false;
        
        if (mc.thePlayer != null) {
            // Başlangıç Y pozisyonunu kaydet
            startY = (int) Math.floor(mc.thePlayer.posY) - 1;
            
            // Başlangıç pozisyonunu kaydet
            lockedYaw = mc.thePlayer.rotationYaw;
            lockedPitch = mc.thePlayer.rotationPitch;
            
            // Yönü belirle (en yakın 90'ın katı)
            direction = getDirectionFromYaw(lockedYaw);
            lockedYaw = YAWS[direction];
            
            MuzMod.LOGGER.info("[Obsidian] Start - Y: " + startY + ", Yaw: " + lockedYaw + ", Direction: " + direction);
        }
        
        MinecraftForge.EVENT_BUS.register(this);
        MuzMod.LOGGER.info("[Obsidian] v0.7.11 enabled - Macro mode with auto-turn");
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        InputSimulator.releaseAll();
        MinecraftForge.EVENT_BUS.unregister(this);
        currentMiningBlock = null;
        redTarget = null;
        yellowTarget = null;
        startY = -1;
        MuzMod.LOGGER.info("[Obsidian] disabled");
    }
    
    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        ModConfig config = MuzMod.instance.getConfig();
        
        // Phase STOPPED veya DONE ise hiçbir şey yapma
        if (phase == Phase.STOPPED) {
            setStatus("§cDURDURULDU");
            return;
        }
        if (phase == Phase.DONE) {
            InputSimulator.releaseAll();
            setStatus("Tamamlandı!");
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
        
        // Blok ve oyuncu kontrolü (sadece mining sırasında)
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
            case FIND_TARGET:
                doFindTarget(config);
                break;
            case MINING:
                doMining(config);
                break;
            case TURNING:
                doTurning();
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
        
        double range = 3.0;
        List<EntityPlayer> players = mc.theWorld.playerEntities;
        
        for (EntityPlayer player : players) {
            if (player == mc.thePlayer) continue;
            
            double dx = player.posX - mc.thePlayer.posX;
            double dy = player.posY - mc.thePlayer.posY;
            double dz = player.posZ - mc.thePlayer.posZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            
            if (dist <= range && Math.abs(dy) <= 2) {
                double yaw = Math.toRadians(lockedYaw);
                double lookX = -Math.sin(yaw);
                double lookZ = Math.cos(yaw);
                
                double dot = (dx * lookX + dz * lookZ) / (dist + 0.001);
                if (dot > 0.5) {
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
                MuzMod.LOGGER.info("[Obsidian] Turn complete - New locked yaw: " + lockedYaw);
                phase = Phase.FIND_TARGET;
            }
            return;
        }
        
        // Jitter uygula (sadece mining sırasında)
        if (phase == Phase.MINING) {
            long now = System.currentTimeMillis();
            int jitterInterval = config.getObsidianJitterInterval();
            
            if (now - lastJitterTime >= jitterInterval) {
                lastJitterTime = now;
                float jitterYawRange = config.getObsidianJitterYaw();
                float jitterPitchRange = config.getObsidianJitterPitch();
                
                jitterYaw = (random.nextFloat() - 0.5f) * 2 * jitterYawRange;
                jitterPitch = (random.nextFloat() - 0.5f) * 2 * jitterPitchRange;
            }
        } else {
            jitterYaw = 0;
            jitterPitch = 0;
        }
        
        // Hedef pozisyon (kilitli + jitter)
        float targetYaw = lockedYaw + jitterYaw;
        float targetPitch = lockedPitch + jitterPitch;
        
        targetPitch = Math.max(-90, Math.min(90, targetPitch));
        
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
        
        if (startY == -1) {
            startY = (int) Math.floor(mc.thePlayer.posY) - 1;
        }
        
        phase = Phase.FIND_TARGET;
        MuzMod.LOGGER.info("[Obsidian] Init complete, finding target");
    }
    
    private void doFindTarget(ModConfig config) {
        int playerX = (int) Math.floor(mc.thePlayer.posX);
        int playerZ = (int) Math.floor(mc.thePlayer.posZ);
        
        // Önündeki obsidyen sayısı
        int count = countObsidianInDir(playerX, playerZ, direction);
        debugInfo = "Obsidyen: " + count;
        setStatus(debugInfo);
        
        if (count < 2) {
            // Sağ ve sol kontrol et
            int leftCount = countObsidianInDir(playerX, playerZ, turnLeft(direction));
            int rightCount = countObsidianInDir(playerX, playerZ, turnRight(direction));
            
            MuzMod.LOGGER.info("[Obsidian] Left: " + leftCount + ", Right: " + rightCount);
            
            if (leftCount >= 2 || rightCount >= 2) {
                // Daha çok obsidyen olan tarafa dön
                if (leftCount > rightCount) {
                    direction = turnLeft(direction);
                } else {
                    direction = turnRight(direction);
                }
                startTurn(YAWS[direction]);
                return;
            } else {
                // Arkaya bak
                int backDir = (direction + 2) % 4;
                int backCount = countObsidianInDir(playerX, playerZ, backDir);
                
                if (backCount >= 2) {
                    direction = backDir;
                    startTurn(YAWS[direction]);
                    return;
                } else {
                    MuzMod.LOGGER.info("[Obsidian] No obsidian found, done");
                    phase = Phase.DONE;
                    return;
                }
            }
        }
        
        // Hedef mesafesi: config'den okunan offset değerlerine göre
        int minOffset = config.getObsidianTargetMinOffset();
        int maxOffset = config.getObsidianTargetMaxOffset();
        
        int maxDist = count;
        int minDist = Math.max(2, maxDist - maxOffset);
        int actualMaxDist = Math.max(minDist, maxDist - minOffset);
        int targetDist = minDist + random.nextInt(Math.max(1, actualMaxDist - minDist + 1));
        
        // Kırmızı hedef belirle
        redTarget = new BlockPos(
            playerX + DX[direction] * targetDist,
            startY,
            playerZ + DZ[direction] * targetDist
        );
        
        MuzMod.LOGGER.info("[Obsidian] Red target: " + redTarget + " dist=" + targetDist + " (total=" + count + ")");
        
        // Sarı hedef (sonraki dönüş için önizleme)
        calculateYellowTarget(config);
        
        phase = Phase.MINING;
        miningStartTime = System.currentTimeMillis();
    }
    
    private void doMining(ModConfig config) {
        if (redTarget == null) {
            phase = Phase.FIND_TARGET;
            return;
        }
        
        // Mesafe kontrolü
        double dx = redTarget.getX() + 0.5 - mc.thePlayer.posX;
        double dz = redTarget.getZ() + 0.5 - mc.thePlayer.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        
        debugInfo = "Hedef: " + String.format("%.1f", dist) + " blok";
        setStatus("Kazıyor... [" + debugInfo + "]");
        
        // Hedefe ulaştık mı?
        if (dist < 1.5) {
            MuzMod.LOGGER.info("[Obsidian] Reached target!");
            InputSimulator.releaseAll();
            
            // Sarı hedef varsa ona geç, yoksa dön
            if (yellowTarget != null) {
                redTarget = yellowTarget;
                yellowTarget = null;
            }
            
            phase = Phase.TURNING;
            return;
        }
        
        // Periyodik olarak kalan obsidyen kontrolü yap
        long now = System.currentTimeMillis();
        if (now - lastTargetCheck >= TARGET_CHECK_INTERVAL) {
            lastTargetCheck = now;
            
            int playerX = (int) Math.floor(mc.thePlayer.posX);
            int playerZ = (int) Math.floor(mc.thePlayer.posZ);
            int remaining = countObsidianInDir(playerX, playerZ, direction);
            
            // Eğer önde çok az obsidyen kaldıysa ve hedefe yaklaştıysak, erken dönüş yap
            if (remaining < 3 && dist < 3) {
                MuzMod.LOGGER.info("[Obsidian] Low obsidian remaining (" + remaining + "), turning early");
                InputSimulator.releaseAll();
                phase = Phase.TURNING;
                return;
            }
        }
        
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
    
    private void doTurning() {
        debugInfo = "Dönüyor...";
        setStatus(debugInfo);
        
        InputSimulator.releaseAll();
        
        int playerX = (int) Math.floor(mc.thePlayer.posX);
        int playerZ = (int) Math.floor(mc.thePlayer.posZ);
        
        // Sağ ve sol kontrol et
        int leftDir = turnLeft(direction);
        int rightDir = turnRight(direction);
        
        int leftCount = countObsidianInDir(playerX, playerZ, leftDir);
        int rightCount = countObsidianInDir(playerX, playerZ, rightDir);
        
        MuzMod.LOGGER.info("[Obsidian] Turn - Left: " + leftCount + ", Right: " + rightCount);
        
        if (leftCount < 2 && rightCount < 2) {
            // Arkaya bak
            int backDir = (direction + 2) % 4;
            int backCount = countObsidianInDir(playerX, playerZ, backDir);
            
            if (backCount >= 2) {
                direction = backDir;
                MuzMod.LOGGER.info("[Obsidian] Turning back");
            } else {
                MuzMod.LOGGER.info("[Obsidian] No obsidian anywhere, done");
                phase = Phase.DONE;
                return;
            }
        } else if (leftCount > rightCount) {
            direction = leftDir;
            MuzMod.LOGGER.info("[Obsidian] Turning left");
        } else {
            direction = rightDir;
            MuzMod.LOGGER.info("[Obsidian] Turning right");
        }
        
        startTurn(YAWS[direction]);
    }
    
    /**
     * Dönüş başlat
     */
    private void startTurn(float targetYaw) {
        // Yaw farkını normalize et
        float diff = targetYaw - mc.thePlayer.rotationYaw;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        turnTargetYaw = mc.thePlayer.rotationYaw + diff;
        
        isTurning = true;
        MuzMod.LOGGER.info("[Obsidian] Starting turn to yaw: " + targetYaw);
    }
    
    /**
     * Sarı hedef hesapla (sonraki dönüş için önizleme)
     */
    private void calculateYellowTarget(ModConfig config) {
        if (redTarget == null) {
            yellowTarget = null;
            return;
        }
        
        int leftDir = turnLeft(direction);
        int rightDir = turnRight(direction);
        
        int leftCount = countObsidianInDir(redTarget.getX(), redTarget.getZ(), leftDir);
        int rightCount = countObsidianInDir(redTarget.getX(), redTarget.getZ(), rightDir);
        
        int nextDir = leftCount > rightCount ? leftDir : rightDir;
        int nextCount = Math.max(leftCount, rightCount);
        
        if (nextCount >= 2) {
            int minOffset = config.getObsidianTargetMinOffset();
            int maxOffset = config.getObsidianTargetMaxOffset();
            
            int maxDist = nextCount;
            int minDist = Math.max(2, maxDist - maxOffset);
            int actualMaxDist = Math.max(minDist, maxDist - minOffset);
            int targetDist = minDist + random.nextInt(Math.max(1, actualMaxDist - minDist + 1));
            
            yellowTarget = new BlockPos(
                redTarget.getX() + DX[nextDir] * targetDist,
                startY,
                redTarget.getZ() + DZ[nextDir] * targetDist
            );
            MuzMod.LOGGER.info("[Obsidian] Yellow target: " + yellowTarget);
        } else {
            yellowTarget = null;
        }
    }
    
    // ===================== HELPER METHODS =====================
    
    private int getDirectionFromYaw(float yaw) {
        while (yaw < 0) yaw += 360;
        while (yaw >= 360) yaw -= 360;
        
        if (yaw >= 315 || yaw < 45) return 0;   // Güney (0)
        if (yaw >= 45 && yaw < 135) return 1;   // Batı (90)
        if (yaw >= 135 && yaw < 225) return 2;  // Kuzey (180)
        return 3;  // Doğu (270/-90)
    }
    
    private int turnLeft(int dir) {
        return (dir + 3) % 4;
    }
    
    private int turnRight(int dir) {
        return (dir + 1) % 4;
    }
    
    private int countObsidianInDir(int startX, int startZ, int dir) {
        int count = 0;
        for (int i = 1; i <= 100; i++) {
            int x = startX + DX[dir] * i;
            int z = startZ + DZ[dir] * i;
            // Y seviyesinden bağımsız - birkaç Y seviyesinde obsidyen ara
            boolean foundObsidian = false;
            for (int yOffset = -2; yOffset <= 2; yOffset++) {
                BlockPos pos = new BlockPos(x, startY + yOffset, z);
                if (mc.theWorld.getBlockState(pos).getBlock() == Blocks.obsidian) {
                    foundObsidian = true;
                    break;
                }
            }
            if (foundObsidian) {
                count++;
            }
        }
        return count;
    }
    
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
        
        // Kamera pozisyonunu hesapla
        double camX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double camY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks;
        double camZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;
        
        // Kırmızı hedef
        if (redTarget != null) {
            renderBlockOutline(redTarget, 1, 0, 0, 0.8f, camX, camY, camZ);
        }
        
        // Sarı hedef
        if (yellowTarget != null) {
            renderBlockOutline(yellowTarget, 1, 1, 0, 0.6f, camX, camY, camZ);
        }
        
        // Yeşil - şu an baktığımız blok
        if (currentMiningBlock != null) {
            Block block = mc.theWorld.getBlockState(currentMiningBlock).getBlock();
            if (block == Blocks.obsidian) {
                renderBlockOutline(currentMiningBlock, 0, 1, 0, 0.8f, camX, camY, camZ);
            } else if (block != Blocks.air) {
                renderBlockOutline(currentMiningBlock, 1, 0.5f, 0, 0.8f, camX, camY, camZ); // Turuncu (yanlış)
            }
        }
    }
    
    private void renderBlockOutline(BlockPos pos, float r, float g, float b, float a, double camX, double camY, double camZ) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        
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
        wr.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        tessellator.draw();
        
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        tessellator.draw();
        
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
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
        if (phase == Phase.DONE) {
            return "§a" + status;
        }
        return status;
    }
}
