# 员工管理模块单元测试说明

## 📋 测试概览

本测试套件为HRM系统的员工管理模块提供了全面的单元测试覆盖，包括Service层和Controller层的测试。

### 测试文件结构

```
src/test/java/com/qiujie/
├── TestConfig.java                          # 测试配置类
├── service/
│   └── StaffServiceTest.java               # Service层测试（28个测试用例）
└── controller/
    ├── SecurityUtils.java                   # 安全认证工具类
    └── StaffControllerTest.java            # Controller层测试（30个测试用例）
```

## 🎯 测试覆盖范围

### Service层测试 (StaffServiceTest.java)

#### 1. 新增功能测试 (5个用例)
- ✅ 正常新增员工
- ✅ 必填字段为空的情况
- ✅ 验证默认密码设置（加密后的"123"）
- ✅ 验证工号自动生成（格式：staff_{id}）

#### 2. 删除功能测试 (4个用例)
- ✅ 单个员工逻辑删除
- ✅ 删除不存在的员工ID
- ✅ 批量逻辑删除
- ✅ 批量删除空列表

#### 3. 更新功能测试 (4个用例)
- ✅ 正常编辑员工信息
- ✅ 更新员工状态（在职→禁用）
- ✅ 更新员工部门
- ✅ 更新不存在的员工

#### 4. 查询功能测试 (11个用例)
- ✅ 根据ID查询 - 成功场景
- ✅ 根据ID查询 - 不存在的ID
- ✅ 多条件分页查询 - 无条件
- ✅ 按姓名模糊查询
- ✅ 按部门ID查询
- ✅ 按状态查询
- ✅ 组合条件查询
- ✅ 分页功能验证
- ✅ 查询员工详细信息（含部门名称）
- ✅ 年龄自动计算验证
- ✅ 空结果集处理

#### 5. 边界条件测试 (4个用例)
- ✅ 手机号重复情况
- ✅ 性别枚举值修改
- ✅ 特殊字符姓名查询
- ✅ 空结果集分页

### Controller层测试 (StaffControllerTest.java)

#### 1. API接口测试 - 新增 (3个用例)
- ✅ 正常新增 - 有权限
- ✅ 新增失败 - 无权限（403 Forbidden）
- ✅ 新增失败 - 未认证（401 Unauthorized）

#### 2. API接口测试 - 删除 (3个用例)
- ✅ 单个删除 - 成功
- ✅ 删除失败 - 无权限
- ✅ 批量删除 - 成功

#### 3. API接口测试 - 更新 (2个用例)
- ✅ 正常编辑 - 有权限
- ✅ 编辑失败 - 无权限

#### 4. API接口测试 - 查询 (10个用例)
- ✅ 根据ID查询 - 成功
- ✅ 根据ID查询 - 不存在
- ✅ 查询详细信息
- ✅ 分页查询 - 无条件
- ✅ 分页查询 - 按姓名
- ✅ 分页查询 - 按部门
- ✅ 分页查询 - 按状态
- ✅ 分页查询 - 组合条件
- ✅ 查询失败 - 无权限
- ✅ 分页参数默认值

#### 5. 密码相关测试 (3个用例)
- ✅ 验证正确密码
- ✅ 验证错误密码
- ✅ 重置密码

#### 6. 角色管理测试 (2个用例)
- ✅ 为员工设置角色
- ✅ 查询员工角色

#### 7. 异常场景测试 (4个用例)
- ✅ 无效JSON格式
- ✅ 无效的ID格式
- ✅ 无效的页码
- ✅ 无效的页面大小

## 🚀 运行测试

### 前置条件

1. **数据库准备**
   ```sql
   -- 确保hrm数据库已创建并初始化
   -- 执行 hrm.sql 文件
   ```

2. **Redis服务**
   ```bash
   # 确保Redis服务正在运行
   redis-server
   ```

3. **配置文件**
   - 检查 `application.yml` 中的数据库连接配置
   - 检查 Redis 连接配置

### 运行所有测试

```bash
# 在项目根目录（hrm/）下执行
mvn test
```

### 运行特定测试类

```bash
# 只运行Service层测试
mvn test -Dtest=StaffServiceTest

# 只运行Controller层测试
mvn test -Dtest=StaffControllerTest
```

### 运行单个测试方法

```bash
# 运行特定的测试方法
mvn test -Dtest=StaffServiceTest#testAdd_Success
```

### 使用IDE运行

- **IntelliJ IDEA**: 
  - 右键点击测试类或测试方法
  - 选择 "Run 'StaffServiceTest'" 或 "Debug 'StaffServiceTest'"

- **Eclipse**:
  - 右键点击测试类
  - 选择 "Run As" → "JUnit Test"

## 📊 测试特性

### 1. 数据隔离
- 使用 `@Transactional` 注解，每个测试方法执行后自动回滚
- 保证测试之间互不影响
- 不会污染数据库

### 2. 测试命名规范
- 采用 `test{方法名}_{场景}` 的命名方式
- 使用 `@DisplayName` 提供中文描述
- 清晰表达测试意图

### 3. 断言策略
- 验证响应状态码
- 验证返回数据结构
- 验证业务逻辑正确性
- 验证数据库状态变化

### 4. 权限测试
- 使用 `@WithMockUser` 模拟不同权限的用户
- 测试有权限、无权限、未认证等场景
- 验证Spring Security配置正确性

## 🔍 测试设计原则

### AAA模式
每个测试用例遵循 **Arrange-Act-Assert** 模式：

```java
@Test
void testExample() {
    // Arrange - 准备测试数据
    Staff staff = new Staff();
    staff.setName("测试");
    
    // Act - 执行被测试的方法
    ResponseDTO response = staffService.add(staff);
    
    // Assert - 验证结果
    assertEquals(200, response.getCode());
}
```

### 边界值分析
- 空值、null值测试
- 极大值、极小值测试
- 特殊字符测试
- 重复数据测试

### 等价类划分
- 有效输入 vs 无效输入
- 有权限 vs 无权限
- 存在记录 vs 不存在记录

## 📝 测试报告

### 生成测试报告

```bash
# 生成HTML格式的测试报告
mvn surefire-report:report

# 报告位置：target/site/surefire-report.html
```

### 查看覆盖率

```bash
# 需要添加JaCoCo插件到pom.xml
mvn jacoco:report

# 报告位置：target/site/jacoco/index.html
```

## ⚠️ 注意事项

### 1. 测试数据依赖
- 部分测试假设部门ID=1存在
- 如需修改，请调整 `setUp()` 方法中的 `testDeptId`

### 2. 数据库状态
- 测试使用真实数据库
- 由于 `@Transactional`，测试后数据会回滚
- 但自增ID不会回滚

### 3. Spring Security
- Controller测试需要认证
- 使用 `@WithMockUser` 或 `SecurityUtils` 模拟用户
- 确保权限字符串与数据库中的权限一致

### 4. 异步问题
- 当前测试均为同步测试
- 如有异步操作，需要添加等待机制

## 🐛 常见问题

### Q1: 测试失败 - 数据库连接错误
**解决方案**: 检查 `application.yml` 中的数据库配置

### Q2: 测试失败 - Redis连接错误
**解决方案**: 确保Redis服务正在运行

### Q3: 权限测试失败
**解决方案**: 检查 `@WithMockUser` 中的权限是否与 `@PreAuthorize` 中的一致

### Q4: 测试数据冲突
**解决方案**: 确保每个测试都使用 `@Transactional`，或在 `@BeforeEach` 中清理数据

## 📈 后续扩展建议

### 1. 增加集成测试
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StaffIntegrationTest {
    // 完整的端到端测试
}
```

### 2. 增加性能测试
```java
@Test
void testListPerformance() {
    // 测试大量数据下的查询性能
}
```

### 3. 增加并发测试
```java
@Test
void testConcurrentAdd() {
    // 测试并发新增员工
}
```

### 4. Mock外部依赖
```java
@MockBean
private DeptService deptService;

@Test
void testAddWithMock() {
    when(deptService.getById(1)).thenReturn(mockDept);
    // 测试逻辑
}
```

## 📚 参考资料

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Spring Security Test](https://docs.spring.io/spring-security/reference/servlet/test/index.html)
- [MockMvc Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-mvc-test-framework)

## 👥 维护者

如有问题或建议，请联系开发团队。

---

**最后更新**: 2024-04-22  
**测试版本**: v1.0
