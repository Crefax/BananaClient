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
 */
public class ScheduleManager {
    
    private List<ScheduleEntry> entries = new ArrayList<>();
    private boolean scheduleEnabled = true;
    private boolean autoAfkWhenIdle = true; // Boş zamanlarda otomatik AFK
    private String defaultAfkWarp = "/warp afk";
    private String defaultMiningWarp = "/warp maden";
    
    private File scheduleFile;
    private Gson gson;
    
    private ScheduleEntry.EventType lastScheduledType = null;
    private long lastTypeChangeTime = 0;
    
    public ScheduleManager(File configDir) {
        this.scheduleFile = new File(configDir, "schedule.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }
    
    /**
     * Şu anki zamana göre aktif etkinliği bul
     */
    public ScheduleEntry getCurrentEntry(int dayOfWeek, int hour, int minute) {
        if (!scheduleEnabled) return null;
        
        return entries.stream()
            .filter(e -> e.getDayOfWeek() == dayOfWeek && e.isActiveAt(hour, minute))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Şu anki zamana göre hangi state'te olmalı?
     */
    public ScheduleEntry.EventType getCurrentScheduledType(int dayOfWeek, int hour, int minute) {
        ScheduleEntry entry = getCurrentEntry(dayOfWeek, hour, minute);
        
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
        for (int day = 0; day < 5; day++) { // Pzt-Cum
            if (day != sourceDay) {
                clearDay(day);
                copyDay(sourceDay, day);
            }
        }
    }
    
    /**
     * Hafta sonu günlerine aynı programı uygula
     */
    public void applyToWeekends(int sourceDay) {
        for (int day = 5; day < 7; day++) { // Cmt-Paz
            if (day != sourceDay) {
                clearDay(day);
                copyDay(sourceDay, day);
            }
        }
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
        
        // Mining saatleri (1 dk erken git + 20 dk kal = 21 dk)
        // Hafta içi (Pzt-Cum, 0-4): 03:40, 15:40, 21:40, 23:40 -> 1 dk erken başla
        for (int day = 0; day < 5; day++) {
            entries.add(new ScheduleEntry(day, 3, 39, 4, 0, ScheduleEntry.EventType.MINING));   // 03:40 eventi
            entries.add(new ScheduleEntry(day, 15, 39, 16, 0, ScheduleEntry.EventType.MINING)); // 15:40 eventi
            entries.add(new ScheduleEntry(day, 21, 39, 22, 0, ScheduleEntry.EventType.MINING)); // 21:40 eventi
            entries.add(new ScheduleEntry(day, 23, 39, 0, 0, ScheduleEntry.EventType.MINING));  // 23:40 eventi
        }
        
        // Hafta sonu (Cmt-Paz, 5-6): 03:40, 11:40, 15:40, 21:40, 23:40 -> 1 dk erken başla
        for (int day = 5; day < 7; day++) {
            entries.add(new ScheduleEntry(day, 3, 39, 4, 0, ScheduleEntry.EventType.MINING));   // 03:40 eventi
            entries.add(new ScheduleEntry(day, 11, 39, 12, 0, ScheduleEntry.EventType.MINING)); // 11:40 eventi
            entries.add(new ScheduleEntry(day, 15, 39, 16, 0, ScheduleEntry.EventType.MINING)); // 15:40 eventi
            entries.add(new ScheduleEntry(day, 21, 39, 22, 0, ScheduleEntry.EventType.MINING)); // 21:40 eventi
            entries.add(new ScheduleEntry(day, 23, 39, 0, 0, ScheduleEntry.EventType.MINING));  // 23:40 eventi
        }
        
        // OX Event saatleri (1 dk erken git + 10 dk kal = :49 - :59)
        // Hafta içi (Pzt-Cum): 17:50, 00:05 -> 1 dk erken başla, 10 dk
        for (int day = 0; day < 5; day++) {
            entries.add(new ScheduleEntry(day, 0, 4, 0, 14, ScheduleEntry.EventType.OX));    // 00:05 eventi -> 00:04-00:14
            entries.add(new ScheduleEntry(day, 17, 49, 17, 59, ScheduleEntry.EventType.OX)); // 17:50 eventi -> 17:49-17:59
        }
        
        // Hafta sonu (Cmt-Paz): 11:00, 17:50, 00:05 -> 1 dk erken başla, 10 dk
        for (int day = 5; day < 7; day++) {
            entries.add(new ScheduleEntry(day, 0, 4, 0, 14, ScheduleEntry.EventType.OX));    // 00:05 eventi -> 00:04-00:14
            entries.add(new ScheduleEntry(day, 10, 59, 11, 9, ScheduleEntry.EventType.OX));  // 11:00 eventi -> 10:59-11:09
            entries.add(new ScheduleEntry(day, 17, 49, 17, 59, ScheduleEntry.EventType.OX)); // 17:50 eventi -> 17:49-17:59
        }
        
        save();
        MuzMod.LOGGER.info("[Schedule] Created default schedule with Mining and OX events");
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
}
