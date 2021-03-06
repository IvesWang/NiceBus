package com.ives.lib_nicebus.annotation;

import com.ives.lib_nicebus.ThreadMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author wangziguang
 * @date 2021/5/28
 * @description
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NiceEvent {
    ThreadMode threadMode();// = ThreadMode.POST;
    String[] events();
}
