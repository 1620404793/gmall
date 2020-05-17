package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SkuSaleVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
@Data
public class OrderItemVO {
    private Long skuId;
    private String title;
    private String defaultImage;
    private BigDecimal price; //数据库中取的价格（即：最新）
    private Integer count;
    private Boolean store;//库存
    private List<SkuSaleAttrValueEntity> saleAttrValues;
    private List<SkuSaleVO> sales;
    private BigDecimal weight;
}
