package com.muzmod.gui;

import com.muzmod.account.GuiProxyConnecting;
import com.muzmod.account.ProxyManager;
import com.muzmod.util.BananaLogger;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.multiplayer.ServerData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Custom GuiMultiplayer that intercepts connectToServer() for proxy connections.
 * This is the ONLY way to intercept connections BEFORE the thread starts.
 * 
 * connectToServer() is PRIVATE in vanilla, so we use reflection for normal calls.
 */
public class GuiProxyMultiplayer extends GuiMultiplayer {
    
    private final BananaLogger log = BananaLogger.getInstance();
    private static Method connectToServerMethod = null;
    
    // Play butonu tıklama intercept için
    private boolean interceptNextConnect = false;
    private ServerData pendingServer = null;
    
    // Çift tıklama tespiti için
    private long lastClickTime = 0;
    private int lastClickedSlot = -1;
    private static final long DOUBLE_CLICK_TIME = 250; // ms
    
    static {
        try {
            // func_146791_a = connectToServer (MCP name)
            connectToServerMethod = GuiMultiplayer.class.getDeclaredMethod("func_146791_a", ServerData.class);
            connectToServerMethod.setAccessible(true);
        } catch (Exception e) {
            try {
                // Try obfuscated name
                for (Method m : GuiMultiplayer.class.getDeclaredMethods()) {
                    if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == ServerData.class) {
                        connectToServerMethod = m;
                        connectToServerMethod.setAccessible(true);
                        break;
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }
    
    public GuiProxyMultiplayer(GuiScreen parent) {
        super(parent);
        log.proxy("GuiProxyMultiplayer created");
    }
    
    /**
     * This method is called when user double-clicks, clicks Join Server, or play button.
     * We intercept it to use proxy connection.
     */
    public void connectToServerProxy(ServerData server) {
        ProxyManager pm = ProxyManager.getInstance();
        
        log.proxy("*** connectToServerProxy called ***");
        log.proxy("Server: " + (server != null ? server.serverIP : "null"));
        log.proxy("Proxy enabled: " + pm.isProxyEnabled());
        
        if (pm.isProxyEnabled() && server != null && server.serverIP != null) {
            // Proxy ile bağlan - GuiConnecting OLUŞTURULMAYACAK!
            log.proxy(">>> REDIRECTING TO PROXY CONNECTION <<<");
            this.mc.displayGuiScreen(new GuiProxyConnecting(this, server));
        } else {
            // Normal bağlantı - reflection ile parent'ın connectToServer'ını çağır
            log.proxy("Using normal connection (proxy disabled or no server)");
            callSuperConnectToServer(server);
        }
    }
    
    private void callSuperConnectToServer(ServerData server) {
        try {
            if (connectToServerMethod != null) {
                connectToServerMethod.invoke(this, server);
            } else {
                log.error("GuiProxyMultiplayer", "connectToServerMethod is null!");
            }
        } catch (Exception e) {
            log.error("GuiProxyMultiplayer", "Failed to call connectToServer: " + e.getMessage());
        }
    }
    
    /**
     * Override actionPerformed to intercept Join Server button (ID=1)
     */
    @Override
    protected void actionPerformed(net.minecraft.client.gui.GuiButton button) throws java.io.IOException {
        log.proxy("*** actionPerformed called: button.id=" + button.id + ", text='" + button.displayString + "' ***");
        
        // Join Server button veya benzeri
        if (button.id == 1 || button.id == 7) {
            ServerData server = getSelectedServer();
            log.proxy("Selected server: " + (server != null ? server.serverIP : "null"));
            
            if (server != null && ProxyManager.getInstance().isProxyEnabled()) {
                log.proxy("*** JOIN SERVER BUTTON - intercepting ***");
                this.mc.setServerData(server);
                connectToServerProxy(server);
                return;
            }
        }
        
        // Diğer butonlar için parent'ı çağır
        log.proxy("Passing to parent actionPerformed");
        super.actionPerformed(button);
    }
    
    /**
     * Get the currently selected server
     */
    private ServerData getSelectedServer() {
        try {
            // field_146803_h = serverListSelector
            Field selectorField = GuiMultiplayer.class.getDeclaredField("field_146803_h");
            selectorField.setAccessible(true);
            Object selector = selectorField.get(this);
            
            if (selector != null) {
                // Get selected index from selector (field_148197_o)
                Field indexField = selector.getClass().getDeclaredField("field_148197_o");
                indexField.setAccessible(true);
                int index = indexField.getInt(selector);
                
                // Get server list (field_146804_i)
                Field listField = GuiMultiplayer.class.getDeclaredField("field_146804_i");
                listField.setAccessible(true);
                net.minecraft.client.multiplayer.ServerList serverList = 
                    (net.minecraft.client.multiplayer.ServerList) listField.get(this);
                
                if (serverList != null && index >= 0 && index < serverList.countServers()) {
                    return serverList.getServerData(index);
                }
            }
        } catch (Exception e) {
            log.error("GuiProxyMultiplayer", "Failed to get selected server: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Get server at specific index
     */
    private ServerData getServerAt(int index) {
        try {
            Field listField = GuiMultiplayer.class.getDeclaredField("field_146804_i");
            listField.setAccessible(true);
            net.minecraft.client.multiplayer.ServerList serverList = 
                (net.minecraft.client.multiplayer.ServerList) listField.get(this);
            
            if (serverList != null && index >= 0 && index < serverList.countServers()) {
                return serverList.getServerData(index);
            }
        } catch (Exception e) {
            log.error("GuiProxyMultiplayer", "Failed to get server at " + index);
        }
        return null;
    }
    
    /**
     * Override mouseClicked to COMPLETELY handle server list interactions when proxy is enabled.
     * We DO NOT call super.mouseClicked() for list area - this prevents vanilla connectToServer()
     * from ever being called, eliminating parallel connections completely.
     * 
     * Handles ALL mouse buttons (left, right, middle) to prevent any vanilla connection.
     */
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        if (ProxyManager.getInstance().isProxyEnabled()) {
            
            // Hangi slot'a tıklandı tespit et
            int clickedSlot = getClickedSlot(mouseX, mouseY);
            
            if (clickedSlot >= 0) {
                long now = System.currentTimeMillis();
                ServerData server = getServerAt(clickedSlot);
                
                // 1. Çift tıklama kontrolü (HERHANGİ BİR MOUSE BUTONU) - BAĞLAN
                if (clickedSlot == lastClickedSlot && (now - lastClickTime) < DOUBLE_CLICK_TIME) {
                    if (server != null) {
                        log.proxy("*** DOUBLE-CLICK (button=" + mouseButton + ") - CONNECTING via proxy ***");
                        log.proxy("Server: " + server.serverIP);
                        this.mc.setServerData(server);
                        connectToServerProxy(server);
                        lastClickTime = 0;
                        lastClickedSlot = -1;
                        return;
                    }
                }
                
                // 2. Play butonu kontrolü (sol tık) - BAĞLAN
                if (mouseButton == 0 && isPlayButtonClick(mouseX, mouseY, clickedSlot)) {
                    if (server != null) {
                        log.proxy("*** PLAY BUTTON - CONNECTING via proxy ***");
                        log.proxy("Server: " + server.serverIP);
                        this.mc.setServerData(server);
                        connectToServerProxy(server);
                        return;
                    }
                }
                
                // 3. Tek tıklama - sadece SEÇIM yap (super.mouseClicked ÇAĞIRMA!)
                lastClickTime = now;
                lastClickedSlot = clickedSlot;
                
                // Manuel slot seçimi
                selectSlot(clickedSlot);
                log.debug("GuiProxyMultiplayer", "Selected slot " + clickedSlot + " (button=" + mouseButton + ")");
                
                // super.mouseClicked() ÇAĞIRMA - vanilla bağlantı başlatabilir!
                // Sadece butonları handle et (list dışı)
                if (mouseButton == 0) {
                    handleButtonsOnly(mouseX, mouseY, mouseButton);
                }
                return;
            }
        }
        
        // List dışına tıklama veya proxy kapalı - normal işlem
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    /**
     * Override mouseReleased to prevent any vanilla connection triggers
     */
    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (ProxyManager.getInstance().isProxyEnabled()) {
            int clickedSlot = getClickedSlot(mouseX, mouseY);
            if (clickedSlot >= 0) {
                // List alanında mouse release - vanilla'ya geçirme
                // Sadece butonları handle et
                super.mouseReleased(mouseX, mouseY, state);
                return;
            }
        }
        super.mouseReleased(mouseX, mouseY, state);
    }
    
    /**
     * Manuel slot seçimi - vanilla'yı bypass eder
     */
    private void selectSlot(int slotIndex) {
        try {
            // field_146803_h = serverListSelector
            Field selectorField = GuiMultiplayer.class.getDeclaredField("field_146803_h");
            selectorField.setAccessible(true);
            Object selector = selectorField.get(this);
            
            if (selector != null) {
                // field_148197_o = selected index
                Class<?> clazz = selector.getClass();
                while (clazz != null) {
                    for (Field f : clazz.getDeclaredFields()) {
                        if (f.getName().equals("field_148197_o")) {
                            f.setAccessible(true);
                            f.setInt(selector, slotIndex);
                            log.debug("GuiProxyMultiplayer", "Set field_148197_o to " + slotIndex);
                            
                            // selectServer callback - butonları günceller
                            try {
                                Method selectMethod = GuiMultiplayer.class.getDeclaredMethod("func_146790_a", int.class);
                                selectMethod.setAccessible(true);
                                selectMethod.invoke(this, slotIndex);
                            } catch (Exception e) {
                                // Method bulunamadı, sorun değil
                            }
                            return;
                        }
                    }
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception e) {
            log.error("GuiProxyMultiplayer", "selectSlot error: " + e.getMessage());
        }
    }
    
    /**
     * Sadece butonları handle et (Add, Edit, Delete vs.)
     * List'e dokunma - vanilla bağlantı başlatabilir
     */
    private void handleButtonsOnly(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        // Butonları kontrol et
        for (int i = 0; i < this.buttonList.size(); ++i) {
            net.minecraft.client.gui.GuiButton button = (net.minecraft.client.gui.GuiButton)this.buttonList.get(i);
            if (button.mousePressed(this.mc, mouseX, mouseY)) {
                button.playPressSound(this.mc.getSoundHandler());
                this.actionPerformed(button);
                return;
            }
        }
    }
    
    /**
     * Hangi slot'a tıklandığını tespit et
     */
    private int getClickedSlot(int mouseX, int mouseY) {
        try {
            Field selectorField = GuiMultiplayer.class.getDeclaredField("field_146803_h");
            selectorField.setAccessible(true);
            Object selector = selectorField.get(this);
            
            if (selector == null) return -1;
            
            // List bounds
            int top = 32;
            int bottom = this.height - 64;
            int left = 0;
            int right = this.width;
            float scroll = 0;
            int slotHeight = 36;
            
            Class<?> clazz = selector.getClass();
            while (clazz != null) {
                for (Field f : clazz.getDeclaredFields()) {
                    f.setAccessible(true);
                    String name = f.getName();
                    try {
                        if (name.equals("field_148153_b")) top = f.getInt(selector);
                        else if (name.equals("field_148154_c")) bottom = f.getInt(selector);
                        else if (name.equals("field_148152_e")) left = f.getInt(selector);
                        else if (name.equals("field_148151_d")) right = f.getInt(selector);
                        else if (name.equals("field_148169_q")) scroll = f.getFloat(selector);
                        else if (name.equals("field_148149_f")) slotHeight = f.getInt(selector);
                    } catch (Exception e) {}
                }
                clazz = clazz.getSuperclass();
            }
            
            // Mouse list alanı içinde mi?
            if (mouseY < top || mouseY > bottom) return -1;
            
            // Slot index hesapla
            int relativeY = mouseY - top + (int)scroll;
            return relativeY / slotHeight;
            
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Play butonuna tıklandı mı?
     * Play butonu slot'un sağ tarafında, üst kısmında
     */
    private boolean isPlayButtonClick(int mouseX, int mouseY, int slotIndex) {
        try {
            Field selectorField = GuiMultiplayer.class.getDeclaredField("field_146803_h");
            selectorField.setAccessible(true);
            Object selector = selectorField.get(this);
            
            if (selector == null) return false;
            
            int top = 32;
            int right = this.width;
            float scroll = 0;
            int slotHeight = 36;
            
            Class<?> clazz = selector.getClass();
            while (clazz != null) {
                for (Field f : clazz.getDeclaredFields()) {
                    f.setAccessible(true);
                    String name = f.getName();
                    try {
                        if (name.equals("field_148153_b")) top = f.getInt(selector);
                        else if (name.equals("field_148151_d")) right = f.getInt(selector);
                        else if (name.equals("field_148169_q")) scroll = f.getFloat(selector);
                        else if (name.equals("field_148149_f")) slotHeight = f.getInt(selector);
                    } catch (Exception e) {}
                }
                clazz = clazz.getSuperclass();
            }
            
            // Slot içindeki Y pozisyonu
            int relativeY = mouseY - top + (int)scroll;
            int slotRelativeY = relativeY % slotHeight;
            
            // Play butonu: sağ tarafta (son 35px), üst kısımda (ilk 32px)
            // ServerListEntryNormal'da play butonu x=entryWidth-35'te başlar
            int playButtonX = right - 35;
            
            if (mouseX >= playButtonX && slotRelativeY <= 32) {
                log.debug("GuiProxyMultiplayer", "Play button area: mouseX=" + mouseX + " >= " + playButtonX);
                return true;
            }
            
        } catch (Exception e) {
            log.error("GuiProxyMultiplayer", "isPlayButtonClick error: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Intercept confirmClicked which is called after direct connect dialog
     */
    @Override
    public void confirmClicked(boolean result, int id) {
        // Direct connect dialog (id=1)
        if (result && id == 1) {
            // Get the direct connect server data
            try {
                Field directField = GuiMultiplayer.class.getDeclaredField("field_146801_C");
                directField.setAccessible(true);
                ServerData directServer = (ServerData) directField.get(this);
                
                if (directServer != null && ProxyManager.getInstance().isProxyEnabled()) {
                    log.proxy("*** DIRECT CONNECT - intercepting ***");
                    this.mc.setServerData(directServer);
                    connectToServerProxy(directServer);
                    return;
                }
            } catch (Exception e) {
                log.error("GuiProxyMultiplayer", "Failed to intercept direct connect: " + e.getMessage());
            }
        }
        super.confirmClicked(result, id);
    }
}
