package com.lawfirm.application.matter;

import com.lawfirm.application.matter.command.CreateMatterCommand;
import com.lawfirm.application.matter.dto.MatterDTO;
import com.lawfirm.application.matter.dto.MatterQueryDTO;
import com.lawfirm.application.matter.service.MatterAppService;
import com.lawfirm.common.result.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 项目管理集成测试
 * 
 * 测试项目管理全流程：创建 → 查询 → 更新 → 关闭
 * 
 * @author junyuzhan
 * @since 2026-01-10
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("项目管理集成测试")
@org.junit.jupiter.api.Disabled("集成测试需要配置 SecurityContext，待后续完善")
class MatterIntegrationTest {

    @Autowired
    private MatterAppService matterAppService;

    @Test
    @DisplayName("分页查询项目列表")
    void testListMatters() {
        // Given
        MatterQueryDTO query = new MatterQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);

        // When
        PageResult<MatterDTO> result = matterAppService.listMatters(query);

        // Then
        assertNotNull(result);
        assertNotNull(result.getList());
        assertTrue(result.getTotal() >= 0);
    }

    @Test
    @DisplayName("查询我的项目")
    void testListMyMatters() {
        // Given
        MatterQueryDTO query = new MatterQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setMyMatters(true);

        // When
        PageResult<MatterDTO> result = matterAppService.listMatters(query);

        // Then
        assertNotNull(result);
        assertNotNull(result.getList());
    }

    @Test
    @DisplayName("按状态筛选项目")
    void testListMattersByStatus() {
        // Given
        MatterQueryDTO query = new MatterQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setStatus("ACTIVE");

        // When
        PageResult<MatterDTO> result = matterAppService.listMatters(query);

        // Then
        assertNotNull(result);
        // 验证返回的项目状态都是 ACTIVE
        result.getList().forEach(matter -> 
            assertEquals("ACTIVE", matter.getStatus())
        );
    }

    @Test
    @DisplayName("创建项目 - 无合同应失败")
    void testCreateMatterWithoutContract() {
        // Given
        CreateMatterCommand command = new CreateMatterCommand();
        command.setName("测试项目");
        command.setClientId(1L);
        // 不设置 contractId

        // When & Then
        assertThrows(Exception.class, () -> 
            matterAppService.createMatter(command)
        );
    }

    @Test
    @DisplayName("获取项目详情 - 不存在应返回null或抛异常")
    void testGetMatterNotFound() {
        // Given
        Long nonExistentId = 999999L;

        // When & Then
        assertThrows(Exception.class, () -> 
            matterAppService.getMatterById(nonExistentId)
        );
    }
}
