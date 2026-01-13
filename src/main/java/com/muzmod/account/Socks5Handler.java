package com.muzmod.account;

import com.muzmod.MuzMod;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;

/**
 * SOCKS5 Proxy Handler for Netty
 * 
 * Bu handler proxy'ye bağlandıktan sonra SOCKS5 handshake yapar
 * ve hedef sunucuya tunnel açar.
 */
public class Socks5Handler extends ChannelInboundHandlerAdapter {
    
    private static final byte SOCKS_VERSION = 0x05;
    private static final byte AUTH_NONE = 0x00;
    private static final byte AUTH_PASSWORD = 0x02;
    private static final byte CMD_CONNECT = 0x01;
    private static final byte ADDR_TYPE_DOMAIN = 0x03;
    private static final byte ADDR_TYPE_IPV4 = 0x01;
    
    public enum State {
        GREETING,
        AUTH,
        CONNECT,
        CONNECTED
    }
    
    private State state = State.GREETING;
    private final String targetHost;
    private final int targetPort;
    private final String username;
    private final String password;
    
    private ChannelHandlerContext savedCtx;
    
    public Socks5Handler(String targetHost, int targetPort) {
        this(targetHost, targetPort, null, null);
    }
    
    public Socks5Handler(String targetHost, int targetPort, String username, String password) {
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.username = username;
        this.password = password;
        
        MuzMod.LOGGER.info("[Socks5Handler] Created for target: " + targetHost + ":" + targetPort);
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.savedCtx = ctx;
        MuzMod.LOGGER.info("[Socks5Handler] Channel active, sending greeting...");
        
        // SOCKS5 greeting gönder
        sendGreeting(ctx);
    }
    
    private void sendGreeting(ChannelHandlerContext ctx) {
        state = State.GREETING;
        
        ByteBuf buf;
        if (username != null && !username.isEmpty()) {
            // Username/password auth destekle
            buf = ctx.alloc().buffer(4);
            buf.writeByte(SOCKS_VERSION);  // VER
            buf.writeByte(2);               // NMETHODS
            buf.writeByte(AUTH_NONE);       // NO AUTH
            buf.writeByte(AUTH_PASSWORD);   // USER/PASS
        } else {
            // Sadece no-auth
            buf = ctx.alloc().buffer(3);
            buf.writeByte(SOCKS_VERSION);  // VER
            buf.writeByte(1);               // NMETHODS
            buf.writeByte(AUTH_NONE);       // NO AUTH
        }
        
        ctx.writeAndFlush(buf).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    MuzMod.LOGGER.info("[Socks5Handler] Greeting sent");
                } else {
                    MuzMod.LOGGER.error("[Socks5Handler] Failed to send greeting: " + future.cause());
                }
            }
        });
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (state == State.CONNECTED) {
            // Handshake tamamlandı, veriyi üst katmana ilet
            ctx.fireChannelRead(msg);
            return;
        }
        
        ByteBuf buf = (ByteBuf) msg;
        
        try {
            switch (state) {
                case GREETING:
                    handleGreetingResponse(ctx, buf);
                    break;
                case AUTH:
                    handleAuthResponse(ctx, buf);
                    break;
                case CONNECT:
                    handleConnectResponse(ctx, buf);
                    break;
                default:
                    ctx.fireChannelRead(msg);
                    return;
            }
        } finally {
            buf.release();
        }
    }
    
    private void handleGreetingResponse(ChannelHandlerContext ctx, ByteBuf buf) {
        if (buf.readableBytes() < 2) {
            MuzMod.LOGGER.error("[Socks5Handler] Greeting response too short");
            ctx.close();
            return;
        }
        
        byte version = buf.readByte();
        byte method = buf.readByte();
        
        MuzMod.LOGGER.info("[Socks5Handler] Greeting response: version=" + version + ", method=" + method);
        
        if (version != SOCKS_VERSION) {
            MuzMod.LOGGER.error("[Socks5Handler] Invalid SOCKS version: " + version);
            ctx.close();
            return;
        }
        
        if (method == AUTH_PASSWORD) {
            // Username/password auth gerekli
            MuzMod.LOGGER.info("[Socks5Handler] Sending auth...");
            sendAuth(ctx);
        } else if (method == AUTH_NONE) {
            // Auth gerekmiyor, connect gönder
            MuzMod.LOGGER.info("[Socks5Handler] No auth required, sending connect...");
            sendConnect(ctx);
        } else if (method == (byte) 0xFF) {
            MuzMod.LOGGER.error("[Socks5Handler] No acceptable auth methods!");
            ctx.close();
        } else {
            MuzMod.LOGGER.error("[Socks5Handler] Unsupported auth method: " + method);
            ctx.close();
        }
    }
    
    private void sendAuth(ChannelHandlerContext ctx) {
        state = State.AUTH;
        
        byte[] userBytes = (username != null ? username : "").getBytes(CharsetUtil.UTF_8);
        byte[] passBytes = (password != null ? password : "").getBytes(CharsetUtil.UTF_8);
        
        ByteBuf buf = ctx.alloc().buffer(3 + userBytes.length + passBytes.length);
        buf.writeByte(0x01);                // Auth version
        buf.writeByte(userBytes.length);    // Username length
        buf.writeBytes(userBytes);          // Username
        buf.writeByte(passBytes.length);    // Password length
        buf.writeBytes(passBytes);          // Password
        
        ctx.writeAndFlush(buf);
        MuzMod.LOGGER.info("[Socks5Handler] Auth sent");
    }
    
    private void handleAuthResponse(ChannelHandlerContext ctx, ByteBuf buf) {
        if (buf.readableBytes() < 2) {
            MuzMod.LOGGER.error("[Socks5Handler] Auth response too short");
            ctx.close();
            return;
        }
        
        byte version = buf.readByte();
        byte status = buf.readByte();
        
        MuzMod.LOGGER.info("[Socks5Handler] Auth response: version=" + version + ", status=" + status);
        
        if (status != 0x00) {
            MuzMod.LOGGER.error("[Socks5Handler] Auth failed! Status: " + status);
            ctx.close();
            return;
        }
        
        MuzMod.LOGGER.info("[Socks5Handler] Auth successful, sending connect...");
        sendConnect(ctx);
    }
    
    private void sendConnect(ChannelHandlerContext ctx) {
        state = State.CONNECT;
        
        byte[] hostBytes = targetHost.getBytes(CharsetUtil.UTF_8);
        
        ByteBuf buf = ctx.alloc().buffer(7 + hostBytes.length);
        buf.writeByte(SOCKS_VERSION);       // VER
        buf.writeByte(CMD_CONNECT);         // CMD = CONNECT
        buf.writeByte(0x00);                // RSV
        buf.writeByte(ADDR_TYPE_DOMAIN);    // ATYP = Domain name
        buf.writeByte(hostBytes.length);    // Domain length
        buf.writeBytes(hostBytes);          // Domain name
        buf.writeShort(targetPort);         // DST.PORT
        
        ctx.writeAndFlush(buf);
        MuzMod.LOGGER.info("[Socks5Handler] Connect request sent for " + targetHost + ":" + targetPort);
    }
    
    private void handleConnectResponse(ChannelHandlerContext ctx, ByteBuf buf) {
        if (buf.readableBytes() < 4) {
            MuzMod.LOGGER.error("[Socks5Handler] Connect response too short");
            ctx.close();
            return;
        }
        
        byte version = buf.readByte();
        byte status = buf.readByte();
        buf.readByte(); // RSV
        byte addrType = buf.readByte();
        
        MuzMod.LOGGER.info("[Socks5Handler] Connect response: version=" + version + 
                          ", status=" + status + ", addrType=" + addrType);
        
        // Adresi oku ve atla (ilgilenmiyoruz)
        try {
            switch (addrType) {
                case ADDR_TYPE_IPV4:
                    if (buf.readableBytes() >= 6) {
                        buf.skipBytes(4); // IPv4
                        buf.skipBytes(2); // Port
                    }
                    break;
                case ADDR_TYPE_DOMAIN:
                    if (buf.readableBytes() >= 1) {
                        int len = buf.readByte() & 0xFF;
                        if (buf.readableBytes() >= len + 2) {
                            buf.skipBytes(len); // Domain
                            buf.skipBytes(2);   // Port
                        }
                    }
                    break;
                case 0x04: // IPv6
                    if (buf.readableBytes() >= 18) {
                        buf.skipBytes(16); // IPv6
                        buf.skipBytes(2);  // Port
                    }
                    break;
            }
        } catch (Exception e) {
            // Adresi okuyamadık, sorun değil
        }
        
        if (status != 0x00) {
            String errorMsg = getSocksErrorMessage(status);
            MuzMod.LOGGER.error("[Socks5Handler] Connect failed: " + errorMsg);
            ctx.close();
            return;
        }
        
        // Başarılı! Artık tunnel açık
        state = State.CONNECTED;
        MuzMod.LOGGER.info("[Socks5Handler] *** TUNNEL ESTABLISHED to " + targetHost + ":" + targetPort + " ***");
        
        // Pipeline'dan kendimizi kaldır, artık şeffaf proxy
        ctx.pipeline().remove(this);
        
        // Upstream handler'lara channel active bildir (Minecraft'ın handshake başlatması için)
        ctx.fireChannelActive();
    }
    
    private String getSocksErrorMessage(byte status) {
        switch (status) {
            case 0x01: return "General SOCKS server failure";
            case 0x02: return "Connection not allowed by ruleset";
            case 0x03: return "Network unreachable";
            case 0x04: return "Host unreachable";
            case 0x05: return "Connection refused";
            case 0x06: return "TTL expired";
            case 0x07: return "Command not supported";
            case 0x08: return "Address type not supported";
            default: return "Unknown error: " + status;
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        MuzMod.LOGGER.error("[Socks5Handler] Exception: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}
