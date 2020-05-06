package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.dao.SkuFullReductionDao;
import com.atguigu.gmall.sms.dao.SkuLadderDao;
import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.service.SkuLadderService;
import com.atguigu.gmall.sms.vo.SkuSaleVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
    public void saveSkuSaleVO(SkuSaleVO skuSaleVO) {
        //3.1   保存sms_sku_bounds
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        skuBoundsEntity.setSkuId(skuSaleVO.getSkuId());
        skuBoundsEntity.setGrowBounds(skuSaleVO.getGrowBounds());
        skuBoundsEntity.setBuyBounds(skuSaleVO.getBuyBounds());
        List<Integer> work = skuSaleVO.getWork();
        /**
         * 优惠生效情况[1111（四个状态位，从右到左）;
         * 0 - 无优惠，成长积分是否赠送;
         * 1 - 无优惠，购物积分是否赠送;
         * 2 - 有优惠，成长积分是否赠送;
         * 3 - 有优惠，购物积分是否赠送【状态位0：不赠送，1：赠送】]
         */
        skuBoundsEntity.setWork(work.get(3)*1+work.get(2)*2+work.get(1)*4+work.get(0)*8);
        this.save(skuBoundsEntity);
        //3.2   保存sms_sku_ladder
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        skuLadderEntity.setSkuId(skuSaleVO.getSkuId());
        skuLadderEntity.setDiscount(skuSaleVO.getDiscount());
        skuLadderEntity.setFullCount(skuSaleVO.getFullCount());
        skuLadderEntity.setAddOther(skuSaleVO.getLadderAddOther());
        this.skuLadderDao.insert(skuLadderEntity);
        //3.3   保存sms_sku_full_reduction
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        skuFullReductionEntity.setFullPrice(skuSaleVO.getFullPrice());
        skuFullReductionEntity.setReducePrice(skuSaleVO.getReducePrice());
        skuFullReductionEntity.setSkuId(skuSaleVO.getSkuId());
        skuFullReductionEntity.setAddOther(skuSaleVO.getFullAddOther());
        this.reductionDao.insert(skuFullReductionEntity);
    }

}