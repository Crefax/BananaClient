package com.muzmod.state.impl;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
import com.muzmod.state.AbstractState;
import com.muzmod.util.InputSimulator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemPickaxe;

/**
 * Repair State v1.6.7
 * 
 * Basitleştirilmiş tamir sistemi.
 * Her adım, bir sonraki adıma geçmeden önce configden bekleme süresi bekler.
 * Repair sonrası önceki state'e döner (Mining, Obsidyen vb.)
 */
public class RepairState extends AbstractState {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    
    // Adım takibi
    private int currentStep = 0;
    private long lastActionTime = 0;
    private int retryCount = 0;
    private boolean waitingForAction = false;
    private int confirmRetryCount = 0; // Onayla butonu için retry sayacı
    
    // Önceki state hatırlama
    private String previousStateName = null;
    
    // Debug
    private String debugInfo = "";
    private float remainingWait = 0;
    
    // Adımlar (Her adım arasında bekleme var)
    private static final int STEP_INIT = 0;           // Başlangıç
    private static final int STEP_COMMAND = 1;        // /tamirci gönder
    private static final int STEP_WAIT_GUI1 = 2;      // İlk GUI bekle
    private static final int STEP_CLICK_REPAIR = 3;   // Tekli Onarım tıkla
    private static final int STEP_WAIT_GUI2 = 4;      // İkinci GUI bekle
    private static final int STEP_PUT_PICKAXE = 5;    // Kazmayı koy
    private static final int STEP_CLICK_CONFIRM = 6;  // Onayla tıkla
    private static final int STEP_FINISH = 7;         // Bitir
    private static final int STEP_DONE = 8;           // Tamamlandı
    private static final int STEP_FAILED = 9;         // Başarısız
    
    public RepairState() {
        this.status = "Tamir hazır";
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        // Önceki state'i kaydet (dönüş için)
        com.muzmod.state.IState prevState = MuzMod.instance.getStateManager().getPreviousState();
        if (prevState != null) {
            String prevName = prevState.getName().toLowerCase();
            // Mining veya Obsidyen'den geliyorsak hatırla
            if (prevName.equals("mining") || prevName.equals("obsidyen")) {
                previousStateName = prevName.equals("mining") ? "mining" : "obsidian";
                MuzMod.LOGGER.info("[Repair] Önceki state kaydedildi: " + previousStateName);
            } else {
                // Idle, AFK veya diğer state'lerden geliyorsak, repair sonrası bir yere gitme
                previousStateName = null;
                MuzMod.LOGGER.info("[Repair] Önceki state (" + prevName + ") repair sonrası dönülmeyecek");
            }
        } else {
            previousStateName = null;
        }
        
        currentStep = STEP_INIT;
        lastActionTime = System.currentTimeMillis();
        retryCount = 0;
        waitingForAction = false;
        confirmRetryCount = 0;
        debugInfo = "Başlatılıyor...";
        remainingWait = 0;
        
        InputSimulator.releaseAll();
        setStatus("Tamir başlıyor");
        
        MuzMod.LOGGER.info("[Repair] =============================");
        MuzMod.LOGGER.info("[Repair] REPAIR STATE BAŞLADI");
        MuzMod.LOGGER.info("[Repair] =============================");
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        InputSimulator.releaseAll();
        MuzMod.LOGGER.info("[Repair] REPAIR STATE KAPANDI");
    }
    
    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        ModConfig config = MuzMod.instance.getConfig();
        long now = System.currentTimeMillis();
        long elapsed = now - lastActionTime;
        long delay = (long)(config.getRepairClickDelay() * 1000); // saniyeden ms'ye
        
        // Kalan bekleme süresini hesapla
        if (elapsed < delay && currentStep != STEP_WAIT_GUI1 && currentStep != STEP_WAIT_GUI2) {
            remainingWait = (delay - elapsed) / 1000.0f;
        } else {
            remainingWait = 0;
        }
        
        switch (currentStep) {
            case STEP_INIT:
                doStepInit(delay, elapsed);
                break;
                
            case STEP_COMMAND:
                doStepCommand(delay, elapsed, config);
                break;
                
            case STEP_WAIT_GUI1:
                doStepWaitGui1(delay, elapsed);
                break;
                
            case STEP_CLICK_REPAIR:
                doStepClickRepair(delay, elapsed, config);
                break;
                
            case STEP_WAIT_GUI2:
                doStepWaitGui2(delay, elapsed);
                break;
                
            case STEP_PUT_PICKAXE:
                doStepPutPickaxe(delay, elapsed);
                break;
                
            case STEP_CLICK_CONFIRM:
                doStepClickConfirm(delay, elapsed, config);
                break;
                
            case STEP_FINISH:
                doStepFinish(delay, elapsed);
                break;
                
            case STEP_DONE:
                // Önceki state'e dön (varsa)
                if (previousStateName != null) {
                    debugInfo = "Tamamlandı! " + previousStateName + "'e dönülüyor...";
                    setStatus(debugInfo);
                    MuzMod.LOGGER.info("[Repair] " + previousStateName + "'e dönülüyor");
                    MuzMod.instance.getStateManager().forceState(previousStateName);
                } else {
                    // Önceki state yoksa idle'a geç
                    debugInfo = "Tamamlandı! Idle'a dönülüyor...";
                    setStatus(debugInfo);
                    MuzMod.LOGGER.info("[Repair] Önceki state yok, Idle'a dönülüyor");
                    MuzMod.instance.getStateManager().forceState("idle");
                }
                break;
                
            case STEP_FAILED:
                debugInfo = "BAŞARISIZ! Idle'a geçiliyor...";
                setStatus(debugInfo);
                MuzMod.LOGGER.error("[Repair] Tamir başarısız!");
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                }
                MuzMod.instance.getStateManager().forceState("idle");
                break;
        }
    }
    
    // =========================================
    // ADIM FONKSİYONLARI
    // =========================================
    
    private void doStepInit(long delay, long elapsed) {
        debugInfo = "Adım 0: Başlangıç beklemesi";
        setStatus(debugInfo);
        
        if (elapsed >= 1000) { // 1 saniye başlangıç beklemesi
            MuzMod.LOGGER.info("[Repair] Adım 0 tamamlandı, adım 1'e geçiliyor");
            goToStep(STEP_COMMAND);
        }
    }
    
    private void doStepCommand(long delay, long elapsed, ModConfig config) {
        debugInfo = String.format("Adım 1: Bekleme (%.1f sn)", remainingWait);
        setStatus(debugInfo);
        
        // Önce bekle, sonra komutu gönder
        if (elapsed >= delay) {
            String cmd = config.getRepairCommand();
            MuzMod.LOGGER.info("[Repair] Komut gönderiliyor: " + cmd);
            mc.thePlayer.sendChatMessage(cmd);
            debugInfo = "Adım 1: Komut gönderildi!";
            setStatus(debugInfo);
            goToStep(STEP_WAIT_GUI1);
        }
    }
    
    private void doStepWaitGui1(long delay, long elapsed) {
        debugInfo = "Adım 2: GUI açılması bekleniyor...";
        setStatus(debugInfo);
        
        if (mc.currentScreen instanceof GuiChest) {
            MuzMod.LOGGER.info("[Repair] İlk GUI açıldı!");
            goToStep(STEP_CLICK_REPAIR);
        } else if (elapsed > 5000) {
            MuzMod.LOGGER.warn("[Repair] GUI açılmadı, timeout!");
            retryOrFail();
        }
    }
    
    private void doStepClickRepair(long delay, long elapsed, ModConfig config) {
        debugInfo = String.format("Adım 3: Bekleme (%.1f sn)", remainingWait);
        setStatus(debugInfo);
        
        if (elapsed < delay) {
            return; // Bekle
        }
        
        // GUI kontrol
        if (!(mc.currentScreen instanceof GuiChest)) {
            MuzMod.LOGGER.warn("[Repair] GUI kapandı!");
            retryOrFail();
            return;
        }
        
        int slot = config.getRepairSlot();
        MuzMod.LOGGER.info("[Repair] Tekli Onarım tıklanıyor, slot: " + slot);
        clickSlot(slot);
        debugInfo = "Adım 3: Slot " + slot + " tıklandı!";
        setStatus(debugInfo);
        goToStep(STEP_WAIT_GUI2);
    }
    
    private void doStepWaitGui2(long delay, long elapsed) {
        debugInfo = "Adım 4: İkinci GUI bekleniyor...";
        setStatus(debugInfo);
        
        // En az delay kadar bekle
        if (elapsed < delay) {
            return;
        }
        
        if (mc.currentScreen instanceof GuiChest) {
            MuzMod.LOGGER.info("[Repair] İkinci GUI hazır!");
            goToStep(STEP_PUT_PICKAXE);
        } else if (elapsed > 5000) {
            MuzMod.LOGGER.warn("[Repair] İkinci GUI timeout!");
            retryOrFail();
        }
    }
    
    private void doStepPutPickaxe(long delay, long elapsed) {
        debugInfo = String.format("Adım 5: Bekleme (%.1f sn)", remainingWait);
        setStatus(debugInfo);
        
        if (elapsed < delay) {
            return; // Bekle
        }
        
        // GUI kontrol
        if (!(mc.currentScreen instanceof GuiChest)) {
            MuzMod.LOGGER.warn("[Repair] GUI kapandı!");
            retryOrFail();
            return;
        }
        
        int pickaxeSlot = findPickaxeSlot();
        if (pickaxeSlot == -1) {
            MuzMod.LOGGER.error("[Repair] Kazma bulunamadı!");
            currentStep = STEP_FAILED;
            return;
        }
        
        MuzMod.LOGGER.info("[Repair] Kazma taşınıyor, slot: " + pickaxeSlot);
        
        // Shift+click ile taşı
        GuiChest chest = (GuiChest) mc.currentScreen;
        ContainerChest container = (ContainerChest) chest.inventorySlots;
        mc.playerController.windowClick(container.windowId, pickaxeSlot, 0, 1, mc.thePlayer);
        
        debugInfo = "Adım 5: Kazma taşındı!";
        setStatus(debugInfo);
        goToStep(STEP_CLICK_CONFIRM);
    }
    
    private void doStepClickConfirm(long delay, long elapsed, ModConfig config) {
        debugInfo = String.format("Adım 6: Bekleme (%.1f sn)", remainingWait);
        setStatus(debugInfo);
        
        if (elapsed < delay) {
            return; // Bekle
        }
        
        // GUI kontrol
        if (!(mc.currentScreen instanceof GuiChest)) {
            MuzMod.LOGGER.warn("[Repair] GUI kapandı!");
            retryOrFail();
            return;
        }
        
        int confirmSlot = findConfirmSlot();
        if (confirmSlot == -1) {
            confirmSlot = config.getRepairSlot(); // Fallback
        }
        
        MuzMod.LOGGER.info("[Repair] Onayla tıklanıyor, slot: " + confirmSlot);
        clickSlot(confirmSlot);
        debugInfo = "Adım 6: Onay tıklandı!";
        setStatus(debugInfo);
        goToStep(STEP_FINISH);
    }
    
    private void doStepFinish(long delay, long elapsed) {
        debugInfo = "Adım 7: Tamamlanıyor...";
        setStatus(debugInfo);
        
        // GUI kapandıysa tamir başarılı, hemen mining'e dön
        if (mc.currentScreen == null) {
            MuzMod.LOGGER.info("[Repair] GUI kapandı, tamir başarılı!");
            currentStep = STEP_DONE;
            return;
        }
        
        // GUI hala açıksa kontrol et
        if (mc.currentScreen instanceof GuiChest) {
            // Onay beklemesi için delay kadar bekle
            if (elapsed < delay) {
                return;
            }
            
            // Hala GUI açık, Onayla butonu var mı kontrol et
            int confirmSlot = findConfirmSlot();
            if (confirmSlot != -1 && confirmRetryCount < 3) {
                // Onayla butonu hala var, tekrar tıkla
                confirmRetryCount++;
                MuzMod.LOGGER.warn("[Repair] Onayla hala mevcut, tekrar tıklanıyor (deneme " + confirmRetryCount + "/3)");
                clickSlot(confirmSlot);
                lastActionTime = System.currentTimeMillis(); // Timer'ı resetle
                return;
            }
            
            // 3 deneme yapıldı veya buton yok, zorla kapat
            if (elapsed >= delay * 2 || confirmRetryCount >= 3) {
                MuzMod.LOGGER.info("[Repair] GUI zorla kapatılıyor");
                mc.thePlayer.closeScreen();
                currentStep = STEP_DONE;
            }
        } else {
            // Farklı bir ekran açık, kapat
            mc.thePlayer.closeScreen();
            currentStep = STEP_DONE;
        }
    }
    
    // =========================================
    // YARDIMCI FONKSİYONLAR
    // =========================================
    
    private void goToStep(int step) {
        currentStep = step;
        lastActionTime = System.currentTimeMillis();
        MuzMod.LOGGER.info("[Repair] -> Adım " + step);
    }
    
    private void clickSlot(int slot) {
        if (!(mc.currentScreen instanceof GuiChest)) return;
        
        GuiChest chest = (GuiChest) mc.currentScreen;
        ContainerChest container = (ContainerChest) chest.inventorySlots;
        mc.playerController.windowClick(container.windowId, slot, 0, 0, mc.thePlayer);
    }
    
    private int findPickaxeSlot() {
        if (!(mc.currentScreen instanceof GuiChest)) return -1;
        
        GuiChest chest = (GuiChest) mc.currentScreen;
        ContainerChest container = (ContainerChest) chest.inventorySlots;
        int chestSize = container.getLowerChestInventory().getSizeInventory();
        
        // Player inventory'de kazma ara
        for (int i = chestSize; i < container.inventorySlots.size(); i++) {
            ItemStack stack = container.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemPickaxe) {
                return i;
            }
        }
        return -1;
    }
    
    private int findConfirmSlot() {
        if (!(mc.currentScreen instanceof GuiChest)) return -1;
        
        GuiChest chest = (GuiChest) mc.currentScreen;
        ContainerChest container = (ContainerChest) chest.inventorySlots;
        int chestSize = container.getLowerChestInventory().getSizeInventory();
        
        for (int i = 0; i < chestSize; i++) {
            ItemStack stack = container.getSlot(i).getStack();
            if (stack != null) {
                String name = stack.getDisplayName().toLowerCase();
                if (name.contains("onayla") || name.contains("confirm") || 
                    name.contains("tamam") || name.contains("evet")) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    private void retryOrFail() {
        ModConfig config = MuzMod.instance.getConfig();
        retryCount++;
        
        if (mc.currentScreen != null) {
            mc.thePlayer.closeScreen();
        }
        
        if (retryCount >= config.getRepairMaxRetries()) {
            MuzMod.LOGGER.warn("[Repair] Max retry aşıldı!");
            currentStep = STEP_FAILED;
        } else {
            MuzMod.LOGGER.info("[Repair] Retry " + retryCount + "/" + config.getRepairMaxRetries());
            goToStep(STEP_INIT);
        }
    }
    
    @Override
    public String getName() {
        return "Tamir";
    }
    
    @Override
    public boolean shouldActivate() {
        return false; // Manuel aktivasyon
    }
    
    @Override
    public int getPriority() {
        return 5;
    }
    
    // Debug getters
    public String getDebugInfo() { return debugInfo; }
    public int getCurrentStep() { return currentStep; }
    public float getRemainingWait() { return remainingWait; }
    
    /**
     * Kazmanın durability'sini kontrol et
     */
    public static boolean needsRepair(ModConfig config) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return false;
        
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemPickaxe)) {
            return false;
        }
        
        int maxDurability = heldItem.getMaxDamage();
        int currentDamage = heldItem.getItemDamage();
        int remainingDurability = maxDurability - currentDamage;
        
        return remainingDurability < config.getRepairDurabilityThreshold();
    }
}
