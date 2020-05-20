package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.vo.SkuLockVO;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import springfox.documentation.spring.web.json.Json;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private WareSkuDao wareSkuDao;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private AmqpTemplate amqpTemplate;

    private static final String KEY_PREFIX="stock:lock";


    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageVo(page);
    }

    @Override
    @Transactional
    public String checkAndLockStore(List<SkuLockVO> skuLockVOS) {
        if (CollectionUtils.isEmpty(skuLockVOS)){
            return "没有选中的商品";
        }
        //查和锁的时候一定要具备原子性 加分布式锁
        //校验并锁定库存，为了保证原子性单独写了一个方法
        skuLockVOS.forEach(skuLockVO -> {

            lockStore(skuLockVO);
        });
        List<SkuLockVO> unLockSku = skuLockVOS.stream().filter(skuLockVO -> skuLockVO.getLock() == false).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(unLockSku)){
            //解锁已锁定的库存
            List<SkuLockVO> lockVOS = skuLockVOS.stream().filter(SkuLockVO::getLock).collect(Collectors.toList());
            lockVOS.forEach(skuLockVO -> {
                this.wareSkuDao.unLockStore(skuLockVO.getWareSkuId(),skuLockVO.getCount());
            });
            //提示锁定失败的商品
            List<Long> skuIds = unLockSku.stream().map(SkuLockVO::getSkuId).collect(Collectors.toList());
            return "下单失败，商品"+skuIds.toString()+"库存不足";
        }
        String orderToken = skuLockVOS.get(0).getOrderToken();
        this.redisTemplate.opsForValue().set(KEY_PREFIX+orderToken, JSON.toJSONString(skuLockVOS));

        //锁定成功，发送延时消息，定时解锁
        this.amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE","stock.ttl",orderToken);

        return null;
    }
    private void lockStore(SkuLockVO skuLockVO){
        RLock lock = this.redissonClient.getLock("stock:" + skuLockVO.getSkuId());//也给锁起了名字
        lock.lock();
        //查询剩余库存
        List<WareSkuEntity> wareSkuEntities=this.wareSkuDao.checkStore(skuLockVO.getSkuId(),skuLockVO.getCount());
        if (!CollectionUtils.isEmpty(wareSkuEntities)){
            //不为空，锁定库存信息
            WareSkuEntity wareSkuEntity = wareSkuEntities.get(0);
            this.wareSkuDao.lockStore(wareSkuEntity.getId(),skuLockVO.getCount());
            skuLockVO.setWareSkuId(wareSkuEntity.getId());
            skuLockVO.setLock(true);
        }else {
            skuLockVO.setLock(false);//没有锁住
        }


        //释放锁
        lock.unlock();
    }

}