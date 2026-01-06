package com.muzmod.state.impl;

import com.muzmod.MuzMod;
import com.muzmod.state.IState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * OX Event State
 * - /warp ox ile ışınlanır
 * - 30-50 blok düz ilerler
 * - Çoğunluğun bulunduğu tarafa (lime/red) gider
 * - Smooth rotation ile hareket eder
 */
public class OXState implements IState {
    
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();
    
    // State aşamaları
    private static final int STEP_IDLE = 0;
    private static final int STEP_WARPING = 1;
    private static final int STEP_WARP_WAIT = 2;
    private static final int STEP_WALKING_FORWARD = 3;
    private static final int STEP_WAITING_FOR_QUESTION = 4;
    private static final int STEP_ANALYZING = 5;
    private static final int STEP_MOVING_TO_SIDE = 6;
    private static final int STEP_ON_POSITION = 7;
    
    private int currentStep = STEP_IDLE;
    private String status = "Bekliyor...";
    
    // Hareket değişkenleri
    private int walkDistance = 0;
    private int targetWalkDistance = 0;
    private int walkTicks = 0;
    private long stepStartTime = 0;
    
    // Pozisyon takibi
    private BlockPos startPos = null;
    private double startYaw = 0;
    
    // Hedef taraf (true = lime/yeşil, false = red/kırmızı)
    private Boolean targetSide = null;
    private float targetYaw = 0;
    private boolean isRotating = false;
    
    // Smooth rotation
    private float currentRotationSpeed = 0;
    private float maxRotationSpeed = 8.0f;
    
    // Analiz sonuçları
    private int playersOnLime = 0;
    private int playersOnRed = 0;
    private int analysisCount = 0;
    
    // Warp komutu
    private String warpCommand = "/warp ox";
    
    @Override
    public String getName() {
        return "OX Event";
    }
    
    @Override
    public String getStatus() {
        return status;
    }
    
    @Override
    public int getPriority() {
        return 60; // Mining'den düşük, AFK'dan yüksek
    }
    
    @Override
    public boolean shouldActivate() {
        return false; // Manuel veya schedule ile aktifleşir
    }
    
    @Override
    public void onEnable() {
        currentStep = STEP_WARPING;
        status = "OX Event başlatılıyor...";
        stepStartTime = System.currentTimeMillis();
        targetSide = null;
        analysisCount = 0;
        MuzMod.LOGGER.info("[OXState] Enabled");
    }
    
    @Override
    public void onDisable() {
        releaseAllKeys();
        currentStep = STEP_IDLE;
        status = "Durduruldu";
        isRotating = false;
        MuzMod.LOGGER.info("[OXState] Disabled");
    }
    
    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        EntityPlayerSP player = mc.thePlayer;
        long now = System.currentTimeMillis();
        
        switch (currentStep) {
            case STEP_WARPING:
                executeWarp();
                break;
                
            case STEP_WARP_WAIT:
                handleWarpWait(now);
                break;
                
            case STEP_WALKING_FORWARD:
                handleWalkingForward(player, now);
                break;
                
            case STEP_WAITING_FOR_QUESTION:
                handleWaitingForQuestion(player, now);
                break;
                
            case STEP_ANALYZING:
                handleAnalyzing(player);
                break;
                
            case STEP_MOVING_TO_SIDE:
                handleMovingToSide(player);
                break;
                
            case STEP_ON_POSITION:
                handleOnPosition(player, now);
                break;
        }
        
        // Smooth rotation uygula
        if (isRotating) {
            applySmoothRotation(player);
        }
    }
    
    private void executeWarp() {
        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage(warpCommand);
            currentStep = STEP_WARP_WAIT;
            stepStartTime = System.currentTimeMillis();
            status = "Işınlanıyor...";
            MuzMod.LOGGER.info("[OXState] Warp command sent: " + warpCommand);
        }
    }
    
    private void handleWarpWait(long now) {
        // 3 saniye bekle
        if (now - stepStartTime > 3000) {
            currentStep = STEP_WALKING_FORWARD;
            targetWalkDistance = 30 + random.nextInt(21); // 30-50 blok
            walkTicks = 0;
            startPos = mc.thePlayer.getPosition();
            startYaw = mc.thePlayer.rotationYaw;
            status = "İleri yürünüyor (" + targetWalkDistance + " blok)...";
            MuzMod.LOGGER.info("[OXState] Walking forward " + targetWalkDistance + " blocks");
        }
    }
    
    private void handleWalkingForward(EntityPlayerSP player, long now) {
        // İleri yürü
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
        
        // Mesafeyi kontrol et
        if (startPos != null) {
            double distance = player.getDistance(startPos.getX(), startPos.getY(), startPos.getZ());
            walkDistance = (int) distance;
            status = "İleri yürünüyor... " + walkDistance + "/" + targetWalkDistance + " blok";
            
            if (distance >= targetWalkDistance) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                currentStep = STEP_WAITING_FOR_QUESTION;
                stepStartTime = System.currentTimeMillis();
                status = "Soru bekleniyor...";
                MuzMod.LOGGER.info("[OXState] Reached position, waiting for question");
            }
        }
        
        walkTicks++;
        // Timeout - 30 saniye
        if (walkTicks > 600) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
            currentStep = STEP_WAITING_FOR_QUESTION;
            stepStartTime = System.currentTimeMillis();
        }
    }
    
    private void handleWaitingForQuestion(EntityPlayerSP player, long now) {
        // Her 500ms'de bir analiz yap
        if (now - stepStartTime > 500) {
            stepStartTime = now;
            analyzePlayerPositions(player);
            
            // Herhangi bir oyuncu varsa hemen çoğunluğa git
            int total = playersOnLime + playersOnRed;
            if (total >= 1) {
                currentStep = STEP_ANALYZING;
            }
        }
        
        status = "Soru bekleniyor... (L:" + playersOnLime + " R:" + playersOnRed + ")";
    }
    
    private void handleAnalyzing(EntityPlayerSP player) {
        analyzePlayerPositions(player);
        
        int total = playersOnLime + playersOnRed;
        if (total < 1) {
            status = "Oyuncu bekleniyor...";
            return;
        }
        
        // Çoğunluğu belirle - eşitlikte yeşile git
        boolean shouldGoLime = playersOnLime >= playersOnRed;
        
        // Şu an hangi taraftayız?
        Boolean currentSide = getCurrentSide(player);
        
        status = "Analiz: L=" + playersOnLime + " R=" + playersOnRed + 
                 " -> " + (shouldGoLime ? "YEŞİL" : "KIRMIZI");
        
        // Eğer hiç taraf seçmediyse veya ortadaysa hareket et
        if (currentSide == null) {
            targetSide = shouldGoLime;
            startSmoothRotation(player, shouldGoLime);
            currentStep = STEP_MOVING_TO_SIDE;
            MuzMod.LOGGER.info("[OXState] Moving to " + (shouldGoLime ? "LIME" : "RED") + " side (initial)");
        } else if (currentSide != shouldGoLime) {
            // Yanlış taraftayız - çoğunluğa git
            targetSide = shouldGoLime;
            startSmoothRotation(player, shouldGoLime);
            currentStep = STEP_MOVING_TO_SIDE;
            MuzMod.LOGGER.info("[OXState] Moving to " + (shouldGoLime ? "LIME" : "RED") + " side");
        } else {
            currentStep = STEP_ON_POSITION;
            stepStartTime = System.currentTimeMillis();
            status = "Doğru pozisyonda!";
        }
    }
    
    private void handleMovingToSide(EntityPlayerSP player) {
        // Hedef tarafa doğru yürü
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
        
        // Doğru blokta mıyız kontrol et
        Boolean currentSide = getCurrentSide(player);
        
        if (currentSide != null && currentSide == targetSide) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
            isRotating = false;
            currentStep = STEP_ON_POSITION;
            stepStartTime = System.currentTimeMillis();
            status = (targetSide ? "YEŞİL" : "KIRMIZI") + " tarafta!";
            MuzMod.LOGGER.info("[OXState] Reached " + (targetSide ? "LIME" : "RED") + " side");
        } else {
            status = (targetSide ? "YEŞİL" : "KIRMIZI") + " tarafa gidiliyor...";
        }
    }
    
    private void handleOnPosition(EntityPlayerSP player, long now) {
        // Pozisyonda kal ve sürekli analiz et
        if (now - stepStartTime > 1000) {
            stepStartTime = now;
            analyzePlayerPositions(player);
            
            // Sadece diğer taraf FAZLA olunca yer değiştir (eşitlikte kalma)
            if (playersOnLime > 0 || playersOnRed > 0) {
                Boolean currentSide = getCurrentSide(player);
                
                if (currentSide != null) {
                    // Yeşildeysek, sadece kırmızı > yeşil olunca değiş
                    // Kırmızıdaysak, sadece yeşil > kırmızı olunca değiş
                    boolean shouldSwitch = false;
                    if (currentSide) { // Yeşildeyiz
                        shouldSwitch = playersOnRed > playersOnLime;
                    } else { // Kırmızıdayız
                        shouldSwitch = playersOnLime > playersOnRed;
                    }
                    
                    if (shouldSwitch) {
                        targetSide = !currentSide; // Diğer tarafa git
                        startSmoothRotation(player, targetSide);
                        currentStep = STEP_MOVING_TO_SIDE;
                        MuzMod.LOGGER.info("[OXState] Majority changed! Moving to " + (targetSide ? "LIME" : "RED") + 
                                          " (L:" + playersOnLime + " R:" + playersOnRed + ")");
                    }
                }
            }
        }
        
        status = "Pozisyonda (L:" + playersOnLime + " R:" + playersOnRed + ")";
    }
    
    /**
     * Etraftaki oyuncuların pozisyonlarını analiz et
     */
    private void analyzePlayerPositions(EntityPlayerSP player) {
        playersOnLime = 0;
        playersOnRed = 0;
        
        // 30 blok yarıçapındaki oyuncuları kontrol et
        List<EntityPlayer> nearbyPlayers = mc.theWorld.playerEntities;
        
        for (EntityPlayer other : nearbyPlayers) {
            if (other == player) continue;
            if (other.getDistanceToEntity(player) > 30) continue;
            
            // Oyuncunun altındaki bloğu kontrol et
            BlockPos feetPos = new BlockPos(other.posX, other.posY - 0.5, other.posZ);
            IBlockState blockState = mc.theWorld.getBlockState(feetPos);
            Block block = blockState.getBlock();
            
            if (block == Blocks.stained_hardened_clay) {
                EnumDyeColor color = blockState.getValue(BlockColored.COLOR);
                if (color == EnumDyeColor.LIME) {
                    playersOnLime++;
                } else if (color == EnumDyeColor.RED) {
                    playersOnRed++;
                }
            }
        }
        
        analysisCount++;
    }
    
    /**
     * Oyuncunun şu an hangi tarafta olduğunu döndür
     * null = ortada veya belirsiz
     */
    private Boolean getCurrentSide(EntityPlayerSP player) {
        BlockPos feetPos = new BlockPos(player.posX, player.posY - 0.5, player.posZ);
        IBlockState blockState = mc.theWorld.getBlockState(feetPos);
        Block block = blockState.getBlock();
        
        if (block == Blocks.stained_hardened_clay) {
            EnumDyeColor color = blockState.getValue(BlockColored.COLOR);
            if (color == EnumDyeColor.LIME) {
                return true;
            } else if (color == EnumDyeColor.RED) {
                return false;
            }
        }
        
        return null; // Ortada veya başka blokta
    }
    
    /**
     * Smooth rotation başlat - Config'den yön al
     */
    private void startSmoothRotation(EntityPlayerSP player, boolean goToLime) {
        // Config'den hedef yaw değerlerini al
        float limeYaw = MuzMod.instance.getConfig().getOxLimeYaw();  // Default: 90 (West)
        float redYaw = MuzMod.instance.getConfig().getOxRedYaw();    // Default: -90 (East)
        
        // Hedef yaw'ı belirle
        if (goToLime) {
            targetYaw = limeYaw;
        } else {
            targetYaw = redYaw;
        }
        
        MuzMod.LOGGER.info("[OXState] Rotating to " + (goToLime ? "LIME" : "RED") + " (yaw: " + targetYaw + ")");
        
        // Rastgele hız
        maxRotationSpeed = 5.0f + random.nextFloat() * 6.0f; // 5-11 derece/tick
        currentRotationSpeed = 0;
        isRotating = true;
    }
    
    /**
     * Smooth rotation uygula
     */
    private void applySmoothRotation(EntityPlayerSP player) {
        float currentYaw = player.rotationYaw;
        float diff = targetYaw - currentYaw;
        
        // Açı farkını normalize et
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        
        // Hedefe yaklaştıysak dur
        if (Math.abs(diff) < 2.0f) {
            isRotating = false;
            return;
        }
        
        // Hızlanma/yavaşlama
        if (Math.abs(diff) > 30) {
            // Uzaktaysa hızlan
            currentRotationSpeed = Math.min(currentRotationSpeed + 0.5f, maxRotationSpeed);
        } else {
            // Yaklaştıysa yavaşla
            currentRotationSpeed = Math.max(currentRotationSpeed - 0.3f, 2.0f);
        }
        
        // Dönüşü uygula
        float rotation = Math.signum(diff) * Math.min(Math.abs(diff), currentRotationSpeed);
        player.rotationYaw += rotation;
    }
    
    private void releaseAllKeys() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
    }
    
    // Getters/Setters
    public int getCurrentStep() {
        return currentStep;
    }
    
    public String getWarpCommand() {
        return warpCommand;
    }
    
    public void setWarpCommand(String warpCommand) {
        this.warpCommand = warpCommand;
    }
    
    public int getPlayersOnLime() {
        return playersOnLime;
    }
    
    public int getPlayersOnRed() {
        return playersOnRed;
    }
    
    /**
     * Event'i yeniden başlat
     */
    public void restart() {
        onDisable();
        onEnable();
    }
}
