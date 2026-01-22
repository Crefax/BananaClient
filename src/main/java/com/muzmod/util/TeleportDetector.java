package com.muzmod.util;

import com.muzmod.MuzMod;
import com.muzmod.state.StateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Set;

/**
 * Teleport Detection System v1.0
 * 
 * Sunucudan gelen S08PacketPlayerPosLook paketlerini dinler.
 * Bu paket sunucu oyuncuyu zorla bir pozisyona taşıdığında gönderilir:
 * - /tp komutu
 * - /forcetp komutu
 * - Anti-cheat rollback
 * - Spawn'a gönderme
 * 
 * Normal yürüme bu paketi GÖNDERMEZ - bu yüzden 1 blok bile ışınlanma algılanır!
 */
public class TeleportDetector {
    
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static boolean registered = false;
    private static final String HANDLER_NAME = "muzmod_teleport_detector";
    
    // Son teleport bilgisi
    private static double lastTeleportFromX = 0;
    private static double lastTeleportFromY = 0;
    private static double lastTeleportFromZ = 0;
    private static double lastTeleportToX = 0;
    private static double lastTeleportToY = 0;
    private static double lastTeleportToZ = 0;
    private static long lastTeleportTime = 0;
    
    // Algılama eşiği (blok) - 0.1 = 10cm, neredeyse her şeyi algılar
    private static final double TELEPORT_THRESHOLD = 0.1;
    
    // İlk spawn'ı atlamak için
    private static boolean firstSpawnIgnored = false;
    private static long connectionTime = 0;
    
    /**
     * Packet handler'ı pipeline'a ekle
     */
    public static void register() {
        if (registered) return;
        
        try {
            if (mc.getNetHandler() != null && mc.getNetHandler().getNetworkManager() != null) {
                // Zaten varsa kaldır
                try {
                    mc.getNetHandler().getNetworkManager().channel().pipeline().remove(HANDLER_NAME);
                } catch (Exception ignored) {}
                
                TeleportPacketHandler packetHandler = new TeleportPacketHandler();
                mc.getNetHandler().getNetworkManager().channel().pipeline()
                    .addBefore("packet_handler", HANDLER_NAME, packetHandler);
                registered = true;
                connectionTime = System.currentTimeMillis();
                firstSpawnIgnored = false;
                MuzMod.LOGGER.info("[TeleportDetector] Packet handler registered successfully");
            }
        } catch (Exception e) {
            MuzMod.LOGGER.error("[TeleportDetector] Failed to register packet handler", e);
        }
    }
    
    /**
     * Packet handler'ı kaldır
     */
    public static void unregister() {
        if (!registered) return;
        
        try {
            if (mc.getNetHandler() != null && mc.getNetHandler().getNetworkManager() != null) {
                mc.getNetHandler().getNetworkManager().channel().pipeline().remove(HANDLER_NAME);
            }
        } catch (Exception ignored) {}
        
        registered = false;
        MuzMod.LOGGER.info("[TeleportDetector] Packet handler unregistered");
    }
    
    /**
     * Sıfırla (dünya değiştiğinde)
     */
    public static void reset() {
        unregister();
        lastTeleportFromX = 0;
        lastTeleportFromY = 0;
        lastTeleportFromZ = 0;
        lastTeleportToX = 0;
        lastTeleportToY = 0;
        lastTeleportToZ = 0;
        lastTeleportTime = 0;
        firstSpawnIgnored = false;
        connectionTime = 0;
    }
    
    /**
     * Kayıtlı mı?
     */
    public static boolean isRegistered() {
        return registered;
    }
    
    /**
     * Son teleport zamanını al
     */
    public static long getLastTeleportTime() {
        return lastTeleportTime;
    }
    
    /**
     * Son teleport mesafesini al
     */
    public static double getLastTeleportDistance() {
        double dx = lastTeleportToX - lastTeleportFromX;
        double dy = lastTeleportToY - lastTeleportFromY;
        double dz = lastTeleportToZ - lastTeleportFromZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Netty packet handler - S08 paketini yakalar
     */
    @ChannelHandler.Sharable
    private static class TeleportPacketHandler extends ChannelInboundHandlerAdapter {
        
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof S08PacketPlayerPosLook) {
                handleTeleportPacket((S08PacketPlayerPosLook) msg);
            }
            
            // Paketi devam ettir (önemli!)
            super.channelRead(ctx, msg);
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            MuzMod.LOGGER.error("[TeleportDetector] Exception in packet handler", cause);
            super.exceptionCaught(ctx, cause);
        }
        
        private void handleTeleportPacket(S08PacketPlayerPosLook packet) {
            if (mc.thePlayer == null) return;
            
            // İlk 3 saniye içindeki teleportları atla (spawn/login)
            if (System.currentTimeMillis() - connectionTime < 3000) {
                if (!firstSpawnIgnored) {
                    firstSpawnIgnored = true;
                    MuzMod.LOGGER.info("[TeleportDetector] Initial spawn teleport ignored");
                }
                return;
            }
            
            double fromX = mc.thePlayer.posX;
            double fromY = mc.thePlayer.posY;
            double fromZ = mc.thePlayer.posZ;
            
            double toX = packet.getX();
            double toY = packet.getY();
            double toZ = packet.getZ();
            
            // Relative flag kontrolü - eğer relative ise mevcut pozisyona eklenir
            Set<S08PacketPlayerPosLook.EnumFlags> flags = packet.func_179834_f();
            if (flags.contains(S08PacketPlayerPosLook.EnumFlags.X)) {
                toX += fromX;
            }
            if (flags.contains(S08PacketPlayerPosLook.EnumFlags.Y)) {
                toY += fromY;
            }
            if (flags.contains(S08PacketPlayerPosLook.EnumFlags.Z)) {
                toZ += fromZ;
            }
            
            double deltaX = toX - fromX;
            double deltaY = toY - fromY;
            double deltaZ = toZ - fromZ;
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            
            // Kaydet
            lastTeleportFromX = fromX;
            lastTeleportFromY = fromY;
            lastTeleportFromZ = fromZ;
            lastTeleportToX = toX;
            lastTeleportToY = toY;
            lastTeleportToZ = toZ;
            lastTeleportTime = System.currentTimeMillis();
            
            // Eşik değerinden fazla ise teleport olarak algıla
            if (distance > TELEPORT_THRESHOLD) {
                MuzMod.LOGGER.warn("===========================================");
                MuzMod.LOGGER.warn("[TeleportDetector] *** TELEPORT DETECTED ***");
                MuzMod.LOGGER.warn("[TeleportDetector] Distance: " + String.format("%.2f", distance) + " blocks");
                MuzMod.LOGGER.warn("[TeleportDetector] From: " + formatPos(fromX, fromY, fromZ));
                MuzMod.LOGGER.warn("[TeleportDetector] To: " + formatPos(toX, toY, toZ));
                MuzMod.LOGGER.warn("===========================================");
                
                // Main thread'de işle
                final double fFromX = fromX, fFromY = fromY, fFromZ = fromZ;
                final double fToX = toX, fToY = toY, fToZ = toZ;
                final double fDistance = distance;
                
                mc.addScheduledTask(() -> {
                    onTeleportDetected(fFromX, fFromY, fFromZ, fToX, fToY, fToZ, fDistance);
                });
            }
        }
        
        private void onTeleportDetected(double fromX, double fromY, double fromZ,
                                        double toX, double toY, double toZ, double distance) {
            if (mc.thePlayer == null) return;
            
            String playerName = mc.thePlayer.getName();
            
            // 1. Chat mesajı
            String chatMessage = "§c§l[!] IŞINLANMA ALGILANDI! §r§c" + 
                String.format("%.1f", distance) + " blok uzağa taşındınız!";
            AlertSystem.sendChatAlert(chatMessage);
            
            String coordMessage = "§c[!] §7Önceki: §f" + formatPos(fromX, fromY, fromZ) + 
                " §7-> Yeni: §f" + formatPos(toX, toY, toZ);
            AlertSystem.sendChatAlert(coordMessage);
            
            // 2. Title göster
            AlertSystem.showTitle("§c§lIŞINLANMA!", 
                "§e" + String.format("%.1f", distance) + " blok taşındınız!", 
                10, 60, 20);
            
            // 3. Ses çal
            AlertSystem.playAlertSound();
            
            // 4. Windows bildirimi
            AlertSystem.sendWindowsNotification(
                "Işınlanma Algılandı!", 
                String.format("%.1f blok uzağa ışınlandınız! Bot durduruldu.", distance)
            );
            
            // 5. Discord webhook
            sendDiscordAlert(playerName, fromX, fromY, fromZ, toX, toY, toZ, distance);
            
            // 6. Bot'u durdur
            stopBot();
        }
        
        private void sendDiscordAlert(String playerName, 
                                      double fromX, double fromY, double fromZ,
                                      double toX, double toY, double toZ, 
                                      double distance) {
            String direction = getDirection(toX - fromX, toY - fromY, toZ - fromZ);
            
            String description = "**" + String.format("%.2f", distance) + "** blok " + direction + " yönüne ışınlandınız!\n\n" +
                "📍 **Önceki Konum:**\n" +
                "`X: " + String.format("%.1f", fromX) + ", Y: " + String.format("%.1f", fromY) + ", Z: " + String.format("%.1f", fromZ) + "`\n\n" +
                "📍 **Yeni Konum:**\n" +
                "`X: " + String.format("%.1f", toX) + ", Y: " + String.format("%.1f", toY) + ", Z: " + String.format("%.1f", toZ) + "`\n\n" +
                "⚠️ **Bu bir ForceTP, /tp komutu veya anti-cheat rollback olabilir!**";
            
            DiscordWebhook.sendAlert(
                playerName,
                "🌀 Işınlanma Algılandı!",
                description,
                DiscordWebhook.COLOR_RED
            );
        }
        
        private void stopBot() {
            StateManager stateManager = MuzMod.instance.getStateManager();
            if (stateManager == null) return;
            
            String currentStateName = stateManager.getCurrentState().getName();
            
            // Idle değilse durdur
            if (!currentStateName.equals("Idle")) {
                MuzMod.LOGGER.warn("[TeleportDetector] Stopping bot! Current state: " + currentStateName);
                stateManager.forceState("idle");
            }
        }
        
        private String getDirection(double deltaX, double deltaY, double deltaZ) {
            StringBuilder dir = new StringBuilder();
            
            // Y ekseni
            if (Math.abs(deltaY) > 0.5) {
                dir.append(deltaY > 0 ? "Yukarı" : "Aşağı");
            }
            
            // X ve Z ekseni
            if (Math.abs(deltaX) > 0.5 || Math.abs(deltaZ) > 0.5) {
                if (dir.length() > 0) dir.append(" + ");
                
                if (Math.abs(deltaX) > Math.abs(deltaZ)) {
                    dir.append(deltaX > 0 ? "Doğu (+X)" : "Batı (-X)");
                } else {
                    dir.append(deltaZ > 0 ? "Güney (+Z)" : "Kuzey (-Z)");
                }
            }
            
            return dir.length() > 0 ? dir.toString() : "Bilinmeyen";
        }
        
        private String formatPos(double x, double y, double z) {
            return String.format("X: %.1f, Y: %.1f, Z: %.1f", x, y, z);
        }
    }
    
    private static String formatPos(double x, double y, double z) {
        return String.format("X: %.1f, Y: %.1f, Z: %.1f", x, y, z);
    }
}
