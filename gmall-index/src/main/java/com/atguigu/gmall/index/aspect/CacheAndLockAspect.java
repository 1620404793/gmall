package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.annotation.GmallCache;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import springfox.documentation.spring.web.json.Json;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class CacheAndLockAspect {
    /**
     * 使用around需要注意一下几点：
     * 1.返回值类型：Object
     * 2.参数 ProceedingJoinPoint joinPoint
     * 3.抛出Throwable
     * 4.joinPoint.proceed()获取目标方法  ->执行目标方法
     */
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(com.atguigu.gmall.index.annotation.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = null;
        //获取目标方法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        //获取目标方法的返回值
        Class<?> methodReturnType = method.getReturnType();
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        String prefix = gmallCache.prefix();
        //获取目标方法的参数列表
        Object[] args = joinPoint.getArgs();
        //从缓存中查询
        //数组的toString方法是一个哈希字符串，想要获得里面的内容需要转换成list
        String key = prefix + Arrays.asList(args).toString();
        result = this.cacheHit(key, methodReturnType);
        if (result != null) {
            return result;
        }
        //没有命中，加分布式锁（防止击穿）
        RLock lock = this.redissonClient.getLock("lock" + Arrays.asList(args).toString());
        lock.lock();//不需要指定过期时间，可以自动续期
        //双缓存机制（防止数据进入缓存后，还访问数据库）再次查询缓存，如果缓存中没有执行目标方法
        result = this.cacheHit(key, methodReturnType);
        if (result != null) {
            lock.unlock();
            return result;
        }
        //缓存中还没有的话，就查询数据库 -->通过目标参数，执行目标方法
        result = joinPoint.proceed(joinPoint.getArgs());
        int random = gmallCache.random();
        int timeout = gmallCache.timeout();
        this.redisTemplate.opsForValue().set(key, JSON.toJSONString(result), (int) (timeout + (Math.random() * random)), TimeUnit.MINUTES);
        //加入缓存，释放分布式锁
        lock.unlock();
        return result;
    }

    public Object cacheHit(String key, Class<?> methodReturnType) {
        //从缓存中查询
        String json = this.redisTemplate.opsForValue().get(key);
        //命中，直接返回
        if (StringUtils.isNotEmpty(json)) {
            return JSON.parseObject(json, methodReturnType);
        }
        return null;
    }
}
