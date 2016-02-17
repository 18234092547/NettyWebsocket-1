package com.study.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.util.Date;

/**
 * Created by chauncy on 16/2/15.
 */
public class MyWebSocketServerHandler extends ChannelInboundHandlerAdapter {

    private WebSocketServerHandshaker handshaker = null;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {

            handleHttpRequest(ctx, ((FullHttpRequest) msg));

        } else if (msg instanceof WebSocketFrame) {

            handlerWebSocketFrame(ctx, (WebSocketFrame) msg);

        }
    }


    private void handleHttpRequest(ChannelHandlerContext ctx,
                                   FullHttpRequest req) {

        if (!req.getDecoderResult().isSuccess()
                || (!"websocket".equals(req.headers().get("Upgrade")))) {

            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));

            return;
        }

        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://localhost:8888/websocket", null, false);

        handshaker = wsFactory.newHandshaker(req);

        if (handshaker == null) {
            WebSocketServerHandshakerFactory
                    .sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }


    private void handlerWebSocketFrame(ChannelHandlerContext ctx,
                                       WebSocketFrame frame) {

        // 判断是否关闭链路的指令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame
                    .retain());
        }

        // 判断是否ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(
                    new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        if (!(frame instanceof TextWebSocketFrame)) {


            throw new UnsupportedOperationException(String.format(
                    "%s frame types not supported", frame.getClass().getName()));
        }

        // 返回应答消息
        String request = ((TextWebSocketFrame) frame).text();

        System.out.println("recv：" + request);


        TextWebSocketFrame tws = new TextWebSocketFrame(new Date().toString()
                + ctx.channel().localAddress() + "：" + request);

        // 群发
        MyChannelGroup.group.writeAndFlush(tws);


    }

    private static void sendHttpResponse(ChannelHandlerContext ctx,
                                         FullHttpRequest req, DefaultFullHttpResponse res) {

        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(),
                    CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }

        // 如果是非Keep-Alive，关闭连接
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!isKeepAlive(req) || res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }


    private static boolean isKeepAlive(FullHttpRequest req) {

        return false;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        // 添加
        MyChannelGroup.group.add(ctx.channel());

        System.out.println("connect start");

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        // 移除
        MyChannelGroup.group.remove(ctx.channel());

        System.out.println("connect close");

    }

}
