package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Leave;
import com.qiujie.enums.LeaveEnum;
import com.qiujie.mapper.LeaveMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 请假配置 Service 层单元测试
 * 覆盖：add/delete/deleteBatch/edit/query/queryByDeptIdAndTypeNum/setLeave/queryByDeptId/queryAll
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("请假配置Service层单元测试")
class LeaveServiceTest {

    @Mock
    private LeaveMapper leaveMapper;

    private LeaveService leaveService;

    @BeforeEach
    void setUp() {
        leaveService = spy(new LeaveService());
        ReflectionTestUtils.setField(leaveService, "baseMapper", leaveMapper);
        ReflectionTestUtils.setField(leaveService, "leaveMapper", leaveMapper);
    }

    // ==================== add ====================

    @Test
    @DisplayName("add — 保存成功")
    void testAdd_Success() {
        doReturn(true).when(leaveService).save(any(Leave.class));
        ResponseDTO rsp = leaveService.add(new Leave());
        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("add — 保存失败")
    void testAdd_Failure() {
        doReturn(false).when(leaveService).save(any(Leave.class));
        ResponseDTO rsp = leaveService.add(new Leave());
        assertEquals(300, rsp.getCode());
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete — 删除成功")
    void testDelete_Success() {
        doReturn(true).when(leaveService).removeById(1);
        ResponseDTO rsp = leaveService.delete(1);
        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("delete — 删除失败")
    void testDelete_Failure() {
        doReturn(false).when(leaveService).removeById(999);
        ResponseDTO rsp = leaveService.delete(999);
        assertEquals(300, rsp.getCode());
    }

    // ==================== deleteBatch ====================

    @Test
    @DisplayName("deleteBatch — 批量删除成功")
    void testDeleteBatch_Success() {
        doReturn(true).when(leaveService).removeBatchByIds(anyList());
        ResponseDTO rsp = leaveService.deleteBatch(Arrays.asList(1, 2));
        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("deleteBatch — 批量删除失败")
    void testDeleteBatch_Failure() {
        doReturn(false).when(leaveService).removeBatchByIds(anyList());
        ResponseDTO rsp = leaveService.deleteBatch(Arrays.asList(1, 2));
        assertEquals(300, rsp.getCode());
    }

    // ==================== edit ====================

    @Test
    @DisplayName("edit — 更新成功")
    void testEdit_Success() {
        doReturn(true).when(leaveService).updateById(any(Leave.class));
        Leave leave = new Leave();
        leave.setId(1);
        ResponseDTO rsp = leaveService.edit(leave);
        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("edit — 更新失败")
    void testEdit_Failure() {
        doReturn(false).when(leaveService).updateById(any(Leave.class));
        ResponseDTO rsp = leaveService.edit(new Leave());
        assertEquals(300, rsp.getCode());
    }

    // ==================== query ====================

    @Test
    @DisplayName("query — 查询成功")
    void testQuery_Success() {
        Leave leave = new Leave();
        leave.setId(1);
        doReturn(leave).when(leaveService).getById(1);
        ResponseDTO rsp = leaveService.query(1);
        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("query — ID不存在")
    void testQuery_NotFound() {
        doReturn(null).when(leaveService).getById(999);
        ResponseDTO rsp = leaveService.query(999);
        assertEquals(300, rsp.getCode());
    }

    // ==================== queryByDeptIdAndTypeNum ====================

    @Test
    @DisplayName("queryByDeptIdAndTypeNum — 找到记录")
    void testQueryByDeptIdAndTypeNum_Success() {
        Leave leave = new Leave();
        doReturn(leave).when(leaveService).getOne(any(QueryWrapper.class));
        ResponseDTO rsp = leaveService.queryByDeptIdAndTypeNum(10, 0);
        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("queryByDeptIdAndTypeNum — 未找到")
    void testQueryByDeptIdAndTypeNum_NotFound() {
        doReturn(null).when(leaveService).getOne(any(QueryWrapper.class));
        ResponseDTO rsp = leaveService.queryByDeptIdAndTypeNum(10, 99);
        assertEquals(300, rsp.getCode());
    }

    // ==================== setLeave ====================

    @Test
    @DisplayName("setLeave — saveOrUpdate成功")
    void testSetLeave_Success() {
        doReturn(true).when(leaveService).saveOrUpdate(any(Leave.class), any(QueryWrapper.class));
        Leave leave = new Leave();
        leave.setDeptId(10);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        ResponseDTO rsp = leaveService.setLeave(leave);
        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("setLeave — saveOrUpdate失败")
    void testSetLeave_Failure() {
        doReturn(false).when(leaveService).saveOrUpdate(any(Leave.class), any(QueryWrapper.class));
        Leave leave = new Leave();
        leave.setDeptId(10);
        ResponseDTO rsp = leaveService.setLeave(leave);
        assertEquals(300, rsp.getCode());
    }

    // ==================== queryByDeptId ====================

    @Test
    @DisplayName("queryByDeptId — 返回请假配置列表")
    void testQueryByDeptId_Success() {
        when(leaveMapper.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList(new Leave(), new Leave()));
        ResponseDTO rsp = leaveService.queryByDeptId(10);
        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        List<Leave> result = (List<Leave>) rsp.getData();
        assertEquals(2, result.size());
    }

    // ==================== queryAll ====================

    @Test
    @DisplayName("queryAll — 返回所有请假类型枚举")
    void testQueryAll_Success() {
        ResponseDTO rsp = leaveService.queryAll();
        assertEquals(200, rsp.getCode());
    }
}
