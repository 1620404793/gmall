package com.atguigu.gmall.sms.dao;

import com.atguigu.gmall.sms.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author hechaocheng
 * @email 1620407593@qq.com
 * @date 2020-04-17 10:17:42
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
