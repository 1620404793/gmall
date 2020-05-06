package com.atguigu.gmall.index.service.impl;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class IndexServiceImpl implements IndexService {
    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Override
    public List<CategoryEntity> queryV1Categories() {
        Resp<List<CategoryEntity>> listResp = this.gmallPmsClient.queryCategoryByPidOrLevel(1, null);
        return listResp.getData();
    }

    @Override
    public List<CategoryVO> querySubCategories(Long pid) {
        return gmallPmsClient.querySubCategories(pid).getData();
    }
}
