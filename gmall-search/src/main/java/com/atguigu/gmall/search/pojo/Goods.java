package com.atguigu.gmall.search.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Document(indexName = "goods", type = "info", shards = 3, replicas = 2)
@Data
public class Goods {
    @Id
    private Long skuId;
    @Field(type = FieldType.Keyword, index = false)
    private String pic;
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;
    @Field(type = FieldType.Double)
    private Double price;
    @Field(type = FieldType.Long)
    private Long sale;  //销量
    @Field(type = FieldType.Boolean)
    private boolean store;  //是否有货
    @Field(type = FieldType.Date)
    private Date createTime;//创建时间  新品

    //一下字段是为了方便产品的聚合和展示
    @Field(type = FieldType.Long)
    private Long brandId;
    @Field(type = FieldType.Keyword)
    private String brandName;
    @Field(type = FieldType.Long)
    private Long categoryId;
    @Field(type = FieldType.Keyword)
    private String categoryName;

    //属性的集合  Nested嵌套属性
    @Field(type = FieldType.Nested)
    private List<SearchAttr> attrs;

}
