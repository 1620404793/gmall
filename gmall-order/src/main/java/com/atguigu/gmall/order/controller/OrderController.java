package com.atguigu.gmall.order.controller;

import com.alipay.api.AlipayApiException;
import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.order.pay.AlipayTemplate;
import com.atguigu.gmall.order.pay.PayAsyncVo;
import com.atguigu.gmall.order.pay.PayVo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVO;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.wms.vo.SkuLockVO;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private AlipayTemplate alipayTemplate;
    @Autowired
    private AmqpTemplate amqpTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @GetMapping("/confirm")
    public Resp<OrderConfirmVO> confirm(){
        //不需要参数，可以从选中的购物车里直接选
        OrderConfirmVO orderConfirmVO=this.orderService.confirm();
        return Resp.ok(orderConfirmVO);
    }
    @PostMapping("submit")
    public Resp<Object> submit(@RequestBody OrderSubmitVo submitVo){
        OrderEntity orderEntity = this.orderService.submit(submitVo);

        try {
            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(orderEntity.getOrderSn());
            payVo.setTotal_amount(orderEntity.getTotalAmount()!=null?orderEntity.getTotalAmount().toString():"100");
            payVo.setSubject("商城");
            payVo.setBody("支付平台");
            String payForm = this.alipayTemplate.pay(payVo);
            System.out.println(payForm);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return Resp.ok(null);
    }
    @PostMapping("pay/success")
    public Resp<Object> paySuccess(PayAsyncVo payAsyncVo){

        this.amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE","order.pay",payAsyncVo.getOut_trade_no());
        return Resp.ok(null);
    }

    @PostMapping("seckill/{skuId}")
    public Resp<Object> seckill(@PathVariable("skuId")Long skuId){

        RSemaphore semaphore = this.redissonClient.getSemaphore("semaphore:lock:" + skuId);
        semaphore.trySetPermits(500);
        //只有获得资源才能，执行下面的操作
        if (semaphore.tryAcquire()){
            //获取redis中的库存信息
            String countString = this.redisTemplate.opsForValue().get("order:seckill:" + skuId);

            //没有，秒杀结束
            if (StringUtils.isEmpty(countString)||Integer.parseInt(countString)==0){
                return Resp.ok("秒杀结束");
            }
            //有，减库存
            int count = Integer.parseInt(countString);
            this.redisTemplate.opsForValue().set("order:seckill:",String.valueOf(--count));
            //发送消息给消息队列，将来真正的减库存
            SkuLockVO skuLockVO = new SkuLockVO();
            skuLockVO.setCount(1);
            String orderToken = IdWorker.getIdStr();
            skuLockVO.setSkuId(skuId);
            skuLockVO.setOrderToken(orderToken);

            this.amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE","order.seckill",skuLockVO);

            /*在oms中订单保存表后，添加此操作
            RCountDownLatch countDownLatch = this.redissonClient.getCountDownLatch("count:down:" + orderToken);
            countDownLatch.trySetCount(1);
            创建完订单，就立马执行该方法
            countDownLatch.countDown();*/

            //释放锁
            semaphore.release();
            //响应成功
            return Resp.ok("恭喜你秒杀成功！！！");
        }

      return Resp.ok("很遗憾，没有枪到");
    }
    //查询秒杀订单
    @GetMapping("seckill/{orderToken}")
    public Resp<Object> querySeckill(@PathVariable("orderToken") String orderToken) throws InterruptedException {
        RCountDownLatch countDownLatch = this.redissonClient.getCountDownLatch("count:down:" + orderToken);
        countDownLatch.await();

        //查询订单并响应
        //发送远程请求，查询订单

        return Resp.ok("结果");
    }
}
