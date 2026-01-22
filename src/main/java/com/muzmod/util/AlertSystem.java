package com.muzmod.util;

import com.muzmod.MuzMod;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.awt.*;

/**
 * Uyarı Sistemi - Sesli ve görsel uyarılar
 */
public class AlertSystem {
    
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    /**
     * Kritik uyarı gönder - Oyuncu önüne geçti
     */
    public static void alertPlayerBlocking(String accountName, String blockingPlayerName) {
        String title = "BananaClient";
        String message = accountName + " isimli hesabınızın önüne " + blockingPlayerName + " isimli kullanıcı geçti.";
        
        sendAlert(title, message);
        
        // Discord webhook gönder
        DiscordWebhook.sendPlayerBlockingAlert(accountName, blockingPlayerName);
    }
    
    /**
     * Kritik uyarı gönder - Yanlış blok
     */
    public static void alertWrongBlock(String accountName, String blockName) {
        String title = "BananaClient";
        String message = accountName + " isimli hesabınız " + blockName + " bloğunu kazmaya çalıştığı için durduruldu.";
        
        sendAlert(title, message);
        
        // Discord webhook gönder
        DiscordWebhook.sendWrongBlockAlert(accountName, blockName);
    }
    
    /**
     * Kritik uyarı gönder - Halka içine oyuncu girdi
     */
    public static void alertRadiusEntry(String accountName, String nearbyPlayer) {
        String title = "BananaClient";
        String message = accountName + " isimli hesabınızın algılama alanına " + nearbyPlayer + " isimli kullanıcı girdi.";
        
        sendAlert(title, message);
        
        // Discord webhook gönder
        DiscordWebhook.sendRadiusAlert(accountName, nearbyPlayer);
    }
    
    /**
     * Uyarı gönder - Ses + Windows Notification + Chat
     */
    private static void sendAlert(String title, String message) {
        // 1. Chat mesajı
        sendChatMessage("§c§l[UYARI] §r" + message);
        
        // 2. Sesli uyarı
        playAlertSound();
        
        // 3. Windows notification (ayrı thread'de)
        new Thread(() -> {
            try {
                showWindowsNotification(title, message);
            } catch (Exception e) {
                MuzMod.LOGGER.error("[Alert] Windows notification error: " + e.getMessage());
            }
        }).start();
        
        MuzMod.LOGGER.warn("[Alert] " + message);
    }
    
    /**
     * Chat'e mesaj gönder
     */
    private static void sendChatMessage(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
    
    /**
     * Windows notification göster
     */
    private static void showWindowsNotification(String title, String message) {
        try {
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                
                // Basit bir ikon oluştur
                Image image = Toolkit.getDefaultToolkit().createImage(new byte[0]);
                
                TrayIcon trayIcon = new TrayIcon(image, "BananaClient");
                trayIcon.setImageAutoSize(true);
                
                try {
                    tray.add(trayIcon);
                    trayIcon.displayMessage(title, message, TrayIcon.MessageType.WARNING);
                    
                    // 5 saniye sonra tray'den kaldır
                    Thread.sleep(5000);
                    tray.remove(trayIcon);
                } catch (AWTException e) {
                    MuzMod.LOGGER.error("[Alert] Tray add error: " + e.getMessage());
                }
            } else {
                MuzMod.LOGGER.warn("[Alert] SystemTray not supported");
            }
        } catch (Exception e) {
            MuzMod.LOGGER.error("[Alert] Notification error: " + e.getMessage());
        }
    }
    
    // ===== PUBLIC HELPER METHODS =====
    
    /**
     * Chat'e uyarı mesajı gönder (public)
     */
    public static void sendChatAlert(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
    
    /**
     * Title göster
     */
    public static void showTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (mc.ingameGUI != null) {
            mc.ingameGUI.displayTitle(title, null, fadeIn, stay, fadeOut);
            if (subtitle != null) {
                mc.ingameGUI.displayTitle(null, subtitle, fadeIn, stay, fadeOut);
            }
        }
    }
    
    /**
     * Uyarı sesi çal (public)
     */
    public static void playAlertSound() {
        try {
            Toolkit.getDefaultToolkit().beep();
            if (mc.thePlayer != null) {
                mc.thePlayer.playSound("random.anvil_land", 1.0f, 1.0f);
            }
        } catch (Exception e) {
            MuzMod.LOGGER.error("[Alert] Sound error: " + e.getMessage());
        }
    }
    
    /**
     * Windows bildirimi gönder (public)
     */
    public static void sendWindowsNotification(String title, String message) {
        new Thread(() -> {
            try {
                showWindowsNotification(title, message);
            } catch (Exception e) {
                MuzMod.LOGGER.error("[Alert] Windows notification error: " + e.getMessage());
            }
        }).start();
    }
}
