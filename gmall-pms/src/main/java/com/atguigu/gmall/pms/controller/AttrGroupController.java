package com.atguigu.gmall.pms.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.vo.GroupVO;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;


/**
 * 属性分组
 *
 * @author hechaocheng
 * @email lxf@atguigu.com
 * @date 2020-04-15 15:34:34
 */
@Api(tags = "属性分组 管理")
@RestController
@RequestMapping("pms/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @ApiOperation("根据分类id和spuId查询分组及组下的规格参数")
    @GetMapping("item/group/{cid}/{spuId}")
    public Resp<List<ItemGroupVO>> queryItemGroupVObyCidAndSpuId(@PathVariable("cid") Long cid,
                                                                 @PathVariable("spuId") Long spuId) {
        List<ItemGroupVO> itemGroupVOS = this.attrGroupService.queryItemGroupVObyCidAndSpuId(cid, spuId);
        return Resp.ok(itemGroupVOS);
    }

    @ApiOperation("根据三级分类id查询分组及组下的规格参数")
    @GetMapping("/withattrs/cat/{catId}")
    public Resp<List<GroupVO>> queryGroupWithAttrsByCid(@PathVariable("catId") Long cid) {
        List<GroupVO> groupVOS = attrGroupService.queryGroupWithAttrsByCid(cid);
        return Resp.ok(groupVOS);
    }

    @ApiOperation("根据分组id查询分组及组下的规格参数")
    @RequestMapping("withattr/{gid}")
    public Resp<GroupVO> queryGroupWithAttrsByGid(@PathVariable("gid") Long gid) {
        GroupVO groupVO = attrGroupService.queryGroupWithAttrsByGid(gid);
        return Resp.ok(groupVO);
    }

    @ApiOperation("分组的分页查询(排序)")
    @GetMapping("{catId}")
    public Resp<PageVo> queryGroupByPage(QueryCondition condition, @PathVariable("catId") Long catId) {
        PageVo page = attrGroupService.queryGroupByPage(condition, catId);
        return Resp.ok(page);
    }

    /**
     * 列表
     */
    @ApiOperation("分页查询(排序)")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('pms:attrgroup:list')")
    public Resp<PageVo> list(QueryCondition queryCondition) {
        PageVo page = attrGroupService.queryPage(queryCondition);
        return Resp.ok(page);
    }


    /**
     * 信息
     */
    @ApiOperation("详情查询")
    @GetMapping("/info/{attrGroupId}")
    @PreAuthorize("hasAuthority('pms:attrgroup:info')")
    public Resp<AttrGroupEntity> info(@PathVariable("attrGroupId") Long attrGroupId) {
        AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);

        return Resp.ok(attrGroup);
    }

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('pms:attrgroup:save')")
    public Resp<Object> save(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.save(attrGroup);

        return Resp.ok(null);
    }

    /**
     * 修改
     */
    @ApiOperation("修改")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('pms:attrgroup:update')")
    public Resp<Object> update(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.updateById(attrGroup);

        return Resp.ok(null);
    }

    /**
     * 删除
     */
    @ApiOperation("删除")
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('pms:attrgroup:delete')")
    public Resp<Object> delete(@RequestBody Long[] attrGroupIds) {
        attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return Resp.ok(null);
    }

}
