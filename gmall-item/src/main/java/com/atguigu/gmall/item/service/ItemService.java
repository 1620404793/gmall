package com.atguigu.gmall.item.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.item.config.ThreadPoolConfig;
import com.atguigu.gmall.item.vo.ItemVO;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import com.atguigu.gmall.sms.api.GmallSmsApi;
import com.atguigu.gmall.sms.vo.SkuSaleVO;
import com.atguigu.gmall.wms.api.GmallWmsApi;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

@Service
public class ItemService {
    @Autowired
    private GmallPmsApi gmallPmsApi;
    @Autowired
    private GmallWmsApi gmallWmsApi;
    @Autowired
    private GmallSmsApi gmallSmsApi;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    public ItemVO queryItemVO(Long skuId) {
        ItemVO itemVO = new ItemVO();
        itemVO.setSkuId(skuId);

        //根据is查询sku
        CompletableFuture<Object> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Resp<SkuInfoEntity> skuResp = this.gmallPmsApi.querySkuById(skuId);
            SkuInfoEntity skuInfoEntity = skuResp.getData();
            if (skuInfoEntity == null) {
                return itemVO;
            }
            itemVO.setSkuTitle(skuInfoEntity.getSkuTitle());
            itemVO.setSubTitle(skuInfoEntity.getSkuSubtitle());
            itemVO.setPrice(skuInfoEntity.getPrice());
            itemVO.setWeight(skuInfoEntity.getWeight());
            itemVO.setSpuId(skuInfoEntity.getSpuId());
            //获取spuId
            return skuInfoEntity;
            //加个线程池
        }, threadPoolExecutor);

        CompletableFuture<Void> spuCompletableFuture = skuCompletableFuture.thenAcceptAsync(sku -> {
            //根据sku中的spuId查询spu
            Resp<SpuInfoEntity> spuInfoEntityResp = this.gmallPmsApi.querySpuInfoById(((SkuInfoEntity) sku).getSpuId());
            SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
            if (spuInfoEntity != null) {
                itemVO.setSpuName(spuInfoEntity.getSpuName());
            }
        }, threadPoolExecutor);


        CompletableFuture<Void> imageCompletableFuture = CompletableFuture.runAsync(() -> {  //没有依赖前面线程中的数据
            //根据skuId查询 设置sku图片列表
            Resp<List<SkuImagesEntity>> skuImagesResp = this.gmallPmsApi.querySkuImagesBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntities = skuImagesResp.getData();
            itemVO.setPics(skuImagesEntities);
        }, threadPoolExecutor);


        //根据brandId和CategoryId查询品牌和分类
        CompletableFuture<Void> brandCompletableFuture = skuCompletableFuture.thenAcceptAsync(sku -> {
            BrandEntity brandEntity = this.gmallPmsApi.queryBrandById(((SkuInfoEntity) sku).getBrandId()).getData();
            itemVO.setBrandEntity(brandEntity);  //没有使用对象里的get方法就不用判断为空了
        }, threadPoolExecutor);
        CompletableFuture<Void> categoryCompletableFuture = skuCompletableFuture.thenAcceptAsync(sku -> {
            CategoryEntity categoryEntity = this.gmallPmsApi.queryCategoryById(((SkuInfoEntity) sku).getCatalogId()).getData();
            itemVO.setCategoryEntity(categoryEntity);
        }, threadPoolExecutor);

        CompletableFuture<Void> saleCompletableFuture = CompletableFuture.runAsync(() -> {
            //根据skuId查询营销的一些信息
            List<SkuSaleVO> saleVOS = this.gmallSmsApi.querySkuSalesBySkuId(skuId).getData();
            itemVO.setSales(saleVOS);
        }, threadPoolExecutor);

        CompletableFuture<Void> storeCompletableFuture = CompletableFuture.runAsync(() -> {
            //根据skuId查询库存信息
            List<WareSkuEntity> wareSkuEntities = this.gmallWmsApi.queryWareSkuBySkuId(skuId).getData();
            itemVO.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
        }, threadPoolExecutor);


        CompletableFuture<Void> saleAttrCompletableFuture = skuCompletableFuture.thenAcceptAsync(sku -> {
            //根据spuId查询对应的所有skuId，再去查询所有的销售属性
            List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = this.gmallPmsApi.querySkuSaleAttrValueBySpuId(((SkuInfoEntity) sku).getSpuId()).getData();
            itemVO.setSaleAttrs(skuSaleAttrValueEntities);
        }, threadPoolExecutor);


        //根据spuId查询商品描述 即：海报信息
        CompletableFuture<Void> descCompletableFuture = skuCompletableFuture.thenAcceptAsync(sku -> {
            SpuInfoDescEntity infoDescEntity = this.gmallPmsApi.querySpuDesBySpuId(((SkuInfoEntity) sku).getSpuId()).getData();
            if (infoDescEntity != null) {
                String decript = infoDescEntity.getDecript();
                String[] split = StringUtils.split(decript, ",");
                itemVO.setImages(Arrays.asList(split));
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> groupCompletableFuture = skuCompletableFuture.thenAcceptAsync(sku -> {
            //根据spuId和cateId查询组及组下的规格参数（带值的）
            List<ItemGroupVO> itemGroupVOS = this.gmallPmsApi.queryItemGroupVObyCidAndSpuId(((SkuInfoEntity) sku).getCatalogId(), ((SkuInfoEntity) sku).getSpuId()).getData();
            itemVO.setGroups(itemGroupVOS);
        }, threadPoolExecutor);

        CompletableFuture.allOf(spuCompletableFuture, imageCompletableFuture, brandCompletableFuture, categoryCompletableFuture,
                saleCompletableFuture, saleAttrCompletableFuture, descCompletableFuture, groupCompletableFuture).join();
        return itemVO;
    }

    public ItemVO queryItemVOBeifen(Long skuId) {
        ItemVO itemVO = new ItemVO();
        itemVO.setSkuId(skuId);

        //根据is查询sku
        Resp<SkuInfoEntity> skuResp = this.gmallPmsApi.querySkuById(skuId);
        SkuInfoEntity skuInfoEntity = skuResp.getData();
        if (skuInfoEntity == null) {
            return itemVO;
        }
        itemVO.setSkuTitle(skuInfoEntity.getSkuTitle());
        itemVO.setSubTitle(skuInfoEntity.getSkuSubtitle());
        itemVO.setPrice(skuInfoEntity.getPrice());
        itemVO.setWeight(skuInfoEntity.getWeight());

        //根据sku中的spuId查询spu
        Long spuId = skuInfoEntity.getSpuId();
        Resp<SpuInfoEntity> spuInfoEntityResp = this.gmallPmsApi.querySpuInfoById(spuId);
        SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
        itemVO.setSpuId(spuId);
        if (spuInfoEntity != null) {
            itemVO.setSpuName(spuInfoEntity.getSpuName());
        }

        //根据skuId查询 设置sku图片列表
        Resp<List<SkuImagesEntity>> skuImagesResp = this.gmallPmsApi.querySkuImagesBySkuId(skuId);
        List<SkuImagesEntity> skuImagesEntities = skuImagesResp.getData();
        itemVO.setPics(skuImagesEntities);

        //根据brandId和CategoryId查询品牌和分类
        BrandEntity brandEntity = this.gmallPmsApi.queryBrandById(skuInfoEntity.getBrandId()).getData();
        itemVO.setBrandEntity(brandEntity);  //没有使用对象里的get方法就不用判断为空了
        CategoryEntity categoryEntity = this.gmallPmsApi.queryCategoryById(skuInfoEntity.getCatalogId()).getData();
        itemVO.setCategoryEntity(categoryEntity);

        //根据skuId查询营销的一些信息
        List<SkuSaleVO> saleVOS = this.gmallSmsApi.querySkuSalesBySkuId(skuId).getData();
        itemVO.setSales(saleVOS);

        //根据skuId查询库存信息
        List<WareSkuEntity> wareSkuEntities = this.gmallWmsApi.queryWareSkuBySkuId(skuId).getData();
        itemVO.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));

        //根据spuId查询对应的所有skuId，再去查询所有的销售属性
        List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = this.gmallPmsApi.querySkuSaleAttrValueBySpuId(spuId).getData();
        itemVO.setSaleAttrs(skuSaleAttrValueEntities);

        //根据spuId查询商品描述 即：海报信息
        SpuInfoDescEntity infoDescEntity = this.gmallPmsApi.querySpuDesBySpuId(spuId).getData();
        if (infoDescEntity != null) {
            String decript = infoDescEntity.getDecript();
            String[] split = StringUtils.split(decript, ",");
            itemVO.setImages(Arrays.asList(split));
        }

        //根据spuId和cateId查询组及组下的规格参数（带值的）
        List<ItemGroupVO> itemGroupVOS = this.gmallPmsApi.queryItemGroupVObyCidAndSpuId(skuInfoEntity.getCatalogId(), spuId).getData();
        itemVO.setGroups(itemGroupVOS);
        return itemVO;
    }

    public static void main(String[] args) {
        CompletableFuture.supplyAsync(() -> {
            System.out.println("runAsync.........");
            int i = 1 / 0;
            return "hello supplyAsync";
        }).whenComplete((t, u) -> {//让它执行完
            System.out.println(t);  //t是返回正常结果
            System.out.println(u); //u是返回异常结果
        }).exceptionally(t -> {//专门处理异常的结果集
            System.out.println(t);
            return "hello exceptionally..";
        }).handleAsync((t, u) -> {
            System.out.println(t);
            System.out.println(u);
            return "hello handleAsync";
        });


        //线程池
      /*  ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(3, 5, 50, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), Executors.defaultThreadFactory(), (Runnable r, ThreadPoolExecutor executor) -> {
            System.out.println("执行了拒绝策略");
        });
        for (int i=0;i<50;i++){
            poolExecutor.execute(()->{
                System.out.println("thread start..."+Thread.currentThread().getName());
                System.out.println("==================");
                System.out.println("thread end..........");
            });
        }*/

    }
}
