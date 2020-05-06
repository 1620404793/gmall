package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.dao.AttrAttrgroupRelationDao;
import com.atguigu.gmall.pms.dao.AttrDao;
import com.atguigu.gmall.pms.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.vo.GroupVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.pms.dao.AttrGroupDao;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import org.springframework.util.CollectionUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {
    @Autowired
    private AttrAttrgroupRelationDao attrAttrgroupRelationDao;
    @Autowired
    private AttrDao attrDao;
    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public PageVo queryGroupByPage(QueryCondition condition, Long catId) {
        QueryWrapper<AttrGroupEntity> queryWrapper = new QueryWrapper<>();
        if (catId!=null){
            queryWrapper.eq("catelog_id",catId);
        }
        IPage<AttrGroupEntity> page = this.page(
                //转换为Page对象，给dao查询数据库
                new Query<AttrGroupEntity>().getPage(condition),
                queryWrapper
        );
        //转化为PageVo转换给页面
        return new PageVo(page);
    }

    @Override
    //通过组号查询组和中间表及组相关属性
    public GroupVO queryGroupWithAttrsByGid(Long gid) {

        GroupVO groupVO=new GroupVO();
        //查询group
        AttrGroupEntity groupEntity = this.getById(gid);
        BeanUtils.copyProperties(groupEntity,groupVO);

       // System.out.println("使用工具类copy后的结果:"+groupVO);
        //根据gid查询关联关系，并获取attrIds
        List<AttrAttrgroupRelationEntity> relationEntities = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", gid));
        if (CollectionUtils.isEmpty(relationEntities)){
            return groupVO;
        }
        groupVO.setRelations(relationEntities);

        //根据attrIds查询，获取所有的规格参数
        List<Long> attrIds = relationEntities.stream().map(relationEntity -> relationEntity.getAttrId()).collect(Collectors.toList());
        List<AttrEntity> attrEntities = this.attrDao.selectBatchIds(attrIds);
        groupVO.setAttrEntities(attrEntities);
        return groupVO;
    }

    @Override
    public List<GroupVO> queryGroupWithAttrsByCid(Long cid) {
        //根据cid查询三级分类下所有的属性分组
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", cid));
        //1.根据分组中的gid查询中间表
        //2.根据中间表的attrIds查询参数
        //3.数据类型转换：attrGroupEntities-->groupVOS
        List<GroupVO> groupVOS = attrGroupEntities.stream().map(attrGroupEntity -> {
            //调用方法
            return this.queryGroupWithAttrsByGid(attrGroupEntity.getAttrGroupId());
        }).collect(Collectors.toList());
        return groupVOS;
    }

}