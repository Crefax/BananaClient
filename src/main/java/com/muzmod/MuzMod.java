package com.muzmod;

import com.muzmod.config.ModConfig;
import com.muzmod.handler.EventHandler;
import com.muzmod.handler.KeyBindHandler;
import com.muzmod.schedule.ScheduleManager;
import com.muzmod.state.StateManager;
import net.minecraft.client.Minecraft;
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
    
    // Client bilgileri - tek yerden yönetim
    public static final String CLIENT_NAME = "BananaClient";
    public static final String VERSION = "0.7.9";
    public static final String MODID = "bananaclient";
    public static final String GITHUB_URL = "github.com/Crefax/BananaClient";
    
    public static final Logger LOGGER = LogManager.getLogger(CLIENT_NAME);
    
    @Mod.Instance(MODID)
    public static MuzMod instance;
    
    private StateManager stateManager;
    private ModConfig config;
    private ScheduleManager scheduleManager;
    private boolean botEnabled = false;
    private String currentPlayerName = null;
    
    // Config dizini - .minecraft/BananaClient/
    private File clientDir;
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info(CLIENT_NAME + " v" + VERSION + " Pre-Initialization");
        
        // .minecraft/BananaClient/ dizinini oluştur
        File mcDir = Minecraft.getMinecraft().mcDataDir;
        clientDir = new File(mcDir, CLIENT_NAME);
        if (!clientDir.exists()) {
            clientDir.mkdirs();
        }
        
        // Config dosyası: varsayılan config (oyuncu giriş yapınca hesaba göre yüklenecek)
        File configFile = new File(clientDir, "config.cfg");
        config = new ModConfig(configFile);
        config.load();
        
        // Schedule dizini: .minecraft/BananaClient/schedules/
        File schedulesDir = new File(clientDir, "schedules");
        if (!schedulesDir.exists()) {
            schedulesDir.mkdirs();
        }
        scheduleManager = new ScheduleManager(schedulesDir);
    }
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info(CLIENT_NAME + " Initialization");
        
        stateManager = new StateManager();
        
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        MinecraftForge.EVENT_BUS.register(new KeyBindHandler());
        MinecraftForge.EVENT_BUS.register(stateManager);
        
        KeyBindHandler.registerKeyBinds();
    }
    
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info(CLIENT_NAME + " v" + VERSION + " Post-Initialization Complete");
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
    
    public File getClientDir() {
        return clientDir;
    }
    
    /**
     * Oyuncu adına göre config yükler
     * Her hesabın kendi config dosyası olur: config_OyuncuAdi.cfg
     */
    public void loadConfigForPlayer(String playerName) {
        loadConfigForPlayer(playerName, false);
    }
    
    /**
     * Oyuncu adına göre config yükler (force reload opsiyonlu)
     */
    public void loadConfigForPlayer(String playerName, boolean forceReload) {
        if (playerName == null || playerName.isEmpty()) {
            return;
        }
        
        // Aynı oyuncu için tekrar yükleme (force değilse)
        if (!forceReload && playerName.equals(currentPlayerName)) {
            return;
        }
        
        currentPlayerName = playerName;
        
        // Hesaba özel config dosyası: config_OyuncuAdi.cfg
        File playerConfigFile = new File(clientDir, "config_" + playerName + ".cfg");
        
        LOGGER.info("Loading config for player: " + playerName + " -> " + playerConfigFile.getName() + (forceReload ? " (forced)" : ""));
        
        // Yeni config oluştur ve yükle
        config = new ModConfig(playerConfigFile);
        config.load();
        
        LOGGER.info("Config loaded for " + playerName + ": targetMin=" + config.getObsidianTargetMinOffset() + ", targetMax=" + config.getObsidianTargetMaxOffset());
    }
    
    public String getCurrentPlayerName() {
        return currentPlayerName;
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
