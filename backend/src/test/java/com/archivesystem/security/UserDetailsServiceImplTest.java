package com.archivesystem.security;

import com.archivesystem.entity.User;
import com.archivesystem.repository.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setRealName("测试用户");
        testUser.setUserType("ADMIN");
        testUser.setStatus(User.STATUS_ACTIVE);
    }

    @Test
    void testLoadUserByUsername_UserExists() {
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);

        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertTrue(userDetails instanceof UserDetailsImpl);

        UserDetailsImpl impl = (UserDetailsImpl) userDetails;
        assertEquals(1L, impl.getId());
        assertEquals("测试用户", impl.getRealName());
        assertEquals("SYSTEM_ADMIN", impl.getUserType());
    }

    @Test
    void testLoadUserByUsername_UserNotFound() {
        when(userMapper.selectByUsername("nonexistent")).thenReturn(null);

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("nonexistent")
        );

        assertTrue(exception.getMessage().contains("用户不存在"));
        assertTrue(exception.getMessage().contains("nonexistent"));
    }

    @Test
    void testLoadUserByUsername_VerifyMapperCall() {
        when(userMapper.selectByUsername("admin")).thenReturn(testUser);

        userDetailsService.loadUserByUsername("admin");

        verify(userMapper).selectByUsername("admin");
    }

    @Test
    void testLoadUserByUsername_DisabledUser() {
        testUser.setStatus(User.STATUS_DISABLED);
        when(userMapper.selectByUsername("disabled")).thenReturn(testUser);

        UserDetails userDetails = userDetailsService.loadUserByUsername("disabled");

        assertNotNull(userDetails);
        // UserDetailsImpl.build should handle disabled status
    }

    @Test
    void testLoadUserByUsername_SystemAdmin() {
        testUser.setUserType("SYSTEM_ADMIN");
        when(userMapper.selectByUsername("sysadmin")).thenReturn(testUser);

        UserDetails userDetails = userDetailsService.loadUserByUsername("sysadmin");

        assertNotNull(userDetails);
        UserDetailsImpl impl = (UserDetailsImpl) userDetails;
        assertEquals("SYSTEM_ADMIN", impl.getUserType());
    }
}
