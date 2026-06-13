# 测试用例分析报告 (TCA - Test Case Analysis)

> 生成日期：2026-06-13  
> 项目：HRM 人力资源管理系统  
> 分支：UT  

---

## 目录

1. [测试概览](#1-测试概览)
2. [模块一：登录认证](#2-模块一登录认证)
3. [模块二：员工管理](#3-模块二员工管理)
4. [模块三：部门管理](#4-模块三部门管理)
5. [模块四：菜单管理](#5-模块四菜单管理)
6. [模块五：角色管理](#6-模块五角色管理)
7. [模块六：文档管理](#7-模块六文档管理)
8. [模块七：考勤记录](#8-模块七考勤记录)
9. [模块八：请假申请](#9-模块八请假申请)
10. [模块九：加班记录](#10-模块九加班记录)
11. [模块十：城市社保标准](#11-模块十城市社保标准)
12. [模块十一：社保公积金](#12-模块十一社保公积金)
13. [模块十二：薪资管理](#12-模块十二薪资管理)
14. [模块十三：全局异常处理](#13-模块十三全局异常处理)
15. [测试基础设施](#14-测试基础设施)

---

## 核心测试用例索引

> 以下为报告中应优先展示的核心测试（~70 个），按模块和重要程度排列。

| 优先级 | 模块 | 核心测试 | 数量 | 关键价值 |
|--------|------|---------|------|---------|
| 🔴 P0 | 全局异常 | BaseExceptionHandlerWhiteBoxTest | 6 | 6个 @ExceptionHandler 映射验证 |
| 🔴 P0 | 登录认证 | TC-LOGIN-001~006,008~009,015 | 8 | 认证流程 + SQL注入防护 |
| 🔴 P0 | 文档管理 | TC-DOCS-001~004,007,029~030 + WB | 12 | 文件上传安全 + 路径遍历防护 |
| 🟡 P1 | 员工管理 | StaffServiceWhiteBoxTest | 3 | NPE风险 + 硬编码暴露 |
| 🟡 P1 | 请假申请 | StaffLeaveServiceWhiteBoxTest | 6 | 工作流静默失败 + 数据不一致 |
| 🟡 P1 | 加班记录 | StaffOvertimeServiceWhiteBoxTest | 8 | 加班费计算引擎8路径 |
| 🟡 P1 | 考勤记录 | AttendanceServiceWhiteBoxTest + 判定测试 | 7+4 | isLate/isLeaveEarly/isAbsenteeism 多条件 |
| 🟡 P1 | 部门管理 | DeptServiceWhiteBoxTest | 4 | 时间字段完整性校验 |
| 🟡 P1 | 文档管理 | DocsServiceWhiteBoxTest | 7 | 上传校验链 + MD5 + 下载安全 |
| 🟢 P2 | 菜单管理 | MenuServiceTest (树结构) | 3 | 三层树构建 + 分页树 |
| 🟢 P2 | 考勤记录 | AttendanceServiceTest (list/export) | 2 | 月考勤日历 + 报表导出 |
| 🟢 P2 | 首页聚合 | HomeServiceTest | 5 | 季度统计 + 部门人数 + 考勤日历 |
| 🟢 P2 | 薪资管理 | SalaryCalculationTest + ExportTest | 5 | 扣款计算 + 报表导出 |
| 🟢 P2 | 员工管理 | StaffServiceTest (代表CRUD) | 5 | add/delete/edit/query/list |
| 🟢 P2 | 角色管理 | RoleServiceTest (代表CRUD模式) | 3 | CRUD 双分支 + setMenu |
| 🟢 P2 | 社保公积金 | InsuranceServiceTest (list分支) | 2 | 部门/无部门分叉 |
| 🔵 P3 | 各Controller | 边界值参数化测试 | 5 | Insurance/Menu/Dept 等 @CsvSource |
| 🔵 P3 | 各Controller | 正常场景 + 权限 + 认证 | 15 | 每个模块 1-2 个代表 |

> 其余 ~460 个测试可通过摘要表格列出，无需逐方法展开。

---

## 1. 测试概览

### 1.1 测试文件分布（精简后）

| 层次 | 文件数 | 说明 |
|------|--------|------|
| Controller 层 | 13 | REST API 接口集成测试 |
| Service 层 | 16 | 单元测试 + 白盒测试 |
| Mapper 层 | 1 | 数据访问层测试 |
| Exception | 1 | 全局异常处理器白盒测试 |
| Config | 3 | 测试配置/工具类 |
| **总计** | **34** | (已删除 3 个同构 Service 测试文件) |

### 1.2 测试类型分布

| 类型 | 方法级计数 | 说明 |
|------|-----------|------|
| 集成测试 (Controller) | ~280 | SpringBootTest + MockMvc |
| 单元测试 (Service) | ~200 | Mockito + spy |
| 单元测试 (Mapper) | ~25 | SpringBootTest + 真实DB |
| 白盒测试 | ~35 | 反射/私有方法覆盖 |
| **@Test + @ParameterizedTest** | **536** | 总量 |

---

## 2. 模块一：登录认证

### 2.1 测试文件

| 文件 | 路径 | 类型 |
|------|------|------|
| LoginControllerTest.java | `controller/` | Controller 集成测试 |

### 2.2 测试用例清单

| 编号 | 用例名称 | 测试场景 | 预期结果 |
|------|---------|---------|---------|
| TC-LOGIN-001 | 正常登录 | 正确的工号+密码+验证码 | 200, 返回 token |
| TC-LOGIN-002 | 用户名为空 | code=null | 400 |
| TC-LOGIN-003 | 用户名为空字符串 | code="" | 400 |
| TC-LOGIN-004 | 密码为空 | password=null | 401 |
| TC-LOGIN-005 | 密码为空字符串 | password="" | 401 |
| TC-LOGIN-006 | 验证码为空 | validateCode=" "（空格） | 200 + code=300 |
| TC-LOGIN-007 | 用户名不存在 | code="nonexistent_user" | 400 |
| TC-LOGIN-008 | 密码错误 | password="wrong_pwd" | 401 |
| TC-LOGIN-009 | 验证码错误 | validateCode="XXXX" | 200 + code=300 |
| TC-LOGIN-010 | 全部为空 | code=null, password=null | 200 + code=300 |
| TC-LOGIN-011 | 工号最小长度 | code="a" (1字符) | 400 |
| TC-LOGIN-012 | 工号最大长度 | code=50字符 | 400 |
| TC-LOGIN-013 | 工号超长 | code=51字符 | 400 |
| TC-LOGIN-014 | 密码最小长度 | password="1" (1字符) | 401 |
| TC-LOGIN-015 | 工号含SQL注入 | code="admin' OR '1'='1" | 400 |

---

## 3. 模块二：员工管理

### 3.1 测试文件

| 文件 | 路径 | 类型 | 说明 |
|------|------|------|------|
| StaffControllerTest.java | `controller/` | Controller 集成测试 | REST API 接口测试 |
| StaffMapperTest.java | `mapper/` | Mapper 单元测试 | 数据库操作测试 |
| StaffServiceTest.java | `service/` | Service 集成测试 | 增删改查核心功能 |
| StaffServiceWhiteBoxTest.java | `service/` | Service 白盒测试 | NPE风险/硬编码行为 |

### 3.2 Controller 层测试用例 (StaffControllerTest)

| 编号 | 用例名称 | 测试场景 | 预期结果 |
|------|---------|---------|---------|
| - | 新增员工 - 成功场景 | 完整合法数据 | 200 |
| - | 新增员工 - 缺少权限 | authorities为空 | 403 |
| - | 新增员工 - 未认证 | 无认证信息 | 401 |
| - | 删除员工 - 成功场景 | 创建后删除 | 200 |
| - | 删除员工 - 缺少权限 | authorities为空 | 403 |
| - | 批量删除 - 成功场景 | 合法ID列表 | 200 |
| - | 编辑员工 - 成功场景 | 修改名称/电话/地址 | 200 |
| - | 编辑员工 - 缺少权限 | authorities为空 | 403 |
| - | 根据ID查询 - 成功 | 存在的ID | 200 |
| - | 根据ID查询 - 不存在 | id=999999 | 200 + code=300 |
| - | 查询详细信息 | 存在的ID | 200 |
| - | 多条件分页 - 无条件 | 默认分页 | 200 |
| - | 多条件分页 - 按姓名 | name="张" | 200 |
| - | 多条件分页 - 按部门 | deptId=1 | 200 |
| - | 多条件分页 - 按状态 | status=1 | 200 |
| - | 多条件分页 - 组合条件 | name+deptId+status | 200 |
| - | 多条件分页 - 缺少权限 | authorities为空 | 403 |
| - | 分页默认值 | 不提供分页参数 | 200 |
| - | 自定义分页 | current=2, size=5 | 200 |
| - | 验证密码 - 正确 | password="123" | 200 |
| - | 验证密码 - 错误 | password="wrong_password" | 200 + code=300 |
| - | 重置密码 - 成功 | 新的密码 | 200 |
| - | 设置角色 - 成功 | roleIds=[1,2] | 200 |
| - | 查询员工角色 | 存在的员工ID | 200 |
| - | 无效JSON | JSON解析失败 | 400 |
| - | 无效员工ID格式 | id="abc" | 400 |
| - | 无效页码 | current=-1 | 200 |
| - | 无效页面大小 | size=0 | 200 |

### 3.3 Mapper 层测试用例 (StaffMapperTest)

| 编号 | 用例名称 | 测试场景 |
|------|---------|---------|
| - | insert - 插入员工 | 基本插入操作 |
| - | selectById - 存在ID | 查询已存在员工 |
| - | selectById - 不存在ID | id=999999 |
| - | updateById - 更新员工 | 修改名称和电话 |
| - | deleteById - 逻辑删除 | @TableLogic 测试 |
| - | selectList - 查询所有未删除 | 验证deleted flag |
| - | listStaffAttendanceVO - 模糊查询 | 姓名模糊匹配 |
| - | listStaffAttendanceVO - 无匹配 | 不存在的名字 |
| - | listStaffDeptAttendanceVO - 按部门+姓名 | 组合查询 |
| - | queryAttendanceMonthVO | 月考勤报表 |
| - | queryByCode - 存在工号 | 按工号查询 |
| - | queryByCode - 不存在工号 | 返回null |
| - | queryInfo - 详细信息 | JOIN查询 |
| - | queryInfo - 完整字段 | 验证所有字段 |
| - | queryStaffDeptVO - 所有员工 | 部门视图查询 |
| - | listStaffOvertimeVO - 按姓名 | 加班员工查询 |
| - | listStaffDeptOvertimeVO - 按部门+姓名 | 组合查询 |
| - | queryOvertimeMonthVO | 月加班报表 |
| - | queryByRole - 按角色代码 | 角色关联查询 |
| - | 空字符串姓名查询 | name="" |
| - | null姓名查询 | name=null |
| - | 分页第一页 | current=1, size=5 |
| - | 分页第二页 | current=2, size=5 |
| - | 特殊字符姓名 | "欧阳·测试" |
| - | 中文模糊搜索 | LIKE "%测试%" |
| - | JOIN验证部门名称 | 部门名称非空 |
| - | LEFT JOIN - 无部门 | deptId=null |
| - | 批量查询性能 | 50条数据 < 1秒 |

### 3.4 Service 层测试用例 (StaffServiceTest)

| 编号 | 用例名称 | 测试场景 |
|------|---------|---------|
| - | 新增员工 - 成功 | 完整员工信息 |
| - | 新增员工 - 必填字段为空 | name=null |
| - | 默认密码设置 | 验证密码为"123" |
| - | 工号自动生成 | 格式："staff_{id}" |
| - | 逻辑删除 - 成功 | 删除后查询不到 |
| - | 逻辑删除 - 不存在ID | id=999999 |
| - | 批量删除 - 成功 | 两个员工同时删除 |
| - | 批量删除 - 空列表 | 空ID列表 |
| - | 编辑 - 成功 | 修改名称/电话/地址 |
| - | 编辑 - 更新状态为禁用 | status=2 |
| - | 编辑 - 更新部门 | deptId=2 |
| - | 编辑 - 不存在的员工 | id=999999 |
| - | 根据ID查询 - 成功 | 存在员工 |
| - | 根据ID查询 - 不存在 | id=999999 |
| - | 多条件分页 - 无条件 | 验证分页结构 |
| - | 多条件分页 - 按姓名 | LIKE "%张%" |
| - | 多条件分页 - 按部门 | deptId过滤 |
| - | 多条件分页 - 按状态 | status过滤 |
| - | 多条件分页 - 组合条件 | name+deptId+status |
| - | 分页功能 | 15条数据验证2页 |
| - | 查询详细信息 | StaffDeptVO |
| - | 年龄计算 | DateUtil.ageOfNow |
| - | 手机号重复 | 相同手机号 |
| - | 性别枚举值 | MALE→FEMALE |
| - | 特殊字符姓名 | "欧阳·测试" |
| - | 空结果集 | 不存在名字 |

### 3.5 Service 白盒测试 (StaffServiceWhiteBoxTest)

| 编号 | 用例名称 | 暴漏问题 |
|------|---------|---------|
| TC-STAFF-WB-001 | list() — deptId指向不存在部门 → NPE | dept.getName() 未做 null 防护 |
| TC-STAFF-WB-002 | validate() — 员工不存在 → NPE | staff.getPassword() 未做 null 防护 |
| TC-STAFF-WB-003 | imp() — 导入员工deptId强制为13 | 硬编码覆盖Excel原始值 |

---

## 4. 模块三：部门管理

### 4.1 测试文件

| 文件 | 路径 | 类型 |
|------|------|------|
| DeptControllerTest.java | `controller/` | Controller 集成测试 |
| DeptServiceWhiteBoxTest.java | `service/` | Service 白盒测试 |

### 4.2 Controller 层测试用例 (DeptControllerTest)

| 编号 | 用例名称 | 测试场景 | 预期结果 |
|------|---------|---------|---------|
| TC-DEPT-001 | 新增根部门 - 成功 | parentId=0 | 200 |
| TC-DEPT-002 | 新增子部门 - 成功 | parentId=已知部门 | 200 |
| TC-DEPT-003 | 新增部门 - 名称为空 | name=null | 200 + code=300 |
| TC-DEPT-004 | 新增部门 - 工作时间不合理 | 结束<开始 | 200 + code=300 |
| TC-DEPT-005 | 删除部门 - 成功 | 创建后删除 | 200 |
| TC-DEPT-006 | 删除部门 - 有子部门 | 父部门有子部门 | 200 + code=300 |
| TC-DEPT-007 | 更新部门 - 修改名称 | 修改名称 | 200 |
| TC-DEPT-009 | 查询所有部门(树形) | GET /dept/all | 200 |
| TC-DEPT-010 | 部门列表 - 按名称搜索 | name="技术" | 200 |
| TC-DEPT-013 | 名称边界值(1字符) | name="技" | 200 |
| TC-DEPT-014 | 名称边界值(16字符) | 16字符 | 200 |
| TC-DEPT-015 | 名称超长(31字符) | 超出数据库限制 | 200 + code=300 |
| TC-DEPT-016 | 工作时间边界值(0小时) | 开始=结束 | 200 + code=300 |
| TC-DEPT-017 | 工作时间边界值(12小时) | 全天12小时 | 200 |
| TC-DEPT-018 | 工作时间超限(13小时) | 超出合理范围 | 200 + code=300 |
| TC-DEPT-019 | 时间格式错误 | 缺少其他时间字段 | 200 + code=300 |
| TC-DEPT-020 | 下午时间早于上午 | aftStartTime<morEndTime | 200 + code=300 |
| TC-DEPT-022 | 删除 - ID为0 | id=0 | 200 + code=300 |
| TC-DEPT-023 | 删除 - ID为负数 | id=-1 | 200 + code=300 |
| TC-DEPT-024 | 批量删除 - 空列表 | 空路径变量 | 400 |

### 4.3 Service 白盒测试 (DeptServiceWhiteBoxTest)

| 编号 | 用例名称 | 测试场景 |
|------|---------|---------|
| TC-DEPT-WB-001 | add() — 部分时间字段 → ERROR | 四种缺字段场景 |
| TC-DEPT-WB-002 | delete() — id=null → ERROR | null防护 |
| TC-DEPT-WB-003 | query() — id=null → IllegalArgumentException | Service层空值防护 |
| TC-DEPT-WB-004 | query() — id≤0 → IllegalArgumentException | 非正数ID防护 |

---

## 5. 模块四：菜单管理

### 5.1 测试文件

| 文件 | 路径 | 类型 |
|------|------|------|
| MenuControllerTest.java | `controller/` | Controller 集成测试 |

### 5.2 测试用例清单

| 编号 | 用例名称 | 测试场景 | 预期结果 |
|------|---------|---------|---------|
| TC-MENU-001 | 新增一级菜单 | level=0 | 200 |
| TC-MENU-002 | 新增二级页面 | level=1 | 200 |
| TC-MENU-003 | 新增权限点 | level=2 + permission | 200 |
| TC-MENU-004 | 菜单名称为null | name=null | 200 (name可空) |
| TC-MENU-005 | 菜单名称为空字符串 | name="" | 200 |
| TC-MENU-006 | 编码为null | code=null | 200 |
| TC-MENU-007 | 编码重复 | 相同code | 200 |
| TC-MENU-008 | 名称最小长度(1字符) | name="A" | 200 |
| TC-MENU-009 | 名称最大长度(20字符) | 20中文字符 | 200 |
| TC-MENU-010 | 名称超长(21字符) | 超出数据库限制 | 200 + code=300 |
| TC-MENU-011 | 编码最小长度(1字符) | code="a" | 200 |
| TC-MENU-012 | 编码最大长度(20字符) | 20字符 | 200 |
| TC-MENU-013 | 编码超长(21字符) | 超出数据库限制 | 200 + code=300 |
| TC-MENU-014 | permission超长(201字符) | 超出字段长度 | 200 + code=300 |
| TC-MENU-015 | level边界值0 | 一级菜单下边界 | 200 |
| TC-MENU-016 | level边界值2 | 权限点上边界 | 200 |
| TC-MENU-017 | parentId=0(根菜单) | 根菜单 | 200 |
| TC-MENU-018 | 名称含特殊字符 | "<script>" | 200 (无验证) |
| TC-MENU-019 | 正常编辑菜单 | 修改名称 | 200 |
| TC-MENU-020 | 编辑时名称为空 | name="" | 200 |
| TC-MENU-021 | 正常删除菜单 | 逻辑删除 | 200 |
| TC-MENU-022 | 正常批量删除 | 两个菜单 | 200 |
| TC-MENU-023 | 批量删除空列表 | 空路径变量 | 400 |

---

## 6. 模块五：角色管理

### 6.1 测试文件

| 文件 | 路径 | 类型 |
|------|------|------|
| RoleControllerTest.java | `controller/` | Controller 集成测试 |

### 6.2 测试用例清单

| 编号 | 用例名称 | 测试场景 | 预期结果 |
|------|---------|---------|---------|
| TC-ROLE-001 | 正常新增角色 | 有效角色名 | 200 |
| TC-ROLE-002 | 无权限访问 | authorities为空 | 403 |
| TC-ROLE-003 | 角色名最小长度(1字符) | name="A" | 200 |
| TC-ROLE-004 | 角色名边界长度(20字符) | 20中文字符 | 200 |
| TC-ROLE-005 | 角色名超长(21字符) | 超出数据库限制 | 200 + code=300 |
| TC-ROLE-006 | 备注边界长度(200字符) | 200字符备注 | 200 |
| TC-ROLE-007 | 备注超长(201字符) | 超出数据库限制 | 200 + code=300 |
| TC-ROLE-008 | 角色名含特殊字符 | "测试<script>" | 200 (无验证) |
| TC-ROLE-009 | 正常编辑角色 | 修改名称 | 200 |
| TC-ROLE-010 | 编辑时名称为空 | name="" | 200 |
| TC-ROLE-011 | 正常批量删除 | 两个角色 | 200 |
| TC-ROLE-012 | 批量删除空列表 | 空路径变量 | 400 |
| TC-ROLE-013 | 正常分配菜单 | menuIds=[1,2,3] | 200 |
| TC-ROLE-014 | 分配空菜单列表 | menuIds=[] | 200 |

---

## 7. 模块六：文档管理

### 7.1 测试文件

| 文件 | 路径 | 类型 |
|------|------|------|
| DocsControllerTest.java | `controller/` | Controller 集成测试 |
| DocsControllerWhiteBoxTest.java | `controller/` | Controller 白盒测试 |
| DocsServiceWhiteBoxTest.java | `service/` | Service 白盒测试 |

### 7.2 Controller 层测试用例 (DocsControllerTest)

| 编号 | 用例名称 | 测试场景 | 预期结果 |
|------|---------|---------|---------|
| TC-DOCS-001 | 文件上传 - 成功 | 正常jpg文件 | 200 |
| TC-DOCS-002 | 文件上传 - 格式不支持 | .exe文件 | 200 + code=300 |
| TC-DOCS-003 | 文件上传 - 文件过大 | 21MB | 200 + code=300 |
| TC-DOCS-004 | 文件上传 - 空文件 | 0字节 | 200 + code=300 |
| TC-DOCS-005 | 文件下载 - 成功 | 上传后下载 | 200 |
| TC-DOCS-006 | 文件下载 - 不存在 | 不存在的文件 | 404 |
| TC-DOCS-007 | 文件下载 - 路径遍历 | "../../etc/passwd" | 400 |
| TC-DOCS-008 | 文档新增 - 成功 | 有效文档信息 | 200 |
| TC-DOCS-009 | 文档删除 - 成功 | 存在的ID | 200 |
| TC-DOCS-010 | 文档列表 - 按原文件名搜索 | oldName="报告" | 200 |
| TC-DOCS-011 | 文档列表 - 按员工姓名搜索 | staffName="张三" | 200 |
| TC-DOCS-012 | 头像下载(无需权限) | 无认证 | 404 |
| TC-DOCS-013 | 文件名边界值(1字符) | "a.jpg" | 200 |
| TC-DOCS-014 | 文件名边界值(100字符) | 100字符 | 200 |
| TC-DOCS-015 | 文件名超长(101字符) | 超出限制 | 200 + code=300 |
| TC-DOCS-016 | 文件大小边界值(19.9MB) | 接近20MB | 200 |
| TC-DOCS-019 | 文件大小1字节 | 最小文件 | 200 |
| TC-DOCS-020 | 特殊字符文件名 | "test@#$%^&().pdf" | 200 |
| TC-DOCS-021 | 中文文件名 | "测试文档.pdf" | 200 |
| TC-DOCS-022 | 无扩展名 | "no_extension" | 200 + code=300 |
| TC-DOCS-023 | 多个扩展名 | "test.jpg.exe" | 200 + code=300 |
| TC-DOCS-024 | ID不存在 | id=999999 | 200 + code=300 |
| TC-DOCS-025 | ID为负数 | id=-1 | 200 + code=300 |
| TC-DOCS-026 | 空文件名下载 | /docs/download | 200 (路由转发) |
| TC-DOCS-027 | 文件名含空格 | "test%20file.pdf" | 400 |
| TC-DOCS-028 | 文件名含中文 | URL编码中文 | 400 |
| TC-DOCS-029 | 路径遍历(../) | "../../../etc/passwd" | 400 |
| TC-DOCS-030 | 绝对路径攻击 | "C:/Windows/..." | 404 |
| TC-DOCS-031 | 原文件名边界值(1字符) | "a.pdf" | 200 |
| TC-DOCS-034 | 新文件名重复 | 同名文件 | 200 + code=300 |
| TC-DOCS-035 | 搜索关键词为空 | oldName="" | 200 |
| TC-DOCS-036 | SQL注入关键词 | "' OR '1'='1" | 200 |

### 7.3 Controller 白盒测试 (DocsControllerWhiteBoxTest)

| 编号 | 用例名称 | 测试场景 | 预期结果 |
|------|---------|---------|---------|
| TC-DOCS-WB-001 | download() — ServiceException → 404 | FILE_NOT_EXIST | 404 |
| TC-DOCS-WB-002 | download() — IllegalArgumentException → 400 | 文件名不能为空 | 400 |
| TC-DOCS-WB-003 | download() — 两个catch分支独立 | 独立验证 | 404 / 400 |
| TC-DOCS-WB-004 | getAvatar() — ServiceException → 404 | FILE_NOT_EXIST | 404 |
| TC-DOCS-WB-005 | getAvatar() — IllegalArgumentException → 400 | 非法参数 | 400 |
| TC-DOCS-WB-006 | getAvatar() — 无认证走404 | 公开接口验证 | 404 |

### 7.4 Service 白盒测试 (DocsServiceWhiteBoxTest)

| 编号 | 用例名称 | 测试场景 |
|------|---------|---------|
| TC-DOCS-WB-001 | upload() — 不允许的扩展名 | .exe/.bat/.sh/.js/.py |
| TC-DOCS-WB-002 | upload() — MD5重复跳过磁盘写入 | 复用已有文件UUID |
| TC-DOCS-WB-003 | upload() — transferTo失败 | 磁盘写入失败→ServiceException |
| TC-DOCS-WB-004 | upload() — 刚好20MB | 边界值通过 |
| TC-DOCS-WB-005 | upload() — 文件名100字符 | 边界值通过 |
| TC-DOCS-WB-006 | download() — 反斜杠路径遍历 | Windows路径攻击 |
| TC-DOCS-WB-007 | download() — ".."先于"/"被捕获 | if分支顺序验证 |

---

## 8. 模块七：考勤记录

### 8.1 测试文件

| 文件 | 路径 | 类型 |
|------|------|------|
| AttendanceControllerTest.java | `controller/` | Controller 集成测试 |
| AttendanceServiceWhiteBoxTest.java | `service/` | Service 白盒测试 |

### 8.2 Controller 层测试用例 (AttendanceControllerTest)

| 编号 | 用例名称 | 测试场景 | 预期结果 |
|------|---------|---------|---------|
| TC-ATT-001 | 新增考勤 - 正常 | 所有字段合法 | 200 |
| TC-ATT-002 | 新增考勤 - 员工ID为空 | staffId=null | 200 + code=300 |
| TC-ATT-003 | 新增考勤 - 员工ID不存在 | staffId=999999 | 200 + code=300 |
| TC-ATT-004 | 新增考勤 - 日期为空 | date=null | 200 + code=300 |
| TC-ATT-005 | 新增考勤 - 日期格式错误 | "2026/04/22" | 400 |
| TC-ATT-006 | 新增考勤 - 未来日期 | 2099-01-01 | 200 |
| TC-ATT-007 | 新增考勤 - 上午时间不合理 | 开始≥结束 | 200 |
| TC-ATT-008 | 新增考勤 - 下午时间不合理 | 开始≥结束 | 200 |
| TC-ATT-009 | 新增考勤 - 时间格式错误 | "9点" | 400 |
| TC-ATT-010 | 新增考勤 - 状态值无效 | ABSENTEEISM | 200 |
| TC-ATT-011 | 新增考勤 - 所有状态值 | 6种状态 | 200 |
| TC-ATT-012 | 删除考勤 - 成功 | 创建后删除 | 200 |
| TC-ATT-013 | 删除考勤 - ID不存在 | id=999999 | 200 + code=300 |
| TC-ATT-014 | 删除考勤 - ID为0 | id=0 | 200 + code=300 |
| TC-ATT-015 | 删除考勤 - ID为负数 | id=-1 | 400 |
| TC-ATT-016 | 批量删除 - 成功 | 3条记录 | 200 |
| TC-ATT-017 | 批量删除 - 空列表 | 无ids参数 | 200 |
| TC-ATT-018 | 批量删除 - 单个ID | ids=1 | 200 |
| TC-ATT-019 | 批量删除 - 部分ID不存在 | ids=1,999999,2 | 200 |
| TC-ATT-020 | 更新考勤 - 修改状态 | NORMAL→LATE | 200 |
| TC-ATT-021 | 更新考勤 - 修改时间 | 修改4个时段 | 200 |
| TC-ATT-022 | 更新考勤 - ID不存在 | id=999999 | 200 + code=300 |
| TC-ATT-023 | 查询考勤 - 根据ID | 存在的ID | 200 |
| TC-ATT-024 | 查询考勤 - ID不存在 | id=999999 | 200 + code=300 |
| TC-ATT-025 | 按员工和日期查询 | staffId+date | 200 |
| TC-ATT-026 | 按员工查询 - 无数据 | 未来日期 | 200 + code=300 |
| TC-ATT-027 | 考勤列表 - 无条件分页 | 权限检查 | 403 |
| TC-ATT-028 | 考勤列表 - 按员工ID | staffId=1 | 403 |
| TC-ATT-029 | 考勤列表 - 按日期范围 | startDate+endDate | 403 |
| TC-ATT-030 | 考勤列表 - 组合条件 | 多个过滤条件 | 403 |
| TC-ATT-031 | 考勤列表 - 第0页 | current=0 | 403 |
| TC-ATT-032 | 考勤列表 - size=0 | size=0 | 403 |
| TC-ATT-033 | 考勤列表 - size=100 | size=100 | 403 |
| TC-ATT-034 | 考勤列表 - size=101 | 超出每页大小 | 403 |
| TC-ATT-035 | 边界时间(00:00) | 午夜 | 200 |
| TC-ATT-036 | 边界时间(23:59) | 午夜前 | 200 |
| TC-ATT-037 | 备注最大长度(200字符) | 边界值 | 200 |
| TC-ATT-038 | 备注超长(201字符) | 超出限制 | 200 + code=300 |

### 8.3 Service 白盒测试 (AttendanceServiceWhiteBoxTest)

| 编号 | 用例名称 | 测试场景 | 验证点 |
|------|---------|---------|--------|
| TC-ATT-WB-001 | isAbsenteeism — 四时段缺一 | morStartTime=null | 各时段缺失判定为旷工 |
| TC-ATT-WB-002 | isAbsenteeism — 既迟到又早退 | 优先级判定 | 旷工优先于迟到+早退 |
| TC-ATT-WB-003 | isLate — 仅上午迟到 | morStartTime=09:30 | 仅迟到未早退未旷工 |
| TC-ATT-WB-004 | isLate — 仅下午迟到 | aftStartTime=14:30 | 仅迟到未早退未旷工 |
| TC-ATT-WB-005 | isLate — 踩点不迟到 | 09:00/14:00 | 边界值判定 |
| TC-ATT-WB-006 | isLeaveEarly — 仅上午早退 | morEndTime=11:30 | 仅早退未迟到未旷工 |
| TC-ATT-WB-007 | imp() 判定优先级 | 迟到+早退→旷工 | ABSENTEEISM优先级最高 |

---

## 9. 模块八：请假申请

### 9.1 测试文件

| 文件 | 路径 | 类型 |
|------|------|------|
| StaffLeaveControllerTest.java | `controller/` | Controller 集成测试 |
| StaffLeaveServiceWhiteBoxTest.java | `service/` | Service 白盒测试 |

### 9.2 Controller 层测试用例 (StaffLeaveControllerTest)

| 编号 | 用例名称 | 测试场景 | 预期结果 |
|------|---------|---------|---------|
| TC-LEAVE-001 | 新增请假 - 正常 | 完整数据 | 200 |
| TC-LEAVE-002 | 新增请假 - 员工ID为空 | staffId=null | 200 + code=300 |
| TC-LEAVE-003 | 新增请假 - 类型为空 | typeNum=null | 200 + code=300 |
| TC-LEAVE-004 | 新增请假 - 所有类型 | 遍历LeaveEnum | 200 |
| TC-LEAVE-005 | 新增请假 - 天数为0 | days=0 | 200 + code=300 |
| TC-LEAVE-006 | 新增请假 - 天数为负数 | days=-1 | 200 + code=300 |
| TC-LEAVE-007 | 新增请假 - 天数为小数 | days=1 (最小单位) | 200 |
| TC-LEAVE-008 | 新增请假 - 开始日期为空 | startDate=null | 200 + code=300 |
| TC-LEAVE-009 | 新增请假 - 过去日期 | 2020-01-01 | 200 |
| TC-LEAVE-010 | 冲突检测 - 已有待审核 | UNAUDITED冲突 | 200 |
| TC-LEAVE-011 | 冲突检测 - 已批准不冲突 | APPROVE不冲突 | 200 |
| TC-LEAVE-012 | 更新请假 - 批准 | APPROVE | 200 |
| TC-LEAVE-013 | 更新请假 - 驳回 | REJECT | 200 |
| TC-LEAVE-014 | 更新请假 - ID不存在 | id=999999 | 200 + code=300 |
| TC-LEAVE-015 | 更新请假 - ID为空 | id=null | 200 + code=300 |
| TC-LEAVE-016 | 删除请假 - 成功 | 创建后删除 | 200 |
| TC-LEAVE-017 | 删除请假 - ID不存在 | id=999999 | 200 + code=300 |
| TC-LEAVE-018 | 查询请假 - 根据ID | 存在ID | 200 |
| TC-LEAVE-019 | 查询请假 - ID不存在 | id=999999 | 200 + code=300 |
| TC-LEAVE-020 | 按员工查询请假 | staffId=1 | 200 |
| TC-LEAVE-021 | 请假列表 - 无条件分页 | 默认分页 | 200 |
| TC-LEAVE-022 | 请假列表 - 按员工查询 | staffId=1 | 200 |
| TC-LEAVE-023 | 请假列表 - 按状态查询 | status=0 | 200 |
| TC-LEAVE-024 | 使用add接口创建 | POST /staff-leave | 200 |
| TC-LEAVE-025 | 使用edit接口 | PUT /staff-leave | 200 |
| TC-LEAVE-026 | 使用complete接口 | 审批通过 | 200 |
| TC-LEAVE-027 | 使用cancel接口 | 撤销请假 | 200 |
| TC-LEAVE-028 | 请假导入 | 文件上传 | 200 |
| TC-LEAVE-029 | 请假导出 | GET导出 | 200 |
| TC-LEAVE-030 | 最小天数(1) | days=1 | 200 |
| TC-LEAVE-031 | 最大天数(158) | 产假 | 200 |
| TC-LEAVE-032 | 天数超限(MAX_VALUE) | Integer.MAX_VALUE | 200 + code=300 |
| TC-LEAVE-033 | 备注最大长度(200) | 200字符 | 200 |
| TC-LEAVE-034 | 备注超长(201) | 201字符 | 200 + code=300 |
| TC-LEAVE-035 | 审核备注最大长度(200) | 200字符 | 200 |
| TC-LEAVE-036 | 审核备注超长(201) | 201字符 | 200 + code=300 |

### 9.3 Service 白盒测试 (StaffLeaveServiceWhiteBoxTest)

| 编号 | 用例名称 | 暴露问题 |
|------|---------|---------|
| TC-LEAVE-WB-001 | apply() — REJECT状态也算冲突 | 被驳回请假阻挡新申请 |
| TC-LEAVE-WB-002 | apply() — 工作流失败静默吞掉 | 异常被try-catch忽略 |
| TC-LEAVE-WB-003 | complete() — 未知taskKey发送null variables | map=null边界行为 |
| TC-LEAVE-WB-004 | complete() — hr_audit分支 | hrAuditStatus变量正确设置 |
| TC-LEAVE-WB-005 | complete() — manager_audit分支 | managerAuditStatus变量正确设置 |
| TC-LEAVE-WB-006 | claim() — task不存在时DB已修改 | 数据不一致风险 |

---

## 10. 模块九：加班记录

### 10.1 测试文件

| 文件 | 路径 | 类型 |
|------|------|------|
| StaffOvertimeControllerTest.java | `controller/` | Controller 集成测试 |
| StaffOvertimeServiceWhiteBoxTest.java | `service/` | Service 白盒测试 |

### 10.2 Controller 层测试用例 (StaffOvertimeControllerTest)

| 编号 | 用例名称 | 测试场景 | 预期结果 |
|------|---------|---------|---------|
| TC-OT-001 | 加班新增 - 正常 | 完整数据 | 200 |
| TC-OT-002 | 加班新增 - 员工ID不存在 | staffId=999999 | 200 + code=300 |
| TC-OT-003 | 加班新增 - 类型无效 | staffId=null | 200 + code=300 |
| TC-OT-004 | 加班新增 - 所有类型 | WORKDAY_OVERTIME | 200 |
| TC-OT-005 | 加班新增 - 时长为0 | totalOvertime=0 | 200 + code=300 |
| TC-OT-006 | 加班新增 - 时长为负数 | -2.0 | 200 + code=300 |
| TC-OT-007 | 加班新增 - 时长过短(<2h) | 1.5h工作日 | 200 + code=300 |
| TC-OT-008 | 加班新增 - 按日过短 | DAY_OFF 1.5h | 200 + code=300 |
| TC-OT-009 | 加班新增 - 状态值无效 | INVALID_STATUS | 400 |
| TC-OT-010 | 加班新增 - 所有状态值 | 3种状态 | 200 |
| TC-OT-011 | 加班新增 - 加班费为null | overtimeSalary=null | 200 |
| TC-OT-012 | 加班新增 - 加班费为负数 | -100 | 200 + code=300 |
| TC-OT-013 | 删除加班 - 成功 | 创建后删除 | 200 |
| TC-OT-014 | 删除加班 - ID不存在 | id=999999 | 200 + code=300 |
| TC-OT-015 | 批量删除加班 | ids="1,2,3" | 200 |
| TC-OT-016 | 更新加班 - 修改时长 | 2.0→3.0 | 200 |
| TC-OT-017 | 更新加班 - 修改加班费 | 500.00 | 200 |
| TC-OT-018 | 查询加班 - 根据ID | 存在ID | 200 |
| TC-OT-019 | 查询加班 - ID不存在 | id=999999 | 200 + code=300 |
| TC-OT-020 | 按员工和日期查询 | staffId+date | 200 |
| TC-OT-021 | 加班列表 - 无条件分页 | 默认分页 | 200 |
| TC-OT-022 | 加班列表 - 按员工查询 | staffId=1 | 200 |
| TC-OT-023 | 加班列表 - 按日期范围 | month="202604" | 200 |
| TC-OT-024 | 工作日加班(1.5倍) | 4h工作日 | 200 |
| TC-OT-025 | 休息日加班(2倍) | 8h休息日 | 200 |
| TC-OT-026 | 法定假日加班(3倍) | 8h假日 | 200 |
| TC-OT-027 | 时长边界值-最小 | 2.0h | 200 |
| TC-OT-028 | 时长边界值-小数 | 2.5h | 200 |
| TC-OT-029 | 调休管理 - 设置调休 | TIME_OFF | 200 |
| TC-OT-030 | 按员工查询调休天数 | GET /time/off/{id} | 200 |
| TC-OT-036 | 分页 - 第0页 | current=0 | 200 + code=300 |
| TC-OT-037 | 分页 - size=100 | size=100 | 200 |
| TC-OT-038 | 分页 - size=101 | 超出每页大小 | 200 + code=300 |

### 10.3 Service 白盒测试 (StaffOvertimeServiceWhiteBoxTest)

| 编号 | 用例名称 | 测试场景 | 预期加班费 |
|------|---------|---------|-----------|
| TC-OT-WB-001 | 按小时·工作日·≥2h | 4h×1.5倍+100 | 400.00 |
| TC-OT-WB-002 | 按小时·时数不足 | 1.5h<2h | 0 |
| TC-OT-WB-003 | 按日·休息日不调休·≥8h | 8h×2.0倍+100 | 900.00 |
| TC-OT-WB-004 | 按日·时数不足 | 6h<8h | 0 |
| TC-OT-WB-005 | 按小时·法定假日 | 8h×3.0倍+200 | 1400.00 |
| TC-OT-WB-006 | 休息日调休·≥8h | TIME_OFF | null (不发钱) |
| TC-OT-WB-007 | 休息日调休·时数不足 | <8h | null (静默无) |
| TC-OT-WB-008 | 法定假日·不区分调休 | timeOffFlag无效 | 始终发钱 |

---

## 11. 模块十：城市社保标准

### 11.1 测试文件

| 文件 | 路径 | 类型 |
|------|------|------|
| CityControllerTest.java | `controller/` | Controller 集成测试 |
| CityStandardTest.java | `service/` | Service 单元测试 |

### 11.2 Controller 层测试用例 (CityControllerTest)

| 编号 | 用例名称 | 测试场景 | 预期结果 |
|------|---------|---------|---------|
| TC-CITY-001 | 正常新增城市标准 | 有效数据 | 200 |
| TC-CITY-002 | 城市名称为null | name=null | 200 |
| TC-CITY-003 | 城市名称重复 | 同名城市 | 200 |
| TC-CITY-004 | 最低工资≤499 | lowerSalary=499 | 200 |
| TC-CITY-005 | 最低工资为负数 | lowerSalary=-1000 | 200 |
| TC-CITY-006 | 最低工资≥平均工资 | lowerSalary>avg | 200 |
| TC-CITY-007 | 比例>1 | perPensionRate=1.5 | 200 |
| TC-CITY-008 | 城市名最小长度(1字符) | name="京" | 200 |
| TC-CITY-009 | 城市名最大长度(50字符) | 50字符 | 200 + code=300 |
| TC-CITY-010 | 城市名超长(51字符) | 51字符 | 200 + code=300 |
| TC-CITY-011 | 平均工资=500(下限) | averageSalary=500 | 200 |
| TC-CITY-012 | 平均工资=499.99(低于下限) | 499.99 | 200 |
| TC-CITY-013 | 最低工资=平均工资-0.01 | 刚好<平均 | 200 |
| TC-CITY-014 | 最低工资=平均工资 | 等于边界 | 200 |
| TC-CITY-015 | 比例=0(下边界) | perPensionRate=0 | 200 |
| TC-CITY-016 | 比例=1(上边界) | perPensionRate=1 | 200 |
| TC-CITY-017 | 正常编辑城市标准 | 修改名称 | 200 |
| TC-CITY-018 | 编辑时名称为空 | name="" | 200 |
| TC-CITY-019 | 正常删除城市标准 | 逻辑删除 | 200 |
| TC-CITY-020 | 正常导入Excel | 有效xlsx | 200 |
| TC-CITY-021 | 空文件上传 | 0字节 | 200 + code=300 |
| TC-CITY-022 | 非Excel格式(.txt) | text/plain | Exception |

### 11.3 Service 层测试用例 (CityStandardTest)

| 编号 | 用例名称 | 测试场景 |
|------|---------|---------|
| TC-SS-030 | 不同城市标准 - 正常场景 | 北京市完整标准 |
| TC-SS-031 | 非法字符场景(参数化) | XSS/SQL注入/路径遍历/空字符/换行符 |
| TC-SS-032 | 重复城市场景 | 同名城市 |
| TC-SS-033 | 基数上下限超出范围 | 上限<下限/负数基数 |

---

## 12. 模块十一：社保公积金

### 12.1 测试文件

| 文件 | 路径 | 类型 |
|------|------|------|
| InsuranceControllerTest.java | `controller/` | Controller 集成测试 |
| SocialSecurityTestBase.java | `service/` | Service 单元测试 |
| SocialSecurityRatioTest.java | `service/` | Service 单元测试 |
| SocialSecurityCalculateTest.java | `service/` | Service 单元测试 |

### 12.2 Controller 层测试用例 (InsuranceControllerTest)

| 编号 | 用例名称 | 测试场景 | 预期结果 |
|------|---------|---------|---------|
| TC-INS-001 | 正常设置社保 | 全部有效字段 | 200 |
| TC-INS-002 | 社保基数低于下限 | socialBase=1000 | 200 |
| TC-INS-003 | 社保基数高于上限 | socialBase=50000 | 200 |
| TC-INS-004 | 社保基数为负数 | socialBase=-1000 | 200 |
| TC-INS-005 | 公积金基数低于下限 | houseBase=1000 | 200 |
| TC-INS-006 | 公积金基数高于上限 | houseBase=50000 | 200 |
| TC-INS-007 | 公积金个人比例<0.05 | perHouseRate=0.01 | 200 |
| TC-INS-008 | 公积金个人比例>0.12 | perHouseRate=0.13 | 200 |
| TC-INS-009 | 公积金企业比例<0.05 | comHouseRate=0.01 | 200 |
| TC-INS-010 | 公积金企业比例>0.12 | comHouseRate=0.13 | 200 |
| TC-INS-011 | 工伤比例<0.002 | comInjuryRate=0.001 | 200 |
| TC-INS-012 | 工伤比例>0.019 | comInjuryRate=0.02 | 200 |
| TC-INS-013 | 社保基数=下限9000 | 边界值 | 200 |
| TC-INS-014 | 社保基数=上限45000 | 边界值 | 200 |
| TC-INS-015 | 社保基数=下限-0.01 | 8999.99 | 200 |
| TC-INS-016 | 社保基数=上限+0.01 | 45000.01 | 200 |
| TC-INS-017 | 公积金基数=下限10000 | 边界值 | 200 |
| TC-INS-018 | 公积金基数=上限45000 | 边界值 | 200 |
| TC-INS-019 | 公积金基数=下限-0.01 | 9999.99 | 200 |
| TC-INS-020 | 公积金个人比例=0.05 | 下边界 | 200 |
| TC-INS-021 | 公积金个人比例=0.12 | 上边界 | 200 |
| TC-INS-022 | 公积金个人比例=0.049 | 低于下边界 | 200 |
| TC-INS-023 | 工伤比例=0.002 | 下边界 | 200 |
| TC-INS-024 | 工伤比例=0.019 | 上边界 | 200 |
| TC-INS-025 | 工伤比例=0.001 | 低于下边界 | 200 |
| TC-INS-026 | 正常导入Excel | 有效xlsx | 200 |
| TC-INS-027 | 空文件上传 | 0字节 | 200 + code=300 |
| TC-INS-028 | 非Excel格式(.txt) | text/plain | Exception |

### 12.3 Service 层测试用例

#### SocialSecurityTestBase

| 编号 | 用例名称 | 测试场景 |
|------|---------|---------|
| TC-SS-001 | 社保基数设置 - 正常 | 6000/3000基数 |
| TC-SS-002 | 格式不正确(参数化) | "abc"/"6,000"/"6 000"/"6000.00.0" |
| TC-SS-003 | 社保基数负数 | -1000 |
| TC-SS-004 | 个人缴纳大于基数 | 异常数据 |
| TC-SS-005 | 公积金基数负数 | -1000 |
| TC-SS-006 | 公积金缴纳大于基数 | 异常数据 |

#### SocialSecurityRatioTest

| 编号 | 用例名称 | 测试场景 |
|------|---------|---------|
| TC-SS-007 | 缴纳比例配置 - 正常 | 5%/5%/0.2% |
| TC-SS-008 | 个人公积金比例负数 | -0.01 |
| TC-SS-009 | 个人公积金比例>1(参数化) | 1.01/1.5/2.0/100.0 |
| TC-SS-010 | 企业公积金比例负数 | -0.01 |
| TC-SS-011 | 企业公积金比例>1(参数化) | 1.01/1.5/2.0 |
| TC-SS-012 | 工伤比例负数 | -0.001 |
| TC-SS-013 | 工伤比例>1(参数化) | 1.01/1.5/2.0 |
| TC-SS-014 ~ TC-SS-019 | 其他比例字段异常 | 边界值测试 |
| TC-SS-020 | 公积金比例<0 | 双边负数 |
| TC-SS-021 | 公积金比例>1(参数化) | 1.01/1.5/2.0 |
| TC-SS-022 | 个人企业比例不同 | 5% vs 12% |

#### SocialSecurityCalculateTest

| 编号 | 用例名称 | 测试场景 |
|------|---------|---------|
| TC-SS-023 | 社保缴纳计算 - 正常 | 15000基数，5%公积金 |
| TC-SS-024 | 员工ID不存在 | 9999/null/-1/0 |
| TC-SS-025 | 社保基数小于下限 | 5000<6326 |
| TC-SS-026 | 社保基数大于上限 | 40000>33891 |
| TC-SS-027 | 公积金基数小于下限 | 2000<2320 |
| TC-SS-028 | 公积金基数大于上限 | 40000>33891 |
| TC-SS-029 | 保存失败/异常传播/计算精度 | 各种异常场景 |

---

## 13. 模块十二：薪资管理

### 13.1 测试文件

| 文件 | 路径 | 类型 |
|------|------|------|
| SalaryControllerTest.java | `controller/` | Controller 集成测试 |
| SalaryCalculationTest.java | `service/` | Service 单元测试 |
| SalaryDeductServiceTest.java | `service/` | Service 单元测试 |
| SalaryDetailTest.java | `service/` | Service 单元测试 |
| SalaryExportTest.java | `service/` | Service 单元测试 |

### 13.2 Controller 层测试用例 (SalaryControllerTest)

| 编号 | 用例名称 | 测试场景 | 预期结果 |
|------|---------|---------|---------|
| TC-SAL-001 | 正常设置薪资 | 含全部字段 | 200 |
| TC-SAL-002 | 最小薪资设置 | 全部为0 | 200 |
| TC-SAL-003 | 员工不存在 | staffId=999999 | 200 |
| TC-SAL-004 | staffId为null | staffId=null | 200 |
| TC-SAL-005 | 基础工资=0(边界值) | baseSalary=0 | 200 |
| TC-SAL-006 | 基础工资高精度(0.01) | BigDecimal精度 | 200 |
| TC-SAL-007 | 基础工资负数(-0.01) | 负数边界 | 200 |
| TC-SAL-008 | 补贴=0(边界值) | subsidy=0 | 200 |
| TC-SAL-009 | 补贴负数(-0.01) | 数据库约束限制 | 200 + code=300 |
| TC-SAL-010 | 奖金=0(边界值) | bonus=0 | 200 |
| TC-SAL-011 | 奖金负数(-0.01) | 数据库约束限制 | 200 + code=300 |
| TC-SAL-012 | 正常导入Excel | 有效xlsx | 200 |
| TC-SAL-013 | 空文件上传 | 0字节 | 200 + code=300 |
| TC-SAL-014 | 非Excel格式(.txt) | text/plain | Exception |

### 13.3 Service 层测试用例

#### SalaryCalculationTest

| 编号 | 用例名称 | 测试场景 |
|------|---------|---------|
| TC-SALARY-001 | 薪资计算 - 正常 | 无扣款全勤，10600.00 |
| TC-SALARY-002 | 薪资计算 - 有扣款 | 迟到3次+早退2次+旷工1天+请假2天 |
| TC-SALARY-003 | 员工ID不存在 | 空结果 |
| TC-SALARY-004 | 月份格式错误(参数化) | null/""/2024-13等10种 |
| TC-SALARY-005 | 未来月份 | 下个月 |
| TC-SALARY-006 | 月份不存在 | 202301无数据 |
| TC-SALARY-007 | 基础工资负数 | -1000.00 |
| TC-SALARY-008 | 加班费负数 | -100.00 |
| TC-SALARY-009 | 补贴负数 | -200.00 |
| TC-SALARY-010 | 奖金负数 | -100.00 |
| - | 不完整考勤记录 | null考勤统计 |
| - | 负数迟到次数 | -3次 |
| - | 迟到次数超月天数 | 100次 |
| - | 小数迟到次数 | 3次 |
| - | 负数早退次数 | -2次 |
| - | 早退超月天数 | 50次(2月) |
| - | 小数早退次数 | 2次 |
| - | 负数旷工天数 | -5天 |
| - | 旷工超月天数 | 40天(4月) |
| - | 小数旷工天数 | 3天 |
| - | 负数请假扣款 | 空列表 |
| - | 请假超月天数 | 40天 |
| - | 小数请假扣款 | 3天 |
| - | 不存在扣款规则 | deptId=999 |

#### SalaryDeductServiceTest

| 编号 | 用例名称 | 测试场景 |
|------|---------|---------|
| TC-DEDUCTION-001 | 扣款规则配置 - 正常 | 遍历4种扣款类型 |
| TC-DEDUCTION-002 | 重复规则 | 同部门+同类型 |
| TC-DEDUCTION-003 | 备注超长 | 1000×"备注" |
| TC-DEDUCTION-004 | 扣款类型未知 | typeNum=null |
| TC-DEDUCTION-005 | 金额测试(参数化) | -100到1000 |
| TC-DEDUCTION-006 | 负数金额 | -50/0 |
| TC-DEDUCTION-007 | 极大金额 | Integer.MAX_VALUE |
| TC-DEDUCTION-008 | 无效deleteFlag(参数化) | -1/2/3/999 |
| TC-DEDUCTION-010 | 扣款类型管理 - 正常 | 枚举值验证 |
| TC-DEDUCTION-011 | 枚举名称不重复 | 唯一性 |
| TC-DEDUCTION-012 | 枚举编码不重复 | 唯一性 |
| TC-DEDUCTION-013 | 枚举不含特殊字符 | 安全检查 |

#### SalaryDetailTest

| 编号 | 用例名称 | 测试场景 |
|------|---------|---------|
| TC-SALARY-025 | 薪资明细查看 - 正常 | 完整薪资信息 |
| TC-SALARY-026 | 记录ID不存在 | id=9999 |
| TC-SALARY-027 | 非法ID边界 | -1/0/MAX_VALUE/MIN_VALUE |
| TC-SALARY-028 | null ID | id=null |

#### SalaryExportTest

| 编号 | 用例名称 | 测试场景 |
|------|---------|---------|
| TC-SALARY-029 | 薪资报表导出 - 正常 | 2个员工，完整计算 |
| TC-SALARY-030 | 无数据导出 | 空列表 |
| TC-SALARY-031 | 月份格式错误(参数化) | 7种无效格式 |
| TC-SALARY-032 | 不存在部门 | 空结果 |
| TC-SALARY-033 | 不同文件后缀 | .xlsx/.xls/.pdf/.csv |
| TC-SALARY-034 | 非法文件名(参数化) | 11种非法字符 |
| - | null文件名 | NullPointerException |

---

## 14. 模块十三：全局异常处理

### 14.1 测试文件

| 文件 | 路径 | 类型 |
|------|------|------|
| BaseExceptionHandlerWhiteBoxTest.java | `exception/` | 白盒测试 |

### 14.2 测试用例清单

| 编号 | 用例名称 | 异常类型 | 预期 |
|------|---------|---------|------|
| TC-EXH-WB-001 | "文件名不能为空" → code=600 | IllegalArgumentException | FILE_NOT_EXIST |
| TC-EXH-WB-002 | "其他非法参数" → code=300 | IllegalArgumentException | 300 |
| TC-EXH-WB-003 | ServiceException → 400 | ServiceException(STAFF_NOT_EXIST) | 400 + 保留code |
| TC-EXH-WB-004 | DataIntegrityViolationException → 300 | 唯一约束违反 | 300 |
| TC-EXH-WB-005 | NullPointerException → 300 | staff is null | 300 |
| TC-EXH-WB-006 | URL参数类型不匹配 → 400+300 | "abc"→Integer | 400 + code=300 |

---

## 15. 测试基础设施

### 15.1 配置文件

| 文件 | 说明 |
|------|------|
| `TestConfig.java` | 测试配置类，提供 PasswordEncoder Bean |
| `TestSecurityConfig.java` | 安全测试配置，提供空的 JwtAuthenticationFilter（直接放行） |
| `SecurityUtils.java` | MockMvc 安全工具（模拟admin/normal/custom用户） |

### 15.2 测试框架

| 技术 | 用途 |
|------|------|
| JUnit 5 (Jupiter) | 测试框架 |
| MockMvc | Controller 层 HTTP 模拟测试 |
| Mockito | Mock 依赖（Service/Mapper） |
| @SpringBootTest | 集成测试上下文 |
| @Transactional | 测试数据回滚 |
| @WithMockUser | Spring Security 模拟用户 |
| @Import(TestSecurityConfig.class) | 导入测试安全配置 |

---

## 16. 精简统计

### 16.1 操作记录

| 轮次 | 操作 | 文件变化 | 方法变化 |
|------|------|---------|---------|
| 初始 | - | 38 | ~632 |
| P0 Service CRUD | 新增 6 个文件 | +6 | +104 |
| P2 Service 补完 | 新增 4 个文件 | +4 | +62 |
| 删同构 Service | 删除 City/Leave/Overtime 测试 | -3 | -48 |
| Service 失败分支合并 | Insurance/Menu/Attendance 内合并 | 0 | -26 |
| Controller 参数化 | Insurance(13→1) + Menu(11→1) | 0 | -22 |
| **最终** | | **35** | **536** |

### 16.2 JaCoCo 覆盖率（终版）

> **测试**: 536 个 @Test/@ParameterizedTest | **整体指令**: 47% | **Service 指令**: 79%

| Service | 覆盖率 | Service | 覆盖率 |
|---------|--------|---------|--------|
| LeaveService | 100% | OvertimeService | 100% |
| MenuService | 100% | RoleService | 100% |
| CityService | 100% | InsuranceService | 100% |
| HomeService | 98% | SalaryDeductService | 96% |
| AttendanceService | 72% | SalaryService | 89% |
| StaffService | 86% | DocsService | 84% |
| StaffLeaveService | 60% | StaffOvertimeService | 64% |

### 16.3 文件清单（35 个测试文件，536 个测试方法）

```
src/test/java/com/qiujie/
├── controller/
│   ├── AttendanceControllerTest.java      (26)
│   ├── CityControllerTest.java            (22)
│   ├── DeptControllerTest.java            (21)
│   ├── DocsControllerTest.java            (30)
│   ├── DocsControllerWhiteBoxTest.java    (6)
│   ├── InsuranceControllerTest.java       (16, 含1个@ParameterizedTest)
│   ├── LoginControllerTest.java           (15)
│   ├── MenuControllerTest.java            (13, 含1个@ParameterizedTest)
│   ├── RoleControllerTest.java            (14)
│   ├── SalaryControllerTest.java          (14)
│   ├── StaffControllerTest.java           (28)
│   ├── StaffLeaveControllerTest.java      (36)
│   └── StaffOvertimeControllerTest.java   (31)
├── exception/
│   └── BaseExceptionHandlerWhiteBoxTest.java (6)
├── mapper/
│   └── StaffMapperTest.java               (28)
└── service/
    ├── AttendanceServiceTest.java         (15)
    ├── AttendanceServiceWhiteBoxTest.java (7)
    ├── CityStandardTest.java              (10)
    ├── DeptServiceWhiteBoxTest.java       (4)
    ├── DocsServiceWhiteBoxTest.java       (11)
    ├── HomeServiceTest.java               (8)
    ├── InsuranceServiceTest.java          (10)
    ├── MenuServiceTest.java               (12)
    ├── RoleServiceTest.java               (17)
    ├── SalaryCalculationTest.java         (24)
    ├── SalaryDeductServiceTest.java       (21)
    ├── SalaryDetailTest.java              (4)
    ├── SalaryExportTest.java              (26)
    ├── SocialSecurityCalculateTest.java   (10)
    ├── SocialSecurityRatioTest.java       (25)
    ├── SocialSecurityTestBase.java        (6)
    ├── StaffLeaveServiceWhiteBoxTest.java (6)
    ├── StaffOvertimeServiceWhiteBoxTest.java (8)
    ├── StaffServiceTest.java              (26)
    └── StaffServiceWhiteBoxTest.java      (3)
```

### 16.4 报告建议

536 个测试方法中：
- **核心（~70 个）**：白盒 × 8 文件 + 树结构 + 计算引擎 + 安全测试 → 报告完整展开
- **代表（~40 个）**：RoleService CRUD + Controller 正常/权限/认证 → 每模块 1-2 例
- **摘要（~426 个）**：CRUD 同构 + 边界值 @CsvSource 行 → 表格列出

预计报告篇幅：**80-90 页**，完全在 150 页限制内。

---

## 17. 缩减总结

| 指标 | 初始 | 终版 | 变化 |
|------|------|------|------|
| 测试文件 | 38 | 35 | -3 |
| @Test/@ParameterizedTest | ~632 | 536 | -96 (15%) |
| 整文件删除 | - | 3 | 同构 Service ×3 |
| Service 方法合并 | - | 26→10 | 失败分支 + 列表 |
| Controller 参数化 | - | 24→2 | @CsvSource ×2 |
| 🔴 Service 清零 | 5 个 | **0 个** | 全部≥60% |
| Service 层覆盖率 | 51% | **79%** | +28pp |
| 报告预估页数 | ~120 | **~85** | -35 页 |

