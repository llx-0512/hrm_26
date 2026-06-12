package com.qiujie.controller;

import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.exception.ServiceException;
import com.qiujie.service.DocsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DocsController 白盒测试
 * 覆盖 download() / getAvatar() 的 try-catch 异常→HTTP 状态映射
 *
 * @see TE.md 第 8.3 节
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("DocsController 白盒测试")
class DocsControllerWhiteBoxTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocsService docsService;

    // ================================================================
    // TC-DOCS-WB-001: download() — ServiceException → 404
    // ================================================================

    @Test
    @WithMockUser(username = "admin", authorities = {"system:docs:download"})
    @DisplayName("TC-DOCS-WB-001: download() — ServiceException → 404")
    void testDownload_ServiceException_MapsTo404() throws Exception {
        doThrow(new ServiceException(BusinessStatusEnum.FILE_NOT_EXIST))
                .when(docsService).download(eq("any.pdf"), any(HttpServletResponse.class));

        mockMvc.perform(get("/docs/download/{filename}", "any.pdf"))
                .andExpect(status().isNotFound());
    }

    // ================================================================
    // TC-DOCS-WB-002: download() — IllegalArgumentException → 400
    // ================================================================

    @Test
    @WithMockUser(username = "admin", authorities = {"system:docs:download"})
    @DisplayName("TC-DOCS-WB-002: download() — IllegalArgumentException → 400")
    void testDownload_IllegalArgument_MapsTo400() throws Exception {
        doThrow(new IllegalArgumentException("文件名不能为空"))
                .when(docsService).download(eq(""), any(HttpServletResponse.class));

        mockMvc.perform(get("/docs/download/{filename}", ""))
                .andExpect(status().isBadRequest());
    }

    // ================================================================
    // TC-DOCS-WB-003: download() — 两个 catch 分支独立验证
    // ================================================================

    /**
     * ServiceException 和 IllegalArgumentException 无继承关系，
     * catch 顺序不影响结果。本测试验证两者独立工作。
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"system:docs:download"})
    @DisplayName("TC-DOCS-WB-003: download() — ServiceException 与 IllegalArgumentException 互不干扰")
    void testDownload_CatchBlocks_Independent() throws Exception {
        // 场景1: ServiceException 走第一个 catch → 404
        doThrow(new ServiceException(BusinessStatusEnum.ERROR))
                .when(docsService).download(eq("x"), any(HttpServletResponse.class));

        mockMvc.perform(get("/docs/download/{filename}", "x"))
                .andExpect(status().isNotFound());

        // 场景2: IllegalArgumentException 走第二个 catch → 400
        doThrow(new IllegalArgumentException("非法参数"))
                .when(docsService).download(eq("y"), any(HttpServletResponse.class));

        mockMvc.perform(get("/docs/download/{filename}", "y"))
                .andExpect(status().isBadRequest());
    }

    // ================================================================
    // TC-DOCS-WB-004: getAvatar() — ServiceException → 404
    // ================================================================

    @Test
    @WithMockUser(username = "admin", authorities = {"system:docs:download"})
    @DisplayName("TC-DOCS-WB-004: getAvatar() — ServiceException → 404")
    void testGetAvatar_ServiceException_MapsTo404() throws Exception {
        doThrow(new ServiceException(BusinessStatusEnum.FILE_NOT_EXIST))
                .when(docsService).download(eq("avatar_x.jpg"), any(HttpServletResponse.class));

        mockMvc.perform(get("/docs/avatar/{filename}", "avatar_x.jpg"))
                .andExpect(status().isNotFound());
    }

    // ================================================================
    // TC-DOCS-WB-005: getAvatar() — IllegalArgumentException → 400
    // ================================================================

    @Test
    @WithMockUser(username = "admin", authorities = {"system:docs:download"})
    @DisplayName("TC-DOCS-WB-005: getAvatar() — IllegalArgumentException → 400")
    void testGetAvatar_IllegalArgument_MapsTo400() throws Exception {
        doThrow(new IllegalArgumentException("非法参数"))
                .when(docsService).download(eq(""), any(HttpServletResponse.class));

        mockMvc.perform(get("/docs/avatar/{filename}", ""))
                .andExpect(status().isBadRequest());
    }

    // ================================================================
    // TC-DOCS-WB-006: getAvatar() — 无需认证（公开接口）
    // ================================================================

    /**
     * getAvatar() 没有 @PreAuthorize 注解，是公开接口。
     * 不携带 Token 应走异常分支（404），而非被 Spring Security 拦截（401）。
     */
    @Test
    @DisplayName("TC-DOCS-WB-006: getAvatar() — 无认证应走异常分支(404)而非 401")
    void testGetAvatar_NoAuthenticationRequired() throws Exception {
        doThrow(new ServiceException(BusinessStatusEnum.FILE_NOT_EXIST))
                .when(docsService).download(eq("avatar_1.jpg"), any(HttpServletResponse.class));

        // 不添加任何 @WithMockUser → 应返回 404（ServiceException），证明无 @PreAuthorize
        mockMvc.perform(get("/docs/avatar/{filename}", "avatar_1.jpg"))
                .andExpect(status().isNotFound());
    }
}
