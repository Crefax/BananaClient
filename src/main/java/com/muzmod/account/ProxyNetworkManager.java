package com.muzmod.account;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.muzmod.MuzMod;
import com.muzmod.util.BananaLogger;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.LazyLoadBase;
import net.minecraft.util.MessageDeserializer;
import net.minecraft.util.MessageDeserializer2;
import net.minecraft.util.MessageSerializer;
import net.minecraft.util.MessageSerializer2;

import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Proxy destekli NetworkManager oluşturucu
 * Kendi SOCKS5 handler'ımızı kullanır (Netty 4.0 için)
 */
public class ProxyNetworkManager {
    
    public static final LazyLoadBase<NioEventLoopGroup> CLIENT_NIO_EVENTLOOP = new LazyLoadBase<NioEventLoopGroup>() {
        @Override
        protected NioEventLoopGroup load() {
            return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Client IO #%d").setDaemon(true).build());
        }
    };
    
    /**
     * Proxy destekli sunucu bağlantısı oluştur
     */
    public static NetworkManager createNetworkManagerAndConnect(InetAddress address, int serverPort, boolean useNativeTransport) {
        ProxyManager pm = ProxyManager.getInstance();
        
        if (!pm.isProxyEnabled()) {
            MuzMod.LOGGER.info("[ProxyNetworkManager] Proxy disabled, using direct connection");
            return NetworkManager.createNetworkManagerAndConnect(address, serverPort, useNativeTransport);
        }
        
        return createProxyConnection(address.getHostAddress(), serverPort, pm);
    }
    
    /**
     * Hostname ile bağlantı
     */
    public static NetworkManager createNetworkManagerAndConnect(String hostname, int serverPort, boolean useNativeTransport) {
        ProxyManager pm = ProxyManager.getInstance();
        
        if (!pm.isProxyEnabled()) {
            MuzMod.LOGGER.info("[ProxyNetworkManager] Proxy disabled, using direct connection");
            try {
                return NetworkManager.createNetworkManagerAndConnect(InetAddress.getByName(hostname), serverPort, useNativeTransport);
            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve hostname: " + hostname, e);
            }
        }
        
        return createProxyConnection(hostname, serverPort, pm);
    }
    
    /**
     * SOCKS5 proxy bağlantısı oluştur
     */
    private static NetworkManager createProxyConnection(final String targetHost, final int targetPort, final ProxyManager pm) {
        BananaLogger log = BananaLogger.getInstance();
        
        log.proxy("========================================");
        log.proxy("Creating SOCKS5 proxy connection");
        log.proxy("Target: " + targetHost + ":" + targetPort);
        log.proxy("Proxy: " + pm.getProxyHost() + ":" + pm.getProxyPort());
        log.proxy("========================================");
        
        final NetworkManager networkmanager = new NetworkManager(EnumPacketDirection.CLIENTBOUND);
        final CountDownLatch tunnelLatch = new CountDownLatch(1);
        
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(CLIENT_NIO_EVENTLOOP.getValue())
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) throws Exception {
                    try {
                        channel.config().setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
                    } catch (ChannelException e) {
                        // Ignore
                    }
                    
                    ChannelPipeline pipeline = channel.pipeline();
                    
                    // SOCKS5 handler - hedef sunucu bilgisi ile
                    Socks5Handler socks5Handler;
                    if (pm.getProxyUsername() != null && !pm.getProxyUsername().isEmpty()) {
                        socks5Handler = new Socks5Handler(targetHost, targetPort, pm.getProxyUsername(), pm.getProxyPassword());
                    } else {
                        socks5Handler = new Socks5Handler(targetHost, targetPort);
                    }
                    
                    // Tunnel kurulduğunda sinyal verecek latch'i set et
                    socks5Handler.setTunnelLatch(tunnelLatch);
                    
                    pipeline.addLast("socks5", socks5Handler);
                    
                    // Minecraft handler'ları
                    pipeline.addLast("timeout", new ReadTimeoutHandler(30));
                    pipeline.addLast("splitter", new MessageDeserializer2());
                    pipeline.addLast("decoder", new MessageDeserializer(EnumPacketDirection.CLIENTBOUND));
                    pipeline.addLast("prepender", new MessageSerializer2());
                    pipeline.addLast("encoder", new MessageSerializer(EnumPacketDirection.SERVERBOUND));
                    pipeline.addLast("packet_handler", networkmanager);
                    
                    BananaLogger.getInstance().proxy("Pipeline configured with SOCKS5 handler");
                }
            })
            .channel(NioSocketChannel.class);
        
        // PROXY'ye bağlan!
        log.proxy("Connecting to PROXY: " + pm.getProxyHost() + ":" + pm.getProxyPort());
        
        try {
            bootstrap.connect(pm.getProxyHost(), pm.getProxyPort()).syncUninterruptibly();
            log.proxy("Connected to proxy!");
            
            // SOCKS5 tunnel kurulana kadar bekle (max 30 saniye)
            log.proxy("Waiting for SOCKS5 tunnel to be established...");
            boolean tunnelReady = tunnelLatch.await(30, TimeUnit.SECONDS);
            
            if (!tunnelReady) {
                log.error("Proxy", "SOCKS5 tunnel timeout - tunnel not established in 30 seconds");
                throw new RuntimeException("SOCKS5 tunnel timeout");
            }
            
            log.proxy("SOCKS5 tunnel ready, NetworkManager is now usable");
            
        } catch (InterruptedException e) {
            log.error("Proxy", "Interrupted while waiting for tunnel: " + e.getMessage(), e);
            throw new RuntimeException("Interrupted while waiting for tunnel", e);
        } catch (Exception e) {
            log.error("Proxy", "Connection failed: " + e.getMessage(), e);
            throw new RuntimeException("Failed to connect to proxy", e);
        }
        
        return networkmanager;
    }
}
