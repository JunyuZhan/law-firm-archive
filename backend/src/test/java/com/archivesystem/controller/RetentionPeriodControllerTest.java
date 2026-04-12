package com.archivesystem.controller;

import com.archivesystem.entity.RetentionPeriod;
import com.archivesystem.service.RetentionPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class RetentionPeriodControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RetentionPeriodService retentionPeriodService;

    @InjectMocks
    private RetentionPeriodController retentionPeriodController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(retentionPeriodController).build();
    }

    @Test
    void testList_Success() throws Exception {
        RetentionPeriod period = RetentionPeriod.builder()
                .id(1L)
                .periodCode(RetentionPeriod.Y10)
                .periodName("10年")
                .sortOrder(10)
                .build();
        when(retentionPeriodService.listAll()).thenReturn(List.of(period));

        mockMvc.perform(get("/retention-periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].periodCode").value("Y10"))
                .andExpect(jsonPath("$.data[0].periodName").value("10年"));
    }
}
