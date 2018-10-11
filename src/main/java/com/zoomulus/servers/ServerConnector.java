package com.zoomulus.servers;

import io.netty.channel.ChannelInitializer;

public interface ServerConnector
{
    int getPort();
    String getHost();
    ChannelInitializer<?> getChannelInitializer();
}
