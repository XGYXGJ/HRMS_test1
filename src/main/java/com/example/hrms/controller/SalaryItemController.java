package com.example.hrms.controller;

import com.example.hrms.entity.SalaryItem;
import com.example.hrms.service.SalaryItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 薪酬项目管理控制器
 */
@Controller
@RequestMapping("/admin/salary/item")
public class SalaryItemController {

    @Autowired
    private SalaryItemService itemService;

    // 1. 薪酬项目列表页面
    @GetMapping("/list")
    public String listItemPage(Model model) {
        List<SalaryItem> items = itemService.listAllItems();
        model.addAttribute("items", items);
        model.addAttribute("newItem", new SalaryItem()); // 用于添加表单
        return "admin/salary_item_list";
    }

    // 2. 添加薪酬项目
    @PostMapping("/add")
    public String addItem(SalaryItem item) {
        // 简单校验
        if (item.getItemName() != null && item.getItemType() != null) {
            itemService.saveItem(item);
        }
        return "redirect:/admin/salary/item/list";
    }

    // 3. 删除薪酬项目
    @GetMapping("/delete/{itemId}")
    public String deleteItem(@PathVariable Integer itemId) {
        itemService.removeItem(itemId);
        return "redirect:/admin/salary/item/list";
    }

    // 4. 编辑薪酬项目（跳转到编辑页面）
    @GetMapping("/edit/{itemId}")
    public String editItemPage(@PathVariable Integer itemId, Model model) {
        SalaryItem item = itemService.getItemById(itemId);
        model.addAttribute("item", item);
        return "admin/salary_item_edit";
    }

    // 5. 更新薪酬项目
    @PostMapping("/update")
    public String updateItem(SalaryItem item) {
        itemService.updateItem(item);
        return "redirect:/admin/salary/item/list";
    }
}