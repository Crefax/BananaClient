package com.muzmod.handler;

import com.muzmod.MuzMod;
import com.muzmod.account.AccountManager;
import com.muzmod.account.GuiProxyConnecting;
import com.muzmod.account.ProxyManager;
import com.muzmod.gui.GuiAccountManager;
import com.muzmod.gui.GuiProxyMultiplayer;
import com.muzmod.render.OverlayRenderer;
import com.muzmod.render.WorldRenderer;
import com.muzmod.util.InputSimulator;
import com.muzmod.util.TeleportDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

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
    
    // Button IDs
    private static final int ACCOUNT_BUTTON_ID = 9999;
    private static final int PROXY_CONNECT_BUTTON_ID = 9998;
    
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
     * GuiMultiplayer'a "Proxy ile Bağlan" butonu ekle
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
        
        // GuiMultiplayer'a "Proxy ile Bağlan" butonu ekle
        if (event.gui instanceof GuiMultiplayer) {
            ProxyManager pm = ProxyManager.getInstance();
            
            // Join Server butonunu bul
            GuiButton joinBtn = null;
            for (Object obj : event.buttonList) {
                GuiButton btn = (GuiButton) obj;
                if (btn.id == 1) { // Join Server button ID
                    joinBtn = btn;
                    break;
                }
            }
            
            if (joinBtn != null) {
                // Proxy Connect butonu - Join Server'ın yanına
                String proxyText = pm.isProxyEnabled() ? "§a[Proxy] Bağlan" : "§c[Proxy] Kapalı";
                GuiButton proxyBtn = new GuiButton(PROXY_CONNECT_BUTTON_ID, 
                    joinBtn.xPosition + joinBtn.width + 5, joinBtn.yPosition, 
                    100, 20, proxyText);
                event.buttonList.add(proxyBtn);
                
                MuzMod.LOGGER.info("[EventHandler] Proxy Connect button added (enabled=" + pm.isProxyEnabled() + ")");
            }
        }
    }
    
    /**
     * GuiMultiplayer açıldığında GuiProxyMultiplayer ile değiştir
     * GuiConnecting açıldığında proxy ile değiştir (fallback)
     * 
     * ÖNEMLI: GuiConnecting constructor'ında connect thread başlıyor!
     * Bu yüzden GuiMultiplayer'ı intercept edip connectToServer() metodunu
     * override eden kendi GUI'mizi gösteriyoruz.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiOpen(GuiOpenEvent event) {
        com.muzmod.util.BananaLogger log = com.muzmod.util.BananaLogger.getInstance();
        
        // GuiMultiplayer açılırken GuiProxyMultiplayer ile değiştir
        // Böylece connectToServer() metodunu override edebiliriz
        if (event.gui instanceof GuiMultiplayer && !(event.gui instanceof GuiProxyMultiplayer)) {
            ProxyManager pm = ProxyManager.getInstance();
            
            if (pm.isProxyEnabled()) {
                log.proxy("*** GuiMultiplayer INTERCEPTED - Replacing with GuiProxyMultiplayer ***");
                
                // Parent screen'i bul
                try {
                    Field parentField = GuiMultiplayer.class.getDeclaredField("field_146598_f");
                    parentField.setAccessible(true);
                    net.minecraft.client.gui.GuiScreen parent = (net.minecraft.client.gui.GuiScreen) parentField.get(event.gui);
                    
                    event.gui = new GuiProxyMultiplayer(parent);
                    log.proxy("Replaced with GuiProxyMultiplayer");
                } catch (Exception e) {
                    log.error("EventHandler", "Failed to get parent screen: " + e.getMessage());
                    // Parent olmadan da dene
                    event.gui = new GuiProxyMultiplayer(null);
                    log.proxy("Replaced with GuiProxyMultiplayer (no parent)");
                }
                return;
            }
        }
        
        // Fallback: GuiConnecting hala açılırsa (olmamalı ama)
        if (event.gui instanceof GuiConnecting) {
            ProxyManager pm = ProxyManager.getInstance();
            
            if (!pm.isProxyEnabled()) {
                return; // Proxy kapalı, normal bağlantı
            }
            
            log.proxy("*** GuiConnecting DETECTED (FALLBACK) ***");
            
            GuiConnecting originalGui = (GuiConnecting) event.gui;
            ServerData target = null;
            GuiMultiplayer parentGui = null;
            
            try {
                // field_146374_i = GuiMultiplayer (parent screen)
                Field parentField = GuiConnecting.class.getDeclaredField("field_146374_i");
                parentField.setAccessible(true);
                Object parent = parentField.get(event.gui);
                
                if (parent instanceof GuiMultiplayer) {
                    parentGui = (GuiMultiplayer) parent;
                    target = getSelectedServer(parentGui);
                    
                    if (target != null) {
                        log.proxy("Got server from GuiMultiplayer: " + target.serverIP);
                    }
                }
            } catch (Exception e) {
                log.error("EventHandler", "Failed to get parent GuiMultiplayer: " + e.getMessage());
            }
            
            // Hala bulamadıysak lastSelectedServer dene
            if (target == null && lastSelectedServer != null && lastSelectedServer.serverIP != null) {
                target = lastSelectedServer;
                log.proxy("Using lastSelectedServer: " + target.serverIP);
            }
            
            if (target != null && target.serverIP != null && !target.serverIP.isEmpty()) {
                log.proxy("INTERCEPTING - Redirecting to: " + target.serverIP);
                
                // *** KRİTİK: Orijinal bağlantıyı tamamen durdur ***
                stopOriginalConnection(originalGui, log);
                
                // Event'i tamamen CANCEL et
                event.setCanceled(true);
                log.proxy("Event CANCELED - original GuiConnecting blocked");
                
                // Kendi GUI'mizi bir tick sonra aç (mevcut event döngüsü bittikten sonra)
                final ServerData finalTarget = target;
                final GuiMultiplayer finalParent = parentGui;
                
                // Hemen displayGuiScreen çağırıyoruz - cancel'dan sonra sorun olmaz
                Minecraft.getMinecraft().displayGuiScreen(new GuiProxyConnecting(finalParent, finalTarget));
                log.proxy("GuiProxyConnecting displayed");
                
            } else {
                log.warn("EventHandler", "No server found - cannot intercept");
            }
        }
    }
    
    /**
     * Orijinal GuiConnecting'in başlattığı bağlantıyı tamamen durdur
     */
    private void stopOriginalConnection(GuiConnecting gui, com.muzmod.util.BananaLogger log) {
        try {
            // 1. NetworkManager'ı kapat (field_146371_g)
            Field nmField = GuiConnecting.class.getDeclaredField("field_146371_g");
            nmField.setAccessible(true);
            NetworkManager nm = (NetworkManager) nmField.get(gui);
            
            if (nm != null) {
                // Channel'ı zorla kapat
                if (nm.channel() != null) {
                    nm.channel().close();
                    log.proxy("Closed NetworkManager channel");
                }
                nm.closeChannel(new net.minecraft.util.ChatComponentText("Proxy redirect"));
                log.proxy("Closed NetworkManager");
            }
            
            // NetworkManager'ı null yap ki thread kontrol ettiğinde hata alsın
            nmField.set(gui, null);
            log.proxy("Set NetworkManager to null");
            
        } catch (Exception e) {
            log.debug("EventHandler", "stopOriginalConnection: " + e.getMessage());
        }
        
        try {
            // 2. cancel field'ını true yap (field_146373_h) - varsa
            Field cancelField = GuiConnecting.class.getDeclaredField("field_146373_h");
            cancelField.setAccessible(true);
            cancelField.setBoolean(gui, true);
            log.proxy("Set cancel flag to true");
        } catch (Exception e) {
            // Field olmayabilir
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
            }
            
            com.muzmod.util.BananaLogger log = com.muzmod.util.BananaLogger.getInstance();
            ProxyManager pm = ProxyManager.getInstance();
            
            log.debug("EventHandler", "Button clicked: ID=" + event.button.id + ", displayString='" + event.button.displayString + "'");
            
            // Join Server butonu (ID=1) veya çift tıklama - proxy aktifse intercept et
            // ID=1 = Join Server, ID=4 = Direct Connect dialog açar
            if ((event.button.id == 1 || event.button.id == 7) && pm.isProxyEnabled()) {
                ServerData target = serverData != null ? serverData : lastSelectedServer;
                
                if (target != null && target.serverIP != null && !target.serverIP.isEmpty()) {
                    log.proxy("*** JOIN SERVER INTERCEPTED (button ID=" + event.button.id + ") ***");
                    log.proxy("Target: " + target.serverIP);
                    
                    event.setCanceled(true);
                    Minecraft.getMinecraft().displayGuiScreen(
                        new GuiProxyConnecting(guiMultiplayer, target));
                    return;
                } else {
                    log.warn("EventHandler", "Join Server clicked but no server selected!");
                }
            }
            
            // Proxy Connect butonuna tıklandı
            if (event.button.id == PROXY_CONNECT_BUTTON_ID) {
                log.proxy("*** PROXY BUTTON CLICKED ***");
                log.proxy("ProxyEnabled: " + pm.isProxyEnabled());
                log.proxy("ServerData: " + (serverData != null ? serverData.serverIP : "null"));
                log.proxy("LastSelectedServer: " + (lastSelectedServer != null ? lastSelectedServer.serverIP : "null"));
                
                if (!pm.isProxyEnabled()) {
                    log.proxy("Proxy is DISABLED - aborting");
                    return;
                }
                
                if (serverData == null && lastSelectedServer == null) {
                    log.proxy("No server selected - aborting");
                    return;
                }
                
                ServerData target = serverData != null ? serverData : lastSelectedServer;
                log.proxy("Connecting to: " + target.serverIP);
                
                event.setCanceled(true);
                
                // Proxy ile bağlan
                Minecraft.getMinecraft().displayGuiScreen(
                    new GuiProxyConnecting(guiMultiplayer, target));
            }
        }
    }
    
    /**
     * GuiMultiplayer'dan seçili sunucuyu al
     */
    private ServerData getSelectedServer(GuiMultiplayer gui) {
        com.muzmod.util.BananaLogger log = com.muzmod.util.BananaLogger.getInstance();
        
        try {
            // ServerList'i al
            Field serverListField = GuiMultiplayer.class.getDeclaredField("field_146804_i");
            serverListField.setAccessible(true);
            ServerList serverList = (ServerList) serverListField.get(gui);
            
            if (serverList == null || serverList.countServers() == 0) {
                log.warn("EventHandler", "ServerList is empty!");
                return null;
            }
            
            int serverCount = serverList.countServers();
            log.debug("EventHandler", "ServerList has " + serverCount + " servers");
            
            // ServerSelectionList'i al
            Field listField = GuiMultiplayer.class.getDeclaredField("field_146803_h");
            listField.setAccessible(true);
            Object selectionList = listField.get(gui);
            
            if (selectionList != null) {
                // Tüm int field'ları tara (parent class dahil)
                Class<?> clazz = selectionList.getClass();
                while (clazz != null) {
                    for (Field f : clazz.getDeclaredFields()) {
                        f.setAccessible(true);
                        if (f.getType() == int.class) {
                            int idx = f.getInt(selectionList);
                            log.debug("EventHandler", "Field " + clazz.getSimpleName() + "." + f.getName() + " = " + idx);
                            
                            // Geçerli index mi?
                            if (idx >= 0 && idx < serverCount) {
                                ServerData sd = serverList.getServerData(idx);
                                if (sd != null && sd.serverIP != null && !sd.serverIP.isEmpty()) {
                                    log.info("EventHandler", "*** FOUND via " + f.getName() + "[" + idx + "]: " + sd.serverIP);
                                    return sd;
                                }
                            }
                        }
                    }
                    clazz = clazz.getSuperclass();
                }
            }
            
            // GuiMultiplayer'daki tüm int field'ları da tara
            for (Field f : GuiMultiplayer.class.getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getType() == int.class) {
                    int idx = f.getInt(gui);
                    log.debug("EventHandler", "GuiMultiplayer." + f.getName() + " = " + idx);
                    
                    if (idx >= 0 && idx < serverCount) {
                        ServerData sd = serverList.getServerData(idx);
                        if (sd != null && sd.serverIP != null && !sd.serverIP.isEmpty()) {
                            log.info("EventHandler", "*** FOUND via GuiMultiplayer." + f.getName() + "[" + idx + "]: " + sd.serverIP);
                            return sd;
                        }
                    }
                }
            }
            
            log.warn("EventHandler", "Could not find selected server!");
            
        } catch (Exception e) {
            log.error("EventHandler", "getSelectedServer error: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * Oyuncu dünyaya girdiğinde hesaba özel config yükle
     * Ayrıca focus bypass'ı uygula
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // Focus bypass'ı START'ta uygula - sendClickBlockToController'dan ÖNCE
        if (event.phase == TickEvent.Phase.START) {
            InputSimulator.tickFocusBypass();
            return;
        }
        
        // END fazında config yükleme ve teleport detector registration
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && mc.theWorld != null) {
            String playerName = mc.thePlayer.getName();
            if (playerName != null && !playerName.equals(MuzMod.instance.getCurrentPlayerName())) {
                MuzMod.instance.loadConfigForPlayer(playerName);
            }
            
            // Teleport detector'ı register et (henüz yapılmadıysa)
            if (!TeleportDetector.isRegistered()) {
                TeleportDetector.register();
            }
        }
    }
    
    /**
     * Sunucudan disconnect olunca teleport detector'ı sıfırla
     */
    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        TeleportDetector.reset();
        MuzMod.LOGGER.info("[EventHandler] Client disconnected, TeleportDetector reset");
    }
}
