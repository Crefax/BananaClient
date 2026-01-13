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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

@Mod(modid = MuzMod.MODID, version = MuzMod.VERSION, clientSideOnly = true)
public class MuzMod {
    
    // Client bilgileri - tek yerden yönetim
    public static final String CLIENT_NAME = "BananaClient";
    public static final String MOD_NAME = CLIENT_NAME; // Alias
    public static final String VERSION = "0.8.2";
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
    private File configsDir;    // Oyuncu config'leri için
    private File schedulesDir;  // Oyuncu schedule'ları için
    
    // Varsayılan config/schedule dosyaları (ana dizinde)
    private static final String DEFAULT_CONFIG_NAME = "default_config.cfg";
    private static final String DEFAULT_SCHEDULE_NAME = "default_schedule.json";
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info(CLIENT_NAME + " v" + VERSION + " Pre-Initialization");
        
        // .minecraft/BananaClient/ dizinini oluştur
        File mcDir = Minecraft.getMinecraft().mcDataDir;
        clientDir = new File(mcDir, CLIENT_NAME);
        if (!clientDir.exists()) {
            clientDir.mkdirs();
        }
        
        // Configs dizini: .minecraft/BananaClient/configs/
        configsDir = new File(clientDir, "configs");
        if (!configsDir.exists()) {
            configsDir.mkdirs();
        }
        
        // Schedule dizini: .minecraft/BananaClient/schedules/
        schedulesDir = new File(clientDir, "schedules");
        if (!schedulesDir.exists()) {
            schedulesDir.mkdirs();
        }
        
        // Varsayılan config dosyası (ana dizinde)
        File defaultConfigFile = new File(clientDir, DEFAULT_CONFIG_NAME);
        config = new ModConfig(defaultConfigFile);
        config.load();
        
        // Varsayılan schedule (ana dizinde, oyuncu bazlı yüklenecek)
        scheduleManager = new ScheduleManager(clientDir, schedulesDir, DEFAULT_SCHEDULE_NAME);
    }
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info(CLIENT_NAME + " Initialization");
        
        stateManager = new StateManager();
        
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        MinecraftForge.EVENT_BUS.register(new KeyBindHandler());
        MinecraftForge.EVENT_BUS.register(new com.muzmod.handler.ChatCommandHandler());
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
    
    public File getConfigsDir() {
        return configsDir;
    }
    
    public File getSchedulesDir() {
        return schedulesDir;
    }
    
    /**
     * Oyuncu adına göre config yükler
     * Her hesabın kendi config dosyası olur: configs/OyuncuAdi.cfg
     * Yeni oyuncu için varsayılan config kopyalanır
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
        
        // Hesaba özel config dosyası: configs/OyuncuAdi.cfg
        File playerConfigFile = new File(configsDir, playerName + ".cfg");
        
        // Eğer oyuncunun config dosyası yoksa, varsayılan config'i kopyala
        File defaultConfigFile = new File(clientDir, DEFAULT_CONFIG_NAME);
        if (!playerConfigFile.exists() && defaultConfigFile.exists()) {
            LOGGER.info("Creating config for new player: " + playerName + " (copying from default)");
            copyFile(defaultConfigFile, playerConfigFile);
        }
        
        LOGGER.info("Loading config for player: " + playerName + " -> configs/" + playerConfigFile.getName() + (forceReload ? " (forced)" : ""));
        
        // Yeni config oluştur ve yükle
        config = new ModConfig(playerConfigFile);
        config.load();
        
        // Oyuncu bazlı schedule yükle
        scheduleManager.loadForPlayer(playerName);
        
        LOGGER.info("Config & Schedule loaded for " + playerName);
    }
    
    /**
     * Mevcut config'i varsayılan olarak kaydet (Default'a aktar)
     */
    public boolean exportConfigToDefault() {
        if (currentPlayerName == null) {
            LOGGER.warn("No player config loaded to export");
            return false;
        }
        
        File playerConfigFile = new File(configsDir, currentPlayerName + ".cfg");
        File defaultConfigFile = new File(clientDir, DEFAULT_CONFIG_NAME);
        
        if (!playerConfigFile.exists()) {
            LOGGER.warn("Player config file not found: " + playerConfigFile.getName());
            return false;
        }
        
        // Önce mevcut config'i kaydet
        config.save();
        
        // Sonra default'a kopyala
        copyFile(playerConfigFile, defaultConfigFile);
        LOGGER.info("Config exported to default: " + currentPlayerName + " -> default_config.cfg");
        return true;
    }
    
    /**
     * Varsayılan config'i mevcut hesaba yükle (Default'tan al)
     */
    public boolean importConfigFromDefault() {
        if (currentPlayerName == null) {
            LOGGER.warn("No player loaded to import config");
            return false;
        }
        
        File defaultConfigFile = new File(clientDir, DEFAULT_CONFIG_NAME);
        File playerConfigFile = new File(configsDir, currentPlayerName + ".cfg");
        
        if (!defaultConfigFile.exists()) {
            LOGGER.warn("Default config file not found");
            return false;
        }
        
        // Default'tan oyuncu config'ine kopyala
        copyFile(defaultConfigFile, playerConfigFile);
        
        // Config'i yeniden yükle
        config = new ModConfig(playerConfigFile);
        config.load();
        
        LOGGER.info("Config imported from default: default_config.cfg -> " + currentPlayerName);
        return true;
    }
    
    /**
     * Schedule'ı varsayılan olarak kaydet
     */
    public boolean exportScheduleToDefault() {
        return scheduleManager.exportToDefault();
    }
    
    /**
     * Varsayılan schedule'ı yükle
     */
    public boolean importScheduleFromDefault() {
        return scheduleManager.importFromDefault();
    }
    
    /**
     * Dosya kopyalama yardımcı metodu
     */
    private void copyFile(File source, File dest) {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(dest);
             FileChannel sourceChannel = fis.getChannel();
             FileChannel destChannel = fos.getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } catch (IOException e) {
            LOGGER.error("Error copying file: " + e.getMessage());
        }
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
    
    /**
     * Send a chat message to the player (client-side only)
     */
    public void sendChat(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(message));
        }
    }
    
    /**
     * Get the DuelAnalyzerState from StateManager
     */
    public com.muzmod.duel.DuelAnalyzerState getDuelAnalyzerState() {
        return stateManager != null ? stateManager.getDuelAnalyzerState() : null;
    }
}
