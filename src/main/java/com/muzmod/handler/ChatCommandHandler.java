package com.muzmod.handler;

import com.muzmod.MuzMod;
import com.muzmod.gui.MuzModGuiModern;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/**
 * Handles client-side chat commands with . prefix
 * Commands with . prefix are NOT sent to server
 */
public class ChatCommandHandler {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        // Chat açık değilse return
        if (!(mc.currentScreen instanceof net.minecraft.client.gui.GuiChat)) {
            return;
        }
        
        // Enter tuşuna basıldı mı kontrol et
        if (Keyboard.getEventKey() == Keyboard.KEY_RETURN && Keyboard.getEventKeyState()) {
            net.minecraft.client.gui.GuiChat chatGui = (net.minecraft.client.gui.GuiChat) mc.currentScreen;
            
            // Reflection ile chat içeriğini al
            try {
                java.lang.reflect.Field inputField = net.minecraft.client.gui.GuiChat.class.getDeclaredField("field_146415_a");
                inputField.setAccessible(true);
                net.minecraft.client.gui.GuiTextField textField = (net.minecraft.client.gui.GuiTextField) inputField.get(chatGui);
                String message = textField.getText();
                
                // . ile başlıyorsa işle
                if (message.startsWith(".")) {
                    // Chat'i kapat
                    mc.displayGuiScreen(null);
                    
                    // Komutu işle
                    handleCommand(message);
                    
                    // Event'i iptal etmek için keyboard event'i consume et
                    while (Keyboard.next()) {
                        // Clear keyboard buffer
                    }
                }
            } catch (Exception e) {
                MuzMod.LOGGER.error("Failed to handle chat command: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handle the command
     */
    private void handleCommand(String message) {
        // Remove the . prefix
        String command = message.substring(1).trim().toLowerCase();
        
        // Handle commands
        if (command.equals("bananaclient") || command.equals("bc")) {
            // Open GUI
            mc.displayGuiScreen(new MuzModGuiModern());
        }
        else if (command.equals("help")) {
            // Show help
            sendClientMessage("§6§l=== BananaClient Komutlar ===");
            sendClientMessage("§e.bananaclient §7veya §e.bc §7- GUI'yi aç");
            sendClientMessage("§e.help §7- Bu yardım mesajını göster");
        }
        else if (command.isEmpty()) {
            // Just . pressed
            return;
        }
        else {
            // Unknown command
            sendClientMessage("§cBilinmeyen komut: §7." + command);
            sendClientMessage("§7Komutlar için §e.help §7yaz");
        }
    }
    
    /**
     * Send a message to the client (not to server)
     */
    private void sendClientMessage(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(message));
        }
    }
}
