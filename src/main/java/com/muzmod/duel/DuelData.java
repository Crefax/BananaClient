package com.muzmod.duel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores duel statistics for a single player
 */
public class DuelData {
    
    // Oyuncu bilgisi
    private String playerName;
    private long startTime;
    private long endTime;
    
    // Savaş istatistikleri
    private int hitsGiven = 0;      // Verdiği hit
    private int hitsReceived = 0;   // Aldığı hit
    
    // Tüketilen itemler
    private int goldenApplesEaten = 0;      // Normal altın elma
    private int enchantedApplesEaten = 0;   // Enchanted (Notch) elma
    
    // Kırılan zırh parçaları (tamamen kırıldığında)
    private int helmetsBroken = 0;
    private int chestplatesBroken = 0;
    private int leggingsBroken = 0;
    private int bootsBroken = 0;
    
    // Kaybedilen toplam durability
    private int totalHelmetDurabilityLost = 0;
    private int totalChestplateDurabilityLost = 0;
    private int totalLeggingsDurabilityLost = 0;
    private int totalBootsDurabilityLost = 0;
    
    // Kılıç bilgisi
    private SwordInfo swordInfo;
    
    // Zırh durumu (başlangıç ve bitiş)
    private Map<String, Integer> startArmor = new HashMap<>();
    private Map<String, Integer> endArmor = new HashMap<>();
    
    // Sonuç
    private boolean isDead = false;
    private boolean isWinner = false;
    
    public DuelData(String playerName) {
        this.playerName = playerName;
        this.startTime = System.currentTimeMillis();
        this.swordInfo = new SwordInfo();
    }
    
    // ================== GETTERS & SETTERS ==================
    
    public String getPlayerName() { return playerName; }
    
    public int getHitsGiven() { return hitsGiven; }
    public void addHitGiven() { hitsGiven++; }
    
    public int getHitsReceived() { return hitsReceived; }
    public void addHitReceived() { hitsReceived++; }
    
    public int getGoldenApplesEaten() { return goldenApplesEaten; }
    public void addGoldenAppleEaten() { goldenApplesEaten++; }
    
    public int getEnchantedApplesEaten() { return enchantedApplesEaten; }
    public void addEnchantedAppleEaten() { enchantedApplesEaten++; }
    
    // Armor broken counts
    public int getHelmetsBroken() { return helmetsBroken; }
    public void addHelmetBroken() { helmetsBroken++; }
    
    public int getChestplatesBroken() { return chestplatesBroken; }
    public void addChestplateBroken() { chestplatesBroken++; }
    
    public int getLeggingsBroken() { return leggingsBroken; }
    public void addLeggingsBroken() { leggingsBroken++; }
    
    public int getBootsBroken() { return bootsBroken; }
    public void addBootsBroken() { bootsBroken++; }
    
    public int getTotalArmorBroken() {
        return helmetsBroken + chestplatesBroken + leggingsBroken + bootsBroken;
    }
    
    // Durability lost
    public int getTotalHelmetDurabilityLost() { return totalHelmetDurabilityLost; }
    public void addHelmetDurabilityLost(int amount) { totalHelmetDurabilityLost += amount; }
    
    public int getTotalChestplateDurabilityLost() { return totalChestplateDurabilityLost; }
    public void addChestplateDurabilityLost(int amount) { totalChestplateDurabilityLost += amount; }
    
    public int getTotalLeggingsDurabilityLost() { return totalLeggingsDurabilityLost; }
    public void addLeggingsDurabilityLost(int amount) { totalLeggingsDurabilityLost += amount; }
    
    public int getTotalBootsDurabilityLost() { return totalBootsDurabilityLost; }
    public void addBootsDurabilityLost(int amount) { totalBootsDurabilityLost += amount; }
    
    public int getTotalArmorDurabilityLost() {
        return totalHelmetDurabilityLost + totalChestplateDurabilityLost + 
               totalLeggingsDurabilityLost + totalBootsDurabilityLost;
    }
    
    public SwordInfo getSwordInfo() { return swordInfo; }
    
    public void setSwordInfo(ItemStack sword) {
        if (sword != null) {
            this.swordInfo = new SwordInfo(sword);
        }
    }
    
    public boolean isDead() { return isDead; }
    public void setDead(boolean dead) { 
        isDead = dead;
        if (dead) {
            endTime = System.currentTimeMillis();
        }
    }
    
    public boolean isWinner() { return isWinner; }
    public void setWinner(boolean winner) { 
        isWinner = winner;
        endTime = System.currentTimeMillis();
    }
    
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    
    public long getDurationMs() {
        if (endTime > 0) {
            return endTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }
    
    public String getDurationFormatted() {
        long ms = getDurationMs();
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    public void setStartArmor(Map<String, Integer> armor) {
        this.startArmor = new HashMap<>(armor);
    }
    
    public void setEndArmor(Map<String, Integer> armor) {
        this.endArmor = new HashMap<>(armor);
    }
    
    // ================== JSON EXPORT ==================
    
    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    
    // ================== SWORD INFO CLASS ==================
    
    public static class SwordInfo {
        private String displayName = "Unknown";
        private String itemId = "unknown";
        private int damage = 0;
        private int maxDamage = 0;
        private List<String> lore = new ArrayList<>();
        private List<String> enchantments = new ArrayList<>();
        private Map<String, Object> nbtData = new HashMap<>();
        
        public SwordInfo() {}
        
        public SwordInfo(ItemStack sword) {
            if (sword == null) return;
            
            // Temel bilgiler
            this.displayName = sword.getDisplayName();
            this.itemId = sword.getItem().getRegistryName();
            this.damage = sword.getItemDamage();
            this.maxDamage = sword.getMaxDamage();
            
            // Lore
            if (sword.hasTagCompound() && sword.getTagCompound().hasKey("display")) {
                NBTTagCompound display = sword.getTagCompound().getCompoundTag("display");
                if (display.hasKey("Lore")) {
                    NBTTagList loreList = display.getTagList("Lore", 8);
                    for (int i = 0; i < loreList.tagCount(); i++) {
                        lore.add(loreList.getStringTagAt(i));
                    }
                }
            }
            
            // Enchantments
            if (sword.isItemEnchanted()) {
                NBTTagList enchList = sword.getEnchantmentTagList();
                if (enchList != null) {
                    for (int i = 0; i < enchList.tagCount(); i++) {
                        NBTTagCompound ench = enchList.getCompoundTagAt(i);
                        int id = ench.getShort("id");
                        int lvl = ench.getShort("lvl");
                        enchantments.add("ID:" + id + " Lvl:" + lvl);
                    }
                }
            }
            
            // Full NBT data
            if (sword.hasTagCompound()) {
                parseNBT(sword.getTagCompound(), nbtData);
            }
        }
        
        private void parseNBT(NBTTagCompound tag, Map<String, Object> map) {
            for (String key : tag.getKeySet()) {
                byte type = tag.getTagId(key);
                switch (type) {
                    case 1: map.put(key, tag.getByte(key)); break;
                    case 2: map.put(key, tag.getShort(key)); break;
                    case 3: map.put(key, tag.getInteger(key)); break;
                    case 4: map.put(key, tag.getLong(key)); break;
                    case 5: map.put(key, tag.getFloat(key)); break;
                    case 6: map.put(key, tag.getDouble(key)); break;
                    case 8: map.put(key, tag.getString(key)); break;
                    case 10: 
                        Map<String, Object> subMap = new HashMap<>();
                        parseNBT(tag.getCompoundTag(key), subMap);
                        map.put(key, subMap);
                        break;
                    default: map.put(key, "Type:" + type); break;
                }
            }
        }
        
        public String getDisplayName() { return displayName; }
        public String getItemId() { return itemId; }
        public int getDamage() { return damage; }
        public int getMaxDamage() { return maxDamage; }
        public List<String> getLore() { return lore; }
        public List<String> getEnchantments() { return enchantments; }
        public Map<String, Object> getNbtData() { return nbtData; }
    }
}
