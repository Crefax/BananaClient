package com.muzmod.handler;

import com.muzmod.MuzMod;
import com.muzmod.account.AccountManager;
import com.muzmod.gui.GuiAccountManager;
import com.muzmod.render.OverlayRenderer;
import com.muzmod.render.WorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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
     * Ana menüye Account Manager butonu ekle
     */
    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiMainMenu) {
            // AccountManager'ı başlat
            AccountManager.getInstance();
            
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
}
