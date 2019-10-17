package com.netty.core;

import com.netty.client.TcpClient;
import com.netty.constant.Contants;
import com.netty.zk.ZookeeperFactory;
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
        client.getChildren().usingWatcher(this);
        List<String> serverPaths = client.getChildren().forPath(path);
        TcpClient.realServerPath = new HashSet<String>();
        for(String servePath : serverPaths){
            TcpClient.realServerPath.add(servePath.split("#")[0]);
        }
    }
}
