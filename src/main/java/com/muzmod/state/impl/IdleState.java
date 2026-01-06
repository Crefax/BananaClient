package com.muzmod.state.impl;

import com.muzmod.state.AbstractState;

/**
 * Idle state - default state when no other task is active
 * Bot waits and does nothing special
 */
public class IdleState extends AbstractState {
    
    public IdleState() {
        this.status = "Bekleniyor...";
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        setStatus("Bekleniyor...");
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
    }
    
    @Override
    public void onTick() {
        // Idle state does nothing - just waits
        setStatus("Bekleniyor... (Görev yok)");
    }
    
    @Override
    public String getName() {
        return "Idle";
    }
    
    @Override
    public boolean shouldActivate() {
        // Idle is the fallback state, never actively requests activation
        return false;
    }
    
    @Override
    public int getPriority() {
        // Lowest priority - only activates when nothing else does
        return 0;
    }
}
