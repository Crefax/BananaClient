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
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;

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
     * Account Manager butonuna tıklandığında
     */
    @SubscribeEvent
    public void onGuiAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.gui instanceof GuiMainMenu) {
            if (event.button.id == ACCOUNT_BUTTON_ID) {
                Minecraft.getMinecraft().displayGuiScreen(new GuiAccountManager(event.gui));
                event.setCanceled(true);
            }
        }
    }
    
    /**
     * GuiConnecting açılırken proxy aktifse kendi GUI'mizi kullan
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiConnecting && !(event.gui instanceof GuiProxyConnecting)) {
            ProxyManager pm = ProxyManager.getInstance();
            
            if (pm.isProxyEnabled()) {
                // GuiConnecting'den ServerData'yı al
                try {
                    GuiConnecting guiConnecting = (GuiConnecting) event.gui;
                    
                    // ServerData'yı reflection ile al
                    ServerData serverData = getServerDataFromGuiConnecting(guiConnecting);
                    
                    if (serverData != null) {
                        MuzMod.LOGGER.info("[EventHandler] Intercepting connection, using proxy GUI");
                        
                        // Önceki ekranı al
                        GuiMultiplayer previousScreen = getPreviousScreenFromGuiConnecting(guiConnecting);
                        
                        // Proxy GUI'mizi kullan
                        event.gui = new GuiProxyConnecting(previousScreen, serverData);
                    }
                } catch (Exception e) {
                    MuzMod.LOGGER.error("[EventHandler] Failed to intercept connection: " + e.getMessage());
                }
            }
        }
    }
    
    private ServerData getServerDataFromGuiConnecting(GuiConnecting gui) {
        // GuiConnecting'de serverData field'ını bul
        try {
            for (Field field : GuiConnecting.class.getDeclaredFields()) {
                if (field.getType() == ServerData.class) {
                    field.setAccessible(true);
                    return (ServerData) field.get(gui);
                }
            }
        } catch (Exception e) {
            MuzMod.LOGGER.error("[EventHandler] Could not get ServerData: " + e.getMessage());
        }
        return null;
    }
    
    private GuiMultiplayer getPreviousScreenFromGuiConnecting(GuiConnecting gui) {
        // GuiConnecting'de previousGuiScreen field'ını bul
        try {
            for (Field field : GuiConnecting.class.getDeclaredFields()) {
                if (field.getType() == GuiMultiplayer.class || field.getType().getSuperclass() == GuiMultiplayer.class) {
                    field.setAccessible(true);
                    return (GuiMultiplayer) field.get(gui);
                }
            }
            // GuiScreen tipinde arayalım
            for (Field field : GuiConnecting.class.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(gui);
                if (value instanceof GuiMultiplayer) {
                    return (GuiMultiplayer) value;
                }
            }
        } catch (Exception e) {
            MuzMod.LOGGER.error("[EventHandler] Could not get previous screen: " + e.getMessage());
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
