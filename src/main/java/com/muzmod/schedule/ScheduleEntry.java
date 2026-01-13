package com.muzmod.schedule;

/**
 * Tek bir zamanlama girişi
 * Bir günde birden fazla etkinlik olabilir
 */
public class ScheduleEntry {
    
    public enum EventType {
        MINING("Maden", 0xFFFF9800),      // Turuncu
        AFK("AFK", 0xFF2196F3),            // Mavi
        REPAIR("Tamir", 0xFFFFEB3B),       // Sarı
        OX("OX Event", 0xFF9C27B0),        // Mor
        IDLE("Bosta", 0xFF9E9E9E);         // Gri
        
        private final String displayName;
        private final int color;
        
        EventType(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() { return displayName; }
        public int getColor() { return color; }
    }
    
    // Gün: 0=Pazartesi, 1=Salı, 2=Çarşamba, 3=Perşembe, 4=Cuma, 5=Cumartesi, 6=Pazar
    private int dayOfWeek;
    private int startHour;
    private int startMinute;
    private int startSecond;
    private int endHour;
    private int endMinute;
    private int endSecond;
    private EventType eventType;
    private String customWarpCommand; // Her etkinlik için özel warp komutu
    private boolean enabled;
    
    // Benzersiz ID
    private long id;
    
    public ScheduleEntry() {
        this.id = System.currentTimeMillis();
        this.enabled = true;
        this.eventType = EventType.MINING;
        this.customWarpCommand = "";
        this.startSecond = 0;
        this.endSecond = 0;
    }
    
    public ScheduleEntry(int dayOfWeek, int startHour, int startMinute, int endHour, int endMinute, EventType eventType) {
        this();
        this.dayOfWeek = dayOfWeek;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.startSecond = 0;
        this.endHour = endHour;
        this.endMinute = endMinute;
        this.endSecond = 0;
        this.eventType = eventType;
    }
    
    public ScheduleEntry(int dayOfWeek, int startHour, int startMinute, int startSecond, int endHour, int endMinute, int endSecond, EventType eventType) {
        this();
        this.dayOfWeek = dayOfWeek;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.startSecond = startSecond;
        this.endHour = endHour;
        this.endMinute = endMinute;
        this.endSecond = endSecond;
        this.eventType = eventType;
    }
    
    /**
     * Verilen saat bu etkinliğin içinde mi?
     */
    public boolean isActiveAt(int hour, int minute) {
        return isActiveAt(hour, minute, 0);
    }
    
    /**
     * Verilen saat ve saniye bu etkinliğin içinde mi?
     */
    public boolean isActiveAt(int hour, int minute, int second) {
        if (!enabled) return false;
        
        int currentTime = hour * 3600 + minute * 60 + second;
        int startTime = startHour * 3600 + startMinute * 60 + startSecond;
        int endTime = endHour * 3600 + endMinute * 60 + endSecond;
        
        // Gece yarısını geçen durumlar için (örn: 23:00 - 02:00)
        if (endTime < startTime) {
            return currentTime >= startTime || currentTime < endTime;
        }
        
        return currentTime >= startTime && currentTime < endTime;
    }
    
    /**
     * Başlangıç saatini saniye cinsinden döndürür (sıralama için)
     */
    public int getStartTimeSeconds() {
        return startHour * 3600 + startMinute * 60 + startSecond;
    }
    
    /**
     * Başlangıç saatini dakika cinsinden döndürür (geriye uyumluluk için)
     */
    public int getStartTimeMinutes() {
        return startHour * 60 + startMinute;
    }
    
    /**
     * Formatlanmış saat aralığı (saniye dahil)
     */
    public String getTimeRange() {
        if (startSecond == 0 && endSecond == 0) {
            return String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute);
        }
        return String.format("%02d:%02d:%02d - %02d:%02d:%02d", startHour, startMinute, startSecond, endHour, endMinute, endSecond);
    }
    
    /**
     * Başlangıç saati string formatında (saniye dahil)
     */
    public String getStartTimeStr() {
        if (startSecond == 0) {
            return String.format("%02d:%02d", startHour, startMinute);
        }
        return String.format("%02d:%02d:%02d", startHour, startMinute, startSecond);
    }
    
    /**
     * Bitiş saati string formatında (saniye dahil)
     */
    public String getEndTimeStr() {
        if (endSecond == 0) {
            return String.format("%02d:%02d", endHour, endMinute);
        }
        return String.format("%02d:%02d:%02d", endHour, endMinute, endSecond);
    }
    
    /**
     * Gün adını döndürür
     */
    public static String getDayName(int day) {
        String[] days = {"Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"};
        if (day >= 0 && day < days.length) {
            return days[day];
        }
        return "?";
    }
    
    /**
     * Kısa gün adı
     */
    public static String getDayShortName(int day) {
        String[] days = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};
        if (day >= 0 && day < days.length) {
            return days[day];
        }
        return "?";
    }
    
    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    
    public int getStartHour() { return startHour; }
    public void setStartHour(int startHour) { this.startHour = startHour; }
    
    public int getStartMinute() { return startMinute; }
    public void setStartMinute(int startMinute) { this.startMinute = startMinute; }
    
    public int getStartSecond() { return startSecond; }
    public void setStartSecond(int startSecond) { this.startSecond = startSecond; }
    
    public int getEndHour() { return endHour; }
    public void setEndHour(int endHour) { this.endHour = endHour; }
    
    public int getEndMinute() { return endMinute; }
    public void setEndMinute(int endMinute) { this.endMinute = endMinute; }
    
    public int getEndSecond() { return endSecond; }
    public void setEndSecond(int endSecond) { this.endSecond = endSecond; }
    
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    
    public String getCustomWarpCommand() { return customWarpCommand; }
    public void setCustomWarpCommand(String customWarpCommand) { this.customWarpCommand = customWarpCommand; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s %s", getDayShortName(dayOfWeek), getTimeRange(), eventType.getDisplayName());
    }
}
