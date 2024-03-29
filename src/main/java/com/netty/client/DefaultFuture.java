package com.netty.client;

import com.netty.param.Response;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultFuture {

    public static final ConcurrentHashMap<Long, DefaultFuture> allDefaultFuture = new ConcurrentHashMap<Long, DefaultFuture>();
    final Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
    private Response response;
    private long timeout = 2*60*1000L;
    private long startTime = System.currentTimeMillis();

    public DefaultFuture(ClientRequest request) {
        allDefaultFuture.put(request.getId(),this);
    }
//主线程获取数据首先要等待结果
    public Response get(){
        lock.lock();
        try{
            while (!done()){
                condition.await();
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
        return this.response;
    }

    public Response get(long time){
        lock.lock();
        try{
            while (!done()){
                condition.await(time, TimeUnit.SECONDS);
                if((System.currentTimeMillis() - startTime) > time){
                    System.out.println("请求超时");
                    break;
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
        return this.response;
    }

    public static void receive(Response response){
        DefaultFuture df = allDefaultFuture.get(response.getId());
        if(df != null){
            Lock lock = df.lock;
            lock.lock();
            try {
                df.setResponse(response);
                df.condition.signal();
                allDefaultFuture.remove(df);
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                lock.unlock();
            }
        }
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    private boolean done() {
        if(this.response != null) {
            return true;
        }
        return false;
    }

    /**
     * 清除超时结果
     */
    static class FutureThread extends Thread{
        @Override
        public void run() {
            Set<Long> keys = allDefaultFuture.keySet();
            for(Long id : keys){
                DefaultFuture df = allDefaultFuture.get(id);
                if(df == null){
                    allDefaultFuture.remove(df);
                }else {
                    //假如链路超时
                    if(df.getTimeout() < (System.currentTimeMillis() - df.getStartTime())){
                        Response response = new Response();
                        response.setId(id);
                        response.setStatus("33333");
                        response.setMsg("请求超时");
                        receive(response);
                    }
                }
            }
        }
    }

    static {
        FutureThread futureThread = new FutureThread();
        futureThread.setDaemon(true);
        futureThread.start();
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
}
