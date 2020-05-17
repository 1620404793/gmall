package com.atguigu.gmall.search;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttr;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallSearchApplicationTests {
    @Autowired
    private ElasticsearchRestTemplate restTemplate;
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private GmallWmsClient gmallWmsClient;

    @Test
    public void contextLoads() {
        restTemplate.createIndex(Goods.class);
        this.restTemplate.putMapping(Goods.class);
    }

    @Test
    public void importData() {
        Long pageNum = 1l;
        Long pageSize = 100l;
        do {
            //分页查询spu
            QueryCondition queryCondition = new QueryCondition();
            queryCondition.setPage(pageNum);
            queryCondition.setLimit(pageSize);
            Resp<List<SpuInfoEntity>> spuResp = this.gmallPmsClient.querySpuByPage(queryCondition);
            List<SpuInfoEntity> spus = spuResp.getData();
            //遍历spu,查询sku
            spus.forEach(spuInfoEntity -> {
                Resp<List<SkuInfoEntity>> skuResp = this.gmallPmsClient.querySkuBySpuId(spuInfoEntity.getId());
                List<SkuInfoEntity> skuInfoEntityList = skuResp.getData();
                if (!CollectionUtils.isEmpty(skuInfoEntityList)) {
                    //把sku转化成goods对象
                    List<Goods> goodsList = skuInfoEntityList.stream().map(skuInfoEntity -> {
                        Goods goods = new Goods();

                        //查询搜索属性及值
                        Resp<List<ProductAttrValueEntity>> attrValueResp = this.gmallPmsClient.querySearchAttrValueBySpuId(spuInfoEntity.getId());
                        List<ProductAttrValueEntity> attrValueEntities = attrValueResp.getData();
                        if (!CollectionUtils.isEmpty(attrValueEntities)) {
                            List<SearchAttr> searchAttrs = attrValueEntities.stream().map(productAttrValueEntity -> {
                                SearchAttr searchAttr = new SearchAttr();
                                searchAttr.setAttrId(productAttrValueEntity.getAttrId());
                                searchAttr.setAttrName(productAttrValueEntity.getAttrName());
                                searchAttr.setAttrValue(productAttrValueEntity.getAttrValue());
                                return searchAttr;
                            }).collect(Collectors.toList());
                            goods.setAttrs(searchAttrs);
                        }

                        //查询品牌
                        Resp<BrandEntity> brandEntityResp = gmallPmsClient.queryBrandById(skuInfoEntity.getBrandId());
                        BrandEntity brandEntity = brandEntityResp.getData();
                        if (brandEntity != null) {
                            goods.setBrandId(skuInfoEntity.getBrandId());
                            goods.setBrandName(brandEntity.getName());
                        }
                        //查询分类
                        Resp<CategoryEntity> categoryEntityResp = this.gmallPmsClient.queryCategoryById(skuInfoEntity.getCatalogId());
                        CategoryEntity categoryEntity = categoryEntityResp.getData();
                        if (categoryEntity != null) {
                            goods.setCategoryId(skuInfoEntity.getCatalogId());
                            goods.setCategoryName(categoryEntity.getName());
                        }

                        goods.setCreateTime(spuInfoEntity.getCreateTime());
                        goods.setPic(skuInfoEntity.getSkuDefaultImg());

                        goods.setPrice(skuInfoEntity.getPrice().doubleValue());
                        goods.setSale(0l);
                        goods.setSkuId(skuInfoEntity.getSkuId());

                        //查询库存信息
                        Resp<List<WareSkuEntity>> listResp = this.gmallWmsClient.queryWareSkuBySkuId(skuInfoEntity.getSkuId());
                        List<WareSkuEntity> wareSkuEntities = listResp.getData();
                        //stream().anyMatch任意一个进行匹配
                        if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                            boolean flag = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
                            goods.setStore(flag);
                        }

                        goods.setTitle(skuInfoEntity.getSkuTitle());
                        return goods;
                    }).collect(Collectors.toList());
                    goodsRepository.saveAll(goodsList);
                }
            });

            //导入索引库

            pageSize = (long) spus.size();
        } while (pageSize == 100);
    }
}
