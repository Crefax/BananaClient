package com.muzmod.state;

import com.muzmod.MuzMod;

/**
 * Abstract base class for states with common functionality
 */
public abstract class AbstractState implements IState {
    
    protected boolean enabled = false;
    protected String status = "";
    protected long enabledTime = 0;
    
    @Override
    public void onEnable() {
        this.enabled = true;
        this.enabledTime = System.currentTimeMillis();
        this.status = "Başlatılıyor...";
        MuzMod.LOGGER.info("State enabled: " + getName());
    }
    
    @Override
    public void onDisable() {
        this.enabled = false;
        this.status = "Devre dışı";
        MuzMod.LOGGER.info("State disabled: " + getName());
    }
    
    @Override
    public String getStatus() {
        return status;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public long getEnabledDuration() {
        if (!enabled) return 0;
        return System.currentTimeMillis() - enabledTime;
    }
    
    protected void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Check if current time is within the specified range
     * Uses time offset from config to correct clock differences
     */
    protected boolean isTimeInRange(int startHour, int startMin, int endHour, int endMin) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        
        // Apply time offset from config
        int timeOffset = MuzMod.instance.getConfig().getTimeOffsetHours();
        int currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY) + timeOffset;
        int currentMin = cal.get(java.util.Calendar.MINUTE);
        
        // Handle hour overflow/underflow
        if (currentHour >= 24) currentHour -= 24;
        if (currentHour < 0) currentHour += 24;
        
        int currentTime = currentHour * 60 + currentMin;
        int startTime = startHour * 60 + startMin;
        int endTime = endHour * 60 + endMin;
        
        // Handle overnight ranges (e.g., 23:00 - 02:00)
        if (endTime < startTime) {
            return currentTime >= startTime || currentTime < endTime;
        }
        
        return currentTime >= startTime && currentTime < endTime;
    }
    
    /**
     * Get current time with offset applied
     */
    protected int[] getCurrentTimeWithOffset() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int timeOffset = MuzMod.instance.getConfig().getTimeOffsetHours();
        int currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY) + timeOffset;
        int currentMin = cal.get(java.util.Calendar.MINUTE);
        
        if (currentHour >= 24) currentHour -= 24;
        if (currentHour < 0) currentHour += 24;
        
        return new int[]{currentHour, currentMin};
    }
    
    protected String formatTime(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }
}
