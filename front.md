# HRM 系统 — 前端可实现的完整功能清单

> 基于后端 16 个 Controller、100+ API 端点分析，前端 Vue2 + Element UI 项目 (`vue-elementui-hrm`) 已具备完整的 API 封装和视图目录结构。

---

## 一、登录与认证

| 功能 | 说明 | 后端 API |
|------|------|----------|
| 登录页 | 用户名（工号）+ 密码 + 图形验证码表单 | `POST /login/{validateCode}` |
| 图形验证码 | 点击刷新验证码图片 | `GET /validate/code` |
| JWT Token 管理 | 登录成功存储 Token，请求拦截器自动携带 | 响应 Header |
| 401 拦截 | Token 过期自动跳转登录页 | — |
| 动态路由 | 根据用户菜单权限动态注册路由 | `GET /menu/staff/{id}` |
| 按钮权限 | `v-permission` 指令控制按钮显隐 | `GET /menu/permission/{id}` |
| 退出登录 | 清除 Token + 重置路由 | — |

---

## 二、首页仪表盘

| 功能 | UI 组件 | 后端 API |
|------|---------|----------|
| 本年度入职统计 | 季度柱状图/折线图（Q1~Q4） | `GET /home/staff` |
| 数据概览卡片 | 总人数 / 在职人数 / 当日迟到 / 早退 / 旷工 | `GET /home/count` |
| 城市 Top5 | 列表卡片 | `GET /home/city` |
| 员工月考勤日历 | 日历视图，可切换月份，每天着色标记状态 | `GET /home/attendance?id=&month=` |
| 各部门人数分布 | 饼图 / 柱状图 | `GET /home/department` |

---

## 三、员工管理 (`/staff`)

| 功能 | 权限 | UI 组件 |
|------|------|---------|
| 员工列表 | `system:staff:list / search` | 分页表格 + 姓名/生日/部门/状态筛选栏 |
| 新增员工 | `system:staff:add` | 表单弹窗（姓名、性别、手机号、生日、部门、地址等） |
| 编辑员工 | `system:staff:edit` | 编辑表单弹窗 |
| 删除员工 | `system:staff:delete` | 确认弹窗 |
| 批量删除 | `system:staff:delete` | 勾选 + 确认弹窗 |
| 员工详情 | — | 详情页/弹窗（含部门名、年龄自动计算） |
| 员工个人资料 | — | 资料卡片 |
| 密码验证 | — | 输入原密码校验（修改密码前） |
| 重置密码 | — | 修改密码表单 |
| 分配角色 | `system:staff:set_role` | 多选穿梭框 / 多选下拉 |
| 查看角色 | — | 标签列表 |
| Excel 导出 | `system:staff:export` | 导出按钮 |
| Excel 导入 | `system:staff:import` | 文件选择器 + 上传按钮 |

---

## 四、部门管理 (`/dept`)

| 功能 | 权限 | UI 组件 |
|------|------|---------|
| 部门树形结构 | — | Tree 组件 / 级联选择器 |
| 部门列表 | `system:department:list / search` | 树形表格 + 名称搜索 |
| 新增根部门 | `system:department:add` | 表单（名称、时间非必填） |
| 新增子部门 | `system:department:add` | 表单（名称 + 4段时间必填，校验时间合理性） |
| 编辑部门 | `system:department:edit` | 编辑表单（含循环引用校验提示） |
| 删除部门 | `system:department:delete` | 确认弹窗（有子部门时提示不可删除） |
| 批量删除 | `system:department:delete` | 勾选确认 |
| Excel 导出 | `system:department:export` | 导出按钮 |
| Excel 导入 | `system:department:import` | 文件上传 |

---

## 五、考勤管理 (`/attendance`)

| 功能 | 权限 | UI 组件 |
|------|------|---------|
| 月考勤矩阵视图 | `performance:attendance:list / search` | 员工 × 日期表格，状态标签着色（正常=绿/迟到=橙/早退=黄/旷工=红/休假=蓝/调休=紫） |
| 新增考勤记录 | — | 表单（员工、日期、4次打卡时间） |
| 手动修正考勤 | `performance:attendance:set` | 修改打卡时间 / 直接改状态 |
| 按员工+日期查询 | — | 单条详情弹窗 |
| 月考勤报表导出 | `performance:attendance:export` | 导出按钮（含迟到/早退/旷工/调休/休假统计列） |
| 批量导入考勤 | `performance:attendance:import` | 文件上传 → 自动判定迟到/早退/旷工状态 |

---

## 六、请假管理

### 6.1 请假扣款规则配置 (`/leave`)

| 功能 | UI 组件 |
|------|---------|
| 请假类型下拉框 | 枚举下拉（事假/病假/年假/婚假/产假/陪产假/丧假） |
| 按部门配置扣款 | 表单（选择部门 + 类型 + 扣款金额） |
| 查看某部门规则列表 | 表格 |

### 6.2 请假申请与审批 (`/staff-leave`)

| 功能 | 权限 | UI 组件 |
|------|------|---------|
| 我的请假记录 | — | 分页表格 + 状态标签（待审核/审核中/已通过/已驳回/已撤销） |
| 提交请假申请 | — | 表单（类型、天数、开始日期、备注）→ 启动工作流 |
| 防重复提交 | — | 检测待审核/驳回/审核中的申请，提示不可重复提交 |
| 请假列表（HR/经理） | `performance:leave:list / search` | 分页表格 + 按姓名/部门/状态筛选 + 工号关联工作流任务 |
| 拾取审批任务 | `performance:leave:claim` | 按钮（从候选组领取任务） |
| 归还任务 | — | 归还到候选组 |
| 审批通过/驳回 | — | 审核表单（状态选择 + 审核意见） |
| 撤销申请 | — | 撤销按钮 → 删除流程实例 |
| Excel 导出 | `performance:leave:export` | 导出按钮 |
| Excel 导入 | `performance:leave:import` | 文件上传 |

---

## 七、加班管理

### 7.1 加班费率配置 (`/overtime`)

| 功能 | UI 组件 |
|------|---------|
| 加班类型下拉框 | 枚举下拉（工作日加班 1.5倍/休息日加班 2倍/节假日加班 3倍） |
| 按部门配置费率 | 表单（工资倍数、奖金、计费方式「按小时/按天」、是否调休） |

### 7.2 加班记录 (`/staff-overtime`)

| 功能 | 权限 | UI 组件 |
|------|------|---------|
| 加班月视图 | `performance:overtime:list / search` | 员工 × 日期表格，状态标签着色 |
| 新增加班 | — | 表单（员工、日期、4段时间 → 自动计算总时长和加班费） |
| 设置加班 | `performance:overtime:set` | 自动判定加班类型（工作日/休息日/节假日）+ 计算加班工资 |
| 查看调休余额 | — | 数字（剩余可以调休的天数） |
| 月加班报表导出 | `performance:overtime:export` | 导出按钮（含加班次数/调休次数统计） |
| 批量导入 | `performance:overtime:import` | 文件上传 → 自动判定类型并计算费用 |

---

## 八、薪资管理

### 8.1 薪资管理 (`/salary`)

| 功能 | 权限 | UI 组件 |
|------|------|---------|
| 薪资列表 | `money:salary:list / search` | 分页表格 + 姓名/部门/月份筛选，展示完整工资明细列 |
| 设置员工薪资 | `money:salary:set` | 表单（基本工资、补贴、奖金）→ 自动计算日薪(÷21.75)和时薪(÷174) |
| 工资明细查看 | — | 详情弹窗，展示：基本工资 + 补贴 + 奖金 + 加班费 - 迟到扣款 - 早退扣款 - 旷工扣款 - 休假扣款 - 社保 - 公积金 = 实发 |
| 工资条导出 | `money:salary:export` | 按月导出 Excel 按钮 |
| 批量导入 | `money:salary:import` | 文件上传 |

### 8.2 扣款规则配置 (`/salary-deduct`)

| 功能 | UI 组件 |
|------|---------|
| 扣款类型下拉框 | 枚举下拉（迟到扣款/早退扣款/旷工扣款/休假扣款 + 默认值） |
| 按部门配置扣款金额 | 表单（选择部门 + 类型 + 金额） |
| 查看某部门规则 | 表格 |

---

## 九、社保公积金 (`/insurance`)

| 功能 | 权限 | UI 组件 |
|------|------|---------|
| 社保列表 | `money:insurance:list / search` | 分页表格 + 姓名/部门筛选，展示基数、各项比例、个人/企业缴纳额 |
| 为员工设置社保 | `money:insurance:set` | 表单（选择城市、社保基数、公积金基数、个人/企业公积金比例、工伤保险比例）→ 自动计算缴纳金额 |
| 查看员工社保详情 | — | 详情弹窗 |
| Excel 导出 | `money:insurance:export` | 导出按钮 |
| Excel 导入 | `money:insurance:import` | 文件上传 |

---

## 十、城市社保标准 (`/city`)

| 功能 | 权限 | UI 组件 |
|------|------|---------|
| 城市列表 | `money:city:list / search` | 分页表格 + 名称搜索 |
| 新增城市标准 | `money:city:add` | 表单（平均工资、最低工资、社保上下限、公积金上下限、养老/医疗/失业/工伤/生育各项个人与企业比例） |
| 编辑城市标准 | `money:city:edit` | 编辑表单 |
| 删除 | `money:city:delete` | 确认弹窗 |
| 城市下拉框 | — | Select 选择器（用于社保配置中关联城市） |
| Excel 导出 | `money:city:export` | 导出按钮 |
| Excel 导入 | `money:city:import` | 文件上传 |

---

## 十一、角色与权限管理

### 11.1 角色管理 (`/role`)

| 功能 | 权限 | UI 组件 |
|------|------|---------|
| 角色列表 | `permission:role:list / search` | 分页表格 + 名称搜索 |
| 新增角色 | `permission:role:add` | 表单弹窗 |
| 编辑角色 | `permission:role:edit` | 编辑弹窗 |
| 删除角色 | `permission:role:delete` | 确认弹窗 |
| 为角色分配菜单 | `permission:role:set_menu` | 树形多选组件（三级菜单结构） |
| 查看角色菜单 | — | 树形展示 |
| 角色下拉框 | — | Select 选择器（用于员工分配角色） |
| Excel 导出 | `permission:role:export` | 导出按钮 |
| Excel 导入 | `permission:role:import` | 文件上传 |

### 11.2 菜单管理 (`/menu`)

| 功能 | 权限 | UI 组件 |
|------|------|---------|
| 菜单树 | `permission:menu:list / search` | 树形表格（一级菜单 → 二级页面 → 三级按钮/权限点） |
| 新增菜单/按钮 | `permission:menu:add` | 表单（名称、编码、图标、层级、父级、权限标识、路径） |
| 编辑菜单 | `permission:menu:edit` | 编辑表单 |
| 删除菜单 | `permission:menu:delete` | 确认弹窗 |
| 用户菜单渲染 | — | 根据后端返回的菜单树动态渲染左侧侧边栏导航 |
| 用户权限点 | — | 存储到 Vuex，通过 `v-permission` 指令控制页面内按钮显隐 |
| Excel 导出 | `permission:menu:export` | 导出按钮 |
| Excel 导入 | `permission:menu:import` | 文件上传 |

---

## 十二、文档管理 (`/docs`)

| 功能 | 权限 | UI 组件 |
|------|------|---------|
| 文档列表 | `system:docs:list / search` | 分页表格 + 原文件名/员工名搜索 |
| 文件上传 | `system:docs:upload` | 上传组件（限制格式：jpg/pdf/doc/zip 等 20+ 种，≤20MB） |
| 文件下载 | `system:docs:download` | 下载按钮（路径穿越防护） |
| 头像/公开下载 | — | 无需鉴权的图片展示 |
| 新增文档元数据 | — | 表单 |
| 编辑 | `system:docs:edit` | 编辑弹窗 |
| 删除 | `system:docs:delete` | 确认弹窗 |
| Excel 导出 | `system:docs:export` | 导出按钮 |
| Excel 导入 | `system:docs:import` | 文件上传 |

---

## 十三、全局通用功能

| 功能 | 说明 |
|------|------|
| 侧边栏导航 | 根据用户权限动态渲染多级菜单，支持图标 + 折叠 |
| 面包屑导航 | 根据当前路由自动生成 |
| 标签页 (Tabs) | 已打开页面的标签切换 |
| 全屏切换 | 浏览器全屏按钮 |
| 个人中心 | 头像、修改密码、个人信息查看 |
| 404 页面 | 路由不匹配时展示 |
| 500/403 页面 | 服务器错误 / 无权访问提示 |
| 请求 Loading | API 请求时全局加载动画 |
| 响应拦截 | 统一处理错误码 + Toast 提示 |

---

## 十四、权限标识汇总

| 权限域 | 权限标识 |
|--------|----------|
| 员工 | `system:staff:add` `delete` `edit` `list` `search` `export` `import` `set_role` `enable` |
| 部门 | `system:department:add` `delete` `edit` `list` `search` `export` `import` `setting` |
| 文档 | `system:docs:upload` `download` `add` `delete` `edit` `list` `search` `export` `import` |
| 考勤 | `performance:attendance:list` `search` `export` `import` `set` |
| 请假 | `performance:leave:add` `delete` `edit` `list` `search` `export` `import` `claim` |
| 加班 | `performance:overtime:add` `delete` `edit` `list` `search` `export` `import` `set` |
| 薪资 | `money:salary:list` `search` `export` `import` `set` |
| 社保 | `money:insurance:list` `search` `export` `import` `set` |
| 城市 | `money:city:add` `delete` `edit` `list` `search` `export` `import` |
| 角色 | `permission:role:add` `delete` `edit` `list` `search` `export` `import` `set_menu` |
| 菜单 | `permission:menu:add` `delete` `edit` `list` `search` `export` `import` `enable` |
