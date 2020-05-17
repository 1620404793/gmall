package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.dao.SkuFullReductionDao;
import com.atguigu.gmall.sms.dao.SkuLadderDao;
import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.vo.SaleVO;
import com.atguigu.gmall.sms.vo.SkuSaleVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.sms.dao.SkuBoundsDao;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import org.springframework.transaction.annotation.Transactional;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsDao, SkuBoundsEntity> implements SkuBoundsService {
    @Autowired
    private SkuLadderDao skuLadderDao;
    @Autowired
    private SkuFullReductionDao reductionDao;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SkuBoundsEntity> page = this.page(
                new Query<SkuBoundsEntity>().getPage(params),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageVo(page);
    }

    @Override
    @Transactional
    public void saveSkuSaleVO(SaleVO saleVO) {
        //3.1   保存sms_sku_bounds
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        skuBoundsEntity.setSkuId(saleVO.getSkuId());
        skuBoundsEntity.setGrowBounds(saleVO.getGrowBounds());
        skuBoundsEntity.setBuyBounds(saleVO.getBuyBounds());
        List<Integer> work = saleVO.getWork();
        /**
         * 优惠生效情况[1111（四个状态位，从右到左）;
         * 0 - 无优惠，成长积分是否赠送;
         * 1 - 无优惠，购物积分是否赠送;
         * 2 - 有优惠，成长积分是否赠送;
         * 3 - 有优惠，购物积分是否赠送【状态位0：不赠送，1：赠送】]
         */
        skuBoundsEntity.setWork(work.get(3) * 1 + work.get(2) * 2 + work.get(1) * 4 + work.get(0) * 8);
        this.save(skuBoundsEntity);
        //3.2   保存sms_sku_ladder
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        skuLadderEntity.setSkuId(saleVO.getSkuId());
        skuLadderEntity.setDiscount(saleVO.getDiscount());
        skuLadderEntity.setFullCount(saleVO.getFullCount());
        skuLadderEntity.setAddOther(saleVO.getLadderAddOther());
        this.skuLadderDao.insert(skuLadderEntity);
        //3.3   保存sms_sku_full_reduction
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        skuFullReductionEntity.setFullPrice(saleVO.getFullPrice());
        skuFullReductionEntity.setReducePrice(saleVO.getReducePrice());
        skuFullReductionEntity.setSkuId(saleVO.getSkuId());
        skuFullReductionEntity.setAddOther(saleVO.getFullAddOther());
        this.reductionDao.insert(skuFullReductionEntity);
    }

    @Override
    public List<SkuSaleVO> querySkuSalesBySkuId(Long skuId) {
        List<SkuSaleVO> skuSaleVOS = new ArrayList<>();
        //查询积分信息
        SkuBoundsEntity skuBoundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));
        if (skuBoundsEntity != null) {
            SkuSaleVO boundsVO = new SkuSaleVO();
            boundsVO.setType("积分");
            StringBuffer sb = new StringBuffer();
            if (skuBoundsEntity.getGrowBounds() != null && skuBoundsEntity.getGrowBounds().intValue() > 0) {
                sb.append("成长积分送" + skuBoundsEntity.getGrowBounds());
            }
            if (skuBoundsEntity.getBuyBounds() != null && skuBoundsEntity.getBuyBounds().intValue() > 0) {
                if (StringUtils.isNotBlank(sb)) {
                    sb.append(",");
                }
                sb.append("赠送积分" + skuBoundsEntity.getBuyBounds());
            }
            boundsVO.setDesc(sb.toString());
            skuSaleVOS.add(boundsVO);
        }

        //查询打折信息
        QueryWrapper<SkuLadderEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sku_id", skuId);
        SkuLadderEntity skuLadderEntity = this.skuLadderDao.selectOne(queryWrapper);
        if (skuLadderEntity != null) {
            SkuSaleVO ladderVO = new SkuSaleVO();
            ladderVO.setType("打折");
            ladderVO.setDesc("满" + skuLadderEntity.getFullCount() + "件，打" + skuLadderEntity.getDiscount().divide(new BigDecimal(10)) + "折");
            skuSaleVOS.add(ladderVO);
        }

        //查询满减
        SkuFullReductionEntity skuFullReductionEntity = this.reductionDao.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));
        if (skuFullReductionEntity != null) {
            SkuSaleVO reductionVO = new SkuSaleVO();
            reductionVO.setType("满减");
            reductionVO.setDesc("满" + skuFullReductionEntity.getFullPrice() + "减" + skuFullReductionEntity.getReducePrice());
            skuSaleVOS.add(reductionVO);
        }
        return skuSaleVOS;
    }

}