package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.dao.SkuInfoDao;
import com.atguigu.gmall.pms.dao.SpuInfoDescDao;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.BaseAttrVO;
import com.atguigu.gmall.pms.vo.SkuInfoVO;
import com.atguigu.gmall.pms.vo.SpuInfoVO;
import com.atguigu.gmall.sms.vo.SkuSaleVO;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.pms.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {
    @Autowired
    private SpuInfoDescDao descDao;
    @Autowired
    private SkuInfoDao skuInfoDao;
    @Autowired
    private ProductAttrValueService attrValueService;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    private SpuInfoDescService spuInfoDescService;
    @Autowired  //远程调用
    private GmallSmsClient gmallSmsClient;
    @Autowired
    private AmqpTemplate amqpTemplate;
    @Value("${item.rabbitmp.exchange}")
    private String EXCHANG_NAME;
    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public PageVo querySpuPage(QueryCondition queryCondition, Long catId) {
        QueryWrapper<SpuInfoEntity> queryWrapper = new QueryWrapper<>();

        //判断分类是否为0  0：查全站   ！0：查本类
        if (catId!=0){
            queryWrapper.eq("catalog_id",catId);
        }
        //判断是否有关键字   （查询语句存在括号）
        String key = queryCondition.getKey();
        if (StringUtils.isNotEmpty(queryCondition.getKey())){
            queryWrapper.and(t-> t.eq("id",key).or().like("spu_name",key));
        }
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(queryCondition),
                queryWrapper
        );
        return new PageVo(page);
    }

    @Override
    @GlobalTransactional   //这是一个大方法相当于TM
    public void saveSpuInfoVO(SpuInfoVO spuInfoVO) {
        //1.保存spu相关的三张表
        //1.1   保存pms_spu_info信息
        Long spuId = saveSquInfo(spuInfoVO);
        //1.2   保存pms_spu_info_desc(事务使用cglib动态代理，所以要用不同service处理事务嵌套问题)
        this.spuInfoDescService.saveSpuInfoDesc(spuInfoVO, spuId);
        //1.3   保存pms_product_attr_value
        saveBaseAttrValue(spuInfoVO, spuId);
        //2.保存sku相关的三张表
        saveSkuAndSale(spuInfoVO, spuId);
        //int i=1/0;
        sendMsg("insert",spuId);
    }

    private void sendMsg(String type,Long spuId) {

        this.amqpTemplate.convertAndSend(EXCHANG_NAME,"item."+type,spuId);
    }


    private void saveSkuAndSale(SpuInfoVO spuInfoVO, Long spuId) {
        List<SkuInfoVO> skus=spuInfoVO.getSkus();
        if (CollectionUtils.isEmpty(skus)){
            return;//如果skus为空，直接返回
        }
        skus.forEach(skuInfoVO -> {
            //2.1   保存pms_sku_info
            skuInfoVO.setSpuId(spuId);
            skuInfoVO.setSkuCode(UUID.randomUUID().toString());
            skuInfoVO.setBrandId(spuInfoVO.getBrandId());
            skuInfoVO.setCatalogId(spuInfoVO.getCatalogId());
            List<String> images = skuInfoVO.getImages();
            if (!CollectionUtils.isEmpty(images)){
                //根据前端是否有设置默认图片的功能，再设置默认图片
                skuInfoVO.setSkuDefaultImg(StringUtils.isNotBlank(skuInfoVO.getSkuDefaultImg())?skuInfoVO.getSkuDefaultImg():images.get(0));
            }
            this.skuInfoDao.insert(skuInfoVO);
            //2.2   保存pms_sku_images
            Long skuId = skuInfoVO.getSkuId();
            if (!CollectionUtils.isEmpty(images)){
                List<SkuImagesEntity> skuImagesEntities = images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setImgUrl(image);
                    skuImagesEntity.setSkuId(skuId);
                    //设置是否是默认图片*******************************
                    skuImagesEntity.setDefaultImg(StringUtils.equals(skuInfoVO.getSkuDefaultImg(), image) ? 1 : 0);
                    return skuImagesEntity;
                }).collect(Collectors.toList());
                this.skuImagesService.saveBatch(skuImagesEntities);
            }
            //2.3   保存pms_sale_attr_value  (集合可以批量保存)
            List<SkuSaleAttrValueEntity> saleAttrs = skuInfoVO.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)){
                //高效的集合遍历方法
                saleAttrs.forEach(skuSaleAttrValueEntity -> skuSaleAttrValueEntity.setSkuId(skuId));
                this.skuSaleAttrValueService.saveBatch(saleAttrs);
            }
            //3.保存营销相关的三张表（feign远程调用）
            SkuSaleVO skuSaleVO = new SkuSaleVO();
            BeanUtils.copyProperties(skuInfoVO,skuSaleVO);
            skuSaleVO.setSkuId(skuId);
            this.gmallSmsClient.saveSaleVO(skuSaleVO);
        });
    }

    private void saveBaseAttrValue(SpuInfoVO spuInfoVO, Long spuId) {
        List<BaseAttrVO> baseAttrs = spuInfoVO.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)){
            List<ProductAttrValueEntity> productAttrValueEntities = baseAttrs.stream().map(baseAttrVO -> {
                ProductAttrValueEntity productAttrValueEntity=baseAttrVO;
                productAttrValueEntity.setSpuId(spuId);
                return productAttrValueEntity;
            }).collect(Collectors.toList());
            this.attrValueService.saveBatch(productAttrValueEntities);
        }
    }


    private Long saveSquInfo(SpuInfoVO spuInfoVO) {
        spuInfoVO.setCreateTime(new Date());
        spuInfoVO.setUodateTime(spuInfoVO.getCreateTime());
        this.save(spuInfoVO);
        return spuInfoVO.getId();
    }

}