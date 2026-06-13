package com.qiujie.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiujie.dto.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.qiujie.util.RedisUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 集成测试基类
 * 提供 MockMvc、ObjectMapper 等公共能力，以及 Redis Mock 配置
 *
 * @author qiujie
 * @since 2026-06-13
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Mock Redis 依赖，避免测试环境 Redis 不可用导致测试失败
     */
    @MockBean
    protected RedisUtil redisUtil;

    protected static final String TEST_VALIDATE_CODE = "TEST12";

    @BeforeEach
    void setUpRedisMock() {
        when(redisUtil.get("validate:code")).thenReturn(TEST_VALIDATE_CODE);
        when(redisUtil.get("expire:time")).thenReturn(LocalDateTime.now().plusMinutes(5).toString());
    }

    /**
     * 解析 JSON 响应为 ResponseDTO
     */
    protected ResponseDTO parseResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        if (json == null || json.isEmpty()) {
            return new ResponseDTO(-1, "empty response");
        }
        return objectMapper.readValue(json, ResponseDTO.class);
    }

    /**
     * 从响应中提取 data 字段中的 id (支持 Integer/Map/Entity 等多种格式)
     */
    protected Integer parseDataAsInteger(MvcResult result) throws Exception {
        ResponseDTO response = parseResponse(result);
        if (response.getData() == null) {
            return null;
        }
        if (response.getData() instanceof Integer) {
            return (Integer) response.getData();
        }
        if (response.getData() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) response.getData();
            Object idObj = map.get("id");
            if (idObj instanceof Integer) {
                return (Integer) idObj;
            }
            if (idObj instanceof Number) {
                return ((Number) idObj).intValue();
            }
        }
        // 如果 data 是 List，取第一个元素的 id
        if (response.getData() instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) response.getData();
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> firstItem = (Map<String, Object>) list.get(0);
                Object idObj = firstItem.get("id");
                if (idObj instanceof Integer) {
                    return (Integer) idObj;
                }
                if (idObj instanceof Number) {
                    return ((Number) idObj).intValue();
                }
            }
        }
        try {
            return objectMapper.convertValue(response.getData(), Integer.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 执行 POST 请求并返回 MvcResult
     */
    protected MvcResult performPost(String url, Object body) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders
                        .post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    /**
     * 执行 PUT 请求并返回 MvcResult
     */
    protected MvcResult performPut(String url, Object body) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders
                        .put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    /**
     * 执行 GET 请求并返回 MvcResult
     */
    protected MvcResult performGet(String url) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders
                        .get(url))
                .andReturn();
    }

    /**
     * 创建实体并返回解析后的 data 中的 ID (Integer)
     * 期望 API 返回 {code:200, data: {id: xxx}} 或 {code:200, data: xxx}
     */
    protected Integer createEntityAndGetId(String url, Object body) throws Exception {
        MvcResult result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return parseDataAsInteger(result);
    }

    /**
     * 创建实体并断言成功，返回 Integer ID
     * @deprecated 使用 createEntityAndGetId 代替
     */
    @Deprecated
    protected Integer createAndGetId(String url, Object body) throws Exception {
        return createEntityAndGetId(url, body);
    }
}
