package com.archivesystem.controller;

import com.archivesystem.entity.Role;
import com.archivesystem.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleController roleController;

    private ObjectMapper objectMapper;
    private Role testRole;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roleController).build();
        objectMapper = new ObjectMapper();

        testRole = new Role();
        testRole.setId(1L);
        testRole.setRoleCode("ADMIN");
        testRole.setRoleName("管理员");
        testRole.setDescription("系统管理员角色");
        testRole.setStatus("ACTIVE");
        testRole.setDeleted(true);
    }

    @Test
    void testCreate_Success() throws Exception {
        RoleController.CreateRoleRequest request = new RoleController.CreateRoleRequest();
        request.setRoleCode("USER");
        request.setRoleName("普通用户");
        request.setDescription("普通用户角色");

        when(roleService.create(any(Role.class))).thenReturn(testRole);

        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.roleCode").value("ADMIN"))
                .andExpect(jsonPath("$.data.deleted").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist());
    }

    @Test
    void testUpdate_Success() throws Exception {
        RoleController.UpdateRoleRequest request = new RoleController.UpdateRoleRequest();
        request.setRoleCode("ADMIN");
        request.setRoleName("超级管理员");
        request.setDescription("更新后的描述");
        request.setStatus("ACTIVE");

        when(roleService.update(eq(1L), any(Role.class))).thenReturn(testRole);

        mockMvc.perform(put("/roles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testGetById_Success() throws Exception {
        when(roleService.getById(1L)).thenReturn(testRole);

        mockMvc.perform(get("/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.roleCode").value("ADMIN"))
                .andExpect(jsonPath("$.data.roleName").value("管理员"))
                .andExpect(jsonPath("$.data.deleted").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist());
    }

    @Test
    void testList_Success() throws Exception {
        Role role2 = new Role();
        role2.setId(2L);
        role2.setRoleCode("USER");
        role2.setRoleName("普通用户");
        role2.setStatus("ACTIVE");

        when(roleService.list()).thenReturn(Arrays.asList(testRole, role2));

        mockMvc.perform(get("/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].roleCode").value("ADMIN"))
                .andExpect(jsonPath("$.data[1].roleCode").value("USER"))
                .andExpect(jsonPath("$.data[0].deleted").doesNotExist())
                .andExpect(jsonPath("$.data[0].updatedAt").doesNotExist());
    }

    @Test
    void testListOptions_Success() throws Exception {
        when(roleService.list()).thenReturn(Arrays.asList(testRole));

        mockMvc.perform(get("/roles/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].roleName").value("管理员"))
                .andExpect(jsonPath("$.data[0].roleCode").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdAt").doesNotExist());
    }

    @Test
    void testDelete_Success() throws Exception {
        doNothing().when(roleService).delete(1L);

        mockMvc.perform(delete("/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(roleService).delete(1L);
    }
}
