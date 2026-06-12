package com.qiujie.service;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Dept;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;

/**
 * DeptService 白盒测试
 * 覆盖：时间字段完整性校验、delete/query 的 null 和边界防护
 *
 * @see TE.md 第 9.2 节
 */
@DisplayName("DeptService 白盒测试")
class DeptServiceWhiteBoxTest {

    private DeptService deptService;

    @BeforeEach
    void setUp() {
        deptService = spy(new DeptService());
    }

    // ================================================================
    // TC-DEPT-WB-001: add() — 时间字段不完整
    // ================================================================

    /**
     * 传入任意一个时间字段但缺少其他三个 → hasAnyTimeField=true，
     * 进入时间校验块，因 morEndTime/aftStartTime/aftEndTime 为 null 返回 ERROR。
     * 本测试验证四种"只传一个时间字段"的情况。
     */
    @Test
    @DisplayName("TC-DEPT-WB-001: add() — 只传入部分时间字段应返回 ERROR")
    void testAdd_PartialTimeFields_ReturnsError() {
        // 场景1: 只传 morStartTime
        Dept dept1 = new Dept();
        dept1.setName("测试部门");
        dept1.setParentId(0);
        dept1.setMorStartTime(Timestamp.valueOf("2026-06-12 09:00:00"));
        // morEndTime, aftStartTime, aftEndTime 均为 null

        ResponseDTO rsp1 = deptService.add(dept1);
        assertEquals(300, rsp1.getCode(), "只传 morStartTime 应返回 ERROR");

        // 场景2: 只传 morEndTime
        Dept dept2 = new Dept();
        dept2.setName("测试部门");
        dept2.setParentId(0);
        dept2.setMorEndTime(Timestamp.valueOf("2026-06-12 12:00:00"));

        ResponseDTO rsp2 = deptService.add(dept2);
        assertEquals(300, rsp2.getCode(), "只传 morEndTime 应返回 ERROR");

        // 场景3: 只传 aftStartTime
        Dept dept3 = new Dept();
        dept3.setName("测试部门");
        dept3.setParentId(0);
        dept3.setAftStartTime(Timestamp.valueOf("2026-06-12 14:00:00"));

        ResponseDTO rsp3 = deptService.add(dept3);
        assertEquals(300, rsp3.getCode(), "只传 aftStartTime 应返回 ERROR");

        // 场景4: 传了3个但缺1个 (缺 aftEndTime)
        Dept dept4 = new Dept();
        dept4.setName("测试部门");
        dept4.setParentId(0);
        dept4.setMorStartTime(Timestamp.valueOf("2026-06-12 09:00:00"));
        dept4.setMorEndTime(Timestamp.valueOf("2026-06-12 12:00:00"));
        dept4.setAftStartTime(Timestamp.valueOf("2026-06-12 14:00:00"));
        // aftEndTime 为 null

        ResponseDTO rsp4 = deptService.add(dept4);
        assertEquals(300, rsp4.getCode(), "四缺一 (缺 aftEndTime) 应返回 ERROR");
    }

    // ================================================================
    // TC-DEPT-WB-002: delete() — id 为 null
    // ================================================================

    /**
     * Controller 层 Spring 类型转换会拦截 null 路径变量，
     * 但 Service 层防御代码 `if (id == null || id <= 0)` 仍应被测试。
     */
    @Test
    @DisplayName("TC-DEPT-WB-002: delete() — id=null 应返回 ERROR")
    void testDelete_NullId_ReturnsError() {
        ResponseDTO rsp = deptService.delete(null);
        assertEquals(300, rsp.getCode(), "null id 应返回 ERROR");
    }

    // ================================================================
    // TC-DEPT-WB-003: query() — id 为 null → IllegalArgumentException
    // ================================================================

    /**
     * query() 对 null id 抛出 IllegalArgumentException 而非返回 ResponseDTO，
     * 这是唯一通过 Service 直接调用才能触发的路径。
     */
    @Test
    @DisplayName("TC-DEPT-WB-003: query() — id=null 应抛出 IllegalArgumentException")
    void testQuery_NullId_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> deptService.query(null),
                "null id 应抛出 IllegalArgumentException"
        );
        assertEquals("ID不能为空", ex.getMessage());
    }

    // ================================================================
    // TC-DEPT-WB-004: query() — id ≤ 0 → IllegalArgumentException
    // ================================================================

    /**
     * query() 对非正数 id 抛出 IllegalArgumentException，
     * 此路径同样只能通过 Service 直接调用触发。
     */
    @Test
    @DisplayName("TC-DEPT-WB-004: query() — id≤0 应抛出 IllegalArgumentException")
    void testQuery_NonPositiveId_ThrowsIllegalArgumentException() {
        // id = 0
        IllegalArgumentException exZero = assertThrows(
                IllegalArgumentException.class,
                () -> deptService.query(0),
                "id=0 应抛出 IllegalArgumentException"
        );
        assertEquals("ID必须为正数", exZero.getMessage());

        // id = -1
        IllegalArgumentException exNeg = assertThrows(
                IllegalArgumentException.class,
                () -> deptService.query(-1),
                "id=-1 应抛出 IllegalArgumentException"
        );
        assertEquals("ID必须为正数", exNeg.getMessage());
    }
}
