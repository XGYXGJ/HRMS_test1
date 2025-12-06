// src/main/java/com/example/hrms/controller/HRApiController.java
package com.example.hrms.controller;

import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.entity.User;
import com.example.hrms.service.PersonnelService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr/files")
public class HRApiController {

    @Autowired
    private PersonnelService personnelService;

    // 列表查询 (无需修改)
    @GetMapping
    public List<Map<String, Object>> listFiles(HttpSession session,
                                               @RequestParam(value = "q", required = false) String q) {
        User user = (User) session.getAttribute("user");
        if (user == null) throw new RuntimeException("未登录");
        if (user.getPositionId() != null && user.getPositionId() == 1) {
            return personnelService.listFiles(null, q);
        } else {
            return personnelService.listFiles(user.getL3OrgId(), q);
        }
    }

    // 获取单个 (无需修改)
    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable Integer id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) throw new RuntimeException("未登录");
        Map<String, Object> file = personnelService.getFileById(id);
        if (file == null) throw new RuntimeException("档案不存在");
        Integer fileL3 = file.get("L3_Org_ID") != null ? ((Number) file.get("L3_Org_ID")).intValue() : null;
        if (user.getPositionId() != 1 && fileL3 != null && !fileL3.equals(user.getL3OrgId())) {
            throw new RuntimeException("无权查看此档案");
        }
        return file;
    }

    // 创建
    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> payload, HttpSession session) {
        try {
            PersonnelFile file = new PersonnelFile();
            file.setName((String) payload.get("name"));
            file.setGender((String) payload.get("gender"));
            file.setIdNumber((String) payload.get("idNumber"));
            file.setPhoneNumber((String) payload.get("phoneNumber"));
            file.setAddress((String) payload.get("address"));

            User current = (User) session.getAttribute("user");
            Integer l3 = payload.get("l3OrgId") != null ? ((Number) payload.get("l3OrgId")).intValue() : (current != null ? current.getL3OrgId() : null);
            file.setL3OrgId(l3);

            // [FIX] 从 payload 中获取 positionId
            Integer positionId = payload.get("positionId") != null ? ((Number) payload.get("positionId")).intValue() : null;

            String account = (String) payload.get("account");
            String initPassword = (String) payload.get("initPassword");
            if (account != null && !account.trim().isEmpty()) {
                // [FIX] 调用修正后的方法，传入 positionId
                return personnelService.createPersonnelWithGivenAccount(file, account.trim(), initPassword, positionId);
            } else {
                // [FIX] 调用修正后的方法，传入 positionId
                return personnelService.createPersonnelAuto(file, positionId);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return Map.of("error", "create_failed", "exception", ex.getClass().getName(), "message", ex.getMessage());
        }
    }

    // 批量生成
    @PostMapping("/bulk-generate")
    public Map<String, Object> bulkGenerate(@RequestBody Map<String, Object> payload, HttpSession session) {
        User current = (User) session.getAttribute("user");
        if (current == null) throw new RuntimeException("未登录");
        int count = payload.get("count") == null ? 0 : ((Number) payload.get("count")).intValue();
        Integer l3 = payload.get("l3OrgId") != null ? ((Number) payload.get("l3OrgId")).intValue() : current.getL3OrgId();

        // [FIX] 从 payload 中获取 positionId
        Integer positionId = payload.get("positionId") != null ? ((Number) payload.get("positionId")).intValue() : null;


        // [FIX] 调用修正后的方法，传入 positionId
        List<Map<String, Object>> created = personnelService.bulkGenerate(count, l3, positionId);
        return Map.of("created", created, "count", created.size());
    }

    // 审核 (无需修改)
    @PostMapping("/{id}/approve")
    public Map<String, Object> approve(@PathVariable Integer id, HttpSession session) {
        User current = (User) session.getAttribute("user");
        if (current == null) throw new RuntimeException("未登录");
        boolean ok = personnelService.auditFile(id, true, current.getUserId());
        return Map.of("ok", ok);
    }

    // 删除 (无需修改)
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Integer id, @RequestParam(required = false) String reason) {
        boolean ok = personnelService.deleteFile(id, reason);
        return Map.of("ok", ok);
    }

    // 用户计数 (无需修改)
    @GetMapping("/users/count")
    public Map<String, Object> userCount() {
        long c = personnelService.countUsers();
        return Map.of("count", c);
    }
}
