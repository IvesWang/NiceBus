package com.ives.lib_nicebus;

import android.util.Log;

import androidx.collection.ArrayMap;

import com.ives.lib_nicebus.annotation.NiceEvent;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import kotlin.jvm.internal.Intrinsics;

/**
 * @author wangziguang
 * @date 2021/5/28
 * @description
 * todo 编译期需要处理的事：1 kotlin的非空参数排查 2 代码注入，无需手动创建销毁订阅者
 */
public class NiceBus {

    private static NiceBus instance;

    /**
     * 事件-声明该事件的method
     */
    private final ArrayMap<String, ArrayList<Method>> subscriberMethodContainer = new ArrayMap<>();
    /**
     * 添加了事件的类与其对象的容器
     */
    private final ArrayMap<Class, WeakReference<Object>> subscriberObjectContainer = new ArrayMap<>();

    private NiceBus(){}

    public static NiceBus getDefault(){
        if (instance == null){
            synchronized (NiceBus.class){
                if (instance == null){
                    instance = new NiceBus();
                }
            }
        }
        return instance;
    }

    public void register(Object obj){
        if (obj == null)return;

        Method[] declaredMethods = obj.getClass().getDeclaredMethods();//getAllDeclaredMethods(null, clazz);

//        ArrayList<Method> subscribeMethods = subscriberMethodContainer.get(obj.getClass());
//        if (subscribeMethods == null){
//            synchronized (subscriberMethodContainer) {
//                subscribeMethods = new ArrayList<>();
//                subscriberMethodContainer.put(obj.getClass(), subscribeMethods);
//            }
//        } else {
//            // 为提高性能，暂只支持一次录入
//            return;
//        }

        // 添加订阅
        for (Method newMethod : declaredMethods) {
            NiceEvent eventAnnotation = newMethod.getAnnotation(NiceEvent.class);
            if(eventAnnotation != null){
                String[] events = eventAnnotation.events();
                if (events.length == 0)continue;

                for (String event : events) {
                    synchronized (subscriberMethodContainer) {
                        // initialize list
                        ArrayList<Method> subscribeMethods = subscriberMethodContainer.get(event);// 允许""字符串对于使用端可能会有隐患
                        if (subscribeMethods == null) {
                            subscribeMethods = new ArrayList<>();
                            subscriberMethodContainer.put(event, subscribeMethods);
                        }
                        // cache method
                        if (!subscribeMethods.contains(newMethod)) {
                            checkAnnotationGrammar(eventAnnotation, newMethod);
                            subscribeMethods.add(newMethod);
                        }
                    }
                }

                // cache object
                Class clazz = obj.getClass();
                WeakReference<Object> subscribeObject = subscriberObjectContainer.get(clazz);
                if (subscribeObject == null || subscribeObject.get() == null){
                    synchronized (subscriberObjectContainer){
                        subscribeObject = new WeakReference<>(obj);
                        subscriberObjectContainer.put(clazz, subscribeObject);
                    }
                }
            }
        }

    }

    private void checkAnnotationGrammar(NiceEvent annotation, Method method){
        Class[] types = method.getParameterTypes();
        // 多于1个事件，第一个参数必须是String类型
        if (annotation.events().length > 1){
            if(types.length == 0 || (!types[0].getCanonicalName().equals("java.lang.String"))){
                throw new IllegalArgumentException("The type of first parameter in function " + method.toString() + " should be java.lang.String while you declare more than one event with @NiceEvent." +
                        "The String parameter will receive the event name.");
            }
            // 参数长度：单个事件最多5个参数，多个事件包括第一个String参数最多6个
            if (types.length > 6){
                throw new IllegalArgumentException("Function " + method.toString() + " should not more than 6 parameters as a limit with NiceBus.");
            }
        } else if (types.length > 5){
            throw new IllegalArgumentException("As a limit, function " + method.toString() + " should not more than 5 parameters while you declare only one event with @NiceEvent.");
        }

        //todo kotlin的业务参数必须可空
        // kotlin实现可空用到了编译期注解，需要在编译期处理
    }

    /**
     * 递归获取所有方法（含父类）
     */
    @Deprecated
    private Method[] getAllDeclaredMethods(Method[] allDeclaredMethods, Class clazz){
        if (clazz == null){
            return allDeclaredMethods;
        }
        Method[] declaredMethods = clazz.getDeclaredMethods();

        int inputDeclaredLength = allDeclaredMethods==null?0:allDeclaredMethods.length;
        Method[] newAll = new Method[inputDeclaredLength + declaredMethods.length];
        for (int i = 0; i < newAll.length; i++) {
            if (i < inputDeclaredLength){
                newAll[i] = allDeclaredMethods[i];
            } else {
                newAll[i] = declaredMethods[i - inputDeclaredLength];
                newAll[i].setAccessible(true);
            }
        }

        newAll = getAllDeclaredMethods(newAll, clazz.getSuperclass());

        return newAll;
    }

    public void unregister(Object obj){
        synchronized (subscriberMethodContainer) {
            Set<Map.Entry<String, ArrayList<Method>>> entrySet = subscriberMethodContainer.entrySet();

            for (Map.Entry<String, ArrayList<Method>> entry : entrySet) {
                Iterator<Method> it = entry.getValue().iterator();
                while (it.hasNext()){
                    if(obj.getClass().getCanonicalName().equals(it.next().getDeclaringClass())){
                        it.remove();
                    }
                }
                // clear if has no any subscriber
                if (entry.getValue().isEmpty()){
                    subscriberMethodContainer.remove(entry.getKey());
                }
            }
        }
        synchronized (subscriberObjectContainer) {
            subscriberObjectContainer.remove(obj.getClass());
        }
    }

    /**
     * 1. 声明超过一个事件，第一个参数必须提供且为String类型
     * 2. 只声明一个事件，对第一个参数没要求，不会传事件名称到方法
     * 3. 除了表示事件的第一个参数外，最多只接收5个业务参数
     * @param event
     * @param postParams
     */
    public void post(String event, Object... postParams){
        ArrayList<Method> subscriber = subscriberMethodContainer.get(event);
        if (subscriber == null){
            return;
        }
        synchronized (subscriber){
            Iterator<Method> it = subscriber.iterator();
            Method method;
            while (it.hasNext()){
                method = it.next();
                WeakReference weakRef = subscriberObjectContainer.get(method.getDeclaringClass());
                if (weakRef == null || weakRef.get() == null){
                    Log.w("NiceBus", "subscriber has been removed!!!");
                    continue;
                }

                NiceEvent eventAnnotation = method.getAnnotation(NiceEvent.class);
                ThreadMode threadMode = eventAnnotation.threadMode();
                // todo 根据不同的线程来执行
                // todo 增加有参必须传参注解，只接受参数事件
                // todo 跨进程
                // todo 依赖注入，减少手动编写生命周期依赖代码
                try {
                    Class[] parameterTypes = method.getParameterTypes();
                    method.setAccessible(true);
                    switch (parameterTypes.length){
                        case 0:
                            method.invoke(weakRef.get());
                            break;
                        case 1:
                            if (eventAnnotation.events().length == 1){
                                method.invoke(weakRef.get(), getParameterOrDefault(parameterTypes[0], postParams, 0));
                            } else {
                                method.invoke(weakRef.get(), event);
                            }
                            break;
                        case 2:
                            if (eventAnnotation.events().length == 1){// 单个事件，只传业务参数
                                method.invoke(weakRef.get(), getParameterOrDefault(parameterTypes[0], postParams, 0), getParameterOrDefault(parameterTypes[1], postParams, 1));
                            } else {// 多个事件，传入事件名、参数
                                method.invoke(weakRef.get(), event, getParameterOrDefault(parameterTypes[1], postParams, 0));
                            }
                            break;
                        case 3:
                            if (eventAnnotation.events().length == 1){
                                method.invoke(weakRef.get(), getParameterOrDefault(parameterTypes[0], postParams, 0), getParameterOrDefault(parameterTypes[1], postParams, 1), getParameterOrDefault(parameterTypes[2], postParams, 2));
                            } else {
                                method.invoke(weakRef.get(), event, getParameterOrDefault(parameterTypes[1], postParams, 0), getParameterOrDefault(parameterTypes[2], postParams, 1));
                            }
                            break;
                        case 4:
                            if (eventAnnotation.events().length == 1){
                                method.invoke(weakRef.get(), getParameterOrDefault(parameterTypes[0], postParams, 0), getParameterOrDefault(parameterTypes[1], postParams, 1), getParameterOrDefault(parameterTypes[2], postParams, 2), getParameterOrDefault(parameterTypes[3], postParams, 3));
                            } else {
                                method.invoke(weakRef.get(), event, getParameterOrDefault(parameterTypes[1], postParams, 0), getParameterOrDefault(parameterTypes[2], postParams, 1), getParameterOrDefault(parameterTypes[3], postParams, 2));
                            }
                            break;
                        case 5:
                            if (eventAnnotation.events().length == 1){
                                method.invoke(weakRef.get(), getParameterOrDefault(parameterTypes[0], postParams, 0), getParameterOrDefault(parameterTypes[1], postParams, 1), getParameterOrDefault(parameterTypes[2], postParams, 2), getParameterOrDefault(parameterTypes[3], postParams, 3), getParameterOrDefault(parameterTypes[4], postParams, 4));
                            } else {
                                method.invoke(weakRef.get(), event, getParameterOrDefault(parameterTypes[1], postParams, 0), getParameterOrDefault(parameterTypes[2], postParams, 1), getParameterOrDefault(parameterTypes[3], postParams, 2), getParameterOrDefault(parameterTypes[4], postParams, 3));
                            }
                            break;
                        case 6:
                            method.invoke(weakRef.get(), event, getParameterOrDefault(parameterTypes[1], postParams, 0), getParameterOrDefault(parameterTypes[2], postParams, 1), getParameterOrDefault(parameterTypes[3], postParams, 2), getParameterOrDefault(parameterTypes[4], postParams, 3), getParameterOrDefault(parameterTypes[5], postParams, 4));
                            break;
                            default:
                                new IllegalArgumentException("subscribe function should provide parameter count 1 - 6.").printStackTrace();
                                break;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Object getParameterOrDefault(Class type, Object[] postParams, int paramIndex){
        Log.e("NiceBus", "type:"+type.getCanonicalName());
        if (postParams != null && postParams.length >= paramIndex + 1){
            return postParams[paramIndex];
        }
        switch (type.getCanonicalName()){
            case "boolean":
            case "java.lang.Boolean":
                return false;
            case "byte":
            case "java.lang.Byte":
                return (byte) 0;
            case "short":
            case "java.lang.Short":
                return (short) 0;
            case "int":
            case "java.lang.Integer":
                return 0;
            case "float":
            case "java.lang.Float":
                return 0f;
            case "long":
            case "java.lang.Long":
                return 0L;
            case "double":
            case "java.lang.Double":
                return 0d;
            case "char":
            case "java.lang.Character":
                return '0';
        }
        return null;
    }
}
