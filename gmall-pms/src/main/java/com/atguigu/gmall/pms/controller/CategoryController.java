package com.atguigu.gmall.pms.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.vo.CategoryVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;


/**
 * 商品三级分类
 *
 * @author hechaocheng
 * @email lxf@atguigu.com
 * @date 2020-04-15 15:34:34
 */
@Api(tags = "商品三级分类 管理")
@RestController
@RequestMapping("pms/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    //查询两级三级的分类
    @ApiOperation("获取某个菜单的子菜单")
    @GetMapping("{pid}")
    public Resp<List<CategoryVO>> querySubCategories(@PathVariable("pid") Long pid) {
        List<CategoryVO> categoryVOS = this.categoryService.querySubCategories(pid);
        return Resp.ok(categoryVOS);
    }

    @ApiOperation("通过商品的分类查询分组")
    @GetMapping
    public Resp<List<CategoryEntity>> queryCategoryByPidOrLevel(@RequestParam(value = "level", defaultValue = "0") Integer level,//参数能设置默认值 优先设置默认值
                                                                @RequestParam(value = "parentCid", required = false) Long parentCid) {//参数required=false不是必须要传入的参数
        QueryWrapper<CategoryEntity> queryWrapper = new QueryWrapper();
        //判断分类的级别是否为0
        if (level != 0) {
            queryWrapper.eq("cat_level", level);
        }
        //判断父节点的id是否为空
        if (parentCid != null) {
            queryWrapper.eq("parent_cid", parentCid);
        }
        List<CategoryEntity> categoryEntityList = categoryService.list(queryWrapper);
        return Resp.ok(categoryEntityList);
    }

    /**
     * 列表
     */
    @ApiOperation("分页查询(排序)")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('pms:category:list')")
    public Resp<PageVo> list(QueryCondition queryCondition) {
        PageVo page = categoryService.queryPage(queryCondition);
        return Resp.ok(page);
    }


    /**
     * 信息
     */
    @ApiOperation("详情查询")
    @GetMapping("/info/{catId}")
    @PreAuthorize("hasAuthority('pms:category:info')")
    public Resp<CategoryEntity> info(@PathVariable("catId") Long catId) {
        CategoryEntity category = categoryService.getById(catId);
        return Resp.ok(category);
    }

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('pms:category:save')")
    public Resp<Object> save(@RequestBody CategoryEntity category) {
        categoryService.save(category);

        return Resp.ok(null);
    }

    /**
     * 修改
     */
    @ApiOperation("修改")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('pms:category:update')")
    public Resp<Object> update(@RequestBody CategoryEntity category) {
        categoryService.updateById(category);

        return Resp.ok(null);
    }

    /**
     * 删除
     */
    @ApiOperation("删除")
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('pms:category:delete')")
    public Resp<Object> delete(@RequestBody Long[] catIds) {
        categoryService.removeByIds(Arrays.asList(catIds));
        return Resp.ok(null);
    }

}
