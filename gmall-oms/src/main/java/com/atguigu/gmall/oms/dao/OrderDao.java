package com.atguigu.gmall.oms.dao;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 *
 * @author hechaocheng
 * @email 1620407593@qq.com
 * @date 2020-04-17 11:00:50
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {

}
