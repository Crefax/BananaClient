package com.muzmod.account;

import com.muzmod.MuzMod;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * SOCKS5 Proxy Handler for Netty 4.0
 * Minecraft sunucu bağlantılarını SOCKS5 proxy üzerinden yönlendirir
 */
public class Socks5Handler extends ChannelDuplexHandler {
    
    private static final byte SOCKS_VERSION = 0x05;
    private static final byte AUTH_NONE = 0x00;
    private static final byte AUTH_PASSWORD = 0x02;
    private static final byte CMD_CONNECT = 0x01;
    private static final byte ADDR_TYPE_DOMAIN = 0x03;
    private static final byte ADDR_TYPE_IPV4 = 0x01;
    
    private enum State {
        INIT,
        AUTH_SENT,
        AUTH_RESPONSE,
        CONNECT_SENT,
        CONNECTED
    }
    
    private State state = State.INIT;
    private final String proxyHost;
    private final int proxyPort;
    private final String username;
    private final String password;
    private String targetHost;
    private int targetPort;
    
    private ChannelPromise connectPromise;
    private ByteBuf pendingData;
    
    public Socks5Handler(String proxyHost, int proxyPort) {
        this(proxyHost, proxyPort, null, null);
    }
    
    public Socks5Handler(String proxyHost, int proxyPort, String username, String password) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.username = username;
        this.password = password;
    }
    
    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, 
                       SocketAddress localAddress, ChannelPromise promise) throws Exception {
        
        if (remoteAddress instanceof InetSocketAddress) {
            InetSocketAddress targetAddr = (InetSocketAddress) remoteAddress;
            this.targetHost = targetAddr.getHostString();
            this.targetPort = targetAddr.getPort();
            this.connectPromise = promise;
            
            // Proxy'ye bağlan (hedef yerine)
            InetSocketAddress proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
            
            MuzMod.LOGGER.info("[Socks5Handler] Connecting to proxy " + proxyHost + ":" + proxyPort + 
                            " for target " + targetHost + ":" + targetPort);
            
            // Gerçek bağlantıyı proxy'ye yap
            ChannelPromise proxyPromise = ctx.newPromise();
            proxyPromise.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        // Proxy'ye bağlandık, SOCKS5 handshake başlat
                        sendGreeting(ctx);
                    } else {
                        connectPromise.setFailure(future.cause());
                    }
                }
            });
            
            super.connect(ctx, proxyAddr, localAddress, proxyPromise);
        } else {
            super.connect(ctx, remoteAddress, localAddress, promise);
        }
    }
    
    private void sendGreeting(ChannelHandlerContext ctx) {
        state = State.AUTH_SENT;
        
        ByteBuf buf;
        if (username != null && !username.isEmpty()) {
            // Username/password auth destekle
            buf = Unpooled.buffer(4);
            buf.writeByte(SOCKS_VERSION);
            buf.writeByte(2); // 2 auth method
            buf.writeByte(AUTH_NONE);
            buf.writeByte(AUTH_PASSWORD);
        } else {
            // Sadece no-auth
            buf = Unpooled.buffer(3);
            buf.writeByte(SOCKS_VERSION);
            buf.writeByte(1); // 1 auth method
            buf.writeByte(AUTH_NONE);
        }
        
        ctx.writeAndFlush(buf);
        MuzMod.LOGGER.debug("[Socks5Handler] Sent greeting");
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (state == State.CONNECTED) {
            // Proxy handshake tamamlandı, normal data akışı
            super.channelRead(ctx, msg);
            return;
        }
        
        ByteBuf buf = (ByteBuf) msg;
        
        try {
            switch (state) {
                case AUTH_SENT:
                    handleAuthResponse(ctx, buf);
                    break;
                    
                case AUTH_RESPONSE:
                    handleAuthResult(ctx, buf);
                    break;
                    
                case CONNECT_SENT:
                    handleConnectResponse(ctx, buf);
                    break;
                    
                default:
                    buf.release();
                    break;
            }
        } catch (Exception e) {
            buf.release();
            failConnect(ctx, e);
        }
    }
    
    private void handleAuthResponse(ChannelHandlerContext ctx, ByteBuf buf) {
        if (buf.readableBytes() < 2) {
            buf.release();
            return;
        }
        
        byte version = buf.readByte();
        byte method = buf.readByte();
        buf.release();
        
        if (version != SOCKS_VERSION) {
            failConnect(ctx, new Exception("Invalid SOCKS version: " + version));
            return;
        }
        
        MuzMod.LOGGER.debug("[Socks5Handler] Auth method selected: " + method);
        
        if (method == AUTH_PASSWORD) {
            // Username/password auth gönder
            sendAuth(ctx);
            state = State.AUTH_RESPONSE;
        } else if (method == AUTH_NONE) {
            // Auth gerekmiyor, connect gönder
            sendConnect(ctx);
        } else {
            failConnect(ctx, new Exception("Unsupported auth method: " + method));
        }
    }
    
    private void sendAuth(ChannelHandlerContext ctx) {
        byte[] userBytes = username.getBytes(CharsetUtil.UTF_8);
        byte[] passBytes = password.getBytes(CharsetUtil.UTF_8);
        
        ByteBuf buf = Unpooled.buffer(3 + userBytes.length + passBytes.length);
        buf.writeByte(0x01); // Auth version
        buf.writeByte(userBytes.length);
        buf.writeBytes(userBytes);
        buf.writeByte(passBytes.length);
        buf.writeBytes(passBytes);
        
        ctx.writeAndFlush(buf);
        MuzMod.LOGGER.debug("[Socks5Handler] Sent auth credentials");
    }
    
    private void handleAuthResult(ChannelHandlerContext ctx, ByteBuf buf) {
        if (buf.readableBytes() < 2) {
            buf.release();
            return;
        }
        
        byte version = buf.readByte();
        byte status = buf.readByte();
        buf.release();
        
        if (status != 0x00) {
            failConnect(ctx, new Exception("SOCKS5 authentication failed! Status: " + status));
            return;
        }
        
        MuzMod.LOGGER.debug("[Socks5Handler] Auth successful");
        sendConnect(ctx);
    }
    
    private void sendConnect(ChannelHandlerContext ctx) {
        state = State.CONNECT_SENT;
        
        byte[] hostBytes = targetHost.getBytes(CharsetUtil.UTF_8);
        
        ByteBuf buf = Unpooled.buffer(7 + hostBytes.length);
        buf.writeByte(SOCKS_VERSION);
        buf.writeByte(CMD_CONNECT);
        buf.writeByte(0x00); // Reserved
        buf.writeByte(ADDR_TYPE_DOMAIN); // Domain name
        buf.writeByte(hostBytes.length);
        buf.writeBytes(hostBytes);
        buf.writeShort(targetPort);
        
        ctx.writeAndFlush(buf);
        MuzMod.LOGGER.debug("[Socks5Handler] Sent connect request for " + targetHost + ":" + targetPort);
    }
    
    private void handleConnectResponse(ChannelHandlerContext ctx, ByteBuf buf) {
        if (buf.readableBytes() < 4) {
            buf.release();
            return;
        }
        
        byte version = buf.readByte();
        byte status = buf.readByte();
        buf.readByte(); // Reserved
        byte addrType = buf.readByte();
        
        // Adres bilgisini oku ve atla
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
                        buf.skipBytes(2); // Port
                    }
                }
                break;
            case 0x04: // IPv6
                if (buf.readableBytes() >= 18) {
                    buf.skipBytes(16); // IPv6
                    buf.skipBytes(2); // Port
                }
                break;
        }
        
        buf.release();
        
        if (status != 0x00) {
            String errorMsg = getSocksErrorMessage(status);
            failConnect(ctx, new Exception("SOCKS5 connect failed: " + errorMsg));
            return;
        }
        
        // Bağlantı başarılı!
        state = State.CONNECTED;
        MuzMod.LOGGER.info("[Socks5Handler] Connected to " + targetHost + ":" + targetPort + " via proxy!");
        
        // Orijinal connect promise'i başarılı yap
        if (connectPromise != null) {
            connectPromise.setSuccess();
        }
        
        // Bekleyen data varsa gönder
        if (pendingData != null && pendingData.isReadable()) {
            ctx.writeAndFlush(pendingData);
            pendingData = null;
        }
    }
    
    private String getSocksErrorMessage(byte status) {
        switch (status) {
            case 0x01: return "General failure";
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
    
    private void failConnect(ChannelHandlerContext ctx, Exception e) {
        MuzMod.LOGGER.error("[Socks5Handler] Connection failed: " + e.getMessage());
        
        if (connectPromise != null) {
            connectPromise.setFailure(e);
        }
        
        ctx.close();
    }
    
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (state != State.CONNECTED) {
            // Handshake tamamlanmadan data gönderme, beklet
            if (msg instanceof ByteBuf) {
                if (pendingData == null) {
                    pendingData = Unpooled.buffer();
                }
                pendingData.writeBytes((ByteBuf) msg);
                ((ByteBuf) msg).release();
                promise.setSuccess();
                return;
            }
        }
        
        super.write(ctx, msg, promise);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        MuzMod.LOGGER.error("[Socks5Handler] Exception: " + cause.getMessage());
        failConnect(ctx, new Exception(cause));
    }
}
