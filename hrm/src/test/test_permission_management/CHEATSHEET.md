# 🧪 员工管理测试 - 快速参考卡

## 📋 一键运行命令

```bash
# 进入项目目录
cd C:\Users\hp\Desktop\fo11ow-me\hrm\hrm

# 运行所有员工测试
mvn test -Dtest=StaffServiceTest,StaffControllerTest,StaffMapperTest

# 只运行Service层测试
mvn test -Dtest=StaffServiceTest

# 只运行Controller层测试
mvn test -Dtest=StaffControllerTest

# 只运行Mapper层测试
mvn test -Dtest=StaffMapperTest

# 运行单个测试方法
mvn test -Dtest=StaffServiceTest#testAdd_Success
```

---

## 🎯 测试文件速查

| 测试文件 | 位置 | 用例数 | 测试内容 |
|---------|------|-------|---------|
| StaffServiceTest | `src/test/java/com/qiujie/service/` | 28个 | Service业务逻辑 |
| StaffControllerTest | `src/test/java/com/qiujie/controller/` | 30个 | REST API接口 |
| StaffMapperTest | `src/test/java/com/qiujie/mapper/` | 25个 | 数据库SQL查询 |

---

## ✅ 前置检查清单

运行测试前确认：

- [ ] MySQL已启动
- [ ] Redis已启动
- [ ] 已执行 `hrm.sql`
- [ ] `application.yml` 配置正确
- [ ] Maven依赖已下载

---

## 🔍 常用测试场景

### 测试新增功能
```bash
mvn test -Dtest=StaffServiceTest#testAdd_Success
```

### 测试删除功能
```bash
mvn test -Dtest=StaffServiceTest#testDelete_Success
```

### 测试更新功能
```bash
mvn test -Dtest=StaffServiceTest#testEdit_Success
```

### 测试查询功能
```bash
mvn test -Dtest=StaffServiceTest#testQueryById_Success
```

### 测试权限控制
```bash
mvn test -Dtest=StaffControllerTest#testAdd_NoPermission
```

---

## 🐛 故障快速排查

### 问题：数据库连接失败
```
解决：检查 application.yml 中的数据库配置
     确认MySQL服务已启动
```

### 问题：Redis连接失败
```
解决：启动Redis服务
     检查Redis配置
```

### 问题：编译失败
```
解决：mvn clean compile
     mvn dependency:resolve
```

### 问题：权限测试失败
```
解决：检查 @WithMockUser 中的权限字符串
     确保与 @PreAuthorize 一致
```

---

## 📊 测试统计

- **总测试数**: 83个
- **预计耗时**: ~33秒
- **预期通过率**: 100%
- **代码覆盖率**: ~80%

---

## 📚 文档导航

- 🚀 快速开始: `QUICK_START.md`
- 📖 详细说明: `README_TEST.md`
- 📊 测试总结: `TEST_SUMMARY.md`
- 📈 覆盖矩阵: `TEST_COVERAGE_MATRIX.md`
- 🏠 总索引: `INDEX.md`

---

## 💡 提示

1. **首次运行**: 先阅读 `QUICK_START.md`
2. **查看详细**: 阅读 `README_TEST.md`
3. **了解设计**: 阅读 `TEST_SUMMARY.md`
4. **查看覆盖**: 阅读 `TEST_COVERAGE_MATRIX.md`

---

## 🎉 成功标志

```
[INFO] Tests run: 83, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

看到以上输出表示所有测试通过！✅
