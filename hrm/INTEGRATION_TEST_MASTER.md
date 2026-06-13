# HRM 系统 — 集成测试总体规划文档

> 版本：v2.0  
> 生成日期：2026-06-13  
> 项目：HRM 人力资源管理系统  
> 技术栈：Spring Boot 2.5.6 + MyBatis-Plus 3.5.1 + Activiti 7.0.0 + Spring Security + JWT + Redis

---

## 目录

1. [测试架构概览](#1-测试架构概览)
2. [系统模块与数据流分析](#2-系统模块与数据流分析)
3. [集成测试策略](#3-集成测试策略)
4. [测试环境配置](#4-测试环境配置)
5. [测试数据管理](#5-测试数据管理)
6. [测试基础设施](#6-测试基础设施)
7. [API 路由清单](#7-api-路由清单)
8. [测试执行计划](#8-测试执行计划)
9. [风险与依赖管理](#9-风险与依赖管理)

---

## 1. 测试架构概览

### 1.1 测试金字塔

```
              ┌──────────┐
              │   E2E    │  ~10 用例   (Selenium/Cypress)
              │  端到端   │
              └──────────┘
           ┌───────────────┐
           │  集成测试      │  ~37 用例   (本文档 · SpringBootTest + MockMvc)
           │  Integration  │
           └───────────────┘
       ┌──────────────────────┐
       │  Service 白盒测试     │  ~47 用例   (Mockito · 反射)
       │  White-Box Tests     │
       └──────────────────────┘
   ┌──────────────────────────────┐
   │  Controller 单元测试          │  ~230 用例  (MockMvc · @WebMvcTest)
   │  API Contract Tests          │
   └──────────────────────────────┘
┌────────────────────────────────────┐
│  Mapper 测试 · 工具类测试           │  ~28 用例   (SpringBootTest)
│  Data Layer · Utility Tests        │
└────────────────────────────────────┘
```

### 1.2 测试维度矩阵

| 维度 | 单元测试 | Service白盒 | 集成测试 | E2E |
|------|:--:|:--:|:--:|:--:|
| API 契约验证 | ✅ | — | ✅ | — |
| 参数校验/边界值 | ✅ | ✅ | — | — |
| 业务逻辑分支覆盖 | — | ✅ | — | — |
| **跨模块数据流** | — | — | ✅ | ✅ |
| **事务一致性** | — | — | ✅ | ✅ |
| **权限鉴权链路** | ✅ | — | ✅ | — |
| **工作流状态机** | — | ✅ | ✅ | — |
| **文件上传完整性** | ✅ | — | ✅ | — |
| UI 交互流程 | — | — | — | ✅ |

### 1.3 现有测试资产统计

| 测试层次 | 文件数 | 用例数(约) | 覆盖模块 |
|---------|:--:|:--:|------|
| Controller 集成测试 | 13 | ~230 | 全部 13 个 Controller |
| Service 单元测试 | 16 | ~120 | Staff/Dept/City/Insurance/Salary |
| Mapper 测试 | 1 | ~28 | Staff |
| 全局异常测试 | 1 | ~6 | BaseExceptionHandler |
| Service 白盒测试 | 8 | ~47 | 8 个核心 Service |
| **合计** | **39** | **~431** | — |

---

## 2. 系统模块与数据流分析

### 2.1 模块依赖关系图

```
                     ┌─────────────────┐
                     │  LoginController │
                     │  (JWT Token 签发) │
                     └────────┬────────┘
                              │ Token
                              ▼
┌──────────────────────────────────────────────────────────────┐
│                    Spring Security Filter                     │
│              JwtAuthenticationFilter → @PreAuthorize          │
└──────────────────────────────────────────────────────────────┘
         │                │                │                │
         ▼                ▼                ▼                ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│ Staff/DEPT   │  │ Role/Menu   │  │ Attendance  │  │ Salary/Ins  │
│ 组织架构      │  │ 权限管理     │  │ 考勤请假     │  │ 薪酬社保     │
└──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
       │                │                │                │
       └────────────────┴────────────────┴────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │    跨模块数据流依赖关系        │
                    │                               │
                    │  Staff ← Dept (deptId)        │
                    │  Staff ← Role (staffRole)     │
                    │  Attendance ← Staff (staffId) │
                    │  Attendance ← Dept (时间标准)  │
                    │  StaffLeave ← Staff (staffId) │
                    │  StaffOvertime ← Staff        │
                    │  Insurance ← Staff + City     │
                    │  Salary ← Staff + Insurance   │
                    │           + Attendance        │
                    │           + StaffOvertime      │
                    │           + SalaryDeduct      │
                    │  Docs ← Staff (staffId)       │
                    └───────────────────────────────┘
```

### 2.2 关键数据流链路

#### 链路 1：薪资计算全链路 (最复杂)
```
City(社保标准) → Insurance(员工社保设置) ─┐
Staff(基础薪资设置) → Salary(薪资记录) ────┤
Dept(部门扣款标准) → SalaryDeduct(扣款规则) ┤→ SalaryService.list() → StaffSalaryVO
Attendance(考勤:迟到/早退/旷工/请假) ──────┤
StaffLeave(请假天数) ──────────────────────┤
StaffOvertime(加班费) ─────────────────────┘
```

#### 链路 2：RBAC 权限鉴权链路
```
Menu(菜单+权限点) → RoleMenu(角色菜单关联) → Role
                                              ↓
                                         StaffRole(员工角色关联)
                                              ↓
                              JWT Token → @PreAuthorize 鉴权
```

#### 链路 3：请假审批工作流链路
```
StaffLeave.apply() → Activiti 启动流程 ─→ 主管拾取(claim)
                                      ─→ 主管审批(complete)
                                      ─→ HR审批(complete)
                                      ─→ 员工撤销(cancel)
                                      ─→ 主管归还(revert)
```

#### 链路 4：文件上传下载链路
```
MultipartFile → DocsService.upload()
  ├─ 扩展名白名单检查
  ├─ 文件大小检查(≤20MB)
  ├─ MD5 去重
  ├─ 磁盘写入
  └─ Docs 记录保存

DocsController.download(filename)
  ├─ 路径遍历检测
  ├─ 文件存在性检查
  └─ 流式输出
```

### 2.3 数据库表关系

```
per_staff (员工)               per_dept (部门)
  │ dept_id                      │ parent_id (自引用)
  │                              │
  ├── per_staff_role             │
  │     └── per_role             │
  │           └── per_role_menu  │
  │                 └── per_menu │
  │                              
  ├── att_attendance (考勤)      
  │     status → 迟到/早退/旷工/休假
  │                              
  ├── att_staff_leave (请假)     
  │     status → 待审核/审核中/批准/驳回/撤销
  │     ↑ Activiti 工作流
  │                              
  ├── att_staff_overtime (加班)  
  │     type → 工作日/休息日/法定假日
  │     countType → 按小时/按日
  │                              
  ├── soc_insurance (社保公积金)  
  │     └── soc_city (城市标准)  
  │                              
  ├── sal_salary (薪资)          
  │     └── sal_deduct (扣款规则)
  │                              
  └── per_docs (文档)            
```

---

## 3. 集成测试策略

### 3.1 集成测试定义

> **集成测试在本项目中的定义**：验证多个真实模块（不使用 Mock）通过 API 调用完成一个完整业务流程，检查跨模块数据流转的正确性和数据一致性。

### 3.2 Mock 策略

| 组件 | 策略 | 原因 |
|------|------|------|
| **数据库 (MySQL)** | ✅ 真实 | 验证 SQL 语义、事务、外键约束 |
| **Redis** | ⚠️ Mock | 外部依赖，测试环境可能不可用 |
| **Activiti 引擎** | ✅ 真实 | 验证工作流状态机 |
| **文件系统** | ✅ 真实 | 验证上传下载完整性 |
| **Spring 容器** | ✅ 完整加载 | 验证 DI、AOP、事务代理 |

### 3.3 测试优先级定义

| 优先级 | 定义 | 用例数 | 目标覆盖率 |
|:--:|------|:--:|:--:|
| **P0** | 核心业务流程，阻塞性缺陷 | 20 | 100% |
| **P1** | 重要业务流程，影响用户体验 | 13 | ≥90% |
| **P2** | 边界场景与异常路径 | 7 | ≥70% |

### 3.4 集成测试与单元测试的边界

```
单元测试关注点                    集成测试关注点
─────────────────                ─────────────────
单 API 参数校验    ←──不重复──→   跨 API 业务流程
Service 逻辑分支   ←──不重复──→   模块间数据流转
Mapper SQL 语义    ←──不重复──→   事务一致性
异常处理器映射     ←──互补──→     真实异常传播
权限注解验证       ←──互补──→     完整鉴权链路
```

---

## 4. 测试环境配置

### 4.1 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | 编译和运行 |
| Maven | 3.8+ | 构建工具 |
| MySQL | 8.0 | 测试数据库 |
| Redis | 6.0+ | 缓存(测试中 Mock) |
| Activiti | 7.0.0.GA | 工作流引擎 |

### 4.2 测试数据库配置

```properties
# application-test.properties
spring.datasource.url=jdbc:mysql://localhost:3306/hrm_test?useUnicode=true&characterEncoding=utf-8
spring.datasource.username=test
spring.datasource.password=test123

# 初始化 SQL
spring.sql.init.mode=always
spring.sql.init.data-locations=classpath:test-data.sql

# MyBatis-Plus
mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl

# Activiti
spring.activiti.database-schema-update=true
spring.activiti.db-history-used=true
spring.activiti.history-level=full

# Redis Mock
spring.redis.enabled=false
```

### 4.3 测试 Profile

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional  // 每个测试方法后自动回滚
public class BaseIntegrationTest {
    // 公共测试基类
}
```

---

## 5. 测试数据管理

### 5.1 数据准备策略

```
测试前 (BeforeClass)
├─ 执行 schema.sql → 创建表结构
├─ 执行 test-data.sql → 插入基础数据
│   ├─ 部门：技术部(1)、产品部(2)、人事部(3)
│   ├─ 员工：admin(管理员)、zhangsan(普通员工)、lisi(HR)
│   ├─ 角色：超级管理员、部门管理员、普通用户
│   ├─ 菜单：系统管理、权限管理、考勤管理(含权限点)
│   └─ 城市社保标准：北京、上海
│
每个测试方法 (BeforeEach)
├─ 方法级 @Transactional 保证隔离
│
测试方法执行后 (AfterEach)
└─ 自动回滚，恢复初始状态
```

### 5.2 基础测试数据清单

| 数据实体 | 关键字段 | 用途 |
|---------|---------|------|
| admin 员工 | code=admin, password=123, role=超级管理员 | 管理员操作 |
| zhangsan 员工 | code=zhangsan, password=123, role=普通用户 | 普通员工操作 |
| lisi 员工 | code=lisi, password=123, role=HR | HR审批操作 |
| 技术部 | id=1, parentId=0, 工作时间09:00-18:00 | 部门归属 |
| 产品部 | id=2, parentId=0 | 部门归属 |
| 北京市社保标准 | lowerSalary=2320, averageSalary=11297 | 社保计算 |
| 管理员角色 | 全部菜单权限 | 权限验证 |
| 普通用户角色 | 列表查看权限 | 权限验证 |

---

## 6. 测试基础设施

### 6.1 测试工具类

```java
// SecurityUtils.java — 模拟不同权限用户
public class SecurityUtils {
    // 模拟管理员(全部权限)
    public static MockHttpServletRequestBuilder withAdmin(MockHttpServletRequestBuilder builder) {
        return builder.with(user("admin").authorities(
            new SimpleGrantedAuthority("system:staff:add"),
            new SimpleGrantedAuthority("system:department:add"),
            // ... 更多权限
        ));
    }

    // 模拟普通用户
    public static MockHttpServletRequestBuilder withNormalUser(MockHttpServletRequestBuilder builder) {
        return builder.with(user("zhangsan").authorities(
            new SimpleGrantedAuthority("system:staff:list")
        ));
    }

    // 自定义权限
    public static MockHttpServletRequestBuilder withCustomAuthorities(
            MockHttpServletRequestBuilder builder, String... authorities) {
        // ...
    }
}
```

### 6.2 测试配置类

```java
// TestConfig.java
@TestConfiguration
public class TestConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

// TestSecurityConfig.java
@TestConfiguration
@Import(SecurityConfig.class)
public class TestSecurityConfig {
    // 替换 JwtAuthenticationFilter 为直接放行版本
}
```

---

## 7. API 路由清单

### 7.1 完整 API 端点矩阵

#### 认证模块 (LoginController)
| Method | Path | 权限 | 说明 |
|--------|------|------|------|
| POST | `/login/{validateCode}` | 无 | 用户登录 |
| GET | `/validate/code` | 无 | 获取验证码 |

#### 员工管理 (StaffController)
| Method | Path | 权限 | 说明 |
|--------|------|------|------|
| POST | `/staff` | `system:staff:add` | 新增员工 |
| DELETE | `/staff/{id}` | `system:staff:delete` | 删除员工 |
| DELETE | `/staff/batch/{ids}` | `system:staff:delete` | 批量删除 |
| PUT | `/staff` | `system:staff:edit` | 编辑员工 |
| GET | `/staff/{id}` | 无 | 查询员工 |
| GET | `/staff/info/{id}` | 无 | 查询详情 |
| GET | `/staff` | `system:staff:list` | 分页查询 |
| GET | `/staff/export/{filename}` | `system:staff:export` | 导出 |
| POST | `/staff/import` | `system:staff:import` | 导入 |
| POST | `/staff/set/{id}` | `system:staff:set_role` | 设置角色 |
| GET | `/staff/staff/{id}` | 无 | 查询员工角色 |
| GET | `/staff/{pwd}/{id}` | 无 | 验证密码 |
| PUT | `/staff/reset` | 无 | 重置密码 |

#### 部门管理 (DeptController)
| Method | Path | 权限 | 说明 |
|--------|------|------|------|
| POST | `/dept` | `system:department:add` | 新增部门 |
| DELETE | `/dept/{id}` | `system:department:delete` | 删除部门 |
| DELETE | `/dept/batch/{ids}` | `system:department:delete` | 批量删除 |
| PUT | `/dept` | `system:department:edit` | 编辑部门 |
| GET | `/dept/{id}` | 无 | 查询部门 |
| GET | `/dept/all` | 无 | 查询所有(树形) |
| GET | `/dept` | `system:department:list` | 条件查询 |
| GET | `/dept/export/{filename}` | `system:department:export` | 导出 |
| POST | `/dept/import` | `system:department:import` | 导入 |

#### 菜单管理 (MenuController)
| Method | Path | 权限 | 说明 |
|--------|------|------|------|
| POST | `/menu` | `system:menu:add` | 新增菜单 |
| DELETE | `/menu/{id}` | `system:menu:delete` | 删除菜单 |
| DELETE | `/menu/batch/{ids}` | `system:menu:delete` | 批量删除 |
| PUT | `/menu` | `system:menu:edit` | 编辑菜单 |
| GET | `/menu/{id}` | 无 | 查询菜单 |
| GET | `/menu/all` | 无 | 查询所有 |
| GET | `/menu` | `system:menu:list` | 条件查询 |

#### 角色管理 (RoleController)
| Method | Path | 权限 | 说明 |
|--------|------|------|------|
| POST | `/role` | `system:role:add` | 新增角色 |
| DELETE | `/role/{id}` | `system:role:delete` | 删除角色 |
| DELETE | `/role/batch/{ids}` | `system:role:delete` | 批量删除 |
| PUT | `/role` | `system:role:edit` | 编辑角色 |
| GET | `/role/{id}` | 无 | 查询角色 |
| GET | `/role/all` | 无 | 查询所有 |
| GET | `/role` | `system:role:list` | 条件查询 |
| POST | `/role/set/{id}` | `system:role:set_menu` | 分配菜单 |

#### 考勤管理 (AttendanceController)
| Method | Path | 权限 | 说明 |
|--------|------|------|------|
| POST | `/attendance` | `attendance:add` | 新增考勤 |
| DELETE | `/attendance/{id}` | `attendance:delete` | 删除考勤 |
| DELETE | `/attendance/batch/{ids}` | `attendance:delete` | 批量删除 |
| PUT | `/attendance` | `attendance:edit` | 更新考勤 |
| GET | `/attendance/{id}` | 无 | 查询考勤 |
| GET | `/attendance` | `attendance:list` | 列表查询 |
| GET | `/attendance/export/{month}/{filename}` | `attendance:export` | 导出 |
| POST | `/attendance/import` | `attendance:import` | 导入 |
| PUT | `/attendance/set` | 无 | 设置考勤 |
| GET | `/attendance/query` | 无 | 按员工+日期查询 |
| GET | `/attendance/all` | 无 | 所有枚举值 |

#### 请假管理 (StaffLeaveController + LeaveController)
| Method | Path | 权限 | 说明 |
|--------|------|------|------|
| POST | `/staff-leave` | `leave:add` | 新增请假 |
| DELETE | `/staff-leave/{id}` | `leave:delete` | 删除请假 |
| PUT | `/staff-leave` | `leave:edit` | 编辑请假 |
| GET | `/staff-leave/{id}` | 无 | 查询请假 |
| GET | `/staff-leave` | `leave:list` | 列表查询 |
| GET | `/staff-leave/staff/{id}` | 无 | 按员工查询 |
| POST | `/staff-leave/apply/{code}` | 无 | 申请请假(启工作流) |
| POST | `/staff-leave/claim/{code}` | 无 | 拾取任务 |
| POST | `/staff-leave/complete/{code}` | 无 | 完成任务 |
| POST | `/staff-leave/revert/{code}` | 无 | 归还任务 |
| POST | `/staff-leave/cancel` | 无 | 撤销请假 |

#### 加班管理 (StaffOvertimeController + OvertimeController)
| Method | Path | 权限 | 说明 |
|--------|------|------|------|
| POST | `/staff-overtime` | `overtime:add` | 新增加班 |
| DELETE | `/staff-overtime/{id}` | `overtime:delete` | 删除加班 |
| DELETE | `/staff-overtime/batch/{ids}` | `overtime:delete` | 批量删除 |
| PUT | `/staff-overtime` | `overtime:edit` | 编辑加班 |
| PUT | `/staff-overtime/set` | 无 | 设置加班规则 |
| GET | `/staff-overtime/{id}` | 无 | 查询加班 |
| GET | `/staff-overtime` | `overtime:list` | 列表查询 |
| GET | `/staff-overtime/time/off/{id}` | 无 | 查询调休天数 |

#### 薪资管理 (SalaryController + SalaryDeductController)
| Method | Path | 权限 | 说明 |
|--------|------|------|------|
| POST | `/salary/set` | `salary:set` | 设置薪资 |
| GET | `/salary/{id}` | 无 | 查询薪资 |
| GET | `/salary` | `salary:list` | 列表查询 |
| GET | `/salary/export/{month}/{filename}` | `salary:export` | 导出薪资 |
| POST | `/salary/import` | `salary:import` | 导入薪资 |
| POST | `/salary-deduct/set` | `salary:deduct:set` | 设置扣款规则 |

#### 社保管理 (InsuranceController + CityController)
| Method | Path | 权限 | 说明 |
|--------|------|------|------|
| POST | `/insurance/set` | `insurance:set` | 设置社保 |
| GET | `/insurance/{id}` | 无 | 查询社保 |
| GET | `/insurance` | `insurance:list` | 列表查询 |
| POST | `/insurance/import` | `insurance:import` | 导入社保 |
| POST | `/city` | `city:add` | 新增城市标准 |
| PUT | `/city` | `city:edit` | 编辑城市标准 |
| DELETE | `/city/{id}` | `city:delete` | 删除城市标准 |
| GET | `/city` | `city:list` | 列表查询 |
| POST | `/city/import` | `city:import` | 导入城市标准 |

#### 文件管理 (DocsController)
| Method | Path | 权限 | 说明 |
|--------|------|------|------|
| POST | `/docs` | `docs:add` | 新增文档记录 |
| POST | `/docs/upload/{id}` | `docs:upload` | 上传文件 |
| DELETE | `/docs/{id}` | `docs:delete` | 删除文档 |
| GET | `/docs` | `docs:list` | 文档列表 |
| GET | `/docs/download/{filename}` | 无 | 下载文件 |
| GET | `/docs/avatar/{filename}` | 无 | 下载头像(公开) |

---

## 8. 测试执行计划

### 8.1 测试执行阶段

| 阶段 | 名称 | 用例数 | 执行时机 | 预计耗时 |
|:--:|------|:--:|------|:--:|
| 1 | **冒烟测试** (P0核心) | 8 | 每次提交 | 5 min |
| 2 | **核心集成** (全部P0) | 20 | 每日构建 | 15 min |
| 3 | **完整集成** (P0+P1) | 33 | 提测前 | 25 min |
| 4 | **全量回归** (全部) | 37+ | 发布前 | 35 min |

### 8.2 Maven 执行命令

```bash
# 冒烟测试
mvn test -Dtest="IntegrationSmokeTest" -Dspring.profiles.active=test

# 核心集成测试
mvn test -Dgroups="P0" -Dspring.profiles.active=test

# 完整集成测试
mvn test -Dgroups="P0,P1" -Dspring.profiles.active=test

# 全量回归测试
mvn verify -P integration-test -Dspring.profiles.active=test

# 生成覆盖率报告
mvn verify -P coverage -Dspring.profiles.active=test
# 报告位置: target/site/jacoco/index.html
```

### 8.3 CI/CD 集成 (GitHub Actions)

```yaml
name: Integration Tests
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  integration-test:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: test123
          MYSQL_DATABASE: hrm_test
        ports:
          - 3306:3306
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run Integration Tests
        run: mvn test -Dgroups="P0,P1" -Dspring.profiles.active=ci
```

---

## 9. 风险与依赖管理

### 9.1 已知风险

| 风险 | 影响模块 | 严重程度 | 缓解措施 |
|------|---------|:--:|------|
| Activiti 引擎启动失败 | 请假工作流 | 🔴 高 | 条件跳过 + @DisabledIf |
| Redis 不可用 | 登录(验证码) | 🟡 中 | Mock RedisUtil Bean |
| MySQL 版本兼容性 | 全部 | 🟡 中 | 使用 H2 内存数据库作为备选 |
| 文件系统权限 | 文档管理 | 🟢 低 | 使用临时目录 |
| 测试数据冲突 | 全部 | 🟡 中 | @Transactional 回滚 + 唯一数据隔离 |

### 9.2 外部依赖

| 依赖 | 集成测试策略 | 备选方案 |
|------|------------|---------|
| MySQL | 真实连接 | H2 内存数据库 |
| Redis | Mock (验证码除外) | Embedded Redis |
| Activiti | 真实引擎 | 条件跳过 |
| 文件系统 | 真实 (临时目录) | — |
| JWT | 真实生成/验证 | — |

---

## 附录

### A. 相关文档索引

| 文档 | 路径 | 说明 |
|------|------|------|
| 集成测试用例设计 | `INTEGRATION_TEST_DESIGN.md` | 37 个集成测试用例设计 |
| 测试用例分析 | `TCA.md` | 全部 13 模块的测试用例清单 |
| 测试设计评估 | `TE.md` | 黑盒/白盒测试分析 |
| 白盒测试结果 | `WBTE.md` | 47 个白盒测试用例结果 |
| 考勤测试设计 | `src/test/test_attendence/TEST_CASE_DESIGN.md` | 考勤/请假/加班等价类 |
| 权限测试设计 | `src/test/test_permission_management/TEST_CASE_DESIGN.md` | 员工/权限等价类 |
| 系统测试设计 | `src/test/test_system_management/TEST_CASE_DESIGN.md` | 员工/部门/文档等价类 |

### B. 数据库表结构摘要

| 表前缀 | 模块 | 表名 |
|--------|------|------|
| per_ | 组织权限 | per_staff, per_dept, per_role, per_menu, per_role_menu, per_staff_role, per_docs |
| att_ | 考勤管理 | att_attendance, att_staff_leave, att_staff_overtime, att_leave, att_overtime |
| soc_ | 社保管理 | soc_city, soc_insurance |
| sal_ | 薪资管理 | sal_salary, sal_deduct |
| sys_ | 系统 | sys_validate_code |

---

> 📝 本文档为 HRM 系统集成测试的总规划和指导文档。具体的测试用例设计见 [INTEGRATION_TEST_DESIGN.md](INTEGRATION_TEST_DESIGN.md)。
