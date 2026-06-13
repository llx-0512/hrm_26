package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Overtime;
import com.qiujie.enums.OvertimeEnum;
import com.qiujie.mapper.OvertimeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 加班配置 Service 层单元测试
 * 覆盖：add/delete/deleteBatch/edit/query/queryByDeptIdAndTypeNum/setOvertime/queryAll
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("加班配置Service层单元测试")
class OvertimeServiceTest {

    @Mock
    private OvertimeMapper overtimeMapper;

    private OvertimeService overtimeService;

    @BeforeEach
    void setUp() {
        overtimeService = spy(new OvertimeService());
        ReflectionTestUtils.setField(overtimeService, "baseMapper", overtimeMapper);
    }

    // ==================== add ====================

    @Test
    @DisplayName("add — 保存成功")
    void testAdd_Success() {
        doReturn(true).when(overtimeService).save(any(Overtime.class));
        ResponseDTO rsp = overtimeService.add(new Overtime());
        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("add — 保存失败")
    void testAdd_Failure() {
        doReturn(false).when(overtimeService).save(any(Overtime.class));
        ResponseDTO rsp = overtimeService.add(new Overtime());
        assertEquals(300, rsp.getCode());
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete — 删除成功")
    void testDelete_Success() {
        doReturn(true).when(overtimeService).removeById(1);
        ResponseDTO rsp = overtimeService.delete(1);
        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("delete — 删除失败")
    void testDelete_Failure() {
        doReturn(false).when(overtimeService).removeById(999);
        ResponseDTO rsp = overtimeService.delete(999);
        assertEquals(300, rsp.getCode());
    }

    // ==================== deleteBatch ====================

    @Test
    @DisplayName("deleteBatch — 批量删除成功")
    void testDeleteBatch_Success() {
        doReturn(true).when(overtimeService).removeBatchByIds(anyList());
        ResponseDTO rsp = overtimeService.deleteBatch(Arrays.asList(1, 2));
        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("deleteBatch — 批量删除失败")
    void testDeleteBatch_Failure() {
        doReturn(false).when(overtimeService).removeBatchByIds(anyList());
        ResponseDTO rsp = overtimeService.deleteBatch(Arrays.asList(1, 2));
        assertEquals(300, rsp.getCode());
    }

    // ==================== edit ====================

    @Test
    @DisplayName("edit — 更新成功")
    void testEdit_Success() {
        doReturn(true).when(overtimeService).updateById(any(Overtime.class));
        Overtime overtime = new Overtime();
        overtime.setId(1);
        ResponseDTO rsp = overtimeService.edit(overtime);
        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("edit — 更新失败")
    void testEdit_Failure() {
        doReturn(false).when(overtimeService).updateById(any(Overtime.class));
        ResponseDTO rsp = overtimeService.edit(new Overtime());
        assertEquals(300, rsp.getCode());
    }

    // ==================== query ====================

    @Test
    @DisplayName("query — 查询成功")
    void testQuery_Success() {
        Overtime overtime = new Overtime();
        overtime.setId(1);
        doReturn(overtime).when(overtimeService).getById(1);
        ResponseDTO rsp = overtimeService.query(1);
        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("query — ID不存在")
    void testQuery_NotFound() {
        doReturn(null).when(overtimeService).getById(999);
        ResponseDTO rsp = overtimeService.query(999);
        assertEquals(300, rsp.getCode());
    }

    // ==================== queryByDeptIdAndTypeNum ====================

    @Test
    @DisplayName("queryByDeptIdAndTypeNum — 找到配置")
    void testQueryByDeptIdAndTypeNum_Success() {
        Overtime overtime = new Overtime();
        doReturn(overtime).when(overtimeService).getOne(any(QueryWrapper.class));
        ResponseDTO rsp = overtimeService.queryByDeptIdAndTypeNum(10, "WORKDAY_OVERTIME");
        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("queryByDeptIdAndTypeNum — 未找到")
    void testQueryByDeptIdAndTypeNum_NotFound() {
        doReturn(null).when(overtimeService).getOne(any(QueryWrapper.class));
        ResponseDTO rsp = overtimeService.queryByDeptIdAndTypeNum(10, "INVALID");
        assertEquals(300, rsp.getCode());
    }

    // ==================== setOvertime ====================

    @Test
    @DisplayName("setOvertime — saveOrUpdate成功")
    void testSetOvertime_Success() {
        doReturn(true).when(overtimeService).saveOrUpdate(any(Overtime.class), any(QueryWrapper.class));
        Overtime overtime = new Overtime();
        overtime.setDeptId(10);
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        ResponseDTO rsp = overtimeService.setOvertime(overtime);
        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("setOvertime — saveOrUpdate失败")
    void testSetOvertime_Failure() {
        doReturn(false).when(overtimeService).saveOrUpdate(any(Overtime.class), any(QueryWrapper.class));
        Overtime overtime = new Overtime();
        overtime.setDeptId(10);
        ResponseDTO rsp = overtimeService.setOvertime(overtime);
        assertEquals(300, rsp.getCode());
    }

    // ==================== queryAll ====================

    @Test
    @DisplayName("queryAll — 返回加班类型枚举并注入lowerLimit")
    void testQueryAll_Success() {
        ResponseDTO rsp = overtimeService.queryAll();

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) rsp.getData();
        assertNotNull(result);
        assertEquals(3, result.size(), "应有3种加班类型");
        // 验证每个都有lowerLimit字段
        for (Map<String, Object> map : result) {
            assertTrue(map.containsKey("lowerLimit"), "每种类型应有lowerLimit字段");
            assertTrue(map.containsKey("code"), "每种类型应有code字段");
        }
    }
}
