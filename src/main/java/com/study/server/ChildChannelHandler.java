package com.study.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;


/**
 * Created by chauncy on 16/2/15.
 */
public class ChildChannelHandler extends ChannelInitializer<SocketChannel> {


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast("decoder", new HttpRequestDecoder());   //用于解析http报文的handler
        ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));   //用于将解析出来的数据封装成http对象，httprequest什么的
        ch.pipeline().addLast("encoder", new HttpResponseEncoder());   //用于将response编码成httpresponse报文发送
        ch.pipeline().addLast("handshake", new WebSocketServerProtocolHandler("", "", true));  //websocket的handler部分定义的，它会自己处理握手等操作
        ch.pipeline().addLast("handler", new MyWebSocketServerHandler());
    }
}
