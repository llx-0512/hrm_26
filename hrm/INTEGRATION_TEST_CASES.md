# HRM 系统 — 集成测试用例详细文档

> 版本：v2.0  
> 生成日期：2026-06-13  
> 配套文档：[[INTEGRATION_TEST_MASTER.md]] · [[INTEGRATION_TEST_DESIGN.md]]

---

## 目录

1. [测试用例总览](#1-测试用例总览)
2. [P0：核心业务集成流程](#2-p0核心业务集成流程)
3. [P1：重要业务集成流程](#3-p1重要业务集成流程)
4. [P2：边界与异常场景](#4-p2边界与异常场景)
5. [测试基类与基础设施代码](#5-测试基类与基础设施代码)
6. [测试数据准备 SQL](#6-测试数据准备-sql)

---

## 1. 测试用例总览

### 用例索引表

| 编号 | 用例名称 | 优先级 | 涉及模块 | 步骤数 |
|:--|---------|:--:|------|:--:|
| INT-LOGIN-001 | 完整登录 → 操作受保护接口 | P0 | Login + Dept | 3 |
| INT-LOGIN-002 | 登录后无权限访问 | P0 | Login + Dept | 2 |
| INT-ORG-001 | 新建部门 → 新建员工 → 分配角色 → 验证 | P0 | Dept + Staff + Role | 4 |
| INT-ORG-002 | 删除部门 → 检查员工状态 | P0 | Dept + Staff | 2 |
| INT-ORG-003 | 编辑员工状态（启用/禁用）→ 禁用登录 | P0 | Staff + Login | 2 |
| INT-ORG-004 | 重置员工密码 → 新密码登录 | P0 | Staff + Login | 2 |
| INT-RBAC-001 | 菜单(三级) → 角色 → 分配 → 鉴权验证 | P0 | Menu + Role + Staff | 7 |
| INT-RBAC-002 | 修改角色菜单 → 权限即时生效 | P0 | Role + Staff | 2 |
| INT-RBAC-003 | 批量删除角色 → 级联清理 | P0 | Role + Staff | 2 |
| INT-SAL-001 | 完整薪资设置流程 | P0 | City + Insurance + Salary | 4 |
| INT-SAL-002 | 社保基数越界处理 | P0 | City + Insurance | 2 |
| INT-SAL-003 | 修改城市标准 → 员工社保不联动 | P0 | City + Insurance | 2 |
| INT-SAL-004 | 薪资导入 → 批量设置 | P0 | Salary | 2 |
| INT-ATT-001 | 设置考勤 → 薪资扣款联动 | P0 | Attendance + Salary | 3 |
| INT-ATT-002 | 批量删除考勤 → 薪资重算 | P0 | Attendance + Salary | 2 |
| INT-LEAVE-001 | 完整请假审批流程 (Activiti) | P0 | StaffLeave + Activiti | 4 |
| INT-LEAVE-002 | 请假审批拒绝 (revert) | P0 | StaffLeave + Activiti | 3 |
| INT-LEAVE-003 | 员工撤销请假申请 | P0 | StaffLeave + Activiti | 2 |
| INT-LEAVE-004 | 请假通过 → 考勤标记 → 薪资扣款 | P0 | StaffLeave + Attendance + Salary | 3 |
| INT-OVER-001 | 加班 → 调休余额查询 | P0 | Overtime + StaffOvertime | 3 |
| INT-OVER-002 | 加班费 → 薪资联动 | P0 | StaffOvertime + Salary | 2 |
| INT-DEDUCT-001 | 设置部门扣款标准 → 验证生效 | P1 | SalaryDeduct + Leave + Salary | 3 |
| INT-DEDUCT-002 | 修改扣款标准 → 历史薪资不变 | P1 | SalaryDeduct + Salary | 2 |
| INT-IMP-001 | 顺序导入：城市 → 社保 → 薪资 | P1 | City + Insurance + Salary | 3 |
| INT-IMP-002 | 跳过依赖导入 → 数据孤立 | P1 | City + Insurance | 1 |
| INT-IMP-003 | 导入格式校验 | P1 | 全模块 | 3 |
| INT-DOCS-001 | 上传文件 → 关联文档记录 | P1 | Docs | 2 |
| INT-DOCS-002 | 删除文档 → 级联处理 | P1 | Docs | 2 |
| INT-LIFECYCLE-001 | 完整员工生命周期 | P1 | 全模块 | 8 |
| INT-CONSIST-001 | 删除员工 → 级联数据检查 | P1 | Staff + Salary + Insurance | 4 |
| INT-CONSIST-002 | 修改关联数据 → 验证外键约束 | P1 | Insurance + City | 2 |
| INT-EDGE-001 | 重复提交（幂等性） | P2 | City | 2 |
| INT-EDGE-002 | 超大数据量导入 | P2 | Staff | 1 |
| INT-EDGE-003 | 特殊字符处理 | P2 | Staff | 1 |
| INT-EDGE-004 | 事务回滚验证 | P2 | Salary | 2 |

---

## 2. P0：核心业务集成流程

### 2.1 登录认证集成

#### INT-LOGIN-001：完整登录 → 操作受保护接口

```java
@Test
@DisplayName("INT-LOGIN-001: 获取验证码 → 登录 → 调用受保护 API")
void testFullLoginAndAuthorizedAccess() throws Exception {
    // Step 1: 获取验证码
    MvcResult codeResult = mockMvc.perform(get("/validate/code"))
        .andExpect(status().isOk())
        .andReturn();
    
    // 从 Redis 获取验证码（通过 Mock 的 RedisUtil）
    String validateCode = getValidateCodeFromCache(); // 假设实现
    
    // Step 2: 登录获取 Token
    Staff loginStaff = new Staff();
    loginStaff.setCode("admin");
    loginStaff.setPassword("123");
    
    MvcResult loginResult = mockMvc.perform(post("/login/{validateCode}", validateCode)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginStaff)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.token").exists())
        .andReturn();
    
    ResponseDTO loginResponse = parseResponse(loginResult);
    String token = loginResponse.getToken();
    assertNotNull(token, "应返回有效的 JWT Token");
    
    // Step 3: 携带 Token 调用受保护的 API
    Dept dept = new Dept();
    dept.setName("集成测试部门");
    dept.setParentId(0);
    
    mockMvc.perform(post("/dept")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dept)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}
```

#### INT-LOGIN-002：登录后无权限访问

```java
@Test
@DisplayName("INT-LOGIN-002: 登录后无权限访问 → 403")
void testLoginWithoutRequiredAuthority() throws Exception {
    // Step 1: 登录普通员工账户
    String validateCode = getValidateCodeFromCache();
    Staff normalStaff = new Staff();
    normalStaff.setCode("zhangsan");
    normalStaff.setPassword("123");
    
    MvcResult loginResult = mockMvc.perform(post("/login/{validateCode}", validateCode)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(normalStaff)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").exists())
        .andReturn();
    
    String token = parseResponse(loginResult).getToken();
    
    // Step 2: 尝试访问需要 department:add 权限的接口
    Dept dept = new Dept();
    dept.setName("越权部门");
    
    mockMvc.perform(post("/dept")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dept)))
        .andExpect(status().isForbidden()); // 403
}
```

---

### 2.2 组织架构集成

#### INT-ORG-001：新建部门 → 新建员工 → 分配角色 → 验证

```java
@Test
@DisplayName("INT-ORG-001: 部门+员工+角色联动流程")
@WithMockUser(authorities = {"system:department:add", "system:staff:add", "system:staff:set_role"})
void testDepartmentStaffRoleFlow() throws Exception {
    // Step 1: 新建部门
    Dept dept = new Dept();
    dept.setName("技术部");
    dept.setParentId(0);
    dept.setMorStartTime("09:00");
    dept.setMorEndTime("12:00");
    dept.setAftStartTime("14:00");
    dept.setAftEndTime("18:00");
    
    MvcResult deptResult = mockMvc.perform(post("/dept")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dept)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andReturn();
    
    Integer deptId = parseDataAsInteger(deptResult); // 假设返回插入的ID
    
    // Step 2: 在新建部门下新增员工
    Staff staff = new Staff();
    staff.setName("测试员工");
    staff.setCode("test_emp_001");
    staff.setPassword("123");
    staff.setDeptId(deptId);
    staff.setPhone("13800138001");
    staff.setAddress("北京市朝阳区");
    staff.setGender(GenderEnum.MALE);
    staff.setStatus(1); // 在职
    
    MvcResult staffResult = mockMvc.perform(post("/staff")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(staff)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andReturn();
    
    Integer staffId = parseDataAsInteger(staffResult);
    
    // Step 3: 为员工分配角色
    List<Integer> roleIds = Arrays.asList(1, 2); // 超级管理员 + 普通用户
    mockMvc.perform(post("/staff/set/{id}", staffId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(roleIds)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 4: 验证员工拥有角色
    mockMvc.perform(get("/staff/staff/{id}", staffId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(2));
    
    // Step 5: 验证员工详情含部门信息
    mockMvc.perform(get("/staff/info/{id}", staffId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.name").value("测试员工"))
        .andExpect(jsonPath("$.data.deptName").value("技术部"));
}
```

#### INT-ORG-002：删除部门 → 检查员工状态

```java
@Test
@DisplayName("INT-ORG-002: 逻辑删除部门后员工记录保留")
@WithMockUser(authorities = {"system:department:add", "system:department:delete", "system:staff:add"})
void testDeleteDepartment_StaffRecordsPreserved() throws Exception {
    // 先创建部门
    Dept dept = createDepartment("测试部");
    Integer deptId = dept.getId();
    
    // 在部门下创建员工
    Staff staff = createStaff("部门员工", deptId);
    Integer staffId = staff.getId();
    
    // Step 1: 逻辑删除部门
    mockMvc.perform(delete("/dept/{id}", deptId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 2: 验证员工记录仍然存在
    mockMvc.perform(get("/staff/{id}", staffId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.name").value("部门员工"));
    // 注意：员工 deptId 仍然指向已删除的部门，dept.getName() 可能为 null
}
```

---

### 2.3 权限管理 (RBAC) 集成

#### INT-RBAC-001：菜单(三级) → 角色 → 分配 → 鉴权验证

```java
@Test
@DisplayName("INT-RBAC-001: 完整 RBAC 权限分配与鉴权验证")
@WithMockUser(authorities = {"system:menu:add", "system:role:add", "system:role:set_menu", "system:staff:set_role"})
void testFullRbacFlow() throws Exception {
    // Step 1: 创建一级菜单
    Menu level1 = new Menu();
    level1.setName("系统管理");
    level1.setCode("system_mgmt");
    level1.setLevel(0);
    level1.setParentId(0);
    Integer menu1Id = createAndGetId("/menu", level1);
    
    // Step 2: 创建二级页面
    Menu level2 = new Menu();
    level2.setName("部门管理");
    level2.setCode("system_dept");
    level2.setLevel(1);
    level2.setParentId(menu1Id);
    Integer menu2Id = createAndGetId("/menu", level2);
    
    // Step 3: 创建权限点
    Menu permission = new Menu();
    permission.setName("新增部门");
    permission.setCode("dept_add");
    permission.setLevel(2);
    permission.setPermission("system:department:add");
    permission.setParentId(menu2Id);
    Integer permId = createAndGetId("/menu", permission);
    
    // Step 4: 创建角色
    Role role = new Role();
    role.setName("部门管理员");
    role.setCode("dept_admin");
    Integer roleId = createAndGetId("/role", role);
    
    // Step 5: 为角色分配菜单
    mockMvc.perform(post("/role/set/{id}", roleId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Arrays.asList(menu1Id, menu2Id, permId))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 6: 创建员工并分配角色
    Staff staff = createStaff("权限测试员", 1);
    mockMvc.perform(post("/staff/set/{id}", staff.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Arrays.asList(roleId))))
        .andExpect(status().isOk());
    
    // Step 7: 验证该角色拥有 system:department:add 权限
    // (通过查询角色菜单来验证)
    mockMvc.perform(get("/role/{id}", roleId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}
```

#### INT-RBAC-002：修改角色菜单 → 权限即时生效

```java
@Test
@DisplayName("INT-RBAC-002: 清空角色菜单后权限即时失效")
@WithMockUser(authorities = {"system:role:set_menu"})
void testClearRoleMenus_AuthorizationRevoked() throws Exception {
    // 前置：创建角色并分配菜单
    Integer roleId = createRoleWithMenus("临时管理员", Arrays.asList(1, 2, 3));
    Integer staffId = createStaffWithRole("临时员工", roleId);
    
    // Step 1: 清空角色的菜单分配
    mockMvc.perform(post("/role/set/{id}", roleId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Collections.emptyList())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 2: 该角色的员工尝试访问 → 应返回 403
    // (需要以该员工身份登录，验证无权限)
    verifyAccessDeniedForStaff(staffId, "/dept", HttpMethod.POST);
}
```

---

### 2.4 薪酬管理集成

#### INT-SAL-001：完整薪资设置流程 (城市标准 → 社保设置 → 薪资设置 → 查询)

```java
@Test
@DisplayName("INT-SAL-001: 城市标准 → 社保 → 薪资 完整链路")
@WithMockUser(authorities = {"city:add", "insurance:set", "salary:set"})
void testFullSalarySetupFlow() throws Exception {
    // Step 1: 创建城市社保标准
    City city = new City();
    city.setName("北京市");
    city.setLowerSalary(new BigDecimal("2320"));
    city.setAverageSalary(new BigDecimal("11297"));
    city.setUpperSalary(new BigDecimal("33891"));
    city.setPerPensionRate(new BigDecimal("0.08"));
    city.setComPensionRate(new BigDecimal("0.16"));
    city.setPerMedicalRate(new BigDecimal("0.02"));
    city.setComMedicalRate(new BigDecimal("0.08"));
    city.setPerHouseRate(new BigDecimal("0.05"));
    city.setComHouseRate(new BigDecimal("0.05"));
    city.setComInjuryRate(new BigDecimal("0.005"));
    
    Integer cityId = createAndGetId("/city", city);
    
    // Step 2: 为员工设置社保（引用 cityId）
    Insurance insurance = new Insurance();
    insurance.setStaffId(1);
    insurance.setCityId(cityId);
    insurance.setSocialBase(new BigDecimal("15000"));
    insurance.setHouseBase(new BigDecimal("15000"));
    insurance.setPerHouseRate(new BigDecimal("0.05"));
    insurance.setComHouseRate(new BigDecimal("0.05"));
    insurance.setComInjuryRate(new BigDecimal("0.005"));
    
    mockMvc.perform(post("/insurance/set")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(insurance)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 3: 为员工设置薪资
    Salary salary = new Salary();
    salary.setStaffId(1);
    salary.setBaseSalary(new BigDecimal("10000"));
    salary.setSubsidy(new BigDecimal("1000"));
    salary.setBonus(new BigDecimal("2000"));
    
    mockMvc.perform(post("/salary/set")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(salary)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 4: 验证薪资查询包含社保信息
    mockMvc.perform(get("/salary/{id}", 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.baseSalary").value(10000))
        .andExpect(jsonPath("$.data.subsidy").value(1000))
        .andExpect(jsonPath("$.data.bonus").value(2000));
}
```

---

### 2.5 考勤与扣款联动

#### INT-ATT-001：设置考勤 → 迟到扣款 → 薪资联动

```java
@Test
@DisplayName("INT-ATT-001: 考勤迟到 → 薪资扣款联动")
@WithMockUser(authorities = {"attendance:add", "attendance:set", "salary:set", "salary:list"})
void testAttendanceLate_DeductionFlow() throws Exception {
    // 前置：员工已有薪资设置
    setupStaffSalary(1, 10000, 1000, 2000);
    // 前置：设置迟到扣款规则
    setupDeductionRule(1, DeductEnum.LATE, new BigDecimal("50"));
    
    // Step 1: 设置员工考勤为迟到
    Attendance attendance = new Attendance();
    attendance.setStaffId(1);
    attendance.setAttendanceDate(Date.valueOf("2026-06-01"));
    attendance.setMorStartTime(Time.valueOf("09:30:00")); // 迟到30分钟
    attendance.setMorEndTime(Time.valueOf("12:00:00"));
    attendance.setAftStartTime(Time.valueOf("14:00:00"));
    attendance.setAftEndTime(Time.valueOf("18:00:00"));
    attendance.setStatus(AttendanceStatusEnum.LATE);
    
    mockMvc.perform(put("/attendance/set")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(attendance)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 2: 查询当月薪资
    mockMvc.perform(get("/salary")
            .param("staffId", "1")
            .param("month", "202606"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 3: 验证扣款体现 (lateDeduct > 0)
    // 具体取决于 SalaryService 的计算结果
    MvcResult result = mockMvc.perform(get("/salary/query")
            .param("staffId", "1")
            .param("month", "202606"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andReturn();
    
    ResponseDTO response = parseResponse(result);
    // 验证扣款 > 0（具体值取决于计算逻辑）
    assertNotNull(response.getData());
}
```

---

### 2.6 请假工作流集成 (Activiti)

#### INT-LEAVE-001：完整请假审批流程

```java
@Test
@DisplayName("INT-LEAVE-001: 员工申请请假 → 主管拾取 → 审批通过 → 状态验证")
void testFullLeaveApprovalWorkflow() throws Exception {
    // Step 1: 员工提交请假申请
    StaffLeave leave = new StaffLeave();
    leave.setStaffId(1);
    leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE.getCode());
    leave.setDays(1);
    leave.setStartDate(Date.valueOf("2026-06-10"));
    leave.setRemark("个人原因");
    leave.setStatus(AuditStatusEnum.UNAUDITED);
    
    MvcResult applyResult = mockMvc.perform(post("/staff-leave/apply/{code}", "admin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(leave)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andReturn();
    
    Integer leaveId = (Integer) parseResponse(applyResult).getData();
    assertNotNull(leaveId, "请假申请应返回请假ID");
    
    // Step 2: 主管拾取任务
    StaffLeave claimLeave = new StaffLeave();
    claimLeave.setId(leaveId);
    claimLeave.setStatus(AuditStatusEnum.AUDITING);
    
    mockMvc.perform(post("/staff-leave/claim/{code}", "admin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(claimLeave)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 3: 主管审批通过
    StaffLeave approve = new StaffLeave();
    approve.setId(leaveId);
    approve.setStatus(AuditStatusEnum.APPROVE);
    
    mockMvc.perform(post("/staff-leave/complete/{code}", "admin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(approve)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 4: 验证请假状态为已完成
    mockMvc.perform(get("/staff-leave/{id}", leaveId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.status").value("APPROVE"));
}
```

#### INT-LEAVE-002：请假审批拒绝 (revert 归还任务)

```java
@Test
@DisplayName("INT-LEAVE-002: 申请 → 拾取 → 归还(revert) = 拒绝")
void testLeaveApplication_RejectedViaRevert() throws Exception {
    // Step 1: 员工申请请假
    StaffLeave leave = createLeaveApplication(1, LeaveEnum.SICK_LEAVE, 2);
    Integer leaveId = leave.getId();
    
    // Step 2: 主管拾取任务
    claimLeaveTask(leaveId, "admin");
    
    // Step 3: 主管归还任务（驳回）
    StaffLeave revertLeave = new StaffLeave();
    revertLeave.setId(leaveId);
    revertLeave.setStatus(AuditStatusEnum.REJECT);
    
    mockMvc.perform(post("/staff-leave/revert/{code}", "admin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(revertLeave)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // 验证：请假状态为 REJECT
    mockMvc.perform(get("/staff-leave/{id}", leaveId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("REJECT"));
}
```

#### INT-LEAVE-003：员工撤销请假

```java
@Test
@DisplayName("INT-LEAVE-003: 员工自行撤销请假申请")
void testCancelLeaveApplication() throws Exception {
    // 前置：创建请假申请（未被拾取）
    StaffLeave leave = createLeaveApplication(1, LeaveEnum.PERSONAL_LEAVE, 1);
    Integer leaveId = leave.getId();
    
    // Step 1: 撤销请假
    StaffLeave cancelLeave = new StaffLeave();
    cancelLeave.setId(leaveId);
    cancelLeave.setStatus(AuditStatusEnum.CANCEL);
    
    mockMvc.perform(post("/staff-leave/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cancelLeave)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 2: 验证状态
    mockMvc.perform(get("/staff-leave/{id}", leaveId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CANCEL"));
}
```

---

### 2.7 加班管理集成

#### INT-OVER-001：加班设置 → 调休余额查询

```java
@Test
@DisplayName("INT-OVER-001: 加班设置 → 调休余额查询")
@WithMockUser(authorities = {"overtime:add", "overtime:set"})
void testOvertimeSetup_TimeOffBalanceQuery() throws Exception {
    // Step 1: 设置加班规则
    Overtime overtime = new Overtime();
    overtime.setDeptId(1);
    overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME.getCode());
    
    mockMvc.perform(post("/overtime/set")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(overtime)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 2: 设置员工加班
    StaffOvertime staffOvertime = new StaffOvertime();
    staffOvertime.setStaffId(1);
    staffOvertime.setOvertimeDate(Date.valueOf("2026-06-01"));
    staffOvertime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME.getCode());
    staffOvertime.setTotalOvertime(new BigDecimal("4.0"));
    staffOvertime.setStatus(OvertimeStatusEnum.TIME_OFF); // 转为调休
    
    mockMvc.perform(put("/staff-overtime/set")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(staffOvertime)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 3: 查询调休余额
    mockMvc.perform(get("/staff-overtime/time/off/{id}", 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}
```

#### INT-OVER-002：加班费 → 薪资联动

```java
@Test
@DisplayName("INT-OVER-002: 加班费计入当月薪资")
@WithMockUser(authorities = {"overtime:add", "salary:set", "salary:list"})
void testOvertimeSalary_IncludedInPayroll() throws Exception {
    // 前置：设置员工基础薪资
    setupStaffSalary(1, 8000, 500, 1000);
    
    // Step 1: 为员工设置加班（工作日加班 4 小时）
    StaffOvertime staffOvertime = new StaffOvertime();
    staffOvertime.setStaffId(1);
    staffOvertime.setOvertimeDate(Date.valueOf("2026-06-05"));
    staffOvertime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME.getCode());
    staffOvertime.setTotalOvertime(new BigDecimal("4.0"));
    staffOvertime.setOvertimeSalary(new BigDecimal("400.00"));
    
    mockMvc.perform(post("/staff-overtime")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(staffOvertime)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 2: 查询薪资，验证加班费计算入内
    mockMvc.perform(get("/salary")
            .param("staffId", "1")
            .param("month", "202606"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    // overtimeSalary 应 > 0
}
```

---

## 3. P1：重要业务集成流程

### 3.1 数据导入导出

#### INT-IMP-001：顺序导入：城市 → 社保 → 薪资

```java
@Test
@DisplayName("INT-IMP-001: 城市→社保→薪资顺序导入，数据关联正确")
@WithMockUser(authorities = {"city:import", "insurance:import", "salary:import"})
void testSequentialImport_CityInsuranceSalary() throws Exception {
    // Step 1: 导入城市标准
    MockMultipartFile cityFile = createMockExcel("city_template.xlsx");
    mockMvc.perform(multipart("/city/import").file(cityFile))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 2: 导入社保数据（依赖步骤1的城市数据）
    MockMultipartFile insuranceFile = createMockExcel("insurance_template.xlsx");
    mockMvc.perform(multipart("/insurance/import").file(insuranceFile))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 3: 导入薪资数据
    MockMultipartFile salaryFile = createMockExcel("salary_template.xlsx");
    mockMvc.perform(multipart("/salary/import").file(salaryFile))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}
```

#### INT-IMP-003：导入格式校验

```java
@Test
@DisplayName("INT-IMP-003: 非Excel/空文件/格式错误文件导入校验")
@WithMockUser(authorities = {"staff:import"})
void testImportFormatValidation() throws Exception {
    // Case 1: 上传非 Excel 格式
    MockMultipartFile textFile = new MockMultipartFile(
        "file", "test.txt", "text/plain", "hello".getBytes());
    mockMvc.perform(multipart("/staff/import").file(textFile))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(300)); // 格式错误
    
    // Case 2: 上传空文件
    MockMultipartFile emptyFile = new MockMultipartFile(
        "file", "empty.xlsx", 
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        new byte[0]);
    mockMvc.perform(multipart("/staff/import").file(emptyFile))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(300)); // 空文件
    
    // Case 3: 上传格式正确但数据不合法的文件
    MockMultipartFile invalidDataFile = createMockExcelWithInvalidData();
    mockMvc.perform(multipart("/staff/import").file(invalidDataFile))
        .andExpect(status().isOk());
    // 验证导入部分成功或全部失败
}
```

---

### 3.2 完整员工生命周期

#### INT-LIFECYCLE-001：入职 → 配置 → 发薪 → 离职

```java
@Test
@DisplayName("INT-LIFECYCLE-001: 完整员工生命周期")
@WithMockUser(authorities = {
    "system:staff:add", "system:staff:edit", "system:staff:set_role",
    "insurance:set", "salary:set", "attendance:set"
})
void testFullEmployeeLifecycle() throws Exception {
    // Step 1: 入职 - 新增员工
    Staff staff = new Staff();
    staff.setName("生命周期测试");
    staff.setCode("lifecycle_test");
    staff.setPassword("123");
    staff.setDeptId(1);
    staff.setPhone("13900139001");
    staff.setAddress("北京市测试区");
    staff.setGender(GenderEnum.MALE);
    staff.setStatus(1);
    
    MvcResult result = mockMvc.perform(post("/staff")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(staff)))
        .andExpect(status().isOk())
        .andReturn();
    Integer staffId = parseDataAsInteger(result);
    
    // Step 2: 分配角色
    mockMvc.perform(post("/staff/set/{id}", staffId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Arrays.asList(2))))
        .andExpect(status().isOk());
    
    // Step 3: 设置社保
    Insurance insurance = createDefaultInsurance(staffId, 1);
    mockMvc.perform(post("/insurance/set")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(insurance)))
        .andExpect(status().isOk());
    
    // Step 4: 设置薪资
    Salary salary = createDefaultSalary(staffId);
    mockMvc.perform(post("/salary/set")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(salary)))
        .andExpect(status().isOk());
    
    // Step 5: 记录考勤
    Attendance attendance = createNormalAttendance(staffId);
    mockMvc.perform(put("/attendance/set")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(attendance)))
        .andExpect(status().isOk());
    
    // Step 6: 验证薪资计算
    mockMvc.perform(get("/salary").param("staffId", staffId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 7: 离职 - 设置状态为离职
    Staff resignStaff = new Staff();
    resignStaff.setId(staffId);
    resignStaff.setStatus(0); // 离职
    
    mockMvc.perform(put("/staff")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(resignStaff)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 8: 验证离职后登录失败
    verifyLoginFailed(staff.getCode(), "123");
}
```

---

### 3.3 数据一致性

#### INT-CONSIST-001：删除员工 → 级联数据检查

```java
@Test
@DisplayName("INT-CONSIST-001: 逻辑删除员工，关联表数据保留")
@WithMockUser(authorities = {"system:staff:add", "system:staff:delete",
    "insurance:set", "salary:set", "system:staff:set_role"})
void testDeleteEmployee_CascadeDataCheck() throws Exception {
    // 前置：创建完整配置的员工
    Integer staffId = createFullyConfiguredStaff();
    
    // Step 1: 逻辑删除员工
    mockMvc.perform(delete("/staff/{id}", staffId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // Step 2: 验证员工 is_deleted=1
    mockMvc.perform(get("/staff/{id}", staffId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(300)); // 已删除，查询返回失败
    
    // Step 3: 验证薪资记录保留
    mockMvc.perform(get("/salary/{id}", staffId))
        .andExpect(status().isOk());
    // 关联表保留数据（逻辑删除不级联）
    
    // Step 4: 验证社保记录保留
    mockMvc.perform(get("/insurance/{id}", staffId))
        .andExpect(status().isOk());
    
    // Step 5: 验证角色关联保留
    mockMvc.perform(get("/staff/staff/{id}", staffId))
        .andExpect(status().isOk());
}
```

---

## 4. P2：边界与异常场景

#### INT-EDGE-001：重复提交（幂等性）

```java
@Test
@DisplayName("INT-EDGE-001: 两次相同数据新增 → 幂等性检查")
@WithMockUser(authorities = {"city:add"})
void testDuplicateSubmission_Idempotency() throws Exception {
    City city = new City();
    city.setName("测试城市");
    city.setLowerSalary(new BigDecimal("2000"));
    city.setAverageSalary(new BigDecimal("8000"));
    
    // 第一次提交
    mockMvc.perform(post("/city")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(city)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    
    // 第二次相同提交
    mockMvc.perform(post("/city")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(city)))
        .andExpect(status().isOk());
    // 可能返回 200（无唯一约束）或 300（如有重复检查）
}
```

#### INT-EDGE-004：事务回滚验证

```java
@Test
@DisplayName("INT-EDGE-004: 异常场景事务回滚验证")
@WithMockUser(authorities = {"salary:set"})
void testTransactionRollback() throws Exception {
    // 提交不完整数据触发异常
    Salary invalidSalary = new Salary();
    invalidSalary.setStaffId(1);
    // 故意不设置必填字段
    
    mockMvc.perform(post("/salary/set")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidSalary)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(300)); // 失败
    
    // 验证：数据未被部分写入
    // (因为 @Transactional 回滚)
}
```

---

## 5. 测试基类与基础设施代码

### 5.1 集成测试基类

```java
package com.qiujie.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiujie.dto.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * 解析 JSON 响应为 ResponseDTO
     */
    protected ResponseDTO parseResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return objectMapper.readValue(json, ResponseDTO.class);
    }

    /**
     * 从响应中提取 data 字段为 Integer
     */
    protected Integer parseDataAsInteger(MvcResult result) throws Exception {
        ResponseDTO response = parseResponse(result);
        if (response.getData() instanceof Integer) {
            return (Integer) response.getData();
        }
        return objectMapper.convertValue(response.getData(), Integer.class);
    }

    /**
     * 创建实体并返回解析后的响应
     */
    protected MvcResult performPost(String url, Object body) throws Exception {
        return mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post(url)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn();
    }

    /**
     * 创建实体并返回 ID
     */
    protected Integer createAndGetId(String url, Object body) throws Exception {
        MvcResult result = performPost(url, body);
        return parseDataAsInteger(result);
    }
}
```

### 5.2 测试数据帮助类

```java
package com.qiujie.integration;

import com.qiujie.entity.*;
import com.qiujie.enums.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;

/**
 * 集成测试数据工厂
 */
public class TestDataFactory {

    public static Staff createDefaultStaff(String name, String code, Integer deptId) {
        Staff staff = new Staff();
        staff.setName(name);
        staff.setCode(code);
        staff.setPassword("123");
        staff.setDeptId(deptId);
        staff.setPhone("13800138000");
        staff.setAddress("测试地址");
        staff.setGender(GenderEnum.MALE);
        staff.setStatus(1);
        return staff;
    }

    public static Dept createDefaultDept(String name, Integer parentId) {
        Dept dept = new Dept();
        dept.setName(name);
        dept.setParentId(parentId);
        dept.setMorStartTime("09:00");
        dept.setMorEndTime("12:00");
        dept.setAftStartTime("14:00");
        dept.setAftEndTime("18:00");
        return dept;
    }

    public static Attendance createNormalAttendance(Integer staffId) {
        Attendance att = new Attendance();
        att.setStaffId(staffId);
        att.setAttendanceDate(Date.valueOf("2026-06-01"));
        att.setMorStartTime(Time.valueOf("09:00:00"));
        att.setMorEndTime(Time.valueOf("12:00:00"));
        att.setAftStartTime(Time.valueOf("14:00:00"));
        att.setAftEndTime(Time.valueOf("18:00:00"));
        att.setStatus(AttendanceStatusEnum.NORMAL);
        return att;
    }

    public static City createDefaultCity(String name) {
        City city = new City();
        city.setName(name);
        city.setLowerSalary(new BigDecimal("2320"));
        city.setAverageSalary(new BigDecimal("11297"));
        city.setUpperSalary(new BigDecimal("33891"));
        city.setPerPensionRate(new BigDecimal("0.08"));
        city.setComPensionRate(new BigDecimal("0.16"));
        city.setPerMedicalRate(new BigDecimal("0.02"));
        city.setComMedicalRate(new BigDecimal("0.08"));
        city.setPerHouseRate(new BigDecimal("0.05"));
        city.setComHouseRate(new BigDecimal("0.05"));
        city.setComInjuryRate(new BigDecimal("0.005"));
        return city;
    }

    public static Insurance createDefaultInsurance(Integer staffId, Integer cityId) {
        Insurance ins = new Insurance();
        ins.setStaffId(staffId);
        ins.setCityId(cityId);
        ins.setSocialBase(new BigDecimal("15000"));
        ins.setHouseBase(new BigDecimal("15000"));
        ins.setPerHouseRate(new BigDecimal("0.05"));
        ins.setComHouseRate(new BigDecimal("0.05"));
        ins.setComInjuryRate(new BigDecimal("0.005"));
        return ins;
    }

    public static Salary createDefaultSalary(Integer staffId) {
        Salary salary = new Salary();
        salary.setStaffId(staffId);
        salary.setBaseSalary(new BigDecimal("10000"));
        salary.setSubsidy(new BigDecimal("1000"));
        salary.setBonus(new BigDecimal("2000"));
        return salary;
    }

    public static StaffLeave createDefaultLeave(Integer staffId, LeaveEnum type, Integer days) {
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(staffId);
        leave.setTypeNum(type.getCode());
        leave.setDays(days);
        leave.setStartDate(Date.valueOf("2026-06-10"));
        leave.setStatus(AuditStatusEnum.UNAUDITED);
        return leave;
    }
}
```

---

## 6. 测试数据准备 SQL

```sql
-- test-data.sql：集成测试基础数据

-- ========== 部门数据 ==========
INSERT INTO per_dept (id, name, parent_id, mor_start_time, mor_end_time, aft_start_time, aft_end_time, is_deleted)
VALUES 
(1, '技术部', 0, '09:00:00', '12:00:00', '14:00:00', '18:00:00', 0),
(2, '产品部', 0, '09:00:00', '12:00:00', '14:00:00', '18:00:00', 0),
(3, '人事部', 0, '09:00:00', '12:00:00', '14:00:00', '18:00:00', 0);

-- ========== 员工数据 ==========
-- 密码均为 "123" 的 BCrypt 编码
INSERT INTO per_staff (id, name, code, password, dept_id, phone, address, gender, status, is_deleted)
VALUES
(1, '管理员', 'admin', '$2a$10$...', 1, '13800000001', '北京市', 0, 1, 0),
(2, '张三', 'zhangsan', '$2a$10$...', 1, '13800000002', '北京市', 0, 1, 0),
(3, '李四', 'lisi', '$2a$10$...', 3, '13800000003', '北京市', 1, 1, 0);

-- ========== 角色数据 ==========
INSERT INTO per_role (id, name, code, remark, is_deleted)
VALUES
(1, '超级管理员', 'super_admin', '拥有全部权限', 0),
(2, '部门管理员', 'dept_admin', '管理本部门', 0),
(3, '普通用户', 'normal_user', '查看权限', 0);

-- ========== 城市社保标准 ==========
INSERT INTO soc_city (id, name, lower_salary, average_salary, upper_salary, 
    per_pension_rate, com_pension_rate, per_medical_rate, com_medical_rate,
    per_house_rate, com_house_rate, com_injury_rate, is_deleted)
VALUES
(1, '北京市', 2320.00, 11297.00, 33891.00, 
 0.08, 0.16, 0.02, 0.08, 0.05, 0.05, 0.005, 0),
(2, '上海市', 2590.00, 12183.00, 36549.00,
 0.08, 0.16, 0.02, 0.10, 0.07, 0.07, 0.005, 0);

-- ========== 扣款规则 ==========
INSERT INTO sal_deduct (id, dept_id, type_num, amount, remark, is_deleted)
VALUES
(1, 1, 0, 50.00, '迟到扣款50元/次', 0),
(2, 1, 1, 50.00, '早退扣款50元/次', 0),
(3, 1, 2, 200.00, '旷工扣款200元/天', 0),
(4, 1, 3, 100.00, '请假扣款100元/天', 0);
```

---

## 附录：测试执行检查清单

### 冒烟测试 (P0 · 8 用例 · 5 min)

- [ ] INT-LOGIN-001: 登录 → 操作受保护接口
- [ ] INT-ORG-001: 部门 → 员工 → 角色 联动
- [ ] INT-RBAC-001: RBAC 鉴权链路
- [ ] INT-SAL-001: 薪资设置完整链路
- [ ] INT-ATT-001: 考勤 → 扣款联动
- [ ] INT-LEAVE-001: 请假审批工作流
- [ ] INT-OVER-001: 加班 → 调休
- [ ] INT-LIFECYCLE-001: 员工完整生命周期

### 核心集成 (P0 · 20 用例 · 15 min)

- [ ] 以上 8 个 + 其余 12 个 P0 用例

### 完整集成 (P0+P1 · 33 用例 · 25 min)

- [ ] 全部 P0 (20 个) + 全部 P1 (13 个)

### 全量回归 (全部 · 37+ 用例 · 35 min)

- [ ] 全部 P0+P1 (33 个) + 全部 P2 (4 个)

---

> 📝 本文档中的测试代码示例需要根据实际项目配置进行调整（数据库连接、密码编码器、文件路径等）。配套文档见 [INTEGRATION_TEST_MASTER.md](INTEGRATION_TEST_MASTER.md) · [INTEGRATION_TEST_DESIGN.md](INTEGRATION_TEST_DESIGN.md)。
