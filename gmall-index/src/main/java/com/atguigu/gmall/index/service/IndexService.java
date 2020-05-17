package com.atguigu.gmall.index.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVO;

import java.util.List;

public interface IndexService {

    public List<CategoryEntity> queryV1Categories();

    List<CategoryVO> querySubCategories(Long pid);

    void testLock();

    String testRead();

    String testWrite();

    String testLatch() throws InterruptedException;

    String testCount();
}
