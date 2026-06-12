package com.qiujie.exception;

import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.service.StaffService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BaseExceptionHandler 白盒测试
 * 通过 Mock StaffService 注入各类异常，精确验证 6 个 @ExceptionHandler 的映射。
 *
 * @see TE.md 第 8.4 节
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("BaseExceptionHandler 白盒测试")
class BaseExceptionHandlerWhiteBoxTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StaffService staffService;

    // ================================================================
    // TC-EXH-WB-001: IllegalArgumentException — "文件名不能为空" → code=600
    // ================================================================

    @Test
    @WithMockUser
    @DisplayName("TC-EXH-WB-001: IllegalArgumentException('文件名不能为空') → code=600")
    void testIllegalArgument_EmptyFileName_ReturnsFileNotExist() throws Exception {
        when(staffService.query(anyInt()))
                .thenThrow(new IllegalArgumentException("文件名不能为空"));

        mockMvc.perform(get("/staff/{id}", 1))
                .andExpect(jsonPath("$.code")
                        .value(BusinessStatusEnum.FILE_NOT_EXIST.getCode()));
    }

    // ================================================================
    // TC-EXH-WB-002: IllegalArgumentException — 其他消息 → code=300
    // ================================================================

    @Test
    @WithMockUser
    @DisplayName("TC-EXH-WB-002: IllegalArgumentException('其他非法参数') → code=300")
    void testIllegalArgument_OtherMessage_ReturnsError() throws Exception {
        when(staffService.query(anyInt()))
                .thenThrow(new IllegalArgumentException("其他非法参数"));

        mockMvc.perform(get("/staff/{id}", 1))
                .andExpect(jsonPath("$.code").value(300));
    }

    // ================================================================
    // TC-EXH-WB-003: ServiceException → 400 + 异常中的 code
    // ================================================================

    @Test
    @WithMockUser
    @DisplayName("TC-EXH-WB-003: ServiceException → 400 + 保留异常 code")
    void testServiceException_PreservesExceptionCode() throws Exception {
        when(staffService.query(anyInt()))
                .thenThrow(new ServiceException(BusinessStatusEnum.STAFF_NOT_EXIST));

        mockMvc.perform(get("/staff/{id}", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value(BusinessStatusEnum.STAFF_NOT_EXIST.getCode()));
    }

    // ================================================================
    // TC-EXH-WB-004: DataIntegrityViolationException → code=300
    // ================================================================

    @Test
    @WithMockUser
    @DisplayName("TC-EXH-WB-004: DataIntegrityViolationException → code=300")
    void testDataIntegrityViolation_ReturnsError() throws Exception {
        when(staffService.query(anyInt()))
                .thenThrow(new DataIntegrityViolationException("违反唯一约束"));

        // DataIntegrityViolationException 穿透 Controller → 被 @ControllerAdvice 统一捕获
        mockMvc.perform(get("/staff/{id}", 1))
                .andExpect(jsonPath("$.code").value(300));
    }

    // ================================================================
    // TC-EXH-WB-005: NullPointerException → code=300
    // ================================================================

    @Test
    @WithMockUser
    @DisplayName("TC-EXH-WB-005: NullPointerException → code=300")
    void testNullPointer_ReturnsError() throws Exception {
        when(staffService.query(anyInt()))
                .thenThrow(new NullPointerException("staff is null"));

        mockMvc.perform(get("/staff/{id}", 1))
                .andExpect(jsonPath("$.code").value(300));
    }

    // ================================================================
    // TC-EXH-WB-006: MethodArgumentTypeMismatchException → 400 + code=300
    // ================================================================

    @Test
    @WithMockUser
    @DisplayName("TC-EXH-WB-006: URL 参数类型不匹配 ('abc'→Integer) → 400 + code=300")
    void testTypeMismatch_ReturnsError() throws Exception {
        // 发送 "abc" 到需要 Integer 的 @PathVariable，触发类型转换异常
        mockMvc.perform(get("/staff/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(300));
    }
}
