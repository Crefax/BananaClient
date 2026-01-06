package com.muzmod;

import com.muzmod.config.ModConfig;
import com.muzmod.handler.EventHandler;
import com.muzmod.handler.KeyBindHandler;
import com.muzmod.schedule.ScheduleManager;
import com.muzmod.state.StateManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = MuzMod.MODID, version = MuzMod.VERSION, clientSideOnly = true)
public class MuzMod {
    
    public static final String MODID = "muzmod";
    public static final String VERSION = "2.3.0";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    
    @Mod.Instance(MODID)
    public static MuzMod instance;
    
    private StateManager stateManager;
    private ModConfig config;
    private ScheduleManager scheduleManager;
    private boolean botEnabled = false;
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("MuzMod Pre-Initialization");
        File configDir = event.getModConfigurationDirectory();
        File muzmodDir = new File(configDir, "muzmod");
        if (!muzmodDir.exists()) {
            muzmodDir.mkdirs();
        }
        
        config = new ModConfig(event.getSuggestedConfigurationFile());
        config.load();
        
        scheduleManager = new ScheduleManager(muzmodDir);
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
    
    public ScheduleManager getScheduleManager() {
        return scheduleManager;
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
