package com.qiujie.service;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Insurance;
import com.qiujie.mapper.InsuranceMapper;
import com.qiujie.vo.StaffInsuranceVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 社保公积金 Service 层单元测试
 * 覆盖：add/delete/deleteBatch/edit/query/queryByStaffId/list/export/imp 全部分支路径
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("社保公积金Service层单元测试")
class InsuranceServiceTest {

    @Mock
    private InsuranceMapper insuranceMapper;

    private InsuranceService insuranceService;

    private Insurance buildInsurance() {
        Insurance insurance = new Insurance();
        insurance.setStaffId(1);
        insurance.setCityId(1);
        insurance.setSocialBase(new BigDecimal("15000"));
        insurance.setHouseBase(new BigDecimal("15000"));
        return insurance;
    }

    @BeforeEach
    void setUp() {
        insuranceService = spy(new InsuranceService());
        ReflectionTestUtils.setField(insuranceService, "baseMapper", insuranceMapper);
        ReflectionTestUtils.setField(insuranceService, "insuranceMapper", insuranceMapper);
    }

    // ==================== add ====================

    @Test
    @DisplayName("add — 保存成功")
    void testAdd_Success() {
        doReturn(true).when(insuranceService).save(any(Insurance.class));

        ResponseDTO rsp = insuranceService.add(buildInsurance());

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("add — 保存失败")
    void testAdd_Failure() {
        doReturn(false).when(insuranceService).save(any(Insurance.class));

        ResponseDTO rsp = insuranceService.add(buildInsurance());

        assertEquals(300, rsp.getCode());
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete — 删除成功")
    void testDelete_Success() {
        doReturn(true).when(insuranceService).removeById(1);

        ResponseDTO rsp = insuranceService.delete(1);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("delete — 删除失败")
    void testDelete_Failure() {
        doReturn(false).when(insuranceService).removeById(999);

        ResponseDTO rsp = insuranceService.delete(999);

        assertEquals(300, rsp.getCode());
    }

    // ==================== deleteBatch ====================

    @Test
    @DisplayName("deleteBatch — 批量删除成功")
    void testDeleteBatch_Success() {
        List<Integer> ids = Arrays.asList(1, 2);
        doReturn(true).when(insuranceService).removeBatchByIds(ids);

        ResponseDTO rsp = insuranceService.deleteBatch(ids);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("deleteBatch — 批量删除失败")
    void testDeleteBatch_Failure() {
        List<Integer> ids = Arrays.asList(1, 2);
        doReturn(false).when(insuranceService).removeBatchByIds(ids);

        ResponseDTO rsp = insuranceService.deleteBatch(ids);

        assertEquals(300, rsp.getCode());
    }

    // ==================== edit ====================

    @Test
    @DisplayName("edit — 更新成功")
    void testEdit_Success() {
        doReturn(true).when(insuranceService).updateById(any(Insurance.class));

        Insurance insurance = buildInsurance();
        insurance.setId(1);
        ResponseDTO rsp = insuranceService.edit(insurance);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("edit — 更新失败")
    void testEdit_Failure() {
        doReturn(false).when(insuranceService).updateById(any(Insurance.class));

        ResponseDTO rsp = insuranceService.edit(buildInsurance());

        assertEquals(300, rsp.getCode());
    }

    // ==================== query ====================

    @Test
    @DisplayName("query — 查询成功")
    void testQuery_Success() {
        Insurance insurance = buildInsurance();
        insurance.setId(1);
        doReturn(insurance).when(insuranceService).getById(1);

        ResponseDTO rsp = insuranceService.query(1);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("query — ID不存在")
    void testQuery_NotFound() {
        doReturn(null).when(insuranceService).getById(999);

        ResponseDTO rsp = insuranceService.query(999);

        assertEquals(300, rsp.getCode());
    }

    // ==================== queryByStaffId ====================

    @Test
    @DisplayName("queryByStaffId — 查询成功")
    void testQueryByStaffId_Success() {
        Insurance insurance = buildInsurance();
        doReturn(insurance).when(insuranceService).getOne(any(QueryWrapper.class));

        ResponseDTO rsp = insuranceService.queryByStaffId(1);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("queryByStaffId — 员工无社保记录")
    void testQueryByStaffId_NotFound() {
        doReturn(null).when(insuranceService).getOne(any(QueryWrapper.class));

        ResponseDTO rsp = insuranceService.queryByStaffId(999);

        assertEquals(300, rsp.getCode());
    }

    // ==================== list ====================

    @Test
    @DisplayName("list — 无条件分页（deptId=null，查询全部）")
    void testList_NoDept() {
        IPage<StaffInsuranceVO> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(new StaffInsuranceVO()));
        mockPage.setTotal(1);
        mockPage.setPages(1);
        when(insuranceMapper.listStaffInsuranceVO(any(IPage.class), eq(""))).thenReturn(mockPage);

        ResponseDTO rsp = insuranceService.list(1, 10, null, null);

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) rsp.getData();
        assertNotNull(data.get("list"));
    }

    @Test
    @DisplayName("list — 按部门过滤查询")
    void testList_ByDept() {
        IPage<StaffInsuranceVO> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(new StaffInsuranceVO()));
        mockPage.setTotal(1);
        mockPage.setPages(1);
        when(insuranceMapper.listStaffDeptInsuranceVO(any(IPage.class), eq(""), eq(1))).thenReturn(mockPage);

        ResponseDTO rsp = insuranceService.list(1, 10, null, 1);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("list — name为null时自动转空字符串")
    void testList_NullName() {
        IPage<StaffInsuranceVO> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList());
        mockPage.setTotal(0);
        mockPage.setPages(0);
        when(insuranceMapper.listStaffInsuranceVO(any(IPage.class), eq(""))).thenReturn(mockPage);

        ResponseDTO rsp = insuranceService.list(1, 10, null, null);

        assertEquals(200, rsp.getCode());
    }

    // ==================== export ====================

    @Test
    @DisplayName("export — 写Excel不抛异常即成功")
    void testExport_Success() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(insuranceMapper.queryStaffInsuranceVO()).thenReturn(Arrays.asList(new StaffInsuranceVO()));

        assertDoesNotThrow(() -> insuranceService.export(response, "insurance"));

        assertTrue(response.getContentType().startsWith("application/vnd.ms-excel"));
    }

    // ==================== imp ====================

    @Test
    @DisplayName("imp — 导入成功")
    void testImp_Success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        doReturn(true).when(insuranceService).saveBatch(anyList());

        try (MockedStatic<com.qiujie.util.HutoolExcelUtil> hutoolMock = mockStatic(com.qiujie.util.HutoolExcelUtil.class)) {
            hutoolMock.when(() -> com.qiujie.util.HutoolExcelUtil.readExcel(any(InputStream.class), eq(1), eq(Insurance.class)))
                    .thenReturn(Arrays.asList(buildInsurance()));

            ResponseDTO rsp = insuranceService.imp(file);

            assertEquals(200, rsp.getCode());
            verify(insuranceService, times(1)).saveBatch(anyList());
        }
    }

    @Test
    @DisplayName("imp — 导入失败（saveBatch返回false）")
    void testImp_Failure() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        doReturn(false).when(insuranceService).saveBatch(anyList());

        try (MockedStatic<com.qiujie.util.HutoolExcelUtil> hutoolMock = mockStatic(com.qiujie.util.HutoolExcelUtil.class)) {
            hutoolMock.when(() -> com.qiujie.util.HutoolExcelUtil.readExcel(any(InputStream.class), eq(1), eq(Insurance.class)))
                    .thenReturn(Arrays.asList(buildInsurance()));

            ResponseDTO rsp = insuranceService.imp(file);

            assertEquals(300, rsp.getCode());
        }
    }
}
