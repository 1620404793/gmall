package com.atguigu.gmall.pms.vo;


import com.atguigu.gmall.pms.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class GroupVO extends AttrGroupEntity {
    private List<AttrEntity> attrEntities;
    private List<AttrAttrgroupRelationEntity> relations;
}
