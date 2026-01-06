package com.muzmod.state;

import com.muzmod.MuzMod;
import com.muzmod.state.impl.AFKState;
import com.muzmod.state.impl.IdleState;
import com.muzmod.state.impl.MiningState;
import com.muzmod.state.impl.RepairState;
import com.muzmod.state.impl.SafeState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
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
    
    private int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20; // Check every second (20 ticks)
    
    public StateManager() {
        initStates();
    }
    
    private void initStates() {
        idleState = new IdleState();
        afkState = new AFKState();
        miningState = new MiningState();
        repairState = new RepairState();
        safeState = new SafeState();
        
        states.add(idleState);
        states.add(afkState);
        states.add(miningState);
        states.add(repairState);
        states.add(safeState);
        
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
        
        tickCounter++;
        
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
        switch (stateName.toLowerCase()) {
            case "idle":
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
    
    public List<IState> getAllStates() {
        return new ArrayList<>(states);
    }
    
    public String getCurrentStateName() {
        return currentState != null ? currentState.getName() : "Yok";
    }
    
    public String getCurrentStateStatus() {
        return currentState != null ? currentState.getStatus() : "";
    }
}
