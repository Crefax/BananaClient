package com.muzmod.state.impl;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
import com.muzmod.state.AbstractState;
import com.muzmod.util.InputSimulator;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;

/**
 * Safe State - Güvenli Mod
 * 
 * Kritik durumlarda aktif olur:
 * - Kazma durability çok düşük ve tamir çalışmıyor
 * - Kazma kırılmak üzere
 * - Kritik hatalar
 * 
 * Bu state'de:
 * - Tüm tuşlar bırakılır
 * - Kazma korunur
 * - Kullanıcı müdahalesi beklenir
 */
public class SafeState extends AbstractState {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    
    private String safeReason = "";
    private long enterTime = 0;
    private int blinkCounter = 0;
    
    // Neden güvenli moda geçildi
    public enum SafeReason {
        PICKAXE_CRITICAL("Kazma kritik durumda!"),
        PICKAXE_MISSING("Kazma bulunamadı!"),
        REPAIR_FAILED("Tamir başarısız!"),
        UNKNOWN("Bilinmeyen hata");
        
        private final String message;
        SafeReason(String message) { this.message = message; }
        public String getMessage() { return message; }
    }
    
    private SafeReason reason = SafeReason.UNKNOWN;
    
    public SafeState() {
        this.status = "Güvenli Mod";
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        enterTime = System.currentTimeMillis();
        blinkCounter = 0;
        
        // Tüm tuşları bırak
        InputSimulator.releaseAll();
        
        MuzMod.LOGGER.warn("[SafeState] ================================");
        MuzMod.LOGGER.warn("[SafeState] GÜVENLİ MOD AKTİF!");
        MuzMod.LOGGER.warn("[SafeState] Sebep: " + reason.getMessage());
        MuzMod.LOGGER.warn("[SafeState] ================================");
        
        setStatus("⚠ " + reason.getMessage());
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        reason = SafeReason.UNKNOWN;
        safeReason = "";
    }
    
    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        // Her zaman tuşları bırak
        InputSimulator.releaseAll();
        
        // Blink efekti için
        blinkCounter++;
        
        // Status güncelle (yanıp sönen uyarı)
        if ((blinkCounter / 20) % 2 == 0) {
            setStatus("⚠ " + reason.getMessage());
        } else {
            setStatus("§c⚠ " + reason.getMessage() + " §7[RSHIFT ile çık]");
        }
        
        // Kazma durumu kontrol et - düzeldi mi?
        ModConfig config = MuzMod.instance.getConfig();
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        
        if (reason == SafeReason.PICKAXE_CRITICAL || reason == SafeReason.PICKAXE_MISSING) {
            // Kazma var mı ve durability yeterli mi kontrol et
            if (heldItem != null && heldItem.getItem() instanceof ItemPickaxe) {
                int maxDurability = heldItem.getMaxDamage();
                int currentDamage = heldItem.getItemDamage();
                int remaining = maxDurability - currentDamage;
                
                // Eğer durability threshold'un 2 katından fazlaysa, güvenli
                if (remaining > config.getRepairDurabilityThreshold() * 2) {
                    MuzMod.LOGGER.info("[SafeState] Kazma durumu düzeldi, Mining'e dönülüyor");
                    MuzMod.instance.getStateManager().forceState("mining");
                    return;
                }
            }
        }
    }
    
    @Override
    public String getName() {
        return "Güvenli";
    }
    
    @Override
    public boolean shouldActivate() {
        return false; // Manuel aktivasyon
    }
    
    @Override
    public int getPriority() {
        return 100; // En yüksek öncelik
    }
    
    // Setter for reason
    public void setReason(SafeReason reason) {
        this.reason = reason;
    }
    
    public SafeReason getReason() {
        return reason;
    }
}
