package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.oms.dao.OrderItemDao;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.feign.GmallUmsClient;
import com.atguigu.gmall.oms.service.OrderItemService;
import com.atguigu.gmall.oms.vo.OrderItemVO;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SpuInfoEntity;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.oms.dao.OrderDao;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {
    @Autowired
    private GmallUmsClient gmallUmsClient;
    @Autowired
    private OrderItemDao itemDao;
    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private AmqpTemplate amqpTemplate;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageVo(page);
    }
    @Transactional
    @Override
    public OrderEntity saveOrder(OrderSubmitVo submitVo) {
        //保存OrderEntity

        MemberReceiveAddressEntity address = submitVo.getAddress();
        OrderEntity orderEntity=new OrderEntity();
        orderEntity.setReceiverRegion(address.getRegion());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());
        orderEntity.setReceiverCity(address.getCity());
        Resp<MemberEntity> memberEntityResp = gmallUmsClient.queryMemberById(submitVo.getUserId());
        MemberEntity memberEntity = memberEntityResp.getData();
        orderEntity.setMemberId(submitVo.getUserId());
        orderEntity.setMemberUsername(memberEntity.getUsername());

        //清算每个商品的赠送积分
        orderEntity.setIntegration(0);
        orderEntity.setGrowth(0);
        orderEntity.setDeleteStatus(0);
        orderEntity.setStatus(0);

        orderEntity.setCreateTime(new Date());
        orderEntity.setModifyTime(orderEntity.getCreateTime());
        orderEntity.setDeliveryCompany(submitVo.getDeliveryCompany());
        orderEntity.setSourceType(1);
        orderEntity.setPayType(submitVo.getPayType());
        orderEntity.setTotalAmount(submitVo.getTotalPrice());
        orderEntity.setOrderSn(submitVo.getOrderToken());
        //...
        this.save(orderEntity);
        Long orderId = orderEntity.getId();//save操作之前实体类中id为null，save之后自动返回带id的实体类
        
        //保存订单详情OrderItemEntity
        List<OrderItemVO> items = submitVo.getItems();
        items.forEach(item->{
            OrderItemEntity itemEntity = new OrderItemEntity();
            itemEntity.setSkuId(item.getSkuId());
            Resp<SkuInfoEntity> skuInfoEntityResp = gmallPmsClient.querySkuById(item.getSkuId());
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();

            Resp<SpuInfoEntity> spuInfoEntityResp = gmallPmsClient.querySpuInfoById(skuInfoEntity.getSpuId());
            SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();

            itemEntity.setSkuPrice(skuInfoEntity.getPrice());
            itemEntity.setSkuAttrsVals(JSON.toJSONString(item.getSaleAttrValues()));
            itemEntity.setCategoryId(skuInfoEntity.getCatalogId());
            itemEntity.setOrderId(orderId);
            itemEntity.setOrderSn(submitVo.getOrderToken());
            itemEntity.setSpuId(spuInfoEntity.getId());
            itemEntity.setSkuName(skuInfoEntity.getSkuName());
            itemEntity.setSpuPic(skuInfoEntity.getSkuDefaultImg());
            itemEntity.setSkuQuantity(item.getCount());
            itemEntity.setSpuName(spuInfoEntity.getSpuName());
            itemDao.insert(itemEntity);
        });

        //关闭订单，写在这里的原因，是防止响应的时候出现宕机的情况
        //订单创建之后，在响应之前发送延时消息，达到定时关单的效果
        this.amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE","order.ttl",submitVo.getOrderToken());
        return orderEntity;
    }

}