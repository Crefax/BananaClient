package com.muzmod.account;

import com.muzmod.MuzMod;
import com.muzmod.util.BananaLogger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;

/**
 * SOCKS5 Proxy Handler for Netty 4.0
 * Proxy'ye bağlandıktan sonra SOCKS5 handshake yapar ve hedef sunucuya tunnel açar.
 */
public class Socks5Handler extends ChannelInboundHandlerAdapter {
    
    private static final byte SOCKS_VERSION = 0x05;
    private static final byte AUTH_NONE = 0x00;
    private static final byte AUTH_PASSWORD = 0x02;
    private static final byte CMD_CONNECT = 0x01;
    private static final byte ADDR_TYPE_DOMAIN = 0x03;
    
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
    
    // Tunnel kurulduğunda signal verecek latch
    private java.util.concurrent.CountDownLatch tunnelLatch;
    
    public Socks5Handler(String targetHost, int targetPort) {
        this(targetHost, targetPort, null, null);
    }
    
    public Socks5Handler(String targetHost, int targetPort, String username, String password) {
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.username = username;
        this.password = password;
        BananaLogger.getInstance().proxy("Created for target: " + targetHost + ":" + targetPort);
    }
    
    public void setTunnelLatch(java.util.concurrent.CountDownLatch latch) {
        this.tunnelLatch = latch;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        BananaLogger.getInstance().proxy("Channel active, sending SOCKS5 greeting...");
        sendGreeting(ctx);
    }
    
    private void sendGreeting(ChannelHandlerContext ctx) {
        state = State.GREETING;
        
        ByteBuf buf;
        if (username != null && !username.isEmpty()) {
            buf = ctx.alloc().buffer(4);
            buf.writeByte(SOCKS_VERSION);
            buf.writeByte(2);
            buf.writeByte(AUTH_NONE);
            buf.writeByte(AUTH_PASSWORD);
        } else {
            buf = ctx.alloc().buffer(3);
            buf.writeByte(SOCKS_VERSION);
            buf.writeByte(1);
            buf.writeByte(AUTH_NONE);
        }
        
        ctx.writeAndFlush(buf).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    BananaLogger.getInstance().proxy("Greeting sent successfully");
                } else {
                    BananaLogger.getInstance().error("Socks5", "Failed to send greeting: " + future.cause());
                }
            }
        });
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (state == State.CONNECTED) {
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
            BananaLogger.getInstance().error("Socks5", "Greeting response too short");
            ctx.close();
            return;
        }
        
        byte version = buf.readByte();
        byte method = buf.readByte();
        
        BananaLogger.getInstance().proxy("Greeting response: version=" + version + ", method=" + method);
        
        if (version != SOCKS_VERSION) {
            BananaLogger.getInstance().error("Socks5", "Invalid SOCKS version: " + version);
            ctx.close();
            return;
        }
        
        if (method == AUTH_PASSWORD) {
            BananaLogger.getInstance().proxy("Server requires authentication");
            sendAuth(ctx);
        } else if (method == AUTH_NONE) {
            BananaLogger.getInstance().proxy("No auth required, sending CONNECT");
            sendConnect(ctx);
        } else if (method == (byte) 0xFF) {
            BananaLogger.getInstance().error("Socks5", "No acceptable auth methods!");
            ctx.close();
        } else {
            BananaLogger.getInstance().error("Socks5", "Unsupported auth method: " + method);
            ctx.close();
        }
    }
    
    private void sendAuth(ChannelHandlerContext ctx) {
        state = State.AUTH;
        
        byte[] userBytes = (username != null ? username : "").getBytes(CharsetUtil.UTF_8);
        byte[] passBytes = (password != null ? password : "").getBytes(CharsetUtil.UTF_8);
        
        ByteBuf buf = ctx.alloc().buffer(3 + userBytes.length + passBytes.length);
        buf.writeByte(0x01);
        buf.writeByte(userBytes.length);
        buf.writeBytes(userBytes);
        buf.writeByte(passBytes.length);
        buf.writeBytes(passBytes);
        
        ctx.writeAndFlush(buf);
        BananaLogger.getInstance().proxy("Auth credentials sent");
    }
    
    private void handleAuthResponse(ChannelHandlerContext ctx, ByteBuf buf) {
        if (buf.readableBytes() < 2) {
            BananaLogger.getInstance().error("Socks5", "Auth response too short");
            ctx.close();
            return;
        }
        
        byte version = buf.readByte();
        byte status = buf.readByte();
        
        BananaLogger.getInstance().proxy("Auth response: version=" + version + ", status=" + status);
        
        if (status != 0x00) {
            BananaLogger.getInstance().error("Socks5", "Authentication failed! Status: " + status);
            ctx.close();
            return;
        }
        
        BananaLogger.getInstance().proxy("Authentication successful!");
        sendConnect(ctx);
    }
    
    private void sendConnect(ChannelHandlerContext ctx) {
        state = State.CONNECT;
        
        byte[] hostBytes = targetHost.getBytes(CharsetUtil.UTF_8);
        
        ByteBuf buf = ctx.alloc().buffer(7 + hostBytes.length);
        buf.writeByte(SOCKS_VERSION);
        buf.writeByte(CMD_CONNECT);
        buf.writeByte(0x00);
        buf.writeByte(ADDR_TYPE_DOMAIN);
        buf.writeByte(hostBytes.length);
        buf.writeBytes(hostBytes);
        buf.writeShort(targetPort);
        
        ctx.writeAndFlush(buf);
        BananaLogger.getInstance().proxy("CONNECT request sent for " + targetHost + ":" + targetPort);
    }
    
    private void handleConnectResponse(ChannelHandlerContext ctx, ByteBuf buf) {
        if (buf.readableBytes() < 4) {
            BananaLogger.getInstance().error("Socks5", "Connect response too short");
            ctx.close();
            return;
        }
        
        byte version = buf.readByte();
        byte status = buf.readByte();
        buf.readByte(); // RSV
        byte addrType = buf.readByte();
        
        BananaLogger.getInstance().proxy("Connect response: version=" + version + ", status=" + status);
        
        // Adresi oku ve atla
        try {
            switch (addrType) {
                case 0x01: // IPv4
                    if (buf.readableBytes() >= 6) {
                        buf.skipBytes(4);
                        buf.skipBytes(2);
                    }
                    break;
                case 0x03: // Domain
                    if (buf.readableBytes() >= 1) {
                        int len = buf.readByte() & 0xFF;
                        if (buf.readableBytes() >= len + 2) {
                            buf.skipBytes(len);
                            buf.skipBytes(2);
                        }
                    }
                    break;
                case 0x04: // IPv6
                    if (buf.readableBytes() >= 18) {
                        buf.skipBytes(16);
                        buf.skipBytes(2);
                    }
                    break;
            }
        } catch (Exception e) {
            // Ignore
        }
        
        if (status != 0x00) {
            BananaLogger.getInstance().error("Socks5", "CONNECT failed! Status: " + getSocksError(status));
            ctx.close();
            return;
        }
        
        // SUCCESS!
        state = State.CONNECTED;
        BananaLogger.getInstance().proxy("*** TUNNEL ESTABLISHED to " + targetHost + ":" + targetPort + " ***");
        
        // Tunnel kuruldu sinyali ver
        if (tunnelLatch != null) {
            tunnelLatch.countDown();
            BananaLogger.getInstance().proxy("Tunnel latch signaled");
        }
        
        // Handler'ı kaldır
        ctx.pipeline().remove(this);
        
        // Upstream'e bildir
        ctx.fireChannelActive();
    }
    
    private String getSocksError(byte status) {
        switch (status) {
            case 0x01: return "General failure";
            case 0x02: return "Connection not allowed";
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
        BananaLogger.getInstance().error("Socks5", "Exception: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}
