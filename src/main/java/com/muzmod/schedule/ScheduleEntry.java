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
    private int endHour;
    private int endMinute;
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
    }
    
    public ScheduleEntry(int dayOfWeek, int startHour, int startMinute, int endHour, int endMinute, EventType eventType) {
        this();
        this.dayOfWeek = dayOfWeek;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.endHour = endHour;
        this.endMinute = endMinute;
        this.eventType = eventType;
    }
    
    /**
     * Verilen saat bu etkinliğin içinde mi?
     */
    public boolean isActiveAt(int hour, int minute) {
        if (!enabled) return false;
        
        int currentTime = hour * 60 + minute;
        int startTime = startHour * 60 + startMinute;
        int endTime = endHour * 60 + endMinute;
        
        // Gece yarısını geçen durumlar için (örn: 23:00 - 02:00)
        if (endTime < startTime) {
            return currentTime >= startTime || currentTime < endTime;
        }
        
        return currentTime >= startTime && currentTime < endTime;
    }
    
    /**
     * Başlangıç saatini dakika cinsinden döndürür (sıralama için)
     */
    public int getStartTimeMinutes() {
        return startHour * 60 + startMinute;
    }
    
    /**
     * Formatlanmış saat aralığı
     */
    public String getTimeRange() {
        return String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute);
    }
    
    /**
     * Başlangıç saati string formatında
     */
    public String getStartTimeStr() {
        return String.format("%02d:%02d", startHour, startMinute);
    }
    
    /**
     * Bitiş saati string formatında
     */
    public String getEndTimeStr() {
        return String.format("%02d:%02d", endHour, endMinute);
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
    
    public int getEndHour() { return endHour; }
    public void setEndHour(int endHour) { this.endHour = endHour; }
    
    public int getEndMinute() { return endMinute; }
    public void setEndMinute(int endMinute) { this.endMinute = endMinute; }
    
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
