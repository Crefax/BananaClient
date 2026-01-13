package com.muzmod.account;

import com.muzmod.MuzMod;
import com.muzmod.util.BananaLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.util.ChatComponentText;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Proxy destekli sunucu bağlantısı yapan GUI
 * GuiConnecting'i proxy ile değiştirmek için kullanılır
 */
public class GuiProxyConnecting extends GuiScreen {
    
    private final GuiScreen previousScreen;
    private final ServerData serverData;
    
    private NetworkManager networkManager;
    private boolean cancelled = false;
    private String statusMessage = "Bağlanılıyor...";
    
    public GuiProxyConnecting(GuiScreen parent, ServerData server) {
        this.previousScreen = parent;
        this.serverData = server;
    }
    
    @Override
    public void initGui() {
        // Bağlantı thread'ini başlat
        new Thread("Proxy Server Connector") {
            @Override
            public void run() {
                try {
                    connect();
                } catch (Exception e) {
                    if (!cancelled) {
                        MuzMod.LOGGER.error("[GuiProxyConnecting] Connection failed: " + e.getMessage());
                        onConnectionFailed("Bağlantı hatası: " + e.getMessage());
                    }
                }
            }
        }.start();
    }
    
    private void connect() {
        ProxyManager pm = ProxyManager.getInstance();
        BananaLogger log = BananaLogger.getInstance();
        
        log.proxy("============ CONNECTION START ============");
        log.proxy("ProxyEnabled: " + pm.isProxyEnabled());
        log.proxy("ProxyHost: " + pm.getProxyHost());
        log.proxy("ProxyPort: " + pm.getProxyPort());
        
        String[] addressParts = serverData.serverIP.split(":");
        String host = addressParts[0];
        int port = addressParts.length > 1 ? Integer.parseInt(addressParts[1]) : 25565;
        
        log.proxy("Target Server: " + host + ":" + port);
        
        if (pm.isProxyEnabled()) {
            statusMessage = "Proxy ile bağlanılıyor...";
            log.proxy(">>> USING PROXY CONNECTION <<<");
            
            try {
                // Proxy üzerinden bağlan - HOSTNAME ile (DNS leak önleme)
                networkManager = ProxyNetworkManager.createNetworkManagerAndConnect(
                    host, port, true);
                
                log.proxy("NetworkManager created successfully");
                
            } catch (Exception e) {
                BananaLogger.getInstance().error("Proxy", "Connection error: " + e.getMessage(), e);
                onConnectionFailed("Proxy bağlantı hatası: " + e.getMessage());
                return;
            }
        } else {
            statusMessage = "Direkt bağlanılıyor...";
            BananaLogger.getInstance().proxy(">>> USING DIRECT CONNECTION (NO PROXY) <<<");
            
            try {
                networkManager = NetworkManager.createNetworkManagerAndConnect(
                    InetAddress.getByName(host), port, true);
                    
            } catch (UnknownHostException e) {
                onConnectionFailed("Sunucu bulunamadı: " + host);
                return;
            } catch (Exception e) {
                onConnectionFailed("Bağlantı hatası: " + e.getMessage());
                return;
            }
        }
        
        if (networkManager == null || cancelled) {
            return;
        }
        
        BananaLogger.getInstance().proxy("NetworkManager channel open: " + networkManager.isChannelOpen());
        
        // Network handler ayarla ve handshake başlat
        statusMessage = "Handshake yapılıyor...";
        networkManager.setNetHandler(new ProxyLoginHandler(networkManager, mc, previousScreen));
        
        BananaLogger.getInstance().proxy("NetHandler set, sending handshake...");
        
        // Handshake paketi gönder
        networkManager.sendPacket(new C00Handshake(47, host, port, EnumConnectionState.LOGIN));
        
        BananaLogger.getInstance().proxy("Handshake packet sent");
        
        // Login paketi gönder
        statusMessage = "Giriş yapılıyor...";
        networkManager.sendPacket(new C00PacketLoginStart(mc.getSession().getProfile()));
        
        BananaLogger.getInstance().proxy("Login packet sent for: " + mc.getSession().getProfile().getName());
        BananaLogger.getInstance().proxy("Waiting for server response...");
    }
    
    private void onConnectionFailed(final String reason) {
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                mc.displayGuiScreen(new GuiDisconnected(previousScreen, "connect.failed", 
                    new ChatComponentText(reason)));
            }
        });
    }
    
    @Override
    public void updateScreen() {
        if (networkManager != null) {
            if (networkManager.isChannelOpen()) {
                try {
                    networkManager.processReceivedPackets();
                } catch (Exception e) {
                    BananaLogger.getInstance().error("GuiProxyConnecting", "Packet processing error: " + e.getMessage(), e);
                    if (!cancelled) {
                        onConnectionFailed("Paket işleme hatası: " + e.getMessage());
                    }
                }
            } else {
                BananaLogger.getInstance().proxy("Channel closed! Exit message: " + 
                    (networkManager.getExitMessage() != null ? networkManager.getExitMessage().getUnformattedText() : "null"));
                if (networkManager.getExitMessage() != null) {
                    onConnectionFailed(networkManager.getExitMessage().getUnformattedText());
                }
            }
        }
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        // Status mesajı
        drawCenteredString(fontRendererObj, statusMessage, width / 2, height / 2 - 20, 0xFFFFFF);
        
        // Proxy bilgisi
        ProxyManager pm = ProxyManager.getInstance();
        if (pm.isProxyEnabled()) {
            drawCenteredString(fontRendererObj, "§d[Proxy: " + pm.getProxyString() + "]", 
                width / 2, height / 2, 0xAAAAAA);
        }
        
        // İpucu
        drawCenteredString(fontRendererObj, "§7ESC ile iptal edilebilir", 
            width / 2, height / 2 + 30, 0x666666);
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) { // ESC
            cancelled = true;
            if (networkManager != null) {
                networkManager.closeChannel(new ChatComponentText("İptal edildi"));
            }
            mc.displayGuiScreen(previousScreen);
        }
    }
    
    @Override
    public void onGuiClosed() {
        // GUI kapanırsa bağlantıyı kapat
    }
}
