package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Staff;
import com.qiujie.entity.StaffLeave;
import com.qiujie.enums.AuditStatusEnum;
import com.qiujie.enums.LeaveEnum;
import com.qiujie.mapper.StaffLeaveMapper;
import com.qiujie.mapper.StaffMapper;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Date;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StaffLeaveService 白盒测试
 * 覆盖：状态机冲突检测、工作流静默失败、审批分支、数据一致性
 *
 * @see TE.md 第 9.4 节
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StaffLeaveService 白盒测试")
class StaffLeaveServiceWhiteBoxTest {

    @Mock
    private StaffLeaveMapper staffLeaveMapper;

    @Mock
    private StaffMapper staffMapper;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private TaskService taskService;

    private StaffLeaveService staffLeaveService;

    // ========== 复用工具 ==========

    /** 创建一个合法的请假申请对象 */
    private StaffLeave validLeave() {
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-06-15"));
        return leave;
    }

    @BeforeEach
    void setUp() {
        staffLeaveService = spy(new StaffLeaveService());
        ReflectionTestUtils.setField(staffLeaveService, "staffLeaveMapper", staffLeaveMapper);
        ReflectionTestUtils.setField(staffLeaveService, "staffMapper", staffMapper);
        ReflectionTestUtils.setField(staffLeaveService, "runtimeService", runtimeService);
        ReflectionTestUtils.setField(staffLeaveService, "taskService", taskService);
    }

    // ================================================================
    // TC-LEAVE-WB-001: apply() — REJECT 状态也算冲突
    // ================================================================

    /**
     * 设计文档说 "不能有多个待审核请假"，
     * 但代码中 REJECT 状态的请假也会阻挡新申请。
     * 本测试验证这是设计意图还是 bug。
     */
    @Test
    @DisplayName("TC-LEAVE-WB-001: apply() — 被驳回(REJECT)的请假也触发冲突")
    void testApply_RejectedLeaveIsConflict() {
        // Given: 存在一条 status=REJECT 的请假
        StaffLeave rejectedLeave = new StaffLeave();
        rejectedLeave.setId(100);
        rejectedLeave.setStaffId(1);
        rejectedLeave.setStatus(AuditStatusEnum.REJECT);

        when(staffLeaveMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(rejectedLeave));

        StaffLeave newLeave = validLeave();

        // When
        ResponseDTO rsp = staffLeaveService.apply(newLeave, "EMP001");

        // Then: 冲突，应返回错误
        assertEquals(300, rsp.getCode());
        assertNotNull(rsp.getMessage());
        assertTrue(rsp.getMessage().contains("待审核") ||
                   rsp.getMessage().contains("驳回") ||
                   rsp.getMessage().contains("审核中"));

        // 验证未执行 save（冲突即返回）
        verify(staffLeaveService, never()).save(any(StaffLeave.class));
        verify(runtimeService, never()).startProcessInstanceByKey(anyString(), anyString(), anyMap());
    }

    // ================================================================
    // TC-LEAVE-WB-002: apply() — 工作流失败被静默吞掉
    // ================================================================

    /**
     * 当工作流引擎不可用时，请假保存成功但工作流启动失败。
     * 当前代码 try-catch 静默吞掉异常，仍返回 success。
     * 本测试暴露：调用方无法得知工作流未启动。
     */
    @Test
    @DisplayName("TC-LEAVE-WB-002: apply() — 工作流启动失败，请假仍保存成功")
    void testApply_WorkflowFails_LeaveStillSaved() {
        // Given: 无冲突，save 成功
        when(staffLeaveMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        doReturn(true).when(staffLeaveService).save(any(StaffLeave.class));

        // 工作流启动时抛出异常
        when(runtimeService.startProcessInstanceByKey(eq("leave"), anyString(), anyMap()))
                .thenThrow(new RuntimeException("Activiti 引擎不可用"));

        StaffLeave newLeave = validLeave();

        // When
        ResponseDTO rsp = staffLeaveService.apply(newLeave, "EMP001");

        // Then: 请假保存成功 (code=200)，但工作流未启动
        assertEquals(200, rsp.getCode());
        assertEquals(newLeave.getId(), rsp.getData());

        // ⚠️ 关键断言：save 被执行了，但工作流异常被吞掉了
        verify(staffLeaveService, times(1)).save(any(StaffLeave.class));
        verify(runtimeService, times(1))
                .startProcessInstanceByKey(eq("leave"), anyString(), anyMap());
        // taskService 完全没被调用（因为 startProcessInstanceByKey 就抛异常了）
        verify(taskService, never()).createTaskQuery();
    }

    // ================================================================
    // TC-LEAVE-WB-003: complete() — 未知 taskDefinitionKey → map=null
    // ================================================================

    /**
     * 当 Activiti 任务节点的 key 不是 "hr_audit" 也不是 "manager_audit" 时，
     * map 被设为 null，导致 taskService.complete() 收到 null variables。
     * 本测试验证这个边界行为。
     */
    @Test
    @DisplayName("TC-LEAVE-WB-003: complete() — 未知 taskKey 发送 null variables")
    void testComplete_UnknownTaskKey_SendsNullVariables() {
        // Given: updateById 成功
        doReturn(true).when(staffLeaveService).updateById(any(StaffLeave.class));

        // Mock Activiti TaskQuery 链
        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processDefinitionKey(anyString())).thenReturn(taskQuery);
        when(taskQuery.processInstanceBusinessKey(anyString())).thenReturn(taskQuery);
        when(taskQuery.taskAssignee(anyString())).thenReturn(taskQuery);

        Task mockTask = mock(Task.class);
        when(mockTask.getId()).thenReturn("task-001");
        when(mockTask.getTaskDefinitionKey()).thenReturn("unknown_node"); // ← 关键
        when(taskQuery.singleResult()).thenReturn(mockTask);

        StaffLeave leave = new StaffLeave();
        leave.setId(1);
        leave.setStaffId(1);
        leave.setStatus(AuditStatusEnum.APPROVE);

        // When
        ResponseDTO rsp = staffLeaveService.complete(leave, "admin");

        // Then: 返回 success
        assertEquals(200, rsp.getCode());

        // ⚠️ 关键断言：taskService.complete() 收到 null 作为 variables
        verify(taskService, times(1))
                .complete(eq("task-001"), isNull(), isNull());
    }

    // ================================================================
    // TC-LEAVE-WB-004: complete() — hr_audit 分支
    // ================================================================

    /**
     * 验证 "hr_audit" 节点完成时，流程变量 hrAuditStatus 被正确设置。
     */
    @Test
    @DisplayName("TC-LEAVE-WB-004: complete() — hr_audit 设置 hrAuditStatus 变量")
    void testComplete_HrAudit_SetsCorrectVariable() {
        // Given
        doReturn(true).when(staffLeaveService).updateById(any(StaffLeave.class));

        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processDefinitionKey(anyString())).thenReturn(taskQuery);
        when(taskQuery.processInstanceBusinessKey(anyString())).thenReturn(taskQuery);
        when(taskQuery.taskAssignee(anyString())).thenReturn(taskQuery);

        Task mockTask = mock(Task.class);
        when(mockTask.getId()).thenReturn("task-hr-001");
        when(mockTask.getTaskDefinitionKey()).thenReturn("hr_audit"); // ← HR 审批节点
        when(taskQuery.singleResult()).thenReturn(mockTask);

        StaffLeave leave = new StaffLeave();
        leave.setId(2);
        leave.setStaffId(1);
        leave.setStatus(AuditStatusEnum.APPROVE); // status.getCode() = 1

        // When
        ResponseDTO rsp = staffLeaveService.complete(leave, "hr_user");

        // Then
        assertEquals(200, rsp.getCode());

        // 捕获传给 taskService.complete() 的 variables map
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(taskService, times(1))
                .complete(eq("task-hr-001"), isNull(), captor.capture());

        Map<String, Object> variables = captor.getValue();
        assertNotNull(variables, "hr_audit 节点的 variables 不应为 null");
        assertEquals(AuditStatusEnum.APPROVE.getCode(), variables.get("hrAuditStatus"),
                "hrAuditStatus 应为 APPROVE(1)");
        assertNull(variables.get("managerAuditStatus"),
                "hr_audit 节点不应设置 managerAuditStatus");
    }

    // ================================================================
    // TC-LEAVE-WB-005: complete() — manager_audit 分支
    // ================================================================

    /**
     * 验证 "manager_audit" 节点完成时，流程变量 managerAuditStatus 被正确设置。
     */
    @Test
    @DisplayName("TC-LEAVE-WB-005: complete() — manager_audit 设置 managerAuditStatus 变量")
    void testComplete_ManagerAudit_SetsCorrectVariable() {
        // Given
        doReturn(true).when(staffLeaveService).updateById(any(StaffLeave.class));

        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processDefinitionKey(anyString())).thenReturn(taskQuery);
        when(taskQuery.processInstanceBusinessKey(anyString())).thenReturn(taskQuery);
        when(taskQuery.taskAssignee(anyString())).thenReturn(taskQuery);

        Task mockTask = mock(Task.class);
        when(mockTask.getId()).thenReturn("task-mgr-001");
        when(mockTask.getTaskDefinitionKey()).thenReturn("manager_audit"); // ← 经理审批节点
        when(taskQuery.singleResult()).thenReturn(mockTask);

        StaffLeave leave = new StaffLeave();
        leave.setId(3);
        leave.setStaffId(1);
        leave.setStatus(AuditStatusEnum.REJECT); // status.getCode() = 2

        // When
        ResponseDTO rsp = staffLeaveService.complete(leave, "manager_user");

        // Then
        assertEquals(200, rsp.getCode());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(taskService, times(1))
                .complete(eq("task-mgr-001"), isNull(), captor.capture());

        Map<String, Object> variables = captor.getValue();
        assertNotNull(variables, "manager_audit 节点的 variables 不应为 null");
        assertEquals(AuditStatusEnum.REJECT.getCode(), variables.get("managerAuditStatus"),
                "managerAuditStatus 应为 REJECT(2)");
        assertNull(variables.get("hrAuditStatus"),
                "manager_audit 节点不应设置 hrAuditStatus");
    }

    // ================================================================
    // TC-LEAVE-WB-006: claim() — updateById 成功但 task 不存在
    // ================================================================

    /**
     * claim() 先执行 updateById 修改数据库，再查询 Activiti 任务。
     * 当 task 不存在时返回 ERROR，但数据库已被修改 —— 数据不一致！
     */
    @Test
    @DisplayName("TC-LEAVE-WB-006: claim() — task 不存在时数据库已修改（不一致风险）")
    void testClaim_TaskNotFound_DataInconsistency() {
        // Given: updateById 成功（数据库已改）
        doReturn(true).when(staffLeaveService).updateById(any(StaffLeave.class));

        // Mock: TaskQuery 返回 null（Activiti 任务不存在）
        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processDefinitionKey(anyString())).thenReturn(taskQuery);
        when(taskQuery.processInstanceBusinessKey(anyString())).thenReturn(taskQuery);
        when(taskQuery.taskCandidateUser(anyString())).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(null); // ← 任务不存在

        StaffLeave leave = new StaffLeave();
        leave.setId(1);
        leave.setStaffId(1);

        // When
        ResponseDTO rsp = staffLeaveService.claim(leave, "EMP001");

        // Then: 返回 ERROR
        assertEquals(300, rsp.getCode());

        // ⚠️ 关键断言：数据库已修改（updateById 先执行了），但 Activiti 未变更
        verify(staffLeaveService, times(1)).updateById(any(StaffLeave.class));
        verify(taskService, never()).claim(anyString(), anyString());
        // → 数据不一致：DB 中 leave 已更新，Activiti 中 task 仍未被认领
    }
}
