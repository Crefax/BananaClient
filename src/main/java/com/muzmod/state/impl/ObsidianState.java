package com.muzmod.state.impl;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
import com.muzmod.state.AbstractState;
import com.muzmod.util.InputSimulator;
import com.muzmod.navigation.NavigationManager;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.*;

/**
 * Obsidian Mining State v1.1.0
 * 
 * Atolye bölgesinde obsidyen kazma sistemi.
 * 
 * Özellikler:
 * - /warp atolye ile ışınlanma
 * - Config'den okunan ilk hareket (ileri + yan tek seferde)
 * - En üst obsidyen tabakasına gitme
 * - Obsidyenlerin dışına çıkmama
 * - Değişken patern ile kazma (aynı yönde sürekli gitmeme)
 * - Diğer oyuncuların kazdığı boş alanlara gitmeme
 * - A* benzeri hedef belirleme
 */
public class ObsidianState extends AbstractState {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();
    private final ModConfig config = MuzMod.instance.getConfig();
    
    // Fazlar
    private ObsidianPhase phase = ObsidianPhase.WARPING;
    private long phaseStartTime = 0;
    private long lastActionTime = 0;
    
    // Warp ayarları (config'den okunacak)
    private static final long WARP_DELAY = 3000;
    
    // İlk hareket hedefi
    private BlockPos initialTarget = null;
    private boolean initialMoveCompleted = false;
    
    // Obsidyen alan bilgileri
    private BlockPos topLayerY = null;  // En üst obsidyen seviyesi
    private BlockPos miningCenter = null;  // Mining merkezi
    private Set<BlockPos> obsidianBounds = new HashSet<>();  // Obsidyen sınırları
    
    // Hareket ve kazma
    private BlockPos currentTarget = null;
    private int blocksMinedInDirection = 0;
    private float currentYaw = 0;
    private int directionChanges = 0;
    private static final int BLOCKS_BEFORE_TURN = 8;  // Kaç blok sonra yön değiştir
    
    // Retry ve güvenlik
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;
    private long stuckCheckTime = 0;
    private BlockPos lastPosition = null;
    private int stuckCounter = 0;
    
    // Debug
    private String debugInfo = "";
    
    private enum ObsidianPhase {
        WARPING,           // Atolye'ye ışınlanma
        WAITING_WARP,      // Warp'ın tamamlanmasını bekle
        INITIAL_MOVE,      // Config'den okunan ilk hareket (tek seferde)
        FINDING_TOP,       // En üst obsidyen tabakasını bul
        POSITIONING,       // Doğru pozisyona git
        DESCENDING,        // 1 blok aşağı in
        SCANNING,          // Çevreyi tara, hedef belirle
        MINING,            // Obsidyen kaz
        REPOSITIONING,     // Yön değiştir
        WALKING,           // Hedef bloğa yürü
        DONE,              // Tamamlandı
        FAILED             // Başarısız
    }
    
    public ObsidianState() {
        this.status = "Obsidyen hazır";
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        phase = ObsidianPhase.WARPING;
        phaseStartTime = System.currentTimeMillis();
        lastActionTime = System.currentTimeMillis();
        retryCount = 0;
        blocksMinedInDirection = 0;
        directionChanges = 0;
        currentTarget = null;
        topLayerY = null;
        miningCenter = null;
        obsidianBounds.clear();
        stuckCounter = 0;
        initialTarget = null;
        initialMoveCompleted = false;
        
        InputSimulator.releaseAll();
        setStatus("Obsidyen başlıyor");
        
        MuzMod.LOGGER.info("[Obsidian] =============================");
        MuzMod.LOGGER.info("[Obsidian] OBSIDIAN STATE BAŞLADI");
        MuzMod.LOGGER.info("[Obsidian] Config: Forward=" + config.getObsidianForwardMin() + "-" + config.getObsidianForwardMax() +
                          ", Side=" + config.getObsidianSideMin() + "-" + config.getObsidianSideMax() + 
                          ", GoLeft=" + config.isObsidianGoLeft());
        MuzMod.LOGGER.info("[Obsidian] =============================");
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        InputSimulator.releaseAll();
        MuzMod.LOGGER.info("[Obsidian] OBSIDIAN STATE KAPANDI");
    }
    
    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        long now = System.currentTimeMillis();
        
        // Stuck detection
        checkStuck(now);
        
        switch (phase) {
            case WARPING:
                handleWarping();
                break;
            case WAITING_WARP:
                handleWaitingWarp(now);
                break;
            case INITIAL_MOVE:
                handleInitialMove();
                break;
            case FINDING_TOP:
                handleFindingTop();
                break;
            case POSITIONING:
                handlePositioning();
                break;
            case DESCENDING:
                handleDescending();
                break;
            case SCANNING:
                handleScanning();
                break;
            case MINING:
                handleMining();
                break;
            case REPOSITIONING:
                handleRepositioning();
                break;
            case WALKING:
                handleWalking();
                break;
            case DONE:
                setStatus("Tamamlandı");
                MuzMod.instance.getStateManager().forceState("idle");
                break;
            case FAILED:
                setStatus("Başarısız!");
                MuzMod.instance.getStateManager().forceState("idle");
                break;
        }
    }
    
    // ==================== PHASE HANDLERS ====================
    
    private void handleWarping() {
        debugInfo = "Warp gönderiliyor...";
        setStatus(debugInfo);
        
        String warpCommand = config.getObsidianWarpCommand();
        mc.thePlayer.sendChatMessage(warpCommand);
        MuzMod.LOGGER.info("[Obsidian] Warp komutu gönderildi: " + warpCommand);
        
        setPhase(ObsidianPhase.WAITING_WARP);
        phaseStartTime = System.currentTimeMillis();
    }
    
    private void handleWaitingWarp(long now) {
        debugInfo = "Warp bekleniyor...";
        setStatus(debugInfo);
        
        if (now - phaseStartTime >= WARP_DELAY) {
            MuzMod.LOGGER.info("[Obsidian] Warp tamamlandı, ilk hareket hesaplanıyor...");
            setPhase(ObsidianPhase.INITIAL_MOVE);
        }
    }
    
    /**
     * İlk hareket - Config'den okunan ileri ve yan mesafeyi
     * tek bir hedef olarak hesaplayıp NavigationManager'a gönder
     */
    private void handleInitialMove() {
        NavigationManager nav = NavigationManager.getInstance();
        
        // Eğer henüz hedef hesaplanmadıysa hesapla
        if (initialTarget == null) {
            initialTarget = calculateInitialTarget();
            MuzMod.LOGGER.info("[Obsidian] İlk hareket hedefi hesaplandı: " + initialTarget);
            
            debugInfo = "İlk pozisyona gidiliyor...";
            setStatus(debugInfo);
            
            // Tek seferde hedefe git
            nav.goTo(initialTarget).onComplete(() -> {
                MuzMod.LOGGER.info("[Obsidian] İlk hareket tamamlandı!");
                initialMoveCompleted = true;
            });
            return;
        }
        
        // Navigation tamamlandı mı?
        if (initialMoveCompleted || !nav.isNavigating()) {
            MuzMod.LOGGER.info("[Obsidian] İlk hareket bitti, üst tabaka aranıyor...");
            setPhase(ObsidianPhase.FINDING_TOP);
            return;
        }
        
        // Navigation devam ediyor
        debugInfo = String.format("İlk pozisyona gidiliyor... (%.1f blok kaldı)", 
            Math.sqrt(mc.thePlayer.getPosition().distanceSq(initialTarget)));
        setStatus(debugInfo);
    }
    
    /**
     * Config'den okunan değerlerle ilk hareket hedefini hesapla
     * 
     * Oyuncu spawn'da güneye (Z+) bakıyor kabul ediyoruz.
     * İleri = Z+ yönünde (güney)
     * Sol = X- yönünde (batı)
     * Sağ = X+ yönünde (doğu)
     */
    private BlockPos calculateInitialTarget() {
        BlockPos playerPos = mc.thePlayer.getPosition();
        
        // Config'den değerleri al
        int forwardMin = config.getObsidianForwardMin();
        int forwardMax = config.getObsidianForwardMax();
        int sideMin = config.getObsidianSideMin();
        int sideMax = config.getObsidianSideMax();
        boolean goLeft = config.isObsidianGoLeft();
        
        // Rastgele mesafeler belirle
        int forwardDist = forwardMin + random.nextInt(Math.max(1, forwardMax - forwardMin + 1));
        int sideDist = sideMin + random.nextInt(Math.max(1, sideMax - sideMin + 1));
        
        MuzMod.LOGGER.info("[Obsidian] Hesaplanan mesafeler: ileri=" + forwardDist + ", yan=" + sideDist + ", sol=" + goLeft);
        
        // Hedef pozisyonu hesapla
        // İleri = Z+ (güney)
        // Sol = X- (batı), Sağ = X+ (doğu)
        int targetX = playerPos.getX() + (goLeft ? -sideDist : sideDist);
        int targetZ = playerPos.getZ() + forwardDist;
        int targetY = playerPos.getY();
        
        return new BlockPos(targetX, targetY, targetZ);
    }
    
    private void handleFindingTop() {
        debugInfo = "En üst obsidyen tabakası aranıyor...";
        setStatus(debugInfo);
        
        // Çevrede obsidyen ara
        BlockPos playerPos = mc.thePlayer.getPosition();
        int searchRadius = 30;
        int highestY = -1;
        BlockPos highestObsidian = null;
        
        // Obsidyen alanını tara
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                for (int y = 10; y >= -10; y--) {  // Yukarıdan aşağı tara
                    BlockPos checkPos = playerPos.add(x, y, z);
                    Block block = mc.theWorld.getBlockState(checkPos).getBlock();
                    
                    if (block == Blocks.obsidian) {
                        if (checkPos.getY() > highestY) {
                            highestY = checkPos.getY();
                            highestObsidian = checkPos;
                        }
                        obsidianBounds.add(checkPos);
                    }
                }
            }
        }
        
        if (highestObsidian != null) {
            topLayerY = new BlockPos(highestObsidian.getX(), highestY, highestObsidian.getZ());
            miningCenter = playerPos;
            MuzMod.LOGGER.info("[Obsidian] En üst tabaka bulundu: Y=" + highestY + ", Toplam obsidyen: " + obsidianBounds.size());
            setPhase(ObsidianPhase.POSITIONING);
        } else {
            MuzMod.LOGGER.warn("[Obsidian] Obsidyen bulunamadı!");
            retryOrFail();
        }
    }
    
    private void handlePositioning() {
        debugInfo = "Üst tabakaya gidiliyor...";
        setStatus(debugInfo);
        
        BlockPos playerPos = mc.thePlayer.getPosition();
        int targetY = topLayerY.getY() + 1;  // Obsidyenin 1 üstünde dur
        
        // Yatay mesafeyi kontrol et
        BlockPos nearestTopObsidian = findNearestObsidianAtY(playerPos, topLayerY.getY());
        
        if (nearestTopObsidian == null) {
            MuzMod.LOGGER.warn("[Obsidian] Üst tabakada obsidyen bulunamadı");
            setPhase(ObsidianPhase.SCANNING);
            return;
        }
        
        double horizontalDist = Math.sqrt(
            Math.pow(playerPos.getX() - nearestTopObsidian.getX(), 2) +
            Math.pow(playerPos.getZ() - nearestTopObsidian.getZ(), 2)
        );
        
        // Üst tabakadayız mı?
        if (playerPos.getY() == targetY && horizontalDist < 3) {
            MuzMod.LOGGER.info("[Obsidian] Üst tabakada, aşağı iniliyor...");
            setPhase(ObsidianPhase.DESCENDING);
            return;
        }
        
        // Üst tabakaya git
        // Basit yürüyüş - en yakın üst obsidyene doğru
        float[] rotation = getRotationToBlock(nearestTopObsidian.up());
        smoothRotateTo(rotation[0], rotation[1]);
        
        if (mc.thePlayer.onGround) {
            InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
            
            // Zıplama gerekiyorsa
            if (playerPos.getY() < targetY) {
                InputSimulator.holdKey(mc.gameSettings.keyBindJump, true);
            }
        }
    }
    
    private void handleDescending() {
        debugInfo = "Obsidyen seviyesine iniliyor...";
        setStatus(debugInfo);
        
        InputSimulator.releaseAll();
        
        BlockPos playerPos = mc.thePlayer.getPosition();
        int obsidianY = topLayerY.getY();
        
        // Şu an obsidyen seviyesindeyiz mi? (Obsidyenin üstünde duruyoruz)
        BlockPos below = playerPos.down();
        Block blockBelow = mc.theWorld.getBlockState(below).getBlock();
        
        if (blockBelow == Blocks.obsidian && playerPos.getY() == obsidianY + 1) {
            // Doğru pozisyondayız, şimdi önümüzdeki obsidyeni kazarak ilerleyeceğiz
            MuzMod.LOGGER.info("[Obsidian] Obsidyen seviyesine ulaşıldı, kazma başlıyor...");
            
            // Rastgele başlangıç yönü belirle
            currentYaw = mc.thePlayer.rotationYaw;
            // 4 ana yönden birine yuvarla
            currentYaw = Math.round(currentYaw / 90) * 90;
            
            setPhase(ObsidianPhase.SCANNING);
            return;
        }
        
        // Henüz doğru pozisyonda değiliz, aşağı bakalım ve önümüzdeki bloğu kazalım
        // Veya üstteki bloğu kazalım
        BlockPos above = playerPos.up();
        Block blockAbove = mc.theWorld.getBlockState(above).getBlock();
        
        if (blockAbove != Blocks.air) {
            // Üstteki bloğu kaz
            lookAtBlock(above);
            InputSimulator.holdLeftClick(true);
        } else if (playerPos.getY() > obsidianY + 1) {
            // Aşağı in
            // Ayağımızın altındaki bloğu kaz
            lookAtBlock(below);
            if (blockBelow != Blocks.obsidian) {
                InputSimulator.holdLeftClick(true);
            }
        }
    }
    
    private void handleScanning() {
        debugInfo = "Hedef aranıyor...";
        setStatus(debugInfo);
        
        InputSimulator.releaseAll();
        
        // En iyi kazma hedefini bul
        currentTarget = findBestMiningTarget();
        
        if (currentTarget != null) {
            MuzMod.LOGGER.info("[Obsidian] Hedef bulundu: " + currentTarget);
            setPhase(ObsidianPhase.MINING);
        } else {
            // Hedef bulunamadı, yön değiştir
            MuzMod.LOGGER.info("[Obsidian] Hedef bulunamadı, yön değiştiriliyor...");
            setPhase(ObsidianPhase.REPOSITIONING);
        }
    }
    
    private void handleMining() {
        if (currentTarget == null) {
            setPhase(ObsidianPhase.SCANNING);
            return;
        }
        
        Block targetBlock = mc.theWorld.getBlockState(currentTarget).getBlock();
        
        // Hedef blok hala obsidyen mi?
        if (targetBlock != Blocks.obsidian) {
            blocksMinedInDirection++;
            MuzMod.LOGGER.info("[Obsidian] Blok kazıldı, toplam: " + blocksMinedInDirection);
            
            // Belirli sayıda blok sonra yön değiştir
            if (blocksMinedInDirection >= BLOCKS_BEFORE_TURN + random.nextInt(5)) {
                blocksMinedInDirection = 0;
                directionChanges++;
                setPhase(ObsidianPhase.REPOSITIONING);
                return;
            }
            
            // Yeni hedef bul
            setPhase(ObsidianPhase.SCANNING);
            return;
        }
        
        // Hedefe bak ve kaz
        float[] rotation = getRotationToBlock(currentTarget);
        smoothRotateTo(rotation[0], rotation[1]);
        
        // Rotasyon tamamlandıysa kaz
        float yawDiff = Math.abs(mc.thePlayer.rotationYaw - rotation[0]);
        while (yawDiff > 180) yawDiff -= 360;
        
        if (Math.abs(yawDiff) < 5) {
            InputSimulator.holdLeftClick(true);
            debugInfo = String.format("Obsidyen kazılıyor... (%d/%d)", 
                blocksMinedInDirection, BLOCKS_BEFORE_TURN);
            setStatus(debugInfo);
        } else {
            InputSimulator.releaseLeftClick();
            debugInfo = "Hedefe dönülüyor...";
            setStatus(debugInfo);
        }
    }
    
    private void handleRepositioning() {
        debugInfo = "Yön değiştiriliyor...";
        setStatus(debugInfo);
        
        InputSimulator.releaseAll();
        
        // Yeni yön belirle (90 derece sağa veya sola)
        float turnAmount = random.nextBoolean() ? 90 : -90;
        
        // Bazen 180 derece dön
        if (random.nextFloat() < 0.2f) {
            turnAmount = 180;
        }
        
        currentYaw += turnAmount;
        // Normalize
        while (currentYaw > 180) currentYaw -= 360;
        while (currentYaw < -180) currentYaw += 360;
        
        MuzMod.LOGGER.info("[Obsidian] Yeni yön: " + currentYaw + "° (dönüş: " + turnAmount + "°)");
        
        // Dönüşü uygula
        smoothRotateTo(currentYaw, 0);
        
        // Dönüş tamamlandıysa taramaya geç
        float diff = Math.abs(mc.thePlayer.rotationYaw - currentYaw);
        while (diff > 180) diff -= 360;
        
        if (Math.abs(diff) < 5) {
            setPhase(ObsidianPhase.SCANNING);
        }
    }
    
    private void handleWalking() {
        // İleri yürü (obsidyen olmayan alana)
        if (currentTarget == null) {
            setPhase(ObsidianPhase.SCANNING);
            return;
        }
        
        BlockPos playerPos = mc.thePlayer.getPosition();
        double dist = playerPos.distanceSq(currentTarget);
        
        if (dist < 4) {
            // Hedefe ulaştık
            InputSimulator.releaseAll();
            setPhase(ObsidianPhase.SCANNING);
            return;
        }
        
        // Hedefe yürü
        float[] rotation = getRotationToBlock(currentTarget);
        smoothRotateTo(rotation[0], rotation[1]);
        InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
        
        debugInfo = "Hedefe yürünüyor...";
        setStatus(debugInfo);
    }
    
    // ==================== HELPER METHODS ====================
    
    private void setPhase(ObsidianPhase newPhase) {
        phase = newPhase;
        phaseStartTime = System.currentTimeMillis();
        lastActionTime = System.currentTimeMillis();
        MuzMod.LOGGER.info("[Obsidian] -> Faz: " + newPhase);
    }
    
    private BlockPos findNearestObsidianAtY(BlockPos from, int y) {
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        
        for (BlockPos pos : obsidianBounds) {
            if (pos.getY() == y) {
                double dist = from.distanceSq(pos);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = pos;
                }
            }
        }
        
        return nearest;
    }
    
    /**
     * En iyi kazma hedefini bul
     * Kriterler:
     * - Önümüzde olmalı
     * - En üst tabakada olmalı
     * - Boş alanların yanında olmamalı (diğer oyuncuların kazdığı)
     * - Obsidyen sınırları içinde olmalı
     */
    private BlockPos findBestMiningTarget() {
        BlockPos playerPos = mc.thePlayer.getPosition();
        int obsidianY = topLayerY.getY();
        
        // Önümüzdeki yöne doğru tara
        double rad = Math.toRadians(currentYaw);
        
        // 1-5 blok önümüzü tara
        for (int dist = 1; dist <= 5; dist++) {
            int checkX = playerPos.getX() - (int)(Math.sin(rad) * dist);
            int checkZ = playerPos.getZ() + (int)(Math.cos(rad) * dist);
            
            BlockPos checkPos = new BlockPos(checkX, obsidianY, checkZ);
            Block block = mc.theWorld.getBlockState(checkPos).getBlock();
            
            if (block == Blocks.obsidian) {
                // Bu obsidyenin yanında çok fazla boşluk var mı kontrol et
                if (!isNearEmptyArea(checkPos)) {
                    return checkPos;
                }
            }
        }
        
        // Önde obsidyen yoksa, yanlarda ara
        for (int angle = -45; angle <= 45; angle += 15) {
            if (angle == 0) continue;
            
            double checkRad = Math.toRadians(currentYaw + angle);
            
            for (int dist = 1; dist <= 3; dist++) {
                int checkX = playerPos.getX() - (int)(Math.sin(checkRad) * dist);
                int checkZ = playerPos.getZ() + (int)(Math.cos(checkRad) * dist);
                
                BlockPos checkPos = new BlockPos(checkX, obsidianY, checkZ);
                Block block = mc.theWorld.getBlockState(checkPos).getBlock();
                
                if (block == Blocks.obsidian && !isNearEmptyArea(checkPos)) {
                    return checkPos;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Bu pozisyon çok fazla boş alanın yanında mı?
     * (Diğer oyuncuların kazdığı yerler)
     */
    private boolean isNearEmptyArea(BlockPos pos) {
        int emptyCount = 0;
        int obsidianY = topLayerY.getY();
        
        // 4 yönü kontrol et
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            BlockPos neighbor = pos.offset(facing);
            Block block = mc.theWorld.getBlockState(neighbor).getBlock();
            
            if (block != Blocks.obsidian) {
                emptyCount++;
            }
        }
        
        // 2 veya daha fazla tarafı boşsa, bu bir kenar veya köşe
        return emptyCount >= 3;
    }
    
    /**
     * Obsidyen alanının dışında mıyız?
     */
    private boolean isOutsideObsidianArea(BlockPos pos) {
        int obsidianY = topLayerY.getY();
        
        // Bu seviyede herhangi bir obsidyen var mı kontrol et
        BlockPos checkPos = new BlockPos(pos.getX(), obsidianY, pos.getZ());
        
        // 3 blok çapında obsidyen var mı
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos nearby = checkPos.add(x, 0, z);
                if (mc.theWorld.getBlockState(nearby).getBlock() == Blocks.obsidian) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private void checkStuck(long now) {
        if (now - stuckCheckTime < 2000) return;
        stuckCheckTime = now;
        
        BlockPos currentPos = mc.thePlayer.getPosition();
        
        if (lastPosition != null && lastPosition.equals(currentPos)) {
            stuckCounter++;
            
            if (stuckCounter >= 5) {
                MuzMod.LOGGER.warn("[Obsidian] Stuck detected, repositioning...");
                stuckCounter = 0;
                setPhase(ObsidianPhase.REPOSITIONING);
            }
        } else {
            stuckCounter = 0;
        }
        
        lastPosition = currentPos;
    }
    
    private void retryOrFail() {
        retryCount++;
        if (retryCount >= MAX_RETRIES) {
            MuzMod.LOGGER.error("[Obsidian] Max retry aşıldı!");
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
        
        // Normalize yaw
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        // Smooth rotation
        float rotSpeed = 15.0f;
        float yawStep = Math.signum(yawDiff) * Math.min(Math.abs(yawDiff), rotSpeed);
        float pitchStep = Math.signum(pitchDiff) * Math.min(Math.abs(pitchDiff), rotSpeed);
        
        mc.thePlayer.rotationYaw += yawStep;
        mc.thePlayer.rotationPitch += pitchStep;
        
        // Clamp pitch
        mc.thePlayer.rotationPitch = Math.max(-90, Math.min(90, mc.thePlayer.rotationPitch));
    }
    
    private void lookAtBlock(BlockPos pos) {
        float[] rotation = getRotationToBlock(pos);
        mc.thePlayer.rotationYaw = rotation[0];
        mc.thePlayer.rotationPitch = rotation[1];
    }
    
    @Override
    public String getName() {
        return "Obsidyen";
    }
    
    @Override
    public boolean shouldActivate() {
        return false;  // Manuel aktivasyon
    }
    
    @Override
    public int getPriority() {
        return 3;
    }
    
    // Debug getters
    public String getDebugInfo() { return debugInfo; }
    public ObsidianPhase getPhase() { return phase; }
    public int getBlocksMinedInDirection() { return blocksMinedInDirection; }
    public int getDirectionChanges() { return directionChanges; }
}
