package com.muzmod.account;

/**
 * Alt hesap bilgileri - Proxy destekli
 */
public class AltAccount {
    
    private String username;
    private boolean proxyEnabled;
    private String proxyHost;
    private int proxyPort;
    private String proxyUsername;
    private String proxyPassword;
    
    // Son giriş zamanı
    private long lastLogin;
    
    public AltAccount() {}
    
    public AltAccount(String username) {
        this.username = username;
        this.proxyEnabled = false;
        this.lastLogin = 0;
    }
    
    public AltAccount(String username, String proxyHost, int proxyPort) {
        this.username = username;
        this.proxyEnabled = true;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.lastLogin = 0;
    }
    
    public AltAccount(String username, String proxyHost, int proxyPort, String proxyUser, String proxyPass) {
        this.username = username;
        this.proxyEnabled = true;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUsername = proxyUser;
        this.proxyPassword = proxyPass;
        this.lastLogin = 0;
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public boolean isProxyEnabled() {
        return proxyEnabled;
    }
    
    public void setProxyEnabled(boolean proxyEnabled) {
        this.proxyEnabled = proxyEnabled;
    }
    
    public String getProxyHost() {
        return proxyHost;
    }
    
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }
    
    public int getProxyPort() {
        return proxyPort;
    }
    
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }
    
    public String getProxyUsername() {
        return proxyUsername;
    }
    
    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }
    
    public String getProxyPassword() {
        return proxyPassword;
    }
    
    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }
    
    public long getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public boolean hasProxyAuth() {
        return proxyUsername != null && !proxyUsername.isEmpty() 
            && proxyPassword != null && !proxyPassword.isEmpty();
    }
    
    public String getProxyString() {
        if (!proxyEnabled || proxyHost == null) {
            return "Proxy Yok";
        }
        return proxyHost + ":" + proxyPort;
    }
    
    @Override
    public String toString() {
        return username + (proxyEnabled ? " [P]" : "");
    }
}
