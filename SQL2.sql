-- ============================================================
-- 创建数据库
-- ============================================================
CREATE DATABASE IF NOT EXISTS hr_system
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hr_system;

-- ============================================================
-- 1. 机构表 (T_Organization)
-- ============================================================
DROP TABLE IF EXISTS T_Organization;

CREATE TABLE T_Organization (
                                Org_ID INT AUTO_INCREMENT PRIMARY KEY COMMENT '机构编号',
                                Org_Name VARCHAR(100) NOT NULL UNIQUE COMMENT '机构名称',
                                Level INT NOT NULL COMMENT '机构级别 (1,2,3)',
                                Parent_Org_ID INT DEFAULT NULL COMMENT '上级机构编号',
                                L1_Org_Name VARCHAR(100) COMMENT '一级机构名称',
                                L2_Org_Name VARCHAR(100) COMMENT '二级机构名称',
                                L3_Org_Name VARCHAR(100) COMMENT '三级机构名称（自身）',
                                FOREIGN KEY (Parent_Org_ID) REFERENCES T_Organization(Org_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- 2. 职位表 (T_Position)
-- ============================================================
CREATE TABLE T_Position (
                            Position_ID INT AUTO_INCREMENT PRIMARY KEY COMMENT '职位编号',
                            Position_Name VARCHAR(50) NOT NULL COMMENT '职位名称',
                            Auth_Level ENUM('Admin','Management','HR_Manager','Salary_Manager','Employee') NOT NULL COMMENT '权限级别'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 更新管理员账户的 Position_ID
UPDATE T_User SET Position_ID = 1 WHERE Username = 'admin';


-- 如果需要保留原数据中的「软件工程师」，额外插入（不冲突）
INSERT INTO T_Position (Position_ID, Position_Name, Auth_Level)
VALUES (6, '软件工程师', 'Employee');

-- ============================================================
-- 3. 用户表 (T_User)
-- ============================================================
DROP TABLE IF EXISTS T_User;

CREATE TABLE T_User (
                        User_ID INT AUTO_INCREMENT PRIMARY KEY COMMENT '系统用户编号',
                        Username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
                        Password_Hash VARCHAR(255) NOT NULL COMMENT '密码（开发可存明文）',
                        Position_ID INT COMMENT '职位编号',
                        L3_Org_ID INT COMMENT '所属第三级机构',
                        Is_Deleted BOOLEAN NOT NULL DEFAULT 0 COMMENT '删除状态',
                        Deletion_Reason TEXT COMMENT '删除原因',
                        Deletion_Time DATETIME COMMENT '删除时间',
                        FOREIGN KEY (Position_ID) REFERENCES T_Position(Position_ID),
                        FOREIGN KEY (L3_Org_ID) REFERENCES T_Organization(Org_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- 4. 人事档案表 (T_Personnel_File)
-- ============================================================
DROP TABLE IF EXISTS T_Personnel_File;

CREATE TABLE T_Personnel_File (
                                  File_ID INT AUTO_INCREMENT PRIMARY KEY COMMENT '档案编号',
                                  User_ID INT UNIQUE COMMENT '关联用户编号',
                                  Name VARCHAR(50) NOT NULL COMMENT '姓名',
                                  Gender VARCHAR(10) COMMENT '性别',
                                  ID_Number VARCHAR(20) UNIQUE COMMENT '身份证号',
                                  Phone_Number VARCHAR(20) COMMENT '手机号',
                                  Address VARCHAR(255) COMMENT '住址',
                                  L3_Org_ID INT COMMENT '所属机构',
                                  Creation_Time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建档时间',
                                  HR_Submitter_ID INT COMMENT '提交档案HR',
                                  Admin_Auditor_ID INT COMMENT '审核管理员',
                                  Audit_Status ENUM('Pending','Approved','Rejected') NOT NULL DEFAULT 'Pending' COMMENT '审核状态',
                                  Audit_Time DATETIME COMMENT '审核时间',
                                  Is_Deleted BOOLEAN NOT NULL DEFAULT 0 COMMENT '删除状态',
                                  FOREIGN KEY (User_ID) REFERENCES T_User(User_ID),
                                  FOREIGN KEY (L3_Org_ID) REFERENCES T_Organization(Org_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- 5. 薪酬项目表 (T_Salary_Item)
-- ============================================================
DROP TABLE IF EXISTS T_Salary_Item;

CREATE TABLE T_Salary_Item (
                               Item_ID INT AUTO_INCREMENT PRIMARY KEY,
                               Item_Name VARCHAR(50) NOT NULL UNIQUE COMMENT '项目名称',
                               Item_Type ENUM('Base','Subsidy','Bonus','KPI','Penalty') NOT NULL COMMENT '项目类型',
                               Is_Active BOOLEAN DEFAULT 1 COMMENT '是否启用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 仅当 T_Salary_Item.Item_Type 是 ENUM 类型时执行
ALTER TABLE T_Salary_Item
    MODIFY COLUMN Item_Type ENUM('Base', 'Subsidy', 'Bonus', 'Penalty', 'KPI', 'Ratio') NOT NULL;
-- 将所有 'KPI' 类型（如果您的本意是系数）更新为新的 'Ratio' 类型
UPDATE T_Salary_Item SET Item_Type = 'Ratio' WHERE Item_Type = 'KPI';


-- ============================================================
-- 6. 薪酬标准主表 (T_Salary_Standard_Master)
-- ============================================================
DROP TABLE IF EXISTS T_Salary_Standard_Master;

CREATE TABLE T_Salary_Standard_Master (
                                          Standard_ID INT AUTO_INCREMENT PRIMARY KEY,
                                          Standard_Name VARCHAR(100) NOT NULL COMMENT '标准名称',
                                          L3_Org_ID INT COMMENT '适用机构',
                                          Position_ID INT COMMENT '适用职位',
                                          Submitter_ID INT COMMENT '提交人（HR）',
                                          Submission_Time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
                                          Audit_Status ENUM('Pending','Approved','Rejected') NOT NULL DEFAULT 'Pending',
                                          Auditor_ID INT COMMENT '审核人（Admin）',
                                          Audit_Time DATETIME COMMENT '审核时间',
                                          FOREIGN KEY (L3_Org_ID) REFERENCES T_Organization(Org_ID),
                                          FOREIGN KEY (Position_ID) REFERENCES T_Position(Position_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 修改薪酬标准主表，增加标准编码字段
ALTER TABLE T_Salary_Standard_Master
    ADD COLUMN Standard_Code VARCHAR(50) NOT NULL COMMENT '标准编号(FAT+日期+序号)' AFTER Standard_ID;

-- 确保编号唯一（可选）
ALTER TABLE T_Salary_Standard_Master
    ADD CONSTRAINT UK_Standard_Code UNIQUE (Standard_Code);

ALTER TABLE T_Salary_Register_Master
    ADD COLUMN Register_Code VARCHAR(30) UNIQUE COMMENT '薪资发放单号 (格式: PAYYYYYMMDDNN)';
-- ============================================================
-- 7. 薪酬标准详情表 (T_Salary_Standard_Detail)
-- ============================================================
DROP TABLE IF EXISTS T_Salary_Standard_Detail;

CREATE TABLE T_Salary_Standard_Detail (
                                          Detail_ID INT AUTO_INCREMENT PRIMARY KEY,
                                          Standard_ID INT NOT NULL,
                                          Item_ID INT NOT NULL,
                                          Value DECIMAL(10,2) NOT NULL COMMENT '项目金额/比例',
                                          FOREIGN KEY (Standard_ID) REFERENCES T_Salary_Standard_Master(Standard_ID) ON DELETE CASCADE,
                                          FOREIGN KEY (Item_ID) REFERENCES T_Salary_Item(Item_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- 8. 薪酬发放主表 (T_Salary_Register_Master)
-- ============================================================
DROP TABLE IF EXISTS T_Salary_Register_Master;

CREATE TABLE T_Salary_Register_Master (
                                          Register_ID INT AUTO_INCREMENT PRIMARY KEY,
                                          L3_Org_ID INT NOT NULL COMMENT '机构ID',
                                          Register_Time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
                                          Submitter_ID INT COMMENT 'HR提交人',
                                          Total_People INT COMMENT '总人数',
                                          Total_Amount DECIMAL(12,2) COMMENT '总金额',
                                          Audit_Status ENUM('Pending','Approved','Rejected') NOT NULL DEFAULT 'Pending' COMMENT '审核状态',
                                          Auditor_ID INT COMMENT '审核管理员',
                                          Audit_Time DATETIME,
                                          Pay_Date DATE NOT NULL COMMENT '薪酬月份',
                                          FOREIGN KEY (L3_Org_ID) REFERENCES T_Organization(Org_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- 9. 薪酬发放详情表 (T_Salary_Register_Detail)
-- ============================================================
DROP TABLE IF EXISTS T_Salary_Register_Detail;

CREATE TABLE T_Salary_Register_Detail (
                                          Detail_ID INT AUTO_INCREMENT PRIMARY KEY,
                                          Register_ID INT NOT NULL COMMENT '主表ID',
                                          User_ID INT NOT NULL COMMENT '员工ID',
                                          Standard_ID_Used INT COMMENT '使用的工资标准ID',
                                          KPI_Units DECIMAL(5,2) DEFAULT 0 COMMENT 'KPI数量',
                                          Attendance_Count INT DEFAULT 0 COMMENT '出勤天数',
                                          Overtime_Hours DECIMAL(5,2) DEFAULT 0 COMMENT '加班时长',

                                          Base_Salary DECIMAL(10,2),
                                          Total_Subsidy DECIMAL(10,2),
                                          KPI_Bonus DECIMAL(10,2),
                                          Attendance_Adjustment DECIMAL(10,2),
                                          Overtime_Pay DECIMAL(10,2),
                                          Gross_Money DECIMAL(10,2) NOT NULL COMMENT '实际发放工资',

                                          Payroll_Month DATE NOT NULL COMMENT '薪资月份',

                                          FOREIGN KEY (Register_ID) REFERENCES T_Salary_Register_Master(Register_ID) ON DELETE CASCADE,
                                          FOREIGN KEY (User_ID) REFERENCES T_User(User_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- 初始化基础数据
-- ============================================================

-- 职位
INSERT INTO T_Position (Position_Name, Auth_Level) VALUES
                                                       ('管理员', 'Admin'),
                                                       ('人事经理', 'HR_Manager'),
                                                       ('普通员工', 'Employee');

-- 管理员账户
INSERT INTO T_User (Username, Password_Hash, Position_ID) VALUES
    ('admin', '123', 1);

-- 基础薪酬项目
INSERT INTO T_Salary_Item (Item_Name, Item_Type) VALUES
                                                     ('基本工资', 'Base'),
                                                     ('交通补贴', 'Subsidy'),
                                                     ('全勤奖', 'Bonus'),
                                                     ('KPI 单价', 'KPI'),
                                                     ('旷工扣除', 'Penalty');
