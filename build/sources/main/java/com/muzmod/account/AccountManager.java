package com.muzmod.account;

import com.mojang.authlib.GameProfile;
import com.muzmod.MuzMod;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * Account Manager - Offline login support
 */
public class AccountManager {
    
    private static AccountManager instance;
    
    // İlk girişteki orijinal session
    private Session originalSession;
    private String originalUsername;
    
    // Şu anki session
    private String currentUsername;
    
    private AccountManager() {
        // Orijinal session'ı kaydet
        Minecraft mc = Minecraft.getMinecraft();
        this.originalSession = mc.getSession();
        this.originalUsername = mc.getSession().getUsername();
        this.currentUsername = this.originalUsername;
        
        MuzMod.LOGGER.info("[AccountManager] Original account: " + originalUsername);
    }
    
    public static AccountManager getInstance() {
        if (instance == null) {
            instance = new AccountManager();
        }
        return instance;
    }
    
    /**
     * Offline hesap ile giriş yap
     */
    public boolean loginOffline(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        username = username.trim();
        
        // Geçersiz karakterleri temizle
        if (!isValidUsername(username)) {
            MuzMod.LOGGER.warn("[AccountManager] Invalid username: " + username);
            return false;
        }
        
        try {
            // Offline UUID oluştur (Minecraft'ın offline UUID formatı)
            UUID offlineUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
            
            // Yeni session oluştur
            Session newSession = new Session(username, offlineUUID.toString().replace("-", ""), "0", "legacy");
            
            // Session'ı değiştir (reflection ile)
            setSession(newSession);
            
            currentUsername = username;
            MuzMod.LOGGER.info("[AccountManager] Logged in as: " + username);
            return true;
            
        } catch (Exception e) {
            MuzMod.LOGGER.error("[AccountManager] Login failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Orijinal hesaba geri dön
     */
    public boolean restoreOriginal() {
        if (originalSession == null) {
            return false;
        }
        
        try {
            setSession(originalSession);
            currentUsername = originalUsername;
            MuzMod.LOGGER.info("[AccountManager] Restored to original: " + originalUsername);
            return true;
        } catch (Exception e) {
            MuzMod.LOGGER.error("[AccountManager] Restore failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Session'ı reflection ile değiştir
     */
    private void setSession(Session session) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        
        // Minecraft sınıfındaki session field'ını bul
        Field sessionField = null;
        
        for (Field field : Minecraft.class.getDeclaredFields()) {
            if (field.getType() == Session.class) {
                sessionField = field;
                break;
            }
        }
        
        if (sessionField == null) {
            throw new Exception("Session field not found");
        }
        
        sessionField.setAccessible(true);
        sessionField.set(mc, session);
    }
    
    /**
     * Kullanıcı adı geçerli mi?
     */
    private boolean isValidUsername(String username) {
        if (username.length() < 1 || username.length() > 16) {
            return false;
        }
        
        // Sadece harf, rakam ve alt çizgi
        for (char c : username.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }
        
        return true;
    }
    
    // Getters
    public String getOriginalUsername() {
        return originalUsername;
    }
    
    public String getCurrentUsername() {
        return currentUsername;
    }
    
    public boolean isUsingOriginal() {
        return currentUsername.equals(originalUsername);
    }
    
    public Session getOriginalSession() {
        return originalSession;
    }
}
