package com.muzmod.state;

/**
 * Base interface for all bot states
 */
public interface IState {
    
    /**
     * Called when this state becomes active
     */
    void onEnable();
    
    /**
     * Called when this state is deactivated
     */
    void onDisable();
    
    /**
     * Called every tick while this state is active
     */
    void onTick();
    
    /**
     * Get the display name of this state
     */
    String getName();
    
    /**
     * Get detailed status description
     */
    String getStatus();
    
    /**
     * Check if this state should activate based on current time
     */
    boolean shouldActivate();
    
    /**
     * Get priority - higher priority states take precedence
     */
    int getPriority();
}
