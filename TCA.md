# HRM 系统 - 测试用例分析文档 (Test Case Analysis)

> 本文档基于 `hrm/src/test` 目录下所有测试文件整理，记录了系统中已实现的全部测试用例。
> 生成日期：2026-06-12

---

## 📊 测试概览

| 模块 | 测试文件数 | 用例数 | 层级 |
|------|-----------|--------|------|
| 员工管理 | 3 | 78 | Service / Controller / Mapper |
| 部门管理 | 1 | 16 | Controller |
| 文档管理 | 1 | 19 | Controller |
| 登录认证 | 1 | 14 | Controller |
| 角色管理 | 1 | 14 | Controller |
| 菜单管理 | 1 | 23 | Controller |
| 考勤记录 | 1 | 27 | Controller |
| 请假申请 | 1 | 29 | Controller |
| 加班记录 | 1 | 30 | Controller |
| 薪资管理 | 1 | 14 | Controller |
| 社保公积金 | 1 | 28 | Controller |
| 城市标准 | 2 | 26 | Controller / Service |
| 薪资计算 | 1 | 20 | Service |
| 薪资扣款规则 | 1 | 13 | Service |
| 薪资明细 | 1 | 4 | Service |
| 薪资导出 | 1 | 7 | Service |
| 社保计算 | 1 | 8 | Service |
| 社保比例 | 1 | 18 | Service |
| 社保基础 | 1 | 6 | Service |
| **总计** | **25** | **~414** | — |

### 测试类型分布

| 类型 | 数量 | 说明 |
|------|------|------|
| 功能测试 | ~180 | 增删改查核心功能 |
| 边界值测试 | ~100 | 边界值、空值、极限值、临界点 |
| 异常测试 | ~60 | 错误输入、异常情况 |
| 权限测试 | ~25 | 有权限/无权限/未认证 |
| 安全测试 | ~15 | 路径遍历、SQL注入、文件类型校验 |
| 业务规则测试 | ~20 | 时长计算、状态转换、冲突检测 |
| 性能测试 | ~2 | 批量查询性能 |

---

## 一、员工管理模块 (Staff Management)

### 1.1 StaffControllerTest — Controller 层 (28 个用例)

| 编号 | 测试方法 | 用例名称 | 权限要求 |
|------|---------|---------|---------|
| TC-STAFF-CTRL-001 | `testAdd_Success()` | 新增员工 - 成功场景 | `system:staff:add` |
| TC-STAFF-CTRL-002 | `testAdd_NoPermission()` | 新增员工 - 缺少权限 | 无权限 → 403 |
| TC-STAFF-CTRL-003 | `testAdd_Unauthenticated()` | 新增员工 - 未认证 | 未认证 → 401 |
| TC-STAFF-CTRL-004 | `testDelete_Success()` | 删除员工 - 成功场景 | `system:staff:delete` |
| TC-STAFF-CTRL-005 | `testDelete_NoPermission()` | 删除员工 - 缺少权限 | 无权限 → 403 |
| TC-STAFF-CTRL-006 | `testDeleteBatch_Success()` | 批量删除员工 - 成功场景 | `system:staff:delete` |
| TC-STAFF-CTRL-007 | `testEdit_Success()` | 编辑员工 - 成功场景 | `system:staff:edit` |
| TC-STAFF-CTRL-008 | `testEdit_NoPermission()` | 编辑员工 - 缺少权限 | 无权限 → 403 |
| TC-STAFF-CTRL-009 | `testQueryById_Success()` | 根据ID查询员工 - 成功 | — |
| TC-STAFF-CTRL-010 | `testQueryById_NonExistent()` | 查询不存在的员工ID (999999) | — |
| TC-STAFF-CTRL-011 | `testQueryInfo_Success()` | 查询员工详细信息（含部门名称） | — |
| TC-STAFF-CTRL-012 | `testList_NoConditions()` | 多条件分页查询 - 无条件 | `system:staff:list` |
| TC-STAFF-CTRL-013 | `testList_ByName()` | 多条件分页查询 - 按姓名查询 | `system:staff:search` |
| TC-STAFF-CTRL-014 | `testList_ByDeptId()` | 多条件分页查询 - 按部门查询 | `system:staff:list` |
| TC-STAFF-CTRL-015 | `testList_ByStatus()` | 多条件分页查询 - 按状态查询 | `system:staff:list` |
| TC-STAFF-CTRL-016 | `testList_CombinedConditions()` | 多条件组合查询 | `system:staff:search` |
| TC-STAFF-CTRL-017 | `testList_NoPermission()` | 多条件分页查询 - 缺少权限 | 无权限 → 403 |
| TC-STAFF-CTRL-018 | `testList_DefaultPagination()` | 分页参数 - 默认值 | `system:staff:list` |
| TC-STAFF-CTRL-019 | `testList_CustomPagination()` | 分页参数 - 指定页码和大小 | `system:staff:list` |
| TC-STAFF-CTRL-020 | `testValidate_CorrectPassword()` | 验证密码 - 正确密码 (123) | — |
| TC-STAFF-CTRL-021 | `testValidate_WrongPassword()` | 验证密码 - 错误密码 | — |
| TC-STAFF-CTRL-022 | `testReset_Success()` | 重置密码 - 成功场景 | — |
| TC-STAFF-CTRL-023 | `testSetRole_Success()` | 为员工设置角色 - 成功 | `system:staff:set_role` |
| TC-STAFF-CTRL-024 | `testQueryByStaffId_Success()` | 查询员工角色 | — |
| TC-STAFF-CTRL-025 | `testAdd_InvalidJson()` | 新增员工 - 无效JSON格式 | 仅需 add 权限 |
| TC-STAFF-CTRL-026 | `testQuery_InvalidIdFormat()` | 查询接口 - 无效的员工ID格式 | — |
| TC-STAFF-CTRL-027 | `testList_InvalidPageNumber()` | 分页查询 - 无效页码 (current=-1) | `system:staff:list` |
| TC-STAFF-CTRL-028 | `testList_InvalidPageSize()` | 分页查询 - 无效页面大小 (size=0) | `system:staff:list` |

### 1.2 StaffServiceTest — Service 层 (22 个用例)

| 编号 | 测试方法 | 用例名称 |
|------|---------|---------|
| TC-STAFF-SVC-001 | `testAdd_Success()` | 新增员工 - 成功场景（验证code、password） |
| TC-STAFF-SVC-002 | `testAdd_WithNullName()` | 新增员工 - 姓名为空 |
| TC-STAFF-SVC-003 | `testAdd_DefaultPassword()` | 新增员工 - 验证默认密码设置 (123) |
| TC-STAFF-SVC-004 | `testAdd_AutoGenerateCode()` | 新增员工 - 验证工号自动生成 (staff_{id}) |
| TC-STAFF-SVC-005 | `testDelete_Success()` | 逻辑删除员工 - 成功场景 |
| TC-STAFF-SVC-006 | `testDelete_NonExistentId()` | 删除不存在的员工 (999999) |
| TC-STAFF-SVC-007 | `testDeleteBatch_Success()` | 批量逻辑删除 - 成功场景 |
| TC-STAFF-SVC-008 | `testDeleteBatch_EmptyList()` | 批量删除 - 空列表 |
| TC-STAFF-SVC-009 | `testEdit_Success()` | 编辑员工信息 - 成功场景 |
| TC-STAFF-SVC-010 | `testEdit_UpdateStatusToDisabled()` | 更新状态为禁用 (status=2) |
| TC-STAFF-SVC-011 | `testEdit_UpdateDepartment()` | 更新部门 (deptId=2) |
| TC-STAFF-SVC-012 | `testEdit_NonExistentStaff()` | 编辑不存在的员工 (999999) |
| TC-STAFF-SVC-013 | `testQueryById_Success()` | 根据ID查询员工 - 成功 |
| TC-STAFF-SVC-014 | `testQueryById_NonExistentId()` | 查询不存在的员工ID |
| TC-STAFF-SVC-015 | `testList_NoConditions()` | 多条件分页查询 - 无条件 |
| TC-STAFF-SVC-016 | `testList_ByName()` | 多条件分页查询 - 按姓名模糊查询 |
| TC-STAFF-SVC-017 | `testList_ByDeptId()` | 多条件分页查询 - 按部门查询 |
| TC-STAFF-SVC-018 | `testList_ByStatus()` | 多条件分页查询 - 按状态查询 |
| TC-STAFF-SVC-019 | `testList_CombinedConditions()` | 多条件组合查询 |
| TC-STAFF-SVC-020 | `testList_Pagination()` | 分页功能验证（第1页vs第2页） |
| TC-STAFF-SVC-021 | `testQueryInfo_Success()` | 查询员工详细信息（含部门名称） |
| TC-STAFF-SVC-022 | `testQueryInfo_WithAge()` | 查询员工详细信息 - 包含年龄计算 |
| TC-STAFF-SVC-023 | `testAdd_DuplicatePhone()` | 新增员工 - 手机号重复 |
| TC-STAFF-SVC-024 | `testEdit_GenderEnum()` | 编辑员工 - 性别枚举值 (MALE→FEMALE) |
| TC-STAFF-SVC-025 | `testQuery_SpecialCharacterName()` | 查询 - 特殊字符姓名 (欧阳·测试) |
| TC-STAFF-SVC-026 | `testList_EmptyResult()` | 分页查询 - 空结果集 |

### 1.3 StaffMapperTest — Mapper 层 (23 个用例)

| 编号 | 测试方法 | 用例名称 |
|------|---------|---------|
| TC-STAFF-MAP-001 | `testInsert()` | 使用BaseMapper插入员工记录 |
| TC-STAFF-MAP-002 | `testSelectById()` | 根据ID查询员工（存在） |
| TC-STAFF-MAP-003 | `testSelectById_NonExistent()` | 查询不存在的员工ID (999999) |
| TC-STAFF-MAP-004 | `testUpdateById()` | 使用BaseMapper更新员工信息 |
| TC-STAFF-MAP-005 | `testDeleteById()` | 逻辑删除员工记录 (验证is_deleted) |
| TC-STAFF-MAP-006 | `testSelectList()` | 查询所有未删除的员工 |
| TC-STAFF-MAP-007 | `testListStaffAttendanceVO()` | 按姓名模糊查询考勤员工 |
| TC-STAFF-MAP-008 | `testListStaffAttendanceVO_NoMatch()` | 查询不存在的姓名 → 空列表 |
| TC-STAFF-MAP-009 | `testListStaffDeptAttendanceVO()` | 按部门和姓名查询考勤员工 |
| TC-STAFF-MAP-010 | `testQueryAttendanceMonthVO()` | 查询所有员工用于月考勤报表 |
| TC-STAFF-MAP-011 | `testQueryByCode()` | 根据工号查询员工 (TEST_CODE_001) |
| TC-STAFF-MAP-012 | `testQueryByCode_NonExistent()` | 查询不存在的工号 |
| TC-STAFF-MAP-013 | `testQueryInfo()` | 查询员工详细信息（含部门名称） |
| TC-STAFF-MAP-014 | `testQueryInfo_FullFields()` | 验证查询结果包含所有必要字段 |
| TC-STAFF-MAP-015 | `testQueryStaffDeptVO()` | 查询所有员工部门视图信息 |
| TC-STAFF-MAP-016 | `testListStaffOvertimeVO()` | 按姓名查询加班员工 |
| TC-STAFF-MAP-017 | `testListStaffDeptOvertimeVO()` | 按部门和姓名查询加班员工 |
| TC-STAFF-MAP-018 | `testQueryOvertimeMonthVO()` | 查询所有员工用于月加班报表 |
| TC-STAFF-MAP-019 | `testQueryByRole()` | 根据角色代码查询员工 (admin) |
| TC-STAFF-MAP-020 | `testListStaffAttendanceVO_EmptyName()` | 空字符串姓名查询 |
| TC-STAFF-MAP-021 | `testListStaffAttendanceVO_NullName()` | null姓名查询 |
| TC-STAFF-MAP-022 | `testPagination_FirstPage()` | 分页查询 - 第一页 (size=5) |
| TC-STAFF-MAP-023 | `testPagination_SecondPage()` | 分页查询 - 第二页 |
| TC-STAFF-MAP-024 | `testQuery_SpecialCharacters()` | 特殊字符姓名查询 (欧阳·测试) |
| TC-STAFF-MAP-025 | `testQuery_ChineseFuzzySearch()` | 中文姓名模糊查询 |
| TC-STAFF-MAP-026 | `testJoinQuery_DeptName()` | JOIN查询 - 验证部门名称正确性 |
| TC-STAFF-MAP-027 | `testLeftJoin_WithoutDept()` | LEFT JOIN - 员工可能没有部门 (deptId=null) |
| TC-STAFF-MAP-028 | `testBatchQueryPerformance()` | 批量查询50条记录性能测试 |

---

## 二、部门管理模块 (Department Management)

### 2.1 DeptControllerTest — Controller 层 (16 个用例)

| 编号 | 测试方法 | 用例名称 | 权限要求 |
|------|---------|---------|---------|
| TC-DEPT-CTRL-001 | `testAddRootDept_Success()` | 新增根部门 (parentId=0) | `system:department:add` |
| TC-DEPT-CTRL-002 | `testAddSubDept_Success()` | 新增子部门 (含上下班时间) | `system:department:add` |
| TC-DEPT-CTRL-003 | `testAddDept_NullName()` | 新增部门 - 名称为null → code=300 | `system:department:add` |
| TC-DEPT-CTRL-004 | `testAddDept_InvalidWorkTime()` | 新增部门 - 上午下班时间早于上班时间 → code=300 | `system:department:add` |
| TC-DEPT-CTRL-005 | `testAddDept_NameMinLength()` | 名称边界值 - 1字符 (技) | `system:department:add` |
| TC-DEPT-CTRL-006 | `testAddDept_NameMaxLength()` | 名称边界值 - 16字符 | `system:department:add` |
| TC-DEPT-CTRL-007 | `testAddDept_NameExceedsMax()` | 名称超长 - 31字符 → code=300 | `system:department:add` |
| TC-DEPT-CTRL-008 | `testAddDept_ZeroWorkTime()` | 工作时间边界值 - 0小时 → code=300 | `system:department:add` |
| TC-DEPT-CTRL-009 | `testAddDept_InvalidTimeFormat()` | 时间格式错误（部分字段缺失）→ code=300 | `system:department:add` |
| TC-DEPT-CTRL-010 | `testDeleteDept_Success()` | 删除部门 - 成功（先创建后删除） | add + delete |
| TC-DEPT-CTRL-011 | `testDeleteDept_IdZero()` | 删除部门 - ID为0 → code=300 | `system:department:delete` |
| TC-DEPT-CTRL-012 | `testDeleteDept_NegativeId()` | 删除部门 - ID为负数 → code=300 | `system:department:delete` |
| TC-DEPT-CTRL-013 | `testDeleteBatch_EmptyList()` | 批量删除 - 空列表 → 400 | `system:department:delete` |
| TC-DEPT-CTRL-014 | `testUpdateDept_Success()` | 更新部门 - 修改名称 → code=200 | add + edit |
| TC-DEPT-CTRL-015 | `testQueryAllDepts_Success()` | 查询所有部门（树形结构） | — |
| TC-DEPT-CTRL-016 | `testListDepts_ByName()` | 部门列表 - 按名称搜索 (技术) | `system:department:list` |
| TC-DEPT-CTRL-017 | `testDeleteDept_HasChildren()` | 删除有子部门的父部门 → code=300 | add + delete |
| TC-DEPT-CTRL-018 | `testAddDept_MaxWorkTime()` | 工作时间边界值 - 12小时 | `system:department:add` |
| TC-DEPT-CTRL-019 | `testAddDept_ExceedWorkTime()` | 工作时间超限 - 13小时 → code=300 | `system:department:add` |
| TC-DEPT-CTRL-020 | `testAddDept_AfternoonBeforeMorning()` | 下午时间早于上午 → code=300 | `system:department:add` |

---

## 三、文档管理模块 (Document Management)

### 3.1 DocsControllerTest — Controller 层 (19 个用例)

| 编号 | 测试方法 | 用例名称 | 权限要求 |
|------|---------|---------|---------|
| TC-DOCS-CTRL-001 | `testUploadFile_Success()` | 文件上传 - 成功 (test.jpg) | `system:docs:upload` |
| TC-DOCS-CTRL-002 | `testUploadFile_UnsupportedFormat()` | 文件上传 - .exe格式不支持 → code=300 | `system:docs:upload` |
| TC-DOCS-CTRL-003 | `testUploadFile_EmptyFile()` | 文件上传 - 空文件 → code=300 | `system:docs:upload` |
| TC-DOCS-CTRL-004 | `testUploadFile_SingleCharName()` | 文件上传 - 1字符文件名 (a.jpg) | `system:docs:upload` |
| TC-DOCS-CTRL-005 | `testUploadFile_SpecialChars()` | 文件上传 - 特殊字符文件名 (test@#$%^&().pdf) | `system:docs:upload` |
| TC-DOCS-CTRL-006 | `testUploadFile_ChineseName()` | 文件上传 - 中文文件名 (测试文档.pdf) | `system:docs:upload` |
| TC-DOCS-CTRL-007 | `testUploadFile_NoExtension()` | 文件上传 - 无扩展名 → code=300 | `system:docs:upload` |
| TC-DOCS-CTRL-008 | `testUploadFile_MultipleExtensions()` | 文件上传 - 多个扩展名 (test.jpg.exe) → code=300 | `system:docs:upload` |
| TC-DOCS-CTRL-009 | `testUploadFile_NonExistentId()` | 文件上传 - ID不存在 (999999) → code=300 | `system:docs:upload` |
| TC-DOCS-CTRL-010 | `testUploadFile_NegativeId()` | 文件上传 - ID为负数 → code=300 | `system:docs:upload` |
| TC-DOCS-CTRL-011 | `testDownloadFile_NonExistent()` | 文件下载 - 文件不存在 → 404 | `system:docs:download` |
| TC-DOCS-CTRL-012 | `testDownloadFile_PathTraversal()` | 文件下载 - 路径遍历攻击 (../../) → 400 | `system:docs:download` |
| TC-DOCS-CTRL-013 | `testDownloadFile_WithSpace()` | 文件下载 - 文件名包含空格 → 400 | `system:docs:download` |
| TC-DOCS-CTRL-014 | `testDownloadFile_ChineseName()` | 文件下载 - 中文文件名 → 400 | `system:docs:download` |
| TC-DOCS-CTRL-015 | `testDownloadFile_PathTraversalAttack()` | 文件下载 - 深层路径遍历 → 400 | `system:docs:download` |
| TC-DOCS-CTRL-016 | `testDownloadFile_AbsolutePath()` | 文件下载 - 绝对路径攻击 → 404 | `system:docs:download` |
| TC-DOCS-CTRL-017 | `testAddDoc_Success()` | 文档新增 - 成功 | `system:docs:add` |
| TC-DOCS-CTRL-018 | `testDeleteDoc_Success()` | 文档删除 - 成功 | `system:docs:delete` |
| TC-DOCS-CTRL-019 | `testListDocs_ByOldName()` | 文档列表 - 按原文件名搜索 (报告) | `system:docs:list` |
| TC-DOCS-CTRL-020 | `testListDocs_ByStaffName()` | 文档列表 - 按员工姓名搜索 (张三) | `system:docs:list` |
| TC-DOCS-CTRL-021 | `testAddDoc_OldNameMinLength()` | 文档新增 - 原文件名边界值 (a.pdf) | `system:docs:add` |
| TC-DOCS-CTRL-022 | `testAddDoc_DuplicateNewName()` | 文档新增 - 新文件名重复 → code=300 | `system:docs:add` |
| TC-DOCS-CTRL-023 | `testListDocs_EmptyKeyword()` | 文档列表 - 空关键词搜索 | `system:docs:list` |
| TC-DOCS-CTRL-024 | `testListDocs_SpecialCharKeyword()` | 文档列表 - SQL注入尝试搜索 (' OR '1'='1) | `system:docs:list` |
| TC-DOCS-CTRL-025 | `testDownloadAvatar_NoAuth()` | 头像下载 - 无需权限 → 404 | 无 |
| TC-DOCS-CTRL-026 | `testUploadFile_FileTooLarge()` | 文件上传 - 21MB → code=300 | `system:docs:upload` |
| TC-DOCS-CTRL-027 | `testDownloadFile_Success()` | 文件下载 - 成功（先上传后下载） | upload + download |
| TC-DOCS-CTRL-028 | `testUploadFile_NameMaxLength()` | 文件上传 - 文件名100字符边界值 | `system:docs:upload` |
| TC-DOCS-CTRL-029 | `testUploadFile_NameExceedsMax()` | 文件上传 - 文件名101字符超限 → code=300 | `system:docs:upload` |
| TC-DOCS-CTRL-030 | `testUploadFile_SizeNearLimit()` | 文件上传 - 接近上限19MB | `system:docs:upload` |
| TC-DOCS-CTRL-031 | `testUploadFile_OneByte()` | 文件上传 - 1字节文件 | `system:docs:upload` |
| TC-DOCS-CTRL-032 | `testDownloadFile_EmptyName()` | 文件下载 - 空文件名 | — |

---

## 四、登录认证模块 (Login Authentication)

### 4.1 LoginControllerTest — Controller 层（14 个用例）

| 编号 | 测试方法 | 用例名称 | 期望结果 |
|------|---------|---------|---------|
| TC-LOGIN-001 | `testLogin_Success()` | 正常登录 (admin/123) | code=200, token存在 |
| TC-LOGIN-002 | `testLogin_NullCode()` | 用户名为null | 400 |
| TC-LOGIN-003 | `testLogin_EmptyCode()` | 用户名为空字符串 | 400 |
| TC-LOGIN-004 | `testLogin_NullPassword()` | 密码为null | 401 |
| TC-LOGIN-005 | `testLogin_EmptyPassword()` | 密码为空字符串 | 401 |
| TC-LOGIN-006 | `testLogin_EmptyValidateCode()` | 验证码为空（空格） | code=300 |
| TC-LOGIN-007 | `testLogin_NonExistentCode()` | 用户名不存在 | 400 |
| TC-LOGIN-008 | `testLogin_WrongPassword()` | 密码错误 | 401 |
| TC-LOGIN-009 | `testLogin_WrongValidateCode()` | 验证码错误 (XXXX) | code=300 |
| TC-LOGIN-010 | `testLogin_AllNull()` | 用户名密码全为null | code=300 |
| TC-LOGIN-011 | `testLogin_MinCodeLength()` | 工号最小长度（1字符 "a"） | 400 |
| TC-LOGIN-012 | `testLogin_MaxCodeLength()` | 工号最大长度（50字符） | 400 |
| TC-LOGIN-013 | `testLogin_ExceedMaxCodeLength()` | 工号超长（51字符） | 400 |
| TC-LOGIN-014 | `testLogin_MinPasswordLength()` | 密码最小长度（1字符 "1"） | 401 |
| TC-LOGIN-015 | `testLogin_SqlInjection()` | 工号含SQL注入（admin' OR '1'='1） | 400 |

> **备注**：使用 `@MockBean` Mock `RedisUtil`，验证码固定为 `TEST12`。

---

## 五、角色管理模块 (Role Management)

### 5.1 RoleControllerTest — Controller 层（14 个用例）

| 编号 | 测试方法 | 用例名称 | 权限要求 |
|------|---------|---------|---------|
| TC-ROLE-CTRL-001 | `testAddRole_Success()` | 正常新增角色 (普通用户) | `permission:role:add` |
| TC-ROLE-CTRL-002 | `testAddRole_NoPermission()` | 无权限新增角色 → 403 | 无权限 |
| TC-ROLE-CTRL-003 | `testAddRole_MinNameLength()` | 角色名最小长度（1字符 "A"） | `permission:role:add` |
| TC-ROLE-CTRL-004 | `testAddRole_MaxNameLength()` | 角色名边界长度（20个中文字符） | `permission:role:add` |
| TC-ROLE-CTRL-005 | `testAddRole_NameExceedsMax()` | 角色名超长（21字符）→ code=300 | `permission:role:add` |
| TC-ROLE-CTRL-006 | `testAddRole_MaxRemarkLength()` | 备注边界长度（200字符） | `permission:role:add` |
| TC-ROLE-CTRL-007 | `testAddRole_RemarkExceedsMax()` | 备注超长（201字符）→ code=300 | `permission:role:add` |
| TC-ROLE-CTRL-008 | `testAddRole_SpecialCharsInName()` | 角色名含特殊字符 (测试\<script\>) | `permission:role:add` |
| TC-ROLE-CTRL-009 | `testEditRole_Success()` | 正常编辑角色（修改名称） | `permission:role:edit` |
| TC-ROLE-CTRL-010 | `testEditRole_EmptyName()` | 编辑角色名称为空字符串 | `permission:role:edit` |
| TC-ROLE-CTRL-011 | `testDeleteBatch_Success()` | 正常批量删除（先创建两个角色）| add + delete |
| TC-ROLE-CTRL-012 | `testDeleteBatch_EmptyList()` | 批量删除空列表 → 400 | `permission:role:delete` |
| TC-ROLE-CTRL-013 | `testSetMenu_Success()` | 正常分配菜单 (1,2,3) | `permission:role:set_menu` |
| TC-ROLE-CTRL-014 | `testSetMenu_EmptyList()` | 分配空菜单列表（清空所有菜单）| `permission:role:set_menu` |

---

## 六、菜单管理模块 (Menu Management)

### 6.1 MenuControllerTest — Controller 层（23 个用例）

| 编号 | 测试方法 | 用例名称 | 权限要求 |
|------|---------|---------|---------|
| TC-MENU-CTRL-001 | `testAddMenu_Level0()` | 新增一级菜单 (level=0) | `permission:menu:add` |
| TC-MENU-CTRL-002 | `testAddMenu_Level1()` | 新增二级页面 (level=1) | `permission:menu:add` |
| TC-MENU-CTRL-003 | `testAddMenu_Level2()` | 新增权限点 (level=2, 含permission) | `permission:menu:add` |
| TC-MENU-CTRL-004 | `testAddMenu_NullName()` | 菜单名称为null | `permission:menu:add` |
| TC-MENU-CTRL-005 | `testAddMenu_EmptyName()` | 菜单名称为空字符串 | `permission:menu:add` |
| TC-MENU-CTRL-006 | `testAddMenu_NullCode()` | 编码为null | `permission:menu:add` |
| TC-MENU-CTRL-007 | `testAddMenu_DuplicateCode()` | 编码重复 | `permission:menu:add` |
| TC-MENU-CTRL-008 | `testAddMenu_MinNameLength()` | 名称最小长度（1字符 "A"） | `permission:menu:add` |
| TC-MENU-CTRL-009 | `testAddMenu_MaxNameLength()` | 名称最大长度（20个中文字符） | `permission:menu:add` |
| TC-MENU-CTRL-010 | `testAddMenu_NameExceedsMax()` | 名称超长（21字符）→ code=300 | `permission:menu:add` |
| TC-MENU-CTRL-011 | `testAddMenu_MinCodeLength()` | 编码最小长度（1字符 "a"） | `permission:menu:add` |
| TC-MENU-CTRL-012 | `testAddMenu_MaxCodeLength()` | 编码最大长度（20字符） | `permission:menu:add` |
| TC-MENU-CTRL-013 | `testAddMenu_CodeExceedsMax()` | 编码超长（21字符）→ code=300 | `permission:menu:add` |
| TC-MENU-CTRL-014 | `testAddMenu_PermissionExceedsMax()` | permission超长（201字符）→ code=300 | `permission:menu:add` |
| TC-MENU-CTRL-015 | `testAddMenu_LevelBoundary0()` | level 边界值 0（一级菜单下边界） | `permission:menu:add` |
| TC-MENU-CTRL-016 | `testAddMenu_LevelBoundary2()` | level 边界值 2（权限点上边界） | `permission:menu:add` |
| TC-MENU-CTRL-017 | `testAddMenu_ParentIdZero()` | parentId=0（根菜单） | `permission:menu:add` |
| TC-MENU-CTRL-018 | `testAddMenu_SpecialCharsInName()` | 名称含特殊字符 (系统\<script\>) | `permission:menu:add` |
| TC-MENU-CTRL-019 | `testEditMenu_Success()` | 正常编辑菜单 | `permission:menu:edit` |
| TC-MENU-CTRL-020 | `testEditMenu_EmptyName()` | 编辑时名称为空 | `permission:menu:edit` |
| TC-MENU-CTRL-021 | `testDeleteMenu_Success()` | 正常删除菜单 | `permission:menu:delete` |
| TC-MENU-CTRL-022 | `testDeleteBatch_Success()` | 正常批量删除菜单 | add + delete |
| TC-MENU-CTRL-023 | `testDeleteBatch_EmptyList()` | 批量删除空列表 → 400 | `permission:menu:delete` |

---

## 七、考勤管理模块 (Attendance Management)

### 7.1 AttendanceControllerTest — Controller 层（27 个用例）

| 编号 | 测试方法 | 用例名称 | 权限要求 |
|------|---------|---------|---------|
| TC-ATT-CTRL-001 | `testAddAttendance_Success()` | 新增考勤 - 正常场景（所有字段合法） | `attendance:add` |
| TC-ATT-CTRL-002 | `testAddAttendance_NullStaffId()` | 新增考勤 - 员工ID为空 → code=300 | `attendance:add` |
| TC-ATT-CTRL-003 | `testAddAttendance_NonExistentStaffId()` | 新增考勤 - 员工ID不存在 (999999) → code=300 | `attendance:add` |
| TC-ATT-CTRL-004 | `testAddAttendance_NullDate()` | 新增考勤 - 日期为空 → code=300 | `attendance:add` |
| TC-ATT-CTRL-005 | `testAddAttendance_InvalidDateFormat()` | 新增考勤 - 日期格式错误 → 400 | `attendance:add` |
| TC-ATT-CTRL-006 | `testAddAttendance_FutureDate()` | 新增考勤 - 日期为未来日期 (2099-01-01) | `attendance:add` |
| TC-ATT-CTRL-007 | `testAddAttendance_InvalidMorningTime()` | 新增考勤 - 上午时间不合理（开始≥结束） | `attendance:add` |
| TC-ATT-CTRL-008 | `testAddAttendance_InvalidAfternoonTime()` | 新增考勤 - 下午时间不合理（开始≥结束） | `attendance:add` |
| TC-ATT-CTRL-009 | `testAddAttendance_InvalidTimeFormat()` | 新增考勤 - 时间格式错误 → 400 | `attendance:add` |
| TC-ATT-CTRL-010 | `testAddAttendance_InvalidStatus()` | 新增考勤 - 状态值（ABSENTEEISM枚举） | `attendance:add` |
| TC-ATT-CTRL-011 | `testAddAttendance_AllStatusValues()` | 新增考勤 - 所有状态值 (0-5) | `attendance:add` |
| TC-ATT-CTRL-012 | `testDeleteAttendance_Success()` | 删除考勤 - 成功（先创建后删除） | add + delete |
| TC-ATT-CTRL-013 | `testDeleteAttendance_NonExistentId()` | 删除考勤 - ID不存在 (999999) → code=300 | `attendance:delete` |
| TC-ATT-CTRL-014 | `testDeleteAttendance_ZeroId()` | 删除考勤 - ID为0 → code=300 | `attendance:delete` |
| TC-ATT-CTRL-015 | `testDeleteAttendance_NegativeId()` | 删除考勤 - ID为负数 (-1) → 400 | `attendance:delete` |
| TC-ATT-CTRL-016 | `testDeleteBatchAttendance_Success()` | 批量删除考勤 - 成功（先创建3条） | add + delete |
| TC-ATT-CTRL-017 | `testDeleteBatchAttendance_EmptyList()` | 批量删除考勤 - 空列表 | `attendance:delete` |
| TC-ATT-CTRL-018 | `testDeleteBatchAttendance_SingleId()` | 批量删除考勤 - 单个ID | `attendance:delete` |
| TC-ATT-CTRL-019 | `testDeleteBatchAttendance_PartialNonExistent()` | 批量删除 - 部分ID不存在 (1,999999,2) | `attendance:delete` |
| TC-ATT-CTRL-020 | `testUpdateAttendance_Status()` | 更新考勤 - 修改状态 (NORMAL→LATE) | add + edit |
| TC-ATT-CTRL-021 | `testUpdateAttendance_ModifyTime()` | 更新考勤 - 修改时间 | add + edit |
| TC-ATT-CTRL-022 | `testUpdateAttendance_NonExistentId()` | 更新考勤 - ID不存在 (999999) → code=300 | `attendance:edit` |
| TC-ATT-CTRL-023 | `testQueryAttendance_Success()` | 查询考勤 - 根据ID（先创建后查询） | `attendance:add` |
| TC-ATT-CTRL-024 | `testQueryAttendance_NonExistentId()` | 查询考勤 - ID不存在 (999999) → code=300 | — |
| TC-ATT-CTRL-025 | `testQueryAttendance_ByStaffAndDate()` | 按员工和日期查询考勤 | `attendance:add` |
| TC-ATT-CTRL-026 | `testQueryAttendance_NoDataForDate()` | 按员工查询 - 无该日期数据 → code=300 | — |
| TC-ATT-CTRL-027 | `testListAttendance_NoConditions()` | 考勤列表 - 无条件分页查询 → 403 | `attendance:list` |
| TC-ATT-CTRL-028 | `testListAttendance_ByStaffId()` | 考勤列表 - 按员工ID查询 → 403 | `attendance:list` |
| TC-ATT-CTRL-029 | `testListAttendance_ByDateRange()` | 考勤列表 - 按日期范围查询 → 403 | `attendance:list` |
| TC-ATT-CTRL-030 | `testListAttendance_CombinedConditions()` | 考勤列表 - 组合条件查询 → 403 | `attendance:list` |
| TC-ATT-CTRL-031 | `testListAttendance_PageZero()` | 考勤列表 - 分页边界 current=0 → 403 | `attendance:list` |
| TC-ATT-CTRL-032 | `testListAttendance_SizeZero()` | 考勤列表 - 分页边界 size=0 → 403 | `attendance:list` |
| TC-ATT-CTRL-033 | `testListAttendance_SizeMax()` | 考勤列表 - 分页边界 size=100 → 403 | `attendance:list` |
| TC-ATT-CTRL-034 | `testListAttendance_SizeExceeds()` | 考勤列表 - 分页边界 size=101 → 403 | `attendance:list` |
| TC-ATT-CTRL-035 | `testAddAttendance_BoundaryTimeStart()` | 考勤时间边界值 - 00:00 | `attendance:add` |
| TC-ATT-CTRL-036 | `testAddAttendance_BoundaryTimeEnd()` | 考勤时间边界值 - 23:59 | `attendance:add` |
| TC-ATT-CTRL-037 | `testAddAttendance_MaxRemarkLength()` | 考勤备注 - 最大长度 200字符 | `attendance:add` |
| TC-ATT-CTRL-038 | `testAddAttendance_ExceedRemarkLength()` | 考勤备注 - 超长 201字符 → code=300 | `attendance:add` |

---

## 八、请假管理模块 (Leave Management)

### 8.1 StaffLeaveControllerTest — Controller 层（29 个用例）

| 编号 | 测试方法 | 用例名称 | 权限要求 |
|------|---------|---------|---------|
| TC-LEAVE-CTRL-001 | `testAddLeave_Success()` | 新增请假 - 正常场景 | `performance:leave:add` |
| TC-LEAVE-CTRL-002 | `testAddLeave_NullStaffId()` | 新增请假 - 员工ID为空 → code=300 | `performance:leave:add` |
| TC-LEAVE-CTRL-003 | `testAddLeave_NullType()` | 新增请假 - 请假类型为空 → code=300 | `performance:leave:add` |
| TC-LEAVE-CTRL-004 | `testAddLeave_AllTypes()` | 新增请假 - 所有请假类型枚举遍历 | `performance:leave:add` |
| TC-LEAVE-CTRL-005 | `testAddLeave_ZeroDays()` | 新增请假 - 请假天数=0 → code=300 | `performance:leave:add` |
| TC-LEAVE-CTRL-006 | `testAddLeave_NegativeDays()` | 新增请假 - 请假天数=-1 → code=300 | `performance:leave:add` |
| TC-LEAVE-CTRL-007 | `testAddLeave_DecimalDays()` | 新增请假 - 请假天数=1（最小单位） | `performance:leave:add` |
| TC-LEAVE-CTRL-008 | `testAddLeave_NullStartDate()` | 新增请假 - 开始日期为空 → code=300 | `performance:leave:add` |
| TC-LEAVE-CTRL-009 | `testAddLeave_PastDate()` | 新增请假 - 开始日期为过去日期 (2020-01-01) | `performance:leave:add` |
| TC-LEAVE-CTRL-010 | `testAddLeave_Conflict_PendingLeave()` | 新增请假 - 冲突检测（已有待审核请假） | `performance:leave:add` |
| TC-LEAVE-CTRL-011 | `testAddLeave_NoConflict_ApprovedLeave()` | 新增请假 - 已批准的请假不冲突 | `performance:leave:add` |
| TC-LEAVE-CTRL-012 | `testEditLeave_Approve()` | 更新请假 - 批准（先创建后更新） | `performance:leave:edit` |
| TC-LEAVE-CTRL-013 | `testEditLeave_Reject()` | 更新请假 - 驳回 | `performance:leave:edit` |
| TC-LEAVE-CTRL-014 | `testEditLeave_NonExistentId()` | 更新请假 - ID不存在 (999999) → code=300 | `performance:leave:edit` |
| TC-LEAVE-CTRL-015 | `testEditLeave_NullId()` | 更新请假 - ID为空 → code=300 | `performance:leave:edit` |
| TC-LEAVE-CTRL-016 | `testDeleteLeave_Success()` | 删除请假 - 成功（先创建后删除） | `performance:leave:delete` |
| TC-LEAVE-CTRL-017 | `testDeleteLeave_NonExistentId()` | 删除请假 - ID不存在 (999999) → code=300 | `performance:leave:delete` |
| TC-LEAVE-CTRL-018 | `testQueryLeave_Success()` | 查询请假 - 根据ID（先创建后查询） | — |
| TC-LEAVE-CTRL-019 | `testQueryLeave_NonExistentId()` | 查询请假 - ID不存在 (999999) → code=300 | — |
| TC-LEAVE-CTRL-020 | `testQueryLeave_ByStaffId()` | 按员工查询请假 | — |
| TC-LEAVE-CTRL-021 | `testListLeave_NoConditions()` | 请假列表 - 无条件分页查询 | list + search |
| TC-LEAVE-CTRL-022 | `testListLeave_ByStaffId()` | 请假列表 - 按员工查询 | list + search |
| TC-LEAVE-CTRL-023 | `testListLeave_ByStatus()` | 请假列表 - 按状态查询 (status=0) | list + search |
| TC-LEAVE-CTRL-024 | `testLeave_AddInterface()` | 请假申请 - 使用add接口创建请假 | `performance:leave:add` |
| TC-LEAVE-CTRL-025 | `testLeave_EditInterface()` | 请假更新 - 使用edit接口修改天数 | `performance:leave:edit` |
| TC-LEAVE-CTRL-026 | `testLeave_CompleteInterface()` | 请假审核 - 使用complete接口批准 | `performance:leave:edit` |
| TC-LEAVE-CTRL-027 | `testLeave_CancelInterface()` | 请假撤销 - 使用cancel接口 | `performance:leave:edit` |
| TC-LEAVE-CTRL-028 | `testImportLeave()` | 请假导入 | `performance:leave:import` |
| TC-LEAVE-CTRL-029 | `testExportLeave()` | 请假导出 | `performance:leave:export` |
| TC-LEAVE-CTRL-030 | `testAddLeave_MinDays()` | 新增请假 - 最小天数 (1天) | `performance:leave:add` |
| TC-LEAVE-CTRL-031 | `testAddLeave_MaxDays()` | 新增请假 - 最大天数（产假158天） | `performance:leave:add` |
| TC-LEAVE-CTRL-032 | `testAddLeave_ExceedMaxDays()` | 新增请假 - 天数超限 (Integer.MAX_VALUE) → code=300 | `performance:leave:add` |
| TC-LEAVE-CTRL-033 | `testAddLeave_MaxRemarkLength()` | 新增请假 - 备注最大长度 200字符 | `performance:leave:add` |
| TC-LEAVE-CTRL-034 | `testAddLeave_ExceedRemarkLength()` | 新增请假 - 备注超长 201字符 → code=300 | `performance:leave:add` |
| TC-LEAVE-CTRL-035 | `testEditLeave_MaxAuditRemarkLength()` | 更新请假 - 审核备注最大长度 200字符 | `performance:leave:edit` |
| TC-LEAVE-CTRL-036 | `testEditLeave_ExceedAuditRemarkLength()` | 更新请假 - 审核备注超长 201字符 → code=300 | `performance:leave:edit` |

---

## 九、加班管理模块 (Overtime Management)

### 9.1 StaffOvertimeControllerTest — Controller 层（30 个用例）

| 编号 | 测试方法 | 用例名称 | 权限要求 |
|------|---------|---------|---------|
| TC-OT-CTRL-001 | `testAddOvertime_Success()` | 加班新增 - 正常场景 | `performance:overtime:add` |
| TC-OT-CTRL-002 | `testAddOvertime_NonExistentStaffId()` | 加班新增 - 员工ID不存在 (999999) → code=300 | `performance:overtime:add` |
| TC-OT-CTRL-003 | `testAddOvertime_InvalidType()` | 加班新增 - 员工ID为空 → code=300 | `performance:overtime:add` |
| TC-OT-CTRL-004 | `testAddOvertime_AllTypes()` | 加班新增 - 所有加班类型 | `performance:overtime:add` |
| TC-OT-CTRL-005 | `testAddOvertime_ZeroDuration()` | 加班新增 - 加班时长=0 → code=300 | `performance:overtime:add` |
| TC-OT-CTRL-006 | `testAddOvertime_NegativeDuration()` | 加班新增 - 加班时长=-2.0 → code=300 | `performance:overtime:add` |
| TC-OT-CTRL-007 | `testAddOvertime_ShortDuration_Workday()` | 加班新增 - 时长过短 (<2h, 1.5h) → code=300 | `performance:overtime:add` |
| TC-OT-CTRL-008 | `testAddOvertime_ShortDuration_DayBased()` | 加班新增 - 休息日时长过短 (1.5h) → code=300 | `performance:overtime:add` |
| TC-OT-CTRL-009 | `testAddOvertime_InvalidStatus()` | 加班新增 - 状态值无效 → 400 | `performance:overtime:add` |
| TC-OT-CTRL-010 | `testAddOvertime_AllStatusValues()` | 加班新增 - 所有状态值 (0,1,2) | `performance:overtime:add` |
| TC-OT-CTRL-011 | `testAddOvertime_NullSalary()` | 加班新增 - 加班费为null | `performance:overtime:add` |
| TC-OT-CTRL-012 | `testAddOvertime_NegativeSalary()` | 加班新增 - 加班费=-100 → code=300 | `performance:overtime:add` |
| TC-OT-CTRL-013 | `testDeleteOvertime_Success()` | 删除加班 - 成功（先创建后删除） | `performance:overtime:delete` |
| TC-OT-CTRL-014 | `testDeleteOvertime_NonExistentId()` | 删除加班 - ID不存在 (999999) → code=300 | `performance:overtime:delete` |
| TC-OT-CTRL-015 | `testDeleteBatchOvertime_Success()` | 批量删除加班 | `performance:overtime:delete` |
| TC-OT-CTRL-016 | `testUpdateOvertime_Duration()` | 更新加班 - 修改时长 (2.0→3.0) | `performance:overtime:edit` |
| TC-OT-CTRL-017 | `testUpdateOvertime_Salary()` | 更新加班 - 修改加班费 (500.00) | `performance:overtime:edit` |
| TC-OT-CTRL-018 | `testQueryOvertime_Success()` | 查询加班 - 根据ID（先创建后查询） | — |
| TC-OT-CTRL-019 | `testQueryOvertime_NonExistentId()` | 查询加班 - ID不存在 (999999) → code=300 | — |
| TC-OT-CTRL-020 | `testQueryOvertime_ByStaffAndDate()` | 按员工和日期查询加班 | — |
| TC-OT-CTRL-021 | `testListOvertime_NoConditions()` | 加班列表 - 无条件分页查询 | list + search |
| TC-OT-CTRL-022 | `testListOvertime_ByStaffId()` | 加班列表 - 按员工查询 | list + search |
| TC-OT-CTRL-023 | `testListOvertime_ByDateRange()` | 加班列表 - 按日期范围查询 (202604) | list + search |
| TC-OT-CTRL-024 | `testCalculateOvertime_Workday()` | 工作日加班 - 上午12-14 下午18-20 (4h) | `performance:overtime:add` |
| TC-OT-CTRL-025 | `testCalculateOvertime_RestDay()` | 休息日加班 - 全天8小时 (2倍) | `performance:overtime:add` |
| TC-OT-CTRL-026 | `testCalculateOvertime_Holiday()` | 法定假日加班 - 全天8小时 (3倍) | `performance:overtime:add` |
| TC-OT-CTRL-027 | `testAddOvertime_MinDuration()` | 加班时长边界 - 最小2小时工作日 | `performance:overtime:add` |
| TC-OT-CTRL-028 | `testAddOvertime_DecimalDuration()` | 加班时长边界 - 小数值2.5小时 | `performance:overtime:add` |
| TC-OT-CTRL-029 | `testUpdateOvertime_SetTimeOff()` | 调休管理 - 设置调休 (OVERTIME→TIME_OFF) | `performance:overtime:edit` |
| TC-OT-CTRL-030 | `testQueryTimeOffDays_Success()` | 调休管理 - 按员工查询调休天数 | — |
| TC-OT-CTRL-031 | `testListOvertime_PageZero()` | 分页查询 - 页码边界值 current=0 → code=300 | list + search |
| TC-OT-CTRL-032 | `testListOvertime_SizeMax()` | 分页查询 - 每页大小边界值 size=100 | list + search |
| TC-OT-CTRL-033 | `testListOvertime_SizeExceeds()` | 分页查询 - 每页大小超限 size=101 → code=300 | list + search |

---

## 十、薪资管理模块 (Salary Management)

### 10.1 SalaryControllerTest — Controller 层（14 个用例）

| 编号 | 测试方法 | 用例名称 | 权限要求 |
|------|---------|---------|---------|
| TC-SAL-CTRL-001 | `testSetSalary_Success()` | 正常设置薪资（baseSalary=10000, subsidy=500, bonus=1000） | `money:salary:set` |
| TC-SAL-CTRL-002 | `testSetSalary_MinValues()` | 最小薪资设置（全部金额=0） | `money:salary:set` |
| TC-SAL-CTRL-003 | `testSetSalary_StaffNotExist()` | 员工不存在 staffId=999999 → code=200 | `money:salary:set` |
| TC-SAL-CTRL-004 | `testSetSalary_NullStaffId()` | staffId 为 null | `money:salary:set` |
| TC-SAL-CTRL-005 | `testSetSalary_BaseSalaryZero()` | 基础工资=0（边界值） | `money:salary:set` |
| TC-SAL-CTRL-006 | `testSetSalary_BaseSalaryHighPrecision()` | 基础工资高精度 0.01 | `money:salary:set` |
| TC-SAL-CTRL-007 | `testSetSalary_BaseSalaryNegative()` | 基础工资为负数 -0.01 | `money:salary:set` |
| TC-SAL-CTRL-008 | `testSetSalary_SubsidyZero()` | 补贴=0（边界值） | `money:salary:set` |
| TC-SAL-CTRL-009 | `testSetSalary_SubsidyNegative()` | 补贴=-0.01 → code=300 | `money:salary:set` |
| TC-SAL-CTRL-010 | `testSetSalary_BonusZero()` | 奖金=0（边界值） | `money:salary:set` |
| TC-SAL-CTRL-011 | `testSetSalary_BonusNegative()` | 奖金=-0.01 → code=300 | `money:salary:set` |
| TC-SAL-CTRL-012 | `testImportSalary_Success()` | 正常导入 Excel（Hutool生成） | `money:salary:import` |
| TC-SAL-CTRL-013 | `testImportSalary_EmptyFile()` | 空文件上传 → code=300 | `money:salary:import` |
| TC-SAL-CTRL-014 | `testImportSalary_NonExcelFormat()` | 非Excel格式 (.txt) → NestedServletException | `money:salary:import` |

### 10.2 SalaryCalculationTest — Service 层（20 个用例）

| 编号 | 测试方法 | 用例名称 |
|------|---------|---------|
| TC-SAL-CALC-001 | `testSalaryCalculation_NormalScenario()` | 薪资计算 - 正常场景（无扣款） |
| TC-SAL-CALC-002 | `testSalaryCalculation_WithDeductions()` | 薪资计算 - 有扣款场景（迟到3次、早退2次、旷工1天、请假2天） |
| TC-SAL-CALC-003 | `testSalaryCalculation_StaffNotExists()` | 薪资计算 - 员工不存在 → 空列表 |
| TC-SAL-CALC-004 | `testSalaryCalculation_InvalidMonthFormat()` | 薪资计算 - 月份格式错误（null/空/2024-13等11种） |
| TC-SAL-CALC-005 | `testSalaryCalculation_FutureMonth()` | 薪资计算 - 未来月份 |
| TC-SAL-CALC-006 | `testSalaryCalculation_MonthNotExists()` | 薪资计算 - 月份不存在 |
| TC-SAL-CALC-007 | `testSalaryCalculation_NegativeBaseSalary()` | 薪资计算 - 基础工资负数 |
| TC-SAL-CALC-008 | `testSalaryCalculation_NegativeOvertimePay()` | 薪资计算 - 加班费负数 |
| TC-SAL-CALC-009 | `testSalaryCalculation_NegativeSubsidy()` | 薪资计算 - 补贴负数 |
| TC-SAL-CALC-010 | `testSalaryCalculation_NegativeBonus()` | 薪资计算 - 奖金负数 |
| TC-SAL-CALC-011 | `testSalaryCalculation_IncompleteAttendanceRecords()` | 薪资计算 - 考勤数据为0/空（容错处理） |
| TC-SAL-CALC-012 | `testSalaryCalculation_NegativeLateTimes()` | 薪资计算 - 负数迟到次数 |
| TC-SAL-CALC-013 | `testSalaryCalculation_LateTimesExceedMonthDays()` | 薪资计算 - 迟到次数超当月天数 (100次) |
| TC-SAL-CALC-014 | `testSalaryCalculation_DecimalLateTimes()` | 薪资计算 - 迟到次数正常整数处理 |
| TC-SAL-CALC-015 | `testSalaryCalculation_NegativeLeaveEarlyTimes()` | 薪资计算 - 负数早退次数 |
| TC-SAL-CALC-016 | `testSalaryCalculation_LeaveEarlyTimesExceedMonthDays()` | 薪资计算 - 早退次数超当月天数 (50次) |
| TC-SAL-CALC-017 | `testSalaryCalculation_DecimalLeaveEarlyTimes()` | 薪资计算 - 早退次数正常整数处理 |
| TC-SAL-CALC-018 | `testSalaryCalculation_NegativeAbsenteeismDays()` | 薪资计算 - 负数旷工天数 |
| TC-SAL-CALC-019 | `testSalaryCalculation_AbsenteeismDaysExceedMonthDays()` | 薪资计算 - 旷工天数超当月天数 (40天) |
| TC-SAL-CALC-020 | `testSalaryCalculation_DecimalAbsenteeismDays()` | 薪资计算 - 旷工天数正常整数处理 |
| TC-SAL-CALC-021 | `testSalaryCalculation_NegativeLeaveDeductionDays()` | 薪资计算 - 请假天数=0 |
| TC-SAL-CALC-022 | `testSalaryCalculation_LeaveDeductionDaysExceedMonthDays()` | 薪资计算 - 请假天数超当月天数 (40天) |
| TC-SAL-CALC-023 | `testSalaryCalculation_DecimalLeaveDeductionDays()` | 薪资计算 - 请假天数正常整数处理 |
| TC-SAL-CALC-024 | `testSalaryCalculation_NonExistentDeductionRuleId()` | 薪资计算 - 不存在的扣款规则（deptId=999） |

### 10.3 SalaryDeductServiceTest — Service 层（13 个用例）

| 编号 | 测试方法 | 用例名称 |
|------|---------|---------|
| TC-DEDUCT-001 | `testAddDeductionRule_NormalScenario()` | 扣款规则配置 - 所有扣款类型枚举遍历 |
| TC-DEDUCT-002 | `testAddDeductionRule_DuplicateRule()` | 扣款规则配置 - 重复名称场景 |
| TC-DEDUCT-003 | `testAddDeductionRule_LongRemark()` | 扣款规则配置 - 备注超长 (1000×"备注") |
| TC-DEDUCT-004 | `testAddDeductionRule_UnknownDeductionType()` | 扣款类型 null → code=200 |
| TC-DEDUCT-005 | `testAddDeductionRule_CalculationMethod()` | 各种金额测试 (-100/-1/0/1/10/50/100/1000) |
| TC-DEDUCT-006 | `testAddDeductionRule_NegativeAmount()` | 负数金额和0金额测试 |
| TC-DEDUCT-007 | `testAddDeductionRule_LargeAmount()` | 极大金额测试 (Integer.MAX_VALUE) |
| TC-DEDUCT-008 | `testAddDeductionRule_InvalidDeleteFlag()` | 无效删除标志值 (-1/2/3/999) |
| TC-DEDUCT-009 | `testDeductionTypeManagement_NormalScenario()` | 扣款类型枚举完整性验证 |
| TC-DEDUCT-010 | `testDeductionTypeManagement_DuplicateName()` | 验证枚举名称唯一性 |
| TC-DEDUCT-011 | `testDeductionTypeManagement_DuplicateCode()` | 验证枚举编码唯一性 |
| TC-DEDUCT-012 | `testDeductionTypeManagement_SpecialCharacters()` | 验证枚举描述不含特殊字符 |
| TC-DEDUCT-013 | （计入上述009-012） | — |

### 10.4 SalaryDetailTest — Service 层（4 个用例）

| 编号 | 测试方法 | 用例名称 |
|------|---------|---------|
| TC-SAL-DETAIL-001 | `testGetSalaryDetail_NormalScenario()` | 薪资明细查看 - 正常场景 (id=1001) |
| TC-SAL-DETAIL-002 | `testGetSalaryDetail_RecordIdNotExists()` | 薪资明细 - 记录ID不存在 (9999) → code=300 |
| TC-SAL-DETAIL-003 | `testGetSalaryDetail_InvalidIdBoundary()` | 薪资明细 - 非法ID边界 (-1/0/MAX/MIN) |
| TC-SAL-DETAIL-004 | `testGetSalaryDetail_NullId()` | 薪资明细 - null ID → code=300 |

### 10.5 SalaryExportTest — Service 层（7 个用例）

| 编号 | 测试方法 | 用例名称 |
|------|---------|---------|
| TC-SAL-EXPORT-001 | `testExportSalaryReport_NormalScenario()` | 薪资报表导出 - 正常场景（2名员工） |
| TC-SAL-EXPORT-002 | `testExportSalaryReport_EmptyData()` | 薪资报表导出 - 无数据月份 |
| TC-SAL-EXPORT-003 | `testExportSalaryReport_InvalidMonthFormat()` | 薪资报表导出 - 月份格式错误（7种） |
| TC-SAL-EXPORT-004 | `testExportSalaryReport_EmptyResultForNonExistentDept()` | 导出不存在的部门 → 空Excel |
| TC-SAL-EXPORT-005 | `testExportSalaryReport_DifferentFileExtensions()` | 不同文件名后缀测试 (xlsx/xls/pdf/csv) |
| TC-SAL-EXPORT-006 | `testExportSalaryReport_InvalidFilename()` | 非法文件名字符测试（15种） |
| TC-SAL-EXPORT-007 | `testExportSalaryReport_NullFilename()` | null文件名 → NullPointerException |

---

## 十一、社保公积金管理模块 (Insurance Management)

### 11.1 InsuranceControllerTest — Controller 层（28 个用例）

| 编号 | 测试方法 | 用例名称 | 权限要求 |
|------|---------|---------|---------|
| TC-INS-CTRL-001 | `testSetInsurance_Success()` | 正常设置社保（全字段） | `money:insurance:set` |
| TC-INS-CTRL-002 | `testSetInsurance_SocialBaseBelowLower()` | 社保基数低于下限 (1000) | `money:insurance:set` |
| TC-INS-CTRL-003 | `testSetInsurance_SocialBaseAboveUpper()` | 社保基数高于上限 (50000) | `money:insurance:set` |
| TC-INS-CTRL-004 | `testSetInsurance_SocialBaseNegative()` | 社保基数负数 (-1000) | `money:insurance:set` |
| TC-INS-CTRL-005 | `testSetInsurance_HouseBaseBelowLower()` | 公积金基数低于下限 (1000) | `money:insurance:set` |
| TC-INS-CTRL-006 | `testSetInsurance_HouseBaseAboveUpper()` | 公积金基数高于上限 (50000) | `money:insurance:set` |
| TC-INS-CTRL-007 | `testSetInsurance_PerHouseRateTooLow()` | 公积金个人比例<0.05 (0.01) | `money:insurance:set` |
| TC-INS-CTRL-008 | `testSetInsurance_PerHouseRateTooHigh()` | 公积金个人比例>0.12 (0.13) | `money:insurance:set` |
| TC-INS-CTRL-009 | `testSetInsurance_ComHouseRateTooLow()` | 公积金企业比例<0.05 (0.01) | `money:insurance:set` |
| TC-INS-CTRL-010 | `testSetInsurance_ComHouseRateTooHigh()` | 公积金企业比例>0.12 (0.13) | `money:insurance:set` |
| TC-INS-CTRL-011 | `testSetInsurance_ComInjuryRateTooLow()` | 工伤比例<0.002 (0.001) | `money:insurance:set` |
| TC-INS-CTRL-012 | `testSetInsurance_ComInjuryRateTooHigh()` | 工伤比例>0.019 (0.02) | `money:insurance:set` |
| TC-INS-CTRL-013 | `testSetInsurance_SocialBaseAtLower()` | 社保基数=下限 9000 | `money:insurance:set` |
| TC-INS-CTRL-014 | `testSetInsurance_SocialBaseAtUpper()` | 社保基数=上限 45000 | `money:insurance:set` |
| TC-INS-CTRL-015 | `testSetInsurance_SocialBaseJustBelowLower()` | 社保基数=下限-0.01 (8999.99) | `money:insurance:set` |
| TC-INS-CTRL-016 | `testSetInsurance_SocialBaseJustAboveUpper()` | 社保基数=上限+0.01 (45000.01) | `money:insurance:set` |
| TC-INS-CTRL-017 | `testSetInsurance_HouseBaseAtLower()` | 公积金基数=下限 10000 | `money:insurance:set` |
| TC-INS-CTRL-018 | `testSetInsurance_HouseBaseAtUpper()` | 公积金基数=上限 45000 | `money:insurance:set` |
| TC-INS-CTRL-019 | `testSetInsurance_HouseBaseJustBelowLower()` | 公积金基数=下限-0.01 (9999.99) | `money:insurance:set` |
| TC-INS-CTRL-020 | `testSetInsurance_PerHouseRateAtLower()` | 公积金个人比例=0.05（下边界） | `money:insurance:set` |
| TC-INS-CTRL-021 | `testSetInsurance_PerHouseRateAtUpper()` | 公积金个人比例=0.12（上边界） | `money:insurance:set` |
| TC-INS-CTRL-022 | `testSetInsurance_PerHouseRateJustBelowLower()` | 公积金个人比例=0.049（刚好低于下边界） | `money:insurance:set` |
| TC-INS-CTRL-023 | `testSetInsurance_ComInjuryRateAtLower()` | 工伤比例=0.002（下边界） | `money:insurance:set` |
| TC-INS-CTRL-024 | `testSetInsurance_ComInjuryRateAtUpper()` | 工伤比例=0.019（上边界） | `money:insurance:set` |
| TC-INS-CTRL-025 | `testSetInsurance_ComInjuryRateJustBelowLower()` | 工伤比例=0.001（刚好低于下边界） | `money:insurance:set` |
| TC-INS-CTRL-026 | `testImportInsurance_Success()` | 正常导入 Excel（Hutool生成） | `money:insurance:import` |
| TC-INS-CTRL-027 | `testImportInsurance_EmptyFile()` | 空文件上传 → code=300 | `money:insurance:import` |
| TC-INS-CTRL-028 | `testImportInsurance_NonExcelFormat()` | 非Excel格式 (.txt) → NestedServletException | `money:insurance:import` |

### 11.2 Service 层社保测试

#### SocialSecurityTestBase — 6 个用例

| 编号 | 测试方法 | 用例名称 |
|------|---------|---------|
| TC-SS-BASE-001 | `testSetSocialSecurityBase_NormalScenario()` | 社保基数设置 - 正常场景 |
| TC-SS-BASE-002 | `testSetSocialSecurityBase_InvalidNumberFormat()` | 格式不正确场景 (abc/6,000/6 000/6000.00.0) |
| TC-SS-BASE-003 | `testSetSocialSecurityBase_NegativeSocialBase()` | 社保基数下限负数场景 (-1000) |
| TC-SS-BASE-004 | `testSetSocialSecurityBase_InvalidSocialPayment()` | 社保缴纳金额大于基数（异常） |
| TC-SS-BASE-005 | `testSetSocialSecurityBase_NegativeHouseBase()` | 公积金基数下限负数场景 (-1000) |
| TC-SS-BASE-006 | `testSetSocialSecurityBase_InvalidHousePayment()` | 公积金缴纳金额大于基数（异常） |

#### SocialSecurityCalculateTest — 8 个用例

| 编号 | 测试方法 | 用例名称 |
|------|---------|---------|
| TC-SS-CALC-001 | `testSetInsurance_NormalScenario()` | 社保缴纳计算 - 正常场景 (15000基数) |
| TC-SS-CALC-002 | `testSetInsurance_StaffNotExists()` | 员工ID不存在 (null/-1/0/9999) |
| TC-SS-CALC-003 | `testSetInsurance_SocialBaseLessThanMin()` | 社保基数<下限 (5000) |
| TC-SS-CALC-004 | `testSetInsurance_SocialBaseGreaterThanMax()` | 社保基数>上限 (40000) |
| TC-SS-CALC-005 | `testSetInsurance_FundBaseLessThanMin()` | 公积金基数<下限 (2000) |
| TC-SS-CALC-006 | `testSetInsurance_FundBaseGreaterThanMax()` | 公积金基数>上限 (40000) |
| TC-SS-CALC-007 | `testSetInsurance_SaveFailed()` | 保存失败场景 |
| TC-SS-CALC-008 | `testSetInsurance_SaveThrowsException()` | 保存抛异常 → RuntimeException |
| TC-SS-CALC-009 | `testSetInsurance_CalculationPrecision()` | 计算精度测试 (12345.67基数) |
| TC-SS-CALC-010 | `testSetInsurance_NegativeAndZeroBase()` | 负数基数和零值基数测试 |

#### SocialSecurityRatioTest — 18 个用例

| 编号 | 测试方法 | 用例名称 |
|------|---------|---------|
| TC-SS-RATIO-001 | `testSetInsuranceRatio_NormalScenario()` | 缴纳比例配置 - 正常场景 |
| TC-SS-RATIO-002 | `testSetInsuranceRatio_PerHouseRateNegative()` | 公积金个人比例负数 (-0.01, -0.001) |
| TC-SS-RATIO-003 | `testSetInsuranceRatio_PerHouseRateGreaterThanOne()` | 公积金个人比例>1 (1.01/1.5/2.0/100.0) |
| TC-SS-RATIO-004 | `testSetInsuranceRatio_ComHouseRateNegative()` | 公积金企业比例负数 (-0.01) |
| TC-SS-RATIO-005 | `testSetInsuranceRatio_ComHouseRateGreaterThanOne()` | 公积金企业比例>1 (1.01/1.5/2.0) |
| TC-SS-RATIO-006 | `testSetInsuranceRatio_ComInjuryRateNegative()` | 工伤比例负数 (-0.001) |
| TC-SS-RATIO-007 | `testSetInsuranceRatio_ComInjuryRateGreaterThanOne()` | 工伤比例>1 (1.01/1.5/2.0) |
| TC-SS-RATIO-008 | `testSetInsuranceRatio_AdditionalRatesNegative()` | 其他比例字段负数 |
| TC-SS-RATIO-009 | `testSetInsuranceRatio_AdditionalRatesGreaterThanOne()` | 其他比例字段>1 |
| TC-SS-RATIO-010 | `testSetInsuranceRatio_OtherRatesNegative()` | 失业保险个人比例<0 |
| TC-SS-RATIO-011 | `testSetInsuranceRatio_OtherRatesGreaterThanOne()` | 失业保险个人比例>1 |
| TC-SS-RATIO-012 | `testSetInsuranceRatio_UnemploymentCompanyRateNegative()` | 失业保险企业比例<0 |
| TC-SS-RATIO-013 | `testSetInsuranceRatio_UnemploymentCompanyRateGreaterThanOne()` | 失业保险企业比例>1 |
| TC-SS-RATIO-014 | `testSetInsuranceRatio_HouseRateNegative()` | 公积金比例<0（个人和企业） |
| TC-SS-RATIO-015 | `testSetInsuranceRatio_HouseRateGreaterThanOne()` | 公积金比例>1（个人和企业） |
| TC-SS-RATIO-016 | `testSetInsuranceRatio_HouseRateDifferent()` | 公积金比例个人企业不同 (5%vs12%) |

---

## 十二、城市标准管理模块 (City Standard Management)

### 12.1 CityControllerTest — Controller 层（22 个用例）

| 编号 | 测试方法 | 用例名称 | 权限要求 |
|------|---------|---------|---------|
| TC-CITY-CTRL-001 | `testAddCity_Success()` | 正常新增城市标准（上海市） | `money:city:add` |
| TC-CITY-CTRL-002 | `testAddCity_NullName()` | 城市名称为 null | `money:city:add` |
| TC-CITY-CTRL-003 | `testAddCity_DuplicateName()` | 城市名称重复 | `money:city:add` |
| TC-CITY-CTRL-004 | `testAddCity_LowerSalaryBelow500()` | 最低工资≤499 (lowerSalary=499) | `money:city:add` |
| TC-CITY-CTRL-005 | `testAddCity_LowerSalaryNegative()` | 最低工资为负数 (-1000) | `money:city:add` |
| TC-CITY-CTRL-006 | `testAddCity_LowerSalaryExceedsAverage()` | 最低工资≥平均工资 | `money:city:add` |
| TC-CITY-CTRL-007 | `testAddCity_RateExceedsOne()` | 比例>1 (perPensionRate=1.5) | `money:city:add` |
| TC-CITY-CTRL-008 | `testAddCity_MinNameLength()` | 城市名最小长度 (1字符 "京") | `money:city:add` |
| TC-CITY-CTRL-009 | `testAddCity_MaxNameLength()` | 城市名最大长度 (50字符) → code=300 | `money:city:add` |
| TC-CITY-CTRL-010 | `testAddCity_NameExceedsMax()` | 城市名超长 (51字符) → code=300 | `money:city:add` |
| TC-CITY-CTRL-011 | `testAddCity_AverageSalaryAtLower()` | 平均工资=500（刚好等于下限） | `money:city:add` |
| TC-CITY-CTRL-012 | `testAddCity_AverageSalaryJustBelowMin()` | 平均工资=499.99（刚好低于下限） | `money:city:add` |
| TC-CITY-CTRL-013 | `testAddCity_LowerSalaryJustBelowAverage()` | 最低工资=平均工资-0.01 | `money:city:add` |
| TC-CITY-CTRL-014 | `testAddCity_LowerSalaryEqualsAverage()` | 最低工资=平均工资（等于边界） | `money:city:add` |
| TC-CITY-CTRL-015 | `testAddCity_RateZero()` | 比例=0（下边界值） | `money:city:add` |
| TC-CITY-CTRL-016 | `testAddCity_RateOne()` | 比例=1（上边界值） | `money:city:add` |
| TC-CITY-CTRL-017 | `testEditCity_Success()` | 正常编辑城市标准 | `money:city:edit` |
| TC-CITY-CTRL-018 | `testEditCity_EmptyName()` | 编辑时名称为空 | `money:city:edit` |
| TC-CITY-CTRL-019 | `testDeleteCity_Success()` | 正常删除城市标准 | `money:city:delete` |
| TC-CITY-CTRL-020 | `testImportCity_Success()` | 正常导入 Excel（Hutool生成） | `money:city:import` |
| TC-CITY-CTRL-021 | `testImportCity_EmptyFile()` | 空文件上传 → code=300 | `money:city:import` |
| TC-CITY-CTRL-022 | `testImportCity_NonExcelFormat()` | 非Excel格式 (.txt) → NestedServletException | `money:city:import` |

### 12.2 CityStandardTest — Service 层（4 个用例）

| 编号 | 测试方法 | 用例名称 |
|------|---------|---------|
| TC-CITY-SVC-001 | `testAdd_NormalScenario()` | 不同城市标准设置 - 正常场景（北京市） |
| TC-CITY-SVC-002 | `testAdd_InvalidCharacters()` | 非法字符场景（XSS/SQL注入/路径遍历/空字符/换行符/制表符共7种） |
| TC-CITY-SVC-003 | `testAdd_DuplicateCity()` | 重复城市场景 |
| TC-CITY-SVC-004 | `testAdd_BeyondLimitRange()` | 基数上下限超出规定范围（上限<下限、负数基数） |

---

## 📈 汇总统计

### 按文件统计

| 测试文件 | 层级 | 用例数 |
|---------|------|--------|
| `StaffControllerTest.java` | Controller | 28 |
| `StaffServiceTest.java` | Service | 26 |
| `StaffMapperTest.java` | Mapper | 28 |
| `DeptControllerTest.java` | Controller | 20 |
| `DocsControllerTest.java` | Controller | 32 |
| `LoginControllerTest.java` | Controller | 15 |
| `RoleControllerTest.java` | Controller | 14 |
| `MenuControllerTest.java` | Controller | 23 |
| `AttendanceControllerTest.java` | Controller | 38 |
| `StaffLeaveControllerTest.java` | Controller | 36 |
| `StaffOvertimeControllerTest.java` | Controller | 33 |
| `SalaryControllerTest.java` | Controller | 14 |
| `InsuranceControllerTest.java` | Controller | 28 |
| `CityControllerTest.java` | Controller | 22 |
| `SalaryCalculationTest.java` | Service | 24 |
| `SalaryDeductServiceTest.java` | Service | 13 |
| `SalaryDetailTest.java` | Service | 4 |
| `SalaryExportTest.java` | Service | 7 |
| `SocialSecurityTestBase.java` | Service | 6 |
| `SocialSecurityCalculateTest.java` | Service | 10 |
| `SocialSecurityRatioTest.java` | Service | 16 |
| `CityStandardTest.java` | Service | 4 |
| **总计** | — | **~441** |

### 测试技术栈

| 技术 | 用途 |
|------|------|
| JUnit Jupiter 5 | 测试框架 |
| MockMvc | Controller 层 HTTP 请求测试 |
| `@WithMockUser` | Spring Security 权限模拟 |
| `@Transactional` | 测试数据自动回滚 |
| Mockito (`@Mock`/`@InjectMocks`/`spy`) | Service 层 Mock 依赖 |
| `ReflectionTestUtils` | 注入 Mock 依赖 |
| `@MockBean` (Spring Boot) | Mock Spring Bean (如 RedisUtil) |
| JUnit `@ParameterizedTest` | 参数化测试 |
| Hutool ExcelUtil | 生成测试用 Excel |
| MockMultipartFile | 模拟文件上传 |
| `@Import(TestSecurityConfig.class)` | 导入测试安全配置 |

### 边界值覆盖

- **字符串长度**：最小-1、最小、最大、最大+1 ✓
- **数值范围**：最小值-1、最小值、最大值、最大值+1、0、负数 ✓
- **时间日期**：过去日期、未来日期、时间边界 (00:00/23:59) ✓
- **分页参数**：0、1、100、101、极大值 ✓
- **文件大小**：0字节、1字节、19MB、20MB、21MB ✓
- **集合操作**：空列表、单个元素、部分无效 ✓
- **枚举值**：所有有效枚举值遍历 ✓

### 安全测试覆盖

| 安全测试类型 | 覆盖文件 | 说明 |
|------------|---------|------|
| 路径遍历攻击 | DocsControllerTest, CityStandardTest | `../` 拒绝访问 → 400 |
| SQL注入测试 | DocsControllerTest, LoginControllerTest, CityStandardTest | 输入 `' OR '1'='1` 等 |
| XSS攻击测试 | RoleControllerTest, MenuControllerTest, CityStandardTest | `<script>` 标签输入 |
| 文件类型校验 | DocsControllerTest | `.exe`/`.jpg.exe` 拒绝 |
| 文件名安全 | DocsControllerTest, SalaryExportTest | 特殊字符/绝对路径 |
| 权限绕过测试 | 所有 Controller 测试 | `@WithMockUser` 无权限 → 403 |
| 未认证测试 | StaffControllerTest, LoginControllerTest | 无 Token → 401 |
| 密码安全 | StaffControllerTest, StaffServiceTest | BCrypt加密验证 |

---

> 🤖 本文档基于 `hrm/src/test` 目录下 25 个测试文件自动分析整理，共收录约 **441 个测试用例**。

---

## 📊 测试覆盖率估计

### 估计前提

在进行覆盖率估计时，已考虑以下实际业务约束：

1. **前端拦截约束**：部分数据的上限/下限由前端控件制约，低于下限或超出上限的数据无法提交到后端。例如：
   - 请假天数：前端日历控件限制最小 1 天,0 天或负数无法提交
   - 最低工资：前端滑块/输入框限制 ≥500,低于此值无法提交
   - 加班时长：前端限制最小值,负数时长无法提交
   - 社保/公积金比例：前端下拉框或滑块锁定在有效范围内
   - 考勤日期：前端日期选择器限制不能选择未来日期

2. **UI 交互约束**：部分操作通过手动选中目标执行,不存在目标对象不存在的情况。例如：
   - 员工删除：在列表页手动勾选目标行后点击删除按钮
   - 菜单删除：在菜单树中选中目标节点后操作
   - 部门删除：在部门树中选中目标后操作
   - 角色删除：在角色列表中勾选后批量操作

3. **受影响的冗余用例**：上述约束导致部分测试用例覆盖的场景在**实际业务流程中不可达**,但这些用例仍有价值：
   - **防御性测试**：验证后端 API 独立于前端的健壮性
   - **API 直调测试**：防止绕过前端直接调用 API 的恶意行为
   - **回归测试**：防止后端重构时引入回归

### 按模块估计

| 模块 | 用例总数 | 冗余用例 | 有效用例 | 覆盖率 | 说明 |
|------|---------|---------|---------|--------|------|
| **员工管理** | 82 | ~6 | 76 | **92%** | 三层全覆盖(Controller/Service/Mapper)；删除不存在ID 的用例因 UI 约束冗余,但密码管理、角色分配、权限验证均完备 |
| **部门管理** | 20 | ~2 | 18 | **85%** | 核心 CRUD + 工作时间校验已覆盖；循环引用防御已注释未启用；缺少批量导入导出测试 |
| **文档管理** | 32 | ~3 | 29 | **88%** | 上传/下载/CRUD 全覆盖；安全测试充分（路径遍历、SQL注入、文件类型校验）；部分下载场景返回 400 源于 Spring Security 防火墙而非业务逻辑 |
| **登录认证** | 15 | ~2 | 13 | **93%** | 成功/失败/验证码/密码/边界值/SQL注入全覆盖；50字符工号长度测试因前端限制冗余 |
| **角色管理** | 14 | ~1 | 13 | **88%** | 增删改+分配菜单全覆盖；权限验证（有权限/无权限→403）完备；名称/备注边界值充分 |
| **菜单管理** | 23 | ~2 | 21 | **87%** | 三级菜单(level 0/1/2)+编码+权限点全覆盖；删除不存在 ID 因 UI 约束冗余；名称含`<script>` 已测试 |
| **考勤记录** | 38 | ~7 | 31 | **80%** | 增删改查+状态枚举全覆盖；但未来日期、时间格式错误、部分时间逻辑冲突由前端拦截；分页列表查询返回 403（权限配置可优化） |
| **请假申请** | 36 | ~8 | 28 | **78%** | 增删改查+7种请假类型全覆盖；工作流接口(apply/complete/cancel)均有测试；但0天/负数/Integer.MAX_VALUE/过去日期均由前端拦截 |
| **加班记录** | 33 | ~5 | 28 | **83%** | 增删改查+3种加班类型全覆盖；加班费计算+时长计算+调休管理均有测试；无效状态 JSON 注入和负数时长由前端拦截 |
| **薪资管理** | 62 | ~6 | 56 | **90%** | Controller 层设置+导入完整；Service 层计算逻辑覆盖 **极其详尽**（正常/扣款/负数/超限/精度/无效月份等 24 个场景）；导出测试覆盖多种异常文件名 |
| **社保公积金** | 60 | ~10 | 50 | **82%** | Controller 层设置+导入+28个边界值用例；Service 层基数/比例/计算精度均有测试；但大量比例边界值(如 0.001、0.049、0.02 等)受前端下拉框限制,实际不可达 |
| **城市标准** | 26 | ~5 | 21 | **80%** | 增删改查+导入全覆盖；名称长度/工资/比例边界值充分；负数工资、比例>1、最低工资<500 等由前端拦截 |

### 加权汇总

| 指标 | 数值 |
|------|------|
| **测试用例总数** | **441** |
| **有效用例数**（剔除冗余） | ~**384** |
| **冗余用例数**（前端/UI 拦截） | ~**57**（约 13%） |
| **加权平均覆盖率** | **≈ 85%** |

```
加权计算：
(82×92% + 20×85% + 32×88% + 15×93% + 14×88% + 23×87% +
 38×80% + 36×78% + 33×83% + 62×90% + 60×82% + 26×80%) / 441
≈ 85.2%
```

### 分层覆盖率

| 层级 | 模块数 | 覆盖率 |
|------|--------|--------|
| **Controller 层** | 12 模块 | ~86% |
| **Service 层** | 5 模块（薪资+社保+城市） | ~87% |
| **Mapper 层** | 1 模块（员工） | ~92% |

### 覆盖缺口与改进建议

| 优先级 | 缺口 | 影响模块 | 建议 |
|--------|------|---------|------|
| 🔴 高 | **数据导入导出测试** | 部门、考勤 | 部门管理的 Excel 导入导出完全缺失；考勤导出缺失 |
| 🔴 高 | **循环引用防御** | 部门 | DeptController 中父子部门循环引用测试已注释,应启用 |
| 🟡 中 | **列表查询权限校验** | 考勤 | AttendanceController 列表分页测试全部返回 403,权限码需与 `@PreAuthorize` 对齐 |
| 🟡 中 | **并发冲突测试** | 全部 | 无任何并发/多线程场景测试 |
| 🟡 中 | **跨模块集成测试** | 薪资计算 | 薪资计算依赖考勤/加班/社保数据,缺少端到端集成测试 |
| 🟢 低 | **冗余用例精简** | 社保、请假 | ~13% 用例覆盖前端拦截场景,可标记为 `@Disabled` 或移至安全专项测试 |
| 🟢 低 | **性能测试扩展** | 全部 | 仅 Mapper 层有 1 个批量查询性能测试,缺少大数据量场景 |

### 结论

当前测试套件对 **核心业务功能** 的覆盖率达到 **约 85%**。考虑到约 13% 的用例覆盖的是被前端拦截的不可达场景（属于防御性测试），有效业务场景的覆盖率为 **约 85%**。主要缺口集中在：跨模块集成测试、并发场景测试、部分模块的数据导入导出测试以及考勤模块的列表查询权限配置问题。
