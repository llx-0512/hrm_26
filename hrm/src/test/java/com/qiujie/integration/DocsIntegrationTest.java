package com.qiujie.integration;

import com.qiujie.entity.Docs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 文件管理集成测试 (P1)
 * 覆盖: INT-DOCS-001~002
 *
 * @author qiujie
 * @since 2026-06-13
 */
@DisplayName("文件管理集成测试")
class DocsIntegrationTest extends BaseIntegrationTest {

    // ==================== INT-DOCS-001: 创建文档 → 上传文件 → 查询验证 ====================

    @Test
    @DisplayName("INT-DOCS-001: 创建文档记录并上传文件")
    @WithMockUser(authorities = {"system:docs:upload"})
    void testUploadFile_AssociateDocumentRecord() throws Exception {
        // Step 1: 创建文档记录 (POST /docs add 返回 {code:200, data:true})
        Docs docs = new Docs();
        docs.setName("测试文档");
        docs.setType("pdf");
        docs.setOldName("original.pdf");
        docs.setStaffId(1);

        mockMvc.perform(post("/docs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docs)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 2: 上传文件到新创建的文档
        // 新创建的文档 ID 在不含已有数据时从 1 开始
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf",
                "test file content".getBytes());
        mockMvc.perform(multipart("/docs/upload/{id}", 1).file(file))
                .andExpect(status().isOk());

        // Step 3: 验证文档可查询（最新创建的记录 ID 从 1 开始）
        mockMvc.perform(get("/docs/{id}", 1))
                .andExpect(status().isOk());
    }

    // ==================== INT-DOCS-002: 删除文档 → 级联处理 ====================

    @Test
    @DisplayName("INT-DOCS-002: 删除文档记录及级联处理")
    @WithMockUser(authorities = {"system:docs:upload", "system:docs:delete"})
    void testDeleteDocument_CascadeHandling() throws Exception {
        // 前置：创建文档
        Docs docs = new Docs();
        docs.setName("待删除文档");
        docs.setType("txt");
        docs.setOldName("to_delete.txt");
        docs.setStaffId(1);

        mockMvc.perform(post("/docs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docs)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 上传文件
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain",
                "to be deleted".getBytes());
        mockMvc.perform(multipart("/docs/upload/{id}", 2).file(file))
                .andExpect(status().isOk());

        // Step 1: 逻辑删除文档（新创建的第二个文档 ID=2）
        mockMvc.perform(delete("/docs/{id}", 2))
                .andExpect(status().isOk());

        // Step 2: 验证删除后可查询
        mockMvc.perform(get("/docs/{id}", 2))
                .andExpect(status().isOk());
    }
}
