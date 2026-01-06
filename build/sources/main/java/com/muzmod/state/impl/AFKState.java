package com.muzmod.state.impl;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
import com.muzmod.state.AbstractState;
import net.minecraft.client.Minecraft;

/**
 * AFK State - Warps to AFK area and waits
 */
public class AFKState extends AbstractState {
    
    private boolean hasWarped = false;
    private long warpDelay = 0;
    private static final long WARP_COOLDOWN = 2000; // 2 seconds
    
    public AFKState() {
        this.status = "AFK Bekliyor";
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        hasWarped = false;
        warpDelay = System.currentTimeMillis() + 1000; // 1 second delay before warp
        setStatus("AFK bölgesine gidiliyor...");
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        hasWarped = false;
    }
    
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        
        // Warp to AFK area if not done yet
        if (!hasWarped && System.currentTimeMillis() > warpDelay) {
            ModConfig config = MuzMod.instance.getConfig();
            String warpCommand = config.getAfkWarpCommand();
            
            mc.thePlayer.sendChatMessage(warpCommand);
            hasWarped = true;
            warpDelay = System.currentTimeMillis() + WARP_COOLDOWN;
            setStatus("AFK bölgesinde bekleniyor");
            MuzMod.LOGGER.info("Sent AFK warp command: " + warpCommand);
        }
        
        // Just wait - do nothing
        if (hasWarped) {
            long duration = getEnabledDuration() / 1000;
            setStatus("AFK: " + formatDuration(duration));
        }
    }
    
    @Override
    public String getName() {
        return "AFK";
    }
    
    @Override
    public boolean shouldActivate() {
        ModConfig config = MuzMod.instance.getConfig();
        return isTimeInRange(
            config.getAfkStartHour(),
            config.getAfkStartMinute(),
            config.getAfkEndHour(),
            config.getAfkEndMinute()
        );
    }
    
    @Override
    public int getPriority() {
        // Medium priority
        return 5;
    }
    
    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%d:%02d", minutes, secs);
    }
}
