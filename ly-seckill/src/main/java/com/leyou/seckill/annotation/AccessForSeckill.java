package com.leyou.seckill.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AccessForSeckill {

    /**
     * 限制请求的总时长
     * @return
     */
    int seconds() default 60;

    /**
     * 总时长内最大请求次数
     * @return
     */
    int requestCounts() default 20;

    /**
     * 用户是否需要登录权限
     * @return
     */
    boolean login();
}
