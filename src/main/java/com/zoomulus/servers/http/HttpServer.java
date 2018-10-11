package com.zoomulus.servers.http;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.inject.Injector;
import com.zoomulus.servers.BasicNettyServer;
import com.zoomulus.servers.ServerConnector;
import com.zoomulus.servers.ServerPort;

public class HttpServer extends BasicNettyServer
{
    public static final String LISTEN_HOST_NAME = "listenHost";
    public static final String LISTEN_PORT_NAME = "listenPort";
    
    @Inject
    public HttpServer(final Injector injector, @Named(LISTEN_HOST_NAME) String host, @Named(LISTEN_PORT_NAME) int port)
    {
        super(HttpServerConnector.builder()
                .host(host)
                .port(port)
                .injector(injector)
                .build());
    }

    public HttpServer()
    {
        super(HttpServerConnector.builder()
                .port(ServerPort.ZoomulusPort(ServerPort.PortNumber.HTTP))
                .build());
    }
}
