package com.zoomulus.servers.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import java.util.Optional;

import lombok.Value;

import com.google.inject.Injector;
import com.zoomulus.servers.ServerConnector;
import com.zoomulus.servers.http.responder.DefaultHttpResponder;

@Value
public class HttpServerConnector implements ServerConnector
{
    String host;
    int port;
    final Optional<Injector> injector;
    
    public static final String CODEC_HANDLER_NAME            = "codec_handler";
    public static final String COMPRESSOR_HANDLER_NAME       = "compressor_handler";
    public static final String AGGREGATOR_HANDLER_NAME       = "aggregator_handler";
    public static final String HTTP_REQUEST_HANDLER_NAME     = "http_request_handler";

    public HttpServerConnector() {
        host = null;
        port = -1;
        injector = null;
    }

    private HttpServerConnector(String host, int port, Optional<Injector> injector) {
        this.host = host;
        this.port = port;
        this.injector = injector;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public ChannelInitializer<?> getChannelInitializer()
    {
        return new ChannelInitializer<SocketChannel>()
        {
            @Override
            public void initChannel(final SocketChannel ch) throws Exception
            {
                ch.pipeline().addLast(CODEC_HANDLER_NAME,           new HttpServerCodec());
                ch.pipeline().addLast(AGGREGATOR_HANDLER_NAME,      new HttpObjectAggregator(512*1024));
                ch.pipeline().addLast(HTTP_REQUEST_HANDLER_NAME,
                        injector.isPresent()
                                ? injector.get().getInstance(HttpHandler.class)
                                : new HttpHandler(new DefaultHttpResponder()));
                
                if (compress())
                {
                    ch.pipeline().addAfter(CODEC_HANDLER_NAME, COMPRESSOR_HANDLER_NAME, new HttpContentCompressor());
                }
            }
        };
    }

    protected boolean compress()
    {
        return false;
    }
    
    public static HttpServerConnectorBuilder builder()
    {
        return new HttpServerConnectorBuilder();
    }
    
    public static class HttpServerConnectorBuilder
    {
        String _host = null;
        int _port = 8080;
        Optional<Injector> _injector = Optional.empty();

        public HttpServerConnectorBuilder host(String host) {
            _host = host;
            return this;
        }

        public HttpServerConnectorBuilder port(int port)
        {
            _port = port;
            return this;
        }
        
        public HttpServerConnectorBuilder injector(final Injector injector)
        {
            _injector = Optional.of(injector);
            return this;
        }
        
        public HttpServerConnector build()
        {
            return new HttpServerConnector(_host, _port, _injector);
        }
    }
}
