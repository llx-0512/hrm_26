-- ===================================================
-- CI 集成测试数据库初始化脚本
-- 用于 GitHub Actions / Jenkins 等 CI 环境
-- ===================================================

-- ========== 部门表 (sys_dept) ==========
CREATE TABLE IF NOT EXISTS sys_dept (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) DEFAULT NULL COMMENT '部门编码',
    name VARCHAR(64) NOT NULL COMMENT '部门名称',
    mor_start_time TIME DEFAULT NULL COMMENT '上午上班时间',
    mor_end_time TIME DEFAULT NULL COMMENT '上午下班时间',
    aft_start_time TIME DEFAULT NULL COMMENT '下午上班时间',
    aft_end_time TIME DEFAULT NULL COMMENT '下午下班时间',
    total_work_time DECIMAL(10,2) DEFAULT NULL COMMENT '总工作时长',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    parent_id INT DEFAULT 0 COMMENT '父级部门id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 员工表 (sys_staff) ==========
CREATE TABLE IF NOT EXISTS sys_staff (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL COMMENT '员工编码/工号',
    name VARCHAR(64) NOT NULL COMMENT '员工姓名',
    gender INT DEFAULT 0 COMMENT '性别 0男 1女',
    address VARCHAR(255) DEFAULT NULL COMMENT '家庭住址',
    pwd VARCHAR(255) DEFAULT NULL COMMENT '密码',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像',
    birthday DATE DEFAULT NULL COMMENT '生日',
    phone VARCHAR(20) DEFAULT NULL COMMENT '电话',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    dept_id INT DEFAULT NULL COMMENT '部门id',
    status INT DEFAULT 1 COMMENT '状态 0离职 1在职 2禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 角色表 (per_role) ==========
CREATE TABLE IF NOT EXISTS per_role (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL COMMENT '角色编码',
    name VARCHAR(64) NOT NULL COMMENT '角色名称',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 菜单表 (per_menu) ==========
CREATE TABLE IF NOT EXISTS per_menu (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) DEFAULT NULL COMMENT '菜单编码',
    name VARCHAR(64) NOT NULL COMMENT '菜单名称',
    icon VARCHAR(64) DEFAULT NULL COMMENT '图标',
    permission VARCHAR(255) DEFAULT NULL COMMENT '权限标识',
    parent_id INT DEFAULT 0 COMMENT '父菜单id',
    level INT DEFAULT 0 COMMENT '0一级菜单 1二级菜单 2权限点',
    status INT DEFAULT 1 COMMENT '0禁用 1正常',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 角色菜单关联表 (per_role_menu) ==========
CREATE TABLE IF NOT EXISTS per_role_menu (
    id INT AUTO_INCREMENT PRIMARY KEY,
    role_id INT NOT NULL COMMENT '角色id',
    menu_id INT NOT NULL COMMENT '菜单id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 员工角色关联表 (per_staff_role) ==========
CREATE TABLE IF NOT EXISTS per_staff_role (
    id INT AUTO_INCREMENT PRIMARY KEY,
    staff_id INT NOT NULL COMMENT '员工id',
    role_id INT NOT NULL COMMENT '角色id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 文件管理表 (sys_docs) ==========
CREATE TABLE IF NOT EXISTS sys_docs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) DEFAULT NULL COMMENT '文件名称',
    type VARCHAR(64) DEFAULT NULL COMMENT '文件类型',
    old_name VARCHAR(255) DEFAULT NULL COMMENT '文件原名称',
    md5 VARCHAR(64) DEFAULT NULL COMMENT '文件md5',
    size BIGINT DEFAULT NULL COMMENT '文件大小kB',
    staff_id INT DEFAULT NULL COMMENT '上传者id',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 城市社保标准表 (soc_city) ==========
CREATE TABLE IF NOT EXISTS soc_city (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL COMMENT '参保城市',
    average_salary DECIMAL(10,2) DEFAULT NULL COMMENT '职工上年度平均月工资',
    lower_salary DECIMAL(10,2) DEFAULT NULL COMMENT '职工上年度最低月工资',
    soc_upper_limit DECIMAL(10,2) DEFAULT NULL COMMENT '社保缴纳基数上限',
    soc_lower_limit DECIMAL(10,2) DEFAULT NULL COMMENT '社保缴纳基数下限',
    hou_upper_limit DECIMAL(10,2) DEFAULT NULL COMMENT '公积金缴纳基数上限',
    hou_lower_limit DECIMAL(10,2) DEFAULT NULL COMMENT '公积金缴纳基数下限',
    per_pension_rate DECIMAL(5,4) DEFAULT NULL COMMENT '养老保险个人缴费比例',
    com_pension_rate DECIMAL(5,4) DEFAULT NULL COMMENT '养老保险企业缴费比例',
    per_medical_rate DECIMAL(5,4) DEFAULT NULL COMMENT '医疗保险个人缴费比例',
    com_medical_rate DECIMAL(5,4) DEFAULT NULL COMMENT '医疗保险企业缴费比例',
    per_unemployment_rate DECIMAL(5,4) DEFAULT NULL COMMENT '失业保险个人缴费比例',
    com_unemployment_rate DECIMAL(5,4) DEFAULT NULL COMMENT '失业保险企业缴费比例',
    com_maternity_rate DECIMAL(5,4) DEFAULT NULL COMMENT '生育保险企业缴费比例',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 员工社保表 (soc_insurance) ==========
CREATE TABLE IF NOT EXISTS soc_insurance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    city_id INT NOT NULL COMMENT '城市id',
    staff_id INT NOT NULL COMMENT '员工id',
    house_base DECIMAL(10,2) DEFAULT NULL COMMENT '公积金基数',
    per_house_rate DECIMAL(5,4) DEFAULT NULL COMMENT '公积金个人缴纳比例',
    per_house_pay DECIMAL(10,2) DEFAULT NULL COMMENT '公积金个人缴纳费用',
    com_house_rate DECIMAL(5,4) DEFAULT NULL COMMENT '公积金企业缴纳比例',
    com_house_pay DECIMAL(10,2) DEFAULT NULL COMMENT '公积金企业缴纳费用',
    house_remark VARCHAR(255) DEFAULT NULL COMMENT '公积金备注',
    social_base DECIMAL(10,2) DEFAULT NULL COMMENT '社保基数',
    com_social_pay DECIMAL(10,2) DEFAULT NULL COMMENT '社保企业缴纳费用',
    per_social_pay DECIMAL(10,2) DEFAULT NULL COMMENT '社保个人缴纳费用',
    com_injury_rate DECIMAL(5,4) DEFAULT NULL COMMENT '工伤保险企业缴纳比例',
    social_remark VARCHAR(255) DEFAULT NULL COMMENT '社保备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 薪资表 (sal_salary) ==========
CREATE TABLE IF NOT EXISTS sal_salary (
    id INT AUTO_INCREMENT PRIMARY KEY,
    staff_id INT NOT NULL COMMENT '员工id',
    base_salary DECIMAL(10,2) DEFAULT NULL COMMENT '基础工资',
    day_salary DECIMAL(10,2) DEFAULT NULL COMMENT '平均日薪',
    hour_salary DECIMAL(10,2) DEFAULT NULL COMMENT '平均时薪',
    overtime_salary DECIMAL(10,2) DEFAULT NULL COMMENT '加班费',
    subsidy DECIMAL(10,2) DEFAULT NULL COMMENT '生活补贴',
    bonus DECIMAL(10,2) DEFAULT NULL COMMENT '奖金',
    month VARCHAR(7) DEFAULT NULL COMMENT '月份',
    late_deduct DECIMAL(10,2) DEFAULT NULL COMMENT '迟到扣款',
    leave_deduct DECIMAL(10,2) DEFAULT NULL COMMENT '休假扣款',
    leave_early_deduct DECIMAL(10,2) DEFAULT NULL COMMENT '早退扣款',
    absenteeism_deduct DECIMAL(10,2) DEFAULT NULL COMMENT '旷工扣款',
    total_salary DECIMAL(10,2) DEFAULT NULL COMMENT '总工资',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 薪资扣款表 (sal_salary_deduct) ==========
CREATE TABLE IF NOT EXISTS sal_salary_deduct (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dept_id INT NOT NULL COMMENT '部门id',
    type_num INT DEFAULT NULL COMMENT '扣款类型 0迟到 1早退 2旷工 3休假',
    deduct INT DEFAULT NULL COMMENT '每次扣款金额',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 考勤表 (att_attendance) ==========
CREATE TABLE IF NOT EXISTS att_attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    staff_id INT NOT NULL COMMENT '员工id',
    mor_start_time TIME DEFAULT NULL COMMENT '上午上班时间',
    mor_end_time TIME DEFAULT NULL COMMENT '上午下班时间',
    aft_start_time TIME DEFAULT NULL COMMENT '下午上班时间',
    aft_end_time TIME DEFAULT NULL COMMENT '下午下班时间',
    attendance_date DATE NOT NULL COMMENT '考勤日期',
    status INT DEFAULT 0 COMMENT '0正常 1迟到 2早退 3旷工 4休假',
    remark VARCHAR(255) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 员工请假表 (att_staff_leave) ==========
CREATE TABLE IF NOT EXISTS att_staff_leave (
    id INT AUTO_INCREMENT PRIMARY KEY,
    staff_id INT NOT NULL COMMENT '员工id',
    days INT DEFAULT NULL COMMENT '请假天数',
    type_num INT DEFAULT NULL COMMENT '请假类型',
    start_date DATE DEFAULT NULL COMMENT '起始日期',
    status INT DEFAULT 0 COMMENT '0待审核 1审核通过 2驳回 3撤销 4审核中',
    audit_remark VARCHAR(255) DEFAULT NULL COMMENT '审批意见',
    remark VARCHAR(255) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 员工加班表 (att_staff_overtime) ==========
CREATE TABLE IF NOT EXISTS att_staff_overtime (
    id INT AUTO_INCREMENT PRIMARY KEY,
    staff_id INT NOT NULL COMMENT '员工id',
    mor_start_time TIME DEFAULT NULL,
    mor_end_time TIME DEFAULT NULL,
    aft_start_time TIME DEFAULT NULL,
    aft_end_time TIME DEFAULT NULL,
    overtime_date DATE DEFAULT NULL COMMENT '加班日期',
    total_overtime DECIMAL(10,2) DEFAULT NULL COMMENT '加班时长',
    overtime_salary DECIMAL(10,2) DEFAULT NULL COMMENT '加班工资',
    type_num INT DEFAULT NULL COMMENT '加班类型',
    status INT DEFAULT 0 COMMENT '0正常 1加班 2调休',
    remark VARCHAR(255) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 加班规则表 (att_overtime) ==========
CREATE TABLE IF NOT EXISTS att_overtime (
    id INT AUTO_INCREMENT PRIMARY KEY,
    salary_multiple DECIMAL(3,1) DEFAULT NULL COMMENT '工资倍数',
    bonus DECIMAL(10,2) DEFAULT NULL COMMENT '加班奖金',
    type_num INT DEFAULT NULL COMMENT '加班类型',
    dept_id INT NOT NULL COMMENT '部门id',
    count_type INT DEFAULT 0 COMMENT '0小时 1天',
    remark VARCHAR(255) DEFAULT NULL,
    is_time_off INT DEFAULT 0 COMMENT '0不调休 1调休',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 请假规则表 (att_leave) ==========
CREATE TABLE IF NOT EXISTS att_leave (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type_num INT DEFAULT NULL COMMENT '休假类型',
    dept_id INT NOT NULL COMMENT '部门id',
    days INT DEFAULT NULL COMMENT '休假天数',
    status INT DEFAULT 1 COMMENT '0禁用 1正常',
    remark VARCHAR(255) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 验证码表 (sys_validate_code) ==========
CREATE TABLE IF NOT EXISTS sys_validate_code (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) DEFAULT NULL COMMENT '验证码',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 基础测试数据 ==========

-- 测试部门
INSERT IGNORE INTO sys_dept (id, name, parent_id, mor_start_time, mor_end_time, aft_start_time, aft_end_time) VALUES
(1, '技术部', 0, '09:00:00', '12:00:00', '14:00:00', '18:00:00'),
(2, '产品部', 0, '09:00:00', '12:00:00', '14:00:00', '18:00:00'),
(3, '人事部', 0, '09:00:00', '12:00:00', '14:00:00', '18:00:00');

-- 测试员工 (密码 "123" 的 BCrypt 编码)
-- 注意：以下BCrypt哈希需要替换为实际值
-- 可通过 BCryptPasswordEncoder().encode("123") 生成
INSERT IGNORE INTO sys_staff (id, name, code, pwd, dept_id, phone, address, gender, status) VALUES
(1, '管理员', 'admin', '$2a$10$9eeFH/tStxGnPOcBzu8kM.ox6MU8LhjX6YYkPwhhIrsXRF657UKkG', 1, '13800000001', '北京市', 0, 1),
(2, '张三', 'zhangsan', '$2a$10$9eeFH/tStxGnPOcBzu8kM.ox6MU8LhjX6YYkPwhhIrsXRF657UKkG', 1, '13800000002', '北京市', 0, 1),
(3, '李四', 'lisi', '$2a$10$9eeFH/tStxGnPOcBzu8kM.ox6MU8LhjX6YYkPwhhIrsXRF657UKkG', 3, '13800000003', '北京市', 1, 1);

-- 测试角色
INSERT IGNORE INTO per_role (id, name, code, remark) VALUES
(1, '超级管理员', 'super_admin', '拥有全部权限'),
(2, '部门管理员', 'dept_admin', '管理本部门'),
(3, '普通用户', 'normal_user', '查看权限');

-- 城市社保标准
INSERT IGNORE INTO soc_city (id, name, lower_salary, average_salary, soc_upper_limit, soc_lower_limit, hou_upper_limit, hou_lower_limit,
    per_pension_rate, com_pension_rate, per_medical_rate, com_medical_rate,
    per_unemployment_rate, com_unemployment_rate, com_maternity_rate)
VALUES
(1, '北京市', 2320.00, 11297.00, 28221.00, 5360.00, 28221.00, 2320.00,
 0.08, 0.16, 0.02, 0.08, 0.005, 0.005, 0.008),
(2, '上海市', 2590.00, 12183.00, 36549.00, 7310.00, 36549.00, 2590.00,
 0.08, 0.16, 0.02, 0.10, 0.005, 0.005, 0.008);

-- 扣款规则
INSERT IGNORE INTO sal_salary_deduct (id, dept_id, type_num, deduct, remark) VALUES
(1, 1, 0, 50, '迟到扣款50元/次'),
(2, 1, 1, 50, '早退扣款50元/次'),
(3, 1, 2, 200, '旷工扣款200元/天'),
(4, 1, 3, 100, '请假扣款100元/天');
