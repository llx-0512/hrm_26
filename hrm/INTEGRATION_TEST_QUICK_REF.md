# HRM 系统 — 集成测试快速参考矩阵

> 版本：v2.0 | 日期：2026-06-13

---

## 📊 集成测试覆盖矩阵

| 数据流链路 | 关键模块 | 集成测试 | 单元测试 | Service白盒 | 风险等级 |
|-----------|---------|:--:|:--:|:--:|:--:|
| **登录认证 → API鉴权** | Login + Security + JWT | ✅ 3 | ✅ 15 | — | 🟢 |
| **员工入职 → 配置 → 离职** | Staff + Dept + Role | ✅ 4 | ✅ 30 | ✅ 3 | 🟢 |
| **RBAC 权限体系** | Menu + Role + Staff | ✅ 3 | ✅ 23 | — | 🟢 |
| **文件上传 → MD5去重 → 下载** | Docs + Filesystem | ✅ 2 | ✅ 36 | ✅ 7 | 🟡 |
| **城市标准 → 社保设置** | City + Insurance | ✅ 4 | ✅ 32 | — | 🟡 |
| **考勤 → 迟到判定 → 状态** | Attendance + Dept | ✅ 4 | ✅ 41 | ✅ 7 | 🟡 |
| **请假申请 → 工作流审批** | StaffLeave + Activiti | ✅ 7 | ✅ 36 | ✅ 6 | 🔴 |
| **加班 → 加班费计算** | StaffOvertime + Salary | ✅ 4 | ✅ 38 | ✅ 8 | 🔴 |
| **薪资计算全链路** | Salary + Attendance + Leave + Overtime + Insurance | ✅ 7 | ✅ 65 | — | 🔴 |
| **批量导入/导出** | Excel + 全模块 | ✅ 3 | ✅ 10 | — | 🟡 |
| **全局异常处理** | BaseExceptionHandler | — | ✅ 6 | ✅ 6 | 🟢 |
| **数据一致性** | 全模块软删除 | ✅ 2 | — | — | 🟡 |
| **边界/安全** | XSS/SQL注入/路径遍历 | ✅ 7 | ✅ 10 | — | 🟡 |

---

## 🔗 模块间数据依赖矩阵

```
              Staff  Dept  Role  Menu  City  Insur Salary Attend Leave Overtime Docs
Staff          —     dept  role  —     —     —     —      att    leave over    docs
Dept           staff —     —     —     —     —     —      time   —     over    —
Role           staff —     —     menu  —     —     —      —      —     —       —
Menu           —     —     role  —     —     —     —      —      —     —       —
City           —     —     —     —     —     insur —      —      —     —       —
Insurance      staff —     —     —     city  —     salary —      —     —       —
Salary         staff —     —     —     —     insur —      attend leave over    —
Attendance     staff dept  —     —     —     —     salary —      leave —       —
Leave(请假)    staff —     —     —     —     —     salary attend —     —       —
Overtime(加班) staff dept  —     —     —     —     salary —      —     —       —
Docs           员工   —     —     —     —     —     —      —      —     —       —
```

横向 = 依赖模块 (该模块引用了哪个模块的主键)  
纵向 = 源模块

---

## 🎯 集成测试优先级排序

| 排名 | 用例编号 | 用例名称 | 影响范围 | 执行时长 |
|:--:|:--|---------|------|:--:|
| 1 | INT-LIFECYCLE-001 | 完整员工生命周期 | 全模块 | 3s |
| 2 | INT-E2E-SAL-001 | 完整薪资计算端到端 | 6模块 | 2s |
| 3 | INT-LEAVE-001 | 完整请假审批流程 | 3模块 | 2s |
| 4 | INT-RBAC-001 | RBAC 权限鉴权链路 | 4模块 | 2s |
| 5 | INT-SAL-001 | 薪资设置完整链路 | 3模块 | 1.5s |
| 6 | INT-ORG-001 | 部门+员工+角色联动 | 3模块 | 1.5s |
| 7 | INT-ATT-001 | 考勤→扣款联动 | 3模块 | 1.5s |
| 8 | INT-OVER-001 | 加班→调休联动 | 2模块 | 1s |
| 9 | INT-LOGIN-001 | 登录→受保护API | 2模块 | 1s |
| 10 | INT-E2E-LEAVE-001 | 请假→考勤→薪资全链路 | 5模块 | 2s |

---

## 🏗️ 测试架构速查

```
测试基类
├── BaseIntegrationTest (@SpringBootTest + @AutoConfigureMockMvc + @Transactional)
│   ├── 登录认证测试
│   ├── 组织架构测试
│   ├── 权限管理测试
│   ├── 薪酬管理测试
│   ├── 考勤请假测试
│   ├── 文件管理测试
│   └── 异常/边界测试
│
├── 测试工具类
│   ├── TestDataFactory (测试数据工厂)
│   ├── SecurityUtils (权限模拟工具)
│   └── TestConfig / TestSecurityConfig
│
└── 测试数据
    ├── test-data.sql (基础数据)
    └── Excel 模板文件
```

---

## 📁 文档索引

| 文档 | 相对路径 | 用途 |
|------|---------|------|
| 集成测试总体规划 | `INTEGRATION_TEST_MASTER.md` | 测试架构/策略/环境/路由清单 |
| 集成测试用例设计 | `INTEGRATION_TEST_DESIGN.md` | 52 个集成测试场景设计 |
| 集成测试用例代码 | `INTEGRATION_TEST_CASES.md` | 可执行的测试代码示例 |
| 测试用例分析 | `../TCA.md` | 全部模块的测试用例清单 |
| 测试设计评估 | `../TE.md` | 黑白盒分析/改进建议 |
| 白盒测试结果 | `../WBTE.md` | 47 个白盒用例结果 |
| 考勤测试设计 | `src/test/test_attendence/TEST_CASE_DESIGN.md` | 考勤/请假/加班等价类 |
| 权限测试设计 | `src/test/test_permission_management/TEST_CASE_DESIGN.md` | 员工/权限等价类 |
| 系统测试设计 | `src/test/test_system_management/TEST_CASE_DESIGN.md` | 员工/部门/文档等价类 |

---

> 📝 本文档为快速参考，详细内容见各专项文档。
