package com.atguigu.gmall.wms.vo;

import lombok.Data;

@Data
public class SkuLockVO {

    private Long skuId; //商品id

    private Integer count;//锁住多少件
}
