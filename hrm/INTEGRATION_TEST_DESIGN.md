# HRM 系统集成测试用例设计文档

> 设计方法：业务流程驱动 + 跨模块数据依赖
> 覆盖范围：前端有数据交互的 API（排除纯读取接口：列表/详情/树形查询/导出/下载）
> 优先级：P0 = 核心流程，P1 = 重要流程，P2 = 边界场景

---

## 系统模块总览

| 模块 | Controller | 数据交互 API 数 | 业务定位 |
|------|-----------|:--:|------|
| 登录认证 | LoginController | 1 | 系统入口 |
| 组织架构 | StaffController + DeptController | 9 | 员工/部门管理 |
| 权限管理 | RoleController + MenuController | 9 | RBAC 权限体系 |
| 薪酬管理 | SalaryController + InsuranceController + CityController + SalaryDeductController | 14 | 薪资社保计算 |
| 考勤加班 | AttendanceController + StaffLeaveController + StaffOvertimeController + LeaveController + OvertimeController | 17 | 考勤/请假/加班/工作流 |
| 文件管理 | DocsController | 3 | 文件上传 |
| **合计** | **15 个 Controller** | **53** | — |

---

## 一、登录认证集成流程（P0）

### 1.1 完整登录 → 操作受保护接口

| 用例编号 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 |
|:--|---------|---------|---------|---------|
| INT-LOGIN-001 | 获取验证码 → 登录 → 调用受保护 API | 管理员账户 "admin"/"123" 存在 | 1. GET /validate/code 获取验证码<br>2. POST /login/{code} 携带 code="admin", password="123"<br>3. 从响应提取 Token<br>4. 携带 Token 调用 POST /dept 新增部门 | 步骤2 返回 200 + Token；步骤4 返回 200 |
| INT-LOGIN-002 | 登录后无权限访问 | 普通员工账户 | 1. 登录普通员工账户<br>2. 携带 Token 调用 POST /dept（需要 department:add 权限） | 步骤2 返回 403 Forbidden |
| INT-LOGIN-003 | 验证码过期后登录 | — | 1. 获取过期验证码<br>2. 使用过期验证码登录 | 返回 code=300 |

---

## 二、组织架构管理集成流程（P0）

### 2.1 部门 + 员工 + 角色 联动

| 用例编号 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 |
|:--|---------|---------|---------|---------|
| INT-ORG-001 | 新建部门 → 新建员工 → 分配角色 → 验证 | 角色"普通用户"已存在 | 1. POST /dept 新增部门"技术部"<br>2. POST /staff 新增员工，deptId=技术部.id<br>3. POST /staff/set/{staffId} 分配角色<br>4. GET /staff/info/{staffId} 查询验证 | 步骤1 返回 200<br>步骤2 返回 200<br>步骤3 返回 200<br>步骤4 查到员工含角色信息 |
| INT-ORG-002 | 删除部门 → 检查员工状态 | 部门下有在职员工 | 1. DELETE /dept/{deptId}<br>2. GET /staff?deptId={deptId} 查询 | 系统应阻止删除或标记部门已删除（逻辑删除），员工记录保留 |
| INT-ORG-003 | 编辑员工状态（启用/禁用） | 员工为正常状态 | 1. PUT /staff 设置 status=0(禁用)<br>2. 禁用员工尝试登录 | 步骤2 登录失败 code=500 |
| INT-ORG-004 | 重置员工密码 | 员工存在 | 1. PUT /staff/reset 设置新密码<br>2. 使用新密码登录 | 步骤2 登录成功 |

---

## 三、权限管理集成流程（P0）

### 3.1 RBAC 权限分配 → 鉴权验证

| 用例编号 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 |
|:--|---------|---------|---------|---------|
| INT-RBAC-001 | 菜单（一级+二级+权限点）→ 角色 → 分配菜单 → 验证 | 空菜单表 | 1. POST /menu 创建一级菜单 type=0<br>2. POST /menu 创建二级页面 type=1, parentId=一级.id<br>3. POST /menu 创建权限点 type=2, permission="system:dept:add"<br>4. POST /role 创建角色"部门管理员"<br>5. POST /role/set/{roleId} 分配菜单 [一级.id, 二级.id, 权限点.id]<br>6. 为员工分配该角色<br>7. 员工登录验证是否有部门新增权限 | 步骤7 有权访问 POST /dept |
| INT-RBAC-002 | 修改角色菜单 → 权限即时生效 | 角色已有菜单分配 | 1. POST /role/set/{roleId} 清空菜单 []<br>2. 该角色员工尝试访问对应接口 | 返回 403 |
| INT-RBAC-003 | 批量删除角色 → 级联清理 | 角色已分配给员工 | 1. DELETE /role/batch/{ids}<br>2. 查询关联员工的角色信息 | 角色删除成功，员工角色关联清理 |

---

## 四、薪酬管理集成流程（P0）

### 4.1 城市标准 → 社保设置 → 薪资设置 → 查询

| 用例编号 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 |
|:--|---------|---------|---------|---------|
| INT-SAL-001 | 完整薪资设置流程 | 员工存在，城市存在 | 1. POST /city 创建城市社保标准<br>2. POST /insurance/set 为员工设置社保（引用 cityId）<br>3. POST /salary/set 为员工设置薪资（baseSalary/subsidy/bonus）<br>4. GET /salary/query?staffId=... 验证薪资计算 | 步骤1-3 各返回 200<br>步骤4 查询到完整薪资含社保扣款 |
| INT-SAL-002 | 社保基数越界处理 | 城市标准 socLowerLimit=5000, socUpperLimit=30000 | 1. POST /insurance/set 设置 socialBase=4000（低于下限）<br>2. POST /insurance/set 设置 socialBase=35000（高于上限） | 实际行为取决于业务校验（当前无校验 → 200） |
| INT-SAL-003 | 修改城市标准 → 已设置员工社保不联动 | 员工已按城市A设置社保 | 1. PUT /city 修改城市A的基数上下限<br>2. GET /insurance/staff/{id} 查看员工社保基数 | 员工社保基数不变（快照模式） |
| INT-SAL-004 | 薪资导入 → 批量设置 | 准备好 Excel 文件含多条薪资数据 | 1. POST /salary/import 上传 Excel<br>2. GET /salary 分页查询验证导入数据 | 导入成功，数据一致 |

---

## 五、考勤管理集成流程（P0）

### 5.1 考勤设置 → 扣款联动

| 用例编号 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 |
|:--|---------|---------|---------|---------|
| INT-ATT-001 | 设置考勤 → 迟到扣款 → 薪资联动 | 员工已有薪资设置 | 1. PUT /attendance/set 设置员工考勤（status=迟到）<br>2. GET /salary?month=202601 查询当月薪资<br>3. 验证 lateDeduct > 0 | 考勤设置成功，薪资中体现迟到扣款 |
| INT-ATT-002 | 批量删除考勤 → 薪资重算 | 员工有多条考勤记录 | 1. DELETE /attendance/batch?ids=... 批量删除<br>2. GET /salary?month=202601 验证薪资 | 扣款随考勤删除而重算 |

### 5.2 请假工作流（Activiti）

| 用例编号 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 |
|:--|---------|---------|---------|---------|
| INT-LEAVE-001 | 完整请假审批流程 | Activiti 引擎运行中，员工和主管存在 | 1. POST /staff-leave/apply/{code} 员工提交请假申请<br>2. POST /staff-leave/claim/{code} 主管拾取任务<br>3. POST /staff-leave/complete/{code} 主管审批通过<br>4. GET /staff-leave/{id} 查询请假状态 | 步骤1 返回 200（状态=审批中）<br>步骤2 返回 200<br>步骤3 返回 200（状态=已完成）<br>步骤4 状态为"已完成" |
| INT-LEAVE-002 | 请假审批拒绝 | 同上 | 1. 员工申请请假<br>2. 主管拾取<br>3. 主管调用 revert 归还任务（拒绝） | 请假状态为"已拒绝" |
| INT-LEAVE-003 | 员工撤销请假申请 | 请假申请已提交，未被拾取 | 1. POST /staff-leave/cancel 撤销<br>2. GET /staff-leave/{id} | 状态为"已撤销" |
| INT-LEAVE-004 | 请假通过 → 考勤标记 → 薪资扣款 | 假期扣款配置存在 | 1. 完整请假审批通过<br>2. GET /attendance/query?staffId=...&date=请假日期<br>3. GET /salary?month=请假月份 | 考勤标记为"休假"，薪资体现休假扣款 |

### 5.3 加班管理

| 用例编号 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 |
|:--|---------|---------|---------|---------|
| INT-OVER-001 | 设置加班 → 调休余额查询 | 员工存在 | 1. POST /overtime/set 设置加班规则<br>2. POST /staff-overtime/set 设置员工加班<br>3. GET /staff-overtime/time/off/{staffId} 查询调休 | 调休余额 = 加班小时数 × 调休系数 |
| INT-OVER-002 | 加班费 → 薪资联动 | 员工已有薪资 | 1. 为员工设置加班<br>2. GET /salary?month=当月 | 薪资中体现 overtimeSalary |

---

## 六、扣款配置管理（P1）

### 6.1 扣款标准 → 部门关联

| 用例编号 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 |
|:--|---------|---------|---------|---------|
| INT-DEDUCT-001 | 设置部门扣款标准 → 验证生效 | 部门存在 | 1. POST /salary-deduct/set 设置迟到扣款 = 50元<br>2. POST /leave/set 设置部门假期天数<br>3. 验证员工迟到一次扣款 50元 | 薪资计算正确使用扣款标准 |
| INT-DEDUCT-002 | 修改扣款标准 → 历史薪资不变 | 已有历史薪资记录 | 1. PUT /salary-deduct 修改扣款金额<br>2. 查询历史月份薪资 | 历史薪资不变，仅影响后续计算 |

---

## 七、数据导入导出（P1）

### 7.1 跨模块批量导入

| 用例编号 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 |
|:--|---------|---------|---------|---------|
| INT-IMP-001 | 顺序导入：城市 → 社保 → 薪资 | 员工已存在 | 1. POST /city/import 导入城市标准<br>2. POST /insurance/import 导入社保数据<br>3. POST /salary/import 导入薪资数据 | 三步按顺序成功，数据关联正确 |
| INT-IMP-002 | 跳过依赖导入 → 预期失败 | 城市数据不存在 | 1. POST /insurance/import 导入社保（cityId 引用不存在的城市） | 导入失败或成功但数据孤立 |
| INT-IMP-003 | 导入格式校验 | — | 1. 上传非 Excel 格式文件<br>2. 上传空文件<br>3. 上传格式正确但数据不合法的文件 | 各有对应的错误响应 |

---

## 八、文件管理（P1）

### 8.1 文件上传下载

| 用例编号 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 |
|:--|---------|---------|---------|---------|
| INT-DOCS-001 | 上传文件 → 关联文档记录 | — | 1. POST /docs 创建文档记录<br>2. POST /docs/upload/{docId} 上传文件<br>3. GET /docs/{docId} 验证关联 | 文档记录关联了文件路径 |
| INT-DOCS-002 | 删除文档 → 级联处理 | 文档已上传 | 1. DELETE /docs/{docId}<br>2. 尝试下载已删除文档 | 文档删除，下载失败 |

---

## 九、跨模块集成测试（P1）

### 9.1 完整员工生命周期

| 用例编号 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 |
|:--|---------|---------|---------|---------|
| INT-LIFECYCLE-001 | 入职 → 配置 → 发薪 → 离职 | 组织架构已建立 | 1. POST /staff 新增员工<br>2. POST /staff/set/{id} 分配角色<br>3. POST /insurance/set 设置社保<br>4. POST /salary/set 设置薪资<br>5. PUT /attendance/set 记录考勤<br>6. GET /salary 计算完整薪资<br>7. PUT /staff 设置 status=0（离职）<br>8. 离职员工尝试登录 | 步骤1-6 正常完成<br>步骤7 成功<br>步骤8 登录失败 |

### 9.2 数据一致性验证

| 用例编号 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 |
|:--|---------|---------|---------|---------|
| INT-CONSIST-001 | 删除员工 → 级联数据检查 | 员工有薪资/社保/考勤/角色 | 1. DELETE /staff/{id}<br>2. 查询 sal_salary 表中 staff_id 记录<br>3. 查询 soc_insurance 表中 staff_id 记录<br>4. 查询 per_staff_role 表中 staff_id 记录 | 逻辑删除：员工 is_deleted=1，关联表保留 |
| INT-CONSIST-002 | 修改关联数据 → 验证外键约束 | 员工社保引用了不存在的 cityId | 1. POST /insurance/set 设置 cityId=999999<br>2. 查询验证 | 实际行为取决于数据库外键约束 |

---

## 十、边界与异常场景（P2）

| 用例编号 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 |
|:--|---------|---------|---------|---------|
| INT-EDGE-001 | 重复提交（幂等性） | — | 连续两次 POST /city 完全相同的数据 | 第一次 200，第二次 200（因无唯一约束） |
| INT-EDGE-002 | 超大数据量导入 | 准备 10000+ 行的 Excel | POST /staff/import 上传大文件 | 导入成功，响应时间可接受 |
| INT-EDGE-003 | 特殊字符处理 | — | POST /staff name 含 emoji/特殊 Unicode | 数据正确存储 |
| INT-EDGE-004 | 事务回滚 | 薪资设置时中途失败 | POST /salary/set 后立即回滚 | 数据一致性，无部分写入 |

---

## 📊 集成测试用例统计

| 测试类别 | 优先级 | 用例数 | 涉及模块 |
|---------|:--:|:--:|------|
| 登录认证 | P0 | 3 | Login |
| 组织架构 | P0 | 4 | Staff + Dept + Role |
| 权限管理 | P0 | 3 | Menu + Role + Staff |
| 薪酬管理 | P0 | 4 | City + Insurance + Salary |
| 考勤扣款 | P0 | 4 | Attendance + Salary |
| 请假工作流 | P0 | 4 | StaffLeave(Activiti) + Attendance + Salary |
| 加班管理 | P0 | 2 | Overtime + StaffOvertime + Salary |
| 扣款配置 | P1 | 2 | SalaryDeduct + Leave |
| 数据导入 | P1 | 3 | 全模块 |
| 文件管理 | P1 | 2 | Docs |
| 完整生命周期 | P1 | 2 | 全模块 |
| 边界异常 | P2 | 4 | 全模块 |
| **合计** | — | **37** | — |

---

## 设计方法说明

- **业务流程驱动**：从用户视角出发，按"登录 → 配置 → 操作 → 结果验证"链路设计
- **跨模块依赖**：覆盖模块间的数据引用（如 Staff ← Insurance ← City，Attendance → Salary）
- **数据一致性**：验证 CRUD 操作后的级联影响
- **真实数据流**：使用真实的数据库和 Activiti 引擎（如适用），仅 Mock 外部系统（Redis）
- **排除纯读取**：GET 列表/详情/导出/下载等纯查询接口不单独测试，但在流程中用于结果验证

---

## 与单元测试的互补关系

| 维度 | 单元测试（missing.md） | 集成测试（本文档） |
|------|----------------------|-------------------|
| 粒度 | 单个 API 端点 | 跨 API 业务流程 |
| 关注点 | 参数校验、边界值、权限注解 | 数据流转、模块协作、一致性 |
| Mock 策略 | @MockBean Redis | 仅 Mock 外部服务 |
| 事务 | @Transactional 回滚 | 按需提交/回滚 |
| 用例数 | 116 | 37 |
| 适用阶段 | 开发阶段快速验证 | 提测前/回归测试 |
