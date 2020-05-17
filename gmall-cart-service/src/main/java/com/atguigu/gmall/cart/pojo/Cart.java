package com.atguigu.gmall.cart.pojo;

import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SkuSaleVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
@Data
public class Cart {
    private Long skuId;
    private String title;
    private String defaultImage;
    private BigDecimal price;
    private BigDecimal currentPrice;//当前的价格

    private Integer count;
    private Boolean store;//库存

    private List<SkuSaleAttrValueEntity> saleAttrValues;
    private List<SkuSaleVO> sales;

    private Boolean  check; //勾选状态
}
