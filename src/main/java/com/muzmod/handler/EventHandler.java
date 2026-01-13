package com.muzmod.handler;

import com.muzmod.MuzMod;
import com.muzmod.account.AccountManager;
import com.muzmod.account.GuiProxyConnecting;
import com.muzmod.account.ProxyManager;
import com.muzmod.gui.GuiAccountManager;
import com.muzmod.render.OverlayRenderer;
import com.muzmod.render.WorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Main event handler for rendering and other events
 */
public class EventHandler {
    
    // Son seçilen sunucu - GuiMultiplayer'dan kaydedilir
    private static ServerData lastSelectedServer = null;
    private final OverlayRenderer overlayRenderer = new OverlayRenderer();
    private final WorldRenderer worldRenderer = new WorldRenderer();
    
    // Account Manager button ID
    private static final int ACCOUNT_BUTTON_ID = 9999;
    
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        
        if (MuzMod.instance.getConfig().isShowOverlay()) {
            overlayRenderer.render();
        }
    }
    
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        worldRenderer.render(event.partialTicks);
    }
    
    /**
     * Ana menüye Account Manager butonu ekle ve Realms butonunu kaldır
     */
    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiMainMenu) {
            // AccountManager'ı başlat
            AccountManager.getInstance();
            
            // Realms butonunu bul ve kaldır (ID: 14)
            GuiButton realmsBtn = null;
            for (Object obj : event.buttonList) {
                GuiButton btn = (GuiButton) obj;
                if (btn.id == 14) { // Realms button ID
                    realmsBtn = btn;
                    break;
                }
            }
            if (realmsBtn != null) {
                event.buttonList.remove(realmsBtn);
                MuzMod.LOGGER.info("[EventHandler] Realms button removed");
            }
            
            // Multiplayer butonunu bul ve altına Account Manager ekle
            GuiButton multiplayerBtn = null;
            for (Object obj : event.buttonList) {
                GuiButton btn = (GuiButton) obj;
                if (btn.id == 2) { // Multiplayer button ID
                    multiplayerBtn = btn;
                    break;
                }
            }
            
            if (multiplayerBtn != null) {
                // Account Manager butonu - Multiplayer'ın altına
                int btnX = multiplayerBtn.xPosition;
                int btnY = multiplayerBtn.yPosition + 24;
                int btnW = multiplayerBtn.width;
                
                GuiButton accountBtn = new GuiButton(ACCOUNT_BUTTON_ID, btnX, btnY, btnW, 20, "§dAccount Manager");
                event.buttonList.add(accountBtn);
            }
        }
    }
    
    /**
     * GuiConnecting açıldığında proxy ile değiştir
     * Bu en güvenilir yol - Minecraft'ın kendi bağlantı GUI'sini yakalıyoruz
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiConnecting) {
            ProxyManager pm = ProxyManager.getInstance();
            
            MuzMod.LOGGER.info("[EventHandler] GuiConnecting detected!");
            MuzMod.LOGGER.info("[EventHandler] Proxy enabled: " + pm.isProxyEnabled());
            
            if (pm.isProxyEnabled() && lastSelectedServer != null) {
                MuzMod.LOGGER.info("[EventHandler] *** INTERCEPTING GuiConnecting for: " + lastSelectedServer.serverIP + " ***");
                
                // GuiConnecting'i iptal et
                event.setCanceled(true);
                
                // Kendi proxy GUI'mizi aç
                Minecraft.getMinecraft().displayGuiScreen(
                    new GuiProxyConnecting(null, lastSelectedServer));
            } else if (pm.isProxyEnabled()) {
                MuzMod.LOGGER.warn("[EventHandler] Proxy enabled but no lastSelectedServer!");
            }
        }
    }
    
    /**
     * Account Manager butonuna tıklandığında VE
     * GuiMultiplayer'da Join Server butonuna tıklandığında (proxy için)
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        // Account Manager button
        if (event.gui instanceof GuiMainMenu) {
            if (event.button.id == ACCOUNT_BUTTON_ID) {
                Minecraft.getMinecraft().displayGuiScreen(new GuiAccountManager(event.gui));
                event.setCanceled(true);
                return;
            }
        }
        
        // GuiMultiplayer - her butona basıldığında seçili sunucuyu kaydet
        if (event.gui instanceof GuiMultiplayer) {
            GuiMultiplayer guiMultiplayer = (GuiMultiplayer) event.gui;
            ServerData serverData = getSelectedServer(guiMultiplayer);
            
            if (serverData != null) {
                lastSelectedServer = serverData;
                MuzMod.LOGGER.info("[EventHandler] Saved selected server: " + serverData.serverIP);
            }
            
            MuzMod.LOGGER.info("[EventHandler] GuiMultiplayer button pressed: ID=" + event.button.id);
        }
    }
    
    /**
     * GuiMultiplayer'dan seçili sunucuyu al
     */
    private ServerData getSelectedServer(GuiMultiplayer gui) {
        try {
            // Method 1: Try getSelectedServer() or func_146791_a()
            for (Method method : GuiMultiplayer.class.getDeclaredMethods()) {
                method.setAccessible(true);
                
                // Look for method that returns ServerData and has no parameters
                if (method.getReturnType() == ServerData.class && method.getParameterTypes().length == 0) {
                    MuzMod.LOGGER.info("[EventHandler] Found ServerData method: " + method.getName());
                    Object result = method.invoke(gui);
                    if (result instanceof ServerData) {
                        ServerData sd = (ServerData) result;
                        if (sd.serverIP != null && !sd.serverIP.isEmpty()) {
                            MuzMod.LOGGER.info("[EventHandler] Got server from method: " + sd.serverIP);
                            return sd;
                        }
                    }
                }
            }
            
            // Method 2: Look for ServerSelectionList and get selected
            for (Field field : GuiMultiplayer.class.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(gui);
                
                // ServerSelectionList tipini kontrol et
                if (value != null && value.getClass().getSimpleName().contains("ServerSelectionList")) {
                    MuzMod.LOGGER.info("[EventHandler] Found ServerSelectionList: " + field.getName());
                    
                    // getSelected veya func_148193_k() metodunu dene
                    for (Method m : value.getClass().getDeclaredMethods()) {
                        if (m.getReturnType().getSimpleName().contains("ServerListEntry") && 
                            m.getParameterTypes().length == 0) {
                            m.setAccessible(true);
                            Object entry = m.invoke(value);
                            
                            if (entry != null) {
                                // Entry'den ServerData al
                                for (Method em : entry.getClass().getDeclaredMethods()) {
                                    if (em.getReturnType() == ServerData.class && em.getParameterTypes().length == 0) {
                                        em.setAccessible(true);
                                        ServerData sd = (ServerData) em.invoke(entry);
                                        if (sd != null && sd.serverIP != null) {
                                            MuzMod.LOGGER.info("[EventHandler] Got server from list entry: " + sd.serverIP);
                                            return sd;
                                        }
                                    }
                                }
                                
                                // Field olarak da dene
                                for (Field ef : entry.getClass().getDeclaredFields()) {
                                    ef.setAccessible(true);
                                    Object ev = ef.get(entry);
                                    if (ev instanceof ServerData) {
                                        ServerData sd = (ServerData) ev;
                                        if (sd.serverIP != null && !sd.serverIP.isEmpty()) {
                                            MuzMod.LOGGER.info("[EventHandler] Got server from entry field: " + sd.serverIP);
                                            return sd;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Method 3: Direct ServerData field
            for (Field field : GuiMultiplayer.class.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(gui);
                
                if (value instanceof ServerData) {
                    ServerData sd = (ServerData) value;
                    if (sd.serverIP != null && !sd.serverIP.isEmpty()) {
                        MuzMod.LOGGER.info("[EventHandler] Got server from direct field: " + sd.serverIP);
                        return sd;
                    }
                }
            }
            
            // Method 4: Get from ServerList using selected index
            ServerList serverList = null;
            
            for (Field field : GuiMultiplayer.class.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(gui);
                
                if (value instanceof ServerList) {
                    serverList = (ServerList) value;
                    MuzMod.LOGGER.info("[EventHandler] Found ServerList: " + field.getName());
                    break;
                }
            }
            
            if (serverList != null) {
                // Try to get selected index from various int fields
                for (Field field : GuiMultiplayer.class.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.getType() == int.class) {
                        int idx = field.getInt(gui);
                        if (idx >= 0 && idx < serverList.countServers()) {
                            ServerData sd = serverList.getServerData(idx);
                            if (sd != null && sd.serverIP != null && !sd.serverIP.isEmpty()) {
                                MuzMod.LOGGER.info("[EventHandler] Got server from ServerList[" + idx + "]: " + sd.serverIP);
                                return sd;
                            }
                        }
                    }
                }
            }
            
            MuzMod.LOGGER.warn("[EventHandler] Could not find selected server with any method!");
            
        } catch (Exception e) {
            MuzMod.LOGGER.error("[EventHandler] Exception getting selected server: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Oyuncu dünyaya girdiğinde hesaba özel config yükle
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && mc.theWorld != null) {
            String playerName = mc.thePlayer.getName();
            if (playerName != null && !playerName.equals(MuzMod.instance.getCurrentPlayerName())) {
                MuzMod.instance.loadConfigForPlayer(playerName);
            }
        }
    }
}
