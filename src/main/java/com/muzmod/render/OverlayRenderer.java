package com.muzmod.render;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
import com.muzmod.schedule.ScheduleEntry;
import com.muzmod.schedule.ScheduleManager;
import com.muzmod.state.IState;
import com.muzmod.state.impl.RepairState;
import com.muzmod.state.impl.SafeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Renders the status overlay on screen
 */
public class OverlayRenderer {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    
    public void render() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.gameSettings.showDebugInfo) return;
        
        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRendererObj;
        
        int x = 5;
        int y = 5;
        int lineHeight = 10;
        
        // Title
        String title = "§6§lMuzMod";
        fr.drawStringWithShadow(title, x, y, 0xFFFFFF);
        y += lineHeight + 2;
        
        // Bot Status
        boolean botEnabled = MuzMod.instance.isBotEnabled();
        String botStatus = botEnabled ? "§aAktif" : "§cDevre Dışı";
        fr.drawStringWithShadow("§7Bot: " + botStatus, x, y, 0xFFFFFF);
        y += lineHeight;
        
        // Current Time (with offset)
        ModConfig config = MuzMod.instance.getConfig();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int offsetHours = config.getTimeOffsetHours();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY) + offsetHours;
        if (hour >= 24) hour -= 24;
        if (hour < 0) hour += 24;
        int minute = cal.get(java.util.Calendar.MINUTE);
        int second = cal.get(java.util.Calendar.SECOND);
        String currentTime = String.format("%02d:%02d:%02d", hour, minute, second);
        String offsetStr = offsetHours >= 0 ? "+" + offsetHours : String.valueOf(offsetHours);
        fr.drawStringWithShadow("§7Saat: §f" + currentTime + " §8(UTC" + offsetStr + ")", x, y, 0xFFFFFF);
        y += lineHeight;
        
        // Day of week
        int javaDow = cal.get(java.util.Calendar.DAY_OF_WEEK);
        int dayOfWeek = (javaDow == java.util.Calendar.SUNDAY) ? 6 : javaDow - 2;
        String dayName = ScheduleEntry.getDayName(dayOfWeek);
        fr.drawStringWithShadow("§7Gün: §f" + dayName, x, y, 0xFFFFFF);
        y += lineHeight;
        
        // Current State
        if (botEnabled) {
            IState currentState = MuzMod.instance.getStateManager().getCurrentState();
            if (currentState != null) {
                String stateName = currentState.getName();
                int stateColor = getStateColor(stateName);
                fr.drawStringWithShadow("§7Durum: §" + Integer.toHexString(stateColor) + stateName, x, y, 0xFFFFFF);
                y += lineHeight;
                
                // State status
                String status = currentState.getStatus();
                if (status != null && !status.isEmpty()) {
                    fr.drawStringWithShadow("§8" + status, x, y, 0xFFFFFF);
                    y += lineHeight;
                }
                
                // RepairState debug info
                if (currentState instanceof RepairState) {
                    RepairState repair = (RepairState) currentState;
                    y += 3;
                    fr.drawStringWithShadow("§c§l=== TAMİR DEBUG ===", x, y, 0xFFFFFF);
                    y += lineHeight;
                    fr.drawStringWithShadow("§eAdım: §f" + repair.getCurrentStep() + "/8", x, y, 0xFFFFFF);
                    y += lineHeight;
                    fr.drawStringWithShadow("§eBekleme: §f" + String.format("%.1f", repair.getRemainingWait()) + " sn", x, y, 0xFFFFFF);
                    y += lineHeight;
                    fr.drawStringWithShadow("§eInfo: §f" + repair.getDebugInfo(), x, y, 0xFFFFFF);
                    y += lineHeight;
                }
                
                // SafeState warning
                if (currentState instanceof SafeState) {
                    SafeState safe = (SafeState) currentState;
                    y += 3;
                    fr.drawStringWithShadow("§c§l⚠ GÜVENLİ MOD ⚠", x, y, 0xFFFFFF);
                    y += lineHeight;
                    fr.drawStringWithShadow("§c" + safe.getReason().getMessage(), x, y, 0xFFFFFF);
                    y += lineHeight;
                    fr.drawStringWithShadow("§7RSHIFT ile GUI'yi aç", x, y, 0xFFFFFF);
                    y += lineHeight;
                }
            }
        }
        
        // Pickaxe durability display
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem != null && heldItem.getItem() instanceof ItemPickaxe) {
            int maxDur = heldItem.getMaxDamage();
            int curDmg = heldItem.getItemDamage();
            int remaining = maxDur - curDmg;
            int percent = (remaining * 100) / maxDur;
            
            String durColor = percent > 50 ? "§a" : (percent > 25 ? "§e" : "§c");
            fr.drawStringWithShadow("§7Kazma: " + durColor + remaining + "§8/" + maxDur + " §7(" + percent + "%)", x, y, 0xFFFFFF);
            y += lineHeight;
        }
        
        // Separator
        y += 3;
        fr.drawStringWithShadow("§8───────────", x, y, 0xFFFFFF);
        y += lineHeight;
        
        // Dynamic Schedule Preview from ScheduleManager
        fr.drawStringWithShadow("§7Görev Takvimi:", x, y, 0xFFFFFF);
        y += lineHeight;
        
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        if (schedule != null && schedule.isScheduleEnabled()) {
            // Get today's entries
            int javaDowNow = cal.get(java.util.Calendar.DAY_OF_WEEK);
            int todayDow = (javaDowNow == java.util.Calendar.SUNDAY) ? 6 : javaDowNow - 2;
            List<ScheduleEntry> todayEntries = schedule.getEntriesForDay(todayDow);
            
            int currentMinutes = hour * 60 + minute;
            int shownCount = 0;
            int maxShow = 5; // En fazla 5 görev göster
            
            for (ScheduleEntry entry : todayEntries) {
                if (shownCount >= maxShow) break;
                
                int startMin = entry.getStartTimeMinutes();
                int endMin = entry.getEndHour() * 60 + entry.getEndMinute();
                // Gece yarısını geçen durumlar
                if (endMin < startMin) endMin += 24 * 60;
                int currentMinAdj = currentMinutes;
                if (endMin > 24 * 60 && currentMinutes < startMin) currentMinAdj += 24 * 60;
                
                String icon = getEventIcon(entry.getEventType());
                String timeRange = entry.getTimeRange();
                String eventName = entry.getEventType().getDisplayName();
                
                String line;
                if (currentMinAdj >= startMin && currentMinAdj < endMin) {
                    // Devam eden - yeşil arka plan efekti
                    line = "§a▶ " + icon + " " + timeRange + " §a" + eventName;
                } else if (currentMinAdj >= endMin) {
                    // Bitmiş - üstü çizili gri
                    line = "§8§m  " + timeRange + " " + eventName + "§r §8✓";
                } else {
                    // Gelecek - gri
                    line = "§7○ " + icon + " " + timeRange + " §7" + eventName;
                }
                
                fr.drawStringWithShadow(line, x + 5, y, 0xFFFFFF);
                y += lineHeight;
                shownCount++;
            }
            
            if (todayEntries.isEmpty()) {
                fr.drawStringWithShadow("§8  Bugün görev yok", x + 5, y, 0xFFFFFF);
                y += lineHeight;
            }
        } else {
            fr.drawStringWithShadow("§8  Zamanlama kapalı", x + 5, y, 0xFFFFFF);
            y += lineHeight;
        }
        
        // Keybind hint
        y += 5;
        fr.drawStringWithShadow("§8[RSHIFT] Menü", x, y, 0xFFFFFF);
    }
    
    private String getEventIcon(ScheduleEntry.EventType type) {
        switch (type) {
            case MINING: return "§6⛏";
            case AFK: return "§b⏸";
            case REPAIR: return "§e🔧";
            case OX: return "§d";
            default: return "§7○";
        }
    }
    
    private int getStateColor(String stateName) {
        switch (stateName) {
            case "Mining":
                return 0xE; // Yellow
            case "AFK":
                return 0xB; // Aqua
            case "Idle":
                return 0x7; // Gray
            case "Tamir":
                return 0x6; // Gold
            case "Güvenli":
                return 0xC; // Red
            default:
                return 0xF; // White
        }
    }
}
