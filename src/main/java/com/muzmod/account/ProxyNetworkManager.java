package com.muzmod.account;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.muzmod.MuzMod;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.LazyLoadBase;
import net.minecraft.util.MessageDeserializer;
import net.minecraft.util.MessageDeserializer2;
import net.minecraft.util.MessageSerializer;
import net.minecraft.util.MessageSerializer2;

import java.net.InetAddress;
import java.net.SocketAddress;

/**
 * Proxy destekli NetworkManager oluşturucu
 * Minecraft'ın orijinal createNetworkManagerAndConnect metodunu proxy ile wrapper'lar
 */
public class ProxyNetworkManager {
    
    // Event loop group - Minecraft'ınkiyle aynı
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
        
        // Proxy aktif değilse normal bağlantı
        if (!pm.isProxyEnabled()) {
            MuzMod.LOGGER.info("[ProxyNetworkManager] Proxy disabled, using direct connection");
            return NetworkManager.createNetworkManagerAndConnect(address, serverPort, useNativeTransport);
        }
        
        MuzMod.LOGGER.info("[ProxyNetworkManager] Creating proxy connection to " + address.getHostAddress() + ":" + serverPort);
        MuzMod.LOGGER.info("[ProxyNetworkManager] Using proxy: " + pm.getProxyHost() + ":" + pm.getProxyPort());
        
        final NetworkManager networkmanager = new NetworkManager(EnumPacketDirection.CLIENTBOUND);
        
        // SOCKS5 handler oluştur
        final Socks5Handler socks5Handler;
        if (pm.getProxyUsername() != null && !pm.getProxyUsername().isEmpty()) {
            socks5Handler = new Socks5Handler(pm.getProxyHost(), pm.getProxyPort(), 
                                              pm.getProxyUsername(), pm.getProxyPassword());
        } else {
            socks5Handler = new Socks5Handler(pm.getProxyHost(), pm.getProxyPort());
        }
        
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
                    
                    // SOCKS5 handler'ı İLK olarak ekle
                    channel.pipeline()
                        .addLast("socks5", socks5Handler)
                        .addLast("timeout", new ReadTimeoutHandler(30))
                        .addLast("splitter", new MessageDeserializer2())
                        .addLast("decoder", new MessageDeserializer(EnumPacketDirection.CLIENTBOUND))
                        .addLast("prepender", new MessageSerializer2())
                        .addLast("encoder", new MessageSerializer(EnumPacketDirection.SERVERBOUND))
                        .addLast("packet_handler", networkmanager);
                }
            })
            .channel(NioSocketChannel.class);
        
        // Hedef sunucuya bağlan (Socks5Handler proxy üzerinden yönlendirecek)
        bootstrap.connect(address, serverPort).syncUninterruptibly();
        
        return networkmanager;
    }
    
    /**
     * Hostname ile bağlantı (DNS çözümleme proxy tarafında yapılır)
     */
    public static NetworkManager createNetworkManagerAndConnect(String hostname, int serverPort, boolean useNativeTransport) {
        ProxyManager pm = ProxyManager.getInstance();
        
        // Proxy aktif değilse normal bağlantı
        if (!pm.isProxyEnabled()) {
            MuzMod.LOGGER.info("[ProxyNetworkManager] Proxy disabled, using direct connection");
            try {
                return NetworkManager.createNetworkManagerAndConnect(InetAddress.getByName(hostname), serverPort, useNativeTransport);
            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve hostname: " + hostname, e);
            }
        }
        
        MuzMod.LOGGER.info("[ProxyNetworkManager] Creating proxy connection to " + hostname + ":" + serverPort);
        MuzMod.LOGGER.info("[ProxyNetworkManager] Using proxy: " + pm.getProxyHost() + ":" + pm.getProxyPort());
        
        final NetworkManager networkmanager = new NetworkManager(EnumPacketDirection.CLIENTBOUND);
        
        // SOCKS5 handler oluştur
        final Socks5Handler socks5Handler;
        if (pm.getProxyUsername() != null && !pm.getProxyUsername().isEmpty()) {
            socks5Handler = new Socks5Handler(pm.getProxyHost(), pm.getProxyPort(), 
                                              pm.getProxyUsername(), pm.getProxyPassword());
        } else {
            socks5Handler = new Socks5Handler(pm.getProxyHost(), pm.getProxyPort());
        }
        
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
                    
                    // SOCKS5 handler'ı İLK olarak ekle
                    channel.pipeline()
                        .addLast("socks5", socks5Handler)
                        .addLast("timeout", new ReadTimeoutHandler(30))
                        .addLast("splitter", new MessageDeserializer2())
                        .addLast("decoder", new MessageDeserializer(EnumPacketDirection.CLIENTBOUND))
                        .addLast("prepender", new MessageSerializer2())
                        .addLast("encoder", new MessageSerializer(EnumPacketDirection.SERVERBOUND))
                        .addLast("packet_handler", networkmanager);
                }
            })
            .channel(NioSocketChannel.class);
        
        // Hostname ile bağlan - Socks5Handler DNS çözümlemesini proxy'ye bırakır
        try {
            bootstrap.connect(InetAddress.getByName(hostname), serverPort).syncUninterruptibly();
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect via proxy", e);
        }
        
        return networkmanager;
    }
}
