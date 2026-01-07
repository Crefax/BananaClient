package com.muzmod.navigation;

import com.muzmod.MuzMod;
import com.muzmod.util.InputSimulator;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;

import java.util.List;
import java.util.Random;

/**
 * Navigation Manager v1.0
 * 
 * Merkezi hareket yönetim sistemi.
 * Tüm state'ler bu sınıfı kullanarak hareket komutları verebilir.
 * 
 * Kullanım örnekleri:
 * - nav.goForward(50)                    // 50 blok ileri git
 * - nav.goForward(30, 60)                // 30-60 blok arası ileri git
 * - nav.goTo(new BlockPos(100, 64, 200)) // Koordinata git
 * - nav.goDirection(Direction.EAST, 20)  // Doğuya 20 blok git
 * - nav.turnLeft(90)                     // 90 derece sola dön
 * - nav.stop()                           // Hareketi durdur
 */
public class NavigationManager {
    
    private static NavigationManager instance;
    
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();
    private final PathFinder pathFinder = new PathFinder();
    private final NavigationRenderer renderer = new NavigationRenderer();
    
    // Current movement task
    private MovementTask currentTask = null;
    private boolean isNavigating = false;
    
    // Path tracking
    private List<BlockPos> currentPath = null;
    private int currentPathIndex = 0;
    private BlockPos finalTarget = null;
    
    // Movement state
    private BlockPos startPos = null;
    private double targetDistance = 0;
    private float targetYaw = 0;
    private float targetPitch = 0;
    
    // Rotation state
    private boolean isRotating = false;
    private float rotationSpeed = 15.0f;
    
    // Callbacks
    private Runnable onComplete = null;
    private Runnable onFailed = null;
    
    // Stuck detection
    private long lastMoveTime = 0;
    private BlockPos lastPosition = null;
    private static final long STUCK_TIMEOUT = 3000; // 3 saniye hareket yoksa stuck
    
    private NavigationManager() {}
    
    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Verilen koordinattan aşağı doğru ilk yürünebilir bloğu bul.
     * Eğer hedef havadaysa, zemine indir.
     */
    private BlockPos findGroundLevel(BlockPos target) {
        if (mc.theWorld == null) return target;
        
        int x = target.getX();
        int z = target.getZ();
        int y = target.getY();
        
        // Önce bu Y seviyesinde yürünebilir mi kontrol et
        if (isWalkablePosition(target)) {
            return target;
        }
        
        // Havadaysa aşağı doğru in
        for (int checkY = y; checkY > 0; checkY--) {
            BlockPos checkPos = new BlockPos(x, checkY, z);
            if (isWalkablePosition(checkPos)) {
                MuzMod.LOGGER.info("[Navigation] Ground found at Y=" + checkY + " (was " + y + ")");
                return checkPos;
            }
        }
        
        // Aşağıda bulamadıysa, yukarı da bak
        for (int checkY = y; checkY < 256; checkY++) {
            BlockPos checkPos = new BlockPos(x, checkY, z);
            if (isWalkablePosition(checkPos)) {
                MuzMod.LOGGER.info("[Navigation] Ground found at Y=" + checkY + " (was " + y + ")");
                return checkPos;
            }
        }
        
        // Hiçbir şey bulunamadı, orijinal döndür
        return target;
    }
    
    /**
     * Bir pozisyonun yürünebilir olup olmadığını kontrol et.
     * Ayakların altında katı blok, baş ve gövde boş olmalı.
     */
    private boolean isWalkablePosition(BlockPos pos) {
        if (mc.theWorld == null) return false;
        
        BlockPos below = pos.down();
        BlockPos above = pos.up();
        
        // Altındaki blok katı olmalı
        boolean solidBelow = mc.theWorld.getBlockState(below).getBlock().isFullCube();
        // Ayak seviyesi boş olmalı
        boolean emptyFeet = !mc.theWorld.getBlockState(pos).getBlock().isFullCube();
        // Baş seviyesi boş olmalı
        boolean emptyHead = !mc.theWorld.getBlockState(above).getBlock().isFullCube();
        
        return solidBelow && emptyFeet && emptyHead;
    }
    
    // ==================== BASIC MOVEMENT COMMANDS ====================
    
    /**
     * İleri doğru belirli blok git
     */
    public NavigationManager goForward(int blocks) {
        return goForward(blocks, blocks);
    }
    
    /**
     * İleri doğru rastgele mesafe git (min-max arası)
     */
    public NavigationManager goForward(int minBlocks, int maxBlocks) {
        if (mc.thePlayer == null) return this;
        
        int distance = minBlocks + random.nextInt(Math.max(1, maxBlocks - minBlocks + 1));
        
        // Mevcut yöne doğru hedef hesapla
        double rad = Math.toRadians(mc.thePlayer.rotationYaw);
        double targetX = mc.thePlayer.posX - Math.sin(rad) * distance;
        double targetZ = mc.thePlayer.posZ + Math.cos(rad) * distance;
        
        BlockPos target = new BlockPos(targetX, mc.thePlayer.posY, targetZ);
        
        // Hedef havadaysa zemine indir
        target = findGroundLevel(target);
        
        return goTo(target);
    }
    
    /**
     * Belirli bir yöne doğru git
     */
    public NavigationManager goDirection(Direction direction, int blocks) {
        return goDirection(direction, blocks, blocks);
    }
    
    /**
     * Belirli bir yöne doğru rastgele mesafe git
     */
    public NavigationManager goDirection(Direction direction, int minBlocks, int maxBlocks) {
        if (mc.thePlayer == null) return this;
        
        int distance = minBlocks + random.nextInt(Math.max(1, maxBlocks - minBlocks + 1));
        
        BlockPos current = mc.thePlayer.getPosition();
        BlockPos target;
        
        switch (direction) {
            case NORTH:
                target = current.add(0, 0, -distance);
                break;
            case SOUTH:
                target = current.add(0, 0, distance);
                break;
            case EAST:
                target = current.add(distance, 0, 0);
                break;
            case WEST:
                target = current.add(-distance, 0, 0);
                break;
            case FORWARD:
                return goForward(minBlocks, maxBlocks);
            case BACKWARD:
                // Arkaya dön ve git
                return turnAround().goForward(minBlocks, maxBlocks);
            case LEFT:
                return turnLeft(90).goForward(minBlocks, maxBlocks);
            case RIGHT:
                return turnRight(90).goForward(minBlocks, maxBlocks);
            default:
                target = current;
        }
        
        // Hedef havadaysa zemine indir
        target = findGroundLevel(target);
        
        return goTo(target);
    }
    
    /**
     * Belirli koordinata git
     */
    public NavigationManager goTo(BlockPos target) {
        if (mc.thePlayer == null || target == null) return this;
        
        // Mevcut görevi iptal et
        stop();
        
        this.finalTarget = target;
        this.startPos = mc.thePlayer.getPosition();
        this.lastMoveTime = System.currentTimeMillis();
        this.lastPosition = startPos;
        
        // Pathfinding ile yol bul
        currentPath = pathFinder.findPath(startPos, target);
        currentPathIndex = 0;
        
        if (currentPath == null || currentPath.isEmpty()) {
            // Direkt yol yok, basit yaklaşım dene
            MuzMod.LOGGER.warn("[Navigation] Path not found to " + target + ", trying direct approach");
            currentPath = pathFinder.getSimplePath(startPos, target);
        }
        
        // Renderer'a yolu ver
        renderer.setPath(currentPath);
        renderer.setTarget(target);
        
        // Görevi başlat
        currentTask = new MovementTask(MovementTask.Type.GO_TO, target);
        isNavigating = true;
        
        MuzMod.LOGGER.info("[Navigation] Starting navigation to " + target + 
                          " (distance: " + getDistanceTo(target) + " blocks, path: " + 
                          (currentPath != null ? currentPath.size() : 0) + " waypoints)");
        
        return this;
    }
    
    /**
     * Koordinata git (x, y, z)
     */
    public NavigationManager goTo(double x, double y, double z) {
        return goTo(new BlockPos(x, y, z));
    }
    
    // ==================== ROTATION COMMANDS ====================
    
    /**
     * Sola dön (derece)
     */
    public NavigationManager turnLeft(float degrees) {
        if (mc.thePlayer == null) return this;
        targetYaw = mc.thePlayer.rotationYaw - degrees;
        isRotating = true;
        return this;
    }
    
    /**
     * Sağa dön (derece)
     */
    public NavigationManager turnRight(float degrees) {
        if (mc.thePlayer == null) return this;
        targetYaw = mc.thePlayer.rotationYaw + degrees;
        isRotating = true;
        return this;
    }
    
    /**
     * Arkaya dön (180 derece)
     */
    public NavigationManager turnAround() {
        return turnRight(180);
    }
    
    /**
     * Belirli yaw açısına dön
     */
    public NavigationManager rotateTo(float yaw) {
        targetYaw = yaw;
        isRotating = true;
        return this;
    }
    
    /**
     * Belirli yaw ve pitch açısına dön
     */
    public NavigationManager rotateTo(float yaw, float pitch) {
        targetYaw = yaw;
        targetPitch = pitch;
        isRotating = true;
        return this;
    }
    
    /**
     * Bir bloğa doğru dön
     */
    public NavigationManager lookAt(BlockPos target) {
        if (mc.thePlayer == null || target == null) return this;
        
        float[] rotations = getRotationsTo(target);
        return rotateTo(rotations[0], rotations[1]);
    }
    
    // ==================== CONTROL COMMANDS ====================
    
    /**
     * Hareketi durdur
     */
    public void stop() {
        isNavigating = false;
        isRotating = false;
        currentTask = null;
        currentPath = null;
        finalTarget = null;
        
        // Tuşları bırak
        InputSimulator.releaseAll();
        
        // Renderer'ı temizle
        renderer.clear();
    }
    
    /**
     * Callback ayarla - başarılı tamamlandığında
     */
    public NavigationManager onComplete(Runnable callback) {
        this.onComplete = callback;
        return this;
    }
    
    /**
     * Callback ayarla - başarısız olduğunda
     */
    public NavigationManager onFailed(Runnable callback) {
        this.onFailed = callback;
        return this;
    }
    
    /**
     * Dönüş hızını ayarla
     */
    public NavigationManager setRotationSpeed(float speed) {
        this.rotationSpeed = Math.max(1.0f, Math.min(50.0f, speed));
        return this;
    }
    
    // ==================== UPDATE (her tick çağrılmalı) ====================
    
    /**
     * Her tick çağrılmalı - hareket güncellemesi
     */
    public void update() {
        if (mc.thePlayer == null) return;
        
        // Rotasyon güncelle
        if (isRotating) {
            updateRotation();
        }
        
        // Navigasyon güncelle
        if (isNavigating && currentTask != null) {
            updateNavigation();
        }
        
        // Renderer güncelle
        renderer.update();
    }
    
    private void updateRotation() {
        float currentYaw = mc.thePlayer.rotationYaw;
        float currentPitch = mc.thePlayer.rotationPitch;
        
        // Yaw farkı (en kısa yol)
        float yawDiff = targetYaw - currentYaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        // Pitch farkı
        float pitchDiff = targetPitch - currentPitch;
        
        boolean yawDone = Math.abs(yawDiff) < 2.0f;
        boolean pitchDone = Math.abs(pitchDiff) < 2.0f;
        
        // Yaw smooth rotation
        if (!yawDone) {
            float step = Math.signum(yawDiff) * Math.min(Math.abs(yawDiff), rotationSpeed);
            mc.thePlayer.rotationYaw += step;
        }
        
        // Pitch smooth rotation
        if (!pitchDone) {
            float step = Math.signum(pitchDiff) * Math.min(Math.abs(pitchDiff), rotationSpeed * 0.7f);
            mc.thePlayer.rotationPitch += step;
        }
        
        // Dönüş tamamlandı mı
        if (yawDone && pitchDone) {
            isRotating = false;
        }
    }
    
    private void updateNavigation() {
        if (finalTarget == null) {
            completeNavigation(true);
            return;
        }
        
        // Hedefe ulaştık mı?
        double distanceToFinal = getDistanceTo(finalTarget);
        if (distanceToFinal < 2.0) {
            MuzMod.LOGGER.info("[Navigation] Reached destination: " + finalTarget);
            completeNavigation(true);
            return;
        }
        
        // Stuck kontrolü
        BlockPos currentPos = mc.thePlayer.getPosition();
        if (lastPosition != null && currentPos.equals(lastPosition)) {
            if (System.currentTimeMillis() - lastMoveTime > STUCK_TIMEOUT) {
                MuzMod.LOGGER.warn("[Navigation] Stuck detected, trying to recover...");
                handleStuck();
                return;
            }
        } else {
            lastMoveTime = System.currentTimeMillis();
            lastPosition = currentPos;
        }
        
        // Path varsa takip et
        if (currentPath != null && !currentPath.isEmpty()) {
            followPath();
        } else {
            // Direkt hedefe git
            moveTowards(finalTarget);
        }
    }
    
    private void followPath() {
        if (currentPathIndex >= currentPath.size()) {
            // Path tamamlandı, hedefe ulaştık mı kontrol et
            if (getDistanceTo(finalTarget) < 2.0) {
                completeNavigation(true);
            } else {
                // Yeni path hesapla
                currentPath = pathFinder.findPath(mc.thePlayer.getPosition(), finalTarget);
                currentPathIndex = 0;
                renderer.setPath(currentPath);
            }
            return;
        }
        
        BlockPos waypoint = currentPath.get(currentPathIndex);
        double distToWaypoint = getDistanceTo(waypoint);
        
        // Waypoint'e ulaştık mı?
        if (distToWaypoint < 1.5) {
            currentPathIndex++;
            renderer.setCurrentWaypointIndex(currentPathIndex);
            return;
        }
        
        // Waypoint'e doğru git
        moveTowards(waypoint);
    }
    
    private void moveTowards(BlockPos target) {
        // Hedefe dön
        float[] rotations = getRotationsTo(target);
        float targetYaw = rotations[0];
        
        float currentYaw = mc.thePlayer.rotationYaw;
        float yawDiff = targetYaw - currentYaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        // Smooth rotation
        if (Math.abs(yawDiff) > 5) {
            float step = Math.signum(yawDiff) * Math.min(Math.abs(yawDiff), rotationSpeed);
            mc.thePlayer.rotationYaw += step;
        }
        
        // Yeterince doğru bakıyorsak yürü
        if (Math.abs(yawDiff) < 30) {
            InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
            
            // Sprint kontrolü (uzun mesafede)
            if (getDistanceTo(finalTarget) > 10) {
                InputSimulator.holdKey(mc.gameSettings.keyBindSprint, true);
            }
        } else {
            // Henüz dönmedik, dur
            InputSimulator.releaseKey(mc.gameSettings.keyBindForward);
        }
        
        // Zıplama gerekiyor mu? (önünde blok var mı)
        if (shouldJump()) {
            InputSimulator.holdKey(mc.gameSettings.keyBindJump, true);
        } else {
            InputSimulator.releaseKey(mc.gameSettings.keyBindJump);
        }
    }
    
    private void handleStuck() {
        // Zıpla
        InputSimulator.holdKey(mc.gameSettings.keyBindJump, true);
        
        // Biraz sağa veya sola git
        if (random.nextBoolean()) {
            InputSimulator.holdKey(mc.gameSettings.keyBindLeft, true);
        } else {
            InputSimulator.holdKey(mc.gameSettings.keyBindRight, true);
        }
        
        // Yeni path hesapla
        lastMoveTime = System.currentTimeMillis();
        currentPath = pathFinder.findPath(mc.thePlayer.getPosition(), finalTarget);
        currentPathIndex = 0;
        renderer.setPath(currentPath);
    }
    
    private void completeNavigation(boolean success) {
        InputSimulator.releaseAll();
        isNavigating = false;
        
        if (success && onComplete != null) {
            onComplete.run();
        } else if (!success && onFailed != null) {
            onFailed.run();
        }
        
        // Temizle
        currentTask = null;
        onComplete = null;
        onFailed = null;
        renderer.clear();
    }
    
    // ==================== UTILITY METHODS ====================
    
    private float[] getRotationsTo(BlockPos target) {
        double dx = target.getX() + 0.5 - mc.thePlayer.posX;
        double dy = target.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = target.getZ() + 0.5 - mc.thePlayer.posZ;
        
        double dist = Math.sqrt(dx * dx + dz * dz);
        
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(Math.atan2(dy, dist) * 180.0 / Math.PI);
        
        return new float[]{yaw, pitch};
    }
    
    private double getDistanceTo(BlockPos target) {
        if (mc.thePlayer == null || target == null) return Double.MAX_VALUE;
        
        double dx = target.getX() + 0.5 - mc.thePlayer.posX;
        double dz = target.getZ() + 0.5 - mc.thePlayer.posZ;
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    private boolean shouldJump() {
        if (mc.thePlayer == null) return false;
        
        // Önümüzdeki bloğu kontrol et
        double rad = Math.toRadians(mc.thePlayer.rotationYaw);
        double checkX = mc.thePlayer.posX - Math.sin(rad) * 0.8;
        double checkZ = mc.thePlayer.posZ + Math.cos(rad) * 0.8;
        
        BlockPos ahead = new BlockPos(checkX, mc.thePlayer.posY, checkZ);
        BlockPos aboveAhead = ahead.up();
        
        // Önde blok var ve üstü boş mu?
        boolean blockAhead = !mc.theWorld.isAirBlock(ahead);
        boolean spaceAbove = mc.theWorld.isAirBlock(aboveAhead) && mc.theWorld.isAirBlock(aboveAhead.up());
        
        return blockAhead && spaceAbove;
    }
    
    // ==================== GETTERS ====================
    
    public boolean isNavigating() {
        return isNavigating;
    }
    
    public boolean isRotating() {
        return isRotating;
    }
    
    public boolean isBusy() {
        return isNavigating || isRotating;
    }
    
    public BlockPos getTarget() {
        return finalTarget;
    }
    
    public List<BlockPos> getCurrentPath() {
        return currentPath;
    }
    
    public NavigationRenderer getRenderer() {
        return renderer;
    }
    
    public double getDistanceToTarget() {
        return getDistanceTo(finalTarget);
    }
    
    public String getStatus() {
        if (!isNavigating && !isRotating) {
            return "Idle";
        }
        
        StringBuilder sb = new StringBuilder();
        
        if (isRotating) {
            sb.append("Rotating ");
        }
        
        if (isNavigating && finalTarget != null) {
            sb.append(String.format("Going to %d,%d,%d (%.1f blocks)", 
                finalTarget.getX(), finalTarget.getY(), finalTarget.getZ(),
                getDistanceTo(finalTarget)));
        }
        
        return sb.toString();
    }
}
