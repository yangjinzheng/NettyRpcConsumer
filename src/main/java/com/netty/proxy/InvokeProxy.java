package com.netty.proxy;

import com.netty.annotion.RemoteInvoke;
import com.netty.client.ClientRequest;
import com.netty.client.TcpClient;
import com.netty.param.Response;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
public class InvokeProxy implements BeanPostProcessor {
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Field[] fields = bean.getClass().getDeclaredFields();
        for(Field field : fields){
            final Map<Method, Class> methodClassMap = new HashMap<Method, Class>();
            puMethodClass(methodClassMap,field);
            //带着注解的属性
            if(field.isAnnotationPresent(RemoteInvoke.class)){
                field.setAccessible(true);
                //动态代理类
                Enhancer enhancer = new Enhancer();
                enhancer.setInterfaces(new Class[]{field.getType()});//对那些接口进行代理
                enhancer.setCallback(new MethodInterceptor() {//执行方法前进行拦截
                    public Object intercept(Object instance, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
                    //利用netty客户端去调用服务器
                        ClientRequest clientRequest = new ClientRequest();
                        clientRequest.setCommand(methodClassMap.get(method).getName()+"."+method.getName());
                        clientRequest.setContent(args[0]);
                        Response response = TcpClient.send(clientRequest);

                        return response;
                    }
                });
                try {
                    field.set(bean,enhancer.create());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 对属性的所有方法和属性类型放到一个map中，对象属性的类型就是类
     * @param methodClassMap
     * @param field
     */
    private void puMethodClass(Map<Method, Class> methodClassMap, Field field) {
        Method[] methods = field.getType().getDeclaredMethods();
        for(Method m : methods){
            methodClassMap.put(m,field.getType());
        }
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return null;
    }
}
