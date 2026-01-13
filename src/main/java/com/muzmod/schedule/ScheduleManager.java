package com.muzmod.schedule;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.muzmod.MuzMod;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Haftalık zamanlama yöneticisi
 * Etkinlik ekleme, silme, aktif etkinlik bulma
 * Her oyuncu için ayrı schedule dosyası
 */
public class ScheduleManager {
    
    private List<ScheduleEntry> entries = new ArrayList<>();
    private boolean scheduleEnabled = true;
    private boolean autoAfkWhenIdle = true; // Boş zamanlarda otomatik AFK
    private String defaultAfkWarp = "/warp afk";
    private String defaultMiningWarp = "/warp maden";
    
    private File clientDir;     // Ana dizin (default schedule için)
    private File schedulesDir;  // Oyuncu schedule'ları için
    private File scheduleFile;
    private String defaultScheduleName;
    private String currentPlayerName = null;
    private Gson gson;
    
    private ScheduleEntry.EventType lastScheduledType = null;
    private long lastTypeChangeTime = 0;
    
    public ScheduleManager(File clientDir, File schedulesDir, String defaultScheduleName) {
        this.clientDir = clientDir;
        this.schedulesDir = schedulesDir;
        this.defaultScheduleName = defaultScheduleName;
        this.scheduleFile = new File(clientDir, defaultScheduleName); // Varsayılan ana dizinde
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }
    
    /**
     * Şu anki zamana göre aktif etkinliği bul
     */
    public ScheduleEntry getCurrentEntry(int dayOfWeek, int hour, int minute) {
        return getCurrentEntry(dayOfWeek, hour, minute, 0);
    }
    
    /**
     * Şu anki zamana göre aktif etkinliği bul (saniye dahil)
     */
    public ScheduleEntry getCurrentEntry(int dayOfWeek, int hour, int minute, int second) {
        if (!scheduleEnabled) return null;
        
        return entries.stream()
            .filter(e -> e.getDayOfWeek() == dayOfWeek && e.isActiveAt(hour, minute, second))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Oyuncu bazlı schedule yükle
     * Yeni oyuncu için varsayılan schedule kopyalanır
     */
    public void loadForPlayer(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return;
        }
        
        // Aynı oyuncu için tekrar yükleme
        if (playerName.equals(currentPlayerName)) {
            return;
        }
        
        currentPlayerName = playerName;
        
        // Oyuncuya özel schedule dosyası: schedules/OyuncuAdi.json
        File playerScheduleFile = new File(schedulesDir, playerName + ".json");
        File defaultFile = new File(clientDir, defaultScheduleName); // Ana dizindeki default
        
        // Eğer oyuncunun schedule dosyası yoksa, varsayılan varsa kopyala
        if (!playerScheduleFile.exists() && defaultFile.exists()) {
            MuzMod.LOGGER.info("[Schedule] Creating schedule for new player: " + playerName + " (copying from default)");
            copyFile(defaultFile, playerScheduleFile);
        }
        
        // Schedule dosyasını güncelle ve yükle
        this.scheduleFile = playerScheduleFile;
        load();
        
        MuzMod.LOGGER.info("[Schedule] Loaded for player: " + playerName + " -> " + playerScheduleFile.getName());
    }
    
    /**
     * Dosya kopyalama yardımcı metodu
     */
    private void copyFile(File source, File dest) {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(source);
             java.io.FileOutputStream fos = new java.io.FileOutputStream(dest);
             java.nio.channels.FileChannel sourceChannel = fis.getChannel();
             java.nio.channels.FileChannel destChannel = fos.getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } catch (IOException e) {
            MuzMod.LOGGER.error("[Schedule] Error copying file: " + e.getMessage());
        }
    }
    
    public String getCurrentPlayerName() {
        return currentPlayerName;
    }
    
    /**
     * Mevcut schedule'ı varsayılan olarak kaydet
     */
    public boolean exportToDefault() {
        if (currentPlayerName == null) {
            MuzMod.LOGGER.warn("[Schedule] No player schedule loaded to export");
            return false;
        }
        
        File playerScheduleFile = new File(schedulesDir, currentPlayerName + ".json");
        File defaultFile = new File(clientDir, defaultScheduleName);
        
        if (!playerScheduleFile.exists()) {
            MuzMod.LOGGER.warn("[Schedule] Player schedule file not found");
            return false;
        }
        
        // Önce kaydet
        save();
        
        // Sonra default'a kopyala
        copyFile(playerScheduleFile, defaultFile);
        MuzMod.LOGGER.info("[Schedule] Exported to default: " + currentPlayerName + " -> " + defaultScheduleName);
        return true;
    }
    
    /**
     * Varsayılan schedule'ı mevcut hesaba yükle
     */
    public boolean importFromDefault() {
        if (currentPlayerName == null) {
            MuzMod.LOGGER.warn("[Schedule] No player loaded to import schedule");
            return false;
        }
        
        File defaultFile = new File(clientDir, defaultScheduleName);
        File playerScheduleFile = new File(schedulesDir, currentPlayerName + ".json");
        
        if (!defaultFile.exists()) {
            MuzMod.LOGGER.warn("[Schedule] Default schedule file not found");
            return false;
        }
        
        // Default'tan oyuncu schedule'ına kopyala
        copyFile(defaultFile, playerScheduleFile);
        
        // Yeniden yükle
        this.scheduleFile = playerScheduleFile;
        load();
        
        MuzMod.LOGGER.info("[Schedule] Imported from default: " + defaultScheduleName + " -> " + currentPlayerName);
        return true;
    }
    
    /**
     * Şu anki zamana göre hangi state'te olmalı?
     */
    public ScheduleEntry.EventType getCurrentScheduledType(int dayOfWeek, int hour, int minute) {
        return getCurrentScheduledType(dayOfWeek, hour, minute, 0);
    }
    
    /**
     * Şu anki zamana göre hangi state'te olmalı? (saniye dahil)
     */
    public ScheduleEntry.EventType getCurrentScheduledType(int dayOfWeek, int hour, int minute, int second) {
        ScheduleEntry entry = getCurrentEntry(dayOfWeek, hour, minute, second);
        
        if (entry != null) {
            return entry.getEventType();
        }
        
        // Etkinlik yoksa ve autoAfk açıksa AFK dön
        if (autoAfkWhenIdle && scheduleEnabled) {
            return ScheduleEntry.EventType.AFK;
        }
        
        return ScheduleEntry.EventType.IDLE;
    }
    
    /**
     * Aktif etkinliğin warp komutunu al
     */
    public String getCurrentWarpCommand(int dayOfWeek, int hour, int minute) {
        ScheduleEntry entry = getCurrentEntry(dayOfWeek, hour, minute);
        
        if (entry != null && entry.getCustomWarpCommand() != null && !entry.getCustomWarpCommand().isEmpty()) {
            return entry.getCustomWarpCommand();
        }
        
        ScheduleEntry.EventType type = getCurrentScheduledType(dayOfWeek, hour, minute);
        switch (type) {
            case MINING: return defaultMiningWarp;
            case AFK: return defaultAfkWarp;
            default: return "";
        }
    }
    
    /**
     * Belirli bir gün için tüm etkinlikleri getir (saat sırasına göre)
     */
    public List<ScheduleEntry> getEntriesForDay(int dayOfWeek) {
        return entries.stream()
            .filter(e -> e.getDayOfWeek() == dayOfWeek)
            .sorted(Comparator.comparingInt(ScheduleEntry::getStartTimeMinutes))
            .collect(Collectors.toList());
    }
    
    /**
     * Tüm etkinlikler
     */
    public List<ScheduleEntry> getAllEntries() {
        return new ArrayList<>(entries);
    }
    
    /**
     * Etkinlik ekle
     */
    public void addEntry(ScheduleEntry entry) {
        entries.add(entry);
        save();
        MuzMod.LOGGER.info("[Schedule] Entry added: " + entry);
    }
    
    /**
     * Etkinlik sil
     */
    public void removeEntry(long entryId) {
        entries.removeIf(e -> e.getId() == entryId);
        save();
        MuzMod.LOGGER.info("[Schedule] Entry removed: " + entryId);
    }
    
    /**
     * Etkinlik güncelle
     */
    public void updateEntry(ScheduleEntry entry) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getId() == entry.getId()) {
                entries.set(i, entry);
                save();
                return;
            }
        }
    }
    
    /**
     * ID ile etkinlik bul
     */
    public ScheduleEntry getEntryById(long id) {
        return entries.stream()
            .filter(e -> e.getId() == id)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Bir gün için etkinlik sayısı
     */
    public int getEntryCountForDay(int dayOfWeek) {
        return (int) entries.stream()
            .filter(e -> e.getDayOfWeek() == dayOfWeek)
            .count();
    }
    
    /**
     * Sonraki etkinliğe kalan süre (dakika)
     */
    public int getMinutesToNextEvent(int dayOfWeek, int hour, int minute) {
        int currentMinutes = hour * 60 + minute;
        
        // Bugün için sonraki etkinlik
        Optional<ScheduleEntry> nextToday = entries.stream()
            .filter(e -> e.getDayOfWeek() == dayOfWeek && e.getStartTimeMinutes() > currentMinutes)
            .min(Comparator.comparingInt(ScheduleEntry::getStartTimeMinutes));
        
        if (nextToday.isPresent()) {
            return nextToday.get().getStartTimeMinutes() - currentMinutes;
        }
        
        // Yarın için ilk etkinlik
        int nextDay = (dayOfWeek + 1) % 7;
        Optional<ScheduleEntry> nextTomorrow = entries.stream()
            .filter(e -> e.getDayOfWeek() == nextDay)
            .min(Comparator.comparingInt(ScheduleEntry::getStartTimeMinutes));
        
        if (nextTomorrow.isPresent()) {
            return (24 * 60 - currentMinutes) + nextTomorrow.get().getStartTimeMinutes();
        }
        
        return -1; // Etkinlik yok
    }
    
    /**
     * Tüm etkinlikleri temizle
     */
    public void clearAll() {
        entries.clear();
        save();
    }
    
    /**
     * Belirli bir günün etkinliklerini temizle
     */
    public void clearDay(int dayOfWeek) {
        entries.removeIf(e -> e.getDayOfWeek() == dayOfWeek);
        save();
    }
    
    /**
     * Bir günü başka güne kopyala
     */
    public void copyDay(int fromDay, int toDay) {
        List<ScheduleEntry> toCopy = getEntriesForDay(fromDay);
        for (ScheduleEntry entry : toCopy) {
            ScheduleEntry newEntry = new ScheduleEntry(
                toDay,
                entry.getStartHour(),
                entry.getStartMinute(),
                entry.getEndHour(),
                entry.getEndMinute(),
                entry.getEventType()
            );
            newEntry.setCustomWarpCommand(entry.getCustomWarpCommand());
            newEntry.setEnabled(entry.isEnabled());
            entries.add(newEntry);
        }
        save();
    }
    
    /**
     * Hafta içi günlerine aynı programı uygula
     */
    public void applyToWeekdays(int sourceDay) {
        MuzMod.LOGGER.info("[Schedule] Applying day " + sourceDay + " to weekdays");
        for (int day = 0; day < 5; day++) { // Pzt-Cum
            if (day != sourceDay) {
                clearDay(day);
                copyDay(sourceDay, day);
            }
        }
        save();
        MuzMod.LOGGER.info("[Schedule] Weekdays updated from day " + sourceDay);
    }
    
    /**
     * Hafta sonu günlerine aynı programı uygula
     */
    public void applyToWeekends(int sourceDay) {
        MuzMod.LOGGER.info("[Schedule] Applying day " + sourceDay + " to weekends");
        for (int day = 5; day < 7; day++) { // Cmt-Paz
            if (day != sourceDay) {
                clearDay(day);
                copyDay(sourceDay, day);
            }
        }
        save();
        MuzMod.LOGGER.info("[Schedule] Weekends updated from day " + sourceDay);
    }
    
    // Kaydet/Yükle
    public void save() {
        try {
            ScheduleData data = new ScheduleData();
            data.entries = entries;
            data.scheduleEnabled = scheduleEnabled;
            data.autoAfkWhenIdle = autoAfkWhenIdle;
            data.defaultAfkWarp = defaultAfkWarp;
            data.defaultMiningWarp = defaultMiningWarp;
            
            try (Writer writer = new FileWriter(scheduleFile)) {
                gson.toJson(data, writer);
            }
            MuzMod.LOGGER.info("[Schedule] Saved " + entries.size() + " entries");
        } catch (IOException e) {
            MuzMod.LOGGER.error("[Schedule] Save error: " + e.getMessage());
        }
    }
    
    public void load() {
        if (!scheduleFile.exists()) {
            // Varsayılan program oluştur
            createDefaultSchedule();
            return;
        }
        
        try (Reader reader = new FileReader(scheduleFile)) {
            ScheduleData data = gson.fromJson(reader, ScheduleData.class);
            if (data != null) {
                if (data.entries != null) entries = data.entries;
                scheduleEnabled = data.scheduleEnabled;
                autoAfkWhenIdle = data.autoAfkWhenIdle;
                if (data.defaultAfkWarp != null) defaultAfkWarp = data.defaultAfkWarp;
                if (data.defaultMiningWarp != null) defaultMiningWarp = data.defaultMiningWarp;
            }
            MuzMod.LOGGER.info("[Schedule] Loaded " + entries.size() + " entries");
        } catch (Exception e) {
            MuzMod.LOGGER.error("[Schedule] Load error: " + e.getMessage());
            createDefaultSchedule();
        }
    }
    
    private void createDefaultSchedule() {
        entries.clear();
        
        // Mining saatleri - 20 saniye erken git (SS:MM:40 olduğunda başla)
        // Event: 03:40, 15:40, 21:40, 23:40 -> Başlangıç: 03:39:40, 15:39:40, vb.
        // Hafta içi (Pzt-Cum, 0-4)
        for (int day = 0; day < 5; day++) {
            // 03:40 eventi -> 03:39:40'ta başla
            entries.add(new ScheduleEntry(day, 3, 39, 40, 4, 0, 0, ScheduleEntry.EventType.MINING));
            // 15:40 eventi -> 15:39:40'ta başla
            entries.add(new ScheduleEntry(day, 15, 39, 40, 16, 0, 0, ScheduleEntry.EventType.MINING));
            // 21:40 eventi -> 21:39:40'ta başla
            entries.add(new ScheduleEntry(day, 21, 39, 40, 22, 0, 0, ScheduleEntry.EventType.MINING));
            // 23:40 eventi -> 23:39:40'ta başla
            entries.add(new ScheduleEntry(day, 23, 39, 40, 0, 0, 0, ScheduleEntry.EventType.MINING));
        }
        
        // Hafta sonu (Cmt-Paz, 5-6): 03:40, 11:40, 15:40, 21:40, 23:40
        for (int day = 5; day < 7; day++) {
            entries.add(new ScheduleEntry(day, 3, 39, 40, 4, 0, 0, ScheduleEntry.EventType.MINING));
            entries.add(new ScheduleEntry(day, 11, 39, 40, 12, 0, 0, ScheduleEntry.EventType.MINING));
            entries.add(new ScheduleEntry(day, 15, 39, 40, 16, 0, 0, ScheduleEntry.EventType.MINING));
            entries.add(new ScheduleEntry(day, 21, 39, 40, 22, 0, 0, ScheduleEntry.EventType.MINING));
            entries.add(new ScheduleEntry(day, 23, 39, 40, 0, 0, 0, ScheduleEntry.EventType.MINING));
        }
        
        // OX Event saatleri - 20 saniye erken git
        // Hafta içi (Pzt-Cum): 17:50, 00:05
        for (int day = 0; day < 5; day++) {
            // 00:05 eventi -> 00:04:40'ta başla
            entries.add(new ScheduleEntry(day, 0, 4, 40, 0, 15, 0, ScheduleEntry.EventType.OX));
            // 17:50 eventi -> 17:49:40'ta başla
            entries.add(new ScheduleEntry(day, 17, 49, 40, 18, 0, 0, ScheduleEntry.EventType.OX));
        }
        
        // Hafta sonu (Cmt-Paz): 11:00, 17:50, 00:05
        for (int day = 5; day < 7; day++) {
            entries.add(new ScheduleEntry(day, 0, 4, 40, 0, 15, 0, ScheduleEntry.EventType.OX));
            // 11:00 eventi -> 10:59:40'ta başla
            entries.add(new ScheduleEntry(day, 10, 59, 40, 11, 10, 0, ScheduleEntry.EventType.OX));
            entries.add(new ScheduleEntry(day, 17, 49, 40, 18, 0, 0, ScheduleEntry.EventType.OX));
        }
        
        save();
        MuzMod.LOGGER.info("[Schedule] Created default schedule with Mining and OX events (20s early start)");
    }
    
    // Getters & Setters
    public boolean isScheduleEnabled() { return scheduleEnabled; }
    public void setScheduleEnabled(boolean enabled) { 
        this.scheduleEnabled = enabled; 
        save();
    }
    
    public boolean isAutoAfkWhenIdle() { return autoAfkWhenIdle; }
    public void setAutoAfkWhenIdle(boolean autoAfk) { 
        this.autoAfkWhenIdle = autoAfk; 
        save();
    }
    
    public String getDefaultAfkWarp() { return defaultAfkWarp; }
    public void setDefaultAfkWarp(String warp) { 
        this.defaultAfkWarp = warp; 
        save();
    }
    
    public String getDefaultMiningWarp() { return defaultMiningWarp; }
    public void setDefaultMiningWarp(String warp) { 
        this.defaultMiningWarp = warp; 
        save();
    }
    
    public ScheduleEntry.EventType getLastScheduledType() { return lastScheduledType; }
    public void setLastScheduledType(ScheduleEntry.EventType type) { 
        this.lastScheduledType = type; 
        this.lastTypeChangeTime = System.currentTimeMillis();
    }
    
    public long getLastTypeChangeTime() { return lastTypeChangeTime; }
    
    // İç veri sınıfı
    private static class ScheduleData {
        List<ScheduleEntry> entries;
        boolean scheduleEnabled;
        boolean autoAfkWhenIdle;
        String defaultAfkWarp;
        String defaultMiningWarp;
    }
    
    // Alias for GUI compatibility
    public void saveSchedule() {
        save();
    }
}
