package com.atguigu.gmall.pms.api;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.CategoryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface GmallPmsApi {
    @GetMapping("pms/spuinfo/page")
    public Resp<List<SpuInfoEntity>> querySpuByPage(@RequestBody QueryCondition queryCondition);

    @GetMapping("pms/skuinfo/{spuId}")
    public Resp<List<SkuInfoEntity>> querySkuBySpuId(@PathVariable("spuId")Long supId);

    @GetMapping("pms/spuinfo/info/{id}")
    public Resp<SpuInfoEntity> querySpuInfoById(@PathVariable("id") Long id);

    @GetMapping("pms/brand/info/{brandId}")//改变了原方法名，原方法不用做修改，因为路径一样
    public Resp<BrandEntity> queryBrandById(@PathVariable("brandId") Long brandId);

    @GetMapping("pms/category/info/{catId}")
    public Resp<CategoryEntity> queryCategoryById(@PathVariable("catId") Long catId);

    @GetMapping("pms/productattrvalue/{spuId}")
    public Resp<List<ProductAttrValueEntity>> querySearchAttrValueBySpuId(@PathVariable("spuId")Long spuId);


    //查询一介分类的方法
    @GetMapping("pms/category")
    public Resp<List<CategoryEntity>> queryCategoryByPidOrLevel(@RequestParam(value = "level",defaultValue = "0") Integer level,//参数能设置默认值 优先设置默认值
                                                                @RequestParam(value = "parentCid",required = false)Long parentCid);
    @GetMapping("pms/category/{pid}")
    public Resp<List<CategoryVO>> querySubCategories(@PathVariable("pid") Long pid);

}
