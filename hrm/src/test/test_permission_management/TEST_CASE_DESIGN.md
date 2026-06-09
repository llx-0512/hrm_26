# 权限管理模块 - 单元测试用例设计文档

## 🎯 测试范围

### 1. 员工管理 (Staff Management)
- 员工信息的增删改查（Service/Controller/Mapper三层）
- 员工状态管理（在职/离职/禁用）
- 部门分配
- 多条件分页查询
- 数据导入导出

### 2. 密码管理 (Password Management)
- 密码验证
- 密码重置
- 默认密码设置

### 3. 员工角色管理 (Staff Role Management)
- 为员工分配角色
- 查询员工的角色

### 4. 认证与授权 (Authentication & Authorization)
- JWT Token认证
- 方法级别权限控制 (@PreAuthorize)
- 未认证/无权限场景处理

---

## 📊 等价类划分

### 一、员工管理模块

#### 1.1 员工新增 (POST /staff)

| 输入项 | 有效等价类 | 无效等价类 |
|-------|-----------|-----------|
| **姓名 (name)** | 1. 2-20个字符<br>2. 中英文混合<br>3. 包含特殊字符（如欧阳·张三） | 1. 空值/null<br>2. 长度<2或>20<br>3. 纯数字<br>4. 包含非法字符（@#$%） |
| **性别 (gender)** | 1. MALE (0)<br>2. FEMALE (1) | 1. null<br>2. 其他值 (2, 3, -1) |
| **手机号 (phone)** | 1. 11位中国大陆手机号<br>2. 以13/14/15/16/17/18/19开头 | 1. 空值/null<br>2. 长度≠11<br>3. 非数字字符<br>4. 以0或非1开头<br>5. 重复手机号 |
| **地址 (address)** | 1. 5-100个字符<br>2. 包含省市区街道 | 1. 空值/null<br>2. 长度>100<br>3. 纯特殊字符 |
| **生日 (birthday)** | 1. 合法日期格式 (yyyy-MM-dd)<br>2. 年龄18-60岁 | 1. null (允许)<br>2. 未来日期<br>3. 格式错误<br>4. 年龄<18或>60 |
| **部门ID (deptId)** | 1. 数据库中存在的部门ID<br>2. 正整数 | 1. null<br>2. 不存在的部门ID<br>3. 负数或0 |
| **状态 (status)** | 1. 1 (在职)<br>2. 0 (离职)<br>3. 2 (禁用) | 1. null<br>2. 其他值 (3, 4, -1) |
| **备注 (remark)** | 1. 0-200个字符<br>2. 可为空 | 1. 长度>200 |

#### 1.2 员工删除 (DELETE /staff/{id})

| 输入项 | 有效等价类 | 无效等价类 |
|-------|-----------|-----------|
| **员工ID (id)** | 1. 数据库中存在的员工ID<br>2. 正整数 | 1. null<br>2. 不存在的ID<br>3. 负数或0<br>4. 非数字类型 |

#### 1.3 员工批量删除 (DELETE /staff/batch/{ids})

| 输入项 | 有效等价类 | 无效等价类 |
|-------|-----------|-----------|
| **员工ID列表 (ids)** | 1. 多个存在的员工ID<br>2. 单个ID<br>3. 空列表 | 1. null<br>2. 包含不存在的ID<br>3. 包含负数 |

#### 1.4 员工更新 (PUT /staff)

| 输入项 | 有效等价类 | 无效等价类 |
|-------|-----------|-----------|
| **员工ID (id)** | 1. 存在的员工ID | 1. null<br>2. 不存在的ID |
| **姓名 (name)** | 同新增 | 同新增 |
| **手机号 (phone)** | 同新增 | 同新增 |
| **状态变更** | 1. 在职→离职<br>2. 在职→禁用<br>3. 禁用→在职 | 1. 离职→在职 (不允许)<br>2. 无效状态值 |
| **部门变更** | 1. 切换到存在的部门 | 1. 切换到不存在的部门 |

#### 1.5 员工查询 (GET /staff/{id})

| 输入项 | 有效等价类 | 无效等价类 |
|-------|-----------|-----------|
| **员工ID (id)** | 1. 存在的员工ID | 1. 不存在的ID<br>2. 负数<br>3. 非数字 |

#### 1.6 员工列表查询 (GET /staff)

| 输入项 | 有效等价类 | 无效等价类 |
|-------|-----------|-----------|
| **当前页 (current)** | 1. 正整数 (≥1)<br>2. 默认值1 | 1. ≤0<br>2. 非数字 |
| **每页大小 (size)** | 1. 正整数 (1-100)<br>2. 默认值10 | 1. ≤0或>100<br>2. 非数字 |
| **姓名 (name)** | 1. 部分姓名（模糊查询）<br>2. 空字符串<br>3. null | 1. 超长字符串 (>50) |
| **生日 (birthday)** | 1. 合法日期格式<br>2. null | 1. 格式错误 |
| **部门ID (deptId)** | 1. 存在的部门ID<br>2. null | 1. 不存在的部门ID |
| **状态 (status)** | 1. 0/1/2<br>2. null | 1. 其他值 |

---

### 二、密码管理模块

#### 2.1 密码验证 (GET /staff/{pwd}/{id})

| 输入项 | 有效等价类 | 无效等价类 |
|-------|-----------|-----------|
| **员工ID (id)** | 1. 存在的员工ID | 1. 不存在的ID<br>2. 负数 |
| **密码 (pwd)** | 1. 正确的明文密码<br>2. 默认密码"123" | 1. 错误密码<br>2. 空字符串<br>3. null |

#### 2.2 密码重置 (PUT /staff/reset)

| 输入项 | 有效等价类 | 无效等价类 |
|-------|-----------|-----------|
| **员工ID (id)** | 1. 存在的员工ID | 1. 不存在的ID<br>2. null |
| **新密码 (password)** | 1. 6-20个字符<br>2. 包含字母和数字 | 1. 空值/null<br>2. 长度<6或>20<br>3. 纯数字或纯字母 |

---

### 三、员工角色管理模块

#### 3.1 为员工设置角色 (POST /staff/set/{id})

| 输入项 | 有效等价类 | 无效等价类 |
|-------|-----------|-----------|
| **员工ID (id)** | 1. 存在的员工ID | 1. 不存在的ID<br>2. null |
| **角色ID列表 (roleIds)** | 1. 多个存在的角色ID<br>2. 空列表（清空所有角色）<br>3. 单个ID | 1. null<br>2. 包含不存在的角色ID |

#### 3.2 查询员工的角色 (GET /staff/staff/{id})

| 输入项 | 有效等价类 | 无效等价类 |
|-------|-----------|-----------|
| **员工ID (id)** | 1. 存在的员工ID | 1. 不存在的ID<br>2. 负数 |

---

### 四、认证与授权

#### 4.1 JWT Token认证

| 输入项 | 有效等价类 | 无效等价类 |
|-------|-----------|-----------|
| **Token格式** | 1. Bearer {token}<br>2. 有效的JWT | 1. 缺少Bearer前缀<br>2. Token格式错误 |
| **Token有效期** | 1. 未过期 | 1. 已过期<br>2. 尚未生效 |
| **Token签名** | 1. 签名正确 | 1. 签名被篡改<br>2. 密钥不匹配 |

#### 4.2 权限验证

| 场景 | 有效等价类 | 无效等价类 |
|-----|-----------|-----------|
| **有权限** | 1. 用户拥有所需权限<br>2. 通过@PreAuthorize验证 | 1. 用户无此权限 |
| **无权限** | 1. 用户已认证但无权限 | 1. 返回403 Forbidden |
| **未认证** | 1. 未携带Token | 1. 返回401 Unauthorized |

---

## 🧪 测试用例设计

### 一、员工管理测试用例

#### TC-STAFF-001: 新增员工 - 正常场景 `testAdd_Success()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-001 |
| **用例名称** | 新增员工 - 所有字段合法 |
| **前置条件** | 1. 数据库中存在部门ID=1<br>2. 用户有 system:staff:add 权限 |
| **测试步骤** | 1. 发送 POST /staff 请求<br>2. 请求体包含合法的姓名、性别、手机号、部门ID等 |
| **测试数据** | ```json { "name": "张三", "gender": "MALE", "phone": "13800138000", "address": "北京市朝阳区", "birthday": "1990-01-01", "deptId": 1, "status": 1 } ``` |
| **预期结果** | 1. HTTP状态码: 200<br>2. 响应code: 200<br>3. 自动生成工号 (staff_{id})<br>4. 自动加密密码 (默认123) |

#### TC-STAFF-002: 新增员工 - 必填字段为空 `testAdd_WithNullName()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-002 |
| **用例名称** | 新增员工 - 姓名为null |
| **测试步骤** | 1. 发送 POST /staff 请求<br>2. name 字段为 null |
| **测试数据** | ```json { "name": null, "gender": "MALE", "phone": "13800138001" } ``` |
| **预期结果** | 1. HTTP状态码: 200 或 400<br>2. 响应code: 300 (失败)<br>3. 提示字段验证错误 |

#### TC-STAFF-003: 新增员工 - 验证默认密码设置 `testAdd_DefaultPassword()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-003 |
| **用例名称** | 新增员工 - 自动设置默认密码 |
| **测试步骤** | 1. 创建新员工<br>2. 验证密码字段已加密存储 |
| **预期结果** | 1. 响应code: 200<br>2. 数据库中密码字段不为空<br>3. 密码已BCrypt加密 |

#### TC-STAFF-004: 新增员工 - 验证工号自动生成 `testAdd_AutoGenerateCode()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-004 |
| **用例名称** | 新增员工 - 工号自动生成 |
| **测试步骤** | 1. 创建新员工（不提供code字段）<br>2. 查询该员工 |
| **预期结果** | 1. 响应code: 200<br>2. 工号格式为 staff_{id} |

#### TC-STAFF-005: 删除员工 - 成功场景 `testDelete_Success()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-005 |
| **用例名称** | 逻辑删除员工 |
| **前置条件** | 1. 存在员工ID=1<br>2. 用户有 system:staff:delete 权限 |
| **测试步骤** | 1. 发送 DELETE /staff/1 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 响应code: 200<br>3. 数据库中 is_deleted=1<br>4. 再次查询该员工返回null |

#### TC-STAFF-006: 删除员工 - 不存在的ID `testDelete_NonExistentId()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-006 |
| **用例名称** | 删除不存在的员工 |
| **测试步骤** | 1. 发送 DELETE /staff/999999 请求 |
| **预期结果** | 1. 响应code: 300<br>2. 提示删除失败 |

#### TC-STAFF-007: 批量删除员工 - 成功场景 `testDeleteBatch_Success()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-007 |
| **用例名称** | 批量逻辑删除多个员工 |
| **测试步骤** | 1. 发送 DELETE /staff/batch/1,2,3 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 三个员工的 is_deleted=1 |

#### TC-STAFF-008: 批量删除员工 - 空列表 `testDeleteBatch_EmptyList()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-008 |
| **用例名称** | 批量删除空ID列表 |
| **测试步骤** | 1. 发送 DELETE /staff/batch/ （空列表） |
| **预期结果** | 1. 响应code: 200 或 300<br>2. 根据业务逻辑处理 |

#### TC-STAFF-009: 更新员工 - 修改基本信息 `testEdit_Success()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-009 |
| **用例名称** | 更新员工姓名和电话 |
| **前置条件** | 存在员工ID=1 |
| **测试步骤** | 1. 发送 PUT /staff 请求<br>2. 修改 name 和 phone |
| **测试数据** | ```json { "id": 1, "name": "张三丰", "phone": "13900139000" } ``` |
| **预期结果** | 1. HTTP状态码: 200<br>2. 响应code: 200<br>3. 数据库中信息已更新 |

#### TC-STAFF-010: 更新员工 - 更新状态为禁用 `testEdit_UpdateStatusToDisabled()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-010 |
| **用例名称** | 禁用员工账号 |
| **测试数据** | ```json { "id": 1, "status": 2 } ``` |
| **预期结果** | 1. 响应code: 200<br>2. 员工状态变为禁用<br>3. 员工无法登录 |

#### TC-STAFF-011: 更新员工 - 更新部门 `testEdit_UpdateDepartment()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-011 |
| **用例名称** | 将员工调到其他部门 |
| **测试数据** | ```json { "id": 1, "deptId": 2 } ``` |
| **预期结果** | 1. 响应code: 200<br>2. 部门ID更新成功 |

#### TC-STAFF-012: 更新员工 - 不存在的员工 `testEdit_NonExistentStaff()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-012 |
| **用例名称** | 更新不存在的员工 |
| **测试数据** | ```json { "id": 999999, "name": "测试" } ``` |
| **预期结果** | 1. 响应code: 300<br>2. 提示员工不存在 |

#### TC-STAFF-013: 查询员工 - 根据ID（存在） `testQueryById_Success()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-013 |
| **用例名称** | 查询存在的员工 |
| **测试步骤** | 1. 发送 GET /staff/1 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 返回员工详细信息 |

#### TC-STAFF-014: 查询员工 - 根据ID（不存在） `testQueryById_NonExistentId()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-014 |
| **用例名称** | 查询不存在的员工 |
| **测试步骤** | 1. 发送 GET /staff/999999 请求 |
| **预期结果** | 1. 响应code: 300<br>2. data为null |

#### TC-STAFF-015: 员工列表 - 无条件分页查询 `testList_NoConditions()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-015 |
| **用例名称** | 查询所有员工（分页） |
| **测试步骤** | 1. 发送 GET /staff?current=1&size=10 请求 |
| **预期结果** | 1. 返回分页数据<br>2. 包含 pages, total, list 字段<br>3. list中包含员工+部门名称+年龄 |

#### TC-STAFF-016: 员工列表 - 按姓名模糊查询 `testList_ByName()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-016 |
| **用例名称** | 搜索姓"张"的员工 |
| **测试步骤** | 1. 发送 GET /staff?name=张 请求 |
| **预期结果** | 1. 返回所有姓名包含"张"的员工<br>2. 支持模糊匹配 |

#### TC-STAFF-017: 员工列表 - 按部门查询 `testList_ByDeptId()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-017 |
| **用例名称** | 查询指定部门的员工 |
| **测试步骤** | 1. 发送 GET /staff?deptId=1 请求 |
| **预期结果** | 1. 只返回 deptId=1 的员工 |

#### TC-STAFF-018: 员工列表 - 按状态查询 `testList_ByStatus()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-018 |
| **用例名称** | 查询在职员工 |
| **测试步骤** | 1. 发送 GET /staff?status=1 请求 |
| **预期结果** | 1. 只返回 status=1 的员工 |

#### TC-STAFF-019: 员工列表 - 组合条件查询 `testList_CombinedConditions()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-019 |
| **用例名称** | 多条件组合查询 |
| **测试步骤** | 1. 发送 GET /staff?name=张&deptId=1&status=1 请求 |
| **预期结果** | 1. 返回同时满足三个条件的员工 |

#### TC-STAFF-020: 员工列表 - 分页功能验证 `testList_Pagination()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-020 |
| **用例名称** | 验证分页参数有效性 |
| **测试步骤** | 1. 发送 GET /staff?current=1&size=5 请求<br>2. 发送 GET /staff?current=2&size=5 请求 |
| **预期结果** | 1. 第一页和第二页数据不重复<br>2. 总记录数一致 |

#### TC-STAFF-021: 查询员工详细信息 `testQueryInfo_Success()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-021 |
| **用例名称** | 查询员工详细信息（含部门名称） |
| **测试步骤** | 1. 发送 GET /staff/info/1 请求 |
| **预期结果** | 1. 响应code: 200<br>2. 包含完整的员工信息<br>3. 包含部门名称 |

#### TC-STAFF-022: 查询员工详细信息 - 包含年龄计算 `testQueryInfo_WithAge()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-022 |
| **用例名称** | 验证年龄自动计算 |
| **测试步骤** | 1. 查询员工详细信息<br>2. 验证age字段 |
| **预期结果** | 1. age = 当前年份 - 出生年份<br>2. 年龄计算准确 |

#### TC-STAFF-023: 新增员工 - 手机号重复 `testAdd_DuplicatePhone()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-023 |
| **用例名称** | 新增员工 - 使用已存在的手机号 |
| **测试步骤** | 1. 先创建一个员工A (phone: 13800138000)<br>2. 再创建员工B，使用相同手机号 |
| **预期结果** | 1. 根据业务逻辑：<br>   - 允许重复：成功<br>   - 不允许重复：失败，提示手机号已存在 |

#### TC-STAFF-024: 更新员工 - 性别枚举值 `testEdit_GenderEnum()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-024 |
| **用例名称** | 修改员工性别 |
| **测试步骤** | 1. 创建员工（性别=MALE）<br>2. 更新性别为FEMALE |
| **预期结果** | 1. 响应code: 200<br>2. 性别更新成功 |

#### TC-STAFF-025: 查询员工 - 特殊字符姓名 `testQuery_SpecialCharacterName()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-025 |
| **用例名称** | 搜索包含特殊字符的姓名 |
| **测试步骤** | 1. 创建员工（姓名=欧阳·测试）<br>2. 发送 GET /staff?name=欧阳 请求 |
| **预期结果** | 1. 响应code: 200<br>2. 能搜索到该员工 |

#### TC-STAFF-026: 员工列表 - 空结果集 `testList_EmptyResult()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-STAFF-026 |
| **用例名称** | 查询不存在的姓名 |
| **测试步骤** | 1. 发送 GET /staff?name=不存在的名字 请求 |
| **预期结果** | 1. 响应code: 200<br>2. list为空数组<br>3. total=0 |

---

### 三、Controller层API接口测试用例

#### TC-AUTH-001: 新增员工 - 有权限 `testAdd_Success()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-AUTH-001 |
| **用例名称** | 有 system:staff:add 权限时新增员工 |
| **前置条件** | 用户拥有 system:staff:add 权限 |
| **测试步骤** | 1. 使用 @WithMockUser(username="admin", authorities={"system:staff:add"})<br>2. 发送 POST /staff 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 响应code: 200<br>3. 员工创建成功 |

#### TC-AUTH-002: 新增员工 - 无权限 `testAdd_NoPermission()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-AUTH-002 |
| **用例名称** | 无 system:staff:add 权限时新增员工 |
| **前置条件** | 用户没有 system:staff:add 权限 |
| **测试步骤** | 1. 使用 @WithMockUser(username="user", authorities={})<br>2. 发送 POST /staff 请求 |
| **预期结果** | 1. HTTP状态码: 403<br>2. 响应code: 1300<br>3. 提示没有权限 |

#### TC-AUTH-003: 新增员工 - 未认证 `testAdd_Unauthenticated()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-AUTH-003 |
| **用例名称** | 未登录时访问员工接口 |
| **测试步骤** | 1. 不使用 @WithMockUser 注解<br>2. 发送 POST /staff 请求 |
| **预期结果** | 1. HTTP状态码: 401<br>2. 响应code: 1200<br>3. 提示认证失败 |

#### TC-AUTH-004: 删除员工 - 有权限 `testDelete_Success()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-AUTH-004 |
| **用例名称** | 有 system:staff:delete 权限时删除员工 |
| **前置条件** | 用户拥有 system:staff:delete 权限 |
| **测试步骤** | 1. 使用 @WithMockUser(authorities={"system:staff:delete"})<br>2. 发送 DELETE /staff/1 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 删除成功 |

#### TC-AUTH-005: 删除员工 - 无权限 `testDelete_NoPermission()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-AUTH-005 |
| **用例名称** | 无 system:staff:delete 权限时删除员工 |
| **测试步骤** | 1. 使用 @WithMockUser(authorities={})<br>2. 发送 DELETE /staff/1 请求 |
| **预期结果** | 1. HTTP状态码: 403 |

#### TC-AUTH-006: 编辑员工 - 有权限 `testEdit_Success()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-AUTH-006 |
| **用例名称** | 有 system:staff:edit 权限时编辑员工 |
| **前置条件** | 用户拥有 system:staff:edit 权限 |
| **测试步骤** | 1. 使用 @WithMockUser(authorities={"system:staff:edit"})<br>2. 发送 PUT /staff 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 更新成功 |

#### TC-AUTH-007: 编辑员工 - 无权限 `testEdit_NoPermission()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-AUTH-007 |
| **用例名称** | 无 system:staff:edit 权限时编辑员工 |
| **测试步骤** | 1. 使用 @WithMockUser(authorities={})<br>2. 发送 PUT /staff 请求 |
| **预期结果** | 1. HTTP状态码: 403 |

#### TC-AUTH-008: 查询员工列表 - 有权限 `testList_NoConditions()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-AUTH-008 |
| **用例名称** | 有 system:staff:list 权限时查询员工列表 |
| **前置条件** | 用户拥有 system:staff:list 权限 |
| **测试步骤** | 1. 使用 @WithMockUser(authorities={"system:staff:list"})<br>2. 发送 GET /staff 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 返回员工列表 |

#### TC-AUTH-009: 查询员工列表 - 无权限 `testList_NoPermission()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-AUTH-009 |
| **用例名称** | 无 system:staff:list 权限时查询员工列表 |
| **测试步骤** | 1. 使用 @WithMockUser(authorities={})<br>2. 发送 GET /staff 请求 |
| **预期结果** | 1. HTTP状态码: 403 |

#### TC-CTRL-010: 分页参数 - 默认值 `testList_DefaultPagination()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-CTRL-010 |
| **用例名称** | 不提供分页参数时使用默认值 |
| **测试步骤** | 1. 发送 GET /staff 请求（不带current和size参数） |
| **预期结果** | 1. HTTP状态码: 200<br>2. 使用默认分页参数（current=1, size=10） |

#### TC-CTRL-011: 分页参数 - 指定页码和大小 `testList_CustomPagination()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-CTRL-011 |
| **用例名称** | 自定义分页参数查询 |
| **测试步骤** | 1. 发送 GET /staff?current=2&size=5 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 返回第2页，每页5条记录 |

#### TC-CTRL-012: 删除员工接口 - 成功场景 `testDelete_Success()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-CTRL-012 |
| **用例名称** | 删除存在的员工 |
| **前置条件** | 用户有 system:staff:delete 权限 |
| **测试步骤** | 1. 发送 DELETE /staff/1 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 响应code: 200<br>3. 员工被逻辑删除 |

#### TC-CTRL-013: 批量删除员工接口 - 成功场景 `testDeleteBatch_Success()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-CTRL-013 |
| **用例名称** | 批量删除多个员工 |
| **前置条件** | 用户有 system:staff:delete 权限 |
| **测试步骤** | 1. 发送 DELETE /staff/batch 请求<br>2. 请求体包含ID列表 |
| **测试数据** | ```json [1, 2, 3] ``` |
| **预期结果** | 1. HTTP状态码: 200<br>2. 所有员工被逻辑删除 |

#### TC-CTRL-014: 编辑员工接口 - 成功场景 `testEdit_Success()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-CTRL-014 |
| **用例名称** | 更新员工信息 |
| **前置条件** | 用户有 system:staff:edit 权限 |
| **测试步骤** | 1. 发送 PUT /staff 请求<br>2. 请求体包含更新的字段 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 员工信息更新成功 |

#### TC-CTRL-015: 查询员工ByID - 不存在的ID `testQueryById_NonExistent()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-CTRL-015 |
| **用例名称** | 查询不存在的员工ID |
| **测试步骤** | 1. 发送 GET /staff/999999 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 响应code: 300<br>3. 提示员工不存在 |

#### TC-CTRL-016: 查询员工详细信息接口 `testQueryInfo_Success()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-CTRL-016 |
| **用例名称** | 查询员工详细信息（含部门名称） |
| **测试步骤** | 1. 发送 GET /staff/info/1 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 返回StaffDeptVO<br>3. 包含deptName字段 |

#### TC-CTRL-017: 多条件分页查询 - 无条件 `testList_NoConditions()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-CTRL-017 |
| **用例名称** | 不带任何条件的分页查询 |
| **测试步骤** | 1. 发送 GET /staff?current=1&size=10 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 返回所有员工 |

#### TC-CTRL-018: 多条件分页查询 - 按姓名查询 `testList_ByName()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-CTRL-018 |
| **用例名称** | 按姓名模糊查询员工 |
| **测试步骤** | 1. 发送 GET /staff?name=张&current=1&size=10 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 返回姓名包含“张”的员工 |

#### TC-CTRL-019: 多条件分页查询 - 按部门查询 `testList_ByDeptId()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-CTRL-019 |
| **用例名称** | 按部门ID查询员工 |
| **测试步骤** | 1. 发送 GET /staff?deptId=1&current=1&size=10 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 返回指定部门的员工 |

#### TC-CTRL-020: 多条件分页查询 - 按状态查询 `testList_ByStatus()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-CTRL-020 |
| **用例名称** | 按员工状态查询 |
| **测试步骤** | 1. 发送 GET /staff?status=1&current=1&size=10 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 返回指定状态的员工 |

#### TC-CTRL-021: 多条件分页查询 - 组合条件 `testList_CombinedConditions()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-CTRL-021 |
| **用例名称** | 多条件组合查询员工 |
| **测试步骤** | 1. 发送 GET /staff?name=张&deptId=1&status=1&current=1&size=10 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 返回满足所有条件的员工 |

---

### 四、密码管理测试用例

#### TC-PWD-001: 验证密码 - 正确密码 `testValidate_CorrectPassword()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-PWD-001 |
| **用例名称** | 验证正确的密码 |
| **前置条件** | 存在员工ID=1，密码为"123" |
| **测试步骤** | 1. 发送 GET /staff/123/1 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 响应code: 200<br>3. 密码验证通过 |

#### TC-PWD-002: 验证密码 - 错误密码 `testValidate_WrongPassword()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-PWD-002 |
| **用例名称** | 验证错误的密码 |
| **测试步骤** | 1. 发送 GET /staff/wrong_password/1 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 响应code: 300<br>3. 密码验证失败 |

#### TC-PWD-003: 重置密码 - 成功场景 `testReset_Success()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-PWD-003 |
| **用例名称** | 重置员工密码 |
| **测试步骤** | 1. 发送 PUT /staff/reset 请求<br>2. 请求体包含员工ID和新密码 |
| **测试数据** | ```json { "id": 1, "password": "new_password" } ``` |
| **预期结果** | 1. HTTP状态码: 200<br>2. 响应code: 200<br>3. 密码更新成功 |

---

### 五、员工角色管理测试用例

#### TC-ROLE-001: 为员工设置角色 - 成功场景 `testSetRole_Success()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-ROLE-001 |
| **用例名称** | 为员工分配多个角色 |
| **前置条件** | 1. 存在员工ID=1<br>2. 存在角色ID=1,2<br>3. 用户有 system:staff:set_role 权限 |
| **测试步骤** | 1. 发送 POST /staff/set/1 请求<br>2. 请求体包含角色ID列表 |
| **测试数据** | ```json [1, 2] ``` |
| **预期结果** | 1. HTTP状态码: 200<br>2. 响应code: 200<br>3. 员工与角色关联成功<br>4. 旧的关联被清除 |

#### TC-ROLE-002: 查询员工的角色 `testQueryByStaffId_Success()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-ROLE-002 |
| **用例名称** | 查询员工已分配的角色 |
| **测试步骤** | 1. 发送 GET /staff/staff/1 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 返回员工关联的角色ID列表 |

---

### 六、Mapper层SQL查询测试用例

#### TC-MAPPER-001: insert - 插入员工 `testInsert()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-001 |
| **用例名称** | 使用BaseMapper插入员工记录 |
| **测试步骤** | 1. 创建Staff对象<br>2. 调用 staffMapper.insert(staff) |
| **预期结果** | 1. 返回影响行数 > 0<br>2. staff.getId() 不为null |

#### TC-MAPPER-002: selectById - 根据ID查询（存在） `testSelectById()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-002 |
| **用例名称** | 查询存在的员工ID |
| **测试步骤** | 1. 调用 staffMapper.selectById(1) |
| **预期结果** | 1. 返回Staff对象<br>2. 字段值正确 |

#### TC-MAPPER-003: selectById - 根据ID查询（不存在） `testSelectById_NonExistent()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-003 |
| **用例名称** | 查询不存在的员工ID |
| **测试步骤** | 1. 调用 staffMapper.selectById(999999) |
| **预期结果** | 1. 返回null |

#### TC-MAPPER-004: updateById - 更新员工 `testUpdateById()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-004 |
| **用例名称** | 使用BaseMapper更新员工信息 |
| **测试步骤** | 1. 查询员工<br>2. 修改字段<br>3. 调用 staffMapper.updateById(staff) |
| **预期结果** | 1. 返回影响行数 > 0<br>2. 数据库中数据已更新 |

#### TC-MAPPER-005: deleteById - 逻辑删除 `testDeleteById()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-005 |
| **用例名称** | 逻辑删除员工记录 |
| **测试步骤** | 1. 调用 staffMapper.deleteById(id) |
| **预期结果** | 1. 返回影响行数 > 0<br>2. is_deleted=1<br>3. selectById返回null |

#### TC-MAPPER-006: selectList - 查询所有未删除的员工 `testSelectList()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-006 |
| **用例名称** | 查询所有is_deleted=0的员工 |
| **测试步骤** | 1. 调用 staffMapper.selectList(null) |
| **预期结果** | 1. 返回List<Staff><br>2. 不包含已删除的员工 |

#### TC-MAPPER-007: listStaffAttendanceVO - 按姓名模糊查询 `testListStaffAttendanceVO()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-007 |
| **用例名称** | 根据姓名模糊查询考勤员工 |
| **测试步骤** | 1. 调用 staffMapper.listStaffAttendanceVO(page, "张") |
| **预期结果** | 1. 返回IPage<StaffAttendanceVO><br>2. 姓名包含"张"的员工 |

#### TC-MAPPER-008: listStaffAttendanceVO - 无匹配结果 `testListStaffAttendanceVO_NoMatch()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-008 |
| **用例名称** | 查询不存在的姓名 |
| **测试步骤** | 1. 调用 staffMapper.listStaffAttendanceVO(page, "不存在的名字") |
| **预期结果** | 1. 返回IPage<StaffAttendanceVO><br>2. records为空列表<br>3. total=0 |

#### TC-MAPPER-009: listStaffDeptAttendanceVO - 按部门和姓名查询 `testListStaffDeptAttendanceVO()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-009 |
| **用例名称** | 根据部门ID和姓名查询考勤员工 |
| **测试步骤** | 1. 调用 staffMapper.listStaffDeptAttendanceVO(page, deptId, name) |
| **预期结果** | 1. 返回指定部门的员工<br>2. 姓名匹配的员工 |

#### TC-MAPPER-010: queryAttendanceMonthVO - 查询所有员工用于月考勤报表 `testQueryAttendanceMonthVO()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-010 |
| **用例名称** | 查询所有员工用于生成月考勤报表 |
| **测试步骤** | 1. 调用 staffMapper.queryAttendanceMonthVO() |
| **预期结果** | 1. 返回List<AttendanceMonthVO><br>2. 包含所有在职员工 |

#### TC-MAPPER-011: queryByCode - 根据工号查询（存在） `testQueryByCode()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-011 |
| **用例名称** | 根据工号查询员工 |
| **测试步骤** | 1. 调用 staffMapper.queryByCode("staff_1") |
| **预期结果** | 1. 返回StaffDeptVO<br>2. 工号匹配的员工 |

#### TC-MAPPER-012: queryByCode - 根据工号查询（不存在） `testQueryByCode_NonExistent()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-012 |
| **用例名称** | 查询不存在的工号 |
| **测试步骤** | 1. 调用 staffMapper.queryByCode("not_exist") |
| **预期结果** | 1. 返回null |

#### TC-MAPPER-013: queryInfo - 查询员工详细信息 `testQueryInfo()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-013 |
| **用例名称** | 查询员工详细信息（含部门名称） |
| **测试步骤** | 1. 调用 staffMapper.queryInfo(staffId) |
| **预期结果** | 1. 返回StaffDeptVO<br>2. 包含deptName字段 |

#### TC-MAPPER-014: queryInfo - 包含完整字段 `testQueryInfo_FullFields()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-014 |
| **用例名称** | 验证查询结果包含所有必要字段 |
| **测试步骤** | 1. 调用 staffMapper.queryInfo(staffId)<br>2. 验证各字段不为null |
| **预期结果** | 1. id, name, gender, phone等字段都有值<br>2. deptName不为空 |

#### TC-MAPPER-015: queryStaffDeptVO - 查询所有员工部门视图 `testQueryStaffDeptVO()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-015 |
| **用例名称** | 查询所有员工的部门视图信息 |
| **测试步骤** | 1. 调用 staffMapper.queryStaffDeptVO() |
| **预期结果** | 1. 返回List<StaffDeptVO><br>2. 每个员工都包含部门信息 |

#### TC-MAPPER-016: listStaffOvertimeVO - 按姓名查询加班员工 `testListStaffOvertimeVO()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-016 |
| **用例名称** | 根据姓名查询加班员工列表 |
| **测试步骤** | 1. 调用 staffMapper.listStaffOvertimeVO(name) |
| **预期结果** | 1. 返回List<StaffOvertimeVO><br>2. 姓名匹配的员工 |

#### TC-MAPPER-017: listStaffDeptOvertimeVO - 按部门和姓名查询加班员工 `testListStaffDeptOvertimeVO()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-017 |
| **用例名称** | 根据部门ID和姓名查询加班员工 |
| **测试步骤** | 1. 调用 staffMapper.listStaffDeptOvertimeVO(deptId, name) |
| **预期结果** | 1. 返回指定部门的加班员工<br>2. 姓名匹配 |

#### TC-MAPPER-018: queryOvertimeMonthVO - 查询所有员工用于月加班报表 `testQueryOvertimeMonthVO()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-018 |
| **用例名称** | 查询所有员工用于生成月加班报表 |
| **测试步骤** | 1. 调用 staffMapper.queryOvertimeMonthVO() |
| **预期结果** | 1. 返回List<OvertimeMonthVO><br>2. 包含所有在职员工 |

#### TC-MAPPER-019: queryByRole - 根据角色代码查询员工 `testQueryByRole()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-019 |
| **用例名称** | 根据角色代码查询拥有该角色的员工 |
| **测试步骤** | 1. 调用 staffMapper.queryByRole("admin") |
| **预期结果** | 1. 返回List<Staff><br>2. 所有员工都拥有admin角色 |

#### TC-MAPPER-020: listStaffAttendanceVO - 空字符串姓名 `testListStaffAttendanceVO_EmptyName()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-020 |
| **用例名称** | 使用空字符串作为姓名查询 |
| **测试步骤** | 1. 调用 staffMapper.listStaffAttendanceVO(page, "") |
| **预期结果** | 1. 返回所有员工（等同于无条件查询） |

#### TC-MAPPER-021: listStaffAttendanceVO - null姓名 `testListStaffAttendanceVO_NullName()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-021 |
| **用例名称** | 使用null作为姓名查询 |
| **测试步骤** | 1. 调用 staffMapper.listStaffAttendanceVO(page, null) |
| **预期结果** | 1. 返回所有员工（忽略姓名条件） |

#### TC-MAPPER-022: 分页查询 - 第一页 `testPagination_FirstPage()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-022 |
| **用例名称** | 查询第一页数据 |
| **测试步骤** | 1. 调用 staffMapper.listStaffAttendanceVO(new Page<>(1, 10), null) |
| **预期结果** | 1. 返回最多10条记录<br>2. current=1, size=10 |

#### TC-MAPPER-023: 分页查询 - 第二页 `testPagination_SecondPage()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-023 |
| **用例名称** | 查询第二页数据 |
| **测试步骤** | 1. 调用 staffMapper.listStaffAttendanceVO(new Page<>(2, 10), null) |
| **预期结果** | 1. 返回第11-20条记录<br>2. 与第一页数据不重复 |

#### TC-MAPPER-024: 特殊字符姓名查询 `testQuery_SpecialCharacters()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-024 |
| **用例名称** | 查询包含特殊字符的姓名 |
| **测试步骤** | 1. 创建员工（姓名=欧阳·测试）<br>2. 调用 staffMapper.listStaffAttendanceVO(page, "欧阳") |
| **预期结果** | 1. 能搜索到该员工<br>2. 支持中文特殊字符 |

#### TC-MAPPER-025: 中文姓名模糊查询 `testQuery_ChineseFuzzySearch()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-025 |
| **用例名称** | 使用中文关键词模糊查询 |
| **测试步骤** | 1. 调用 staffMapper.listStaffAttendanceVO(page, "测试") |
| **预期结果** | 1. 返回所有姓名包含"测试"的员工<br>2. 模糊匹配正确 |

#### TC-MAPPER-026: JOIN查询 - 验证部门名称正确性 `testJoinQuery_DeptName()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-026 |
| **用例名称** | 验证LEFT JOIN查询返回正确的部门名称 |
| **测试步骤** | 1. 调用 staffMapper.queryInfo(staffId)<br>2. 验证deptName字段 |
| **预期结果** | 1. deptName不为null<br>2. deptName不为空字符串<br>3. 与数据库中的部门名称一致 |

#### TC-MAPPER-027: LEFT JOIN - 员工可能没有部门 `testLeftJoin_WithoutDept()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-027 |
| **用例名称** | 查询没有部门的员工（deptId=null） |
| **测试步骤** | 1. 创建员工（deptId=null）<br>2. 调用 staffMapper.queryInfo(staffId) |
| **预期结果** | 1. 返回StaffDeptVO<br>2. name字段有值<br>3. deptName可能为null |

#### TC-MAPPER-028: 批量查询性能测试 `testBatchQueryPerformance()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-MAPPER-028 |
| **用例名称** | 测试批量查询50条记录的性能 |
| **测试步骤** | 1. 插入50条员工数据<br>2. 调用 staffMapper.queryStaffDeptVO()<br>3. 记录查询耗时 |
| **预期结果** | 1. 返回至少50条记录<br>2. 查询耗时 < 1秒<br>3. 性能符合预期 |

---

### 七、异常场景测试用例

#### TC-EXCEPTION-001: 新增员工 - 无效JSON `testAdd_InvalidJson()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-EXCEPTION-001 |
| **用例名称** | 发送无效的JSON格式 |
| **测试步骤** | 1. 发送 POST /staff 请求<br>2. 请求体为 {invalid json} |
| **预期结果** | 1. HTTP状态码: 400<br>2. 提示JSON格式错误 |

#### TC-EXCEPTION-002: 查询员工 - 无效的ID格式 `testQuery_InvalidIdFormat()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-EXCEPTION-002 |
| **用例名称** | 使用非数字ID查询员工 |
| **测试步骤** | 1. 发送 GET /staff/abc 请求 |
| **预期结果** | 1. HTTP状态码: 400<br>2. 提示参数类型错误 |

#### TC-EXCEPTION-003: 分页查询 - 无效的页码 `testList_InvalidPageNumber()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-EXCEPTION-003 |
| **用例名称** | 使用负数页码查询 |
| **测试步骤** | 1. 发送 GET /staff?current=-1&size=10 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. MyBatis-Plus可能会处理负数或使用默认值 |

#### TC-EXCEPTION-004: 分页查询 - 无效的页面大小 `testList_InvalidPageSize()`

| 项目 | 内容 |
|-----|------|
| **用例编号** | TC-EXCEPTION-004 |
| **用例名称** | 使用0作为页面大小 |
| **测试步骤** | 1. 发送 GET /staff?current=1&size=0 请求 |
| **预期结果** | 1. HTTP状态码: 200<br>2. 可能使用默认值或返回错误 |

---

## 📈 测试覆盖统计

### 实际测试用例统计（已实现）

| 模块 | 测试文件 | Service层 | Controller层 | Mapper层 | 总计 |
|-----|---------|----------|-------------|---------|------|
| **员工管理** | StaffServiceTest<br>StaffControllerTest<br>StaffMapperTest | 26个 | 28个 | 28个 | **82个** |
| **总计** | **3个文件** | **26个** | **28个** | **28个** | **82个** |

### 测试类型分布（已实现的82个用例）

| 测试类型 | 用例数量 | 说明 |
|---------|---------|------|
| 功能测试 | 45个 | 增删改查核心功能 |
| 边界值测试 | 12个 | 边界值、空值、极限值 |
| 异常测试 | 10个 | 错误输入、异常情况 |
| 权限测试 | 8个 | 有权限/无权限/未认证 |
| 性能测试 | 1个 | 批量查询性能测试 |
| SQL查询测试 | 6个 | JOIN查询、模糊查询等 |

### 各层测试用例详细统计

#### Service层测试用例（26个）

| 用例编号范围 | 数量 | 说明 |
|------------|------|------|
| TC-STAFF-001 ~ TC-STAFF-026 | 26个 | 员工管理业务逻辑测试 |

**覆盖场景**：
- 新增员工：4个（成功、必填字段为空、默认密码、工号自动生成）
- 删除员工：3个（成功、不存在ID、批量删除）
- 批量删除：2个（成功、空列表）
- 编辑员工：5个（成功、更新状态、更新部门、不存在员工、性别枚举）
- 查询员工：7个（ByID成功、ByID不存在、多条件分页查询5个）
- 详细信息：2个（查询详情、年龄计算）
- 异常场景：3个（手机号重复、特殊字符姓名、空结果集）

#### Controller层测试用例（28个）

| 用例编号范围 | 数量 | 说明 |
|------------|------|------|
| TC-AUTH-001 ~ TC-AUTH-009 | 9个 | 认证与授权测试 |
| TC-CTRL-010 ~ TC-CTRL-021 | 12个 | API接口功能测试 |
| TC-PWD-001 ~ TC-PWD-003 | 3个 | 密码管理测试 |
| TC-ROLE-001 ~ TC-ROLE-002 | 2个 | 员工角色管理测试 |
| TC-EXCEPTION-001 ~ TC-EXCEPTION-004 | 4个 | 异常场景测试 |
| **小计** | **30个** | （注：其中2个与Service层重复计算） |

**实际Controller层独立用例**：**28个**

**覆盖场景**：
- 权限验证：9个（增删改查的有权限/无权限/未认证）
- 新增员工：3个（成功、无权限、未认证）
- 删除员工：3个（成功、无权限、批量删除）
- 编辑员工：2个（成功、无权限）
- 查询员工：9个（ByID、详细信息、多条件分页、分页参数）
- 密码管理：3个（正确密码、错误密码、重置密码）
- 角色管理：2个（设置角色、查询角色）
- 异常处理：4个（无效JSON、无效ID、无效分页参数）

#### Mapper层测试用例（28个）

| 用例编号范围 | 数量 | 说明 |
|------------|------|------|
| TC-MAPPER-001 ~ TC-MAPPER-028 | 28个 | SQL查询和数据库操作测试 |

**覆盖场景**：
- 基础CRUD：5个（insert、selectById、updateById、deleteById、selectList）
- 考勤查询：4个（listStaffAttendanceVO、listStaffDeptAttendanceVO、queryAttendanceMonthVO）
- 加班查询：3个（listStaffOvertimeVO、listStaffDeptOvertimeVO、queryOvertimeMonthVO）
- 员工信息查询：5个（queryByCode、queryInfo、queryStaffDeptVO、queryByRole）
- 边界值测试：4个（空字符串、null、不存在的数据）
- 分页测试：3个（第一页、第二页、性能测试）
- JOIN查询：2个（验证部门名称、LEFT JOIN）
- 特殊字符：2个（中文模糊查询、特殊字符姓名）

---

## 📝 后续扩展建议

### Phase 1: 补充边界值测试

- [ ] 增加更多姓名字符长度边界测试
- [ ] 增加手机号格式边界测试
- [ ] 增加日期范围边界测试
- [ ] 增加分页参数边界测试

### Phase 2: 增强员工角色关联测试

- [ ] 为员工设置角色 - 清空所有角色
- [ ] 为员工设置角色 - 员工不存在
- [ ] 为员工设置角色 - 无效角色ID
- [ ] 查询员工的角色 - 未分配角色的员工

### Phase 3: 完善认证授权测试

- [ ] 创建独立的 AuthenticationTest 测试类
- [ ] Token过期测试
- [ ] Token篡改测试
- [ ] 多权限验证测试（hasAnyAuthority）
- [ ] 公开接口无需认证测试

### Phase 4: 性能测试

- [x] 批量查询性能测试（已在StaffMapperTest中实现）
- [ ] 大量员工数据的分页查询性能
- [ ] 多条件组合查询性能
- [ ] 并发设置角色/权限的性能

### Phase 5: 安全测试

- [ ] SQL注入测试（员工姓名、手机号）
- [ ] XSS攻击测试
- [ ] 权限绕过测试
- [ ] 密码强度验证测试

### Phase 6: 集成测试

- [ ] 完整的权限验证流程测试
- [ ] 员工-角色-权限的完整关联测试
- [ ] 前端菜单渲染的端到端测试

