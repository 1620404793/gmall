package com.atguigu.gmall.index.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {
    //定义等价的关系
    @AliasFor("prefix")
    String value() default "";

    /**
     * 定义key的前缀
     *
     * @return
     */
    @AliasFor("value")
    String prefix() default "";

    /*
     * 缓存的过期时间以分为单位
     * */
    int timeout() default 5;

    /**
     * 防止缓存雪崩指定的时间值范围
     *
     * @return
     */
    int random() default 5;
}
