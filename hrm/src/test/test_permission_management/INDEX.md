# HRM系统 - 员工管理模块单元测试

## 📚 文档导航

### 🚀 快速开始
**新手必读** → [QUICK_START.md](QUICK_START.md)
- 5分钟快速启动指南
- 常见问题排查
- 测试运行方法

### 📖 详细说明
**深入了解** → [README_TEST.md](README_TEST.md)
- 完整的测试说明文档
- 测试覆盖范围详解
- 测试设计原则
- 运行和配置指南

### 📊 测试总结
**概览信息** → [TEST_SUMMARY.md](TEST_SUMMARY.md)
- 测试统计信息
- 测试用例清单
- 覆盖率目标
- 持续改进建议

---

## 📁 测试文件结构

```
hrm/src/test/java/com/qiujie/
│
├── TestConfig.java                          # 测试配置类
│
├── service/
│   └── StaffServiceTest.java               # Service层测试 (28个用例)
│       ├── 新增功能测试 (5个)
│       ├── 删除功能测试 (4个)
│       ├── 更新功能测试 (4个)
│       ├── 查询功能测试 (11个)
│       └── 边界条件测试 (4个)
│
├── controller/
│   ├── SecurityUtils.java                   # 安全认证工具类
│   └── StaffControllerTest.java            # Controller层测试 (30个用例)
│       ├── API接口测试 (20个)
│       ├── 权限控制测试 (6个)
│       └── 异常处理测试 (4个)
│
└── mapper/
    └── StaffMapperTest.java                # Mapper层测试 (25个用例)
        ├── BaseMapper方法测试 (6个)
        ├── 自定义SQL测试 (14个)
        └── 边界条件测试 (5个)
```

**总计**: 3个测试文件，83个测试用例

---

## 🎯 测试覆盖的功能

### ✅ 员工基本信息管理
- [x] 员工新增（自动设置默认密码、工号）
- [x] 员工删除（逻辑删除）
- [x] 员工批量删除
- [x] 员工信息编辑
- [x] 员工信息查询（单条、列表、分页）
- [x] 员工详细信息查询（含部门信息、年龄计算）

### ✅ 密码管理
- [x] 密码验证
- [x] 密码重置
- [x] 密码加密（BCrypt）

### ✅ 角色管理
- [x] 为员工分配角色
- [x] 查询员工角色

### ✅ 权限控制
- [x] 基于Spring Security的权限验证
- [x] 有权限场景测试
- [x] 无权限场景测试（403 Forbidden）
- [x] 未认证场景测试（401 Unauthorized）

### ✅ 数据查询
- [x] 多条件组合查询
- [x] 模糊查询（姓名）
- [x] 精确查询（部门、状态）
- [x] 分页查询
- [x] 空结果集处理

### ✅ 边界条件
- [x] 空值处理
- [x] 特殊字符处理
- [x] 重复数据处理
- [x] 不存在的数据处理
- [x] 枚举值处理

---

## 🚀 快速运行

### 运行所有员工管理测试
```bash
cd hrm
mvn test -Dtest=StaffServiceTest,StaffControllerTest,StaffMapperTest
```

### 运行单个测试类
```bash
# Service层测试
mvn test -Dtest=StaffServiceTest

# Controller层测试
mvn test -Dtest=StaffControllerTest

# Mapper层测试
mvn test -Dtest=StaffMapperTest
```

### 运行单个测试方法
```bash
mvn test -Dtest=StaffServiceTest#testAdd_Success
```

### 使用IDEA运行
1. 打开测试文件
2. 右键点击测试类或测试方法
3. 选择 "Run" 或 "Debug"

---

## 📊 测试统计

| 测试层级 | 测试文件 | 用例数量 | 预计耗时 |
|---------|---------|---------|---------|
| Service层 | StaffServiceTest.java | 28个 | ~10秒 |
| Controller层 | StaffControllerTest.java | 30个 | ~15秒 |
| Mapper层 | StaffMapperTest.java | 25个 | ~8秒 |
| **总计** | **3个** | **83个** | **~33秒** |

---

## 🔧 技术栈

### 测试框架
- **JUnit 5** (Jupiter) - 测试框架
- **Spring Boot Test** - Spring集成测试
- **MockMvc** - Web层测试

### 辅助工具
- **Spring Security Test** - 安全测试
- **Hamcrest** - 匹配器库
- **JSONPath** - JSON断言

### 特性
- **@Transactional** - 测试数据自动回滚
- **@WithMockUser** - 模拟认证用户
- **@DisplayName** - 中文测试描述
- **@BeforeEach** - 测试数据准备

---

## 💡 测试设计亮点

### 1. 分层测试
- **Mapper层**: 测试数据库操作和SQL查询
- **Service层**: 测试业务逻辑和数据验证
- **Controller层**: 测试API接口和权限控制

### 2. 全面覆盖
- 正常场景（Happy Path）
- 异常场景（错误处理）
- 边界场景（极限值、空值）
- 权限场景（有权限、无权限）

### 3. 数据隔离
- 使用 `@Transactional` 确保测试间互不影响
- 每个测试独立准备数据
- 测试后自动清理

### 4. 清晰命名
- 采用 `test{方法}_{场景}` 命名规范
- 使用 `@DisplayName` 提供中文描述
- 一眼看出测试意图

### 5. AAA模式
```java
@Test
void testExample() {
    // Arrange - 准备测试数据
    Staff staff = createTestStaff();
    
    // Act - 执行被测试方法
    ResponseDTO response = staffService.add(staff);
    
    // Assert - 验证结果
    assertEquals(200, response.getCode());
}
```

---

## ⚠️ 前置条件

运行测试前，请确保：

1. **MySQL数据库**
   - 服务已启动
   - 已执行 `hrm.sql` 初始化脚本
   - 配置文件中的连接信息正确

2. **Redis**
   - 服务已启动
   - 配置文件中连接信息正确

3. **Maven依赖**
   - 已下载所有依赖
   - 项目可以正常编译

4. **配置文件**
   - `application.yml` 配置正确
   - 数据库用户名密码正确

---

## 📈 后续扩展计划

### Phase 1: 完善当前模块
- [ ] 增加导入导出功能测试
- [ ] 增加并发场景测试
- [ ] 提高测试覆盖率到90%+

### Phase 2: 扩展其他模块
- [ ] 部门管理测试
- [ ] 角色管理测试
- [ ] 考勤管理测试
- [ ] 薪资管理测试
- [ ] 请假管理测试
- [ ] 加班管理测试

### Phase 3: 高级测试
- [ ] 集成测试（Integration Test）
- [ ] 性能测试（Performance Test）
- [ ] 端到端测试（E2E Test）
- [ ] 压力测试（Stress Test）

### Phase 4: 自动化
- [ ] 配置CI/CD自动测试
- [ ] 设置测试覆盖率门禁
- [ ] 自动生成测试报告
- [ ] Git Hook预提交检查

---

## 🐛 故障排查

### 常见问题

1. **数据库连接失败**
   - 检查MySQL是否启动
   - 检查配置文件中的连接信息
   - 确认数据库已创建

2. **Redis连接失败**
   - 检查Redis是否启动
   - 检查Redis配置

3. **编译失败**
   ```bash
   mvn clean compile
   mvn dependency:resolve
   ```

4. **权限测试失败**
   - 检查 `@WithMockUser` 中的权限字符串
   - 确保与 `@PreAuthorize` 中的一致

详细排查方法请参考 [QUICK_START.md](QUICK_START.md)

---

## 📞 支持与反馈

### 获取帮助
1. 查看相关文档
2. 阅读测试代码注释
3. 联系开发团队
4. 提交Issue

### 贡献测试
1. Fork项目
2. 编写测试用例
3. 提交Pull Request
4. Code Review

---

## 📝 更新日志

### v1.0 (2024-04-22)
- ✅ 完成Service层测试（28个用例）
- ✅ 完成Controller层测试（30个用例）
- ✅ 完成Mapper层测试（25个用例）
- ✅ 编写完整文档
- ✅ 创建辅助工具类

---

## 📄 许可证

本项目遵循项目主许可证。

---

## 👥 维护者

HRM开发团队

---

**最后更新**: 2024-04-22  
**测试版本**: v1.0  
**总测试数**: 83个

---

## 🎉 开始测试

选择一个文档开始：

- 🚀 **新手**: 阅读 [QUICK_START.md](QUICK_START.md)
- 📖 **进阶**: 阅读 [README_TEST.md](README_TEST.md)
- 📊 **概览**: 阅读 [TEST_SUMMARY.md](TEST_SUMMARY.md)

或直接运行测试：
```bash
mvn test -Dtest=StaffServiceTest
```

祝测试顺利！✨
