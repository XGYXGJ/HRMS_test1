package com.example.hrms.service;

import com.example.hrms.entity.SalaryItem;
import com.example.hrms.mapper.SalaryItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 薪酬项目业务服务
 */
@Service
public class SalaryItemService {

    @Autowired
    private SalaryItemMapper itemMapper;

    // 获取所有项目
    public List<SalaryItem> listAllItems() {
        return itemMapper.selectList(null);
    }

    // 保存新项目
    public void saveItem(SalaryItem item) {
        itemMapper.insert(item);
    }

    // 根据ID删除项目
    public void removeItem(Integer itemId) {
        itemMapper.deleteById(itemId);
    }

    // 根据ID查询项目
    public SalaryItem getItemById(Integer itemId) {
        return itemMapper.selectById(itemId);
    }

    // 更新项目
    public void updateItem(SalaryItem item) {
        itemMapper.updateById(item);
    }
}