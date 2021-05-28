package com.ives.lib_nicebus;

import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;

import com.ives.lib_nicebus.annotation.NiceEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * @author wangziguang
 * @date 2021/5/28
 * @description
 */
public class NiceBus {

    private static NiceBus instance;

    private final ArrayMap<Class, ArrayList<Method>> subscriberContainer = new ArrayMap<>();

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

    public void register(Class clazz){
        if (clazz == null)return;

        Method[] declaredMethods = clazz.getDeclaredMethods();//getAllDeclaredMethods(null, clazz);

        ArrayList<Method> subscribeMethods = subscriberContainer.get(clazz);
        if (subscribeMethods == null){
            synchronized (subscriberContainer) {
                subscribeMethods = new ArrayList<>();
                subscriberContainer.put(clazz, subscribeMethods);
            }
        } else {
            // 为提高性能，暂只支持一次录入
            return;
        }

        for (Method newMethod : declaredMethods) {
            if(newMethod.getAnnotation(NiceEvent.class) != null && !subscribeMethods.contains(newMethod)){
                subscribeMethods.add(newMethod);
            }
        }

    }

    /**
     * 递归获取所有方法（含父类）
     */
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

    public void unregister(Class clazz){
        synchronized (subscriberContainer) {
            subscriberContainer.remove(clazz);
        }
    }

    public void post(Class clazz, Object msg){

    }
}
