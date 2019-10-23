package com.netty.client;

import com.alibaba.fastjson.JSON;
import com.netty.constant.Contants;
import com.netty.core.ChannelManager;
import com.netty.core.ServerWatcher;
import com.netty.handler.SimpleClientHandler;
import com.netty.param.Response;
import com.netty.zk.ZookeeperFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.AttributeKey;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.Watcher;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TcpClient {

    public static final Bootstrap b = new Bootstrap();
    static ChannelFuture f = null;
    public static Set<String> realServerPath = new HashSet<String>();
    static {
        String host = "localhost";
        int port = 8080;
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        b.group(workerGroup); // (2)
        b.channel(NioSocketChannel.class); // (3)
        b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Delimiters.lineDelimiter()[0]));
                ch.pipeline().addLast(new StringDecoder());
                ch.pipeline().addLast(new SimpleClientHandler());
                ch.pipeline().addLast(new StringEncoder());

            }
        });
        CuratorFramework client = ZookeeperFactory.create();
        try {
            //因为没在catch里打断点不知道是因为path末尾加了/报错
            List<String> serverPaths = client.getChildren().forPath(Contants.SERVER_PATH);
            //加上zk监听服务器变化
            CuratorWatcher watcher = new ServerWatcher();
            client.getChildren().usingWatcher(watcher).forPath(Contants.SERVER_PATH);
            for(String servePath : serverPaths){
                String[] str = servePath.split("#");
                int weight = Integer.parseInt(str[2]);
                if(weight > 0) {
                    for (int w = 0; w <= weight; w++) {
                        ChannelManager.realServerPath.add(str[0] + "#" + str[1]);
                        ChannelFuture f = TcpClient.b.connect(str[0], Integer.valueOf(str[1]));
                        ChannelManager.addChannel(f);
                    }
                }

            }
            if(ChannelManager.realServerPath.size() > 0){
                String[] hostAndPort = ChannelManager.realServerPath.toArray()[0].toString().split("#");
                host = hostAndPort[0];
                port = Integer.valueOf(hostAndPort[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        try {
//            f = b.connect(host, port).sync();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }
    static int i = 0;

    //发送数据长连接
    //一个链接考虑并发问题：1.request:唯一请求id，请求内容
    public static Response send(ClientRequest request){
        f = ChannelManager.getChannel(ChannelManager.position);
        f.channel().writeAndFlush(JSON.toJSONString(request));
        f.channel().writeAndFlush("\r\n");
        DefaultFuture df = new DefaultFuture(request);
        return df.get();
    }
    public static void main(String[] args) throws InterruptedException {
        String host = "localhost";
        int port = 8080;
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap(); // (1)
            b.group(workerGroup); // (2)
            b.channel(NioSocketChannel.class); // (3)
            b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Delimiters.lineDelimiter()[0]));
                    ch.pipeline().addLast(new StringDecoder());
                    ch.pipeline().addLast(new SimpleClientHandler());
                    ch.pipeline().addLast(new StringEncoder());

                }
            });

            // Start the com.netty.client.
            ChannelFuture f = b.connect(host, port).sync(); // (5)

            Object result = f.channel().attr(AttributeKey.valueOf("sssss")).get();
            System.out.println("获取到服务器返回的数据"+result);
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}
