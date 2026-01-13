package com.muzmod.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.muzmod.MuzMod;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Alt hesap yöneticisi - Hesapları kaydet/yükle
 */
public class AltManager {
    
    private static AltManager instance;
    
    private final File altsFile;
    private final Gson gson;
    private List<AltAccount> accounts;
    
    // Aktif alt hesap (giriş yapılmış)
    private AltAccount activeAccount;
    
    private AltManager() {
        // BananaClient/alts.json
        File configDir = new File("BananaClient");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        this.altsFile = new File(configDir, "alts.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.accounts = new ArrayList<>();
        
        load();
    }
    
    public static AltManager getInstance() {
        if (instance == null) {
            instance = new AltManager();
        }
        return instance;
    }
    
    /**
     * Yeni hesap ekle
     */
    public void addAccount(AltAccount account) {
        // Aynı isimde hesap varsa güncelle
        for (int i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).getUsername().equalsIgnoreCase(account.getUsername())) {
                accounts.set(i, account);
                save();
                return;
            }
        }
        
        accounts.add(account);
        save();
        MuzMod.LOGGER.info("[AltManager] Added account: " + account.getUsername());
    }
    
    /**
     * Hesap sil
     */
    public void removeAccount(String username) {
        accounts.removeIf(acc -> acc.getUsername().equalsIgnoreCase(username));
        save();
        MuzMod.LOGGER.info("[AltManager] Removed account: " + username);
    }
    
    /**
     * Hesap sil (index ile)
     */
    public void removeAccount(int index) {
        if (index >= 0 && index < accounts.size()) {
            String name = accounts.get(index).getUsername();
            accounts.remove(index);
            save();
            MuzMod.LOGGER.info("[AltManager] Removed account: " + name);
        }
    }
    
    /**
     * Hesap al
     */
    public AltAccount getAccount(String username) {
        for (AltAccount acc : accounts) {
            if (acc.getUsername().equalsIgnoreCase(username)) {
                return acc;
            }
        }
        return null;
    }
    
    /**
     * Hesap al (index ile)
     */
    public AltAccount getAccount(int index) {
        if (index >= 0 && index < accounts.size()) {
            return accounts.get(index);
        }
        return null;
    }
    
    /**
     * Tüm hesapları al
     */
    public List<AltAccount> getAccounts() {
        return accounts;
    }
    
    /**
     * Hesap sayısı
     */
    public int getAccountCount() {
        return accounts.size();
    }
    
    /**
     * Hesap ile giriş yap
     */
    public boolean login(AltAccount account) {
        if (account == null) return false;
        
        // Önce proxy'i aktifle (eğer varsa)
        if (account.isProxyEnabled()) {
            ProxyManager.getInstance().setProxy(
                account.getProxyHost(),
                account.getProxyPort(),
                account.getProxyUsername(),
                account.getProxyPassword()
            );
            ProxyManager.getInstance().enableProxy();
        } else {
            ProxyManager.getInstance().disableProxy();
        }
        
        // Offline giriş yap
        boolean success = AccountManager.getInstance().loginOffline(account.getUsername());
        
        if (success) {
            activeAccount = account;
            account.setLastLogin(System.currentTimeMillis());
            save();
        }
        
        return success;
    }
    
    /**
     * Aktif hesabı al
     */
    public AltAccount getActiveAccount() {
        return activeAccount;
    }
    
    /**
     * Aktif hesabı temizle
     */
    public void clearActiveAccount() {
        activeAccount = null;
        ProxyManager.getInstance().disableProxy();
    }
    
    /**
     * Kaydet
     */
    public void save() {
        try (Writer writer = new FileWriter(altsFile)) {
            gson.toJson(accounts, writer);
            MuzMod.LOGGER.info("[AltManager] Saved " + accounts.size() + " accounts");
        } catch (IOException e) {
            MuzMod.LOGGER.error("[AltManager] Save error: " + e.getMessage());
        }
    }
    
    /**
     * Yükle
     */
    public void load() {
        if (!altsFile.exists()) {
            accounts = new ArrayList<>();
            return;
        }
        
        try (Reader reader = new FileReader(altsFile)) {
            AltAccount[] loaded = gson.fromJson(reader, AltAccount[].class);
            if (loaded != null) {
                accounts = new ArrayList<>();
                for (AltAccount acc : loaded) {
                    accounts.add(acc);
                }
            }
            MuzMod.LOGGER.info("[AltManager] Loaded " + accounts.size() + " accounts");
        } catch (Exception e) {
            MuzMod.LOGGER.error("[AltManager] Load error: " + e.getMessage());
            accounts = new ArrayList<>();
        }
    }
}
