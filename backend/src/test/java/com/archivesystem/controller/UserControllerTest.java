package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.entity.User;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRealName("测试用户");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setUserType("ADMIN");
        testUser.setDepartment("IT部门");
        testUser.setStatus(User.STATUS_ACTIVE);
    }

    @Test
    void testCreate_Success() throws Exception {
        UserController.CreateUserRequest request = new UserController.CreateUserRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setRealName("新用户");
        request.setEmail("new@example.com");
        request.setPhone("13900139000");
        request.setUserType("USER");
        request.setDepartment("销售部");

        when(userService.create(any(User.class))).thenReturn(testUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testUpdate_Success() throws Exception {
        UserController.UpdateUserRequest request = new UserController.UpdateUserRequest();
        request.setUsername("updateduser");
        request.setRealName("更新用户");
        request.setEmail("updated@example.com");

        when(userService.update(eq(1L), any(User.class))).thenReturn(testUser);

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testGetById_Success() throws Exception {
        when(userService.getById(1L)).thenReturn(testUser);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void testQuery_Success() throws Exception {
        PageResult<User> pageResult = PageResult.of(1L, 10L, 1L, Arrays.asList(testUser));

        when(userService.query(any(), any(), any(), eq(1), eq(10))).thenReturn(pageResult);

        mockMvc.perform(get("/users")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void testQuery_WithFilters() throws Exception {
        PageResult<User> pageResult = PageResult.of(1L, 10L, 1L, Arrays.asList(testUser));

        when(userService.query(eq("测试"), eq("ADMIN"), eq("ACTIVE"), eq(1), eq(10)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/users")
                        .param("keyword", "测试")
                        .param("userType", "ADMIN")
                        .param("status", "ACTIVE")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testDelete_Success() throws Exception {
        doNothing().when(userService).delete(1L);

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(userService).delete(1L);
    }

    @Test
    void testResetPassword_Success() throws Exception {
        UserController.ResetPasswordRequest request = new UserController.ResetPasswordRequest();
        request.setNewPassword("newpassword123");

        doNothing().when(userService).resetPassword(1L, "newpassword123");

        mockMvc.perform(put("/users/1/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testChangePassword_Success() throws Exception {
        UserController.ChangePasswordRequest request = new UserController.ChangePasswordRequest();
        request.setOldPassword("oldpassword");
        request.setNewPassword("newpassword123");

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            doNothing().when(userService).changePassword(1L, "oldpassword", "newpassword123");

            mockMvc.perform(put("/users/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"));
        }
    }

    @Test
    void testUpdateStatus_Success() throws Exception {
        UserController.UpdateStatusRequest request = new UserController.UpdateStatusRequest();
        request.setStatus("DISABLED");

        doNothing().when(userService).updateStatus(1L, "DISABLED");

        mockMvc.perform(put("/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void testAssignRoles_Success() throws Exception {
        UserController.AssignRolesRequest request = new UserController.AssignRolesRequest();
        request.setRoleIds(Arrays.asList(1L, 2L, 3L));

        doNothing().when(userService).assignRoles(1L, Arrays.asList(1L, 2L, 3L));

        mockMvc.perform(put("/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(userService).assignRoles(1L, Arrays.asList(1L, 2L, 3L));
    }

    @Test
    void testGetUserRoles_Success() throws Exception {
        List<Long> roleIds = Arrays.asList(1L, 2L);
        when(userService.getUserRoleIds(1L)).thenReturn(roleIds);

        mockMvc.perform(get("/users/1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0]").value(1))
                .andExpect(jsonPath("$.data[1]").value(2));
    }

    @Test
    void testGetCurrentUser_Success() throws Exception {
        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(userService.getById(1L)).thenReturn(testUser);

            mockMvc.perform(get("/users/current"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }
    }
}
