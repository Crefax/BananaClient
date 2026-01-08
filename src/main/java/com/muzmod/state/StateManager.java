package com.muzmod.state;

import com.muzmod.MuzMod;
import com.muzmod.gui.MuzModGui;
import com.muzmod.gui.MuzModGuiModern;
import com.muzmod.navigation.NavigationManager;
import com.muzmod.schedule.ScheduleEntry;
import com.muzmod.schedule.ScheduleManager;
import com.muzmod.state.impl.AFKState;
import com.muzmod.state.impl.IdleState;
import com.muzmod.state.impl.MiningState;
import com.muzmod.state.impl.OXState;
import com.muzmod.state.impl.RepairState;
import com.muzmod.state.impl.SafeState;
import net.minecraft.client.gui.GuiChat;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

/**
 * Manages all bot states and handles transitions based on time
 */
public class StateManager {
    
    private final List<IState> states = new ArrayList<>();
    private IState currentState = null;
    private IState previousState = null;
    
    private IdleState idleState;
    private AFKState afkState;
    private MiningState miningState;
    private RepairState repairState;
    private SafeState safeState;
    private OXState oxState;
    
    private int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20; // Check every second (20 ticks)
    
    // Schedule tabanlı state geçişi
    private boolean useScheduleBasedTransition = true;
    private ScheduleEntry.EventType lastScheduleType = null;
    
    // Manuel kontrol - true olduğunda otomatik geçiş yapma
    private boolean manualOverride = false;
    private long manualOverrideTime = 0;
    
    public StateManager() {
        initStates();
    }
    
    private void initStates() {
        idleState = new IdleState();
        afkState = new AFKState();
        miningState = new MiningState();
        repairState = new RepairState();
        safeState = new SafeState();
        oxState = new OXState();
        
        states.add(idleState);
        states.add(afkState);
        states.add(miningState);
        states.add(repairState);
        states.add(safeState);
        states.add(oxState);
        
        // Sort by priority (highest first)
        states.sort(Comparator.comparingInt(IState::getPriority).reversed());
        
        // Start with idle state
        currentState = idleState;
        currentState.onEnable();
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!MuzMod.instance.isBotEnabled()) return;
        
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        // GUI kontrolü - BananaClient GUI'leri ve chat açıkken devam et
        // Diğer Minecraft ekranları (envanter, ESC menü vb.) açıkken durdur
        // NOT: Repair state GUI içinde çalışır, bu yüzden Repair'da GUI kontrolü yapılmaz
        if (mc.currentScreen != null) {
            boolean isOurGui = mc.currentScreen instanceof MuzModGui || 
                              mc.currentScreen instanceof MuzModGuiModern;
            boolean isChat = mc.currentScreen instanceof GuiChat;
            boolean isRepairState = currentState != null && currentState.getName().equals("Tamir");
            
            // Bizim GUI'miz, chat veya Repair state ise devam et
            if (!isOurGui && !isChat && !isRepairState) {
                return; // Minecraft ekranı açık - mining durdur
            }
        }
        
        tickCounter++;
        
        // Update navigation system
        NavigationManager.getInstance().update();
        
        // Check for state transitions periodically
        if (tickCounter >= CHECK_INTERVAL) {
            tickCounter = 0;
            checkStateTransition();
        }
        
        // Run current state tick
        if (currentState != null) {
            currentState.onTick();
        }
    }
    
    private void checkStateTransition() {
        // Manuel override aktifse, otomatik geçiş yapma
        if (manualOverride) {
            return;
        }
        
        // SafeState aktifse, müdahale etme
        if (currentState instanceof SafeState) {
            return;
        }
        
        // RepairState aktifse ve tamamlanmadıysa, state değişimini engelle
        if (currentState instanceof RepairState) {
            RepairState repair = (RepairState) currentState;
            int step = repair.getCurrentStep();
            // STEP_DONE (8) veya STEP_FAILED (9) değilse devam et
            if (step < 8) {
                return; // RepairState çalışıyor, müdahale etme
            }
        }
        
        // Schedule tabanlı geçiş
        if (useScheduleBasedTransition) {
            checkScheduleBasedTransition();
            return;
        }
        
        // Eski mantık (schedule kapalıyken)
        IState newState = null;
        
        // Find highest priority state that should activate
        for (IState state : states) {
            if (state.shouldActivate()) {
                newState = state;
                break;
            }
        }
        
        // Default to idle if no state wants to activate
        if (newState == null) {
            newState = idleState;
        }
        
        // Transition if different state
        if (newState != currentState) {
            transitionTo(newState);
        }
    }
    
    /**
     * Schedule'a göre state geçişi yap
     */
    private void checkScheduleBasedTransition() {
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        if (schedule == null || !schedule.isScheduleEnabled()) {
            // Schedule kapalı, eski mantığa dön
            useScheduleBasedTransition = false;
            return;
        }
        
        // Şu anki zamanı al
        Calendar cal = Calendar.getInstance();
        int timeOffset = MuzMod.instance.getConfig().getTimeOffsetHours();
        int hour = (cal.get(Calendar.HOUR_OF_DAY) + timeOffset + 24) % 24;
        int minute = cal.get(Calendar.MINUTE);
        // Java Calendar: Pazar=1, Pazartesi=2, ... Cumartesi=7
        // Bizim sistem: Pazartesi=0, ... Pazar=6
        int javaDow = cal.get(Calendar.DAY_OF_WEEK);
        int dayOfWeek = (javaDow == Calendar.SUNDAY) ? 6 : javaDow - 2;
        
        // Schedule'dan aktif event tipini al
        ScheduleEntry.EventType scheduledType = schedule.getCurrentScheduledType(dayOfWeek, hour, minute);
        
        // RepairState'e geçiş gerekiyorsa öncelikli kontrol
        if (repairState.shouldActivate() && scheduledType == ScheduleEntry.EventType.MINING) {
            if (!(currentState instanceof RepairState)) {
                transitionTo(repairState);
            }
            return;
        }
        
        // Hedef state'i belirle
        IState targetState = getStateForEventType(scheduledType);
        
        // Tip değiştiyse veya mevcut state hedefle uyuşmuyorsa geçiş yap
        if (scheduledType != lastScheduleType || (targetState != null && targetState != currentState && !(currentState instanceof RepairState))) {
            lastScheduleType = scheduledType;
            schedule.setLastScheduledType(scheduledType);
            
            if (targetState != null && targetState != currentState) {
                MuzMod.LOGGER.info("[Schedule] Transitioning to: " + scheduledType.getDisplayName() + " (current state: " + (currentState != null ? currentState.getName() : "null") + ")");
                transitionTo(targetState);
            }
        }
    }
    
    /**
     * EventType'a göre state döndür
     */
    private IState getStateForEventType(ScheduleEntry.EventType type) {
        switch (type) {
            case MINING: return miningState;
            case AFK: return afkState;
            case REPAIR: return repairState;
            case OX: return oxState;
            case IDLE: 
            default: return idleState;
        }
    }
    
    public void transitionTo(IState newState) {
        if (currentState != null) {
            currentState.onDisable();
            previousState = currentState;
        }
        
        currentState = newState;
        
        if (currentState != null) {
            currentState.onEnable();
            MuzMod.LOGGER.info("State transition: " + 
                (previousState != null ? previousState.getName() : "null") + 
                " -> " + currentState.getName());
        }
    }
    
    public void forceState(IState state) {
        transitionTo(state);
    }
    
    /**
     * Force transition to a state by name
     */
    public void forceState(String stateName) {
        // Manuel override'u aktifleştir (schedule kapalıyken)
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        if (schedule == null || !schedule.isScheduleEnabled()) {
            manualOverride = true;
            manualOverrideTime = System.currentTimeMillis();
            MuzMod.LOGGER.info("[StateManager] Manual override enabled");
        }
        
        switch (stateName.toLowerCase()) {
            case "idle":
                manualOverride = false; // Idle'a geçince override kapat
                transitionTo(idleState);
                break;
            case "afk":
                transitionTo(afkState);
                break;
            case "mining":
                transitionTo(miningState);
                break;
            case "repair":
                transitionTo(repairState);
                break;
            case "safe":
                transitionTo(safeState);
                break;
            case "ox":
                transitionTo(oxState);
                break;
            default:
                MuzMod.LOGGER.warn("Unknown state: " + stateName);
        }
    }
    
    /**
     * Güvenli moda geç (kritik durumlar için)
     */
    public void enterSafeMode(SafeState.SafeReason reason) {
        safeState.setReason(reason);
        transitionTo(safeState);
    }
    
    public void stopCurrentState() {
        if (currentState != null) {
            currentState.onDisable();
            previousState = currentState;
        }
        currentState = idleState;
        currentState.onEnable();
    }
    
    // Getters
    public IState getCurrentState() {
        return currentState;
    }
    
    public IState getPreviousState() {
        return previousState;
    }
    
    public IdleState getIdleState() {
        return idleState;
    }
    
    public AFKState getAfkState() {
        return afkState;
    }
    
    public MiningState getMiningState() {
        return miningState;
    }
    
    public RepairState getRepairState() {
        return repairState;
    }
    
    public SafeState getSafeState() {
        return safeState;
    }
    
    public OXState getOxState() {
        return oxState;
    }
    
    public List<IState> getAllStates() {
        return new ArrayList<>(states);
    }
    
    public String getCurrentStateName() {
        return currentState != null ? currentState.getName() : "Yok";
    }
    
    public String getCurrentStateStatus() {
        return currentState != null ? currentState.getStatus() : "";
    }
    
    public boolean isUseScheduleBasedTransition() {
        return useScheduleBasedTransition;
    }
    
    public void setUseScheduleBasedTransition(boolean use) {
        this.useScheduleBasedTransition = use;
        if (use) {
            lastScheduleType = null; // Reset to force check
        }
    }
    
    public ScheduleEntry.EventType getLastScheduleType() {
        return lastScheduleType;
    }
}
