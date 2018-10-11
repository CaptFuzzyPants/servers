package com.zoomulus.servers;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import com.google.common.collect.Lists;

@Slf4j
public class BasicNettyServer implements Server
{
    private final List<ServerConnector> connectors;
    private List<ChannelFuture> channels = Lists.newArrayList();
    private final EventLoopGroup masterGroup;
    private final EventLoopGroup slaveGroup;
    
    private boolean shutdownCalled = false;

    protected String getName()
    {
        return this.getClass().getSimpleName();
    }

    public BasicNettyServer(final ServerConnector connector)
    {
        this(Lists.newArrayList(connector));
    }

    public BasicNettyServer(final List<ServerConnector> connectors)
    {
        this.connectors = connectors;
        masterGroup = new NioEventLoopGroup();
        slaveGroup = new NioEventLoopGroup();
    }
    
    public void start()
    {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() { shutdown(); }
        });
        
        log.info(String.format("Starting new server of type \"%s\"...", getName()));

        try
        {
            // for each connector, build a bootstrap, start and save the ChannelFuture
            for (final ServerConnector connector : connectors)
            {
                final ServerBootstrap bootstrap =
                        new ServerBootstrap()
                            .group(masterGroup, slaveGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(connector.getChannelInitializer())
                            .option(ChannelOption.SO_BACKLOG, 128)
                            .childOption(ChannelOption.SO_KEEPALIVE, true);
                channels.add(bind(connector, bootstrap).sync());
            }
        }
        catch (final InterruptedException e) { }
        
        log.info("Startup complete.");
    }

    private ChannelFuture bind(ServerConnector serverConnector, AbstractBootstrap bootstrap) {
        String host = serverConnector.getHost();
        int port = serverConnector.getPort();
        if(host == null || "0.0.0.0".equals(host) || "*".equals(host)) {
            log.info(String.format("Binding to port %d on all server's hostnames and IP addresses", port));
            return bootstrap.bind(port);
        }
        else {
            log.info(String.format("Binding to port %d on host %s", port, host));
            return bootstrap.bind(host, port);
        }
    }

    public void shutdown()
    {
        if (! shutdownCalled)
        {
            log.info(String.format("Shutting down server of type \"%s\"...", getName()));
        }
        
        slaveGroup.shutdownGracefully();
        masterGroup.shutdownGracefully();

        for (final ChannelFuture channel : channels)
        {
            try
            {
                channel.channel().closeFuture().sync();
            }
            catch (InterruptedException e) { }
        }
        
        if (! shutdownCalled)
        {
            shutdownCalled = true;
            log.info("Shutdown complete.");
        }
    }
}
