-- 1. 添加薪酬项目
INSERT INTO T_Salary_Item (Item_Name, Item_Type) VALUES ('基本工资', 'Base');
INSERT INTO T_Salary_Item (Item_Name, Item_Type) VALUES ('交通补贴', 'Subsidy');
INSERT INTO T_Salary_Item (Item_Name, Item_Type) VALUES ('绩效基数', 'Bonus');

-- 2. 添加测试用户
-- Admin (ID=1)
INSERT INTO T_User (Username, Password_Hash, Position_ID, L3_Org_ID) VALUES ('admin', '123', 1, NULL);
-- HR Manager (ID=2)
INSERT INTO T_User (Username, Password_Hash, Position_ID, L3_Org_ID) VALUES ('hr01', '123', 2, 10); -- 假设 10 是某部门ID
-- Employee (ID=3)
INSERT INTO T_User (Username, Password_Hash, Position_ID, L3_Org_ID) VALUES ('emp01', '123', 3, 10);

-- 3. 确保 Position 表有数据
INSERT INTO T_Position (Position_ID, Position_Name, Auth_Level) VALUES (1, '管理员', 'Admin');
INSERT INTO T_Position (Position_ID, Position_Name, Auth_Level) VALUES (2, '人事经理', 'HR_Manager');
INSERT INTO T_Position (Position_ID, Position_Name, Auth_Level) VALUES (3, '软件工程师', 'Employee');

-- 3. 上面的是老东西
INSERT INTO T_Position (Position_ID, Position_Name, Auth_Level)
VALUES
    (1, '系统管理员', 'Admin'),        -- 对应原需求的管理员职位
    (2, '管理部门', 'Management'),     -- 新增管理部门职位
    (3, '人事经理', 'HR_Manager'),     -- 保留原人事经理职位
    (4, '薪酬经理', 'Salary_Manager'), -- 新增薪酬经理职位
    (5, '普通员工', 'Employee');