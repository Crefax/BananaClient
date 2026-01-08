package com.muzmod.config;

import com.muzmod.MuzMod;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class ModConfig {
    
    private final Configuration config;
    
    // Time Settings
    private int timeOffsetHours = 1; // Saat offset (varsayılan +1 saat)
    
    // Mining Settings
    private String miningWarpCommand = "/warp maden";
    private int miningStartHour = 4;
    private int miningStartMinute = 30;
    private int miningEndHour = 4;
    private int miningEndMinute = 40;
    private int miningRadius = 3;
    private double miningLookPitch = 45.0;
    private double antiAfkRotationRange = 10.0;
    private long antiAfkInterval = 3000;
    
    // Pathfinding Settings
    private int oreSearchRadius = 50; // Chunk arama yarıçapı
    private int initialWalkDistanceMin = 40; // İlk yürüme mesafesi minimum
    private int initialWalkDistanceMax = 50; // İlk yürüme mesafesi maximum
    private int initialWalkDistance = 45; // Legacy - artık min-max kullanılıyor
    private double walkYawVariation = 10.0; // Yürürken yaw varyasyonu (sol-sağ)
    private boolean enablePathfinding = true;
    private int maxMoveDistance = 15; // Ore'a giderken max mesafe
    
    // Mining Center Settings
    private int maxDistanceFromCenter = 30; // Merkezden maksimum uzaklık
    
    // İkinci yürüyüş ayarları (east/west)
    private boolean secondWalkEnabled = true; // İkinci yürüyüş aktif mi
    private int secondWalkDistanceMin = 10; // Minimum ikinci yürüyüş mesafesi
    private int secondWalkDistanceMax = 30; // Maximum ikinci yürüyüş mesafesi
    private float secondWalkAngleVariation = 20.0f; // East/West'e +/- açı varyasyonu
    private boolean secondWalkRandomDirection = true; // Rastgele east/west seçimi
    
    // Anti-AFK Rotation Settings (mining sırasında hafif dönüşler)
    private float antiAfkYawMin = 0.5f;   // Minimum yaw dönüş (derece)
    private float antiAfkYawMax = 3.0f;   // Maximum yaw dönüş (derece)
    private float antiAfkPitchMin = 0.2f; // Minimum pitch dönüş (derece)
    private float antiAfkPitchMax = 1.5f; // Maximum pitch dönüş (derece)
    private long antiAfkSmoothSpeed = 500; // Smooth hızı (ms)
    
    // Position Adjustment Settings (takılınca pozisyon ayarlama)
    private float adjustYawMin = 5.0f;    // Minimum yaw ayar açısı
    private float adjustYawMax = 25.0f;   // Maximum yaw ayar açısı
    private float adjustPitchMin = 3.0f;  // Minimum pitch ayar açısı
    private float adjustPitchMax = 15.0f; // Maximum pitch ayar açısı
    private long adjustSmoothSpeed = 800; // Adjust smooth hızı (ms)
    private boolean enableBlockLock = false; // Bloğa kilitlenme (smooth look)
    
    // AFK Settings
    private String afkWarpCommand = "/warp afk";
    private int afkStartHour = 4;
    private int afkStartMinute = 40;
    private int afkEndHour = 5;
    private int afkEndMinute = 0;
    
    // Player Detection
    private double playerDetectionRadius = 3.0;
    private boolean avoidPlayers = true;
    private boolean instantFlee = false; // Hemen kaç mı yoksa kontrol et mi
    private boolean mineWhileMoving = true; // Hareket ederken de kaz
    private double playerBlockingDistance = 2.0; // Önünde oyuncu var mı mesafesi
    private long playerBlockingTimeout = 2000; // Oyuncu engelledikten sonra kaç ms bekle (varsayılan 2sn)
    private boolean smoothPlayerAvoidance = true; // Oyuncudan kaçarken smooth dönüş
    private long miningProgressCheckInterval = 3000; // Kazma progress kontrolü (3sn)
    
    // General
    private boolean showOverlay = true;
    private boolean debugMode = false;
    
    // Anti-AFK Strafe (sağ-sol hareket)
    private long strafeInterval = 30000; // 30 saniye
    private long strafeDuration = 150;   // 150ms basılı tutma
    private boolean strafeEnabled = true;
    
    // Repair Settings
    private int repairDurabilityThreshold = 1000; // Bu değerin altında tamir
    private String repairCommand = "/tamirci";
    private int repairSlot = 21; // 0-indexed, 22. slot
    private int repairMaxRetries = 3;
    private float repairClickDelay = 2.0f; // Tıklamalar arası bekleme (saniye)
    
    // OX Event Settings
    // Minecraft yaw: 0=South, 90=West, 180/-180=North, -90/270=East
    private float oxLimeYaw = 90.0f;  // West (Batı) - Yeşil taraf
    private float oxRedYaw = -90.0f;  // East (Doğu) - Kırmızı taraf
    private int oxMinPlayers = 5;     // Minimum oyuncu sayısı
    
    public ModConfig(File configFile) {
        config = new Configuration(configFile);
    }
    
    public void load() {
        try {
            config.load();
            
            // Time Category
            timeOffsetHours = config.getInt("timeOffsetHours", "time", 1, -12, 12,
                "Time offset in hours (to fix clock difference)");
            
            // Mining Category
            miningWarpCommand = config.getString("warpCommand", "mining", "/warp maden", 
                "Command to teleport to mining area");
            miningStartHour = config.getInt("startHour", "mining", 4, 0, 23, 
                "Hour to start mining (24h format)");
            miningStartMinute = config.getInt("startMinute", "mining", 30, 0, 59, 
                "Minute to start mining");
            miningEndHour = config.getInt("endHour", "mining", 4, 0, 23, 
                "Hour to end mining (24h format)");
            miningEndMinute = config.getInt("endMinute", "mining", 40, 0, 59, 
                "Minute to end mining");
            miningRadius = config.getInt("radius", "mining", 3, 1, 10, 
                "Radius to search for ores");
            miningLookPitch = config.getFloat("lookPitch", "mining", 45.0f, 0.0f, 90.0f, 
                "Pitch angle when mining (degrees down)");
            antiAfkRotationRange = config.getFloat("antiAfkRotation", "mining", 10.0f, 1.0f, 45.0f, 
                "Random rotation range for anti-AFK (degrees)");
            antiAfkInterval = config.getInt("antiAfkInterval", "mining", 3000, 1000, 10000, 
                "Interval between anti-AFK movements (ms)");
            
            // Pathfinding Category
            oreSearchRadius = config.getInt("oreSearchRadius", "pathfinding", 50, 16, 128,
                "Radius to search for ores (blocks)");
            initialWalkDistanceMin = config.getInt("initialWalkDistanceMin", "pathfinding", 40, 5, 100,
                "Minimum distance to walk south when searching for ores");
            initialWalkDistanceMax = config.getInt("initialWalkDistanceMax", "pathfinding", 50, 5, 100,
                "Maximum distance to walk south when searching for ores");
            initialWalkDistance = initialWalkDistanceMin; // Legacy compatibility
            walkYawVariation = config.getFloat("walkYawVariation", "pathfinding", 10.0f, 0.0f, 45.0f,
                "Random yaw variation when walking (degrees left-right)");
            enablePathfinding = config.getBoolean("enablePathfinding", "pathfinding", true,
                "Enable pathfinding to avoid obstacles");
            maxMoveDistance = config.getInt("maxMoveDistance", "pathfinding", 15, 5, 50,
                "Maximum distance to walk towards an ore");
            maxDistanceFromCenter = config.getInt("maxDistanceFromCenter", "pathfinding", 30, 10, 100,
                "Maximum distance from mining center before returning");
            
            // Second Walk Category (ikinci yürüyüş - east/west)
            secondWalkEnabled = config.getBoolean("secondWalkEnabled", "secondwalk", true,
                "Enable second walk after initial walk (east/west direction)");
            secondWalkDistanceMin = config.getInt("secondWalkDistanceMin", "secondwalk", 10, 1, 100,
                "Minimum distance for second walk");
            secondWalkDistanceMax = config.getInt("secondWalkDistanceMax", "secondwalk", 30, 1, 100,
                "Maximum distance for second walk");
            secondWalkAngleVariation = config.getFloat("secondWalkAngleVariation", "secondwalk", 20.0f, 0.0f, 45.0f,
                "Angle variation from east/west direction (degrees)");
            secondWalkRandomDirection = config.getBoolean("secondWalkRandomDirection", "secondwalk", true,
                "Randomly choose between east and west (false = always west)");
            
            // Anti-AFK Rotation Category (kazarken hafif dönüşler)
            antiAfkYawMin = config.getFloat("antiAfkYawMin", "antiafk", 0.5f, 0.1f, 100.0f,
                "Minimum yaw rotation for anti-AFK (degrees)");
            antiAfkYawMax = config.getFloat("antiAfkYawMax", "antiafk", 3.0f, 0.1f, 100.0f,
                "Maximum yaw rotation for anti-AFK (degrees)");
            antiAfkPitchMin = config.getFloat("antiAfkPitchMin", "antiafk", 0.2f, 0.1f, 50.0f,
                "Minimum pitch rotation for anti-AFK (degrees)");
            antiAfkPitchMax = config.getFloat("antiAfkPitchMax", "antiafk", 1.5f, 0.1f, 50.0f,
                "Maximum pitch rotation for anti-AFK (degrees)");
            antiAfkSmoothSpeed = config.getInt("antiAfkSmoothSpeed", "antiafk", 500, 50, 3000,
                "Smooth rotation duration for anti-AFK (ms)");
            
            // Position Adjustment Category (takılınca pozisyon değiştirme)
            adjustYawMin = config.getFloat("adjustYawMin", "adjustment", 5.0f, 0.1f, 100.0f,
                "Minimum yaw rotation when adjusting position (degrees)");
            adjustYawMax = config.getFloat("adjustYawMax", "adjustment", 25.0f, 0.1f, 100.0f,
                "Maximum yaw rotation when adjusting position (degrees)");
            adjustPitchMin = config.getFloat("adjustPitchMin", "adjustment", 3.0f, 0.1f, 50.0f,
                "Minimum pitch change when adjusting position (degrees)");
            adjustPitchMax = config.getFloat("adjustPitchMax", "adjustment", 15.0f, 0.1f, 50.0f,
                "Maximum pitch change when adjusting position (degrees)");
            adjustSmoothSpeed = config.getInt("adjustSmoothSpeed", "adjustment", 800, 50, 3000,
                "Smooth rotation duration when adjusting (ms)");
            enableBlockLock = config.getBoolean("enableBlockLock", "adjustment", false,
                "Enable smooth look/lock to block (can interfere with position adjustment)");
            
            // AFK Category
            afkWarpCommand = config.getString("warpCommand", "afk", "/warp afk", 
                "Command to teleport to AFK area");
            afkStartHour = config.getInt("startHour", "afk", 4, 0, 23, 
                "Hour to start AFK (24h format)");
            afkStartMinute = config.getInt("startMinute", "afk", 40, 0, 59, 
                "Minute to start AFK");
            afkEndHour = config.getInt("endHour", "afk", 5, 0, 23, 
                "Hour to end AFK (24h format)");
            afkEndMinute = config.getInt("endMinute", "afk", 0, 0, 59, 
                "Minute to end AFK");
            
            // Player Detection Category
            playerDetectionRadius = config.getFloat("detectionRadius", "players", 3.0f, 1.0f, 20.0f, 
                "Radius to detect nearby players");
            avoidPlayers = config.getBoolean("avoidPlayers", "players", true, 
                "Move away when players are nearby");
            instantFlee = config.getBoolean("instantFlee", "players", false,
                "Instantly flee when player enters radius (false = smart check)");
            mineWhileMoving = config.getBoolean("mineWhileMoving", "players", true,
                "Continue mining while moving away from players");
            playerBlockingDistance = config.getFloat("playerBlockingDistance", "players", 2.0f, 1.0f, 5.0f,
                "Distance to check if player is blocking mining");
            playerBlockingTimeout = config.getInt("playerBlockingTimeout", "players", 2000, 500, 10000,
                "Time to wait before turning away when player blocks (ms)");
            smoothPlayerAvoidance = config.getBoolean("smoothPlayerAvoidance", "players", true,
                "Use smooth rotation when avoiding players");
            miningProgressCheckInterval = config.getInt("miningProgressCheckInterval", "players", 3000, 1000, 10000,
                "Interval to check if mining is actually happening (ms)");
            
            // General Category
            showOverlay = config.getBoolean("showOverlay", "general", true, 
                "Show status overlay on screen");
            debugMode = config.getBoolean("debugMode", "general", false, 
                "Enable debug logging");
            
            // Anti-AFK Strafe Category
            strafeEnabled = config.getBoolean("strafeEnabled", "strafe", true,
                "Enable A-D strafe movement for anti-AFK");
            strafeInterval = config.getInt("strafeInterval", "strafe", 30000, 5000, 120000,
                "Interval between strafe movements (ms)");
            strafeDuration = config.getInt("strafeDuration", "strafe", 150, 50, 500,
                "Duration to hold strafe key (ms)");
            
            // Repair Category
            repairDurabilityThreshold = config.getInt("durabilityThreshold", "repair", 1000, 1, 2000,
                "Repair pickaxe when durability falls below this value");
            repairCommand = config.getString("repairCommand", "repair", "/tamirci",
                "Command to open repair GUI");
            repairSlot = config.getInt("repairSlot", "repair", 21, 0, 53,
                "Slot index for repair anvil (0-indexed)");
            repairMaxRetries = config.getInt("maxRetries", "repair", 3, 1, 10,
                "Maximum repair attempts before giving up");
            repairClickDelay = (float) config.getFloat("clickDelay", "repair", 2.0f, 0.5f, 10.0f,
                "Delay between clicks in repair GUI (seconds)");
            
            // OX Event Category
            oxLimeYaw = config.getFloat("limeYaw", "ox", 90.0f, -180.0f, 180.0f,
                "Yaw direction for LIME side (default: 90 = West)");
            oxRedYaw = config.getFloat("redYaw", "ox", -90.0f, -180.0f, 180.0f,
                "Yaw direction for RED side (default: -90 = East)");
            oxMinPlayers = config.getInt("minPlayers", "ox", 5, 1, 50,
                "Minimum players required to start OX event");
            
        } catch (Exception e) {
            MuzMod.LOGGER.error("Error loading config", e);
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }
    
    public void save() {
        config.save();
    }
    
    // Time Getters/Setters
    public int getTimeOffsetHours() { return timeOffsetHours; }
    public void setTimeOffsetHours(int hours) {
        this.timeOffsetHours = hours;
        config.get("time", "timeOffsetHours", 1).set(hours);
        save();
    }
    
    // Mining Getters
    public String getMiningWarpCommand() { return miningWarpCommand; }
    public int getMiningStartHour() { return miningStartHour; }
    public int getMiningStartMinute() { return miningStartMinute; }
    public int getMiningEndHour() { return miningEndHour; }
    public int getMiningEndMinute() { return miningEndMinute; }
    public int getMiningRadius() { return miningRadius; }
    public double getMiningLookPitch() { return miningLookPitch; }
    public double getAntiAfkRotationRange() { return antiAfkRotationRange; }
    public long getAntiAfkInterval() { return antiAfkInterval; }
    
    // Pathfinding Getters
    public int getOreSearchRadius() { return oreSearchRadius; }
    public int getInitialWalkDistance() { 
        // Random between min and max
        return initialWalkDistanceMin + (int)(Math.random() * (initialWalkDistanceMax - initialWalkDistanceMin + 1));
    }
    public int getInitialWalkDistanceMin() { return initialWalkDistanceMin; }
    public int getInitialWalkDistanceMax() { return initialWalkDistanceMax; }
    public double getWalkYawVariation() { return walkYawVariation; }
    public boolean isPathfindingEnabled() { return enablePathfinding; }
    public int getMaxMoveDistance() { return maxMoveDistance; }
    public int getMaxDistanceFromCenter() { return maxDistanceFromCenter; }
    
    // Pathfinding Setters
    public void setInitialWalkDistanceMin(int min) {
        this.initialWalkDistanceMin = min;
        config.get("pathfinding", "initialWalkDistanceMin", 40).set(min);
        save();
    }
    public void setInitialWalkDistanceMax(int max) {
        this.initialWalkDistanceMax = max;
        config.get("pathfinding", "initialWalkDistanceMax", 50).set(max);
        save();
    }
    public void setMaxDistanceFromCenter(int dist) {
        this.maxDistanceFromCenter = dist;
        config.get("pathfinding", "maxDistanceFromCenter", 30).set(dist);
        save();
    }
    public void setWalkYawVariation(double variation) {
        this.walkYawVariation = variation;
        config.get("pathfinding", "walkYawVariation", 10.0f).set((float)variation);
        save();
    }
    
    // Anti-AFK Rotation Getters
    public float getAntiAfkYawMin() { return antiAfkYawMin; }
    public float getAntiAfkYawMax() { return antiAfkYawMax; }
    public float getAntiAfkPitchMin() { return antiAfkPitchMin; }
    public float getAntiAfkPitchMax() { return antiAfkPitchMax; }
    public long getAntiAfkSmoothSpeed() { return antiAfkSmoothSpeed; }
    
    // Position Adjustment Getters
    public float getAdjustYawMin() { return adjustYawMin; }
    public float getAdjustYawMax() { return adjustYawMax; }
    public float getAdjustPitchMin() { return adjustPitchMin; }
    public float getAdjustPitchMax() { return adjustPitchMax; }
    public long getAdjustSmoothSpeed() { return adjustSmoothSpeed; }
    public boolean isBlockLockEnabled() { return enableBlockLock; }
    
    // Second Walk Getters
    public boolean isSecondWalkEnabled() { return secondWalkEnabled; }
    public int getSecondWalkDistanceMin() { return secondWalkDistanceMin; }
    public int getSecondWalkDistanceMax() { return secondWalkDistanceMax; }
    public float getSecondWalkAngleVariation() { return secondWalkAngleVariation; }
    public boolean isSecondWalkRandomDirection() { return secondWalkRandomDirection; }
    
    // Second Walk Setters
    public void setSecondWalkEnabled(boolean enabled) {
        this.secondWalkEnabled = enabled;
        config.get("secondwalk", "secondWalkEnabled", true).set(enabled);
    }
    public void setSecondWalkDistanceMin(int dist) {
        this.secondWalkDistanceMin = dist;
        config.get("secondwalk", "secondWalkDistanceMin", 10).set(dist);
    }
    public void setSecondWalkDistanceMax(int dist) {
        this.secondWalkDistanceMax = dist;
        config.get("secondwalk", "secondWalkDistanceMax", 30).set(dist);
    }
    public void setSecondWalkAngleVariation(float angle) {
        this.secondWalkAngleVariation = angle;
        config.get("secondwalk", "secondWalkAngleVariation", 20.0f).set(angle);
    }
    public void setSecondWalkRandomDirection(boolean random) {
        this.secondWalkRandomDirection = random;
        config.get("secondwalk", "secondWalkRandomDirection", true).set(random);
    }
    
    // AFK Getters
    public String getAfkWarpCommand() { return afkWarpCommand; }
    public int getAfkStartHour() { return afkStartHour; }
    public int getAfkStartMinute() { return afkStartMinute; }
    public int getAfkEndHour() { return afkEndHour; }
    public int getAfkEndMinute() { return afkEndMinute; }
    
    // Player Detection Getters
    public double getPlayerDetectionRadius() { return playerDetectionRadius; }
    public boolean shouldAvoidPlayers() { return avoidPlayers; }
    public boolean isInstantFlee() { return instantFlee; }
    public boolean shouldMineWhileMoving() { return mineWhileMoving; }
    public double getPlayerBlockingDistance() { return playerBlockingDistance; }
    public long getPlayerBlockingTimeout() { return playerBlockingTimeout; }
    public boolean isSmoothPlayerAvoidance() { return smoothPlayerAvoidance; }
    public long getMiningProgressCheckInterval() { return miningProgressCheckInterval; }
    
    // General Getters
    public boolean isShowOverlay() { return showOverlay; }
    public boolean isDebugMode() { return debugMode; }
    
    // Strafe Anti-AFK Getters
    public boolean isStrafeEnabled() { return strafeEnabled; }
    public long getStrafeInterval() { return strafeInterval; }
    public long getStrafeDuration() { return strafeDuration; }
    
    // Repair Getters
    public int getRepairDurabilityThreshold() { return repairDurabilityThreshold; }
    public String getRepairCommand() { return repairCommand; }
    public int getRepairSlot() { return repairSlot; }
    public int getRepairMaxRetries() { return repairMaxRetries; }
    public float getRepairClickDelay() { return repairClickDelay; }
    
    // OX Event Getters/Setters
    public float getOxLimeYaw() { return oxLimeYaw; }
    public float getOxRedYaw() { return oxRedYaw; }
    
    public void setOxLimeYaw(float yaw) {
        this.oxLimeYaw = yaw;
        config.get("ox", "limeYaw", 90.0f).set(yaw);
        save();
    }
    
    public void setOxRedYaw(float yaw) {
        this.oxRedYaw = yaw;
        config.get("ox", "redYaw", -90.0f).set(yaw);
        save();
    }
    
    public int getOxMinPlayers() { return oxMinPlayers; }
    
    public void setOxMinPlayers(int minPlayers) {
        this.oxMinPlayers = minPlayers;
        config.get("ox", "minPlayers", 5).set(minPlayers);
        save();
    }
    
    // Mining Setters
    public void setMiningWarpCommand(String cmd) { 
        this.miningWarpCommand = cmd; 
        config.get("mining", "warpCommand", "/warp maden").set(cmd);
        save();
    }
    
    public void setMiningStartTime(int hour, int minute) {
        this.miningStartHour = hour;
        this.miningStartMinute = minute;
        config.get("mining", "startHour", 4).set(hour);
        config.get("mining", "startMinute", 30).set(minute);
        save();
    }
    
    public void setMiningEndTime(int hour, int minute) {
        this.miningEndHour = hour;
        this.miningEndMinute = minute;
        config.get("mining", "endHour", 4).set(hour);
        config.get("mining", "endMinute", 40).set(minute);
        save();
    }
    
    // AFK Setters
    public void setAfkWarpCommand(String cmd) {
        this.afkWarpCommand = cmd;
        config.get("afk", "warpCommand", "/warp afk").set(cmd);
        save();
    }
    
    public void setAfkStartTime(int hour, int minute) {
        this.afkStartHour = hour;
        this.afkStartMinute = minute;
        config.get("afk", "startHour", 4).set(hour);
        config.get("afk", "startMinute", 40).set(minute);
        save();
    }
    
    public void setAfkEndTime(int hour, int minute) {
        this.afkEndHour = hour;
        this.afkEndMinute = minute;
        config.get("afk", "endHour", 5).set(hour);
        config.get("afk", "endMinute", 0).set(minute);
        save();
    }
    
    // Player Detection Setters
    public void setPlayerDetectionRadius(double radius) {
        this.playerDetectionRadius = radius;
        config.get("players", "detectionRadius", 3.0f).set(radius);
        save();
    }
    
    public void setAvoidPlayers(boolean avoid) {
        this.avoidPlayers = avoid;
        config.get("players", "avoidPlayers", true).set(avoid);
        save();
    }
    
    public void setInstantFlee(boolean instant) {
        this.instantFlee = instant;
        config.get("players", "instantFlee", false).set(instant);
        save();
    }
    
    public void setMineWhileMoving(boolean mine) {
        this.mineWhileMoving = mine;
        config.get("players", "mineWhileMoving", true).set(mine);
        save();
    }
    
    public void setShowOverlay(boolean show) {
        this.showOverlay = show;
        config.get("general", "showOverlay", true).set(show);
        save();
    }
    
    // Pathfinding Setters (setInitialWalkDistance already defined above in mining section)
    
    public void setMaxMoveDistance(int distance) {
        this.maxMoveDistance = distance;
        config.get("pathfinding", "maxMoveDistance", 15).set(distance);
        save();
    }
    
    // Anti-AFK Rotation Setters
    public void setAntiAfkYawMin(float min) {
        this.antiAfkYawMin = min;
        config.get("antiafk", "antiAfkYawMin", 0.5f).set(min);
        save();
    }
    
    public void setAntiAfkYawMax(float max) {
        this.antiAfkYawMax = max;
        config.get("antiafk", "antiAfkYawMax", 3.0f).set(max);
        save();
    }
    
    public void setAntiAfkPitchMin(float min) {
        this.antiAfkPitchMin = min;
        config.get("antiafk", "antiAfkPitchMin", 0.2f).set(min);
        save();
    }
    
    public void setAntiAfkPitchMax(float max) {
        this.antiAfkPitchMax = max;
        config.get("antiafk", "antiAfkPitchMax", 1.5f).set(max);
        save();
    }
    
    public void setAntiAfkSmoothSpeed(long speed) {
        this.antiAfkSmoothSpeed = speed;
        config.get("antiafk", "antiAfkSmoothSpeed", 500).set((int) speed);
        save();
    }
    
    public void setAntiAfkInterval(long interval) {
        this.antiAfkInterval = interval;
        config.get("mining", "antiAfkInterval", 3000).set((int) interval);
        save();
    }
    
    // Position Adjustment Setters
    public void setAdjustYawMin(float min) {
        this.adjustYawMin = min;
        config.get("adjustment", "adjustYawMin", 5.0f).set(min);
        save();
    }
    
    public void setAdjustYawMax(float max) {
        this.adjustYawMax = max;
        config.get("adjustment", "adjustYawMax", 25.0f).set(max);
        save();
    }
    
    public void setAdjustPitchMin(float min) {
        this.adjustPitchMin = min;
        config.get("adjustment", "adjustPitchMin", 3.0f).set(min);
        save();
    }
    
    public void setAdjustPitchMax(float max) {
        this.adjustPitchMax = max;
        config.get("adjustment", "adjustPitchMax", 15.0f).set(max);
        save();
    }
    
    public void setAdjustSmoothSpeed(long speed) {
        this.adjustSmoothSpeed = speed;
        config.get("adjustment", "adjustSmoothSpeed", 800).set((int) speed);
        save();
    }
    
    public void setBlockLockEnabled(boolean enabled) {
        this.enableBlockLock = enabled;
        config.get("adjustment", "enableBlockLock", false).set(enabled);
        save();
    }
    
    // Strafe Setters
    public void setStrafeEnabled(boolean enabled) {
        this.strafeEnabled = enabled;
        config.get("strafe", "strafeEnabled", true).set(enabled);
        save();
    }
    
    public void setStrafeInterval(long interval) {
        this.strafeInterval = interval;
        config.get("strafe", "strafeInterval", 30000).set((int) interval);
        save();
    }
    
    public void setStrafeDuration(long duration) {
        this.strafeDuration = duration;
        config.get("strafe", "strafeDuration", 150).set((int) duration);
        save();
    }
    
    // Repair Setters
    public void setRepairDurabilityThreshold(int threshold) {
        this.repairDurabilityThreshold = threshold;
        config.get("repair", "durabilityThreshold", 1000).set(threshold);
        save();
    }
    
    public void setRepairCommand(String cmd) {
        this.repairCommand = cmd;
        config.get("repair", "repairCommand", "/tamirci").set(cmd);
        save();
    }
    
    public void setRepairSlot(int slot) {
        this.repairSlot = slot;
        config.get("repair", "repairSlot", 21).set(slot);
        save();
    }
    
    public void setRepairMaxRetries(int retries) {
        this.repairMaxRetries = retries;
        config.get("repair", "maxRetries", 3).set(retries);
        save();
    }
    
    public void setRepairClickDelay(float delay) {
        this.repairClickDelay = delay;
        config.get("repair", "clickDelay", 2.0f).set(delay);
        save();
    }
}
