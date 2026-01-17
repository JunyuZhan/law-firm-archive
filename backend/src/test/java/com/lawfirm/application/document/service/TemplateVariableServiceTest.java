package com.lawfirm.application.document.service;

import com.lawfirm.application.system.service.CauseOfActionService;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.MatterClient;
import com.lawfirm.domain.matter.entity.MatterParticipant;
import com.lawfirm.domain.matter.repository.MatterClientRepository;
import com.lawfirm.domain.matter.repository.MatterParticipantRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.ApprovalMapper;
import com.lawfirm.infrastructure.persistence.mapper.SysConfigMapper;
import com.lawfirm.infrastructure.security.LoginUser;
import com.lawfirm.common.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TemplateVariableService 单元测试
 *
 * 测试模板变量收集和替换功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateVariableService 模板变量服务测试")
class TemplateVariableServiceTest {

    @Mock
    private MatterRepository matterRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ApprovalMapper approvalMapper;

    @Mock
    private SysConfigMapper sysConfigMapper;

    @Mock
    private CauseOfActionService causeOfActionService;

    @Mock
    private MatterClientRepository matterClientRepository;

    @Mock
    private MatterParticipantRepository matterParticipantRepository;

    @InjectMocks
    private TemplateVariableService service;

    // ========== 测试数据 ==========

    private Matter testMatter;
    private Client testClient;
    private User testLawyer;
    private Contract testContract;
    private MatterClient testMatterClient;
    private MatterParticipant testParticipant;

    @BeforeEach
    void setUp() {
        // 设置测试用的案件
        testMatter = new Matter();
        testMatter.setId(1L);
        testMatter.setName("测试案件");
        testMatter.setMatterNo("MT2024001");
        testMatter.setStatus("ACTIVE");
        testMatter.setBusinessType("LITIGATION");
        testMatter.setCaseType("CIVIL");
        testMatter.setMatterType("LITIGATION");
        testMatter.setDescription("案件描述");
        testMatter.setOpposingParty("对方当事人");
        testMatter.setCauseOfAction("001");
        testMatter.setLitigationStage("FIRST_INSTANCE");
        testMatter.setFilingDate(LocalDate.of(2024, 1, 1));
        testMatter.setExpectedClosingDate(LocalDate.of(2024, 12, 31));
        testMatter.setClientId(100L);
        testMatter.setLeadLawyerId(200L);
        testMatter.setContractId(300L);

        // 设置测试用的客户
        testClient = new Client();
        testClient.setId(100L);
        testClient.setName("测试客户");
        testClient.setClientType("ENTERPRISE");
        testClient.setLegalRepresentative("法人代表");
        testClient.setRegisteredAddress("客户地址");
        testClient.setContactPhone("13800138000");
        testClient.setContactEmail("test@example.com");
        testClient.setCreditCode("91110000123456789X");

        // 设置测试用的律师
        testLawyer = new User();
        testLawyer.setId(200L);
        testLawyer.setRealName("张律师");
        testLawyer.setPhone("13900139000");
        testLawyer.setEmail("lawyer@example.com");
        testLawyer.setLawyerLicenseNo("1234567890");

        // 设置测试用的合同
        testContract = new Contract();
        testContract.setId(300L);
        testContract.setContractNo("CT2024001");
        testContract.setName("测试合同");
        testContract.setFeeType("FIXED");
        testContract.setPaymentTerms("一次性付款");
        testContract.setCaseType("CIVIL");
        testContract.setOpposingParty("对方当事人");
        testContract.setCauseOfAction("001");
        testContract.setTotalAmount(new BigDecimal("10000.00"));
        testContract.setSignDate(LocalDate.of(2024, 1, 1));
        testContract.setContractType("LITIGATION");
        testContract.setTrialStage("FIRST_TRIAL");

        // 设置测试用的案件客户关联
        testMatterClient = new MatterClient();
        testMatterClient.setMatterId(1L);
        testMatterClient.setClientId(100L);
        testMatterClient.setIsPrimary(true);

        // 设置测试用的参与人
        testParticipant = new MatterParticipant();
        testParticipant.setMatterId(1L);
        testParticipant.setUserId(200L);
        testParticipant.setRole("LEAD");
        testParticipant.setStatus("ACTIVE");

        // Mock 系统配置
        when(sysConfigMapper.selectValueByKey(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return switch (key) {
                case "firm.name" -> "测试律师事务所";
                case "firm.address" -> "律所地址";
                case "firm.phone" -> "010-12345678";
                case "firm.legalRep" -> "律所负责人";
                default -> null;
            };
        });

        // Mock 案由名称
        when(causeOfActionService.getCauseName(anyString(), anyString())).thenReturn("合同纠纷");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========== collectVariables 测试 ==========

    @Test
    @DisplayName("应该收集案件基本信息变量")
    void collectVariables_shouldCollectMatterVariables() {
        when(matterRepository.getByIdOrThrow(1L, anyString())).thenReturn(testMatter);
        when(matterClientRepository.findByMatterId(1L)).thenReturn(List.of());
        when(matterParticipantRepository.findByMatterId(1L)).thenReturn(List.of());

        Map<String, Object> variables = service.collectVariables(1L);

        assertThat(variables).isNotNull();
        assertThat(variables.get("matter.name")).isEqualTo("测试案件");
        assertThat(variables.get("matter.no")).isEqualTo("MT2024001");
        assertThat(variables.get("matter.status")).isEqualTo("ACTIVE");
        assertThat(variables.get("matter.caseType")).isEqualTo("CIVIL");
        assertThat(variables.get("matter.caseTypeName")).isEqualTo("民事案件");
        assertThat(variables.get("matter.matterTypeName")).isEqualTo("诉讼");
        assertThat(variables.get("matter.filingDate")).isEqualTo("2024年01月01日");
    }

    @Test
    @DisplayName("应该收集客户信息变量")
    void collectVariables_shouldCollectClientVariables() {
        when(matterRepository.getByIdOrThrow(1L, anyString())).thenReturn(testMatter);
        when(matterClientRepository.findByMatterId(1L)).thenReturn(List.of());
        when(clientRepository.findById(100L)).thenReturn(testClient);
        when(matterParticipantRepository.findByMatterId(1L)).thenReturn(List.of());

        Map<String, Object> variables = service.collectVariables(1L);

        assertThat(variables.get("client.name")).isEqualTo("测试客户");
        assertThat(variables.get("client.type")).isEqualTo("ENTERPRISE");
        assertThat(variables.get("client.typeName")).isEqualTo("企业客户");
        assertThat(variables.get("client.phone")).isEqualTo("13800138000");
        assertThat(variables.get("client.idLabel")).isEqualTo("统一社会信用代码");
        assertThat(variables.get("client.idNumber")).isEqualTo("91110000123456789X");
    }

    @Test
    @DisplayName("应该收集律师信息变量")
    void collectVariables_shouldCollectLawyerVariables() {
        when(matterRepository.getByIdOrThrow(1L, anyString())).thenReturn(testMatter);
        when(matterClientRepository.findByMatterId(1L)).thenReturn(List.of());
        when(matterParticipantRepository.findByMatterId(1L)).thenReturn(List.of());
        when(userRepository.findById(200L)).thenReturn(testLawyer);

        Map<String, Object> variables = service.collectVariables(1L);

        assertThat(variables.get("lawyer.name")).isEqualTo("张律师");
        assertThat(variables.get("lawyer.phone")).isEqualTo("13900139000");
        assertThat(variables.get("lawyer.email")).isEqualTo("lawyer@example.com");
        assertThat(variables.get("lawyer.licenseNo")).isEqualTo("1234567890");
    }

    @Test
    @DisplayName("应该收集合同信息变量")
    void collectVariables_shouldCollectContractVariables() {
        when(matterRepository.getByIdOrThrow(1L, anyString())).thenReturn(testMatter);
        when(matterClientRepository.findByMatterId(1L)).thenReturn(List.of());
        when(matterParticipantRepository.findByMatterId(1L)).thenReturn(List.of());
        when(contractRepository.getById(300L)).thenReturn(testContract);
        when(approvalMapper.selectByBusiness(eq("CONTRACT"), eq(300L))).thenReturn(List.of());

        Map<String, Object> variables = service.collectVariables(1L);

        assertThat(variables.get("contract.no")).isEqualTo("CT2024001");
        assertThat(variables.get("contract.name")).isEqualTo("测试合同");
        assertThat(variables.get("contract.feeTypeName")).isEqualTo("固定收费");
        assertThat(variables.get("contract.totalAmount")).isEqualTo("10000.00");
        assertThat(variables.get("contract.signDate")).isEqualTo("2024年01月01日");
    }

    @Test
    @DisplayName("应该收集日期变量")
    void collectVariables_shouldCollectDateVariables() {
        when(matterRepository.getByIdOrThrow(1L, anyString())).thenReturn(testMatter);
        when(matterClientRepository.findByMatterId(1L)).thenReturn(List.of());
        when(matterParticipantRepository.findByMatterId(1L)).thenReturn(List.of());

        // Fix the date to ensure consistent test
        LocalDate fixedDate = LocalDate.of(2024, 6, 15);
        try (var mockedLocalDate = mockStatic(LocalDate.class, MockitoExtension.CALLS_REAL_METHODS)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(fixedDate);

            Map<String, Object> variables = service.collectVariables(1L);

            assertThat(variables.get("date.today")).isEqualTo("2024年06月15日");
            assertThat(variables.get("date.todayShort")).isEqualTo("2024-06-15");
            assertThat(variables.get("date.year")).isEqualTo("2024");
            assertThat(variables.get("date.month")).isEqualTo("6");
            assertThat(variables.get("date.day")).isEqualTo("15");
        }
    }

    @Test
    @DisplayName("应该收集律所信息变量")
    void collectVariables_shouldCollectFirmVariables() {
        when(matterRepository.getByIdOrThrow(1L, anyString())).thenReturn(testMatter);
        when(matterClientRepository.findByMatterId(1L)).thenReturn(List.of());
        when(matterParticipantRepository.findByMatterId(1L)).thenReturn(List.of());

        Map<String, Object> variables = service.collectVariables(1L);

        assertThat(variables.get("firm.name")).isEqualTo("测试律师事务所");
        assertThat(variables.get("firm.address")).isEqualTo("律所地址");
        assertThat(variables.get("firm.phone")).isEqualTo("010-12345678");
        assertThat(variables.get("firm.legalRep")).isEqualTo("律所负责人");
    }

    @Test
    @DisplayName("应该处理多个委托人")
    void collectVariables_shouldHandleMultipleClients() {
        // 添加第二个客户
        Client client2 = new Client();
        client2.setId(101L);
        client2.setName("第二个客户");
        client2.setClientType("INDIVIDUAL");
        client2.setIdCard("110101199001011234");
        client2.setContactPhone("13900139001");
        client2.setRegisteredAddress("客户地址2");

        MatterClient mc2 = new MatterClient();
        mc2.setMatterId(1L);
        mc2.setClientId(101L);
        mc2.setIsPrimary(false);

        when(matterRepository.getByIdOrThrow(1L, anyString())).thenReturn(testMatter);
        when(matterClientRepository.findByMatterId(1L)).thenReturn(List.of(testMatterClient, mc2));
        when(clientRepository.findById(100L)).thenReturn(testClient);
        when(clientRepository.findById(101L)).thenReturn(client2);
        when(matterParticipantRepository.findByMatterId(1L)).thenReturn(List.of());

        Map<String, Object> variables = service.collectVariables(1L);

        assertThat(variables.get("clients.allNames")).isNotNull();
        String allNames = (String) variables.get("clients.allNames");
        assertThat(allNames).contains("测试客户");
        assertThat(allNames).contains("第二个客户");

        assertThat(variables.get("clients.allInfo")).isNotNull();
    }

    @Test
    @DisplayName("应该处理多个受托律师")
    void collectVariables_shouldHandleMultipleLawyers() {
        User lawyer2 = new User();
        lawyer2.setId(201L);
        lawyer2.setRealName("李律师");
        lawyer2.setLawyerLicenseNo("0987654321");

        MatterParticipant p2 = new MatterParticipant();
        p2.setMatterId(1L);
        p2.setUserId(201L);
        p2.setRole("CO_COUNSEL");
        p2.setStatus("ACTIVE");

        when(matterRepository.getByIdOrThrow(1L, anyString())).thenReturn(testMatter);
        when(matterClientRepository.findByMatterId(1L)).thenReturn(List.of());
        when(matterParticipantRepository.findByMatterId(1L)).thenReturn(List.of(testParticipant, p2));
        when(userRepository.findById(200L)).thenReturn(testLawyer);
        when(userRepository.findById(201L)).thenReturn(lawyer2);

        Map<String, Object> variables = service.collectVariables(1L);

        String allNames = (String) variables.get("lawyers.allNames");
        assertThat(allNames).contains("张律师");
        assertThat(allNames).contains("李律师");
    }

    // ========== replaceVariables 测试 ==========

    @Test
    @DisplayName("应该正确替换模板中的变量")
    void replaceVariables_shouldReplaceVariables() {
        Map<String, Object> variables = Map.of(
            "matter.name", "测试案件",
            "client.name", "测试客户",
            "lawyer.name", "张律师"
        );

        String template = "案件：${matter.name}，客户：${client.name}，律师：${lawyer.name}";
        String result = service.replaceVariables(template, variables);

        assertThat(result).isEqualTo("案件：测试案件，客户：测试客户，律师：张律师");
    }

    @Test
    @DisplayName("null 模板应该返回 null")
    void replaceVariables_shouldReturnNullForNullTemplate() {
        Map<String, Object> variables = Map.of("key", "value");

        String result = service.replaceVariables(null, variables);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("应该清理未替换的变量占位符")
    void replaceVariables_shouldCleanUnreplacedPlaceholders() {
        Map<String, Object> variables = Map.of("matter.name", "测试案件");

        String template = "案件：${matter.name}，未知：${unknown.key}";
        String result = service.replaceVariables(template, variables);

        assertThat(result).isEqualTo("案件：测试案件，未知：");
    }

    @Test
    @DisplayName("null 变量值应该替换为空字符串")
    void replaceVariables_shouldReplaceNullWithEmpty() {
        Map<String, Object> variables = Map.of(
            "matter.name", "测试案件",
            "matter.description", null
        );

        String template = "案件：${matter.name}，描述：${matter.description}";
        String result = service.replaceVariables(template, variables);

        assertThat(result).isEqualTo("案件：测试案件，描述：");
    }

    // ========== 边界情况测试 ==========

    @Test
    @DisplayName("案件不存在应该抛出异常")
    void collectVariables_shouldThrowExceptionWhenMatterNotFound() {
        when(matterRepository.getByIdOrThrow(1L, anyString()))
            .thenThrow(new RuntimeException("案件不存在"));

        org.junit.jupiter.api.Assertions.assertThrows(
            RuntimeException.class,
            () -> service.collectVariables(1L)
        );
    }

    @Test
    @DisplayName("应该处理空值的案件字段")
    void collectVariables_shouldHandleNullMatterFields() {
        Matter emptyMatter = new Matter();
        emptyMatter.setId(1L);
        emptyMatter.setName(null);
        emptyMatter.setCaseType(null);
        emptyMatter.setFilingDate(null);

        when(matterRepository.getByIdOrThrow(1L, anyString())).thenReturn(emptyMatter);
        when(matterClientRepository.findByMatterId(1L)).thenReturn(List.of());
        when(matterParticipantRepository.findByMatterId(1L)).thenReturn(List.of());

        Map<String, Object> variables = service.collectVariables(1L);

        assertThat(variables.get("matter.name")).isEqualTo("");
        assertThat(variables.get("matter.caseType")).isEqualTo("");
        assertThat(variables.get("matter.filingDate")).isEqualTo("");
    }

    @Test
    @DisplayName("应该处理不同案件类型名称")
    void collectVariables_shouldHandleDifferentCaseTypes() {
        testMatter.setCaseType("CRIMINAL");
        when(matterRepository.getByIdOrThrow(1L, anyString())).thenReturn(testMatter);
        when(matterClientRepository.findByMatterId(1L)).thenReturn(List.of());
        when(matterParticipantRepository.findByMatterId(1L)).thenReturn(List.of());

        Map<String, Object> variables = service.collectVariables(1L);

        assertThat(variables.get("matter.caseTypeName")).isEqualTo("刑事案件");
    }

    @Test
    @DisplayName("应该处理国家赔偿案件类型")
    void collectVariables_shouldHandleStateCompensationCaseTypes() {
        testMatter.setCaseType("STATE_COMP_ADMIN");
        when(matterRepository.getByIdOrThrow(1L, anyString())).thenReturn(testMatter);
        when(matterClientRepository.findByMatterId(1L)).thenReturn(List.of());
        when(matterParticipantRepository.findByMatterId(1L)).thenReturn(List.of());

        Map<String, Object> variables = service.collectVariables(1L);

        assertThat(variables.get("matter.caseTypeName")).isEqualTo("行政国家赔偿");
    }

    @Test
    @DisplayName("应该处理个人客户类型")
    void collectVariables_shouldHandleIndividualClient() {
        testClient.setClientType("INDIVIDUAL");
        testClient.setIdCard("110101199001011234");

        when(matterRepository.getByIdOrThrow(1L, anyString())).thenReturn(testMatter);
        when(matterClientRepository.findByMatterId(1L)).thenReturn(List.of());
        when(clientRepository.findById(100L)).thenReturn(testClient);
        when(matterParticipantRepository.findByMatterId(1L)).thenReturn(List.of());

        Map<String, Object> variables = service.collectVariables(1L);

        assertThat(variables.get("client.typeName")).isEqualTo("个人客户");
        assertThat(variables.get("client.idLabel")).isEqualTo("身份证号");
        assertThat(variables.get("client.idNumber")).isEqualTo("110101199001011234");
    }

    @Test
    @DisplayName("应该处理不同收费方式")
    void collectVariables_shouldHandleDifferentFeeTypes() {
        testContract.setFeeType("CONTINGENCY");

        when(matterRepository.getByIdOrThrow(1L, anyString())).thenReturn(testMatter);
        when(matterClientRepository.findByMatterId(1L)).thenReturn(List.of());
        when(matterParticipantRepository.findByMatterId(1L)).thenReturn(List.of());
        when(contractRepository.getById(300L)).thenReturn(testContract);
        when(approvalMapper.selectByBusiness(eq("CONTRACT"), eq(300L))).thenReturn(List.of());

        Map<String, Object> variables = service.collectVariables(1L);

        assertThat(variables.get("contract.feeTypeName")).isEqualTo("风险代理");
    }

    @Test
    @DisplayName("应该处理对方律师信息")
    void collectVariables_shouldHandleOpposingLawyer() {
        testMatter.setOpposingLawyerName("对方律师");
        testMatter.setOpposingLawyerLicenseNo("9876543210");
        testMatter.setOpposingLawyerFirm("对方律所");
        testMatter.setOpposingLawyerPhone("13700137000");
        testMatter.setOpposingLawyerEmail("opposing@example.com");

        when(matterRepository.getByIdOrThrow(1L, anyString())).thenReturn(testMatter);
        when(matterClientRepository.findByMatterId(1L)).thenReturn(List.of());
        when(matterParticipantRepository.findByMatterId(1L)).thenReturn(List.of());

        Map<String, Object> variables = service.collectVariables(1L);

        assertThat(variables.get("opposingLawyer.name")).isEqualTo("对方律师");
        assertThat(variables.get("opposingLawyer.licenseNo")).isEqualTo("9876543210");
        assertThat(variables.get("opposingLawyer.firm")).isEqualTo("对方律所");
    }
}
