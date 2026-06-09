# 缺失测试用例设计文档

> 设计方法：等价类划分 + 边界值分析
> 覆盖范围：P0(3) + P1(3) = 6 个 Controller，仅含前端可操作的数据交互 API
> 排除：纯读取（列表/详情/树形查询/导出）、已有测试覆盖的模块、P2 纯配置类模块

---

## 一、LoginController — 登录认证

### 1.1 POST /login/{validateCode} — 用户登录

#### 等价类划分

| 字段 | 有效等价类 | 无效等价类 |
|------|-----------|-----------|
| `code`（工号） | 非空字符串，存在于数据库中 | 空/null、不存在于数据库 |
| `password` | 非空字符串，与数据库中密码匹配 | 空/null、与数据库不匹配 |
| `validateCode`（路径参数） | 与图形验证码一致 | 空/null、与图形验证码不一致、已过期 |

#### 测试用例

| 编号 | 测试场景 | 等价类 | code | password | validateCode | 预期结果 |
|------|---------|--------|------|----------|-------------|---------|
| TC-LOGIN-001 | 正常登录 | 全部有效 | "admin" | "123" | 有效验证码 | code=200, 响应头含 Token |
| TC-LOGIN-002 | 用户名为空 | code 无效 | null | "123" | 有效验证码 | code=300 |
| TC-LOGIN-003 | 用户名为空字符串 | code 无效 | "" | "123" | 有效验证码 | code=300 |
| TC-LOGIN-004 | 密码为空 | password 无效 | "admin" | null | 有效验证码 | code=300 |
| TC-LOGIN-005 | 密码为空字符串 | password 无效 | "admin" | "" | 有效验证码 | code=300 |
| TC-LOGIN-006 | 验证码为空 | validateCode 无效 | "admin" | "123" | "" | code=300 |
| TC-LOGIN-007 | 用户名不存在 | code 无效 | "nonexistent_user" | "123" | 有效验证码 | code=300 |
| TC-LOGIN-008 | 密码错误 | password 无效 | "admin" | "wrong_pwd" | 有效验证码 | code=300 |
| TC-LOGIN-009 | 验证码错误 | validateCode 无效 | "admin" | "123" | "XXXX" | code=300 |
| TC-LOGIN-010 | 全部为空 | 全部无效 | null | null | "" | code=300 |

#### 边界值

| 编号 | 测试场景 | 边界说明 | 预期结果 |
|------|---------|---------|---------|
| TC-LOGIN-011 | 工号最小长度 (1字符) | code="a" | code=200 或 300（取决于是否存在） |
| TC-LOGIN-012 | 工号最大长度 (50字符) | code=50个字符 | code=200 或 300（取决于是否存在） |
| TC-LOGIN-013 | 工号超长 (51字符) | code=51个字符 | code=300 |
| TC-LOGIN-014 | 密码最小长度 (1字符) | password="1" | code=300（密码不匹配） |
| TC-LOGIN-015 | 工号含 SQL 注入 | code="admin' OR '1'='1" | code=300（防SQL注入） |

---

## 二、RoleController — 角色管理

### 2.1 POST /role — 新增角色

#### 等价类划分

| 字段 | 有效等价类 | 无效等价类 |
|------|-----------|-----------|
| `name` | 非空，1~20字符，不重复 | 空/null、重复、超长 |
| `remark` | 空/null 或 ≤200字符 | 超长（>200字符） |

#### 测试用例

| 编号 | 测试场景 | 等价类 | name | remark | 预期结果 |
|------|---------|--------|------|--------|---------|
| TC-ROLE-001 | 正常新增 | 全部有效 | "普通用户" | null | code=200 |
| TC-ROLE-002 | 无权限访问 | 权限无效 | — | — | status=403 |

#### 边界值

| 编号 | 测试场景 | 边界说明 | 预期结果 |
|------|---------|---------|---------|
| TC-ROLE-003 | 角色名最小长度 (1字符) | name="A" | code=200 |
| TC-ROLE-004 | 角色名边界长度 (20字符) | name=20个中文字符 | code=200 |
| TC-ROLE-005 | 角色名超长 (21字符) | name=21个字符 | code=300 |
| TC-ROLE-006 | 备注边界长度 (200字符) | remark=200字符 | code=200 |
| TC-ROLE-007 | 备注超长 (201字符) | remark=201字符 | code=300 |
| TC-ROLE-008 | 角色名含特殊字符 | name="测试\<script\>" | code=200 或 300 |

### 2.2 PUT /role — 编辑角色

| 编号 | 测试场景 | 等价类 | 预期结果 |
|------|---------|--------|---------|
| TC-ROLE-009 | 正常编辑（修改名称） | 有效 | code=200 |
| TC-ROLE-010 | 编辑时名称为空 | name 无效 | code=300 |

### 2.3 DELETE /role/batch/{ids} — 批量删除角色

| 编号 | 测试场景 | 等价类 | 预期结果 |
|------|---------|--------|---------|
| TC-ROLE-011 | 正常批量删除 | ids 有效 | code=200 |
| TC-ROLE-012 | 批量删除空列表 | ids=[] | code=200 或 300 |

### 2.4 POST /role/set/{id} — 为角色分配菜单

| 编号 | 测试场景 | 等价类 | menuIds | 预期结果 |
|------|---------|--------|---------|---------|
| TC-ROLE-013 | 正常分配菜单 | 有效 | [1,2,3] | code=200 |
| TC-ROLE-014 | 分配空菜单列表 | 有效边界 | [] | code=200 |

---

## 三、MenuController — 菜单管理

### 3.1 POST /menu — 新增菜单/按钮

#### 等价类划分

| 字段 | 有效等价类 | 无效等价类 |
|------|-----------|-----------|
| `name` | 可空，≤20字符 | 超长 |
| `code` | 非空，唯一，≤20字符 | 空/null、重复、超长 |
| `type` | 0(一级菜单)/1(二级页面)/2(权限点) | 非 0/1/2 |
| `parentId` | 0（根）或已存在的菜单ID | 不存在的菜单ID |
| `permission` | 非空（type=2时）如 "system:staff:add" | 空（type=2时） |

#### 测试用例

| 编号 | 测试场景 | 等价类 | 关键字段 | 预期结果 |
|------|---------|--------|---------|---------|
| TC-MENU-001 | 新增一级菜单 | 全部有效 | type=0, name="系统管理", code="system", parentId=0 | code=200 |
| TC-MENU-002 | 新增二级页面 | 全部有效 | type=1, name="员工管理", code="staff", parentId=有效父ID | code=200 |
| TC-MENU-003 | 新增权限点 | 全部有效 | type=2, code="staff:add", permission="system:staff:add", parentId=有效父ID | code=200 |
| TC-MENU-004 | 菜单名称为空 | name 有效 | name=null | code=200 |
| TC-MENU-005 | 菜单名称为空字符串 | name 有效 | name="" | code=200 |
| TC-MENU-006 | 编码为空 | code 无效 | code=null | code=300 |
| TC-MENU-007 | 编码重复 | code 无效 | code=已存在的编码 | code=300 |

#### 边界值

| 编号 | 测试场景 | 边界说明 | 预期结果 |
|------|---------|---------|---------|
| TC-MENU-008 | 名称最小长度 (1字符) | name="A" | code=200 |
| TC-MENU-009 | 名称最大长度 (20字符) | name=20个中文字符 | code=200 |
| TC-MENU-010 | 名称超长 (21字符) | name=21个字符 | code=300 |
| TC-MENU-011 | 编码最小长度 (1字符) | code="a" | code=200 |
| TC-MENU-012 | 编码最大长度 (20字符) | code=20字符 | code=200 |
| TC-MENU-013 | 编码超长 (21字符) | code=21个字符 | code=300 |
| TC-MENU-014 | permission 超长 (201字符) | permission=201字符 | code=300 |
| TC-MENU-015 | type 边界值 0 | type=0 | code=200 |
| TC-MENU-016 | type 边界值 2 | type=2 | code=200 |
| TC-MENU-017 | parentId=0 (根) | 边界值 | code=200 |
| TC-MENU-018 | 名称含特殊字符 | name="系统\<script\>" | code=200 或 300 |

### 3.2 PUT /menu — 编辑菜单

| 编号 | 测试场景 | 等价类 | 预期结果 |
|------|---------|--------|---------|
| TC-MENU-019 | 正常编辑 | 有效 | code=200 |
| TC-MENU-020 | 编辑时名称为空 | name 有效 | code=200 |

### 3.3 DELETE /menu/{id} — 删除菜单

| 编号 | 测试场景 | 等价类 | 预期结果 |
|------|---------|--------|---------|
| TC-MENU-021 | 正常删除 | id 有效 | code=200 |

### 3.4 DELETE /menu/batch/{ids} — 批量删除菜单

| 编号 | 测试场景 | 等价类 | 预期结果 |
|------|---------|--------|---------|
| TC-MENU-022 | 正常批量删除 | ids 有效 | code=200 |
| TC-MENU-023 | 空列表 | ids=[] | code=200 或 300 |

---

## 四、SalaryController — 薪资管理

### 4.1 POST /salary/set — 设置员工薪资

#### 等价类划分

| 字段 | 有效等价类 | 无效等价类 |
|------|-----------|-----------|
| `staffId` | 存在且在职的员工ID | 空/null、不存在 |
| `baseSalary` | BigDecimal ≥ 0 | null、负数 |
| `subsidy` | BigDecimal ≥ 0 或 null | 负数 |
| `bonus` | BigDecimal ≥ 0 或 null | 负数 |

#### 测试用例

| 编号 | 测试场景 | 等价类 | 关键字段 | 预期结果 |
|------|---------|--------|---------|---------|
| TC-SAL-001 | 正常设置（含全部字段） | 全部有效 | baseSalary=10000, subsidy=500, bonus=1000 | code=200 |
| TC-SAL-002 | 最小薪资设置 | 全部有效 | baseSalary=0, subsidy=0, bonus=0 | code=200 |
| TC-SAL-003 | 员工不存在 | staffId 无效 | staffId=999999 | code=300 |
| TC-SAL-004 | staffId 为空 | staffId 无效 | staffId=null | code=300 |

#### 边界值

| 编号 | 测试场景 | 边界说明 | 预期结果 |
|------|---------|---------|---------|
| TC-SAL-005 | 基础工资为0 | 值=0 边界 | code=200 |
| TC-SAL-006 | 基础工资高精度 (0.01) | 精度边界 | code=200 |
| TC-SAL-007 | 基础工资为负数 (-0.01) | 刚好为负 | code=300 |
| TC-SAL-008 | 补贴为0 | 值=0 边界 | code=200 |
| TC-SAL-009 | 补贴为负数 (-0.01) | 刚好为负 | code=300 |
| TC-SAL-010 | 奖金为0 | 值=0 边界 | code=200 |
| TC-SAL-011 | 奖金为负数 (-0.01) | 刚好为负 | code=300 |

### 4.2 POST /salary/import — 批量导入薪资

| 编号 | 测试场景 | 等价类 | 预期结果 |
|------|---------|--------|---------|
| TC-SAL-012 | 正常导入 Excel | 有效文件 | code=200 |
| TC-SAL-013 | 空文件上传 | 文件无效 | code=300 |
| TC-SAL-014 | 非 Excel 格式 (.txt) | 文件无效 | code=300 |

---

## 五、InsuranceController — 社保公积金管理

### 5.1 POST /insurance/set — 设置员工社保

#### 等价类划分

| 字段 | 有效等价类 | 无效等价类 |
|------|-----------|-----------|
| `staffId` | 存在且在职的员工ID | 空/null、不存在 |
| `cityId` | 存在的城市ID | 空/null、不存在 |
| `socialBase` | BigDecimal > 0，在城市社保基数上下限范围内 | null、≤0、超限 |
| `houseBase` | BigDecimal > 0，在城市公积金基数上下限范围内 | null、≤0、超限 |
| `perHouseRate` | 0.05 ~ 0.12 | null、<0.05、>0.12 |
| `comHouseRate` | 0.05 ~ 0.12 | null、<0.05、>0.12 |
| `comInjuryRate` | 0.002 ~ 0.019 | null、<0.002、>0.019 |

#### 测试用例

| 编号 | 测试场景 | 等价类 | 关键字段 | 预期结果 |
|------|---------|--------|---------|---------|
| TC-INS-001 | 正常设置社保 | 全部有效 | socialBase=15000, houseBase=15000, perHouseRate=0.05, comHouseRate=0.05 | code=200 |
| TC-INS-002 | 社保基数低于下限 | socialBase 无效 | socialBase=1000 | code=300 |
| TC-INS-003 | 社保基数高于上限 | socialBase 无效 | socialBase=50000 | code=300 |
| TC-INS-004 | 社保基数为负数 | socialBase 无效 | socialBase=-1000 | code=300 |
| TC-INS-005 | 公积金基数低于下限 | houseBase 无效 | houseBase=1000 | code=300 |
| TC-INS-006 | 公积金基数高于上限 | houseBase 无效 | houseBase=50000 | code=300 |
| TC-INS-007 | 公积金个人比例 < 0.05 | perHouseRate 无效 | perHouseRate=0.01 | code=300 |
| TC-INS-008 | 公积金个人比例 > 0.12 | perHouseRate 无效 | perHouseRate=0.13 | code=300 |
| TC-INS-009 | 公积金企业比例 < 0.05 | comHouseRate 无效 | comHouseRate=0.01 | code=300 |
| TC-INS-010 | 公积金企业比例 > 0.12 | comHouseRate 无效 | comHouseRate=0.13 | code=300 |
| TC-INS-011 | 工伤比例 < 0.002 | comInjuryRate 无效 | comInjuryRate=0.001 | code=300 |
| TC-INS-012 | 工伤比例 > 0.019 | comInjuryRate 无效 | comInjuryRate=0.02 | code=300 |

#### 边界值

| 编号 | 测试场景 | 边界说明 | 预期结果 |
|------|---------|---------|---------|
| TC-INS-013 | 社保基数 = 下限 (9000) | 刚好等于下限 | code=200 |
| TC-INS-014 | 社保基数 = 上限 (45000) | 刚好等于上限 | code=200 |
| TC-INS-015 | 社保基数 = 下限-0.01 (8999.99) | 刚好低于下限 | code=300 |
| TC-INS-016 | 社保基数 = 上限+0.01 (45000.01) | 刚好高于上限 | code=300 |
| TC-INS-017 | 公积金基数 = 下限 (10000) | 刚好等于下限 | code=200 |
| TC-INS-018 | 公积金基数 = 上限 (45000) | 刚好等于上限 | code=200 |
| TC-INS-019 | 公积金基数 = 下限-0.01 | 刚好低于下限 | code=300 |
| TC-INS-020 | 公积金个人比例 = 0.05 | 下边界 | code=200 |
| TC-INS-021 | 公积金个人比例 = 0.12 | 上边界 | code=200 |
| TC-INS-022 | 公积金个人比例 = 0.049 | 刚好低于下边界 | code=300 |
| TC-INS-023 | 工伤比例 = 0.002 | 下边界 | code=200 |
| TC-INS-024 | 工伤比例 = 0.019 | 上边界 | code=200 |
| TC-INS-025 | 工伤比例 = 0.001 | 刚好低于下边界 | code=300 |

### 5.2 POST /insurance/import — 批量导入社保

| 编号 | 测试场景 | 等价类 | 预期结果 |
|------|---------|--------|---------|
| TC-INS-026 | 正常导入 | 有效文件 | code=200 |
| TC-INS-027 | 空文件上传 | 无效文件 | code=300 |
| TC-INS-028 | 非 Excel 格式 | 无效文件 | code=300 |

---

## 六、CityController — 城市社保标准管理

### 6.1 POST /city — 新增城市标准

#### 等价类划分

| 字段 | 有效等价类 | 无效等价类 |
|------|-----------|-----------|
| `name` | 非空，唯一，≤50字符 | 空/null、重复、超长 |
| `averageSalary` | BigDecimal ≥ 500 | null、<500 |
| `lowerSalary` | BigDecimal > 0, < averageSalary | null、≤0、≥averageSalary |
| `socUpperLimit` | BigDecimal > socLowerLimit | null、≤ socLowerLimit |
| `socLowerLimit` | BigDecimal > 0, < socUpperLimit | null、≤0、≥ socUpperLimit |
| `houUpperLimit` | BigDecimal > houLowerLimit | null、≤ houLowerLimit |
| `houLowerLimit` | BigDecimal > 0, < houUpperLimit | null、≤0、≥ houUpperLimit |
| 各比例字段 | 0~1 | null、<0、>1 |

#### 测试用例

| 编号 | 测试场景 | 等价类 | 关键字段 | 预期结果 |
|------|---------|--------|---------|---------|
| TC-CITY-001 | 正常新增 | 全部有效 | name="北京市", 各字段合法 | code=200 |
| TC-CITY-002 | 城市名称为空 | name 无效 | name=null | code=300 |
| TC-CITY-003 | 城市名称重复 | name 无效 | name=已存在的城市 | code=300 |
| TC-CITY-004 | 最低工资 ≤ 499 | lowerSalary 无效 | lowerSalary=499 | code=300 |
| TC-CITY-005 | 最低工资为负数 | lowerSalary 无效 | lowerSalary=-1000 | code=300 |
| TC-CITY-006 | 最低工资 ≥ 平均工资 | averageSalary 无效 | lowerSalary=20000, averageSalary=15000 | code=300 |
| TC-CITY-007 | 比例 > 1 | 比例字段无效 | perPensionRate=1.5 | code=300 |

#### 边界值

| 编号 | 测试场景 | 边界说明 | 预期结果 |
|------|---------|---------|---------|
| TC-CITY-008 | 城市名最小长度 (1字符) | name="京" | code=200 |
| TC-CITY-009 | 城市名最大长度 (50字符) | name=50字符 | code=200 |
| TC-CITY-010 | 城市名超长 (51字符) | name=51字符 | code=300 |
| TC-CITY-011 | 平均工资 = 500 | 刚好等于下限 | code=200 |
| TC-CITY-012 | 平均工资 = 499.99 | 刚好低于下限 | code=300 |
| TC-CITY-013 | 最低工资 = 平均工资-0.01 | 刚好 < 平均工资 | code=200 |
| TC-CITY-014 | 最低工资 = 平均工资 | 等于边界 | code=300 |
| TC-CITY-015 | 比例 = 0 | 边界值 | code=200 |
| TC-CITY-016 | 比例 = 1 | 边界值（最大值） | code=200 |

### 6.2 PUT /city — 编辑城市标准

| 编号 | 测试场景 | 等价类 | 预期结果 |
|------|---------|--------|---------|
| TC-CITY-017 | 正常编辑 | 有效 | code=200 |
| TC-CITY-018 | 编辑时名称为空 | name 无效 | code=300 |

### 6.3 DELETE /city/{id} — 删除城市标准

| 编号 | 测试场景 | 等价类 | 预期结果 |
|------|---------|--------|---------|
| TC-CITY-019 | 正常删除 | id 有效 | code=200 |

### 6.4 POST /city/import — 批量导入城市

| 编号 | 测试场景 | 等价类 | 预期结果 |
|------|---------|--------|---------|
| TC-CITY-020 | 正常导入 | 有效文件 | code=200 |
| TC-CITY-021 | 空文件 | 无效文件 | code=300 |
| TC-CITY-022 | 非 Excel 格式 | 无效文件 | code=300 |

---

## 📊 用例统计汇总

| 模块 | 优先级 | 用例数 | 数据交互 API |
|------|:--:|:--:|------|
| LoginController | P0 | 15 | 登录 |
| RoleController | P0 | 14 | 新增/编辑/批量删除/分配菜单 |
| MenuController | P0 | 23 | 新增/编辑/删除/批量删除 |
| SalaryController | P1 | 14 | 设置薪资/导入 |
| InsuranceController | P1 | 28 | 设置社保/导入 |
| CityController | P1 | 22 | 新增/编辑/删除/导入 |
| **合计** | — | **116** | — |

---

## 设计方法说明

- **等价类划分**：每个输入字段划分为「有效等价类」和「无效等价类」，从每类中选取代表性数据进行测试
- **边界值分析**：对数值范围、字符串长度、枚举值等，选取刚好在边界上和刚好超出边界的值进行测试
- **覆盖原则**：每条用例标注归属的等价类类型，确保每个等价类至少被一条用例覆盖
