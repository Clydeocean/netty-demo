package cn.jsbintask.hellonetty.handle;

import cn.jsbintask.hellonetty.config.GlobalConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.util.Date;

/**
 * @author jsbintask@foxmail.com
 * @date 2018/12/20 21:01
 */
public class CustomWebSocketHandler extends SimpleChannelInboundHandler<Object> {
    private WebSocketServerHandshaker webSocketServerHandshaker;
    public static final String WEB_SOCKET_URL = "ws://localhost:8888/websocket";

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("CustomWebSocketHandler.channelActive");
        GlobalConfig.CHANNEL_GROUP.add(ctx.channel());
        System.out.println("added a channel, name: " + ctx.name());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("CustomWebSocketHandler.channelInactive");
        GlobalConfig.CHANNEL_GROUP.remove(ctx.channel());
        System.out.println("removed a channel, name: " + ctx.name());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
        System.out.println(new Date() + "CustomWebSocketHandler.channelReadComplete");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("CustomWebSocketHandler.exceptionCaught");
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("CustomWebSocketHandler.channelRead0");
        System.out.println("received msg.");
        System.out.println(msg);

        /* handle http handshake request */
        if (msg instanceof FullHttpRequest) {
            handleHttpShakeRequest(ctx, (FullHttpRequest) msg);

            /* websocket request */
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketRequest(ctx, (WebSocketFrame) msg);
        }
    }

    private void handleWebSocketRequest(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) {
        if (webSocketFrame instanceof CloseWebSocketFrame) {
            webSocketServerHandshaker.close(ctx.channel(), ((CloseWebSocketFrame) webSocketFrame).retain());
            return;
        }

        if (webSocketFrame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(webSocketFrame.content().retain()));
            return;
        }

        if (!(webSocketFrame instanceof TextWebSocketFrame)) {
            System.out.println("unsupport binary msg.");
            throw new RuntimeException("unsupport msg.");
        }

        System.out.println("reveviced client msg: " + ((TextWebSocketFrame) webSocketFrame).text());
        TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(ctx.channel().id() + ", already received your msg: " + ((TextWebSocketFrame) webSocketFrame).text());

        GlobalConfig.CHANNEL_GROUP.writeAndFlush(textWebSocketFrame);
    }

    private void handleHttpShakeRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.decoderResult().isSuccess() || !"websocket".equals(request.headers().get("Upgrade"))) {
            /* send error status */
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
        } else {
            WebSocketServerHandshakerFactory webSocketServerHandshakerFactory = new WebSocketServerHandshakerFactory(WEB_SOCKET_URL, null, true);
            webSocketServerHandshaker = webSocketServerHandshakerFactory.newHandshaker(request);
            if (webSocketServerHandshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            } else {
                webSocketServerHandshaker.handshake(ctx.channel(), request);
            }
        }
    }

    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request, DefaultFullHttpResponse defaultFullHttpResponse) {
        if (defaultFullHttpResponse.status().code() != 200) {
            ByteBuf byteBuf = Unpooled.copiedBuffer(defaultFullHttpResponse.status().toString(), CharsetUtil.UTF_8);
            defaultFullHttpResponse.content().writeBytes(byteBuf);
            byteBuf.release();

            ChannelFuture channelFuture = ctx.channel().writeAndFlush(request);
            channelFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
