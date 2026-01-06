package com.muzmod.handler;

import com.muzmod.MuzMod;
import com.muzmod.gui.MuzModGuiModern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/**
 * Handles keybind registration and input events
 */
public class KeyBindHandler {
    
    public static KeyBinding keyOpenGui;
    public static KeyBinding keyToggleBot;
    
    public static void registerKeyBinds() {
        keyOpenGui = new KeyBinding(
            "MuzMod Menü",
            Keyboard.KEY_RSHIFT,
            "MuzMod"
        );
        
        keyToggleBot = new KeyBinding(
            "Bot Aç/Kapat",
            Keyboard.KEY_NONE,
            "MuzMod"
        );
        
        ClientRegistry.registerKeyBinding(keyOpenGui);
        ClientRegistry.registerKeyBinding(keyToggleBot);
    }
    
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        
        if (mc.theWorld == null || mc.thePlayer == null) return;
        
        if (keyOpenGui.isPressed()) {
            mc.displayGuiScreen(new MuzModGuiModern());
        }
        
        if (keyToggleBot.isPressed()) {
            MuzMod.instance.toggleBot();
            
            String status = MuzMod.instance.isBotEnabled() ? "§aAktif" : "§cDevre Dışı";
            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                "§6[MuzMod] §fBot: " + status
            ));
        }
    }
}
