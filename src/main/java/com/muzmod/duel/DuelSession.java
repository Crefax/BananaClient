package com.muzmod.duel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.muzmod.MuzMod;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages a 1v1 duel session between two players
 */
public class DuelSession {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    
    private String player1Name;
    private String player2Name;
    
    private DuelData player1Data;
    private DuelData player2Data;
    
    private boolean isActive = false;
    private long sessionStartTime;
    
    // Önceki tick'teki zırh durumları (kırılma tespiti için)
    private Map<String, ItemStack[]> lastArmorStates = new HashMap<>();
    
    // Önceki tick'teki item sayıları (elma yeme tespiti için)
    private Map<String, Integer> lastGoldenAppleCount = new HashMap<>();
    private Map<String, Integer> lastEnchantedAppleCount = new HashMap<>();
    
    public DuelSession() {
    }
    
    /**
     * Start a new duel session
     */
    public void start(String player1, String player2) {
        this.player1Name = player1;
        this.player2Name = player2;
        
        this.player1Data = new DuelData(player1);
        this.player2Data = new DuelData(player2);
        
        this.isActive = true;
        this.sessionStartTime = System.currentTimeMillis();
        
        // İlk zırh ve item durumlarını kaydet
        initializePlayerState(player1);
        initializePlayerState(player2);
        
        // Kılıç bilgilerini kaydet
        updateSwordInfo(player1);
        updateSwordInfo(player2);
        
        MuzMod.LOGGER.info("[DuelAnalyzer] Session started: " + player1 + " vs " + player2);
    }
    
    /**
     * Stop the duel session
     */
    public void stop() {
        if (!isActive) return;
        
        isActive = false;
        MuzMod.LOGGER.info("[DuelAnalyzer] Session stopped");
    }
    
    /**
     * End the duel with a winner (one player died)
     */
    public void endDuel(String loserName) {
        if (!isActive) return;
        
        DuelData loser = getPlayerData(loserName);
        DuelData winner = getOpponentData(loserName);
        
        if (loser != null) {
            loser.setDead(true);
        }
        if (winner != null) {
            winner.setWinner(true);
        }
        
        // JSON olarak kaydet
        saveDuelRecord();
        
        isActive = false;
        MuzMod.LOGGER.info("[DuelAnalyzer] Duel ended - Winner: " + (winner != null ? winner.getPlayerName() : "Unknown"));
    }
    
    /**
     * Initialize player state tracking
     */
    private void initializePlayerState(String playerName) {
        EntityPlayer player = findPlayer(playerName);
        if (player == null) return;
        
        // Zırh durumu
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            ItemStack stack = player.inventory.armorInventory[i];
            armor[i] = stack != null ? stack.copy() : null;
        }
        lastArmorStates.put(playerName, armor);
        
        // Elma sayıları
        int[] appleCounts = countApples(player);
        lastGoldenAppleCount.put(playerName, appleCounts[0]);
        lastEnchantedAppleCount.put(playerName, appleCounts[1]);
    }
    
    /**
     * Update sword info for a player
     */
    public void updateSwordInfo(String playerName) {
        EntityPlayer player = findPlayer(playerName);
        if (player == null) return;
        
        ItemStack heldItem = player.getHeldItem();
        if (heldItem != null && heldItem.getItem() instanceof ItemSword) {
            DuelData data = getPlayerData(playerName);
            if (data != null) {
                data.setSwordInfo(heldItem);
            }
        }
    }
    
    /**
     * Called every tick to update tracking
     */
    public void onTick() {
        if (!isActive) return;
        
        // Her iki oyuncuyu da kontrol et
        checkPlayerState(player1Name, player1Data);
        checkPlayerState(player2Name, player2Data);
    }
    
    /**
     * Check and update player state
     */
    private void checkPlayerState(String playerName, DuelData data) {
        EntityPlayer player = findPlayer(playerName);
        if (player == null) return;
        
        // Zırh kırılma kontrolü
        checkArmorBreak(playerName, player, data);
        
        // Elma yeme kontrolü
        checkAppleConsumption(playerName, player, data);
        
        // Kılıç güncelle (değişmiş olabilir)
        ItemStack heldItem = player.getHeldItem();
        if (heldItem != null && heldItem.getItem() instanceof ItemSword) {
            data.setSwordInfo(heldItem);
        }
    }
    
    /**
     * Check if armor pieces broke or lost durability
     */
    private void checkArmorBreak(String playerName, EntityPlayer player, DuelData data) {
        ItemStack[] lastArmor = lastArmorStates.get(playerName);
        if (lastArmor == null) {
            lastArmor = new ItemStack[4];
            lastArmorStates.put(playerName, lastArmor);
        }
        
        ItemStack[] currentArmor = player.inventory.armorInventory;
        
        // 0=boots, 1=leggings, 2=chestplate, 3=helmet
        for (int i = 0; i < 4; i++) {
            ItemStack last = lastArmor[i];
            ItemStack current = currentArmor[i];
            
            // Durability kontrolü - eğer aynı item hala varsa
            if (last != null && current != null) {
                // Aynı item tipiyse durability değişimini kontrol et
                if (Item.getIdFromItem(last.getItem()) == Item.getIdFromItem(current.getItem())) {
                    int lastDurability = last.getMaxDamage() - last.getItemDamage();
                    int currentDurability = current.getMaxDamage() - current.getItemDamage();
                    
                    if (currentDurability < lastDurability) {
                        int durabilityLost = lastDurability - currentDurability;
                        switch (i) {
                            case 0: data.addBootsDurabilityLost(durabilityLost); break;
                            case 1: data.addLeggingsDurabilityLost(durabilityLost); break;
                            case 2: data.addChestplateDurabilityLost(durabilityLost); break;
                            case 3: data.addHelmetDurabilityLost(durabilityLost); break;
                        }
                    }
                }
            }
            
            // Önceki tick'te vardı, şimdi yok = kırıldı (tamamen)
            if (last != null && current == null) {
                switch (i) {
                    case 0: data.addBootsBroken(); break;
                    case 1: data.addLeggingsBroken(); break;
                    case 2: data.addChestplateBroken(); break;
                    case 3: data.addHelmetBroken(); break;
                }
                MuzMod.LOGGER.info("[DuelAnalyzer] " + playerName + " broke armor slot " + i);
            }
            
            // Durumu güncelle
            lastArmor[i] = current != null ? current.copy() : null;
        }
    }
    
    /**
     * Check if apples were consumed
     */
    private void checkAppleConsumption(String playerName, EntityPlayer player, DuelData data) {
        int[] currentCounts = countApples(player);
        
        int lastGolden = lastGoldenAppleCount.getOrDefault(playerName, 0);
        int lastEnchanted = lastEnchantedAppleCount.getOrDefault(playerName, 0);
        
        // Azaldıysa yendi
        if (currentCounts[0] < lastGolden) {
            int eaten = lastGolden - currentCounts[0];
            for (int i = 0; i < eaten; i++) {
                data.addGoldenAppleEaten();
            }
            MuzMod.LOGGER.info("[DuelAnalyzer] " + playerName + " ate " + eaten + " golden apple(s)");
        }
        
        if (currentCounts[1] < lastEnchanted) {
            int eaten = lastEnchanted - currentCounts[1];
            for (int i = 0; i < eaten; i++) {
                data.addEnchantedAppleEaten();
            }
            MuzMod.LOGGER.info("[DuelAnalyzer] " + playerName + " ate " + eaten + " enchanted apple(s)");
        }
        
        lastGoldenAppleCount.put(playerName, currentCounts[0]);
        lastEnchantedAppleCount.put(playerName, currentCounts[1]);
    }
    
    /**
     * Count golden and enchanted apples in inventory
     * Returns [goldenCount, enchantedCount]
     */
    private int[] countApples(EntityPlayer player) {
        int golden = 0;
        int enchanted = 0;
        
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack != null) {
                // Golden Apple: minecraft:golden_apple with damage 0
                // Enchanted Golden Apple: minecraft:golden_apple with damage 1
                if (Item.getIdFromItem(stack.getItem()) == 322) { // golden_apple
                    if (stack.getMetadata() == 0) {
                        golden += stack.stackSize;
                    } else if (stack.getMetadata() == 1) {
                        enchanted += stack.stackSize;
                    }
                }
            }
        }
        
        return new int[]{golden, enchanted};
    }
    
    /**
     * Record a hit from attacker to victim
     */
    public void recordHit(String attackerName, String victimName) {
        if (!isActive) return;
        
        // Sadece duel'daki oyuncuları say
        if (!isParticipant(attackerName) || !isParticipant(victimName)) return;
        
        DuelData attacker = getPlayerData(attackerName);
        DuelData victim = getPlayerData(victimName);
        
        if (attacker != null) {
            attacker.addHitGiven();
        }
        if (victim != null) {
            victim.addHitReceived();
        }
    }
    
    /**
     * Save duel record to JSON file
     */
    private void saveDuelRecord() {
        try {
            // .minecraft/BananaClient/duel_records/ klasörü
            File mcDir = Minecraft.getMinecraft().mcDataDir;
            File bananaDir = new File(mcDir, "BananaClient");
            File recordsDir = new File(bananaDir, "duel_records");
            
            if (!recordsDir.exists()) {
                recordsDir.mkdirs();
            }
            
            // Dosya adı: player1_vs_player2_timestamp.json
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = sdf.format(new Date());
            String fileName = player1Name + "_vs_" + player2Name + "_" + timestamp + ".json";
            
            File recordFile = new File(recordsDir, fileName);
            
            // JSON oluştur
            Map<String, Object> record = new HashMap<>();
            record.put("timestamp", timestamp);
            record.put("duration", player1Data.getDurationFormatted());
            record.put("player1", player1Data);
            record.put("player2", player2Data);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(record);
            
            // Dosyaya yaz
            FileWriter writer = new FileWriter(recordFile);
            writer.write(json);
            writer.close();
            
            MuzMod.LOGGER.info("[DuelAnalyzer] Record saved: " + recordFile.getAbsolutePath());
            
        } catch (IOException e) {
            MuzMod.LOGGER.error("[DuelAnalyzer] Failed to save record: " + e.getMessage());
        }
    }
    
    /**
     * Save duel record manually (when user stops analysis)
     */
    public void saveDuelRecordManual() {
        saveDuelRecord();
    }
    
    // ================== HELPER METHODS ==================
    
    private EntityPlayer findPlayer(String name) {
        if (mc.theWorld == null) return null;
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }
    
    public boolean isParticipant(String name) {
        return name.equalsIgnoreCase(player1Name) || name.equalsIgnoreCase(player2Name);
    }
    
    public DuelData getPlayerData(String name) {
        if (name.equalsIgnoreCase(player1Name)) return player1Data;
        if (name.equalsIgnoreCase(player2Name)) return player2Data;
        return null;
    }
    
    public DuelData getOpponentData(String name) {
        if (name.equalsIgnoreCase(player1Name)) return player2Data;
        if (name.equalsIgnoreCase(player2Name)) return player1Data;
        return null;
    }
    
    // ================== GETTERS ==================
    
    public boolean isActive() { return isActive; }
    public String getPlayer1Name() { return player1Name; }
    public String getPlayer2Name() { return player2Name; }
    public DuelData getPlayer1Data() { return player1Data; }
    public DuelData getPlayer2Data() { return player2Data; }
    
    public long getSessionDuration() {
        return System.currentTimeMillis() - sessionStartTime;
    }
    
    public String getSessionDurationFormatted() {
        long ms = getSessionDuration();
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
