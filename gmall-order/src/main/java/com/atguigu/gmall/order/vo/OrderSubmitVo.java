package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitVo {

    private String orderToken;  //防重

    private MemberReceiveAddressEntity address;

    private Integer payType;

    private String deliveryCompany;

    private List<OrderItemVO> items;  //购物清单

    private Integer bounds;

    private BigDecimal totalPrice; //校验价格
}
