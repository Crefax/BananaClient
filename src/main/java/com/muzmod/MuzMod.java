package com.muzmod;

import com.muzmod.config.ModConfig;
import com.muzmod.handler.EventHandler;
import com.muzmod.handler.KeyBindHandler;
import com.muzmod.state.StateManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = MuzMod.MODID, version = MuzMod.VERSION, clientSideOnly = true)
public class MuzMod {
    
    public static final String MODID = "muzmod";
    public static final String VERSION = "1.1.0";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    
    @Mod.Instance(MODID)
    public static MuzMod instance;
    
    private StateManager stateManager;
    private ModConfig config;
    private boolean botEnabled = false;
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("MuzMod Pre-Initialization");
        config = new ModConfig(event.getSuggestedConfigurationFile());
        config.load();
    }
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("MuzMod Initialization");
        
        stateManager = new StateManager();
        
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        MinecraftForge.EVENT_BUS.register(new KeyBindHandler());
        MinecraftForge.EVENT_BUS.register(stateManager);
        
        KeyBindHandler.registerKeyBinds();
    }
    
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("MuzMod Post-Initialization Complete");
    }
    
    public StateManager getStateManager() {
        return stateManager;
    }
    
    public ModConfig getConfig() {
        return config;
    }
    
    public boolean isBotEnabled() {
        return botEnabled;
    }
    
    public void setBotEnabled(boolean enabled) {
        this.botEnabled = enabled;
        if (enabled) {
            LOGGER.info("Bot enabled");
        } else {
            LOGGER.info("Bot disabled");
            stateManager.stopCurrentState();
        }
    }
    
    public void toggleBot() {
        setBotEnabled(!botEnabled);
    }
}
