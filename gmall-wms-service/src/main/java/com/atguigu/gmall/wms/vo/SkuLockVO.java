package com.atguigu.gmall.wms.vo;

import lombok.Data;

@Data
public class SkuLockVO {

    private Long skuId; //商品id

    private Integer count;//锁住多少件

    private Boolean lock;//商品的锁定状态

    private Long wareSkuId;//锁定库存的id

    private String orderToken;
}
