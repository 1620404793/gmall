package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.ProductAttrValueEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class BaseAttrVO extends ProductAttrValueEntity {
    //根据json属性名，重写Set方法
    public void setValueSelected(List<String> selecteds) {
        if (CollectionUtils.isEmpty(selecteds)) {
            return;
        }
        this.setAttrValue(StringUtils.join(','));
    }
}
