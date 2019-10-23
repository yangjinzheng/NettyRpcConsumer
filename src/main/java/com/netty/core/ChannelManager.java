package com.netty.core;

import io.netty.channel.ChannelFuture;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ChannelManager {

    public static CopyOnWriteArrayList<ChannelFuture> channelFutures = new CopyOnWriteArrayList<ChannelFuture>();
    public static CopyOnWriteArrayList<String> realServerPath = new CopyOnWriteArrayList<String>();
    public static AtomicInteger position = new AtomicInteger(0);
    public static void removeChannel(ChannelFuture channelFuture){
        channelFutures.remove(channelFuture);
    }

    public static void addChannel(ChannelFuture channelFuture){
        channelFutures.add(channelFuture);
    }

    public static void clearChannel(){
        channelFutures.clear();
    }

    public static ChannelFuture getChannel(AtomicInteger i) {
        int size = channelFutures.size();
        ChannelFuture channelFuture = null;
        if (i.get() > size) {
            channelFuture = channelFutures.get(0);
            ChannelManager.position = new AtomicInteger(1);
        } else {
            channelFuture = channelFutures.get(i.getAndIncrement());
        }
        if(!channelFuture.channel().isActive()){
            channelFutures.remove(channelFuture);
            return getChannel(position);
        }
        return channelFuture;
    }

}
