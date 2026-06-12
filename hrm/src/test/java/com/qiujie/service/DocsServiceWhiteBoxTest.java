package com.qiujie.service;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Docs;
import com.qiujie.entity.Staff;
import com.qiujie.exception.ServiceException;
import com.qiujie.mapper.DocsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DocsService 白盒测试
 * 覆盖：上传校验链、MD5 去重、磁盘写入失败、下载安全检查
 *
 * @see TE.md 第 9.3 节
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DocsService 白盒测试")
class DocsServiceWhiteBoxTest {

    @Mock
    private DocsMapper docsMapper;

    @Mock
    private StaffService staffService;

    private DocsService docsService;

    private static final Integer STAFF_ID = 1;
    private static final String FILE_PATH = "/tmp/test-uploads/";

    // ========== 复用工具 ==========

    private Staff mockStaff() {
        Staff staff = new Staff();
        staff.setId(STAFF_ID);
        return staff;
    }

    /** 构造 MultipartFile Mock */
    private MultipartFile mockFile(String originalFilename, long size,
                                   boolean isEmpty, byte[] content) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(originalFilename);
        when(file.getSize()).thenReturn(size);
        when(file.isEmpty()).thenReturn(isEmpty);
        when(file.getInputStream()).thenReturn(
                new ByteArrayInputStream(content != null ? content : new byte[0]));
        return file;
    }

    @BeforeEach
    void setUp() {
        docsService = spy(new DocsService());
        ReflectionTestUtils.setField(docsService, "docsMapper", docsMapper);
        ReflectionTestUtils.setField(docsService, "staffService", staffService);
        ReflectionTestUtils.setField(docsService, "filePath", FILE_PATH);

        when(staffService.getById(STAFF_ID)).thenReturn(mockStaff());
    }

    // ================================================================
    // TC-DOCS-WB-001: upload() — 不允许的扩展名
    // ================================================================

    @ParameterizedTest
    @ValueSource(strings = {"test.exe", "test.bat", "test.sh", "test.js", "test.py"})
    @DisplayName("TC-DOCS-WB-001: upload() — 不允许的扩展名应返回 ERROR")
    void testUpload_EachDisallowedExtension_ReturnsError(String filename) throws IOException {
        MultipartFile file = mockFile(filename, 1024, false, "content".getBytes());

        ResponseDTO rsp = docsService.upload(file, STAFF_ID);

        assertEquals(300, rsp.getCode(),
                filename + " 不应被允许上传");
        // 未到达 save，仅走到扩展名校验
        verify(docsService, never()).save(any(Docs.class));
    }

    // ================================================================
    // TC-DOCS-WB-002: upload() — MD5 去重，复用已有文件
    // ================================================================

    /**
     * 上传内容相同的文件时，检测到 MD5 重复后跳过 transferTo，
     * 直接复用已有文件的 UUID 名称，只新增数据库记录。
     */
    @Test
    @DisplayName("TC-DOCS-WB-002: upload() — MD5 重复时跳过磁盘写入")
    void testUpload_DuplicateMd5_SkipsFileStorage() throws IOException {
        MultipartFile file = mockFile("report.pdf", 1024, false, "same content".getBytes());

        // 已有文件的 Docs 记录
        Docs existingDocs = new Docs();
        existingDocs.setName("abc123def456ghi78901.pdf");

        doReturn(Collections.singletonList(existingDocs))
                .when(docsService).list(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class));
        doReturn(true).when(docsService).save(any(Docs.class));

        ResponseDTO rsp = docsService.upload(file, STAFF_ID);

        assertEquals(200, rsp.getCode());
        // 文件名应复用已有文件的 UUID 名
        Docs savedDocs = (Docs) rsp.getData();
        assertEquals("abc123def456ghi78901.pdf", savedDocs.getName(),
                "MD5 重复时应复用已有文件名");
        // 验证 transferTo 未被调用（跳过了磁盘写入）
        verify(file, never()).transferTo(any(File.class));
    }

    // ================================================================
    // TC-DOCS-WB-003: upload() — 磁盘写入失败 → ServiceException
    // ================================================================

    @Test
    @DisplayName("TC-DOCS-WB-003: upload() — transferTo 失败应抛出 ServiceException")
    void testUpload_TransferToFails_ThrowsServiceException() throws IOException {
        MultipartFile file = mockFile("data.xlsx", 1024, false, "content".getBytes());

        doReturn(Collections.emptyList())
                .when(docsService).list(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class));
        // transferTo 时抛出 IOException
        doThrow(new IOException("磁盘空间不足"))
                .when(file).transferTo(any(File.class));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> docsService.upload(file, STAFF_ID),
                "磁盘写入失败应抛出 ServiceException");

        assertNotNull(ex.getMessage());
        // save 不会被调用（异常发生在 transferTo 之后、save 之前）
        verify(docsService, never()).save(any(Docs.class));
    }

    // ================================================================
    // TC-DOCS-WB-004: upload() — 文件大小刚好 20MB（边界值通过）
    // ================================================================

    @Test
    @DisplayName("TC-DOCS-WB-004: upload() — 刚好 20MB 应通过大小校验")
    void testUpload_Exactly20MB_Succeeds() throws IOException {
        long exact20MB = 20L * 1024 * 1024; // 20,971,520 bytes
        MultipartFile file = mockFile("large.jpg", exact20MB, false, "x".getBytes());

        doReturn(Collections.emptyList())
                .when(docsService).list(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class));
        doReturn(true).when(docsService).save(any(Docs.class));

        ResponseDTO rsp = docsService.upload(file, STAFF_ID);

        // 文件大小 == maxSize → 不应走 `> maxSize` 分支
        assertEquals(200, rsp.getCode(),
                "刚好 20MB 应通过大小校验");
        verify(docsService, times(1)).save(any(Docs.class));
    }

    // ================================================================
    // TC-DOCS-WB-005: upload() — 文件名刚好 100 字符（边界值通过）
    // ================================================================

    @Test
    @DisplayName("TC-DOCS-WB-005: upload() — 文件名 100 字符应通过长度校验")
    void testUpload_FilenameExactly100Chars_Succeeds() throws IOException {
        // "a".repeat(96) + ".jpg" = 100 字符
        String name100 = "a".repeat(96) + ".jpg";
        MultipartFile file = mockFile(name100, 1024, false, "x".getBytes());

        doReturn(Collections.emptyList())
                .when(docsService).list(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class));
        doReturn(true).when(docsService).save(any(Docs.class));

        ResponseDTO rsp = docsService.upload(file, STAFF_ID);

        assertEquals(200, rsp.getCode(),
                "文件名 100 字符应通过长度校验");
        verify(docsService, times(1)).save(any(Docs.class));
    }

    // ================================================================
    // TC-DOCS-WB-006: download() — 反斜杠路径遍历（Windows）
    // ================================================================

    @Test
    @DisplayName("TC-DOCS-WB-006: download() — 反斜杠 \\\\ 应抛出 IllegalArgumentException")
    void testDownload_BackslashPath_ThrowsIllegalArgumentException() {
        HttpServletResponse response = new MockHttpServletResponse();

        // "a\\b.pdf" 不含 ".."，只含 "\\" → 命中 contains("\\") 分支
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> docsService.download("a\\b.pdf", response),
                "含反斜杠的路径遍历应被拒绝"
        );
        assertEquals("非法路径格式", ex.getMessage());
    }

    // ================================================================
    // TC-DOCS-WB-007: download() — ".." 在 "/" 之前被捕获
    // ================================================================

    /**
     * "../../etc/passwd" 同时满足 ".." 和 "/" 两个检查条件，
     * 由于代码顺序是 `contains("..")` 在前，应命中第一个 if。
     */
    @Test
    @DisplayName("TC-DOCS-WB-007: download() — \"..\" 检查在 \"/\" 之前生效")
    void testDownload_BothDotDotAndSlash_FirstIfCatches() throws IOException {
        HttpServletResponse response = new MockHttpServletResponse();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> docsService.download("../../etc/passwd", response),
                "同时含 .. 和 / 应被第一个检查捕获"
        );
        // 命中的是 contains("..") → "非法文件名"，而非 contains("/") → "非法路径格式"
        assertEquals("非法文件名", ex.getMessage(),
                "应先命中 '..' 检查，消息为'非法文件名'");
    }
}
