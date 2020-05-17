package com.atguigu.gmall.index.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.annotation.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVO;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class IndexServiceImpl implements IndexService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:cates:";

    @Override
    public List<CategoryEntity> queryV1Categories() {
        Resp<List<CategoryEntity>> listResp = this.gmallPmsClient.queryCategoryByPidOrLevel(1, null);
        return listResp.getData();
    }

    @GmallCache(prefix = "index:cates:", timeout = 7200, random = 100)
    public List<CategoryVO> querySubCategories(Long pid) {


        List<CategoryVO> categoryVOS = gmallPmsClient.querySubCategories(pid).getData();

        return categoryVOS;
    }

    public List<CategoryVO> querySubCategoriesBEIFEN01(Long pid) {
        //1.判断缓存中有没有
        String cateJson = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        //2.如果有，直接从缓存中返回
        if (!StringUtils.isEmpty(cateJson)) {
            return JSON.parseArray(cateJson, CategoryVO.class);
        }

        RLock lock = redissonClient.getLock("lock" + pid);//管理好自己的锁
        lock.lock();

        //使用双重缓存  （1000个请求，1个已经请求放入缓存了，防止其他999的请求继续查询数据库）
        String cateJson2 = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        //如果有，直接从缓存中返回
        if (!StringUtils.isEmpty(cateJson2)) {
            //释放锁
            lock.unlock();
            return JSON.parseArray(cateJson2, CategoryVO.class);
        }
        //3. 没有，查询mysql后放入缓存
        List<CategoryVO> categoryVOS = gmallPmsClient.querySubCategories(pid).getData();
        //解绝雪崩效应，添加随机的过期时间
        this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryVOS), 7 + new Random().nextInt(5), TimeUnit.DAYS);
        //释放锁
        lock.unlock();
        return categoryVOS;
    }

    /*redisson实现分布式锁*/

    /**
     * 锁
     * 1.互斥性
     * 2.获取锁并设置过期时间防止死锁，要具备原子性.   释放锁也要具备原子性才可以
     * 3.解铃还须系铃人
     */
    public void testLock() {
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock();//实现了自动续期
        String numString = this.redisTemplate.opsForValue().get("num");
        if (StringUtils.isEmpty(numString)) {
            return;
        }
        int num = Integer.parseInt(numString);
        redisTemplate.opsForValue().set("num", String.valueOf(++num));

        lock.unlock();
    }

    @Override
    public String testRead() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(10l, TimeUnit.SECONDS);
        String test = this.redisTemplate.opsForValue().get("test");

        //rwLock.readLock().unlock();
        return test;
    }

    @Override
    public String testWrite() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(10l, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set("test", UUID.randomUUID().toString());
        // rwLock.writeLock().unlock();
        return "写入数据";
    }

    @Override
    public String testLatch() throws InterruptedException {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");
        latch.trySetCount(5);

        latch.await();
        return "主业务开始执行>......";
    }

    @Override
    public String testCount() {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");
        latch.countDown();
        return "分支任务执行了一次";
    }


    /*本地锁*/
    public synchronized void testLock1() {
        String numString = this.redisTemplate.opsForValue().get("num");
        if (StringUtils.isEmpty(numString)) {
            return;
        }
        int num = Integer.parseInt(numString);
        redisTemplate.opsForValue().set("num", String.valueOf(++num));
    }
    /*分布式锁----redissetnx命令实现*/

    /**
     * 锁
     * 1.互斥性
     * 2.获取锁并设置过期时间防止死锁，要具备原子性.   释放锁也要具备原子性才可以
     * 3.解铃还须系铃人
     */
    public void testLock2() {
        //防止别人删除自己的锁，给自己生成一个唯一的标志
        String uuid = UUID.randomUUID().toString();
        //执行redis的setnx命令     防止死锁发生---------》设置过期时间且要保证保存和时间的原子性
        Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid, 5, TimeUnit.DAYS);
        if (lock) {
            //判断是否拿到锁
            String numString = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isEmpty(numString)) {
                return;
            }
            int num = Integer.parseInt(numString);
            redisTemplate.opsForValue().set("num", String.valueOf(++num));

            //释放资源，其他请求才能执行  （保证释放锁的原子性使用（LUA））
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script), Arrays.asList("lock"), uuid);
            /*if (StringUtils.equals(this.redisTemplate.opsForValue().get("lock"),uuid)){
                this.redisTemplate.delete("lock");//因为设置了过期时间，存在释放其他锁的风险
            }*/
        } else {
            //其他请求重试获取锁
            testLock();
        }
    }
}
