package com.netty.core;

import com.netty.client.TcpClient;
import com.netty.constant.Contants;
import com.netty.zk.ZookeeperFactory;
import io.netty.channel.ChannelFuture;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.HashSet;
import java.util.List;

public class ServerWatcher implements CuratorWatcher {
    public void process(WatchedEvent event) throws Exception {
        CuratorFramework client = ZookeeperFactory.create();
        String path = event.getPath();
        client.getChildren().usingWatcher(this).forPath(path);
        List<String> serverPaths = client.getChildren().forPath(path);
        ChannelManager.realServerPath.clear();
        for(String servePath : serverPaths){
            //这里可能有重复的比如服务器down掉又起来
            String[] str = servePath.split("#");
            int weight = Integer.parseInt(str[2]);
            if(weight > 0){
                for(int w=0; w<=weight;w++){
                    ChannelManager.realServerPath.add(str[0]+"#"+str[1]);
                }
            }


        }
        ChannelManager.clearChannel();
        for(String realServer : ChannelManager.realServerPath){
            String[] str = realServer.split("#");
            int weight = Integer.parseInt(str[2]);
            if(weight > 0) {
                //服务端注册到哪个端口，客户端要知道并连接
                for(int w=0;w<=weight;w++) {
                    ChannelFuture f = TcpClient.b.connect(str[0], Integer.valueOf(str[1]));
                    ChannelManager.addChannel(f);
                }
            }
        }

    }
}
