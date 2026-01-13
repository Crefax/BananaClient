package com.muzmod.account;

import com.mojang.authlib.GameProfile;
import com.muzmod.MuzMod;
import com.muzmod.util.BananaLogger;
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
    private final BananaLogger log = BananaLogger.getInstance();
    
    public ProxyLoginHandler(NetworkManager networkManager, Minecraft mc, GuiScreen previousScreen) {
        super(networkManager, mc, previousScreen);
        this.networkManager = networkManager;
        this.mc = mc;
        this.previousScreen = previousScreen;
        log.proxy("ProxyLoginHandler created");
    }
    
    @Override
    public void handleLoginSuccess(S02PacketLoginSuccess packet) {
        log.proxy("*** LOGIN SUCCESS! ***");
        log.proxy("Profile: " + packet.getProfile().getName() + " / " + packet.getProfile().getId());
        super.handleLoginSuccess(packet);
    }
    
    @Override
    public void handleDisconnect(S00PacketDisconnect packet) {
        log.proxy("*** DISCONNECTED BY SERVER ***");
        log.proxy("Reason: " + packet.func_149603_c().getUnformattedText());
        super.handleDisconnect(packet);
    }
    
    @Override
    public void handleEncryptionRequest(S01PacketEncryptionRequest packet) {
        log.proxy("*** ENCRYPTION REQUEST - Online mode server ***");
        
        // Online sunucularda bu çağrılmaz
        // Online sunucularda encryption gerekir
        mc.displayGuiScreen(new GuiDisconnected(previousScreen, "connect.failed",
            new ChatComponentText("§cSunucu online modda!\n§7Offline hesap ile online sunuculara bağlanamazsınız.")));
        
        networkManager.closeChannel(new ChatComponentText("Online mode not supported"));
    }
    
    @Override
    public void handleEnableCompression(S03PacketEnableCompression packet) {
        log.proxy("*** COMPRESSION ENABLED: " + packet.getCompressionTreshold() + " ***");
        super.handleEnableCompression(packet);
    }
    
    @Override
    public void onDisconnect(IChatComponent reason) {
        log.proxy("*** CONNECTION CLOSED ***");
        log.proxy("Reason: " + reason.getUnformattedText());
        super.onDisconnect(reason);
    }
}
