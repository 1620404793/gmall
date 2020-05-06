package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.SpuInfoVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.pms.entity.SpuInfoEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;


/**
 * spu信息
 *
 * @author hechaocheng
 * @email lxf@atguigu.com
 * @date 2020-04-15 15:34:34
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageVo queryPage(QueryCondition params);

    PageVo querySpuPage(QueryCondition queryCondition, Long catId);

    void saveSpuInfoVO(SpuInfoVO spuInfoVO);

/*  因为存在事务写到不同的service方法中去做
    public void saveSpuInfoDesc(SpuInfoVO spuInfoVO, Long spuId);
*/
}

