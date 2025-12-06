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

    @GetMapping
    public List<Map<String, Object>> listFiles(HttpSession session,
                                               @RequestParam(value = "q", required = false) String q) {
        User user = (User) session.getAttribute("user");
        if (user == null) throw new RuntimeException("未登录");
        // 传入两个参数：l3OrgId（可为 null）和 q（可为 null）
        if (user.getPositionId() != null && user.getPositionId() == 1) {
            return personnelService.listFiles(null, q);
        } else {
            return personnelService.listFiles(user.getL3OrgId(), q);
        }
    }

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

            String account = (String) payload.get("account");
            String initPassword = (String) payload.get("initPassword");
            if (account != null && !account.trim().isEmpty()) {
                return personnelService.createPersonnelWithGivenAccount(file, account.trim(), initPassword);
            } else {
                return personnelService.createPersonnelAuto(file);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return Map.of("error", "create_failed", "exception", ex.getClass().getName(), "message", ex.getMessage());
        }
    }

    @PostMapping("/bulk-generate")
    public Map<String, Object> bulkGenerate(@RequestBody Map<String, Object> payload, HttpSession session) {
        User current = (User) session.getAttribute("user");
        if (current == null) throw new RuntimeException("未登录");
        int count = payload.get("count") == null ? 0 : ((Number) payload.get("count")).intValue();
        Integer l3 = payload.get("l3OrgId") != null ? ((Number) payload.get("l3OrgId")).intValue() : current.getL3OrgId();

        List<Map<String, Object>> created = personnelService.bulkGenerate(count, l3);
        return Map.of("created", created, "count", created.size());
    }

    @PostMapping("/{id}/approve")
    public Map<String, Object> approve(@PathVariable Integer id, HttpSession session) {
        User current = (User) session.getAttribute("user");
        if (current == null) throw new RuntimeException("未登录");
        boolean ok = personnelService.auditFile(id, true, current.getUserId());
        return Map.of("ok", ok);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Integer id, @RequestParam(required = false) String reason) {
        boolean ok = personnelService.deleteFile(id, reason);
        return Map.of("ok", ok);
    }

    // 调试辅助：返回用户计数
    @GetMapping("/users/count")
    public Map<String, Object> userCount() {
        long c = personnelService.countUsers();
        return Map.of("count", c);
    }
}