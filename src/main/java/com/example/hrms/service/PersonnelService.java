// src/main/java/com/example/hrms/service/PersonnelService.java
package com.example.hrms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.hrms.entity.PersonnelFile;
import com.example.hrms.entity.User;
import com.example.hrms.mapper.PersonnelFileMapper;
import com.example.hrms.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PersonnelService {

    @Autowired
    private PersonnelFileMapper fileMapper;

    @Autowired
    private UserMapper userMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 列表与搜索 (保持不变)
    public List<Map<String, Object>> listFiles(Integer l3OrgId, String q) {
        List<Map<String, Object>> all = fileMapper.selectFilesWithOrgName();
        List<Map<String, Object>> filtered = all;
        if (l3OrgId != null) {
            filtered = filtered.stream().filter(m -> {
                Object v = m.get("L3_Org_ID");
                if (v == null) v = m.get("l3_org_id");
                if (v == null) return false;
                try {
                    return Integer.parseInt(String.valueOf(v)) == l3OrgId;
                } catch (Exception ex) {
                    return false;
                }
            }).collect(Collectors.toList());
        }
        if (q == null || q.trim().isEmpty()) return filtered;
        String key = q.trim().toLowerCase();
        return filtered.stream().filter(m -> {
            String name = safe(m, "Name", "name");
            String phone = safe(m, "Phone_Number", "phoneNumber", "phone_number");
            String archive = safe(m, "Archive_No", "archiveNo");
            String account = safe(m, "Username", "username", "account");
            String orgName = safe(m, "orgName", "ORGNAME");
            String combined = (name + " " + phone + " " + archive + " " + account + " " + orgName).toLowerCase();
            return combined.contains(key);
        }).collect(Collectors.toList());
    }

    private String safe(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            if (k == null) continue;
            Object v = m.get(k);
            if (v != null) return String.valueOf(v);
            v = m.get(k.toUpperCase());
            if (v != null) return String.valueOf(v);
            v = m.get(k.toLowerCase());
            if (v != null) return String.valueOf(v);
        }
        return "";
    }

    // [FIX] 核心创建方法，保持不变
    @Transactional
    public Map<String, Object> createPersonnelAuto(PersonnelFile file, Integer positionId) {
        if (file.getL3OrgId() == null) {
            throw new IllegalArgumentException("必须为新员工指定一个三级机构");
        }
        if (positionId == null) {
            throw new IllegalArgumentException("必须为新员工指定一个职位");
        }

        User user = new User();
        user.setUsername("temp_" + System.currentTimeMillis());
        user.setPasswordHash("123");
        user.setPositionId(positionId);
        user.setL3OrgId(file.getL3OrgId());
        userMapper.insert(user);

        int uid = user.getUserId();
        String dateStr = LocalDate.now().format(DATE_FMT);
        String account = dateStr + String.format("%04d", uid % 10000);

        user.setUsername(account);
        userMapper.updateById(user);

        file.setUserId(uid);
        file.setArchiveNo(account);
        file.setIsDeleted(0);
        fileMapper.insert(file);

        Map<String, Object> res = new HashMap<>();
        res.put("file", file);
        res.put("account", account);
        res.put("initPassword", "123");
        return res;
    }

    // [FIX] 添加 HRApiController 需要的 createPersonnelWithGivenAccount 方法
    @Transactional
    public Map<String, Object> createPersonnelWithGivenAccount(PersonnelFile file, String account, String initPassword, Integer positionId) {
        if (account == null || account.trim().isEmpty()) {
            return createPersonnelAuto(file, positionId);
        }
        if (positionId == null) {
            throw new IllegalArgumentException("必须为新员工指定一个职位");
        }

        String username = account.trim();
        User exist = userMapper.selectOne(new QueryWrapper<User>().eq("Username", username));
        if (exist != null) {
            throw new RuntimeException("账号 '" + username + "' 已存在，请使用其他账号。");
        }

        String initPass = (initPassword == null || initPassword.isEmpty()) ? "123" : initPassword;

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(initPass);
        user.setPositionId(positionId);
        user.setL3OrgId(file.getL3OrgId());
        userMapper.insert(user);

        file.setUserId(user.getUserId());
        file.setArchiveNo(username);
        file.setIsDeleted(0);
        fileMapper.insert(file);

        Map<String, Object> res = new HashMap<>();
        res.put("file", file);
        res.put("account", username);
        res.put("initPassword", initPass);
        return res;
    }

    // [FIX] 添加 HRApiController 需要的 bulkGenerate 方法
    @Transactional
    public List<Map<String, Object>> bulkGenerate(int count, Integer l3OrgId, Integer positionId) {
        if (positionId == null) {
            throw new IllegalArgumentException("批量生成员工时必须指定一个职位");
        }
        List<Map<String, Object>> created = new ArrayList<>();
        if (count <= 0) return created;
        for (int i = 0; i < count; i++) {
            PersonnelFile pf = new PersonnelFile();
            pf.setName("新员工-" + (i + 1));
            pf.setL3OrgId(l3OrgId);
            Map<String, Object> c = createPersonnelAuto(pf, positionId);
            created.add(c);
        }
        return created;
    }

    // 获取单个档案
    public Map<String, Object> getFileById(Integer id) {
        PersonnelFile pf = fileMapper.selectById(id);
        if (pf == null) return null;
        Map<String, Object> m = new HashMap<>();
        m.put("fileId", pf.getFileId());
        m.put("archiveNo", pf.getArchiveNo());
        m.put("userId", pf.getUserId());
        m.put("name", pf.getName());
        m.put("gender", pf.getGender());
        m.put("idNumber", pf.getIdNumber());
        m.put("phoneNumber", pf.getPhoneNumber());
        m.put("address", pf.getAddress());
        m.put("L3_Org_ID", pf.getL3OrgId());
        m.put("auditStatus", pf.getAuditStatus());
        m.put("creationTime", pf.getCreationTime());
        return m;
    }

    // 更新档案
    @Transactional
    public void updatePersonnel(Integer id, Map<String, Object> payload) {
        PersonnelFile pf = fileMapper.selectById(id);
        if (pf == null) throw new RuntimeException("档案不存在");
        if (payload.containsKey("name")) pf.setName((String) payload.get("name"));
        if (payload.containsKey("gender")) pf.setGender((String) payload.get("gender"));
        if (payload.containsKey("idNumber")) pf.setIdNumber((String) payload.get("idNumber"));
        if (payload.containsKey("phoneNumber")) pf.setPhoneNumber((String) payload.get("phoneNumber"));
        if (payload.containsKey("address")) pf.setAddress((String) payload.get("address"));
        fileMapper.updateById(pf);
    }

    // [FIX] 补全 HRApiController 需要的其他方法，防止后续报错
    @Transactional
    public boolean auditFile(Integer fileId, boolean pass, Integer auditorUserId) {
        PersonnelFile pf = fileMapper.selectById(fileId);
        if (pf == null) return false;
        pf.setAuditStatus(pass ? "Approved" : "Rejected");
        // 实际场景可能需要记录审核人ID
        fileMapper.updateById(pf);
        return true;
    }

    @Transactional
    public boolean deleteFile(Integer fileId, String reason) {
        PersonnelFile pf = fileMapper.selectById(fileId);
        if (pf == null) return false;
        pf.setIsDeleted(1);
        fileMapper.updateById(pf);

        // 同时逻辑删除关联的User账户
        if(pf.getUserId() != null){
            User user = userMapper.selectById(pf.getUserId());
            if(user != null){
                user.setIsDeleted(true); // 假设User表有Is_Deleted字段
                userMapper.updateById(user);
            }
        }
        return true;
    }

    public long countUsers() {
        return userMapper.selectCount(new QueryWrapper<User>().eq("Is_Deleted", 0));
    }
}
