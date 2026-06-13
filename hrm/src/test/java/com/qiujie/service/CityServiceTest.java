package com.qiujie.service;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.City;
import com.qiujie.mapper.CityMapper;
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
 * 城市社保标准 Service 层单元测试
 * 覆盖：add/delete/deleteBatch/edit/query/queryAll/list/export/imp 全部分支路径
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("城市社保标准Service层单元测试")
class CityServiceTest {

    @Mock
    private CityMapper cityMapper;

    private CityService cityService;

    private City buildCity() {
        City city = new City();
        city.setName("测试城市");
        city.setAverageSalary(new BigDecimal("10000"));
        city.setLowerSalary(new BigDecimal("5000"));
        return city;
    }

    @BeforeEach
    void setUp() {
        cityService = spy(new CityService());
        ReflectionTestUtils.setField(cityService, "baseMapper", cityMapper);
    }

    // ==================== add ====================

    @Test
    @DisplayName("add — 保存成功")
    void testAdd_Success() {
        doReturn(true).when(cityService).save(any(City.class));

        ResponseDTO rsp = cityService.add(buildCity());

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("add — 保存失败")
    void testAdd_Failure() {
        doReturn(false).when(cityService).save(any(City.class));

        ResponseDTO rsp = cityService.add(buildCity());

        assertEquals(300, rsp.getCode());
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete — 删除成功")
    void testDelete_Success() {
        doReturn(true).when(cityService).removeById(1);

        ResponseDTO rsp = cityService.delete(1);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("delete — 删除失败")
    void testDelete_Failure() {
        doReturn(false).when(cityService).removeById(999);

        ResponseDTO rsp = cityService.delete(999);

        assertEquals(300, rsp.getCode());
    }

    // ==================== deleteBatch ====================

    @Test
    @DisplayName("deleteBatch — 批量删除成功")
    void testDeleteBatch_Success() {
        List<Integer> ids = Arrays.asList(1, 2);
        doReturn(true).when(cityService).removeBatchByIds(ids);

        ResponseDTO rsp = cityService.deleteBatch(ids);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("deleteBatch — 批量删除失败")
    void testDeleteBatch_Failure() {
        List<Integer> ids = Arrays.asList(1, 2);
        doReturn(false).when(cityService).removeBatchByIds(ids);

        ResponseDTO rsp = cityService.deleteBatch(ids);

        assertEquals(300, rsp.getCode());
    }

    // ==================== edit ====================

    @Test
    @DisplayName("edit — 更新成功")
    void testEdit_Success() {
        doReturn(true).when(cityService).updateById(any(City.class));

        City city = buildCity();
        city.setId(1);
        ResponseDTO rsp = cityService.edit(city);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("edit — 更新失败")
    void testEdit_Failure() {
        doReturn(false).when(cityService).updateById(any(City.class));

        ResponseDTO rsp = cityService.edit(buildCity());

        assertEquals(300, rsp.getCode());
    }

    // ==================== query ====================

    @Test
    @DisplayName("query — 查询成功")
    void testQuery_Success() {
        City city = buildCity();
        city.setId(1);
        doReturn(city).when(cityService).getById(1);

        ResponseDTO rsp = cityService.query(1);

        assertEquals(200, rsp.getCode());
        City result = (City) rsp.getData();
        assertEquals("测试城市", result.getName());
    }

    @Test
    @DisplayName("query — ID不存在")
    void testQuery_NotFound() {
        doReturn(null).when(cityService).getById(999);

        ResponseDTO rsp = cityService.query(999);

        assertEquals(300, rsp.getCode());
    }

    // ==================== queryAll ====================

    @Test
    @DisplayName("queryAll — 返回所有城市列表")
    void testQueryAll_Success() {
        doReturn(Arrays.asList(buildCity(), buildCity())).when(cityService).list();

        ResponseDTO rsp = cityService.queryAll();

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        List<City> result = (List<City>) rsp.getData();
        assertEquals(2, result.size());
    }

    // ==================== list ====================

    @Test
    @DisplayName("list — 无条件分页查询")
    void testList_NoCondition() {
        IPage<City> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(buildCity()));
        mockPage.setTotal(1);
        mockPage.setPages(1);
        // name = null → wrapper 为 null
        doReturn(mockPage).when(cityService).page(any(IPage.class), isNull());

        ResponseDTO rsp = cityService.list(1, 10, null);

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) rsp.getData();
        assertEquals(1L, data.get("pages"));
    }

    @Test
    @DisplayName("list — 按名称条件查询")
    void testList_ByName() {
        IPage<City> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(buildCity()));
        mockPage.setTotal(1);
        mockPage.setPages(1);
        doReturn(mockPage).when(cityService).page(any(IPage.class), any(QueryWrapper.class));

        ResponseDTO rsp = cityService.list(1, 10, "北京");

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("list — 名称为空字符串")
    void testList_EmptyName() {
        IPage<City> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList());
        mockPage.setTotal(0);
        mockPage.setPages(0);
        doReturn(mockPage).when(cityService).page(any(IPage.class), isNull());

        ResponseDTO rsp = cityService.list(1, 10, "");

        assertEquals(200, rsp.getCode());
    }

    // ==================== export ====================

    @Test
    @DisplayName("export — 写Excel不抛异常即成功")
    void testExport_Success() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        doReturn(Arrays.asList(buildCity())).when(cityService).list();

        assertDoesNotThrow(() -> cityService.export(response, "cities"));

        assertTrue(response.getContentType().startsWith("application/vnd.ms-excel"));
    }

    // ==================== imp ====================

    @Test
    @DisplayName("imp — 导入成功")
    void testImp_Success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        doReturn(true).when(cityService).saveBatch(anyList());

        try (MockedStatic<com.qiujie.util.HutoolExcelUtil> hutoolMock = mockStatic(com.qiujie.util.HutoolExcelUtil.class)) {
            hutoolMock.when(() -> com.qiujie.util.HutoolExcelUtil.readExcel(any(InputStream.class), eq(1), eq(City.class)))
                    .thenReturn(Arrays.asList(buildCity()));

            ResponseDTO rsp = cityService.imp(file);

            assertEquals(200, rsp.getCode());
            verify(cityService, times(1)).saveBatch(anyList());
        }
    }

    @Test
    @DisplayName("imp — 导入失败（saveBatch返回false）")
    void testImp_Failure() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        doReturn(false).when(cityService).saveBatch(anyList());

        try (MockedStatic<com.qiujie.util.HutoolExcelUtil> hutoolMock = mockStatic(com.qiujie.util.HutoolExcelUtil.class)) {
            hutoolMock.when(() -> com.qiujie.util.HutoolExcelUtil.readExcel(any(InputStream.class), eq(1), eq(City.class)))
                    .thenReturn(Arrays.asList(buildCity()));

            ResponseDTO rsp = cityService.imp(file);

            assertEquals(300, rsp.getCode());
        }
    }
}
