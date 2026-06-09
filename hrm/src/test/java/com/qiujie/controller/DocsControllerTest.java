package com.qiujie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiujie.entity.Docs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 文档管理Controller层单元测试
 * 测试范围：REST API接口
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("文档管理Controller层测试")
class DocsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Integer testDocId;

    @BeforeEach
    void setUp() {
        // 使用已知的文档ID
        testDocId = 1;
    }

    // ==================== 文件上传测试 ====================

    @Test
    @DisplayName("TC-DOCS-001: 文件上传 - 成功场景")
    @WithMockUser(username = "admin", authorities = {"system:docs:upload"})
    void testUploadFile_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.multipart("/docs/upload/{id}", testDocId)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DOCS-002: 文件上传 - 文件格式不支持")
    @WithMockUser(username = "admin", authorities = {"system:docs:upload"})
    void testUploadFile_UnsupportedFormat() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/x-msdownload",
                "exe content".getBytes()
        );

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.multipart("/docs/upload/{id}", testDocId)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-DOCS-004: 文件上传 - 空文件")
    @WithMockUser(username = "admin", authorities = {"system:docs:upload"})
    void testUploadFile_EmptyFile() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.multipart("/docs/upload/{id}", testDocId)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-DOCS-013: 文件上传 - 文件名边界值（1字符）")
    @WithMockUser(username = "admin", authorities = {"system:docs:upload"})
    void testUploadFile_SingleCharName() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "a.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.multipart("/docs/upload/{id}", testDocId)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DOCS-020: 文件上传 - 特殊字符文件名")
    @WithMockUser(username = "admin", authorities = {"system:docs:upload"})
    void testUploadFile_SpecialChars() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test@#$%^&().pdf",
                "application/pdf",
                "test content".getBytes()
        );

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.multipart("/docs/upload/{id}", testDocId)
                        .file(file))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-DOCS-021: 文件上传 - 中文文件名")
    @WithMockUser(username = "admin", authorities = {"system:docs:upload"})
    void testUploadFile_ChineseName() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "测试文档.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.multipart("/docs/upload/{id}", testDocId)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DOCS-022: 文件上传 - 无扩展名")
    @WithMockUser(username = "admin", authorities = {"system:docs:upload"})
    void testUploadFile_NoExtension() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "no_extension",
                "application/octet-stream",
                "test content".getBytes()
        );

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.multipart("/docs/upload/{id}", testDocId)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-DOCS-023: 文件上传 - 多个扩展名")
    @WithMockUser(username = "admin", authorities = {"system:docs:upload"})
    void testUploadFile_MultipleExtensions() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg.exe",
                "application/x-msdownload",
                "test content".getBytes()
        );

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.multipart("/docs/upload/{id}", testDocId)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-DOCS-024: 文件上传 - ID不存在")
    @WithMockUser(username = "admin", authorities = {"system:docs:upload"})
    void testUploadFile_NonExistentId() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.multipart("/docs/upload/{id}", 999999)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-DOCS-025: 文件上传 - ID为负数")
    @WithMockUser(username = "admin", authorities = {"system:docs:upload"})
    void testUploadFile_NegativeId() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        // When & Then - 负数ID会导致员工不存在验证失败，返回code=300
        mockMvc.perform(MockMvcRequestBuilders.multipart("/docs/upload/{id}", -1)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    // ==================== 文件下载测试 ====================

    @Test
    @DisplayName("TC-DOCS-006: 文件下载 - 文件不存在")
    @WithMockUser(username = "admin", authorities = {"system:docs:download"})
    void testDownloadFile_NonExistent() throws Exception {
        // When & Then
        mockMvc.perform(get("/docs/download/{filename}", "not_exist.pdf"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-DOCS-007: 文件下载 - 路径遍历攻击")
    @WithMockUser(username = "admin", authorities = {"system:docs:download"})
    void testDownloadFile_PathTraversal() throws Exception {
        // When & Then - Spring Security 防火墙会拦截包含 .. 的 URL，返回 400
        mockMvc.perform(get("/docs/download/{filename}", "../../etc/passwd"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-DOCS-027: 文件下载 - 文件名包含空格")
    @WithMockUser(username = "admin", authorities = {"system:docs:download"})
    void testDownloadFile_WithSpace() throws Exception {
        // When & Then - URL编码后的空格字符会被拦截，返回400
        mockMvc.perform(get("/docs/download/{filename}", "test%20file.pdf"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-DOCS-028: 文件下载 - 文件名包含中文")
    @WithMockUser(username = "admin", authorities = {"system:docs:download"})
    void testDownloadFile_ChineseName() throws Exception {
        // When & Then - URL编码后的中文字符会被拦截，返回400
        mockMvc.perform(get("/docs/download/{filename}", "%E6%B5%8B%E8%AF%95.pdf"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-DOCS-029: 文件下载 - 路径遍历攻击（../）")
    @WithMockUser(username = "admin", authorities = {"system:docs:download"})
    void testDownloadFile_PathTraversalAttack() throws Exception {
        // When & Then - Spring Security 防火墙会拦截包含 .. 的 URL，返回 400
        mockMvc.perform(get("/docs/download/{filename}", "../../../etc/passwd"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-DOCS-030: 文件下载 - 绝对路径攻击")
    @WithMockUser(username = "admin", authorities = {"system:docs:download"})
    void testDownloadFile_AbsolutePath() throws Exception {
        // When & Then - 绝对路径包含 /，文件不存在返回 404
        mockMvc.perform(get("/docs/download/{filename}", "C:/Windows/System32/config/sam"))
                .andExpect(status().isNotFound());
    }

    // ==================== 文档管理测试 ====================

    @Test
    @DisplayName("TC-DOCS-008: 文档新增 - 成功场景")
    @WithMockUser(username = "admin", authorities = {"system:docs:add"})
    void testAddDoc_Success() throws Exception {
        // Given
        Docs newDoc = new Docs();
        newDoc.setOldName("原始文件名.pdf");
        newDoc.setName("新文件名.pdf");
        newDoc.setStaffId(1);
        newDoc.setType("文档");

        // When & Then
        mockMvc.perform(post("/docs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDoc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DOCS-009: 文档删除 - 成功场景")
    @WithMockUser(username = "admin", authorities = {"system:docs:delete"})
    void testDeleteDoc_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/docs/{id}", testDocId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DOCS-010: 文档列表 - 按原文件名搜索")
    @WithMockUser(username = "admin", authorities = {"system:docs:list"})
    void testListDocs_ByOldName() throws Exception {
        // When & Then
        mockMvc.perform(get("/docs")
                        .param("current", "1")
                        .param("size", "10")
                        .param("oldName", "报告"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DOCS-011: 文档列表 - 按员工姓名搜索")
    @WithMockUser(username = "admin", authorities = {"system:docs:list"})
    void testListDocs_ByStaffName() throws Exception {
        // When & Then
        mockMvc.perform(get("/docs")
                        .param("current", "1")
                        .param("size", "10")
                        .param("staffName", "张三"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DOCS-031: 文档新增 - 原文件名边界值（1字符）")
    @WithMockUser(username = "admin", authorities = {"system:docs:add"})
    void testAddDoc_OldNameMinLength() throws Exception {
        // Given
        Docs newDoc = new Docs();
        newDoc.setOldName("a.pdf");
        newDoc.setName("b.pdf");
        newDoc.setStaffId(1);

        // When & Then
        mockMvc.perform(post("/docs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDoc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DOCS-034: 文档新增 - 新文件名重复")
    @WithMockUser(username = "admin", authorities = {"system:docs:add"})
    void testAddDoc_DuplicateNewName() throws Exception {
        // Given - 先创建一个文档
        Docs existingDoc = new Docs();
        existingDoc.setOldName("existing.pdf");
        existingDoc.setName("existing.pdf");
        existingDoc.setStaffId(1);
        mockMvc.perform(post("/docs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existingDoc)))
                .andExpect(status().isOk());

        // When & Then - 尝试添加相同名称的文档，应该返回300
        Docs newDoc = new Docs();
        newDoc.setOldName("test.pdf");
        newDoc.setName("existing.pdf"); // 与已存在的文档名称相同
        newDoc.setStaffId(1);

        mockMvc.perform(post("/docs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDoc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-DOCS-035: 文档列表 - 搜索关键词为空字符串")
    @WithMockUser(username = "admin", authorities = {"system:docs:list"})
    void testListDocs_EmptyKeyword() throws Exception {
        // When & Then
        mockMvc.perform(get("/docs")
                        .param("current", "1")
                        .param("size", "10")
                        .param("oldName", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DOCS-036: 文档列表 - 搜索关键词为特殊字符")
    @WithMockUser(username = "admin", authorities = {"system:docs:list"})
    void testListDocs_SpecialCharKeyword() throws Exception {
        // When & Then
        mockMvc.perform(get("/docs")
                        .param("current", "1")
                        .param("size", "10")
                        .param("oldName", "' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DOCS-012: 头像下载（无需权限）")
    void testDownloadAvatar_NoAuth() throws Exception {
        // When & Then - 头像文件不存在，返回404
        mockMvc.perform(get("/docs/avatar/{filename}", "avatar_1.jpg"))
                .andExpect(status().isNotFound());
    }

    // ==================== 缺失的测试用例补充 ====================

    @Test
    @DisplayName("TC-DOCS-003: 文件上传 - 文件过大")
    @WithMockUser(username = "admin", authorities = {"system:docs:upload"})
    void testUploadFile_FileTooLarge() throws Exception {
        // Given - 创建一个超过20MB的文件（这里用较小的文件模拟）
        byte[] largeContent = new byte[21 * 1024 * 1024]; // 21MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large_file.zip",
                "application/zip",
                largeContent
        );

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.multipart("/docs/upload/{id}", testDocId)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-DOCS-005: 文件下载 - 成功场景")
    @WithMockUser(username = "admin", authorities = {"system:docs:upload", "system:docs:download"})
    void testDownloadFile_Success() throws Exception {
        // Given - 先上传文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // 上传文件并获取返回的文件名（UUID文件名）
        String uploadResult = mockMvc.perform(MockMvcRequestBuilders.multipart("/docs/upload/{id}", testDocId)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        // 解析返回的JSON获取文件名
        objectMapper.readTree(uploadResult);
        String filename = objectMapper.readTree(uploadResult).get("data").get("name").asText();

        // When & Then - 使用上传后的文件名下载文件
        mockMvc.perform(get("/docs/download/{filename}", filename))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-DOCS-014: 文件上传 - 文件名边界值（100字符）")
    @WithMockUser(username = "admin", authorities = {"system:docs:upload"})
    void testUploadFile_NameMaxLength() throws Exception {
        // Given - 创建100字符的文件名
        String longName = "a".repeat(96) + ".jpg"; // 96个a + .jpg = 100字符
        MockMultipartFile file = new MockMultipartFile(
                "file",
                longName,
                "image/jpeg",
                "test content".getBytes()
        );

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.multipart("/docs/upload/{id}", testDocId)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DOCS-015: 文件上传 - 文件名超长（101字符）")
    @WithMockUser(username = "admin", authorities = {"system:docs:upload"})
    void testUploadFile_NameExceedsMax() throws Exception {
        // Given - 创建101字符的文件名
        String tooLongName = "a".repeat(97) + ".jpg"; // 97个a + .jpg = 101字符
        MockMultipartFile file = new MockMultipartFile(
                "file",
                tooLongName,
                "image/jpeg",
                "test content".getBytes()
        );

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.multipart("/docs/upload/{id}", testDocId)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-DOCS-016: 文件上传 - 文件大小边界值（19.9MB）")
    @WithMockUser(username = "admin", authorities = {"system:docs:upload"})
    void testUploadFile_SizeNearLimit() throws Exception {
        // Given - 创建接近20MB的文件（这里用较小文件模拟）
        byte[] content = new byte[19 * 1024 * 1024]; // 19MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large_file.zip",
                "application/zip",
                content
        );

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.multipart("/docs/upload/{id}", testDocId)
                        .file(file))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-DOCS-019: 文件上传 - 文件大小为1字节")
    @WithMockUser(username = "admin", authorities = {"system:docs:upload"})
    void testUploadFile_OneByte() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tiny.txt",
                "text/plain",
                new byte[]{1} // 1字节
        );

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.multipart("/docs/upload/{id}", testDocId)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DOCS-026: 文件下载 - 空文件名")
    @WithMockUser(username = "admin")
    void testDownloadFile_EmptyName() throws Exception {
        // When & Then - /docs/download 会匹配 /{id} 端点，id="download" 存在则返回200
        mockMvc.perform(get("/docs/download"))
                .andExpect(status().isOk());
    }
}
