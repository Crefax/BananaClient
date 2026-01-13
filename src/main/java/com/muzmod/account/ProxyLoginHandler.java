package com.muzmod.account;

import com.mojang.authlib.GameProfile;
import com.muzmod.MuzMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.server.*;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

/**
 * Proxy bağlantısı için Login handler
 */
public class ProxyLoginHandler extends NetHandlerLoginClient {
    
    private final NetworkManager networkManager;
    private final Minecraft mc;
    private final GuiScreen previousScreen;
    
    public ProxyLoginHandler(NetworkManager networkManager, Minecraft mc, GuiScreen previousScreen) {
        super(networkManager, mc, previousScreen);
        this.networkManager = networkManager;
        this.mc = mc;
        this.previousScreen = previousScreen;
    }
    
    @Override
    public void handleLoginSuccess(S02PacketLoginSuccess packet) {
        MuzMod.LOGGER.info("[ProxyLoginHandler] Login successful!");
        super.handleLoginSuccess(packet);
    }
    
    @Override
    public void handleDisconnect(S00PacketDisconnect packet) {
        MuzMod.LOGGER.info("[ProxyLoginHandler] Disconnected: " + packet.func_149603_c().getUnformattedText());
        super.handleDisconnect(packet);
    }
    
    @Override
    public void handleEncryptionRequest(S01PacketEncryptionRequest packet) {
        // Offline sunucularda bu çağrılmaz
        // Online sunucularda encryption gerekir
        MuzMod.LOGGER.info("[ProxyLoginHandler] Encryption requested - server is online mode");
        
        // Online mode sunuculara bağlanamayız offline hesapla
        mc.displayGuiScreen(new GuiDisconnected(previousScreen, "connect.failed",
            new ChatComponentText("§cSunucu online modda!\n§7Offline hesap ile online sunuculara bağlanamazsınız.")));
        
        networkManager.closeChannel(new ChatComponentText("Online mode not supported"));
    }
    
    @Override
    public void handleEnableCompression(S03PacketEnableCompression packet) {
        MuzMod.LOGGER.info("[ProxyLoginHandler] Compression enabled: " + packet.getCompressionTreshold());
        super.handleEnableCompression(packet);
    }
    
    @Override
    public void onDisconnect(IChatComponent reason) {
        MuzMod.LOGGER.info("[ProxyLoginHandler] Connection closed: " + reason.getUnformattedText());
        super.onDisconnect(reason);
    }
}
