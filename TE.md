# HRM 系统 - 测试设计评估文档 (Test Evaluation)

> 评估日期：2026-06-12
> 评估范围：`hrm/src/test` 下全部 25 个测试文件，约 441 个测试用例
> 参考文档：[[TCA.md]]

---

## 一、黑盒测试与白盒测试兼顾分析

### 1.1 整体结论

测试用例在设计上**兼顾了黑盒测试与白盒测试**，呈现"三明治"分层结构——Controller 层偏黑盒，Service/Mapper 层偏白盒，两者互为补充。整体比例约 **55% 黑盒 : 45% 白盒**。

---

## 二、黑盒测试特征（占比约 55%）

黑盒测试的核心特征是**不关注内部实现，只关注给定输入 → 期望输出**。

### 2.1 系统化的等价类划分

三个 `TEST_CASE_DESIGN.md` 文件（权限管理、系统管理、考勤管理）中，**每个 API 接口都明确列出了等价类表格**。

以员工新增接口 (POST /staff) 为例：

| 输入项 | 有效等价类 | 无效等价类 |
|-------|-----------|-----------|
| **姓名** | 2-20字符、中英文混合 | null、超长、纯数字、含@#$% |
| **手机号** | 11位、13-19开头 | null、长度≠11、非数字、非1开头 |
| **生日** | yyyy-MM-dd、18-60岁 | 未来日期、格式错误、年龄超限 |
| **部门ID** | 存在的部门ID、正整数 | null、不存在的ID、负数或0 |

这是**规范的黑盒测试设计方法论**——先划分类，再从每类取代表值。

### 2.2 边界值分析贯彻始终

| 边界类型 | 典型用例 | 测试值 |
|---------|---------|--------|
| 字符串长度 | TC-MENU-008~010 | 1字符 / 20字符 / 21字符 |
| 数值区间 | TC-SAL-005~007 | 0 / 0.01 / -0.01 |
| 分页参数 | TC-OT-036~038 | current=0 / size=100 / size=101 |
| 文件大小 | TC-DOCS-026~030 | 1字节→21MB |
| 时间边界 | TC-ATT-035~036 | 00:00 / 23:59 |
| 集合操作 | TC-ATT-017~019 | 空列表 / 单元素 / 部分无效 |
| 日期边界 | TC-STAFF-032~036 | 18岁边界 / 60岁边界 / 未来日期 |

### 2.3 Controller 层全部采用黑盒交付

所有 `*ControllerTest` 都通过 `MockMvc` 发 HTTP 请求、校验 JSON 响应——**API 契约测试**：

```java
mockMvc.perform(post("/staff")
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(newStaff)))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value(200));
```

**特点**：
- 测试者不需要知道 Controller 内部调用了什么 Service 方法
- 不关心走了什么分支
- 只关心 HTTP 状态码和响应结构

---

## 三、白盒测试特征（占比约 45%）

白盒测试的核心特征是**利用对内部实现的知识，有意识地覆盖代码路径**。

### 3.1 薪资计算的路径覆盖（白盒典范）

`SalaryCalculationTest` 的 24 个用例最能体现白盒思维——测试者显然阅读了 `SalaryService.list()` 的源码，遍历了每一条代码分支：

| 源码逻辑 | 对应的白盒用例 |
|---------|-------------|
| `if (迟到次数 * 单次扣款)` | `testSalaryCalculation_WithDeductions()` |
| `if (baseSalary < 0)` | `testSalaryCalculation_NegativeBaseSalary()` |
| `if (overtimeSalary < 0)` | `testSalaryCalculation_NegativeOvertimePay()` |
| `if (subsidy < 0)` | `testSalaryCalculation_NegativeSubsidy()` |
| `if (bonus < 0)` | `testSalaryCalculation_NegativeBonus()` |
| `if (lateTimes > monthDays)` | `testSalaryCalculation_LateTimesExceedMonthDays()` |
| `if (leaveDates > monthDays)` | `testSalaryCalculation_LeaveDeductionDaysExceedMonthDays()` |
| `if (扣款规则不存在 → 默认值)` | `testSalaryCalculation_NonExistentDeductionRuleId()` |

### 3.2 Mock 隔离暴露了对依赖的精确认知

```java
// 测试者知道 SalaryService 内部调用了哪些 Mapper 方法，
// 并在调用链的精确位置插入 Mock
when(attendanceMapper.countTimes(eq(1001),
    eq(AttendanceStatusEnum.LATE.getCode()), eq("202401")))
    .thenReturn(3);
when(staffOvertimeMapper.sumMonthOvertimeSalary(eq(1002), eq("202401")))
    .thenReturn(new BigDecimal("100.00"));
doReturn(salary).when(salaryService).getOne(any(QueryWrapper.class));
```

如果不知道 `SalaryService.list()` 内部按什么顺序、以什么参数调用了这些 Mapper，**根本无法写出这些 Mock**。

### 3.3 `verify()` 验证内部调用行为

```java
verify(cityMapper, times(1)).insert(any(City.class));                       // CityStandardTest
verify(insuranceService, times(1)).saveOrUpdate(any(), any());               // SocialSecurityCalculateTest
verify(salaryDeductService, times(deductTypes.length)).save(any(SalaryDeduct.class)); // SalaryDeductServiceTest
```

这已经超出了"输入→输出"的黑盒范畴——验证的是**内部方法是否以正确的次数被调用**。

### 3.4 Mapper 层验证 SQL 语义

```java
// 验证 LEFT JOIN 对无部门员工的处理
noDeptStaff.setDeptId(null);
staffMapper.insert(noDeptStaff);
StaffDeptVO result = staffMapper.queryInfo(noDeptStaff.getId());
assertNotNull(result.getName());
// deptName 可能为 null（LEFT JOIN 语义）
```

测试者必须知道 `queryInfo` 的 SQL 使用了 LEFT JOIN 才能设计这个断言。

---

## 四、分层黑白盒分布

| 测试文件 | 层级 | 偏向 | 原因 |
|---------|------|------|------|
| `StaffControllerTest` | Controller | **黑盒** | MockMvc + JSON 断言，不依赖内部实现 |
| `DeptControllerTest` | Controller | **黑盒** | 同上 |
| `DocsControllerTest` | Controller | **黑盒** | 同上 |
| `LoginControllerTest` | Controller | **混合** | MockMvc 黑盒但 `@MockBean` RedisUtil 引入白盒 |
| `RoleControllerTest` | Controller | **黑盒** | 边界值设计但基于 API 行为 |
| `MenuControllerTest` | Controller | **黑盒** | 同上 |
| `AttendanceControllerTest` | Controller | **黑盒** | 同上 |
| `StaffLeaveControllerTest` | Controller | **黑盒** | 同上 |
| `StaffOvertimeControllerTest` | Controller | **黑盒** | 同上 |
| `SalaryControllerTest` | Controller | **黑盒** | 同上 |
| `InsuranceControllerTest` | Controller | **黑盒** | 同上 |
| `CityControllerTest` | Controller | **黑盒** | 同上 |
| `StaffServiceTest` | Service | **混合** | 部分场景依赖 `passwordEncoder` 内部行为 |
| `StaffMapperTest` | Mapper | **白盒** | JOIN 查询验证 SQL 语义 |
| `SalaryCalculationTest` | Service | **强白盒** | 路径覆盖 + Mock 精确控制 |
| `SalaryDeductServiceTest` | Service | **白盒** | 枚举遍历 + `verify()` |
| `SalaryDetailTest` | Service | **白盒** | Mock `getById` 精确控制 |
| `SalaryExportTest` | Service | **白盒** | Mock 控制 + 流体验证 |
| `SocialSecurityTestBase` | Service | **白盒** | Mock + `verify()` |
| `SocialSecurityCalculateTest` | Service | **白盒** | Mock 控制保存成功/失败/异常分支 |
| `SocialSecurityRatioTest` | Service | **白盒** | 参数化测试精度验证 |
| `CityStandardTest` | Service | **白盒** | Mock Mapper + `verify()` + 参数化 |

### 4.1 汇总

```
Controller 层（12个文件, ~290用例） ▓▓▓▓▓▓▓▓▓▓  黑盒为主
Service  层（8个文件,  ~120用例）  ▓▓▓▓▓▓▓▓▓▓  白盒为主
Mapper   层（1个文件,   ~28用例）  ▓▓▓▓▓▓▓▓▓▓  白盒为主
```

- 黑盒测试占比：**~55%**
- 白盒测试占比：**~45%**

---

## 五、优点与不足

### 5.1 ✅ 做得好的地方

| 优点 | 说明 |
|------|------|
| **架构分层清晰映射测试策略** | Controller → 黑盒 API 契约，Service → 白盒逻辑路径，Mapper → 白盒 SQL 语义。教科书式的分层测试设计。 |
| **黑盒方法论规范化** | 等价类划分 + 边界值分析在 3 个 `TEST_CASE_DESIGN.md` 中有显式文档，不是凭感觉写的。 |
| **薪资模块的白盒深度突出** | `SalaryCalculationTest` 的 24 个用例几乎穷举了计算逻辑的所有异常分支，是白盒测试的标杆。 |
| **安全测试独立维度** | 路径遍历、SQL 注入、XSS、文件类型校验构成独立的安全测试维度，不依赖于功能测试。 |
| **参数化测试减少重复** | `@ParameterizedTest` 在 `SalaryDeductServiceTest`、`SalaryExportTest`、`CityStandardTest` 中广泛使用。 |

### 5.2 ⚠️ 不足之处

| 不足 | 严重程度 | 说明 |
|------|---------|------|
| **缺少代码覆盖率量化指标** | 🔴 高 | 没有 JaCoCo/SonarQube 报告说明行覆盖率/分支覆盖率的具体数字，白盒路径覆盖程度无法量化验证。 |
| **Controller 层白盒深度不够** | 🟡 中 | 大部分 Controller 测试是 Spring Boot 全链路启动——在严格意义上不是纯黑盒(依赖完整 Spring 上下文)，也不是纯白盒(没有精确 Mock Service 依赖)。这导致 Controller 异常分支无法被强制触发。 |
| **缺少控制流/数据流测试** | 🟡 中 | 白盒测试中没有看到基于 `if/else/switch` 控制流图设计的测试，也没有基于变量定义-使用链的数据流测试。 |
| **异常分支覆盖不均衡** | 🟡 中 | 薪资模块的白盒分支覆盖非常详尽，但考勤、请假模块的大量异常分支实际未被验证（如未来日期检测逻辑、冲突检测逻辑都没有真正触发异常分支）。 |
| **缺少状态机测试** | 🟢 低 | 请假审批流程(申请→审核→批准/驳回/撤销)有明显状态机特征，但没有基于状态转换图设计测试。 |
| **缺少集成测试** | 🟢 低 | 跨模块数据流(如考勤→薪资计算→社保扣款)没有被测试覆盖。 |

---

## 六、改进建议

### 6.1 短期改进（可立即执行）

1. **引入 JaCoCo 生成覆盖率报告**
   ```xml
   <!-- pom.xml -->
   <plugin>
       <groupId>org.jacoco</groupId>
       <artifactId>jacoco-maven-plugin</artifactId>
       <version>0.8.11</version>
   </plugin>
   ```
   目标：行覆盖率 ≥ 80%，分支覆盖率 ≥ 70%。

2. **为请假状态机设计转换测试**
   ```
   UNAUDITED → APPROVE ✓ (TC-LEAVE-012)
   UNAUDITED → REJECT  ✓ (TC-LEAVE-013)
   UNAUDITED → CANCEL  ✓ (TC-LEAVE-027)
   APPROVE   → CANCEL  ✗ (缺失)
   REJECT    → REAPPLY  ✗ (缺失)
   ```

3. **补充考勤异常分支的强制触发测试**
   - 使用 `@MockBean` 替换 AttendanceController 依赖的 Service，强制触发 403/异常路径
   - 解决当前列表查询全部返回 403 的权限码对齐问题

### 6.2 中期改进

4. **Controller 层增加纯 Mock 白盒测试**
   ```java
   @WebMvcTest(StaffController.class)  // 仅加载 Controller 层
   class StaffControllerUnitTest {
       @MockBean private StaffService staffService;  // 精确控制
       // 可强制触发 Service 层异常分支
   }
   ```

5. **引入数据流测试**
   - 对薪资计算链路：`Salary.setBaseSalary()` → `SalaryService.list()` → `StaffSalaryVO.totalSalary`，设计 define-use 路径测试

6. **补充跨模块集成测试**
   - 考勤数据 → 薪资计算 → 社保扣款 的端到端验证

---

## 七、总结

| 维度 | 评价 |
|------|------|
| **黑盒方法论** | ⭐⭐⭐⭐⭐ 等价类划分 + 边界值分析规范化，TEST_CASE_DESIGN.md 文档完整 |
| **白盒路径覆盖** | ⭐⭐⭐⭐ 薪资模块优秀，但其他模块路径覆盖不均衡 |
| **分层策略** | ⭐⭐⭐⭐⭐ Controller(黑盒) / Service(白盒) / Mapper(白盒) 分工明确 |
| **安全测试** | ⭐⭐⭐⭐ 独立维度，覆盖路径遍历/SQL注入/XSS/文件校验 |
| **可量化程度** | ⭐⭐ 缺少覆盖率工具数据支撑 |
| **状态机测试** | ⭐⭐ 请假审批状态转换未完整覆盖 |
| **集成测试** | ⭐⭐ 跨模块数据流链路的端到端测试缺失 |

**整体评级**：**B+（良好，有明确的改进路径）**

---

> 📝 本文档供后续测试优化参考。相关文档：[[TCA.md]](TCA.md)

---

## 八、Controller 层白盒测试扩展分析

> 本章节分析当前 12 个 Controller 的**内部实现细节**，识别哪些方法包含纯透传之外的逻辑（异常处理、状态判断、响应转换），并给出可添加的白盒测试方案。

### 8.1 分析框架

判断一个 Controller 方法是否需要白盒测试的唯一标准：

> **该方法是否包含 `return this.xxxService.xxx()` 以外的逻辑？**

- 若只做透传 → 黑盒 API 契约测试已足够
- 若包含 `try-catch`、条件判断、响应转换、异常重抛出 → 需要白盒测试

### 8.2 逐 Controller 内部逻辑扫描

通过阅读全部 Controller 源码，汇总如下：

| Controller | 总方法数 | 纯透传方法 | 含内部逻辑方法 | 判定 |
|-----------|---------|-----------|-------------|------|
| `DocsController` | 11 | 9 | 2 (`download`, `getAvatar`) | ✅ **可加白盒** |
| `StaffLeaveController` | 14 | 14 | 0 | ❌ 仅透传 |
| `StaffOvertimeController` | 10 | 10 | 0 | ❌ 仅透传 |
| `AttendanceController` | 10 | 10 | 0 | ❌ 仅透传 |
| `StaffController` | 14 | 14 | 0 | ❌ 仅透传 |
| `DeptController` | 11 | 11 | 0 | ❌ 仅透传 |
| `SalaryController` | 8 | 8 | 0 | ❌ 仅透传 |
| `InsuranceController` | 9 | 9 | 0 | ❌ 仅透传 |
| `CityController` | 9 | 9 | 0 | ❌ 仅透传 |
| `LoginController` | 1 | 1 | 0 | ❌ 仅透传 |
| `RoleController` | 10 | 10 | 0 | ❌ 仅透传 |
| `MenuController` | 10 | 10 | 0 | ❌ 仅透传 |
| `SalaryDeductController` | 10 | 10 | 0 | ❌ 仅透传 |
| `LeaveController` | 8 | 8 | 0 | ❌ 仅透传 |
| `OvertimeController` | 8 | 8 | 0 | ❌ 仅透传 |
| **全局异常处理器** | 1 类 | — | 6 个 `@ExceptionHandler` | ✅ **可加白盒** |

> **核心发现**：90% 的 Controller 方法是纯透传（一行 `return this.xxxService.xxx()`），不适合也不需要在 Controller 层做白盒测试。真正的白盒目标只有 **2 类**：
>
> 1. 包含 `try-catch` 的方法：`DocsController.download()` / `getAvatar()`
> 2. 全局异常处理器：`BaseExceptionHandler` 的 6 个 `@ExceptionHandler`

---

### 8.3 白盒目标一：`DocsController.download()` / `getAvatar()`

#### 8.3.1 源码级分支分析

```java
// DocsController.java 第 100-122 行
@GetMapping("/download/{filename}")
public void download(@PathVariable String filename, HttpServletResponse response) throws IOException {
    try {
        this.docsService.download(filename, response);     // 路径 A: 正常下载
    } catch (ServiceException e) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文件不存在"); // 路径 B: 业务异常 → 404
    } catch (IllegalArgumentException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法请求"); // 路径 C: 非法参数 → 400
    }
}
```

```
                         ┌─ download(filename) ─┐
                         │                      │
                    ServiceException    IllegalArgumentException
                         │                      │
                    NOT_FOUND (404)       BAD_REQUEST (400)
```

#### 8.3.2 当前测试覆盖情况

| 分支 | 触发条件 | 当前是否覆盖 | 当前测试方法 |
|------|---------|------------|------------|
| **路径 A**（正常下载） | `docsService.download()` 成功 | ✅ 已覆盖 | `testDownloadFile_Success()` |
| **路径 B**（文件不存在 → 404） | Service 抛出 `ServiceException` | ⚠️ **间接覆盖** | `testDownloadFile_NonExistent()` — 依赖真实 DB 状态 |
| **路径 B**（显式触发 → 404） | 用 Mock 强制 Service 抛异常 | ❌ **未覆盖** | — |
| **路径 C**（非法参数 → 400） | Service 抛出 `IllegalArgumentException` | ❌ **未覆盖** | — |
| **finally 块** | （本方法无 finally） | — | — |

#### 8.3.3 建议添加的白盒测试

```java
@WebMvcTest(DocsController.class)               // 仅加载 Controller 层
class DocsControllerWhiteBoxTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private DocsService docsService;   // 精确控制 Service 行为

    // ========== download() 白盒测试 ==========

    /**
     * TC-DOCS-WB-001: 分支 B — ServiceException → 404
     * 当前覆盖：间接（依赖文件不存在）
     * 白盒价值：不依赖文件系统，精确验证异常→HTTP状态的映射
     */
    @Test
    void testDownload_ServiceException_MapsTo404() throws Exception {
        when(docsService.download(eq("any.pdf"), any()))
            .thenThrow(new ServiceException(BusinessStatusEnum.FILE_NOT_EXIST));

        mockMvc.perform(get("/docs/download/{filename}", "any.pdf"))
            .andExpect(status().isNotFound())     // 验证 HTTP 404
            .andExpect(status().reason("文件不存在"));  // 验证原因短语
    }

    /**
     * TC-DOCS-WB-002: 分支 C — IllegalArgumentException → 400
     * 当前覆盖：❌ 未覆盖
     * 白盒价值：这是唯一能触发 IllegalArgumentException 转换为 400 的路径
     */
    @Test
    void testDownload_IllegalArgument_MapsTo400() throws Exception {
        when(docsService.download(eq(""), any()))
            .thenThrow(new IllegalArgumentException("文件名不能为空"));

        mockMvc.perform(get("/docs/download/{filename}", ""))
            .andExpect(status().isBadRequest())   // 验证 HTTP 400
            .andExpect(status().reason("非法请求"));
    }

    /**
     * TC-DOCS-WB-003: 分支 B 和 C 的顺序验证
     * 当前覆盖：❌ 未覆盖
     * 白盒价值：验证 catch 块顺序 — ServiceException 必须是
     *          IllegalArgumentException 的父类才不会被后者捕获
     *          (当前两个是并列关系，无顺序问题，但值得文档化)
     */
    @Test
    void testDownload_CatchOrder_ServiceExceptionFirst() throws Exception {
        // ServiceException 和 IllegalArgumentException 无继承关系，
        // catch 顺序不影响结果，但验证两者独立工作
        when(docsService.download(eq("x"), any()))
            .thenThrow(new ServiceException(BusinessStatusEnum.ERROR));

        mockMvc.perform(get("/docs/download/{filename}", "x"))
            .andExpect(status().isNotFound());    // 确认走 ServiceException 分支
    }

    // ========== getAvatar() 白盒测试（逻辑与 download() 完全相同） ==========

    /**
     * TC-DOCS-WB-004: getAvatar — ServiceException → 404
     */
    @Test
    void testGetAvatar_ServiceException_MapsTo404() throws Exception {
        when(docsService.download(eq("avatar_x.jpg"), any()))
            .thenThrow(new ServiceException(BusinessStatusEnum.FILE_NOT_EXIST));

        mockMvc.perform(get("/docs/avatar/{filename}", "avatar_x.jpg"))
            .andExpect(status().isNotFound());
    }

    /**
     * TC-DOCS-WB-005: getAvatar — IllegalArgumentException → 400
     */
    @Test
    void testGetAvatar_IllegalArgument_MapsTo400() throws Exception {
        when(docsService.download(eq(""), any()))
            .thenThrow(new IllegalArgumentException("文件名不能为空"));

        mockMvc.perform(get("/docs/avatar/{filename}", ""))
            .andExpect(status().isBadRequest());
    }

    /**
     * TC-DOCS-WB-006: getAvatar — 无需认证（与 download() 的关键差异）
     * 当前覆盖：间接（testDownloadAvatar_NoAuth → 404）
     * 白盒价值：验证 @PreAuthorize 注解不存在于 getAvatar() 上
     */
    @Test
    void testGetAvatar_NoAuthenticationRequired() throws Exception {
        // getAvatar() 方法上没有 @PreAuthorize 注解 — 公开接口
        when(docsService.download(eq("avatar_1.jpg"), any()))
            .thenThrow(new ServiceException(BusinessStatusEnum.FILE_NOT_EXIST));

        // 不添加任何认证信息
        mockMvc.perform(get("/docs/avatar/{filename}", "avatar_1.jpg"))
            .andExpect(status().isNotFound())     // 走异常分支，而非 401
            .andExpect(status().reason("文件不存在"));
    }
}
```

**预期新增**：**6 个白盒用例**，覆盖 2 个方法 × 3 个分支维度。

---

### 8.4 白盒目标二：`BaseExceptionHandler` 全局异常处理器

#### 8.4.1 源码级分支分析

```java
@ControllerAdvice
public class BaseExceptionHandler {

    @ExceptionHandler(ServiceException.class)           // Handler 1: 400 + code来自异常
    @ExceptionHandler(IllegalArgumentException.class)   // Handler 2: 区分 "文件名不能为空" 消息
    @ExceptionHandler(DataIntegrityViolationException.class) // Handler 3: 统一返回 code=300
    @ExceptionHandler(RequestRejectedException.class)   // Handler 4: 统一返回 code=300
    @ExceptionHandler(NullPointerException.class)       // Handler 5: 统一返回 code=300
    @ExceptionHandler(MethodArgumentTypeMismatchException) // Handler 6: 400 + code=300
}
```

#### 8.4.2 当前测试覆盖情况

| Handler | 当前是否覆盖 | 当前触发方式 |
|---------|------------|------------|
| `ServiceException` | ⚠️ 间接 | 部分 Service 测试触发，但未显式验证 handler 映射 |
| `IllegalArgumentException` | ❌ 未覆盖 | 分支逻辑（`"文件名不能为空"` vs 通用）**完全未测试** |
| `DataIntegrityViolationException` | ⚠️ 间接 | Controller 测试中数据库约束违反触发，但未显式验证 |
| `RequestRejectedException` | ❌ 未覆盖 | Spring Security 触发路径遍历拦截时产生，但被 Controller 400 掩盖 |
| `NullPointerException` | ❌ 未覆盖 | 无测试显式触发 NPE 并验证 handler 响应 |
| `MethodArgumentTypeMismatchException` | ⚠️ 间接 | `testQuery_InvalidIdFormat()` 触发，但验证的是 400 而非 handler 的 code=300 |

#### 8.4.3 `IllegalArgumentException` Handler 的分支逻辑（最复杂）

```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseDTO handleIllegalArgument(IllegalArgumentException exception) {
    if ("文件名不能为空".equals(exception.getMessage())) {     // 分支 1: 特定消息
        return Response.error(BusinessStatusEnum.FILE_NOT_EXIST);  // code=600
    }
    return Response.error(BusinessStatusEnum.ERROR);             // 分支 2: 通用 → code=300
}
```

这是 **Controller 层唯一包含 if/else 分支的方法**，但当前 **没有任何测试覆盖它**。

#### 8.4.4 建议添加的白盒测试

```java
@WebMvcTest                                      // 加载 Controller + ControllerAdvice
class BaseExceptionHandlerWhiteBoxTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private DocsService docsService;   // 作为异常抛出的入口点

    // ========== IllegalArgumentException Handler ==========

    /**
     * TC-EXH-WB-001: IllegalArgumentException 分支 1 — 文件名为空 → code=600
     * 当前覆盖：❌ 未覆盖
     * 白盒价值：验证 if("文件名不能为空".equals(msg)) 分支
     */
    @Test
    void testIllegalArgument_EmptyFileName_ReturnsFileNotExist() throws Exception {
        when(docsService.download(eq(""), any()))
            .thenThrow(new IllegalArgumentException("文件名不能为空"));

        mockMvc.perform(get("/docs/download/{filename}", ""))
            .andExpect(jsonPath("$.code").value(600))   // 验证 FILE_NOT_EXIST
            .andExpect(jsonPath("$.message").exists());
    }

    /**
     * TC-EXH-WB-002: IllegalArgumentException 分支 2 — 其他消息 → code=300
     * 当前覆盖：❌ 未覆盖
     * 白盒价值：验证 else 分支（通用错误）
     */
    @Test
    void testIllegalArgument_OtherMessage_ReturnsError() throws Exception {
        when(docsService.download(eq("test.pdf"), any()))
            .thenThrow(new IllegalArgumentException("其他非法参数"));

        mockMvc.perform(get("/docs/download/{filename}", "test.pdf"))
            .andExpect(jsonPath("$.code").value(300));   // 验证通用 ERROR
    }

    // ========== ServiceException Handler ==========

    /**
     * TC-EXH-WB-003: ServiceException → 400 + 异常中的 code
     * 当前覆盖：⚠️ 间接
     * 白盒价值：验证 handler 返回的 code 来自异常的 getCode() 而非固定值
     */
    @Test
    void testServiceException_PreservesExceptionCode() throws Exception {
        when(docsService.download(eq("x"), any()))
            .thenThrow(new ServiceException(BusinessStatusEnum.STAFF_NOT_EXIST));

        mockMvc.perform(get("/docs/download/{filename}", "x"))
            .andExpect(status().isBadRequest())            // HTTP 400
            .andExpect(jsonPath("$.code")
                .value(BusinessStatusEnum.STAFF_NOT_EXIST.getCode()));
    }

    // ========== DataIntegrityViolationException Handler ==========

    /**
     * TC-EXH-WB-004: DataIntegrityViolationException → code=300
     * 当前覆盖：⚠️ 间接（通过数据库约束违反触发）
     * 白盒价值：不依赖数据库，精确验证 handler 映射
     */
    @Test
    void testDataIntegrityViolation_ReturnsError() throws Exception {
        when(docsService.download(eq("x"), any()))
            .thenThrow(new DataIntegrityViolationException("违反唯一约束"));

        // DataIntegrityViolationException 被 @ControllerAdvice 捕获
        // 但注意：Controller 层 try-catch 只捕获 ServiceException 和 IllegalArgumentException
        // 所以这个异常会穿透 Controller try-catch，到达 @ControllerAdvice
        mockMvc.perform(get("/docs/download/{filename}", "x"))
            .andExpect(jsonPath("$.code").value(300));
    }

    // ========== NullPointerException Handler ==========

    /**
     * TC-EXH-WB-005: NullPointerException → code=300
     * 当前覆盖：❌ 未覆盖
     * 白盒价值：验证 NPE 被优雅处理而非返回 500
     */
    @Test
    void testNullPointer_ReturnsError() throws Exception {
        when(docsService.download(eq("x"), any()))
            .thenThrow(new NullPointerException("staff is null"));

        mockMvc.perform(get("/docs/download/{filename}", "x"))
            .andExpect(jsonPath("$.code").value(300));
    }

    // ========== MethodArgumentTypeMismatchException Handler ==========

    /**
     * TC-EXH-WB-006: URL 参数类型不匹配 → 400 + code=300
     * 当前覆盖：⚠️ 间接（testQuery_InvalidIdFormat）
     * 白盒价值：显式验证 handler 而非依赖 Spring 默认行为
     */
    @Test
    void testTypeMismatch_ReturnsError() throws Exception {
        // 发送 "abc" 到需要 Integer 的路径变量
        mockMvc.perform(get("/staff/{id}", "abc"))
            .andExpect(status().isBadRequest())         // 验证 HTTP 400
            .andExpect(jsonPath("$.code").value(300));  // 验证 code=300
    }
}
```

**预期新增**：**6 个白盒用例**，覆盖全局异常处理器的 6 个 `@ExceptionHandler`，其中 `IllegalArgumentException` 的 2 个分支各自独立测试。

---

### 8.5 白盒目标三：透传 Controller 中隐含的逻辑

虽然 90% 的 Controller 方法是纯透传，但仍有一些**隐式逻辑**值得白盒验证：

#### 8.5.1 `@PreAuthorize` SpEL 表达式精确匹配

```java
// 当前测试：@WithMockUser(authorities = {"system:staff:add"}) → 200 ✓
//           @WithMockUser(authorities = {}) → 403 ✓

// 缺失的白盒测试：
// TC-AUTH-WB-001: 拥有父级权限不等于拥有子级权限
//   @WithMockUser(authorities = {"system:staff"})
//   → 不能通过 hasAnyAuthority('system:staff:add') → 应返回 403

// TC-AUTH-WB-002: hasAnyAuthority 多值中任一命中即可
//   @WithMockUser(authorities = {"system:staff:list"})
//   → 能通过 hasAnyAuthority('system:staff:list','system:staff:search') → 应返回 200
```

**新增**：**2 个 SpEL 精确验证用例**。

#### 8.5.2 `@RequestParam(defaultValue = "...")` 默认值应用

```java
// 当前测试：显式传参 → 验证成功
//          不传参 → 验证使用默认值

// 缺失的白盒测试：
// TC-PARAM-WB-001: 传入空字符串 "" 时，defaultValue 是否生效
//   GET /staff?current=&size=
//   → @RequestParam(defaultValue = "1") Integer current
//   → 应使用默认值 1 还是触发类型转换异常？

// TC-PARAM-WB-002: 传入空白字符串 "  " 时行为
//   GET /staff?current=   &size=
```

**新增**：**2 个参数处理边界用例**。

---

### 8.6 建议新增的白盒测试汇总

| 类别 | 目标 | 新增用例数 | 技术方案 |
|------|------|-----------|---------|
| 🌐 **全局异常处理器** | `BaseExceptionHandler` 6 个 Handler | **6** | `@WebMvcTest` + `@MockBean` 注入异常 |
| 📁 **文件下载异常分支** | `DocsController.download()` / `getAvatar()` | **6** | `@WebMvcTest` + `@MockBean DocsService` |
| 🔐 **SpEL 权限表达式** | `@PreAuthorize` 精确匹配 | **2** | `@WebMvcTest` + `@WithMockUser` 细粒度控制 |
| 📐 **参数默认值/边界** | `@RequestParam(defaultValue)` | **2** | `@WebMvcTest` + 边界参数值 |
| **合计** | — | **16** | — |

### 8.7 不建议在 Controller 层加白盒的模块

以下模块的 Controller 方法**100% 纯透传**，白盒测试应该放在其对应的 **Service 层**：

| Controller | 有意义的白盒测试位置 | 原因 |
|-----------|------------------|------|
| `StaffLeaveController` | `StaffLeaveService.apply()` / `complete()` / `cancel()` | 工作流状态机逻辑在 Service 中 |
| `StaffOvertimeController` | `StaffOvertimeService.setOvertime()` | 加班时长/类型校验在 Service 中 |
| `AttendanceController` | `AttendanceService.add()` / `setAttendance()` | 考勤时间验证在 Service 中 |
| `DeptController` | `DeptService.add()` / `edit()` | 时间校验、delete/query 防护在 Service 中 |
| `SalaryController` | `SalaryService.setSalary()` | 薪资计算逻辑在 Service 中 |
| `InsuranceController` | `InsuranceService.setInsurance()` | 社保基数校验在 Service 中 |
| `CityController` | `CityService.add()` | 城市标准校验在 Service 中 |

这 7 个 Controller 的 Service 层 **已有部分白盒测试**（见 TCA.md 第十~十二章），按现有模式扩展即可。

### 8.8 实施优先级

| 优先级 | 类别 | 用例数 | 理由 |
|--------|------|--------|------|
| 🔴 **P0** | 全局异常处理器 (8.4) | **6** | 覆盖整个系统的错误响应一致性，影响面最广 |
| 🔴 **P0** | DocsController 异常分支 (8.3) | **6** | 唯一含 try-catch 的 Controller 方法，当前仅间接覆盖 |
| 🟡 **P1** | SpEL 权限精确匹配 (8.5.1) | **2** | 防止权限配置错误导致越权访问 |
| 🟢 **P2** | 参数默认值边界 (8.5.2) | **2** | 边界行为文档化，影响面较小 |

---

## 九、Service 层白盒测试扩展分析

> 通过阅读全部 19 个 Service 源码，逐方法识别 `save()`/`updateById()` 返回值判断之上的**业务分支逻辑**，判断当前白盒覆盖是否充分。

### 9.1 Service 层内部逻辑复杂度总览

| Service | 总方法数 | 纯透传方法 | 含分支逻辑方法 | 当前 Service 测试 | 白盒缺口 |
|---------|---------|-----------|-------------|-----------------|---------|
| `StaffService` | 11 | 2 | 9 | ✅ `StaffServiceTest` (26用例) | ⚠️ 小 |
| `DeptService` | 12 | 3 | 9 | ❌ **无** | 🔴 **大** |
| `DocsService` | 10 | 5 | 5 | ❌ **无** | 🔴 **大** |
| `StaffLeaveService` | 15 | 5 | 10 | ❌ **无** | 🔴 **大** |
| `AttendanceService` | 11 | 3 | 8 | ❌ **无** | 🔴 **大** |
| `StaffOvertimeService` | 12 | 2 | 10 | ❌ **无** | 🔴 **大** |
| `InsuranceService` | 10 | 9 | 1 | ✅ 3个测试文件 (32用例) | 🟢 小 |
| `CityService` | 10 | 9 | 1 | ✅ `CityStandardTest` (4用例) | 🟢 小 |
| `SalaryService` | — | — | — | ✅ 4个测试文件 (65用例) | 🟢 小 |
| `SalaryDeductService` | — | — | — | ✅ 1个测试文件 (13用例) | 🟢 小 |
| 其余 9 个 Service | 多数透传 | — | — | ❌ 无 | 🟢 低优先级 |

> **核心发现**：19 个 Service 中有 **6 个完全没有任何 Service 层测试**（`DeptService`、`DocsService`、`StaffLeaveService`、`AttendanceService`、`StaffOvertimeService`、`LoginService` 等），而这 6 个中有 **5 个包含大量分支逻辑**——白盒测试缺口显著。

---

### 9.2 🔴 P0：`DeptService` — 含 9 个分支方法，零白盒测试

#### 9.2.1 当前状态

| 覆盖来源 | 深度 |
|---------|------|
| `DeptControllerTest` (20 用例) | 黑盒 — 通过 MockMvc 间接触发 Service |
| 专属 Service 测试 | ❌ **不存在** |

黑盒 Controller 测试能触发部分路径，但以下分支**无法通过 Controller 层触发**：

#### 9.2.2 未覆盖的分支清单

**`query()` 方法中的异常抛出**（Controller 层 Spring 类型转换拦截，无法触发）：

```java
// 分支 Q1: id == null → IllegalArgumentException
// 分支 Q2: id <= 0 → IllegalArgumentException
```

#### 9.2.3 循环引用检测代码的评估

`edit()` 方法第 121-142 行的 `parentId` 自引用检查和循环引用 while 遍历是**后期添加的防御代码**，不在原系统设计中。实际业务流程中，前端仅支持"为父部门添加子部门"，**不支持为已有子部门重新指定父部门**，因此 `edit()` 被调用时 `parentId` 不会发生变化——这两个分支在正常业务流程中**不可达**。

> ⚠️ 基于与覆盖率估计一致的原则（前端约束导致不可达 → 排除），**TC-DEPT-WB-003、TC-DEPT-WB-004、TC-DEPT-WB-005 从白盒测试计划中删除**。代码本身保留作为 API 直调防御层，但不需要专门编写测试。

#### 9.2.4 建议新增的白盒测试（4 个）

| 编号 | 测试方法 | 覆盖路径 | 当前状态 |
|------|---------|---------|---------|
| TC-DEPT-WB-001 | `testAdd_PartialTimeFields_ReturnsError()` | 时间字段不完整校验 | ⚠️ 间接 |
| TC-DEPT-WB-002 | `testDelete_NullId_ReturnsError()` | null id → ERROR | ❌ 未覆盖 |
| TC-DEPT-WB-003 | `testQuery_NullId_ThrowsIllegalArgumentException()` | null id → `IllegalArgumentException` | ❌ 无法通过 Controller 触发 |
| TC-DEPT-WB-004 | `testQuery_NonPositiveId_ThrowsIllegalArgumentException()` | id≤0 → `IllegalArgumentException` | ❌ 无法通过 Controller 触发 |

---

### 9.3 🔴 P0：`DocsService` — 含 5 个分支方法，零白盒测试

#### 9.3.1 源码分支分析

`DocsService.upload()` 方法（71-133 行）是**全系统最长的单方法校验链**：

```
upload(file, id)
  ├─ [B1] staffId 不存在        → ERROR
  ├─ [B2] 文件夹不存在           → mkdirs
  ├─ [B3] file.isEmpty()        → ERROR
  ├─ [B4] extName 为空/null      → ERROR
  ├─ [B5] ext 不在白名单内        → ERROR
  ├─ [B6] originalFilename > 100 → ERROR
  ├─ [B7] file.size > 20MB      → ERROR
  ├─ [B8] MD5 已存在            → 跳过上传（复用已有文件）
  ├─ [B9] transferTo 失败       → ServiceException
  └─ [B10] save 失败            → ERROR
```

`DocsService.download()` 方法（145-172 行）含 4 个安全校验分支：

```
download(filename, response)
  ├─ [D1] filename 为空          → ServiceException (→ Controller: 404)
  ├─ [D2] filename 含 ".."       → IllegalArgumentException (→ Controller: 400)
  ├─ [D3] filename 含 "/" 或 "\" → IllegalArgumentException (→ Controller: 400)
  ├─ [D4] 文件不存在              → ServiceException (→ Controller: 404)
  └─ [D5] 正常下载
```

#### 9.3.2 建议新增的白盒测试（7 个）

| 编号 | 测试方法 | 覆盖路径 | 当前状态 |
|------|---------|---------|---------|
| TC-DOCS-WB-001 | `testUpload_EachDisallowedExtension_ReturnsError()` | B5: 参数化测试 .exe/.bat/.sh/.js/.py | ⚠️ 仅 .exe |
| TC-DOCS-WB-002 | `testUpload_DuplicateMd5_SkipsFileStorage()` | B8: MD5 去重逻辑 | ❌ 未覆盖 |
| TC-DOCS-WB-003 | `testUpload_TransferToFails_ThrowsServiceException()` | B9: 磁盘写入失败 → ServiceException | ❌ 未覆盖 |
| TC-DOCS-WB-004 | `testUpload_Exactly20MB_Succeeds()` | B7: 边界值 ==20MB → 成功 | ❌ 未覆盖 |
| TC-DOCS-WB-005 | `testUpload_FilenameExactly100Chars_Succeeds()` | B6: 边界值 ==100字符 → 成功 | ❌ 未覆盖 |
| TC-DOCS-WB-006 | `testDownload_BackslashPath_ThrowsIllegalArgumentException()` | D3: `\\` 路径遍历（Windows） | ❌ 未覆盖 |
| TC-DOCS-WB-007 | `testDownload_BothDotDotAndSlash_FirstIfCatches()` | D2+D3: `".."` 在 `"/"` 之前捕获 | ❌ 未覆盖 |

---

### 9.4 🔴 P0：`StaffLeaveService` — 状态机 + 工作流，零白盒测试

#### 9.4.1 最关键的未覆盖分支

**`apply()` — 冲突检测**：

```java
// 冲突检测范围包含 REJECT 状态 — 驳回后仍不能重新申请？
List<StaffLeave> conflicts = selectList(
    eq("staff_id", id).and(i -> i
        .eq("status", UNAUDITED).or()
        .eq("status", REJECT).or()   // ← REJECT 也算冲突？
        .eq("status", AUDITING))
);
```

**`apply()` — 工作流静默失败**：

```java
try {
    this.runtimeService.startProcessInstanceByKey("leave", ...);
    // ...
} catch (Exception e) {
    // 工作流失败不影响请假申请 ← 静默吞掉！无日志！
}
```

**`complete()` — 审批分支**：

```java
if (Objects.equals(task.getTaskDefinitionKey(), "hr_audit")) {
    map.put("hrAuditStatus", staffLeave.getStatus().getCode());
} else if (Objects.equals(task.getTaskDefinitionKey(), "manager_audit")) {
    map.put("managerAuditStatus", staffLeave.getStatus().getCode());
} else {
    map = null;  // ← 未知 taskKey → null map → 行为不确定！
}
taskService.complete(task.getId(), null, map);
```

**`claim()` / `revert()` — 数据不一致风险**：

```java
// claim() 中：updateById 先执行，task 查询后执行
if (!updateById(staffLeave)) { return ERROR; }  // DB 已改
Task task = ...taskCandidateUser(code).singleResult();
if (task == null) { return ERROR; }             // Activiti 未改 → 不一致！
```

#### 9.4.2 建议新增的白盒测试（6 个）

| 编号 | 测试方法 | 覆盖路径 | 风险等级 |
|------|---------|---------|---------|
| TC-LEAVE-WB-001 | `testApply_RejectedLeaveIsConflict()` | REJECT 状态的冲突检测 | 🟡 设计确认 |
| TC-LEAVE-WB-002 | `testApply_WorkflowFails_LeaveStillSaved()` | 工作流异常被静默吞掉 | 🔴 数据风险 |
| TC-LEAVE-WB-003 | `testComplete_UnknownTaskKey_SendsNullVariables()` | M3: map=null → `taskService.complete()` | 🔴 行为不确定 |
| TC-LEAVE-WB-004 | `testComplete_HrAudit_SetsCorrectVariable()` | M1: hr_audit → hrAuditStatus | 🟡 分支未验证 |
| TC-LEAVE-WB-005 | `testComplete_ManagerAudit_SetsCorrectVariable()` | M2: manager_audit → managerAuditStatus | 🟡 分支未验证 |
| TC-LEAVE-WB-006 | `testRevert_TaskNotFound_DataInconsistency()` | claim/revert 中 DB 与 Activiti 不一致 | 🔴 数据风险 |

---

### 9.5 🔴 P0：`AttendanceService` — 考勤状态判定逻辑，零白盒测试

#### 9.5.1 四个 private 方法均未直接测试

```java
isLate():      迟到条件1(morStartTime > dept.morStartTime) OR 迟到条件2(aftStartTime > dept.aftStartTime)
isLeaveEarly(): 早退条件1(morEndTime < dept.morEndTime) OR 早退条件2(aftEndTime < dept.aftEndTime)
isAbsenteeism(): 旷工条件1(四时段任一为null) OR 旷工条件2(isLate && isLeaveEarly)
isLeave():      状态==LEAVE OR 状态==TIME_OFF
```

#### 9.5.2 关键未覆盖路径

```java
// imp() 方法中的判定链 — 优先级逻辑
if (isAbsenteeism(attendance, dept))       → ABSENTEEISM
else if (isLate(attendance, dept))          → LATE
else if (isLeaveEarly(attendance, dept))    → LEAVE_EARLY
else                                        → NORMAL
// ⚠️ 既迟到又早退 → 被 isAbsenteeism 捕获（而非 LATE + LEAVE_EARLY）
// 这个优先级逻辑没有任何测试验证
```

#### 9.5.3 建议新增的白盒测试（7 个）

| 编号 | 测试方法 | 覆盖路径 |
|------|---------|---------|
| TC-ATT-WB-001 | `testIsAbsenteeism_MissingOneTimeSlot_ReturnsTrue()` | 旷工条件1：四时段缺一 |
| TC-ATT-WB-002 | `testIsAbsenteeism_LateAndLeaveEarly_ReturnsTrue()` | 旷工条件2：既迟到又早退 |
| TC-ATT-WB-003 | `testIsLate_OnlyMorningLate_ReturnsTrue()` | 迟到条件1：仅上午 |
| TC-ATT-WB-004 | `testIsLate_OnlyAfternoonLate_ReturnsTrue()` | 迟到条件2：仅下午 |
| TC-ATT-WB-005 | `testIsLate_ExactlyOnTime_ReturnsFalse()` | 边界：踩点不迟到 |
| TC-ATT-WB-006 | `testIsLeaveEarly_OnlyMorningLeaveEarly_ReturnsTrue()` | 早退条件1 |
| TC-ATT-WB-007 | `testImp_LateAndLeaveEarly_ClassifiedAsAbsenteeism()` | 判定优先级验证 |

---

### 9.6 🔴 P0：`StaffOvertimeService` — 加班计算引擎，零白盒测试

#### 9.6.1 计算引擎的 12 个组合路径

```
countType(按小时/按日) × dateType(工作日/休息日/法定假日) × timeOffFlag(调休/不调休)
= 2 × 3 × 2 = 12 个组合路径，当前 0 个被精确测试
```

#### 9.6.2 建议新增的白盒测试（8 个）

| 编号 | 测试方法 | 覆盖路径 |
|------|---------|---------|
| TC-OT-WB-001 | `testCalcSalary_Hourly_Workday_EnoughHours()` | 按小时, ≥2h, 工作日 → 1.5倍 |
| TC-OT-WB-002 | `testCalcSalary_Hourly_InsufficientHours_ReturnsZero()` | 按小时, <2h → 0 |
| TC-OT-WB-003 | `testCalcSalary_Daily_RestDay_NoTimeOff()` | 按日, ≥8h, 休息日不调休 → 2倍 |
| TC-OT-WB-004 | `testCalcSalary_Daily_InsufficientHours_ReturnsZero()` | 按日, <8h → 0 |
| TC-OT-WB-005 | `testCalcSalary_Hourly_Holiday_TripleRate()` | 按小时, 法定假日 → 3倍 |
| TC-OT-WB-006 | `testImp_RestDay_TimeOff_EnoughHours_SetsTimeOff()` | 休息日调休, ≥8h → TIME_OFF |
| TC-OT-WB-007 | `testImp_RestDay_TimeOff_InsufficientHours_Nothing()` | 休息日调休, <8h → 静默无 |
| TC-OT-WB-008 | `testImp_Holiday_AlwaysSalary()` | 法定假日, 不区分调休 → 直接发加班费 |

---

### 9.7 🟡 P1：`StaffService` — 已有测试但遗漏关键 NPE 路径

| 编号 | 测试方法 | 覆盖路径 | 风险 |
|------|---------|---------|------|
| TC-STAFF-WB-001 | `testList_StaffWithInvalidDeptId_NoNPE()` | `list()` 中 dept 为 null → NPE | 🔴 线上风险 |
| TC-STAFF-WB-002 | `testValidate_NonExistentStaff_NoNPE()` | `validate()` 中 staff 为 null → NPE | 🔴 线上风险 |
| TC-STAFF-WB-003 | `testImp_VerifyFixedDeptIdAssignment()` | `imp()` 中 deptId=13 硬编码行为 | 🟡 行为验证 |

**新增**：**3 个白盒用例**。

---

### 9.8 汇总

| 优先级 | Service | 当前 Service 测试 | 建议新增 | 主要覆盖目标 |
|--------|---------|-----------------|---------|------------|
| 🔴 P0 | `DeptService` | ❌ 无 | **4** | 时间校验完整性、delete/query 的 null 防护 |
| 🔴 P0 | `DocsService` | ❌ 无 | **7** | 扩展名白名单、MD5 去重、磁盘失败、路径遍历分支 |
| 🔴 P0 | `StaffLeaveService` | ❌ 无 | **6** | 状态机冲突、工作流静默失败、complete() 审批分支、数据一致性 |
| 🔴 P0 | `AttendanceService` | ❌ 无 | **7** | 4 个 private 判定方法、判定优先级 |
| 🔴 P0 | `StaffOvertimeService` | ❌ 无 | **8** | 加班费计算（小时/日 × 工作日/休息日/假日 × 调休/不调休） |
| 🟡 P1 | `StaffService` | ✅ 26 用例 | **3** | NPE 防护（dept=null、staff=null） |
| **合计** | **6 个 Service** | — | **35** | — |

### 9.9 不建议加白盒的 Service

| Service | 原因 |
|---------|------|
| `InsuranceService` | 已有 3 个测试文件 32 用例，覆盖充分 |
| `CityService` | 纯 MyBatis-Plus CRUD 透传，仅 `list()` 有 1 个 if |
| `SalaryService` | 已有 4 个测试文件 65 用例，全系统测试最充分 |
| `SalaryDeductService` | 已有 1 个测试文件 13 用例 |
| `RoleService` / `MenuService` | 纯透传 |
| `LoginService` | 业务逻辑简单 |
| 其余简单 Service | 方法体 < 5 行 |

### 9.10 与 Controller 层分析的汇总

```
                     Controller 层白盒        Service 层白盒
                     ────────────────        ──────────────
可加白盒的模块数          1 (DocsController)      6 (Dept/Docs/Leave/Attendance/Overtime/Staff)
建议新增用例数           16                       35
核心覆盖目标              异常→HTTP 映射            业务分支/NPE/计算逻辑/状态机
```

**总计建议新增**：**51 个白盒用例**（Controller 16 + Service 35），预计可将整体行覆盖率从估算的 ~70% 提升至 ~82%。

---

> 📝 本文档供后续测试优化参考。相关文档：[[TCA.md]](TCA.md)

---

## 附录 A：白盒测试逐模块执行清单

> 按提示词可直接执行，每个提示词已包含目标章节、模块名、用例数量。

### 第一轮（P0 · 业务风险最高）

```
请参考 TE.md 第 9.4 节，为 StaffLeaveService 添加 6 个白盒测试用例
请参考 TE.md 第 9.6 节，为 StaffOvertimeService 添加 8 个白盒测试用例
请参考 TE.md 第 9.5 节，为 AttendanceService 添加 7 个白盒测试用例
```

### 第二轮（P0 · 防御性覆盖）

```
请参考 TE.md 第 9.2 节，为 DeptService 添加 4 个白盒测试用例
请参考 TE.md 第 9.3 节，为 DocsService 添加 7 个白盒测试用例
请参考 TE.md 第 8.4 节，为 BaseExceptionHandler 添加 6 个白盒测试用例
```

### 第三轮（P1/P2 · 补充）

```
请参考 TE.md 第 8.3 节，为 DocsController 添加 6 个白盒测试用例
请参考 TE.md 第 9.7 节，为 StaffService 补充 3 个白盒测试用例
```

### 汇总

| 轮次 | 用例数 | 累计 | 提示词 |
|------|--------|------|--------|
| 第一轮 | 21 | 21 | 3 条 |
| 第二轮 | 17 | 38 | 3 条 |
| 第三轮 | 9 | 47 | 2 条 |
| **合计** | **47** | — | **8 条** |

> 注：合计 47 个用例（不含第八章中 4 个参数/SpEL 边界用例，它们更适合随对应模块一并完成）。
