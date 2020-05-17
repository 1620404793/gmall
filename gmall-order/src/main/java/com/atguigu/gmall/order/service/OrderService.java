package com.atguigu.gmall.order.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.exception.OrderException;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.order.config.ThreadPoolConfig;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptors.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVO;
import com.atguigu.gmall.order.vo.OrderItemVO;
import com.atguigu.gmall.order.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SkuSaleVO;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class OrderService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private GmallOmsClient omsClient;
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private GmallCartClient cartClient;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallSmsClient smsClient;

    private static final String TOKEN_PREFIX="order:token:";

    @Autowired //异步编排工具
    private ThreadPoolExecutor threadPoolExecutor;
    public OrderConfirmVO confirm() {
        OrderConfirmVO orderConfirmVO=new OrderConfirmVO();
        UserInfo userInfo= LoginInterceptor.getUserInfo();
        Long userId = userInfo.getId();
        if (userId==null){
            return null;//按道理说不会为null
        }
      //  List<CompletableFuture> futures=new ArrayList<>();
        CompletableFuture<Void> addressCompletableFuture = CompletableFuture.runAsync(() -> {
            //获取用户的id获取地址列表
            Resp<List<MemberReceiveAddressEntity>> addressesResp = this.umsClient.queryAddressesByUserId(userId);
            List<MemberReceiveAddressEntity> addressEntityList = addressesResp.getData();
            orderConfirmVO.setAddresses(addressEntityList);
        }, threadPoolExecutor);
        //futures.add(addressCompletableFuture);
        //获取购物车中选中的信息  下面有的方法需要此返回值
        CompletableFuture<Void> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            List<Cart> cartList = cartClient.queryCheckedCartsByUserId(userId).getData();
            if (CollectionUtils.isEmpty(cartList)) {
                throw new OrderException("请勾选购物车商品！");
            }
            return cartList;
        }, threadPoolExecutor).thenAcceptAsync(cartList -> {
            List<OrderItemVO> orderItemVOS = cartList.stream().map(cart -> {
                Long skuId = cart.getSkuId();
                OrderItemVO orderItemVO = new OrderItemVO();
                CompletableFuture<Void> skuInfoCompletableFuture = CompletableFuture.runAsync(() -> {
                    SkuInfoEntity skuInfoEntity = this.pmsClient.querySkuById(skuId).getData();
                    if (skuInfoEntity != null) {
                        orderItemVO.setDefaultImage(skuInfoEntity.getSkuDefaultImg());
                        orderItemVO.setPrice(skuInfoEntity.getPrice());
                        orderItemVO.setCount(cart.getCount());
                        orderItemVO.setTitle(skuInfoEntity.getSkuTitle());
                        orderItemVO.setSkuId(skuId);
                        orderItemVO.setWeight(skuInfoEntity.getWeight());
                    }
                }, threadPoolExecutor);
               // futures.add(skuInfoCompletableFuture);
                CompletableFuture<Void> attrValueCompletableFuture = CompletableFuture.runAsync(() -> {
                    List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = pmsClient.querySkuSaleAttrValueBySkuId(skuId).getData();
                    orderItemVO.setSaleAttrValues(skuSaleAttrValueEntities);
                }, threadPoolExecutor);
              //  futures.add(attrValueCompletableFuture);

                CompletableFuture<Void> saleVoCompletableFuture = CompletableFuture.runAsync(() -> {
                    List<SkuSaleVO> saleVOS = smsClient.querySkuSalesBySkuId(skuId).getData();
                    orderItemVO.setSales(saleVOS);
                }, threadPoolExecutor);
               // futures.add(saleVoCompletableFuture);

                CompletableFuture<Void> wareSkuCompletableFuture = CompletableFuture.runAsync(() -> {
                    List<WareSkuEntity> wareSkuEntities = this.wmsClient.queryWareSkuBySkuId(skuId).getData();
                    if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                        boolean flag = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
                        orderItemVO.setStore(flag);
                    }
                }, threadPoolExecutor);
               // futures.add(wareSkuCompletableFuture);
                CompletableFuture.allOf(skuInfoCompletableFuture,attrValueCompletableFuture,saleVoCompletableFuture,wareSkuCompletableFuture).join();
                return orderItemVO;
            }).collect(Collectors.toList());
            orderConfirmVO.setOrderItems(orderItemVOS);
        }, threadPoolExecutor);


        //查询用户信息，获取积分
        CompletableFuture<Void> memberCompletableFuture = CompletableFuture.runAsync(() -> {
            MemberEntity memberEntity = this.umsClient.queryMemberById(userId).getData();
            orderConfirmVO.setBounds(memberEntity.getIntegration());
        }, threadPoolExecutor);
        //生成一个唯一标识，防止重复提交（有一份响应到页面，一份保存到redis里）
        CompletableFuture<Void> tokenCompletableFuture = CompletableFuture.runAsync(() -> {
            String orderToken = IdWorker.getIdStr();
            orderConfirmVO.setOrderToken(orderToken);
            this.redisTemplate.opsForValue().set(TOKEN_PREFIX+orderToken,orderToken);
        }, threadPoolExecutor);

        CompletableFuture.allOf(addressCompletableFuture,skuCompletableFuture,memberCompletableFuture,tokenCompletableFuture).join();
        return orderConfirmVO;
    }

    public void submit(OrderSubmitVo submitVo) {
        //1.防重复提交，查询redis中有没有orderTaken的信息。有则是第一次提交，放行并删除redis中的orderTaken

        //2.校验价格，总价一致放行

        //3.校验库存是否充足,并锁定库存，一次性提示库存不够的商品信息（远程接口待开发）

        //4.下单（创建订单，及订单详情，远程接口待开发）

        //5.删除购物车（发送消息（防止购物车删除失败，影响整个业务）删除购物车）


    }
}
