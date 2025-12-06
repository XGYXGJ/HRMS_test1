-- ============================================================
-- 1. 初始化机构数据 (必须先有机构才能分配人员)
-- ============================================================
-- 插入一级机构：集团总部
INSERT INTO T_Organization (Org_Name, Level, L1_Org_Name)
VALUES ('集团总部', 1, '集团总部');

-- 插入二级机构：研发中心 (Parent_Org_ID = 1)
INSERT INTO T_Organization (Org_Name, Level, Parent_Org_ID, L1_Org_Name, L2_Org_Name)
VALUES ('研发中心', 2, 1, '集团总部', '研发中心');

-- 插入三级机构：软件开发部 (Parent_Org_ID = 2) -> ID将为 3
INSERT INTO T_Organization (Org_Name, Level, Parent_Org_ID, L1_Org_Name, L2_Org_Name, L3_Org_Name)
VALUES ('软件开发部', 3, 2, '集团总部', '研发中心', '软件开发部');


-- ============================================================
-- 2. 初始化用户账号 (HR 和 员工)
-- ============================================================
-- 假设 '软件开发部' 的 ID 是 3

-- A. 添加人事经理账号 (Position_ID = 2)
-- 用户名: hr01, 密码: 123
INSERT INTO T_User (Username, Password_Hash, Position_ID, L3_Org_ID)
VALUES ('hr01', '123', 2, 3);

-- B. 添加普通员工账号 (Position_ID = 3)
-- 用户名: emp01, 密码: 123
INSERT INTO T_User (Username, Password_Hash, Position_ID, L3_Org_ID)
VALUES ('emp01', '123', 3, 3);


-- ============================================================
-- 3. 初始化对应的档案信息 (建议执行，否则查询档案会为空)
-- ============================================================
-- 假设 hr01 的 User_ID 是 2, emp01 的 User_ID 是 3

-- 为人事经理建立档案
INSERT INTO T_Personnel_File (User_ID, Name, Gender, Phone_Number, L3_Org_ID, Audit_Status)
VALUES (2, '李人事', '女', '13800138001', 3, 'Approved');

-- 为普通员工建立档案
INSERT INTO T_Personnel_File (User_ID, Name, Gender, Phone_Number, L3_Org_ID, Audit_Status)
VALUES (3, '张开发', '男', '13900139002', 3, 'Approved');

-- 3. 上面的是老东西
INSERT INTO T_Position (Position_ID, Position_Name, Auth_Level)
VALUES
    (1, '系统管理员', 'Admin'),        -- 对应原需求的管理员职位
    (2, '管理部门', 'Management'),     -- 新增管理部门职位
    (3, '人事经理', 'HR_Manager'),     -- 保留原人事经理职位
    (4, '薪酬经理', 'Salary_Manager'), -- 新增薪酬经理职位
    (5, '普通员工', 'Employee');