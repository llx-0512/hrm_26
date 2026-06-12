# HRM 系统 — 白盒测试结果与意义 (White-Box Test Evaluation)

> 生成日期：2026-06-12
> 参考文档：[[TE.md]](TE.md) · [[TCA.md]](TCA.md)

---

## 一、背景

在 TCA.md 和 TE.md 完成了对现有测试用例的分析和覆盖率估计后，识别出以下缺口：

- **Controller 层**：12 个 Controller 中仅 `DocsController` 包含 try-catch 逻辑，其异常→HTTP 状态映射未被精确测试；全局异常处理器 `BaseExceptionHandler` 的 6 个 `@ExceptionHandler` 无一被显式测试
- **Service 层**：19 个 Service 中 6 个核心模块（`DeptService`、`DocsService`、`StaffLeaveService`、`AttendanceService`、`StaffOvertimeService`、`StaffService`）完全没有任何 Service 层白盒测试——大量业务分支、NPE 风险、状态机逻辑未被精确验证

为填补上述缺口，按照 TE.md 附录 A 的优先级顺序，逐模块添加了 **47 个白盒测试用例**，覆盖 8 个模块。

---

## 二、测试结果

| 轮次 | 模块 | 文件 | 用例 | 测试类型 | 结果 |
|------|------|------|------|---------|------|
| P0 | StaffLeaveService | `StaffLeaveServiceWhiteBoxTest.java` | 6 | 纯 Mockito | ✅ |
| P0 | StaffOvertimeService | `StaffOvertimeServiceWhiteBoxTest.java` | 8 | 纯 Mockito | ✅ |
| P0 | AttendanceService | `AttendanceServiceWhiteBoxTest.java` | 7 | 反射 + 零 Mock | ✅ |
| P0 | DeptService | `DeptServiceWhiteBoxTest.java` | 4 | 纯 Mockito | ✅ |
| P0 | DocsService | `DocsServiceWhiteBoxTest.java` | 7 | 纯 Mockito | ✅ |
| P0 | BaseExceptionHandler | `BaseExceptionHandlerWhiteBoxTest.java` | 6 | Spring Boot + MockBean | ✅ |
| P1 | DocsController | `DocsControllerWhiteBoxTest.java` | 6 | Spring Boot + MockBean | ✅ |
| P1 | StaffService | `StaffServiceWhiteBoxTest.java` | 3 | 纯 Mockito + Hutool | ✅ |
| **合计** | **8** | **8 个文件** | **47** | — | **全部通过** |

---

## 三、发现的问题与风险

白盒测试的核心价值不在于"增加了多少测试用例"，而在于**暴露了多少黑盒测试无法发现的问题**。本轮测试共发现以下风险：

### 3.1 🔴 确认的 NPE 线上风险（3 处）

| 位置 | 触发条件 | 当前行为 | 建议 |
|------|---------|---------|------|
| `StaffService.list()` L145 | 员工 deptId 指向不存在的部门，`deptService.getOne()` 返回 null | `NullPointerException` @ `dept.getName()` | 加 `if (dept != null)` 防护 |
| `StaffService.validate()` L216 | `getById()` 返回 null，直接调用 `staff.getPassword()` | `NullPointerException` | 加 `if (staff == null)` 返回错误 |
| `DeptService.edit()` L136 | while 循环中 `getById(parentId)` 可能返回 null | 已有 `if (parentDept == null) { break; }`（已防护 ✅） | — |

### 3.2 🟡 确认的设计风险（4 处）

| 位置 | 问题描述 | 影响 |
|------|---------|------|
| `StaffLeaveService.apply()` L306-308 | 工作流启动失败被 `catch(Exception)` 静默吞掉，**无日志** | 请假保存成功但工作流未启动，调用方无法感知 |
| `StaffLeaveService.claim()` L320-332 | `updateById` 先执行成功（DB 已改），`task` 查询失败时返回 ERROR（Activiti 未改） | **DB 与 Activiti 状态不一致** |
| `StaffLeaveService.complete()` L379-380 | 未知 `taskDefinitionKey` 时 `map=null`，`taskService.complete()` 收到 null variables | 行为不确定，取决于 Activiti 对 null map 的处理 |
| `StaffLeaveService.apply()` L278-283 | 冲突检测范围包含 `REJECT` 状态（驳回后仍不能重新申请） | 设计意图不明确——驳回后应允许修改重提还是彻底禁止？ |

### 3.3 🟡 确认的硬编码行为（2 处）

| 位置 | 硬编码值 | 影响 |
|------|---------|------|
| `StaffService.imp()` L205 | `staff.setDeptId(13)` | 所有导入员工被强制分配到部门 13，Excel 中的 deptId 被忽略 |
| `StaffService.add()` L62 | `staff.setCode("staff_" + staff.getId())` | 工号格式硬编码为 `staff_{id}`，无法自定义前缀 |

### 3.4 🟢 验证通过的防御设计（无问题）

| 位置 | 验证结果 |
|------|---------|
| `DocsService.upload()` 扩展名白名单 | `.exe`/`.bat`/`.sh`/`.js`/`.py` 全部正确拒绝 |
| `DocsService.download()` 路径遍历 | `..`（先捕获）和 `\`/`/`（后捕获）的两个 catch 独立工作 |
| `DocsService.upload()` MD5 去重 | 内容相同的文件复用已有 UUID 文件名，跳过磁盘写入 |
| `BaseExceptionHandler` 6 个 Handler | 每种异常到 HTTP 状态码/业务 code 的映射均正确 |
| `AttendanceService` 考勤判定优先级 | `isAbsenteeism` 在 `isLate`/`isLeaveEarly` 之前生效，既迟到又早退 → ABSENTEEISM |
| `StaffOvertimeService` 加班费计算 | 12 个关键组合路径均正确：3 种日期类型 × 2 种计算方式 × 2 种调休策略 |
| `DocsController` 公开接口 | `getAvatar()` 无 `@PreAuthorize` → 无认证用户可访问（404 而非 401） |

---

## 四、方法论总结

### 4.1 三种白盒测试模式

本轮测试使用了三种不同的技术方案，适配不同的测试目标：

| 模式 | 适用场景 | 示例 |
|------|---------|------|
| **纯 Mockito + 反射** | Service 私有方法、零外部依赖的判定逻辑 | `AttendanceService` 的 `isLate()`/`isLeaveEarly()` — 仅依赖 `DateUtil.compare()` 静态方法，零 Mock |
| **纯 Mockito + spy** | Service 公共方法、Mock 隔离依赖 | `StaffOvertimeService.setOvertime()` — Mock 4 个 Mapper + saveOrUpdate |
| **Spring Boot + MockBean** | Controller 层、全局异常处理器 | `BaseExceptionHandler` — 需要完整 Spring MVC 异常处理链路 |

### 4.2 模式选择决策树

```
被测方法所在的类
├── extends ServiceImpl<M, T>
│   └── 使用 spy() + ReflectionTestUtils 注入 @Mock 依赖
│       ├── 被测方法为 private → 反射 setAccessible(true)
│       └── 被测方法为 public  → 直接调用 + doReturn/doThrow Mock ServiceImpl 方法
└── @Controller / @ControllerAdvice
    └── 使用 @SpringBootTest + @AutoConfigureMockMvc + @MockBean
```

### 4.3 覆盖率贡献估算

| 指标 | 白盒测试前 | 白盒测试后 | 提升 |
|------|-----------|-----------|------|
| 行覆盖率（估算） | ~70% | ~82% | **+12%** |
| Service 层分支覆盖 | ~50% | ~85% | **+35%** |
| 异常处理路径覆盖 | ~20% | ~90% | **+70%** |
| NPE 风险路径覆盖 | ~0% | ~100% | **+100%** |

> 注：覆盖率数据为估算值，准确数据需引入 JaCoCo 生成。

---

## 五、文件清单

```
hrm/src/test/java/com/qiujie/
├── service/
│   ├── StaffLeaveServiceWhiteBoxTest.java       # 状态机/工作流/数据一致性
│   ├── StaffOvertimeServiceWhiteBoxTest.java    # 加班费计算引擎
│   ├── AttendanceServiceWhiteBoxTest.java       # 考勤判定 4 个 private 方法
│   ├── DeptServiceWhiteBoxTest.java             # 时间校验/null 防护
│   ├── DocsServiceWhiteBoxTest.java             # 上传校验链/下载安全检查
│   └── StaffServiceWhiteBoxTest.java            # NPE 风险/硬编码行为
├── controller/
│   └── DocsControllerWhiteBoxTest.java          # 异常→HTTP 状态映射
└── exception/
    └── BaseExceptionHandlerWhiteBoxTest.java    # 6 个 @ExceptionHandler 映射
```

---

## 六、结论

本轮白盒测试在 **不修改任何生产代码** 的前提下，为 8 个核心模块补充了 47 个用例，达到以下效果：

| 维度 | 成果 |
|------|------|
| **风险发现** | 确认 2 处线上 NPE 风险 + 4 处设计风险 + 2 处硬编码行为 |
| **防御验证** | 确认 6 处防御设计（路径遍历/异常映射/计算逻辑）正确工作 |
| **覆盖提升** | 预估行覆盖率 +12%，异常处理路径 +70% |
| **方法论沉淀** | 建立了可复用的 3 种白盒测试模式（纯 Mockito / 反射 / Spring Boot） |

白盒测试的核心意义不在于通过率（47/47），而在于**每一条用例都精确命中了黑盒测试无法触达的代码路径**——私有方法的分支、异常被静默吞掉的 catch 块、继承方法中潜伏的 NPE、Spring 异常处理器的映射规则。

---

> 📝 相关文档：[[TCA.md]](TCA.md) · [[TE.md]](TE.md)
