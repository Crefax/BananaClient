package com.muzmod.util;

import com.muzmod.MuzMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Discord Webhook utility for sending embedded messages
 */
public class DiscordWebhook {
    
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final String AVATAR_API = "https://crafatar.com/avatars/";
    private static final String MINOTAR_API = "https://minotar.net/helm/";
    
    // Embed renkleri (decimal)
    public static final int COLOR_RED = 0xFF0000;      // Kırmızı - Hata
    public static final int COLOR_ORANGE = 0xFF9800;   // Turuncu - Uyarı
    public static final int COLOR_GREEN = 0x00FF00;    // Yeşil - Başarı
    public static final int COLOR_BLUE = 0x3498db;     // Mavi - Bilgi
    public static final int COLOR_PURPLE = 0x9B59B6;   // Mor - Obsidian
    
    /**
     * Oyuncu önüne geçti uyarısı gönder
     */
    public static void sendPlayerBlockingAlert(String playerName, String blockingPlayer) {
        String webhookUrl = MuzMod.instance.getConfig().getDiscordWebhookUrl();
        if (!isWebhookValid(webhookUrl)) {
            MuzMod.LOGGER.warn("[Discord] Invalid webhook URL: " + (webhookUrl == null ? "null" : webhookUrl.substring(0, Math.min(30, webhookUrl.length()))));
            return;
        }
        
        JsonObject embed = createEmbed(
            "⚠️ Oyuncu Engeli!",
            "**" + blockingPlayer + "** isimli oyuncu önünüze geçti!",
            COLOR_ORANGE,
            playerName
        );
        
        addField(embed, "Durum", "Bot durduruldu", true);
        addField(embed, "Engel", blockingPlayer, true);
        addField(embed, "Zaman", getCurrentTime(), true);
        
        sendEmbed(webhookUrl, embed, playerName);
    }
    
    /**
     * Yanlış blok uyarısı gönder
     */
    public static void sendWrongBlockAlert(String playerName, String blockName) {
        String webhookUrl = MuzMod.instance.getConfig().getDiscordWebhookUrl();
        if (!isWebhookValid(webhookUrl)) return;
        
        JsonObject embed = createEmbed(
            "🚫 Yanlış Blok!",
            "Obsidyen yerine **" + blockName + "** bloğu tespit edildi!",
            COLOR_RED,
            playerName
        );
        
        addField(embed, "Durum", "Bot durduruldu", true);
        addField(embed, "Blok", blockName, true);
        addField(embed, "Zaman", getCurrentTime(), true);
        
        sendEmbed(webhookUrl, embed, playerName);
    }
    
    /**
     * Halka içine oyuncu girdi uyarısı
     */
    public static void sendRadiusAlert(String playerName, String nearbyPlayer) {
        String webhookUrl = MuzMod.instance.getConfig().getDiscordWebhookUrl();
        if (!isWebhookValid(webhookUrl)) return;
        
        JsonObject embed = createEmbed(
            "👁️ Oyuncu Yaklaştı!",
            "**" + nearbyPlayer + "** algılama çapı içine girdi!",
            COLOR_ORANGE,
            playerName
        );
        
        addField(embed, "Durum", "Bot durduruldu", true);
        addField(embed, "Oyuncu", nearbyPlayer, true);
        addField(embed, "Zaman", getCurrentTime(), true);
        
        sendEmbed(webhookUrl, embed, playerName);
    }
    
    /**
     * Genel uyarı gönder
     */
    public static void sendAlert(String playerName, String title, String description, int color) {
        String webhookUrl = MuzMod.instance.getConfig().getDiscordWebhookUrl();
        if (!isWebhookValid(webhookUrl)) return;
        
        JsonObject embed = createEmbed(title, description, color, playerName);
        addField(embed, "Zaman", getCurrentTime(), true);
        
        sendEmbed(webhookUrl, embed, playerName);
    }
    
    /**
     * Embed oluştur
     */
    private static JsonObject createEmbed(String title, String description, int color, String playerName) {
        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        embed.addProperty("description", description);
        embed.addProperty("color", color);
        
        // Thumbnail (oyuncu avatarı)
        JsonObject thumbnail = new JsonObject();
        thumbnail.addProperty("url", MINOTAR_API + playerName + "/64.png");
        embed.add("thumbnail", thumbnail);
        
        // Footer
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "MuzMod v0.9.3 • " + playerName);
        footer.addProperty("icon_url", AVATAR_API + playerName + "?size=16&overlay");
        embed.add("footer", footer);
        
        // Fields array
        embed.add("fields", new JsonArray());
        
        return embed;
    }
    
    /**
     * Field ekle
     */
    private static void addField(JsonObject embed, String name, String value, boolean inline) {
        JsonObject field = new JsonObject();
        field.addProperty("name", name);
        field.addProperty("value", value);
        field.addProperty("inline", inline);
        
        embed.getAsJsonArray("fields").add(field);
    }
    
    /**
     * Embed gönder (async)
     */
    private static void sendEmbed(String webhookUrl, JsonObject embed, String playerName) {
        if (!MuzMod.instance.getConfig().isDiscordWebhookEnabled()) {
            MuzMod.LOGGER.info("[Discord] Webhook disabled in config");
            return;
        }
        
        MuzMod.LOGGER.info("[Discord] Attempting to send webhook to: " + webhookUrl.substring(0, Math.min(60, webhookUrl.length())) + "...");
        
        executor.submit(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("username", "MuzMod Alert");
                payload.addProperty("avatar_url", MINOTAR_API + playerName + "/64.png");
                
                JsonArray embeds = new JsonArray();
                embeds.add(embed);
                payload.add("embeds", embeds);
                
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 204 || responseCode == 200) {
                    MuzMod.LOGGER.info("[Discord] Webhook sent successfully");
                } else {
                    MuzMod.LOGGER.warn("[Discord] Webhook failed with code: " + responseCode);
                }
                
                conn.disconnect();
            } catch (Exception e) {
                MuzMod.LOGGER.error("[Discord] Failed to send webhook", e);
            }
        });
    }
    
    /**
     * Webhook URL geçerli mi?
     */
    private static boolean isWebhookValid(String url) {
        if (url == null || url.isEmpty()) return false;
        return url.startsWith("https://discord.com/api/webhooks/") || 
               url.startsWith("https://discordapp.com/api/webhooks/") ||
               url.startsWith("https://canary.discord.com/api/webhooks/") ||
               url.startsWith("https://ptb.discord.com/api/webhooks/");
    }
    
    /**
     * Şu anki zamanı al
     */
    private static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }
}
