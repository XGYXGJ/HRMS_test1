# HRMS 人力资源管理系统

一个基于 Spring Boot 3 + MyBatis Plus + Thymeleaf 的现代化人力资源管理系统，提供完整的员工、机构、职位及权限管理功能。

## 📋 项目概述

HRMS（Human Resource Management System）是一个企业级人力资源管理平台，旨在帮助企业高效管理员工信息、组织机构、职位配置等人力资源相关事务。

### 核心特性

- 🏢 **多层级机构管理** - 支持最多三层机构结构（集团/中心/部门）
- 👥 **员工信息管理** - 完整的员工档案、职位分配管理
- 🔐 **权限管理系统** - 基于角色的访问控制（Admin/HR/Salary/Employee）
- 💼 **职位管理** - 灵活的职位配置与权限等级设置
- 📊 **组织结构可视化** - 清晰展示企业组织架构

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | JDK 21 LTS |
| Spring Boot | 3.2.2 | 应用框架 |
| MyBatis Plus | 3.5.5 | ORM 框架 |
| MySQL | 8.0.33 | 数据库 |
| Thymeleaf | - | 模板引擎 |
| Lombok | - | 代码简化工具 |

## 📦 快速开始

### 前置要求

- Java 21+
- MySQL 8.0+
- Maven 3.6+

### 项目结构
**HRMS_test1 项目结构：**

- `src/` - 源代码目录
- `pom.xml` - Maven 项目配置
- `SQL2.sql` - 数据库表结构和初始数据（必须首先执行）
- `SQL.sql` - 测试数据脚本
- `SQL3.sql` - 额外测试数据脚本
- `test.txt` - 测试数据说明文档
- `测试账号.txt` - 测试账号信息
- `README.md` - 本文件

###  权限级别
-  Admin - 系统管理员权限
- Management - 管理层权限
- HR - 人力资源权限
- Salary - 薪酬管理权限
- Employee - 普通员工权限

### 安装步骤

1. **克隆仓库**
   ```bash
   git clone https://github.com/XGYXGJ/HRMS_test1.git
   cd HRMS_test1