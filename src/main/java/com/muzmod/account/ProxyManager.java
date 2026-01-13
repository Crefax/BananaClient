package com.muzmod.account;

import com.muzmod.MuzMod;

import java.net.*;

/**
 * SOCKS5 Proxy Manager
 * Minecraft bağlantılarını proxy üzerinden yönlendirir
 */
public class ProxyManager {
    
    private static ProxyManager instance;
    
    private boolean proxyEnabled;
    private String proxyHost;
    private int proxyPort;
    private String proxyUsername;
    private String proxyPassword;
    
    // Orijinal proxy ayarları (geri yüklemek için)
    private String originalProxyHost;
    private String originalProxyPort;
    private String originalSocksVersion;
    
    private ProxyManager() {
        // Orijinal sistem ayarlarını kaydet
        originalProxyHost = System.getProperty("socksProxyHost");
        originalProxyPort = System.getProperty("socksProxyPort");
        originalSocksVersion = System.getProperty("socksProxyVersion");
        
        this.proxyEnabled = false;
    }
    
    public static ProxyManager getInstance() {
        if (instance == null) {
            instance = new ProxyManager();
        }
        return instance;
    }
    
    /**
     * Proxy ayarla
     */
    public void setProxy(String host, int port, String username, String password) {
        this.proxyHost = host;
        this.proxyPort = port;
        this.proxyUsername = username;
        this.proxyPassword = password;
    }
    
    /**
     * Proxy'yi aktifle - Sistem genelinde SOCKS5 proxy ayarla
     */
    public void enableProxy() {
        if (proxyHost == null || proxyHost.isEmpty()) {
            MuzMod.LOGGER.warn("[ProxyManager] No proxy host set!");
            return;
        }
        
        try {
            // SOCKS5 proxy ayarları
            System.setProperty("socksProxyHost", proxyHost);
            System.setProperty("socksProxyPort", String.valueOf(proxyPort));
            System.setProperty("socksProxyVersion", "5");
            
            // Proxy authentication (eğer varsa)
            if (proxyUsername != null && !proxyUsername.isEmpty()) {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestingHost().equalsIgnoreCase(proxyHost)) {
                            return new PasswordAuthentication(proxyUsername, proxyPassword.toCharArray());
                        }
                        return null;
                    }
                });
                
                // Java SOCKS5 auth ayarı
                System.setProperty("java.net.socks.username", proxyUsername);
                System.setProperty("java.net.socks.password", proxyPassword);
            }
            
            proxyEnabled = true;
            MuzMod.LOGGER.info("[ProxyManager] Proxy enabled: " + proxyHost + ":" + proxyPort);
            
        } catch (Exception e) {
            MuzMod.LOGGER.error("[ProxyManager] Failed to enable proxy: " + e.getMessage());
        }
    }
    
    /**
     * Proxy'yi devre dışı bırak
     */
    public void disableProxy() {
        try {
            // Orijinal ayarlara geri dön
            if (originalProxyHost != null) {
                System.setProperty("socksProxyHost", originalProxyHost);
            } else {
                System.clearProperty("socksProxyHost");
            }
            
            if (originalProxyPort != null) {
                System.setProperty("socksProxyPort", originalProxyPort);
            } else {
                System.clearProperty("socksProxyPort");
            }
            
            if (originalSocksVersion != null) {
                System.setProperty("socksProxyVersion", originalSocksVersion);
            } else {
                System.clearProperty("socksProxyVersion");
            }
            
            // Auth temizle
            System.clearProperty("java.net.socks.username");
            System.clearProperty("java.net.socks.password");
            Authenticator.setDefault(null);
            
            proxyEnabled = false;
            MuzMod.LOGGER.info("[ProxyManager] Proxy disabled");
            
        } catch (Exception e) {
            MuzMod.LOGGER.error("[ProxyManager] Failed to disable proxy: " + e.getMessage());
        }
    }
    
    /**
     * Proxy bağlantısını test et
     */
    public boolean testConnection() {
        if (proxyHost == null || proxyHost.isEmpty()) {
            return false;
        }
        
        try {
            // SOCKS5 proxy ile test bağlantısı
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));
            
            // Auth varsa ayarla
            if (proxyUsername != null && !proxyUsername.isEmpty()) {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxyUsername, proxyPassword.toCharArray());
                    }
                });
            }
            
            // Google'a test bağlantısı
            URL testUrl = new URL("http://www.google.com");
            HttpURLConnection conn = (HttpURLConnection) testUrl.openConnection(proxy);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            
            MuzMod.LOGGER.info("[ProxyManager] Proxy test successful, response: " + responseCode);
            return responseCode == 200;
            
        } catch (Exception e) {
            MuzMod.LOGGER.error("[ProxyManager] Proxy test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Proxy nesnesi oluştur (Netty bağlantıları için)
     */
    public Proxy getProxy() {
        if (!proxyEnabled || proxyHost == null) {
            return Proxy.NO_PROXY;
        }
        return new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));
    }
    
    // Getters
    public boolean isProxyEnabled() {
        return proxyEnabled;
    }
    
    public String getProxyHost() {
        return proxyHost;
    }
    
    public int getProxyPort() {
        return proxyPort;
    }
    
    public String getProxyUsername() {
        return proxyUsername;
    }
    
    public String getProxyPassword() {
        return proxyPassword;
    }
    
    public String getProxyString() {
        if (!proxyEnabled || proxyHost == null) {
            return "Disabled";
        }
        return proxyHost + ":" + proxyPort;
    }
}
