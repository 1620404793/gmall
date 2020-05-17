package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import lombok.Data;

import java.util.List;
@Data
public class OrderConfirmVO {
    private List<MemberReceiveAddressEntity> addresses;

    private List<OrderItemVO> orderItems;//类比cart的字段

    private Integer bounds;

    private String orderToken;//防止重复提交的字段
}
