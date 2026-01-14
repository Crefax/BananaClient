package com.muzmod.state.impl;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
import com.muzmod.navigation.Direction;
import com.muzmod.navigation.NavigationManager;
import com.muzmod.rotation.PositionAdjuster;
import com.muzmod.state.AbstractState;
import com.muzmod.util.BlockScanner;
import com.muzmod.util.InputSimulator;
import com.muzmod.util.PlayerDetector;
import com.muzmod.util.RotationUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Mining State v1.6.0
 * 
 * Yeni özellikler:
 * - Modüler smooth rotation sistemi
 * - Ayarlanabilir min-max yaw/pitch açıları (0.1 - 100+ derece)
 * - Smooth rotation hızı ayarı
 * - Anti-AFK ve pozisyon ayarlama ayrı sınıflarda
 * - Strafe Anti-AFK (A-D tuşları ile sağ-sol hareket)
 * - Otomatik kazma tamiri (durability düşük olunca)
 */
public class MiningState extends AbstractState {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();
    
    // Modular rotation systems
    private final PositionAdjuster positionAdjuster = new PositionAdjuster();
    
    // State phases
    private MiningPhase phase = MiningPhase.WARPING;
    private long phaseStartTime = 0;
    
    // Reference ore position
    private BlockPos referenceOre = null;
    private float referenceYaw = 0;
    private float referencePitch = 30;
    
    // Mining tracking
    private long lastMiningProgressTime = 0;
    private static final long STUCK_TIMEOUT = 800; // 800ms - hızlı tespit
    
    // Movement - turnLeft and adjustmentCount moved to PositionAdjuster
    private boolean turnLeft = true;
    
    // Initial walk state
    private double targetWalkDistance = 0;
    private double walkedDistance = 0;
    private float walkYaw = 0;
    private BlockPos walkStartPos = null;
    private boolean rotationComplete = false; // Dönüş tamamlandı mı
    
    // Second walk state
    private double secondWalkDistance = 0;
    private double secondWalkedDistance = 0;
    private float secondWalkYaw = 0;
    private BlockPos secondWalkStartPos = null;
    private boolean secondWalkGoEast = false; // East mi west mi
    
    // Timing
    private long warpCooldown = 0;
    
    // Player avoidance
    private boolean playerInRadius = false;
    private boolean playerBlocking = false;
    private BlockPos safeSpot = null;
    private long playerCheckTime = 0;
    private long playerBlockingStartTime = 0; // Oyuncu ne zamandır engelliyor
    private float avoidanceTargetYaw = 0; // Smooth kaçış için hedef yaw
    private boolean isAvoidanceRotating = false; // Smooth dönüş yapılıyor mu
    private boolean avoidanceGoLeft = false; // Kaçış yönü: true=sol, false=sağ
    
    // Variable smooth rotation
    private float currentRotationSpeed = 7.0f; // Mevcut dönüş hızı (4-11 arası)
    private int rotationSpeedTickCounter = 0; // Hız değişim sayacı
    private static final int ROTATION_SPEED_CHANGE_INTERVAL = 5; // Her 5 tick'te bir hız değişir
    
    // Marked ores for rendering
    private Set<BlockPos> markedOres = new HashSet<>();
    
    // Strafe Anti-AFK
    private long lastStrafeTime = 0;
    private long strafeEndTime = 0;
    private boolean isStrafing = false;
    private boolean strafeLeft = true; // Sağ-sol değişimi için
    
    // Jitter Anti-AFK
    private long lastJitterTime = 0;
    
    // Grace period - süre bittiğinde hemen ADJUSTING'e geçmeden önce ore kontrolü
    private long gracePeriodStartTime = 0;
    private boolean inGracePeriod = false;
    private static final long GRACE_PERIOD_MS = 200; // 200ms grace period
    
    // Kazma kontrolü - her saniye kazma durumunu kontrol et
    private long lastMiningCheckTime = 0;
    private static final long MINING_CHECK_INTERVAL = 1000; // 1 saniye
    
    // Focus kontrolü - alt-tab sonrası aim bozulmasını önle
    private boolean hadFocus = true;
    private long focusRegainTime = 0;
    private static final long FOCUS_GRACE_PERIOD = 500; // 500ms grace period
    
    // Navigation system reference
    private final NavigationManager nav = NavigationManager.getInstance();
    private boolean usingNavigation = false; // NavigationManager kullanılıyor mu
    
    // Mining center - yürüyüş tamamlandıktan sonra burası merkez olur
    // Player avoidance ve ore bulunamadığında bu merkeze döneriz
    private BlockPos miningCenter = null;
    private long lastCenterCheckTime = 0;
    
    // Mining progress tracking (manuel durdurma algılama)
    private long lastActualMiningTime = 0; // Gerçekten kazıldığı son zaman
    private BlockPos lastMinedBlockPos = null; // Son kazılan bloğun pozisyonu
    private int lastBlockDamage = -1; // Son blok hasarı
    
    // Pause/Resume support (tamir için)
    private boolean isPaused = false;
    private MiningPhase pausedPhase = null;
    private BlockPos pausedReferenceOre = null;
    private float pausedReferenceYaw = 0;
    private float pausedReferencePitch = 30;
    
    // Constants
    private static final long WARP_DELAY = 3000;
    private static final float MIN_PITCH = 30;  // Minimum 30 derece aşağı bakmalı
    private static final float MAX_PITCH = 60;  // Maximum 60 derece aşağı
    
    public enum MiningPhase {
        WARPING,
        INITIAL_ROTATION,   // Önce dön
        INITIAL_WALK,       // Sonra yürü
        SECOND_ROTATION,    // İkinci yürüyüş öncesi dön
        SECOND_WALK,        // İkinci yürüyüş (east/west)
        FINDING_ORE,
        MINING,
        ADJUSTING,
        RELOCATING,
        FINDING_SAFE_SPOT
    }
    
    public MiningState() {
        this.status = "Maden bekleniyor";
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        // Eğer pause'dan dönüyorsak, eski state'i geri yükle
        if (isPaused && pausedPhase != null) {
            phase = pausedPhase;
            referenceOre = pausedReferenceOre;
            referenceYaw = pausedReferenceYaw;
            referencePitch = pausedReferencePitch;
            phaseStartTime = System.currentTimeMillis();
            lastMiningProgressTime = System.currentTimeMillis();
            lastMiningCheckTime = 0; // Hemen kazma kontrolü yapılsın
            
            // Pause state'i temizle
            isPaused = false;
            pausedPhase = null;
            pausedReferenceOre = null;
            
            MuzMod.LOGGER.info("[Mining] Pause'dan devam ediliyor, phase: " + phase);
            setStatus("Kazmaya devam ediliyor...");
            
            // Eğer MINING fazındaysak hemen kazmaya başla
            if (phase == MiningPhase.MINING) {
                InputSimulator.holdLeftClick(true);
                MuzMod.LOGGER.info("[Mining] Kazma yeniden başlatıldı (pause sonrası)");
            }
            return;
        }
        
        // Normal başlatış
        phase = MiningPhase.WARPING;
        phaseStartTime = System.currentTimeMillis();
        warpCooldown = System.currentTimeMillis() + 1000;
        referenceOre = null;
        walkStartPos = null;
        walkedDistance = 0;
        turnLeft = true;
        playerInRadius = false;
        playerBlocking = false;
        markedOres.clear();
        usingNavigation = false;
        miningCenter = null;
        
        // Stop any existing navigation
        nav.stop();
        
        // Reset rotation systems
        positionAdjuster.reset();
        
        // Setup ore found callback
        positionAdjuster.setOreFoundCallback((pos, yaw, pitch) -> {
            referenceOre = pos;
            referenceYaw = yaw;
            referencePitch = pitch;
            lastMiningProgressTime = System.currentTimeMillis();
            setPhase(MiningPhase.MINING);
            setStatus("Yeni ore bulundu!");
        });
        
        setStatus("Maden bölgesine gidiliyor...");
        InputSimulator.releaseAll();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        InputSimulator.releaseAll();
        referenceOre = null;
        markedOres.clear();
        phase = MiningPhase.WARPING;
        
        // Stop navigation
        nav.stop();
        usingNavigation = false;
        
        // Reset rotation systems
        positionAdjuster.reset();
    }
    
    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        ModConfig config = MuzMod.instance.getConfig();
        
        // === KRİTİK KAZMA KONTROLÜ ===
        // Elde kazma yoksa hotbar'da ara ve seç
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        boolean hasPickaxe = heldItem != null && heldItem.getItem() instanceof ItemPickaxe;
        
        if (!hasPickaxe) {
            // Hotbar'da kazma ara
            int pickaxeSlot = findPickaxeInHotbar();
            if (pickaxeSlot != -1) {
                mc.thePlayer.inventory.currentItem = pickaxeSlot;
                MuzMod.LOGGER.info("[Mining] Kazma seçildi, slot: " + pickaxeSlot);
                return; // Bir tick bekle
            }
        } else {
            // Kazma var, durability kontrol et
            int maxDurability = heldItem.getMaxDamage();
            int currentDamage = heldItem.getItemDamage();
            int remaining = maxDurability - currentDamage;
            int threshold = config.getRepairDurabilityThreshold();
            
            // KRİTİK SEVİYE: Threshold'un yarısından az kaldıysa
            // Güvenli moda geç - kazma ASLA kırılmasın
            if (remaining < threshold / 2 && remaining < 50) {
                InputSimulator.releaseAll();
                MuzMod.LOGGER.error("[Mining] KAZMA KRİTİK! Durability: " + remaining + " - Güvenli moda geçiliyor");
                MuzMod.instance.getStateManager().enterSafeMode(SafeState.SafeReason.PICKAXE_CRITICAL);
                return;
            }
        }
        
        // Handle phases
        switch (phase) {
            case WARPING:
                handleWarpingPhase(config);
                break;
            case INITIAL_ROTATION:
                handleInitialRotationPhase(config);
                break;
            case INITIAL_WALK:
                handleInitialWalkPhase(config);
                break;
            case SECOND_ROTATION:
                handleSecondRotationPhase(config);
                break;
            case SECOND_WALK:
                handleSecondWalkPhase(config);
                break;
            case FINDING_ORE:
                handleFindingOrePhase(config);
                break;
            case MINING:
                handleMiningPhase(config);
                break;
            case ADJUSTING:
                handleAdjustingPhase(config);
                break;
            case RELOCATING:
                handleRelocatingPhase(config);
                break;
            case FINDING_SAFE_SPOT:
                handleFindingSafeSpotPhase(config);
                break;
        }
        
        // Player detection - SADECE ore bulma ve mining fazlarında aktif
        // Yürüyüş ve dönüş fazlarında player kaçış devre dışı (path bozulmasın)
        boolean isWalkingPhase = phase == MiningPhase.WARPING || 
                                 phase == MiningPhase.INITIAL_ROTATION ||
                                 phase == MiningPhase.INITIAL_WALK ||
                                 phase == MiningPhase.SECOND_ROTATION ||
                                 phase == MiningPhase.SECOND_WALK;
        
        if (!isWalkingPhase && config.shouldAvoidPlayers()) {
            handlePlayerDetection(config);
        }
        
        // Strafe Anti-AFK (mining sırasında arada bir A-D yap)
        if (phase == MiningPhase.MINING && config.isStrafeEnabled()) {
            handleStrafeAntiAfk(config);
        }
    }
    
    private void handleWarpingPhase(ModConfig config) {
        if (System.currentTimeMillis() < warpCooldown) return;
        
        mc.thePlayer.sendChatMessage(config.getMiningWarpCommand());
        MuzMod.LOGGER.info("Sent mining warp command: " + config.getMiningWarpCommand());
        
        // Setup initial rotation first
        setPhase(MiningPhase.INITIAL_ROTATION);
        phaseStartTime = System.currentTimeMillis() + WARP_DELAY;
        walkStartPos = null;
        walkedDistance = 0;
        targetWalkDistance = config.getInitialWalkDistance();
        referenceOre = null;
        turnLeft = true;
        positionAdjuster.reset();
        rotationComplete = false;
        
        // South = 0 degrees, with small random variation
        double variation = config.getWalkYawVariation();
        walkYaw = (float) ((random.nextDouble() - 0.5) * variation * 2);
        
        // İkinci yürüyüş ayarları
        secondWalkStartPos = null;
        secondWalkedDistance = 0;
        if (config.isSecondWalkEnabled()) {
            int minDist = config.getSecondWalkDistanceMin();
            int maxDist = config.getSecondWalkDistanceMax();
            secondWalkDistance = minDist + random.nextInt(Math.max(1, maxDist - minDist + 1));
            
            // East (-90) veya West (90) seç
            secondWalkGoEast = config.isSecondWalkRandomDirection() ? random.nextBoolean() : false;
            float baseYaw = secondWalkGoEast ? -90.0f : 90.0f;
            float angleVar = config.getSecondWalkAngleVariation();
            secondWalkYaw = baseYaw + (float)((random.nextDouble() - 0.5) * angleVar * 2);
        }
        
        setStatus("Warp bekleniyor...");
    }
    
    private void handleInitialRotationPhase(ModConfig config) {
        // Wait for warp delay
        if (System.currentTimeMillis() < phaseStartTime) {
            return;
        }
        
        // Hedefe doğru dön
        float currentYaw = mc.thePlayer.rotationYaw;
        float diff = walkYaw - currentYaw;
        
        // Normalize angle to -180 to 180
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        
        // Dönüş tamamlandı mı kontrol et
        if (Math.abs(diff) < 3.0f) {
            mc.thePlayer.rotationYaw = walkYaw;
            rotationComplete = true;
            setPhase(MiningPhase.INITIAL_WALK);
            walkStartPos = null;
            usingNavigation = false; // Reset for walk phase
            setStatus("Yön ayarlandı, yürüyüş başlıyor...");
            MuzMod.LOGGER.info("[Mining] Initial rotation complete, yaw: " + walkYaw);
            return;
        }
        
        // Smooth rotate - hızlı dönüş (20-30 derece/tick)
        float rotSpeed = 25.0f;
        float rotation = Math.signum(diff) * Math.min(Math.abs(diff), rotSpeed);
        mc.thePlayer.rotationYaw += rotation;
        
        // Pitch'i de ayarla (hafif aşağı bak)
        smoothRotatePitchTo(20);
        
        setStatus("Yön ayarlanıyor...");
    }
    
    private void smoothRotatePitchTo(float targetPitch) {
        float currentPitch = mc.thePlayer.rotationPitch;
        float diff = targetPitch - currentPitch;
        
        if (Math.abs(diff) < 2.0f) {
            mc.thePlayer.rotationPitch = targetPitch;
            return;
        }
        
        float rotSpeed = 10.0f;
        float rotation = Math.signum(diff) * Math.min(Math.abs(diff), rotSpeed);
        mc.thePlayer.rotationPitch += rotation;
    }
    
    private void handleInitialWalkPhase(ModConfig config) {
        // NavigationManager ile yürüyüş başlat
        if (!usingNavigation) {
            InputSimulator.releaseAll();
            walkStartPos = mc.thePlayer.getPosition();
            
            // NavigationManager ile yön ve mesafe belirle
            nav.goForward((int)targetWalkDistance, (int)targetWalkDistance)
               .onComplete(() -> {
                   usingNavigation = false;
                   InputSimulator.releaseAll();
                   
                   // İkinci yürüyüş aktifse, önce dönüş yap
                   if (config.isSecondWalkEnabled() && secondWalkDistance > 0) {
                       setPhase(MiningPhase.SECOND_ROTATION);
                       rotationComplete = false;
                       String dir = secondWalkGoEast ? "East" : "West";
                       MuzMod.LOGGER.info("[Mining] Initial walk complete via NavManager, starting second rotation to " + dir);
                       setStatus("İkinci yön ayarlanıyor (" + dir + ")...");
                   } else {
                       // İkinci yürüyüş yoksa, burası mining merkezi
                       miningCenter = mc.thePlayer.getPosition();
                       MuzMod.LOGGER.info("[Mining] Mining center set at: " + miningCenter);
                       setPhase(MiningPhase.FINDING_ORE);
                       setStatus("Ore aranıyor...");
                   }
               })
               .onFailed(() -> {
                   usingNavigation = false;
                   InputSimulator.releaseAll();
                   MuzMod.LOGGER.warn("[Mining] Navigation failed, falling back to FINDING_ORE");
                   setPhase(MiningPhase.FINDING_ORE);
               });
            
            usingNavigation = true;
            setStatus("Ore'lara yürünüyor (NavManager)...");
            MuzMod.LOGGER.info("[Mining] Started initial walk via NavigationManager, target: " + targetWalkDistance + " blocks");
            return;
        }
        
        // NavigationManager aktifken mesafe hesapla ve göster
        double dx = mc.thePlayer.posX - walkStartPos.getX();
        double dz = mc.thePlayer.posZ - walkStartPos.getZ();
        walkedDistance = Math.sqrt(dx * dx + dz * dz);
        
        // Mine while walking if enabled
        if (config.shouldMineWhileMoving()) {
            BlockPos lookingAt = getLookingAtBlock();
            if (lookingAt != null) {
                Block block = mc.theWorld.getBlockState(lookingAt).getBlock();
                if (block == Blocks.quartz_ore) {
                    InputSimulator.holdLeftClick(true);
                    
                    // Found ore while walking - set as reference
                    if (referenceOre == null) {
                        referenceOre = lookingAt;
                        referenceYaw = mc.thePlayer.rotationYaw;
                        referencePitch = mc.thePlayer.rotationPitch;
                        MuzMod.LOGGER.info("Reference ore found while walking: " + referenceOre);
                    }
                } else {
                    InputSimulator.releaseLeftClick();
                }
            } else {
                InputSimulator.releaseLeftClick();
            }
        }
        
        setStatus(String.format("Yürünüyor... %.1f/%.0f blok", walkedDistance, targetWalkDistance));
    }
    
    private void handleSecondRotationPhase(ModConfig config) {
        // Hedefe doğru dön
        float currentYaw = mc.thePlayer.rotationYaw;
        float diff = secondWalkYaw - currentYaw;
        
        // Normalize angle to -180 to 180
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        
        // Dönüş tamamlandı mı kontrol et
        if (Math.abs(diff) < 3.0f) {
            mc.thePlayer.rotationYaw = secondWalkYaw;
            rotationComplete = true;
            setPhase(MiningPhase.SECOND_WALK);
            secondWalkStartPos = null;
            usingNavigation = false; // Reset for second walk phase
            String dir = secondWalkGoEast ? "East" : "West";
            setStatus("İkinci yürüyüş başlıyor (" + dir + ")...");
            MuzMod.LOGGER.info("[Mining] Second rotation complete, yaw: " + secondWalkYaw);
            return;
        }
        
        // Smooth rotate - hızlı dönüş (20-30 derece/tick)
        float rotSpeed = 25.0f;
        float rotation = Math.signum(diff) * Math.min(Math.abs(diff), rotSpeed);
        mc.thePlayer.rotationYaw += rotation;
        
        // Pitch'i de ayarla (hafif aşağı bak)
        smoothRotatePitchTo(20);
        
        String dir = secondWalkGoEast ? "East" : "West";
        setStatus("İkinci yön ayarlanıyor (" + dir + ")...");
    }
    
    private void handleSecondWalkPhase(ModConfig config) {
        // NavigationManager ile ikinci yürüyüş başlat
        if (!usingNavigation) {
            InputSimulator.releaseAll();
            secondWalkStartPos = mc.thePlayer.getPosition();
            
            // East veya West yönüne git
            Direction dir = secondWalkGoEast ? Direction.EAST : Direction.WEST;
            String dirName = secondWalkGoEast ? "East" : "West";
            
            nav.goDirection(dir, (int)secondWalkDistance)
               .onComplete(() -> {
                   usingNavigation = false;
                   InputSimulator.releaseAll();
                   
                   // Mining merkezi olarak bu pozisyonu kaydet
                   miningCenter = mc.thePlayer.getPosition();
                   MuzMod.LOGGER.info("[Mining] Mining center set at: " + miningCenter);
                   
                   setPhase(MiningPhase.FINDING_ORE);
                   setStatus("Ore aranıyor...");
                   MuzMod.LOGGER.info("[Mining] Second walk complete via NavManager, walked: " + secondWalkDistance + " blocks to " + dirName);
               })
               .onFailed(() -> {
                   usingNavigation = false;
                   InputSimulator.releaseAll();
                   MuzMod.LOGGER.warn("[Mining] Second walk navigation failed, falling back to FINDING_ORE");
                   setPhase(MiningPhase.FINDING_ORE);
               });
            
            usingNavigation = true;
            setStatus("İkinci yürüyüş (" + dirName + ") (NavManager)...");
            MuzMod.LOGGER.info("[Mining] Started second walk via NavigationManager to " + dirName + ", distance: " + secondWalkDistance);
            return;
        }
        
        // NavigationManager aktifken mesafe hesapla
        double dx = mc.thePlayer.posX - secondWalkStartPos.getX();
        double dz = mc.thePlayer.posZ - secondWalkStartPos.getZ();
        secondWalkedDistance = Math.sqrt(dx * dx + dz * dz);
        
        // Mine while walking if enabled
        if (config.shouldMineWhileMoving()) {
            BlockPos lookingAt = getLookingAtBlock();
            if (lookingAt != null) {
                Block block = mc.theWorld.getBlockState(lookingAt).getBlock();
                if (block == Blocks.quartz_ore) {
                    InputSimulator.holdLeftClick(true);
                    
                    // Found ore while walking - set as reference
                    if (referenceOre == null) {
                        referenceOre = lookingAt;
                        referenceYaw = mc.thePlayer.rotationYaw;
                        referencePitch = mc.thePlayer.rotationPitch;
                        MuzMod.LOGGER.info("Reference ore found while second walk: " + referenceOre);
                    }
                } else {
                    InputSimulator.releaseLeftClick();
                }
            } else {
                InputSimulator.releaseLeftClick();
            }
        }
        
        String dir = secondWalkGoEast ? "East" : "West";
        setStatus(String.format("İkinci yürüyüş (%s)... %.1f/%.0f blok", dir, secondWalkedDistance, secondWalkDistance));
    }
    
    private void handleFindingOrePhase(ModConfig config) {
        // Kazma kontrolü artık onTick'te yapılıyor
        
        // Scan for nearby ores
        scanForOres(config.getOreSearchRadius());
        
        if (!markedOres.isEmpty()) {
            // Find closest ore
            BlockPos closest = findClosestOre();
            if (closest != null) {
                referenceOre = closest;
                float[] rotations = RotationUtils.getRotationsToBlock(mc.thePlayer, referenceOre);
                referenceYaw = rotations[0];
                referencePitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, rotations[1]));
                
                lastMiningProgressTime = System.currentTimeMillis();
                setPhase(MiningPhase.MINING);
                setStatus("Ore bulundu! Kazılıyor...");
                MuzMod.LOGGER.info("Reference ore set: " + referenceOre);
            }
        } else {
            // No ore found - merkeze uzaklığı kontrol et
            if (miningCenter != null) {
                int maxDist = config.getMaxDistanceFromCenter();
                double distToCenter = mc.thePlayer.getPosition().distanceSq(miningCenter);
                if (distToCenter > maxDist * maxDist) {
                    // Merkezden çok uzaklaştık, geri dön
                    MuzMod.LOGGER.info("[Mining] Too far from center (" + Math.sqrt(distToCenter) + " blocks), returning...");
                    safeSpot = miningCenter;
                    setPhase(MiningPhase.RELOCATING);
                    setStatus("Merkeze dönülüyor...");
                    return;
                }
            }
            
            // Normal pozisyon ayarlama
            setPhase(MiningPhase.ADJUSTING);
            startPositionAdjustment(config);
            setStatus("Ore bulunamadı, pozisyon ayarlanıyor...");
        }
    }
    
    private void handleMiningPhase(ModConfig config) {
        // === HER TICK KAZMA TUŞU KONTROLÜ ===
        // Öncelikle her zaman sol tık basılı olsun
        InputSimulator.holdLeftClick(true);
        
        // Kazma durability kontrolü - düşükse tamire gönder
        if (RepairState.needsRepair(config)) {
            InputSimulator.releaseLeftClick();
            MuzMod.LOGGER.info("[Mining] Kazma durability düşük, tamire gönderiliyor");
            
            // Mevcut state'i kaydet (pause)
            pauseState();
            
            MuzMod.instance.getStateManager().forceState("repair");
            return;
        }
        
        // GUI açıksa bekle (GUI kapanınca devam edecek)
        if (mc.currentScreen != null) {
            setStatus("GUI açık, bekleniyor...");
            return;
        }
        
        // === FOCUS KONTROLÜ (Alt-tab sonrası aim bozulmasını önle) ===
        boolean currentFocus = mc.inGameHasFocus;
        if (!currentFocus) {
            hadFocus = false;
            // Focus yokken sadece kazma kontrolü yap, aim değiştirme
            setStatus("Focus yok, bekleniyor...");
        } else if (!hadFocus && currentFocus) {
            // Focus yeni kazanıldı - grace period başlat
            hadFocus = true;
            focusRegainTime = System.currentTimeMillis();
        }
        
        // Grace period içindeyse jitter ve pitch limit uygulama
        boolean inFocusGracePeriod = (System.currentTimeMillis() - focusRegainTime < FOCUS_GRACE_PERIOD);
        
        // === PERİYODİK KAZMA KONTROLÜ ===
        // Her 500ms kazma durumunu kontrol et, kazmıyorsa yeniden başlat
        long now = System.currentTimeMillis();
        if (now - lastMiningCheckTime >= 500) { // 500ms = daha sık kontrol
            lastMiningCheckTime = now;
            
            // Kazma işlemi var mı kontrol et
            if (!mc.thePlayer.isSwingInProgress) {
                // Kazmıyor! Sol tıkı release/press yap
                InputSimulator.releaseLeftClick();
                InputSimulator.holdLeftClick(true);
                MuzMod.LOGGER.info("[Mining] Kazma durdu, yeniden başlatıldı");
            }
        }
        
        // Pitch limit kontrolü (30-60 derece arası) - sadece sınır dışındaysa ve focus grace period dışındaysa düzelt
        enforcePitchLimits(inFocusGracePeriod);
        
        // Baktığımız bloğu kontrol et
        BlockPos lookingAt = getLookingAtBlock();
        
        if (lookingAt != null) {
            Block block = mc.theWorld.getBlockState(lookingAt).getBlock();
            
            if (block == Blocks.quartz_ore) {
                // === QUARTZ'A BAKIYORUZ ===
                // Quartz'a bakıyoruz = kazılıyor, aim değiştirme
                lastMiningProgressTime = System.currentTimeMillis();
                referenceOre = lookingAt;
                markedOres.clear();
                markedOres.add(lookingAt);
                setStatus("Kazılıyor...");
                
            } else if (block == Blocks.air) {
                // === AIR'E BAKIYORUZ ===
                // Ore respawn olacak, kazmaya devam et, aim değiştirme
                setStatus("Ore bekleniyor...");
                
            } else {
                // === FARKLI BLOĞA BAKIYORUZ ===
                // Timer say, ama aim değiştirme (jitter hariç)
                long timeSinceProgress = System.currentTimeMillis() - lastMiningProgressTime;
                long remaining = STUCK_TIMEOUT - timeSinceProgress;
                
                if (remaining <= 0) {
                    // Süre bitti - ADJUSTING'e geç
                    InputSimulator.releaseLeftClick();
                    setPhase(MiningPhase.ADJUSTING);
                    startPositionAdjustment(config);
                    setStatus("Takıldı, pozisyon ayarlanıyor...");
                } else {
                    setStatus(String.format("Bekleniyor... %.1fs", remaining / 1000.0));
                }
            }
        } else {
            // Hiçbir bloğa bakmıyoruz (çok uzak)
            long timeSinceProgress = System.currentTimeMillis() - lastMiningProgressTime;
            long remaining = STUCK_TIMEOUT - timeSinceProgress;
            
            if (remaining <= 0) {
                InputSimulator.releaseLeftClick();
                setPhase(MiningPhase.ADJUSTING);
                startPositionAdjustment(config);
                setStatus("Blok bulunamadı, pozisyon ayarlanıyor...");
            } else {
                setStatus(String.format("Blok aranıyor... %.1fs", remaining / 1000.0));
            }
        }
        
        // Jitter sistemi - sadece focus grace period dışında çalışsın
        if (!inFocusGracePeriod) {
            applyMiningJitter(config);
        }
    }
    
    /**
     * Pitch'i 30-60 derece arasında tut (focus grace period dışında)
     */
    private void enforcePitchLimits(boolean inFocusGracePeriod) {
        // Focus grace period içindeyse aim'i değiştirme
        if (inFocusGracePeriod) {
            return;
        }
        
        if (mc.thePlayer.rotationPitch < MIN_PITCH) {
            mc.thePlayer.rotationPitch = MIN_PITCH;
        } else if (mc.thePlayer.rotationPitch > MAX_PITCH) {
            mc.thePlayer.rotationPitch = MAX_PITCH;
        }
    }
    
    /**
     * Mining merkezine (veya reference ore'a) smooth dön
     */
    private void smoothLookAtCenter(ModConfig config) {
        if (referenceOre != null) {
            // Reference ore varsa ona dön
            float[] rotations = RotationUtils.getRotationsToBlock(mc.thePlayer, referenceOre);
            float targetYaw = rotations[0];
            float targetPitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, rotations[1]));
            RotationUtils.smoothLookAt(mc.thePlayer, targetYaw, targetPitch, 0.15f);
        } else if (miningCenter != null) {
            // Mining merkezi varsa ona dön
            float[] rotations = RotationUtils.getRotationsToBlock(mc.thePlayer, miningCenter);
            float targetYaw = rotations[0];
            float targetPitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, rotations[1]));
            RotationUtils.smoothLookAt(mc.thePlayer, targetYaw, targetPitch, 0.15f);
        }
    }
    
    /**
     * Yakında ore var mı kontrol et (ADJUSTING'e geçmeden önce)
     * Bu fonksiyon aim döndürmez, sadece kontrol eder
     */
    private boolean hasNearbyOre(int radius) {
        if (mc.thePlayer == null || mc.theWorld == null) return false;
        
        BlockPos playerPos = mc.thePlayer.getPosition();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    Block block = mc.theWorld.getBlockState(checkPos).getBlock();
                    if (block == Blocks.quartz_ore) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Mining sırasında jitter uygula (AFK bypass için)
     * Obsidyen ile aynı mantık - sadece interval'de bir kez uygula
     */
    private void applyMiningJitter(ModConfig config) {
        long now = System.currentTimeMillis();
        int jitterInterval = config.getMiningJitterInterval();
        
        // Sadece interval'de bir kez jitter uygula (her tick değil!)
        if (now - lastJitterTime >= jitterInterval) {
            lastJitterTime = now;
            float jitterYaw = config.getMiningJitterYaw();
            float jitterPitch = config.getMiningJitterPitch();
            
            // Yeni random jitter hesapla ve HEMEN uygula
            float yawChange = (random.nextFloat() - 0.5f) * 2 * jitterYaw;
            float pitchChange = (random.nextFloat() - 0.5f) * 2 * jitterPitch;
            
            // Jitter'ı bir kez uygula
            mc.thePlayer.rotationYaw += yawChange;
            mc.thePlayer.rotationPitch += pitchChange;
            
            // Pitch sınırlarını kontrol et
            mc.thePlayer.rotationPitch = Math.max(-90, Math.min(90, mc.thePlayer.rotationPitch));
        }
    }
    
    /**
     * Pozisyon ayarlama başlat
     * - Yeni modüler PositionAdjuster kullan
     * - Mining merkezi varsa önce merkeze doğru dön
     */
    private void startPositionAdjustment(ModConfig config) {
        positionAdjuster.startAdjustment(referenceYaw, referencePitch, config, miningCenter);
        MuzMod.LOGGER.info("Starting position adjustment #" + positionAdjuster.getAdjustmentCount() + 
            (miningCenter != null ? " (towards center)" : ""));
    }
    
    private void handleAdjustingPhase(ModConfig config) {
        PositionAdjuster.AdjustmentResult result = positionAdjuster.update(config);
        
        switch (result) {
            case ORE_FOUND:
                // Callback already handled setting phase to MINING
                break;
                
            case COMPLETED:
                // Adjustment done, update reference and try mining again
                referenceYaw = positionAdjuster.getTargetYaw();
                referencePitch = positionAdjuster.getTargetPitch();
                
                // Scan for ores at new position
                scanForOres(config.getOreSearchRadius());
                
                if (!markedOres.isEmpty()) {
                    BlockPos closest = findClosestOre();
                    if (closest != null) {
                        referenceOre = closest;
                        float[] rotations = RotationUtils.getRotationsToBlock(mc.thePlayer, referenceOre);
                        referenceYaw = rotations[0];
                        referencePitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, rotations[1]));
                    }
                }
                
                lastMiningProgressTime = System.currentTimeMillis();
                setPhase(MiningPhase.MINING);
                setStatus("Kazılıyor...");
                break;
                
            case IN_PROGRESS:
                setStatus("Pozisyon ayarlanıyor... (" + positionAdjuster.getTurnDirection() + ")");
                break;
                
            case NOT_ADJUSTING:
                // Should not happen, but go back to finding ore
                setPhase(MiningPhase.FINDING_ORE);
                break;
        }
    }
    
    private void handleRelocatingPhase(ModConfig config) {
        if (safeSpot == null || mc.thePlayer.getPosition().distanceSq(safeSpot) < 9) {
            InputSimulator.releaseAll();
            safeSpot = null;
            setPhase(MiningPhase.FINDING_ORE);
            setStatus("Yeni konumda ore aranıyor...");
            return;
        }
        
        // Walk towards safe spot - always smooth
        float[] rotations = RotationUtils.getRotationsToBlock(mc.thePlayer, safeSpot);
        float targetPitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, rotations[1]));
        smoothRotateTo(rotations[0], targetPitch);
        
        InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
        
        // Mine while moving if enabled
        if (config.shouldMineWhileMoving()) {
            BlockPos lookingAt = getLookingAtBlock();
            if (lookingAt != null) {
                Block block = mc.theWorld.getBlockState(lookingAt).getBlock();
                if (block == Blocks.quartz_ore) {
                    InputSimulator.holdLeftClick(true);
                } else {
                    InputSimulator.releaseLeftClick();
                }
            }
        }
        
        setStatus("Güvenli alana gidiliyor...");
    }
    
    private void handleFindingSafeSpotPhase(ModConfig config) {
        List<EntityPlayer> nearbyPlayers = PlayerDetector.getNearbyPlayers(
            mc.thePlayer, config.getPlayerDetectionRadius() * 2
        );
        
        if (nearbyPlayers.isEmpty()) {
            isAvoidanceRotating = false;
            InputSimulator.releaseKey(mc.gameSettings.keyBindForward);
            setPhase(MiningPhase.FINDING_ORE);
            setStatus("Oyuncu gitti, ore aranıyor...");
            return;
        }
        
        // Kaçış yönünü hesapla (henüz hesaplanmadıysa)
        if (!isAvoidanceRotating) {
            // Mining merkezi varsa, merkeze doğru kaçmayı tercih et
            float escapeYaw;
            if (miningCenter != null) {
                // Merkeze olan yönü hesapla
                float[] toCenter = RotationUtils.getRotationsToBlock(mc.thePlayer, miningCenter);
                float centerYaw = toCenter[0];
                
                // Oyuncudan kaçış yönü
                float playerEscapeYaw = PlayerDetector.getEscapeDirection(mc.thePlayer, config.getPlayerDetectionRadius());
                
                // Merkeze dönük yön ile kaçış yönü arasındaki farkı kontrol et
                float diffToCenter = centerYaw - playerEscapeYaw;
                while (diffToCenter > 180) diffToCenter -= 360;
                while (diffToCenter < -180) diffToCenter += 360;
                
                // Eğer kaçış yönü merkeze yakınsa (±90 derece içinde), onu kullan
                // Değilse, merkeze doğru git
                if (Math.abs(diffToCenter) < 90) {
                    escapeYaw = playerEscapeYaw;
                    MuzMod.LOGGER.info("[Mining] Kaçış yönü merkeze uygun, player escape kullanılıyor");
                } else {
                    // Merkeze doğru git, ama biraz sapma ekle
                    escapeYaw = centerYaw;
                    MuzMod.LOGGER.info("[Mining] Kaçış yönü merkeze doğru yönlendirildi");
                }
            } else {
                // Merkez yok, normal kaçış
                escapeYaw = PlayerDetector.getEscapeDirection(mc.thePlayer, config.getPlayerDetectionRadius());
            }
            
            // Rastgele sağ veya sol seç
            avoidanceGoLeft = random.nextBoolean();
            
            // Sağ veya sol yöne 30-60 derece sapma ekle
            float deviation = 30 + random.nextFloat() * 30; // 30-60 arası
            if (avoidanceGoLeft) {
                avoidanceTargetYaw = escapeYaw - deviation;
            } else {
                avoidanceTargetYaw = escapeYaw + deviation;
            }
            
            // Başlangıç hızını ayarla
            currentRotationSpeed = 4.0f + random.nextFloat() * 3.0f; // 4-7 arası başla
            rotationSpeedTickCounter = 0;
            
            isAvoidanceRotating = true;
            MuzMod.LOGGER.info("[Mining] Kaçış yönü belirlendi: " + avoidanceTargetYaw + " (" + (avoidanceGoLeft ? "sol" : "sağ") + ")");
        }
        
        // Değişken hız güncelle (her 5 tick'te bir)
        rotationSpeedTickCounter++;
        if (rotationSpeedTickCounter >= ROTATION_SPEED_CHANGE_INTERVAL) {
            rotationSpeedTickCounter = 0;
            // Hızı 4-11 arasında değiştir, ama ani değişim olmasın (±2 range)
            float change = (random.nextFloat() - 0.5f) * 4.0f; // -2 ile +2 arası
            currentRotationSpeed = Math.max(4.0f, Math.min(11.0f, currentRotationSpeed + change));
        }
        
        // Smooth rotation - dönerken yürü (en kısa yönden dön)
        float currentYaw = mc.thePlayer.rotationYaw;
        float diff = avoidanceTargetYaw - currentYaw;
        // Normalize to -180 to 180 for shortest path
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        
        if (Math.abs(diff) > 2) {
            // Hala dönmemiz gerekiyor - değişken hızla, en kısa yönden
            float step = Math.signum(diff) * Math.min(Math.abs(diff), currentRotationSpeed);
            mc.thePlayer.rotationYaw += step;
            
            // Dönerken ileri yürü
            InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
            
            setStatus(String.format("Kaçılıyor (%s)... %.0f°", avoidanceGoLeft ? "←" : "→", Math.abs(diff)));
        } else {
            // Dönüş tamamlandı - INSTANT YOK, smooth bitir
            // Yürümeye devam et
            InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
            
            // Safe spot'u şu anki yöne göre belirle
            double rad = Math.toRadians(mc.thePlayer.rotationYaw);
            int distance = 5 + random.nextInt(5);
            safeSpot = new BlockPos(
                mc.thePlayer.posX - Math.sin(rad) * distance,
                mc.thePlayer.posY,
                mc.thePlayer.posZ + Math.cos(rad) * distance
            );
            
            isAvoidanceRotating = false;
            setPhase(MiningPhase.RELOCATING);
            MuzMod.LOGGER.info("[Mining] Dönüş tamamlandı, güvenli alana gidiliyor");
        }
    }
    
    /**
     * Strafe Anti-AFK - Mining sırasında arada bir A veya D tuşuna basarak
     * sağa-sola hareket eder. AFK kick'i önler.
     */
    private void handleStrafeAntiAfk(ModConfig config) {
        long now = System.currentTimeMillis();
        
        // Eğer strafe yapıyorsak, bitene kadar devam
        if (isStrafing) {
            if (now >= strafeEndTime) {
                // Strafe bitti, tuşu bırak
                InputSimulator.releaseA();
                InputSimulator.releaseD();
                isStrafing = false;
                strafeLeft = !strafeLeft; // Bir sonraki için yön değiştir
                MuzMod.LOGGER.info("[Strafe] Strafe tamamlandı");
            }
            return;
        }
        
        // Strafe başlatma zamanı geldi mi?
        if (now - lastStrafeTime >= config.getStrafeInterval()) {
            lastStrafeTime = now;
            strafeEndTime = now + config.getStrafeDuration();
            isStrafing = true;
            
            // Sağ veya sol strafe
            if (strafeLeft) {
                InputSimulator.pressA();
                MuzMod.LOGGER.info("[Strafe] Sola strafe başladı");
            } else {
                InputSimulator.pressD();
                MuzMod.LOGGER.info("[Strafe] Sağa strafe başladı");
            }
        }
    }
    
    private void handlePlayerDetection(ModConfig config) {
        long now = System.currentTimeMillis();
        if (now - playerCheckTime < 500) return;
        playerCheckTime = now;
        
        List<EntityPlayer> nearbyPlayers = PlayerDetector.getNearbyPlayers(
            mc.thePlayer, config.getPlayerDetectionRadius()
        );
        
        playerInRadius = !nearbyPlayers.isEmpty();
        
        // Check if player is blocking mining
        boolean wasBlocking = playerBlocking;
        playerBlocking = false;
        
        if (playerInRadius) {
            for (EntityPlayer player : nearbyPlayers) {
                double dist = mc.thePlayer.getDistanceToEntity(player);
                if (dist < config.getPlayerBlockingDistance()) {
                    float[] rotToPlayer = RotationUtils.getRotationsToEntity(mc.thePlayer, player);
                    float yawDiff = Math.abs(RotationUtils.getAngleDistance(
                        mc.thePlayer.rotationYaw, rotToPlayer[0]
                    ));
                    
                    if (yawDiff < 45) {
                        playerBlocking = true;
                        break;
                    }
                }
            }
        }
        
        // Blocking başladıysa zamanı kaydet
        if (playerBlocking && !wasBlocking) {
            playerBlockingStartTime = now;
            MuzMod.LOGGER.info("[Mining] Oyuncu engelliyor, bekleme başladı");
        }
        
        // Decide action
        if (playerInRadius) {
            if (config.isInstantFlee()) {
                // Instant flee - hemen kaç
                InputSimulator.releaseLeftClick();
                setPhase(MiningPhase.FINDING_SAFE_SPOT);
                setStatus("Oyuncu tespit edildi!");
            } else if (playerBlocking && phase == MiningPhase.MINING) {
                // Timeout kontrolü - config'deki süre kadar bekle
                long blockingDuration = now - playerBlockingStartTime;
                long timeout = config.getPlayerBlockingTimeout();
                
                if (blockingDuration >= timeout) {
                    // Timeout doldu, kaçış moduna geç
                    InputSimulator.releaseLeftClick();
                    isAvoidanceRotating = false; // Reset - FINDING_SAFE_SPOT'ta yeniden hesaplanacak
                    setPhase(MiningPhase.FINDING_SAFE_SPOT);
                    setStatus("Oyuncu önde! Kaçılıyor...");
                    MuzMod.LOGGER.info("[Mining] Timeout doldu, kaçış başlıyor");
                } else {
                    // Hala bekliyoruz
                    long remaining = timeout - blockingDuration;
                    setStatus(String.format("Oyuncu önde... %.1fs", remaining / 1000.0));
                }
            }
        } else {
            // Oyuncu yok, blocking durumunu sıfırla
            playerBlockingStartTime = 0;
        }
    }
    
    /**
     * Mining progress kontrolü
     * Manuel sol tık ile durduruldu mu kontrol eder.
     * Eğer belirli süre boyunca hiç progress yoksa, kazmayı yeniden başlatır.
     */
    private void checkMiningProgress(ModConfig config) {
        long now = System.currentTimeMillis();
        long checkInterval = config.getMiningProgressCheckInterval();
        
        // Baktığımız blok değişti mi kontrol et
        BlockPos lookingAt = getLookingAtBlock();
        
        if (lookingAt != null) {
            Block block = mc.theWorld.getBlockState(lookingAt).getBlock();
            
            // Quartz ore'a bakıyorsak ve kazıyorsak
            if (block == Blocks.quartz_ore) {
                // Blok değişti mi?
                if (lastMinedBlockPos == null || !lastMinedBlockPos.equals(lookingAt)) {
                    lastMinedBlockPos = lookingAt;
                    lastActualMiningTime = now;
                    return;
                }
                
                // Aynı bloğa bakıyoruz - progress kontrolü
                // MC'de blok hasarı mc.playerController.curBlockDamageMP ile kontrol edilebilir
                // Ama bu private, bu yüzden zaman bazlı kontrol yapalım
                
                // Eğer checkInterval süresi boyunca aynı bloğa bakıyorsak
                // ve blok hala oradaysa, muhtemelen kazma durmuş
                if (now - lastActualMiningTime > checkInterval) {
                    // Kazmayı yeniden başlat
                    MuzMod.LOGGER.warn("[Mining] Kazma durmuş gibi görünüyor, yeniden başlatılıyor");
                    InputSimulator.releaseLeftClick();
                    
                    // Kısa bekle ve tekrar bas
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    InputSimulator.holdLeftClick(true);
                    
                    lastActualMiningTime = now;
                    setStatus("Kazma yeniden başlatıldı");
                }
            } else {
                // Farklı bloğa bakıyoruz
                lastMinedBlockPos = null;
            }
        } else {
            lastMinedBlockPos = null;
        }
    }
    
    private void scanForOres(int radius) {
        if (mc.thePlayer == null) return;
        
        BlockPos playerPos = mc.thePlayer.getPosition();
        Set<BlockPos> allOres = BlockScanner.findBlocks(
            mc.theWorld,
            playerPos,
            radius,
            Blocks.quartz_ore
        );
        
        // Filter: only keep ores with AIR above them
        markedOres.clear();
        for (BlockPos pos : allOres) {
            BlockPos above = pos.up();
            Block blockAbove = mc.theWorld.getBlockState(above).getBlock();
            if (blockAbove == Blocks.air) {
                markedOres.add(pos);
            }
        }
    }
    
    private BlockPos findClosestOre() {
        if (markedOres.isEmpty()) return null;
        
        ModConfig config = MuzMod.instance.getConfig();
        BlockPos playerPos = mc.thePlayer.getPosition();
        int maxDist = config.getMaxMoveDistance();
        
        double closestDist = Double.MAX_VALUE;
        BlockPos closest = null;
        
        for (BlockPos pos : markedOres) {
            double dist = Math.sqrt(playerPos.distanceSq(pos));
            if (dist <= maxDist && dist < closestDist) {
                closestDist = dist;
                closest = pos;
            }
        }
        
        return closest;
    }
    
    private BlockPos getLookingAtBlock() {
        if (mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null) {
            BlockPos pos = mc.objectMouseOver.getBlockPos();
            double dist = mc.thePlayer.getDistanceSq(pos);
            if (dist <= 25) { // Within 5 blocks
                return pos;
            }
        }
        return null;
    }
    
    /**
     * Smooth rotation helper - değişken hızla hedef açıya döner
     * Tüm instant rotasyonlar yerine bu kullanılmalı
     * En kısa yönden döner (360 derece tur atmaz)
     */
    private void smoothRotateTo(float targetYaw, float targetPitch) {
        float currentYaw = mc.thePlayer.rotationYaw;
        float currentPitch = mc.thePlayer.rotationPitch;
        
        // En kısa yönü hesapla (yönlü fark, -180 ile 180 arası)
        float yawDiff = targetYaw - currentYaw;
        // Normalize to -180 to 180 range for shortest path
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        float pitchDiff = targetPitch - currentPitch;
        
        // Değişken hız kullan (4-11 arası)
        float speed = currentRotationSpeed;
        
        // Yaw smooth - en kısa yönden dön
        if (Math.abs(yawDiff) > 1) {
            float step = Math.signum(yawDiff) * Math.min(Math.abs(yawDiff), speed);
            mc.thePlayer.rotationYaw += step;
        }
        
        // Pitch smooth (biraz daha yavaş)
        if (Math.abs(pitchDiff) > 1) {
            float step = Math.signum(pitchDiff) * Math.min(Math.abs(pitchDiff), speed * 0.7f);
            mc.thePlayer.rotationPitch += step;
        }
    }
    
    private void setPhase(MiningPhase newPhase) {
        this.phase = newPhase;
        this.phaseStartTime = System.currentTimeMillis();
        MuzMod.LOGGER.info("Mining phase changed to: " + newPhase);
    }
    
    @Override
    public String getName() {
        return "Mining";
    }
    
    @Override
    public boolean shouldActivate() {
        ModConfig config = MuzMod.instance.getConfig();
        return isTimeInRange(
            config.getMiningStartHour(),
            config.getMiningStartMinute(),
            config.getMiningEndHour(),
            config.getMiningEndMinute()
        );
    }
    
    @Override
    public int getPriority() {
        return 10;
    }
    
    /**
     * Hotbar'da kazma ara
     * @return Kazma slot indeksi (0-8) veya -1
     */
    private int findPickaxeInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemPickaxe) {
                return i;
            }
        }
        return -1;
    }
    
    // Public getters for rendering
    public Set<BlockPos> getMarkedOres() {
        return new HashSet<>(markedOres);
    }
    
    public BlockPos getCurrentTarget() {
        return referenceOre;
    }
    
    public MiningPhase getPhase() {
        return phase;
    }
    
    public boolean isPlayerInRadius() {
        return playerInRadius;
    }
    
    public boolean isPlayerBlocking() {
        return playerBlocking;
    }
    
    public BlockPos getReferenceOrePos() {
        return referenceOre;
    }
    
    public int getAdjustmentCount() {
        return positionAdjuster.getAdjustmentCount();
    }
    
    /**
     * Get the position adjuster for debugging/rendering
     */
    public PositionAdjuster getPositionAdjuster() {
        return positionAdjuster;
    }
    
    /**
     * Pause the current mining state (tamir için)
     * State bilgileri kaydedilir ve resume'da geri yüklenir
     */
    public void pauseState() {
        isPaused = true;
        pausedPhase = phase;
        pausedReferenceOre = referenceOre;
        pausedReferenceYaw = referenceYaw;
        pausedReferencePitch = referencePitch;
        
        MuzMod.LOGGER.info("[Mining] State paused - Phase: " + phase + ", Ore: " + referenceOre);
    }
    
    /**
     * Check if mining is paused
     */
    public boolean isPaused() {
        return isPaused;
    }
}
