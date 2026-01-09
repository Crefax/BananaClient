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
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * Obsidian Mining State v0.6.4
 * 
 * Basit ve etkili obsidyen kazma sistemi.
 * 
 * Çalışma mantığı:
 * - Bot Y=5'te durur (obsidyenlerin üstünde)
 * - Önündeki Y=4 seviyesindeki obsidyenleri kazarak ilerler
 * - Hedef: max obsidyen sayısı - (0-10 arası rastgele) mesafe
 * - Hedefe ulaşınca sağ/sol kontrol edip çok olan tarafa döner
 * - Y düşerse yukarı doğru kazarak çıkar
 */
public class ObsidianState extends AbstractState {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();
    
    // Başlangıç Y pozisyonu - görev başladığında oyuncunun bulunduğu Y seviyesi
    private int startY = -1;
    
    // Phase
    private Phase phase = Phase.INIT;
    
    // Hedefler
    private BlockPos redTarget = null;    // Mevcut hedef (KIRMIZI)
    private BlockPos yellowTarget = null; // Sonraki hedef (SARI)
    private BlockPos miningBlock = null;  // Şu an kazılan blok (YEŞİL)
    
    // Yön (4 yön: 0=Güney, 1=Batı, 2=Kuzey, 3=Doğu)
    private int direction = 0;
    private static final int[] DX = {0, -1, 0, 1};  // Güney, Batı, Kuzey, Doğu
    private static final int[] DZ = {1, 0, -1, 0};
    private static final float[] YAWS = {0, 90, 180, -90}; // Minecraft yaw değerleri
    
    // Durum
    private String debugInfo = "";
    private long miningStartTime = 0;
    private BlockPos lastMiningBlock = null;
    
    // Smooth aim
    private float targetPitch = 30;
    private float targetYaw = 0;
    private boolean isTurning = false; // Dönüş modunda mı?
    
    // Random aim jitter (doğal görünüm için) - Config'den okunuyor
    private long lastJitterTime = 0;
    
    // Kazma kontrolü - focus kaybedince kazma durabilir, periyodik kontrol
    private long lastMiningCheckTime = 0;
    private static final long MINING_CHECK_INTERVAL = 1000; // 1 saniye
    
    // Focus kontrolü - alt-tab sonrası aim bozulmasını önle
    private boolean hadFocus = true;
    private long focusRegainTime = 0;
    private static final long FOCUS_GRACE_PERIOD = 500; // 500ms grace period
    
    // Envanter kontrolü
    private long lastInventoryCheck = 0;
    private static final long INVENTORY_CHECK_INTERVAL = 2000; // 2 saniyede bir kontrol
    private boolean waitingForCevir = false; // Sell komutu bekleniyor mu
    private long cevirCommandTime = 0;
    // CEVIR_WAIT_TIME artık config'den okunuyor: config.getObsidianSellDelay()
    
    private enum Phase {
        INIT,
        FIND_TARGET,
        MINING,
        TURNING,
        DONE
    }
    
    public ObsidianState() {
        this.status = "Obsidian ready";
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        phase = Phase.INIT;
        redTarget = null;
        yellowTarget = null;
        miningBlock = null;
        direction = 0;
        miningStartTime = 0;
        lastMiningBlock = null;
        // Başlangıç Y pozisyonunu kaydet
        if (mc.thePlayer != null) {
            startY = (int) Math.floor(mc.thePlayer.posY) - 1; // Obsidyen seviyesi (oyuncu 1 blok yukarıda)
            MuzMod.LOGGER.info("[Obsidian] Start Y set to: " + startY);
        }
        MinecraftForge.EVENT_BUS.register(this);
        MuzMod.LOGGER.info("[Obsidian] v0.6.23 enabled");
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        InputSimulator.releaseAll();
        MinecraftForge.EVENT_BUS.unregister(this);
        redTarget = null;
        yellowTarget = null;
        miningBlock = null;
        startY = -1; // Reset start Y
        MuzMod.LOGGER.info("[Obsidian] disabled");
    }
    
    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        ModConfig config = MuzMod.instance.getConfig();
        
        // Kazma durability kontrolü - düşükse tamire gönder
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
        
        // Envanter kontrolü (Mining sırasında) - Config'den açık/kapalı kontrolü
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
        
        // Y kontrolü kaldırıldı - oyuncu başladığı yerde kalacak
        // Zıplama ve climb işlemleri iptal edildi
        
        // Smooth aim uygula
        smoothAim();
        
        switch (phase) {
            case INIT:
                doInit();
                break;
            case FIND_TARGET:
                doFindTarget();
                break;
            case MINING:
                doMining();
                break;
            case TURNING:
                doTurning();
                break;
            case DONE:
                InputSimulator.releaseAll();
                setStatus("Done!");
                break;
        }
    }
    
    /**
     * Smooth aim - yaw ve pitch'i yumuşak şekilde hedefe doğru hareket ettirir
     * NOT: Minecraft focus'u yoksa veya focus yeni kazanıldıysa aim değiştirme (alt-tab desteği)
     */
    private void smoothAim() {
        // Focus kontrolü - alt-tab sonrası aim bozulmasını önle
        boolean currentFocus = mc.inGameHasFocus;
        
        if (!currentFocus) {
            // Focus yok - aim değiştirme, flag'i güncelle
            hadFocus = false;
            return;
        }
        
        // Focus yeni kazanıldı mı?
        if (!hadFocus && currentFocus) {
            // Focus geri geldi - grace period başlat
            hadFocus = true;
            focusRegainTime = System.currentTimeMillis();
            return;
        }
        
        // Grace period içindeyse aim değiştirme (Minecraft'ın mouse grab bug'u için)
        if (System.currentTimeMillis() - focusRegainTime < FOCUS_GRACE_PERIOD) {
            return;
        }
        
        // Config'den aim hızlarını al
        float aimSpeed = MuzMod.instance.getConfig().getObsidianAimSpeed();
        float turnSpeed = MuzMod.instance.getConfig().getObsidianTurnSpeed();
        
        // Dönüş modunda daha hızlı, normal modda yavaş
        float speed = isTurning ? turnSpeed : aimSpeed;
        
        // Yaw smooth
        float yawDiff = targetYaw - mc.thePlayer.rotationYaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        if (Math.abs(yawDiff) > 0.5f) {
            mc.thePlayer.rotationYaw += yawDiff * speed;
        } else {
            mc.thePlayer.rotationYaw = targetYaw;
            // Dönüş bitti
            if (isTurning) {
                isTurning = false;
            }
        }
        
        // Pitch smooth
        float pitchDiff = targetPitch - mc.thePlayer.rotationPitch;
        if (Math.abs(pitchDiff) > 0.5f) {
            mc.thePlayer.rotationPitch += pitchDiff * speed;
        } else {
            mc.thePlayer.rotationPitch = targetPitch;
        }
        
        // Mining sırasında random jitter ekle (doğal görünüm) - Config'den değerler
        // Sadece interval'de bir kez uygula
        if (phase == Phase.MINING && !isTurning) {
            long now = System.currentTimeMillis();
            int jitterInterval = MuzMod.instance.getConfig().getObsidianJitterInterval();
            
            if (now - lastJitterTime >= jitterInterval) {
                lastJitterTime = now;
                float jitterYaw = MuzMod.instance.getConfig().getObsidianJitterYaw();
                float jitterPitch = MuzMod.instance.getConfig().getObsidianJitterPitch();
                
                // Yeni random jitter hesapla ve HEMEN uygula
                float yawChange = (random.nextFloat() - 0.5f) * 2 * jitterYaw;
                float pitchChange = (random.nextFloat() - 0.5f) * 2 * jitterPitch;
                
                mc.thePlayer.rotationYaw += yawChange;
                mc.thePlayer.rotationPitch += pitchChange;
            }
        } else {
            lastJitterTime = 0;
        }
    }
    
    // ===================== PHASE HANDLERS =====================
    
    private void doInit() {
        debugInfo = "Initializing...";
        setStatus(debugInfo);
        
        // startY henüz ayarlanmadıysa şimdi ayarla
        if (startY == -1) {
            startY = (int) Math.floor(mc.thePlayer.posY) - 1;
            MuzMod.LOGGER.info("[Obsidian] Start Y set to: " + startY);
        }
        
        // Y kontrolü kaldırıldı - oyuncu başladığı yerde kalacak
        
        // Baktığı yöne göre direction belirle
        float yaw = mc.thePlayer.rotationYaw;
        direction = getDirectionFromYaw(yaw);
        
        // Smooth aim hedeflerini ayarla
        targetYaw = YAWS[direction];
        targetPitch = 30;
        
        MuzMod.LOGGER.info("[Obsidian] Init - direction=" + direction + " yaw=" + YAWS[direction]);
        
        phase = Phase.FIND_TARGET;
    }
    
    private void doFindTarget() {
        debugInfo = "Finding target...";
        setStatus(debugInfo);
        
        // Smooth aim hedefini ayarla
        targetYaw = YAWS[direction];
        targetPitch = 30;
        
        int playerX = (int) Math.floor(mc.thePlayer.posX);
        int playerZ = (int) Math.floor(mc.thePlayer.posZ);
        
        // İleri yönde kaç obsidyen var?
        int count = countObsidianForward(playerX, playerZ);
        MuzMod.LOGGER.info("[Obsidian] Forward count: " + count);
        
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
                targetYaw = YAWS[direction]; // Smooth turn
                MuzMod.LOGGER.info("[Obsidian] Turned to direction=" + direction);
                return; // Tekrar kontrol et
            } else {
                MuzMod.LOGGER.info("[Obsidian] No obsidian found, done");
                phase = Phase.DONE;
                return;
            }
        }
        
        // Hedef mesafesi: max - (0-10 arası rastgele)
        int maxDist = count;
        int minDist = Math.max(2, maxDist - 10);
        int targetDist = minDist + random.nextInt(maxDist - minDist + 1);
        
        // Kırmızı hedef belirle
        redTarget = new BlockPos(
            playerX + DX[direction] * targetDist,
            startY,
            playerZ + DZ[direction] * targetDist
        );
        
        MuzMod.LOGGER.info("[Obsidian] Red target: " + redTarget + " dist=" + targetDist);
        
        // Sarı hedef (sonraki dönüş için önizleme)
        calculateYellowTarget();
        
        phase = Phase.MINING;
    }
    
    private void doMining() {
        if (redTarget == null) {
            phase = Phase.FIND_TARGET;
            return;
        }
        
        // === KAZMA KONTROLÜ ===
        // Her saniye kazma durumunu kontrol et, kazmıyorsa yeniden başlat
        // (Alt-tab sonrası kazma durabilir)
        long now = System.currentTimeMillis();
        if (now - lastMiningCheckTime >= MINING_CHECK_INTERVAL) {
            lastMiningCheckTime = now;
            
            // Kazma işlemi var mı kontrol et
            if (!mc.thePlayer.isSwingInProgress) {
                // Kazmıyor! Sol tıkı yeniden başlat
                InputSimulator.releaseKey(mc.gameSettings.keyBindAttack);
                InputSimulator.holdKey(mc.gameSettings.keyBindAttack, true);
            }
        }
        
        int playerX = (int) Math.floor(mc.thePlayer.posX);
        int playerZ = (int) Math.floor(mc.thePlayer.posZ);
        
        // Hedefe mesafe
        double dist = Math.sqrt(
            Math.pow(playerX - redTarget.getX(), 2) +
            Math.pow(playerZ - redTarget.getZ(), 2)
        );
        
        debugInfo = String.format("Mining dist=%.1f", dist);
        setStatus(debugInfo);
        
        // Hedefe ulaştık mı?
        if (dist < 1.5) {
            MuzMod.LOGGER.info("[Obsidian] Reached target!");
            InputSimulator.releaseAll();
            redTarget = yellowTarget;
            yellowTarget = null;
            phase = Phase.TURNING;
            return;
        }
        
        // Yön smooth olarak ayarla (sadece yaw, pitch değil)
        targetYaw = YAWS[direction];
        
        // ÖNCE: Mevcut aim zaten bir obsidyene bakıyor mu kontrol et
        BlockPos lookingAt = getObsidianLookingAt();
        
        if (lookingAt != null) {
            // Mevcut aim geçerli - aim değiştirme, sadece kaz!
            miningBlock = lookingAt;
            InputSimulator.holdKey(mc.gameSettings.keyBindAttack, true);
            
            // Stuck detection
            if (lastMiningBlock != null && lastMiningBlock.equals(lookingAt)) {
                if (System.currentTimeMillis() - miningStartTime > 5000) {
                    MuzMod.LOGGER.info("[Obsidian] Stuck on block, moving forward");
                    InputSimulator.releaseKey(mc.gameSettings.keyBindAttack);
                    InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
                    // Zıplama kaldırıldı - sadece ileri git
                    miningStartTime = System.currentTimeMillis();
                }
            } else {
                lastMiningBlock = lookingAt;
                miningStartTime = System.currentTimeMillis();
            }
        } else {
            // Mevcut aim geçersiz - yeni hedef bul
            BlockPos toMine = findObsidianToMine(playerX, playerZ);
            
            if (toMine != null) {
                miningBlock = toMine;
                
                // Bloğa bak - smooth pitch ve yaw hesapla
                double dy = toMine.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
                double dx = toMine.getX() + 0.5 - mc.thePlayer.posX;
                double dz = toMine.getZ() + 0.5 - mc.thePlayer.posZ;
                double horizontalDist = Math.sqrt(dx * dx + dz * dz);
                
                // Pitch hesapla
                targetPitch = (float) Math.toDegrees(Math.atan2(-dy, horizontalDist));
                targetPitch = Math.max(-90, Math.min(90, targetPitch));
                
                // Yaw hesapla (bloğa doğru)
                targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                
                // Kaz
                InputSimulator.holdKey(mc.gameSettings.keyBindAttack, true);
                
                // Stuck detection
                if (lastMiningBlock != null && lastMiningBlock.equals(toMine)) {
                    if (System.currentTimeMillis() - miningStartTime > 5000) {
                        MuzMod.LOGGER.info("[Obsidian] Stuck on block, moving forward");
                        InputSimulator.releaseKey(mc.gameSettings.keyBindAttack);
                        InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
                        // Zıplama kaldırıldı - sadece ileri git
                        miningStartTime = System.currentTimeMillis();
                    }
                } else {
                    lastMiningBlock = toMine;
                    miningStartTime = System.currentTimeMillis();
                }
            } else {
                miningBlock = null;
                InputSimulator.releaseKey(mc.gameSettings.keyBindAttack);
                
                // Düz ileri bak
                targetPitch = 30; // Hafif aşağı
            }
        }
        
        // İleri git
        InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
    }
    
    private void doTurning() {
        debugInfo = "Turning...";
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
        
        // Hızlı turn modu aktif et
        isTurning = true;
        targetYaw = YAWS[direction];
        targetPitch = 30;
        phase = Phase.FIND_TARGET;
    }
    
    // ===================== HELPER METHODS =====================
    
    private int getDirectionFromYaw(float yaw) {
        // Yaw'ı 0-360 arasına normalize et
        while (yaw < 0) yaw += 360;
        while (yaw >= 360) yaw -= 360;
        
        // 4 yöne yuvarla
        if (yaw >= 315 || yaw < 45) return 0;   // Güney (0)
        if (yaw >= 45 && yaw < 135) return 1;   // Batı (90)
        if (yaw >= 135 && yaw < 225) return 2;  // Kuzey (180)
        return 3;  // Doğu (270/-90)
    }
    
    private int turnLeft(int dir) {
        return (dir + 3) % 4;  // Sola dön
    }
    
    private int turnRight(int dir) {
        return (dir + 1) % 4;  // Sağa dön
    }
    
    private int countObsidianForward(int startX, int startZ) {
        return countObsidianInDir(startX, startZ, direction);
    }
    
    private int countObsidianInDir(int startX, int startZ, int dir) {
        int count = 0;
        for (int i = 1; i <= 100; i++) {
            int x = startX + DX[dir] * i;
            int z = startZ + DZ[dir] * i;
            BlockPos pos = new BlockPos(x, startY, z);
            if (mc.theWorld.getBlockState(pos).getBlock() == Blocks.obsidian) {
                count++;
            }
        }
        return count;
    }
    
    private BlockPos findObsidianToMine(int playerX, int playerZ) {
        // Önce direkt önündeki bloklara bak
        for (int dist = 0; dist <= 3; dist++) {
            int x = playerX + DX[direction] * dist;
            int z = playerZ + DZ[direction] * dist;
            BlockPos pos = new BlockPos(x, startY, z);
            if (mc.theWorld.getBlockState(pos).getBlock() == Blocks.obsidian) {
                return pos;
            }
        }
        return null;
    }
    
    /**
     * Mevcut aim'in baktığı obsidyen bloğunu bul (Y=4 seviyesinde)
     * Raycast benzeri kontrol - player'ın baktığı yönde obsidyen var mı?
     */
    private BlockPos getObsidianLookingAt() {
        if (mc.thePlayer == null) return null;
        
        double eyeX = mc.thePlayer.posX;
        double eyeY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
        double eyeZ = mc.thePlayer.posZ;
        
        float yaw = mc.thePlayer.rotationYaw;
        float pitch = mc.thePlayer.rotationPitch;
        
        // Bakış yönü vektörü
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        
        double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dirY = -Math.sin(pitchRad);
        double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);
        
        // Başlangıç Y seviyesine kaç adımda ulaşırız?
        double targetY = startY + 0.5; // Bloğun ortası
        if (dirY == 0) return null; // Yatay bakıyorsa hedef Y'ye hiç ulaşamaz
        
        double t = (targetY - eyeY) / dirY;
        if (t < 0 || t > 10) return null; // Arkada veya çok uzakta
        
        // Hedef noktayı hesapla
        double hitX = eyeX + dirX * t;
        double hitZ = eyeZ + dirZ * t;
        
        // Bu koordinatta obsidyen var mı?
        BlockPos checkPos = new BlockPos((int) Math.floor(hitX), startY, (int) Math.floor(hitZ));
        if (mc.theWorld.getBlockState(checkPos).getBlock() == Blocks.obsidian) {
            return checkPos;
        }
        
        return null;
    }
    
    private void calculateYellowTarget() {
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
            int maxDist = nextCount;
            int minDist = Math.max(2, maxDist - 10);
            int targetDist = minDist + random.nextInt(maxDist - minDist + 1);
            
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
    
    // ===================== RENDERING =====================
    
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!enabled || mc.thePlayer == null) return;
        
        // partialTicks ile interpolasyon yaparak doğru pozisyon hesapla
        float partialTicks = event.partialTicks;
        double interpX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double interpY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks;
        double interpZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;
        
        if (redTarget != null) {
            renderBlock(redTarget, 255, 0, 0, 150, interpX, interpY, interpZ);  // KIRMIZI
        }
        if (yellowTarget != null) {
            renderBlock(yellowTarget, 255, 255, 0, 100, interpX, interpY, interpZ);  // SARI
        }
        
        // Yeşil blok için Minecraft'ın gerçek bakılan bloğunu kullan
        if (mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null) {
            BlockPos actualLookingAt = mc.objectMouseOver.getBlockPos();
            if (mc.theWorld.getBlockState(actualLookingAt).getBlock() == Blocks.obsidian) {
                renderBlock(actualLookingAt, 0, 255, 0, 120, interpX, interpY, interpZ);  // YEŞİL
            }
        } else if (miningBlock != null) {
            renderBlock(miningBlock, 0, 255, 0, 120, interpX, interpY, interpZ);  // YEŞİL
        }
    }
    
    private void renderBlock(BlockPos pos, int r, int g, int b, int a, double interpX, double interpY, double interpZ) {
        double x = pos.getX() - interpX;
        double y = pos.getY() - interpY;
        double z = pos.getZ() - interpZ;
        
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        
        // Kutu çiz
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        
        // Alt
        wr.pos(x, y, z).color(r, g, b, a).endVertex();
        wr.pos(x+1, y, z).color(r, g, b, a).endVertex();
        wr.pos(x+1, y, z+1).color(r, g, b, a).endVertex();
        wr.pos(x, y, z+1).color(r, g, b, a).endVertex();
        
        // Üst
        wr.pos(x, y+1, z).color(r, g, b, a).endVertex();
        wr.pos(x, y+1, z+1).color(r, g, b, a).endVertex();
        wr.pos(x+1, y+1, z+1).color(r, g, b, a).endVertex();
        wr.pos(x+1, y+1, z).color(r, g, b, a).endVertex();
        
        // Ön
        wr.pos(x, y, z).color(r, g, b, a).endVertex();
        wr.pos(x, y+1, z).color(r, g, b, a).endVertex();
        wr.pos(x+1, y+1, z).color(r, g, b, a).endVertex();
        wr.pos(x+1, y, z).color(r, g, b, a).endVertex();
        
        // Arka
        wr.pos(x, y, z+1).color(r, g, b, a).endVertex();
        wr.pos(x+1, y, z+1).color(r, g, b, a).endVertex();
        wr.pos(x+1, y+1, z+1).color(r, g, b, a).endVertex();
        wr.pos(x, y+1, z+1).color(r, g, b, a).endVertex();
        
        // Sol
        wr.pos(x, y, z).color(r, g, b, a).endVertex();
        wr.pos(x, y, z+1).color(r, g, b, a).endVertex();
        wr.pos(x, y+1, z+1).color(r, g, b, a).endVertex();
        wr.pos(x, y+1, z).color(r, g, b, a).endVertex();
        
        // Sağ
        wr.pos(x+1, y, z).color(r, g, b, a).endVertex();
        wr.pos(x+1, y+1, z).color(r, g, b, a).endVertex();
        wr.pos(x+1, y+1, z+1).color(r, g, b, a).endVertex();
        wr.pos(x+1, y, z+1).color(r, g, b, a).endVertex();
        
        tess.draw();
        
        // Çerçeve
        GL11.glLineWidth(2.0f);
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x, y, z).color(r, g, b, 255).endVertex();
        wr.pos(x+1, y, z).color(r, g, b, 255).endVertex();
        wr.pos(x+1, y, z+1).color(r, g, b, 255).endVertex();
        wr.pos(x, y, z+1).color(r, g, b, 255).endVertex();
        wr.pos(x, y, z).color(r, g, b, 255).endVertex();
        tess.draw();
        
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x, y+1, z).color(r, g, b, 255).endVertex();
        wr.pos(x+1, y+1, z).color(r, g, b, 255).endVertex();
        wr.pos(x+1, y+1, z+1).color(r, g, b, 255).endVertex();
        wr.pos(x, y+1, z+1).color(r, g, b, 255).endVertex();
        wr.pos(x, y+1, z).color(r, g, b, 255).endVertex();
        tess.draw();
        
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
    
    // ===================== INVENTORY CHECK =====================
    
    /**
     * Envanterin tam dolu olup olmadığını kontrol eder
     * Slot 0-35 arası (36 slot) kontrol edilir
     */
    private boolean isInventoryFull() {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) return false;
        
        // Main inventory: 9-35 (27 slot)
        // Hotbar: 0-8 (9 slot)
        for (int i = 0; i < 36; i++) {
            if (mc.thePlayer.inventory.getStackInSlot(i) == null) {
                return false; // Boş slot var
            }
        }
        return true; // Tüm slotlar dolu
    }
    
    // ===================== INTERFACE =====================
    
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
        return 5;
    }
}
