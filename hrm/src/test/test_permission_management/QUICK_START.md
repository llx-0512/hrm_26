# 单元测试快速启动指南

## 🚀 5分钟快速开始

### 第一步：环境准备

1. **确保数据库已初始化**
   ```bash
   # 执行SQL文件
   mysql -u root -p < hrm.sql
   mysql -u root -p < hrm_activiti.sql
   ```

2. **启动Redis**
   ```bash
   # Windows
   redis-server.exe
   
   # Linux/Mac
   redis-server
   ```

3. **检查配置文件**
   - 打开 `hrm/src/main/resources/application.yml`
   - 确认数据库连接信息正确
   - 确认Redis连接信息正确

### 第二步：运行测试

#### 方式一：使用Maven命令

```bash
# 进入项目目录
cd C:\Users\hp\Desktop\fo11ow-me\hrm\hrm

# 运行所有员工管理相关测试
mvn test -Dtest=StaffServiceTest,StaffControllerTest,StaffMapperTest

# 或者运行单个测试类
mvn test -Dtest=StaffServiceTest
```

#### 方式二：使用IDEA

1. 打开项目
2. 找到测试文件：
   - `src/test/java/com/qiujie/service/StaffServiceTest.java`
   - `src/test/java/com/qiujie/controller/StaffControllerTest.java`
   - `src/test/java/com/qiujie/mapper/StaffMapperTest.java`
3. 右键点击测试类
4. 选择 "Run 'StaffServiceTest'" 或 "Debug 'StaffServiceTest'"

### 第三步：查看结果

#### Maven输出示例
```
[INFO] Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] BUILD SUCCESS
```

#### IDEA输出
- 绿色 ✓ 表示测试通过
- 红色 ✗ 表示测试失败
- 黄色 ⚠ 表示测试跳过

---

## 📋 测试清单

### ✅ Service层测试 (28个用例)

运行命令：
```bash
mvn test -Dtest=StaffServiceTest
```

测试内容：
- [x] 新增员工 (5个)
- [x] 删除员工 (4个)
- [x] 更新员工 (4个)
- [x] 查询员工 (11个)
- [x] 边界条件 (4个)

预计耗时：~10秒

---

### ✅ Controller层测试 (30个用例)

运行命令：
```bash
mvn test -Dtest=StaffControllerTest
```

测试内容：
- [x] API接口测试 (20个)
- [x] 权限控制测试 (6个)
- [x] 异常处理测试 (4个)

预计耗时：~15秒

---

### ✅ Mapper层测试 (25个用例)

运行命令：
```bash
mvn test -Dtest=StaffMapperTest
```

测试内容：
- [x] BaseMapper方法 (6个)
- [x] 自定义SQL查询 (14个)
- [x] 边界条件 (5个)

预计耗时：~8秒

---

## 🔍 常见问题排查

### 问题1：数据库连接失败

**错误信息**:
```
Could not open JDBC Connection for transaction
```

**解决方案**:
1. 检查MySQL是否启动
2. 检查 `application.yml` 中的数据库配置
3. 确认数据库 `hrm` 已创建
4. 确认用户名密码正确

```yaml
spring:
  datasource:
    master:
      jdbc-url: jdbc:mysql://127.0.0.1:3306/hrm?...
      username: root
      password: your_password  # 修改为你的密码
```

---

### 问题2：Redis连接失败

**错误信息**:
```
Cannot get Jedis connection
```

**解决方案**:
1. 启动Redis服务
2. 检查Redis配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: your_redis_password  # 如果有密码
```

---

### 问题3：测试编译失败

**错误信息**:
```
Compilation failure
```

**解决方案**:
```bash
# 清理并重新编译
mvn clean compile

# 下载依赖
mvn dependency:resolve
```

---

### 问题4：权限测试失败

**错误信息**:
```
Expected status code <200> but was <403>
```

**解决方案**:
1. 检查 `@WithMockUser` 注解中的权限
2. 确保权限字符串与Controller中的 `@PreAuthorize` 一致

```java
// 确保这里的权限与Controller中的一致
@WithMockUser(username = "admin", authorities = {"system:staff:add"})
```

---

### 问题5：测试数据冲突

**症状**: 某些测试偶尔失败

**解决方案**:
1. 确保每个测试类都有 `@Transactional` 注解
2. 在 `@BeforeEach` 中初始化测试数据
3. 避免测试之间共享状态

---

## 💡 测试技巧

### 技巧1：只运行失败的测试

```bash
mvn test -Dtest=StaffServiceTest#testAdd_Success
```

### 技巧2：查看详细日志

在 `application.yml` 中添加：
```yaml
logging:
  level:
    com.qiujie: DEBUG
```

### 技巧3：调试测试

在IDEA中：
1. 在测试代码行号左侧点击设置断点
2. 右键选择 "Debug" 而不是 "Run"
3. 使用调试工具逐步执行

### 技巧4：并行运行测试

```bash
# 在pom.xml中配置
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <parallel>methods</parallel>
        <threadCount>4</threadCount>
    </configuration>
</plugin>
```

---

## 📊 测试报告

### 生成HTML报告

```bash
mvn surefire-report:report
```

报告位置：`target/site/surefire-report.html`

### 查看覆盖率（需要配置JaCoCo）

```bash
mvn jacoco:report
```

报告位置：`target/site/jacoco/index.html`

---

## 🎯 下一步

### 1. 阅读详细文档
- `src/test/README_TEST.md` - 完整测试说明
- `src/test/TEST_SUMMARY.md` - 测试设计总结

### 2. 扩展测试
- 为其他模块编写测试（部门、角色、考勤等）
- 增加集成测试
- 增加性能测试

### 3. 持续集成
- 配置Git Hook自动运行测试
- 配置CI/CD流水线
- 设置测试覆盖率门禁

---

## 📞 获取帮助

### 文档
- [JUnit 5官方文档](https://junit.org/junit5/)
- [Spring Boot测试文档](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)

### 内部资源
- 查看已有的测试代码作为参考
- 联系团队成员
- 查看项目Wiki

---

## ✅ 验证清单

运行测试前，请确认：

- [ ] MySQL服务已启动
- [ ] Redis服务已启动
- [ ] 数据库已初始化（执行了SQL文件）
- [ ] `application.yml` 配置正确
- [ ] Maven依赖已下载
- [ ] 项目可以正常编译

全部确认后，运行：
```bash
mvn test
```

祝测试顺利！🎉
