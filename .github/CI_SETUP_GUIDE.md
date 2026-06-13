# HRM 项目 — CI 集成测试部署指南

> 适用环境：GitHub Actions + MySQL 8.0 + Java 17 + Maven  
> 最后更新：2026-06-13

---

## 目录

1. [部署前的准备工作](#1-部署前的准备工作)
2. [GitHub 仓库设置](#2-github-仓库设置)
3. [本地验证 CI 流程](#3-本地验证-ci-流程)
4. [CI 流水线详解](#4-ci-流水线详解)
5. [常见问题排查](#5-常见问题排查)
6. [高级定制](#6-高级定制)

---

## 1. 部署前的准备工作

### 1.1 生成正确的 BCrypt 密码哈希

`init-test-db.sql` 中需要 "123" 的 BCrypt 编码。由于 BCrypt 每次编码结果不同（salt 随机），你需要生成自己版本的哈希。

#### 方法一：在项目中直接运行测试生成（推荐）

```java
// 在 src/test/java 下创建临时类 HashGenerator.java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("123");
        System.out.println("BCrypt hash of '123': " + hash);
    }
}
```

```bash
# 编译并运行
cd hrm
javac -cp "target/classes:$(mvn dependency:build-classpath -q -DincludeScope=compile)" \
  src/test/java/HashGenerator.java -d target/test-classes
java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -q -DincludeScope=compile)" \
  HashGenerator

# 输出示例: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

#### 方法二：使用在线工具（仅用于测试环境）

访问 https://bcrypt-generator.com ，输入 `123`，Rounds 选 10，复制生成的哈希值。

#### 替换 SQL 中的哈希

```sql
-- 将生成的哈希值替换 init-test-db.sql 中这三行的 pwd 字段
INSERT IGNORE INTO sys_staff (..., pwd, ...) VALUES
(1, '管理员', 'admin',   '<你的哈希值>', ...),
(2, '张三',  'zhangsan', '<你的哈希值>', ...),
(3, '李四',  'lisi',     '<你的哈希值>', ...);
```

### 1.2 导出实际数据库表结构（如果表结构与 SQL 脚本不一致）

```bash
# 从本地 MySQL 导出 hrm 数据库的表结构（不含数据）
mysqldump -h 127.0.0.1 -u root -p --no-data --routines --triggers \
  hrm > hrm_schema.sql

# 仅导出需要的业务表（排除 Activiti 自动生成的表）
mysqldump -h 127.0.0.1 -u root -p --no-data \
  hrm \
  sys_dept sys_staff per_role per_menu per_role_menu per_staff_role \
  sys_docs soc_city soc_insurance sal_salary sal_salary_deduct \
  att_attendance att_staff_leave att_staff_overtime att_overtime \
  att_leave sys_validate_code \
  > hrm/src/test/resources/init-test-db.sql
```

#### 对比确认

执行以下命令检查本地表与 SQL 脚本的差异：

```bash
# 查看本地数据库有哪些业务表
mysql -h 127.0.0.1 -u root -p -e "SHOW TABLES;" hrm | grep -v "^act_"

# 用 diff 对比
diff <(grep "CREATE TABLE" hrm/src/test/resources/init-test-db.sql | sort) \
     <(mysqldump -h 127.0.0.1 -u root -p --no-data hrm 2>/dev/null | grep "CREATE TABLE" | sort)
```

### 1.3 确认项目结构

确保以下文件在正确的位置：

```
hrm/
├── .github/
│   └── workflows/
│       └── ci.yml                    ← GitHub Actions 工作流
├── src/
│   ├── main/
│   │   └── resources/
│   │       └── application.yml       ← 主配置（本地开发）
│   └── test/
│       └── resources/
│           ├── application-test.yml  ← CI 测试环境配置
│           └── init-test-db.sql      ← 测试数据库初始化脚本
└── pom.xml
```

---

## 2. GitHub 仓库设置

### 2.1 推送代码到 GitHub

```bash
cd C:/Users/hp/Desktop/fo11ow-me/hrm

# 添加 CI 配置文件
git add .github/workflows/ci.yml
git add hrm/src/test/resources/application-test.yml
git add hrm/src/test/resources/init-test-db.sql
git add .github/CI_SETUP_GUIDE.md

git commit -m "添加 GitHub Actions CI 集成测试工作流"

git push origin UT
```

### 2.2 启用 GitHub Actions

1. 打开仓库页面 → **Settings** → **Actions** → **General**
2. 确保 **"Allow all actions and reusable workflows"** 已选中
3. **Workflow permissions** 选择 **"Read and write permissions"**
4. 点击 **Save**

### 2.3 验证首次运行

1. 推送后，打开仓库 → **Actions** 标签页
2. 应该能看到 **"HRM CI - 集成测试"** 工作流正在运行
3. 如果失败，点击失败的 Job → 展开日志查看具体错误

### 2.4 设置分支保护规则（可选）

```yaml
# 在 Settings → Branches → Add rule 中设置：
Branch name pattern: master
  ✓ Require a pull request before merging
  ✓ Require status checks to pass before merging
    - 勾选 "integration-test"
    - 勾选 "full-test"
```

---

## 3. 本地验证 CI 流程

### 3.1 模拟 CI 环境本地运行

```bash
# Step 1: 创建测试数据库
mysql -h 127.0.0.1 -u root -p -e "CREATE DATABASE IF NOT EXISTS hrm_test CHARACTER SET utf8mb4;"
mysql -h 127.0.0.1 -u root -p -e "CREATE DATABASE IF NOT EXISTS hrm_activiti_test CHARACTER SET utf8mb4;"

# Step 2: 导入表结构和数据
mysql -h 127.0.0.1 -u root -p hrm_test < hrm/src/test/resources/init-test-db.sql

# Step 3: 以 test profile 运行集成测试
cd hrm
mvn test \
  -Dtest="com.qiujie.integration.*Test" \
  -Dspring.profiles.active=test \
  -DfailIfNoTests=false

# Step 4: 生成覆盖率报告
mvn jacoco:report
# 报告位置: target/site/jacoco/index.html

# Step 5: 清理测试数据库（可选）
mysql -h 127.0.0.1 -u root -p -e "DROP DATABASE IF EXISTS hrm_test;"
mysql -h 127.0.0.1 -u root -p -e "DROP DATABASE IF EXISTS hrm_activiti_test;"
```

### 3.2 使用 act 工具本地模拟 GitHub Actions

```bash
# 安装 act (需要 Docker)
# macOS: brew install act
# Windows: choco install act-cli

# 模拟 PR 事件触发
act pull_request -j integration-test

# 模拟 Push 事件触发
act push -j full-test

# 查看可用 Jobs
act -l
```

### 3.3 验证环境变量

```bash
# 确认 test profile 配置生效
cd hrm
mvn test \
  -Dtest="com.qiujie.integration.LoginIntegrationTest" \
  -Dspring.profiles.active=test \
  2>&1 | grep -E "jdbc-url|datasource|profile"
```

---

## 4. CI 流水线详解

### 4.1 工作流执行顺序

```
代码 Push 到 GitHub
    │
    ▼
GitHub Actions 读取 .github/workflows/ci.yml
    │
    ├─── 触发条件检查 ─── 匹配 on: push/pull_request 规则
    │
    ▼
启动 ubuntu-latest 虚拟机
    │
    ├─── 启动 MySQL 8.0 Service Container
    │    ├── 设置 MYSQL_ROOT_PASSWORD=test123
    │    ├── 创建 hrm_test 数据库
    │    └── 健康检查: mysqladmin ping (最多重试 10 次)
    │
    ├─── Step 1: 检出代码 (actions/checkout@v4)
    ├─── Step 2: 安装 JDK 17 (actions/setup-java@v4)
    ├─── Step 3: 等待 MySQL 就绪 + 创建 hrm_activiti_test
    ├─── Step 4: 导入 init-test-db.sql
    ├─── Step 5: 缓存 ~/.m2/repository
    ├─── Step 6: mvn test (运行测试)
    ├─── Step 7: mvn jacoco:report (生成覆盖率)
    └─── Step 8: 上传 Surefire + JaCoCo 报告为 Artifacts
```

### 4.2 关键超时设置

| 配置项 | 值 | 说明 |
|--------|----|------|
| MySQL 健康检查间隔 | 10s | 每次 ping 之间等待时间 |
| MySQL 最大重试次数 | 10 次 | 总共最多等 10×10=100 秒 |
| 集成测试超时 | 20 min | `timeout-minutes: 20` |
| 全量测试超时 | 25 min | 含 687 个用例，预留宽裕时间 |
| Artifact 保留期 | 30 天 | 测试报告自动清理时间 |

### 4.3 报告下载

每次 CI 运行后会生成两个 Artifact：

```
integration-test-reports/
├── surefire-reports/        ← 测试结果 XML/TXT
└── jacoco/                  ← JaCoCo HTML 覆盖率报告
    └── index.html           ← 用浏览器打开查看覆盖率

full-test-reports/
├── surefire-reports/        ← 全量测试结果
└── jacoco/                  ← 全量覆盖率
```

在 Actions 页面 → 选择某次运行 → 底部 **Artifacts** 区域下载。

---

## 5. 常见问题排查

### 5.1 MySQL 连接失败

**现象：** CI 日志显示 `Communications link failure` 或 `Connection refused`

**排查步骤：**
```bash
# 1. 检查 MySQL Service 是否正常启动
# 在 CI 日志中搜索: "MySQL 已就绪"

# 2. 验证健康检查是否通过
# 如果看到 "Waiting for MySQL..." 重复 30 次后超时
# → MySQL 容器可能内存不足或启动太慢

# 3. 增加启动等待时间
# 在 ci.yml 中将 for i in $(seq 1 30) 改为 $(seq 1 60)
```

### 5.2 表不存在错误

**现象：** `Table 'hrm_test.sys_staff' doesn't exist`

**排查步骤：**
```bash
# 1. 确认 init-test-db.sql 包含了所有必要的表
grep "CREATE TABLE" hrm/src/test/resources/init-test-db.sql | wc -l
# 应该有 16-18 张表

# 2. 检查 CI 日志中 "测试数据库初始化完成" 那一步是否执行成功

# 3. 如果缺少表，从本地导出补充：
mysqldump -h 127.0.0.1 -u root -p --no-data hrm <缺失的表名> >> hrm/src/test/resources/init-test-db.sql
```

### 5.3 登录认证失败

**现象：** 集成测试中 `LoginIntegrationTest` 失败，返回 code=300 或 401

**原因：** BCrypt 密码哈希不匹配

**解决方案：**
```bash
# 使用 1.1 节的方法生成正确的 BCrypt 哈希
# 然后更新 init-test-db.sql
# 重新提交并推送
```

### 5.4 Activiti 表不存在

**现象：** 请假工作流测试报 `act_ru_*` 相关表不存在

**解决方案：** 这是预期行为，Activiti 引擎会自动创建表。确认 `application-test.yml` 中：
```yaml
spring:
  activiti:
    database-schema-update: true   # ← 必须是 true
    history-level: full
    db-history-used: true
```

### 5.5 测试时间过长超时

**现象：** Job 在 20 分钟后被 GitHub Actions 强制终止

**解决方案：**
```yaml
# 在 ci.yml 中增加超时时间
- name: 运行集成测试
  timeout-minutes: 30   # 从 20 改为 30
```

### 5.6 Maven 依赖下载慢

**解决方案 1：使用 Maven 仓库镜像**

在项目根目录创建 `.mvn/maven.config`：
```
-Dmaven.repo.remote=https://repo1.maven.org/maven2
```

或在 `pom.xml` 中添加阿里云镜像：
```xml
<repositories>
    <repository>
        <id>aliyun</id>
        <url>https://maven.aliyun.com/repository/public</url>
    </repository>
</repositories>
```

**解决方案 2：检查缓存是否生效**

在 CI 日志中搜索 `Cache hit`，确认 Maven 依赖缓存正常工作。

### 5.7 文件路径问题（Windows vs Linux）

**现象：** 文件上传测试在 CI 中失败

**原因：** CI 运行在 Linux 环境，文件路径与 Windows 不同

**解决：** `application-test.yml` 已配置：
```yaml
file-path: /tmp/hrm-test-files/   # Linux 临时目录
```

---

## 6. 高级定制

### 6.1 添加 Slack/钉钉/飞书通知

在 `ci.yml` 的 Job 末尾添加：

```yaml
- name: 发送失败通知
  if: failure()
  uses: rtCamp/action-slack-notify@v2
  env:
    SLACK_WEBHOOK: ${{ secrets.SLACK_WEBHOOK }}
    SLACK_TITLE: "❌ HRM 集成测试失败"
    SLACK_MESSAGE: "${{ github.repository }} @ ${{ github.ref_name }}"
```

### 6.2 并行运行测试（加速）

```yaml
# 在 mvn test 命令中添加并行参数
- name: 运行集成测试（并行）
  run: |
    cd hrm
    mvn test \
      -Dtest="com.qiujie.integration.*Test" \
      -Dspring.profiles.active=test \
      -DfailIfNoTests=false \
      -T 2C \        # 每个 CPU 核心 2 个线程
      --batch-mode
```

### 6.3 添加代码风格检查

```yaml
- name: Checkstyle 代码风格检查
  run: |
    cd hrm
    mvn checkstyle:check
```

### 6.4 仅在特定文件变更时触发

```yaml
on:
  push:
    branches: [master]
    paths:
      - 'hrm/src/**'          # 仅源码变更时触发
      - 'hrm/pom.xml'         # 依赖变更时触发
      - '.github/workflows/ci.yml'  # CI 配置变更时触发
```

### 6.5 添加状态徽章到 README

```markdown
<!-- 在 README.md 中添加 -->
[![CI](https://github.com/<用户名>/<仓库名>/actions/workflows/ci.yml/badge.svg)](https://github.com/<用户名>/<仓库名>/actions/workflows/ci.yml)
[![Coverage](https://img.shields.io/badge/coverage-51%25-yellow)](https://github.com/<用户名>/<仓库名>/actions)
```

---

## 附录 A：文件清单

| 文件 | 路径 | 大小 | 说明 |
|------|------|------|------|
| CI 工作流 | `.github/workflows/ci.yml` | ~4 KB | GitHub Actions 主配置 |
| 测试环境配置 | `hrm/src/test/resources/application-test.yml` | ~2 KB | CI 环境参数 |
| 数据库脚本 | `hrm/src/test/resources/init-test-db.sql` | ~10 KB | 18 张表 + 基础数据 |
| 本指南 | `.github/CI_SETUP_GUIDE.md` | — | 你正在读的文档 |

## 附录 B：快速检查清单

部署前请逐项确认：

- [ ] 已在本地生成正确的 BCrypt 哈希并更新 `init-test-db.sql`
- [ ] 已确认 `init-test-db.sql` 包含所有业务表的 DDL（对比本地数据库）
- [ ] 代码已推送到 GitHub 仓库的 UT 分支
- [ ] GitHub Actions 已启用（Settings → Actions → General）
- [ ] 本地验证: `mvn test -Dspring.profiles.active=test -Dtest="com.qiujie.integration.*Test"`
- [ ] `.gitignore` 未排除 `.github/workflows/` 目录
- [ ] CI 工作流首次运行通过

---

> 💡 如果 CI 一直无法通过，可以先在本地 `mvn test -Dspring.profiles.active=test` 确保 test profile 下所有测试通过，再推送到 GitHub。
