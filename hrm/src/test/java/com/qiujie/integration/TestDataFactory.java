package com.qiujie.integration;

import com.qiujie.entity.*;
import com.qiujie.enums.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * 集成测试数据工厂
 * 为集成测试提供标准化的测试数据对象
 *
 * @author qiujie
 * @since 2026-06-13
 */
public class TestDataFactory {

    // ==================== 员工 (Staff) ====================

    public static Staff createDefaultStaff(String name, String code, Integer deptId) {
        Staff staff = new Staff();
        staff.setName(name);
        staff.setCode(code);
        staff.setPassword("123");
        staff.setDeptId(deptId);
        staff.setPhone("13800138000");
        staff.setAddress("测试地址");
        staff.setGender(GenderEnum.MALE);
        staff.setStatus(1);
        return staff;
    }

    public static Staff createStaff(String name, String code, Integer deptId, String phone, GenderEnum gender) {
        Staff staff = new Staff();
        staff.setName(name);
        staff.setCode(code);
        staff.setPassword("123");
        staff.setDeptId(deptId);
        staff.setPhone(phone);
        staff.setAddress("北京市测试区");
        staff.setGender(gender);
        staff.setStatus(1);
        return staff;
    }

    // ==================== 部门 (Dept) ====================

    public static Dept createDefaultDept(String name, Integer parentId) {
        Dept dept = new Dept();
        dept.setName(name);
        dept.setParentId(parentId);
        // 数据库列类型为 TIME, 使用 Time.valueOf 而非 Timestamp.valueOf
        dept.setMorStartTime(new Timestamp(Time.valueOf("09:00:00").getTime()));
        dept.setMorEndTime(new Timestamp(Time.valueOf("12:00:00").getTime()));
        dept.setAftStartTime(new Timestamp(Time.valueOf("14:00:00").getTime()));
        dept.setAftEndTime(new Timestamp(Time.valueOf("18:00:00").getTime()));
        dept.setTotalWorkTime(new BigDecimal("8"));
        return dept;
    }

    // ==================== 考勤 (Attendance) ====================

    public static Attendance createNormalAttendance(Integer staffId) {
        Attendance att = new Attendance();
        att.setStaffId(staffId);
        att.setAttendanceDate(Date.valueOf("2026-06-01"));
        att.setMorStartTime(Timestamp.valueOf("2026-06-01 09:00:00"));
        att.setMorEndTime(Timestamp.valueOf("2026-06-01 12:00:00"));
        att.setAftStartTime(Timestamp.valueOf("2026-06-01 14:00:00"));
        att.setAftEndTime(Timestamp.valueOf("2026-06-01 18:00:00"));
        att.setStatus(AttendanceStatusEnum.NORMAL);
        return att;
    }

    public static Attendance createLateAttendance(Integer staffId) {
        Attendance att = new Attendance();
        att.setStaffId(staffId);
        att.setAttendanceDate(Date.valueOf("2026-06-01"));
        att.setMorStartTime(Timestamp.valueOf("2026-06-01 09:30:00")); // 迟到30分钟
        att.setMorEndTime(Timestamp.valueOf("2026-06-01 12:00:00"));
        att.setAftStartTime(Timestamp.valueOf("2026-06-01 14:00:00"));
        att.setAftEndTime(Timestamp.valueOf("2026-06-01 18:00:00"));
        att.setStatus(AttendanceStatusEnum.LATE);
        return att;
    }

    // ==================== 城市社保标准 (City) ====================

    public static City createDefaultCity(String name) {
        City city = new City();
        city.setName(name);
        city.setLowerSalary(new BigDecimal("2320"));
        city.setAverageSalary(new BigDecimal("11297"));
        city.setSocLowerLimit(new BigDecimal("5360"));
        city.setSocUpperLimit(new BigDecimal("28221"));
        city.setHouLowerLimit(new BigDecimal("2320"));
        city.setHouUpperLimit(new BigDecimal("28221"));
        city.setPerPensionRate(new BigDecimal("0.08"));
        city.setComPensionRate(new BigDecimal("0.16"));
        city.setPerMedicalRate(new BigDecimal("0.02"));
        city.setComMedicalRate(new BigDecimal("0.08"));
        city.setPerUnemploymentRate(new BigDecimal("0.005"));
        city.setComUnemploymentRate(new BigDecimal("0.005"));
        city.setComMaternityRate(new BigDecimal("0.008"));
        return city;
    }

    // ==================== 社保 (Insurance) ====================

    public static Insurance createDefaultInsurance(Integer staffId, Integer cityId) {
        Insurance ins = new Insurance();
        ins.setStaffId(staffId);
        ins.setCityId(cityId);
        ins.setSocialBase(new BigDecimal("15000"));
        ins.setHouseBase(new BigDecimal("15000"));
        ins.setPerHouseRate(new BigDecimal("0.05"));
        ins.setComHouseRate(new BigDecimal("0.05"));
        ins.setComInjuryRate(new BigDecimal("0.005"));
        return ins;
    }

    // ==================== 薪资 (Salary) ====================

    public static Salary createDefaultSalary(Integer staffId) {
        Salary salary = new Salary();
        salary.setStaffId(staffId);
        salary.setBaseSalary(new BigDecimal("10000"));
        salary.setSubsidy(new BigDecimal("1000"));
        salary.setBonus(new BigDecimal("2000"));
        return salary;
    }

    public static Salary createSalary(Integer staffId, int baseSalary, int subsidy, int bonus) {
        Salary salary = new Salary();
        salary.setStaffId(staffId);
        salary.setBaseSalary(new BigDecimal(baseSalary));
        salary.setSubsidy(new BigDecimal(subsidy));
        salary.setBonus(new BigDecimal(bonus));
        return salary;
    }

    // ==================== 请假 (StaffLeave) ====================

    public static StaffLeave createDefaultLeave(Integer staffId, LeaveEnum type, Integer days) {
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(staffId);
        leave.setTypeNum(type);
        leave.setDays(days);
        leave.setStartDate(Date.valueOf("2026-06-10"));
        leave.setStatus(AuditStatusEnum.UNAUDITED);
        return leave;
    }

    // ==================== 加班规则 (Overtime) ====================

    public static Overtime createDefaultOvertime(Integer deptId, OvertimeEnum typeNum) {
        Overtime overtime = new Overtime();
        overtime.setDeptId(deptId);
        overtime.setTypeNum(typeNum);
        overtime.setSalaryMultiple(new BigDecimal(typeNum.getLowerLimit()));
        overtime.setCountType(0); // 按小时
        overtime.setTimeOffFlag(0);
        return overtime;
    }

    // ==================== 员工加班 (StaffOvertime) ====================

    public static StaffOvertime createDefaultStaffOvertime(Integer staffId) {
        StaffOvertime so = new StaffOvertime();
        so.setStaffId(staffId);
        so.setOvertimeDate(Date.valueOf("2026-06-01"));
        so.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        so.setTotalOvertime(new BigDecimal("4.0"));
        so.setStatus(OvertimeStatusEnum.OVERTIME);
        return so;
    }

    // ==================== 扣款规则 (SalaryDeduct) ====================

    public static SalaryDeduct createDefaultDeduct(Integer deptId, DeductEnum typeNum, Integer amount) {
        SalaryDeduct deduct = new SalaryDeduct();
        deduct.setDeptId(deptId);
        deduct.setTypeNum(typeNum);
        deduct.setDeduct(amount);
        return deduct;
    }

    // ==================== 假期设置 (Leave) ====================

    public static Leave createDefaultLeaveConfig(Integer deptId, LeaveEnum typeNum, Integer days) {
        Leave leave = new Leave();
        leave.setDeptId(deptId);
        leave.setTypeNum(typeNum);
        leave.setDays(days);
        leave.setStatus(1);
        return leave;
    }

    // ==================== 菜单 (Menu) ====================

    public static Menu createMenu(String name, String code, Integer level, Integer parentId) {
        Menu menu = new Menu();
        menu.setName(name);
        menu.setCode(code);
        menu.setLevel(level);
        menu.setParentId(parentId);
        menu.setStatus(1);
        return menu;
    }

    public static Menu createPermissionMenu(String name, String code, String permission, Integer parentId) {
        Menu menu = new Menu();
        menu.setName(name);
        menu.setCode(code);
        menu.setLevel(2);
        menu.setParentId(parentId);
        menu.setPermission(permission);
        menu.setStatus(1);
        return menu;
    }

    // ==================== 角色 (Role) ====================

    public static Role createRole(String name, String code) {
        Role role = new Role();
        role.setName(name);
        role.setCode(code);
        return role;
    }
}
