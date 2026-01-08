package com.lawfirm.application.evidence.service;

import com.lawfirm.application.evidence.command.CreateEvidenceCommand;
import com.lawfirm.application.evidence.command.UpdateEvidenceCommand;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.evidence.entity.Evidence;
import com.lawfirm.domain.evidence.repository.EvidenceRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.infrastructure.external.file.FileTypeService;
import com.lawfirm.infrastructure.persistence.mapper.EvidenceCrossExamMapper;
import com.lawfirm.infrastructure.persistence.mapper.EvidenceMapper;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 证据权限控制属性测试
 * 
 * Feature: evidence-management
 * Property 7: 项目状态权限控制
 * Validates: Requirements 4.6, 9.2, 9.3
 * 
 * 测试已归档或已结案的项目，对其证据的编辑和删除操作应被拒绝
 */
class EvidencePermissionPropertyTest {

    // 只读状态（不允许编辑证据）
    private static final Set<String> READONLY_STATUSES = Set.of("ARCHIVED", "CLOSED");
    
    // 可编辑状态
    private static final Set<String> EDITABLE_STATUSES = Set.of("DRAFT", "PENDING", "ACTIVE", "SUSPENDED");

    /**
     * Property 7.1: 已归档或已结案的项目，创建证据应被拒绝
     */
    @Property(tries = 100)
    void createEvidenceShouldBeRejectedForReadonlyMatter(
            @ForAll("readonlyStatuses") String status,
            @ForAll("validEvidenceName") String evidenceName) {
        
        // Setup mocks
        EvidenceAppService service = createServiceWithMockedMatter(1L, status);
        
        CreateEvidenceCommand command = new CreateEvidenceCommand();
        command.setMatterId(1L);
        command.setName(evidenceName);
        command.setEvidenceType("DOCUMENTARY");
        
        // Verify that creating evidence throws BusinessException
        assertThatThrownBy(() -> service.createEvidence(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无法编辑证据");
    }

    /**
     * Property 7.2: 可编辑状态的项目，创建证据应被允许
     */
    @Property(tries = 100)
    void createEvidenceShouldBeAllowedForEditableMatter(
            @ForAll("editableStatuses") String status,
            @ForAll("validEvidenceName") String evidenceName) {
        
        // Setup mocks
        EvidenceRepository evidenceRepository = mock(EvidenceRepository.class);
        MatterRepository matterRepository = mock(MatterRepository.class);
        EvidenceMapper evidenceMapper = mock(EvidenceMapper.class);
        EvidenceCrossExamMapper crossExamMapper = mock(EvidenceCrossExamMapper.class);
        FileTypeService fileTypeService = new FileTypeService();
        
        Matter matter = new Matter();
        matter.setId(1L);
        matter.setStatus(status);
        when(matterRepository.findById(1L)).thenReturn(matter);
        when(evidenceRepository.getMaxSortOrder(anyLong(), any())).thenReturn(0);
        
        EvidenceAppService service = new EvidenceAppService(
                evidenceRepository, evidenceMapper, crossExamMapper, fileTypeService, matterRepository);
        service.setMatterAppService(mock(com.lawfirm.application.matter.service.MatterAppService.class));
        
        CreateEvidenceCommand command = new CreateEvidenceCommand();
        command.setMatterId(1L);
        command.setName(evidenceName);
        command.setEvidenceType("DOCUMENTARY");
        
        // Should not throw exception
        try {
            service.createEvidence(command);
        } catch (BusinessException e) {
            // Should not be a permission error
            assertThat(e.getMessage()).doesNotContain("无法编辑证据");
        } catch (Exception e) {
            // Other exceptions are acceptable (e.g., from mocked repository)
        }
        
        // Verify that permission check passed (matter was queried)
        verify(matterRepository).findById(1L);
    }

    /**
     * Property 7.3: 已归档或已结案的项目，更新证据应被拒绝
     */
    @Property(tries = 100)
    void updateEvidenceShouldBeRejectedForReadonlyMatter(
            @ForAll("readonlyStatuses") String status,
            @ForAll("validEvidenceName") String newName) {
        
        // Setup mocks
        EvidenceRepository evidenceRepository = mock(EvidenceRepository.class);
        MatterRepository matterRepository = mock(MatterRepository.class);
        EvidenceMapper evidenceMapper = mock(EvidenceMapper.class);
        EvidenceCrossExamMapper crossExamMapper = mock(EvidenceCrossExamMapper.class);
        FileTypeService fileTypeService = new FileTypeService();
        
        Evidence evidence = Evidence.builder()
                .id(1L)
                .matterId(1L)
                .name("Test Evidence")
                .evidenceType("DOCUMENTARY")
                .build();
        when(evidenceRepository.getByIdOrThrow(1L, "证据不存在")).thenReturn(evidence);
        
        Matter matter = new Matter();
        matter.setId(1L);
        matter.setStatus(status);
        when(matterRepository.findById(1L)).thenReturn(matter);
        
        EvidenceAppService service = new EvidenceAppService(
                evidenceRepository, evidenceMapper, crossExamMapper, fileTypeService, matterRepository);
        service.setMatterAppService(mock(com.lawfirm.application.matter.service.MatterAppService.class));
        
        UpdateEvidenceCommand command = new UpdateEvidenceCommand();
        command.setName(newName);
        
        // Verify that updating evidence throws BusinessException
        assertThatThrownBy(() -> service.updateEvidence(1L, command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无法编辑证据");
    }

    /**
     * Property 7.4: 已归档或已结案的项目，删除证据应被拒绝
     */
    @Property(tries = 100)
    void deleteEvidenceShouldBeRejectedForReadonlyMatter(
            @ForAll("readonlyStatuses") String status) {
        
        // Setup mocks
        EvidenceRepository evidenceRepository = mock(EvidenceRepository.class);
        MatterRepository matterRepository = mock(MatterRepository.class);
        EvidenceMapper evidenceMapper = mock(EvidenceMapper.class);
        EvidenceCrossExamMapper crossExamMapper = mock(EvidenceCrossExamMapper.class);
        FileTypeService fileTypeService = new FileTypeService();
        
        Evidence evidence = Evidence.builder()
                .id(1L)
                .matterId(1L)
                .name("Test Evidence")
                .evidenceType("DOCUMENTARY")
                .build();
        when(evidenceRepository.getByIdOrThrow(1L, "证据不存在")).thenReturn(evidence);
        
        Matter matter = new Matter();
        matter.setId(1L);
        matter.setStatus(status);
        when(matterRepository.findById(1L)).thenReturn(matter);
        
        EvidenceAppService service = new EvidenceAppService(
                evidenceRepository, evidenceMapper, crossExamMapper, fileTypeService, matterRepository);
        service.setMatterAppService(mock(com.lawfirm.application.matter.service.MatterAppService.class));
        
        // Verify that deleting evidence throws BusinessException
        assertThatThrownBy(() -> service.deleteEvidence(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无法编辑证据");
    }

    /**
     * Property 7.5: canEditEvidence 方法应正确返回权限状态
     */
    @Property(tries = 100)
    void canEditEvidenceShouldReturnCorrectPermission(
            @ForAll("allStatuses") String status) {
        
        // Setup mocks
        MatterRepository matterRepository = mock(MatterRepository.class);
        
        Matter matter = new Matter();
        matter.setId(1L);
        matter.setStatus(status);
        when(matterRepository.findById(1L)).thenReturn(matter);
        
        EvidenceAppService service = new EvidenceAppService(
                mock(EvidenceRepository.class), 
                mock(EvidenceMapper.class), 
                mock(EvidenceCrossExamMapper.class), 
                new FileTypeService(), 
                matterRepository);
        service.setMatterAppService(mock(com.lawfirm.application.matter.service.MatterAppService.class));
        
        boolean canEdit = service.canEditEvidence(1L);
        
        if (READONLY_STATUSES.contains(status)) {
            assertThat(canEdit).isFalse();
        } else {
            assertThat(canEdit).isTrue();
        }
    }

    /**
     * Property 7.6: 批量分组操作对只读项目应被拒绝
     */
    @Property(tries = 100)
    void batchUpdateGroupShouldBeRejectedForReadonlyMatter(
            @ForAll("readonlyStatuses") String status,
            @ForAll("validGroupName") String groupName) {
        
        // Setup mocks
        EvidenceRepository evidenceRepository = mock(EvidenceRepository.class);
        MatterRepository matterRepository = mock(MatterRepository.class);
        
        Evidence evidence = Evidence.builder()
                .id(1L)
                .matterId(1L)
                .name("Test Evidence")
                .evidenceType("DOCUMENTARY")
                .build();
        when(evidenceRepository.findById(1L)).thenReturn(evidence);
        
        Matter matter = new Matter();
        matter.setId(1L);
        matter.setStatus(status);
        when(matterRepository.findById(1L)).thenReturn(matter);
        
        EvidenceAppService service = new EvidenceAppService(
                evidenceRepository, 
                mock(EvidenceMapper.class), 
                mock(EvidenceCrossExamMapper.class), 
                new FileTypeService(), 
                matterRepository);
        service.setMatterAppService(mock(com.lawfirm.application.matter.service.MatterAppService.class));
        
        // Verify that batch update throws BusinessException
        assertThatThrownBy(() -> service.batchUpdateGroup(List.of(1L), groupName))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无法编辑证据");
    }

    // ========== Helper Methods ==========

    private EvidenceAppService createServiceWithMockedMatter(Long matterId, String status) {
        EvidenceRepository evidenceRepository = mock(EvidenceRepository.class);
        MatterRepository matterRepository = mock(MatterRepository.class);
        EvidenceMapper evidenceMapper = mock(EvidenceMapper.class);
        EvidenceCrossExamMapper crossExamMapper = mock(EvidenceCrossExamMapper.class);
        FileTypeService fileTypeService = new FileTypeService();
        
        Matter matter = new Matter();
        matter.setId(matterId);
        matter.setStatus(status);
        when(matterRepository.findById(matterId)).thenReturn(matter);
        
        EvidenceAppService service = new EvidenceAppService(
                evidenceRepository, evidenceMapper, crossExamMapper, fileTypeService, matterRepository);
        service.setMatterAppService(mock(com.lawfirm.application.matter.service.MatterAppService.class));
        return service;
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<String> readonlyStatuses() {
        return Arbitraries.of(READONLY_STATUSES);
    }

    @Provide
    Arbitrary<String> editableStatuses() {
        return Arbitraries.of(EDITABLE_STATUSES);
    }

    @Provide
    Arbitrary<String> allStatuses() {
        return Arbitraries.of("DRAFT", "PENDING", "ACTIVE", "SUSPENDED", "ARCHIVED", "CLOSED");
    }

    @Provide
    Arbitrary<String> validEvidenceName() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(100);
    }

    @Provide
    Arbitrary<String> validGroupName() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(50);
    }
}
