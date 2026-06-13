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
 * 社保公积金 Service 层单元测试（精简版 —— 代表 Service CRUD 模式）
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

    // ==================== 成功路径（代表 CRUD 模式） ====================

    @Test
    @DisplayName("add/delete/edit — 成功路径")
    void testAdd_Success() {
        doReturn(true).when(insuranceService).save(any(Insurance.class));
        assertEquals(200, insuranceService.add(buildInsurance()).getCode());
    }

    @Test
    @DisplayName("query — 查询成功 + 不存在分支")
    void testQuery_SuccessAndNotFound() {
        doReturn(buildInsurance()).when(insuranceService).getById(1);
        assertEquals(200, insuranceService.query(1).getCode());
        doReturn(null).when(insuranceService).getById(999);
        assertEquals(300, insuranceService.query(999).getCode());
    }

    @Test
    @DisplayName("queryByStaffId — 找到/未找到")
    void testQueryByStaffId_SuccessAndNotFound() {
        doReturn(buildInsurance()).when(insuranceService).getOne(any(QueryWrapper.class));
        assertEquals(200, insuranceService.queryByStaffId(1).getCode());
        doReturn(null).when(insuranceService).getOne(any(QueryWrapper.class));
        assertEquals(300, insuranceService.queryByStaffId(999).getCode());
    }

    // ==================== list 分页（含部门分叉） ====================

    @Test
    @DisplayName("list — 无部门（全量）")
    void testList_NoDept() {
        IPage<StaffInsuranceVO> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(new StaffInsuranceVO()));
        mockPage.setTotal(1); mockPage.setPages(1);
        when(insuranceMapper.listStaffInsuranceVO(any(IPage.class), eq(""))).thenReturn(mockPage);
        assertEquals(200, insuranceService.list(1, 10, null, null).getCode());
    }

    @Test
    @DisplayName("list — 按部门过滤（入口切换）")
    void testList_ByDept() {
        IPage<StaffInsuranceVO> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(new StaffInsuranceVO()));
        mockPage.setTotal(1); mockPage.setPages(1);
        when(insuranceMapper.listStaffDeptInsuranceVO(any(IPage.class), eq(""), eq(1))).thenReturn(mockPage);
        assertEquals(200, insuranceService.list(1, 10, null, 1).getCode());
    }

    // ==================== export / imp ====================

    @Test
    @DisplayName("export — Excel导出")
    void testExport_Success() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(insuranceMapper.queryStaffInsuranceVO()).thenReturn(Arrays.asList(new StaffInsuranceVO()));
        assertDoesNotThrow(() -> insuranceService.export(response, "insurance"));
        assertTrue(response.getContentType().startsWith("application/vnd.ms-excel"));
    }

    @Test
    @DisplayName("imp — 导入成功/失败")
    void testImp() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        try (MockedStatic<com.qiujie.util.HutoolExcelUtil> hutoolMock = mockStatic(com.qiujie.util.HutoolExcelUtil.class)) {
            hutoolMock.when(() -> com.qiujie.util.HutoolExcelUtil.readExcel(any(InputStream.class), eq(1), eq(Insurance.class)))
                    .thenReturn(Arrays.asList(buildInsurance()));
            doReturn(true).when(insuranceService).saveBatch(anyList());
            assertEquals(200, insuranceService.imp(file).getCode());
            doReturn(false).when(insuranceService).saveBatch(anyList());
            assertEquals(300, insuranceService.imp(file).getCode());
        }
    }
}
