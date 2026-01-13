package com.muzmod.util;

import com.muzmod.MuzMod;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * BananaClient için ayrı log dosyası
 * Loglar: .minecraft/BananaClient/logs/ klasörüne yazılır
 */
public class BananaLogger {
    
    private static BananaLogger instance;
    private File logFile;
    private PrintWriter writer;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    private BananaLogger() {
        try {
            // .minecraft/BananaClient/logs/ klasörünü oluştur
            File mcDir = new File(System.getenv("APPDATA"), ".minecraft");
            File bananaDir = new File(mcDir, "BananaClient");
            File logsDir = new File(bananaDir, "logs");
            
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
            
            // Log dosyası: banana-2026-01-13.log
            String fileName = "banana-" + dateFormat.format(new Date()) + ".log";
            logFile = new File(logsDir, fileName);
            
            // Append mode
            writer = new PrintWriter(new FileWriter(logFile, true), true);
            
            // Başlangıç
            writer.println("");
            writer.println("========================================");
            writer.println("BananaClient Log Started: " + new Date());
            writer.println("========================================");
            
            System.out.println("[BananaClient] Log file: " + logFile.getAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("[BananaClient] Failed to create log file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static BananaLogger getInstance() {
        if (instance == null) {
            instance = new BananaLogger();
        }
        return instance;
    }
    
    private void write(String level, String tag, String message) {
        if (writer == null) return;
        
        String timestamp = timeFormat.format(new Date());
        String line = String.format("[%s] [%s] [%s] %s", timestamp, level, tag, message);
        
        writer.println(line);
        writer.flush();
        
        // Ayrıca Minecraft loguna da yaz
        MuzMod.LOGGER.info("[" + tag + "] " + message);
    }
    
    public void info(String tag, String message) {
        write("INFO", tag, message);
    }
    
    public void warn(String tag, String message) {
        write("WARN", tag, message);
    }
    
    public void error(String tag, String message) {
        write("ERROR", tag, message);
    }
    
    public void error(String tag, String message, Throwable t) {
        write("ERROR", tag, message);
        if (writer != null && t != null) {
            t.printStackTrace(writer);
            writer.flush();
        }
    }
    
    public void debug(String tag, String message) {
        write("DEBUG", tag, message);
    }
    
    /**
     * Proxy bağlantı logları için özel method
     */
    public void proxy(String message) {
        write("PROXY", "Proxy", message);
    }
    
    /**
     * Log dosyasının yolunu döndür
     */
    public String getLogFilePath() {
        return logFile != null ? logFile.getAbsolutePath() : "N/A";
    }
    
    /**
     * Kapatma
     */
    public void close() {
        if (writer != null) {
            writer.println("========================================");
            writer.println("BananaClient Log Ended: " + new Date());
            writer.println("========================================");
            writer.close();
        }
    }
}
