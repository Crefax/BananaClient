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
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
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
        
        // GuiMultiplayer - Join Server button (ID: 1) veya Direct Connect confirm
        if (event.gui instanceof GuiMultiplayer) {
            ProxyManager pm = ProxyManager.getInstance();
            
            if (pm.isProxyEnabled() && (event.button.id == 1 || event.button.id == 4)) {
                // ID 1 = Join Server, ID 4 = Direct Connect
                GuiMultiplayer guiMultiplayer = (GuiMultiplayer) event.gui;
                
                ServerData serverData = getSelectedServer(guiMultiplayer);
                
                if (serverData != null) {
                    MuzMod.LOGGER.info("[EventHandler] Intercepting Join Server, using proxy for: " + serverData.serverIP);
                    
                    // Cancel the original action
                    event.setCanceled(true);
                    
                    // Open our proxy connecting GUI
                    Minecraft.getMinecraft().displayGuiScreen(new GuiProxyConnecting(guiMultiplayer, serverData));
                }
            }
        }
    }
    
    /**
     * GuiMultiplayer'dan seçili sunucuyu al
     */
    private ServerData getSelectedServer(GuiMultiplayer gui) {
        try {
            // Method 1: func_146791_a() - returns selected ServerData
            try {
                Method getServerData = GuiMultiplayer.class.getDeclaredMethod("func_146791_a");
                getServerData.setAccessible(true);
                Object result = getServerData.invoke(gui);
                if (result instanceof ServerData) {
                    return (ServerData) result;
                }
            } catch (NoSuchMethodException e) {
                // Try obfuscated name
            }
            
            // Method 2: Direct field access
            for (Field field : GuiMultiplayer.class.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(gui);
                
                if (value instanceof ServerData) {
                    ServerData sd = (ServerData) value;
                    if (sd.serverIP != null && !sd.serverIP.isEmpty()) {
                        return sd;
                    }
                }
            }
            
            // Method 3: Get from ServerList using selected index
            ServerList serverList = null;
            int selectedIndex = -1;
            
            for (Field field : GuiMultiplayer.class.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(gui);
                
                if (value instanceof ServerList) {
                    serverList = (ServerList) value;
                }
                
                // selectedServer index field (usually named something like field_146803_h)
                if (field.getType() == int.class) {
                    int intVal = field.getInt(gui);
                    if (intVal >= 0 && intVal < 1000) { // Reasonable index range
                        selectedIndex = intVal;
                    }
                }
            }
            
            if (serverList != null && selectedIndex >= 0) {
                try {
                    ServerData sd = serverList.getServerData(selectedIndex);
                    if (sd != null) {
                        return sd;
                    }
                } catch (Exception e) {
                    // Index out of bounds
                }
            }
            
        } catch (Exception e) {
            MuzMod.LOGGER.error("[EventHandler] Could not get selected server: " + e.getMessage());
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
