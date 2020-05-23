package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.vo.SkuLockVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WareListener {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private WareSkuDao skuDao;
    private static final String KEY_PREFIX="stock:lock";
    //锁库存
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "WMS-UNLOCK-QUEUE",durable = "true"),
            exchange = @Exchange(value = "GMALL-ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"stock.unlock"}
    ))
    public void unlockListener(String orderToken){
        String lockJson = redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        if (StringUtils.isEmpty(lockJson)){
            return;//防止重复解锁
        }
        List<SkuLockVO> skuLockVOS = JSON.parseArray(lockJson, SkuLockVO.class);
        skuLockVOS.forEach(skuLockVO -> {
            skuDao.unLockStore(skuLockVO.getWareSkuId(),skuLockVO.getCount());
        });
        //删除redis中锁定的库存
        this.redisTemplate.delete(KEY_PREFIX+orderToken);
    }
    //减库存
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "WMS-MINUS-QUEUE",durable = "true"),
            exchange = @Exchange(value = "GMALL-ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"stock.minus"}
    ))
    public void minusStoreListener(String orderToken){
        String lockJson = redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        List<SkuLockVO> skuLockVOS = JSON.parseArray(lockJson, SkuLockVO.class);
        skuLockVOS.forEach(skuLockVO -> {
            skuDao.minusStore(skuLockVO.getWareSkuId(),skuLockVO.getCount());
        });

    }
}
