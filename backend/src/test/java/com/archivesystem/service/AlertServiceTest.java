package com.archivesystem.service;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.config.SystemMailSenderFactory;
import com.archivesystem.dto.alert.AlertMessage;
import com.archivesystem.entity.User;
import com.archivesystem.repository.UserMapper;
import com.archivesystem.service.impl.AlertServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ConfigService configService;

    @Mock
    private SystemMailSenderFactory systemMailSenderFactory;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JavaMailSender javaMailSender;

    private AlertServiceImpl alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertServiceImpl(
                new ObjectMapper(),
                restTemplate,
                configService,
                systemMailSenderFactory,
                userMapper
        );
        ReflectionTestUtils.setField(alertService, "envEmailEnabled", true);
        ReflectionTestUtils.setField(alertService, "envEmailFrom", "noreply@example.com");
        ReflectionTestUtils.setField(alertService, "envEmailTo", "valid1@example.com, bad-address ");
        ReflectionTestUtils.setField(alertService, "dingtalkEnabled", false);
    }

    @Test
    void testSend_ShouldIgnoreInvalidRecipientAddresses() {
        User admin1 = new User();
        admin1.setStatus(User.STATUS_ACTIVE);
        admin1.setUserType(User.TYPE_SYSTEM_ADMIN);
        admin1.setEmail("admin@example.com");

        User admin2 = new User();
        admin2.setStatus(User.STATUS_ACTIVE);
        admin2.setUserType(User.TYPE_SECURITY_ADMIN);
        admin2.setEmail("invalid-admin-email");

        when(configService.getValue("system.notify.admin.emails"))
                .thenReturn(" extra@example.com ,invalid-extra");
        when(userMapper.selectList(any())).thenReturn(List.of(admin1, admin2));
        when(systemMailSenderFactory.createFromDatabaseConfig()).thenReturn(javaMailSender);
        when(systemMailSenderFactory.resolveFromAddress()).thenReturn("noreply@example.com");

        AlertMessage message = AlertMessage.builder()
                .title("测试告警")
                .content("测试内容")
                .level(AlertMessage.Level.WARNING)
                .type(AlertMessage.Type.SYSTEM_EVENT)
                .createdAt(LocalDateTime.now())
                .build();

        alertService.send(message);

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(mailCaptor.capture());
        assertArrayEquals(
                new String[]{"valid1@example.com", "extra@example.com", "admin@example.com"},
                mailCaptor.getValue().getTo()
        );
    }

    @Test
    void testSendTestMail_InvalidOverrideRecipient_ShouldRejectEarly() {
        assertThrows(BusinessException.class, () -> alertService.sendTestMail("not-an-email"));

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
        verify(systemMailSenderFactory, never()).resolveFromAddress();
    }

    @Test
    void testSend_DingtalkWebhookWithoutQuery_ShouldAppendQuestionMark() {
        ReflectionTestUtils.setField(alertService, "envEmailEnabled", false);
        ReflectionTestUtils.setField(alertService, "dingtalkEnabled", true);
        ReflectionTestUtils.setField(alertService, "dingtalkWebhook", "https://oapi.dingtalk.com/robot/send");
        ReflectionTestUtils.setField(alertService, "dingtalkSecret", "test-secret");

        AlertMessage message = AlertMessage.builder()
                .title("测试钉钉")
                .content("测试内容")
                .level(AlertMessage.Level.INFO)
                .type(AlertMessage.Type.SYSTEM_EVENT)
                .createdAt(LocalDateTime.now())
                .build();

        alertService.send(message);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).postForObject(urlCaptor.capture(), any(), eq(String.class));
        String url = urlCaptor.getValue();
        assertTrue(url.startsWith("https://oapi.dingtalk.com/robot/send?timestamp="));
        assertTrue(url.contains("&sign="));
    }

    @Test
    void testSend_DingtalkWebhookWithQuery_ShouldAppendAmpersand() {
        ReflectionTestUtils.setField(alertService, "envEmailEnabled", false);
        ReflectionTestUtils.setField(alertService, "dingtalkEnabled", true);
        ReflectionTestUtils.setField(alertService, "dingtalkWebhook", "https://oapi.dingtalk.com/robot/send?access_token=abc");
        ReflectionTestUtils.setField(alertService, "dingtalkSecret", "test-secret");

        AlertMessage message = AlertMessage.builder()
                .title("测试钉钉")
                .content("测试内容")
                .level(AlertMessage.Level.INFO)
                .type(AlertMessage.Type.SYSTEM_EVENT)
                .createdAt(LocalDateTime.now())
                .build();

        alertService.send(message);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).postForObject(urlCaptor.capture(), any(), eq(String.class));
        String url = urlCaptor.getValue();
        assertTrue(url.startsWith("https://oapi.dingtalk.com/robot/send?access_token=abc&timestamp="));
        assertTrue(url.contains("&sign="));
    }
}
