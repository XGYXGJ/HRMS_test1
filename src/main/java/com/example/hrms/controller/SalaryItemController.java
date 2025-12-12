package com.example.hrms.controller;

import com.example.hrms.entity.SalaryItem;
import com.example.hrms.service.SalaryItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/salary-item")
public class SalaryItemController {

    @Autowired
    private SalaryItemService itemService;

    @GetMapping("/list")
    public String listItemPage(Model model) {
        List<SalaryItem> items = itemService.listAllItems();
        model.addAttribute("items", items);
        model.addAttribute("newItem", new SalaryItem());
        return "admin/salary_item_list";
    }

    @PostMapping("/add")
    public String addItem(SalaryItem item, Model model) {
        if (item.getItemName() != null && item.getItemType() != null) {
            itemService.saveItem(item);
            model.addAttribute("success", "薪酬项目添加成功！");
        }
        return listItemPage(model);
    }

    @GetMapping("/delete/{itemId}")
    public String deleteItem(@PathVariable Integer itemId, Model model) {
        try {
            itemService.removeItem(itemId);
            model.addAttribute("success", "薪酬项目删除成功！");
        } catch (Exception e) {
            model.addAttribute("error", "删除失败，该项目可能正在被薪酬标准使用。");
        }
        return listItemPage(model);
    }

    @GetMapping("/edit/{itemId}")
    public String editItemPage(@PathVariable Integer itemId, Model model) {
        SalaryItem item = itemService.getItemById(itemId);
        model.addAttribute("item", item);
        return "admin/salary_item_edit";
    }

    @PostMapping("/edit")
    public String updateItem(SalaryItem item, Model model) {
        itemService.updateItem(item);
        model.addAttribute("success", "薪酬项目更新成功！");
        return listItemPage(model);
    }
}