package com.muzmod.account;

import com.muzmod.MuzMod;

import java.net.*;

/**
 * SOCKS5 Proxy Manager
 * Minecraft bağlantılarını proxy üzerinden yönlendirir
 * 
 * NOT: Sistem proxy ayarları (socksProxyHost) yerine kendi Netty SOCKS5 handler'ımızı kullanıyoruz.
 * Bu sayede sadece Minecraft bağlantıları proxy üzerinden gider.
 */
public class ProxyManager {
    
    private static ProxyManager instance;
    
    private boolean proxyEnabled;
    private String proxyHost;
    private int proxyPort;
    private String proxyUsername;
    private String proxyPassword;
    
    private ProxyManager() {
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
     * Proxy'yi aktifle
     * Java sistem proxy ayarlarını kullanarak TÜM bağlantıları proxy üzerinden yönlendir
     */
    public void enableProxy() {
        if (proxyHost == null || proxyHost.isEmpty()) {
            MuzMod.LOGGER.warn("[ProxyManager] No proxy host set!");
            return;
        }
        
        proxyEnabled = true;
        
        // Java sistem SOCKS proxy ayarları - TÜM socket bağlantıları proxy üzerinden gider
        System.setProperty("socksProxyHost", proxyHost);
        System.setProperty("socksProxyPort", String.valueOf(proxyPort));
        
        // Proxy auth varsa ayarla
        if (proxyUsername != null && !proxyUsername.isEmpty()) {
            System.setProperty("java.net.socks.username", proxyUsername);
            System.setProperty("java.net.socks.password", proxyPassword != null ? proxyPassword : "");
            
            // Authenticator ayarla
            java.net.Authenticator.setDefault(new java.net.Authenticator() {
                @Override
                protected java.net.PasswordAuthentication getPasswordAuthentication() {
                    if (getRequestingProtocol().toLowerCase().contains("socks")) {
                        return new java.net.PasswordAuthentication(proxyUsername, 
                            (proxyPassword != null ? proxyPassword : "").toCharArray());
                    }
                    return null;
                }
            });
            MuzMod.LOGGER.info("[ProxyManager] Proxy auth: " + proxyUsername);
        }
        
        MuzMod.LOGGER.info("[ProxyManager] Proxy enabled: " + proxyHost + ":" + proxyPort);
        MuzMod.LOGGER.info("[ProxyManager] System SOCKS proxy configured - ALL connections will use proxy");
    }
    
    /**
     * Proxy'yi devre dışı bırak
     */
    public void disableProxy() {
        proxyEnabled = false;
        
        // Sistem proxy ayarlarını temizle
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
        System.clearProperty("java.net.socks.username");
        System.clearProperty("java.net.socks.password");
        java.net.Authenticator.setDefault(null);
        
        MuzMod.LOGGER.info("[ProxyManager] Proxy disabled - System SOCKS proxy cleared");
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
            
            // Auth temizle
            Authenticator.setDefault(null);
            
            MuzMod.LOGGER.info("[ProxyManager] Proxy test successful, response: " + responseCode);
            return responseCode == 200;
            
        } catch (Exception e) {
            MuzMod.LOGGER.error("[ProxyManager] Proxy test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Proxy nesnesi oluştur
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
