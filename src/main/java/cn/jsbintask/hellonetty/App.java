package cn.jsbintask.hellonetty;

import cn.jsbintask.hellonetty.config.CustomWebSocketInit;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author jsbintask@foxmail.com
 * @date 2018/12/23 15:40
 */
public class App {
    public static void main(String[] args) {
        EventLoopGroup mainLoopGroup = new NioEventLoopGroup();
        EventLoopGroup workLoopGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(mainLoopGroup, workLoopGroup);
            serverBootstrap.childHandler(new CustomWebSocketInit());
            serverBootstrap.channel(NioServerSocketChannel.class);

            System.out.println("server is waiting for clients connect...");
            Channel channel = serverBootstrap.bind(8888).sync().channel();
            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mainLoopGroup.shutdownGracefully();
            workLoopGroup.shutdownGracefully();
        }
    }
}
