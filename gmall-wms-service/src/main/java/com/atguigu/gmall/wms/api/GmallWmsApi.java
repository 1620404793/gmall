package com.atguigu.gmall.wms.api;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface GmallWmsApi {
    @GetMapping("wms/waresku/{skuId}")  //因为有不同地址的仓库，所以返回值为list
    public Resp<List<WareSkuEntity>> queryWareSkuBySkuId(@PathVariable("skuId") Long skuId);
}
