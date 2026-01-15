# 免费API集成设计方案

> 文档版本：v1.0  
> 创建日期：2026-01-14  
> 状态：设计中

## 一、概述

本方案设计两个免费API的集成：
1. **节假日API** - 用于工作日计算、诉讼时效计算
2. **企业微信机器人** - 用于消息推送，增强现有通知功能

---

## 二、节假日API集成

### 2.1 需求背景

律师工作中很多期限按**工作日**计算：
- 上诉期限（判决书送达后15日）
- 举证期限
- 答辩期限
- 申请执行期限

当前系统的日期计算没有排除节假日，可能导致**期限计算错误**。

### 2.2 API选择

推荐使用 **timor.tech** 节假日API：
- 完全免费，无需注册
- 数据准确，包含调休信息
- 稳定可靠，响应快

| API | 说明 | 示例 |
|-----|------|-----|
| 单日查询 | 查询某天是否节假日 | `/api/holiday/info/2026-01-14` |
| 批量查询 | 查询某年/某月节假日 | `/api/holiday/year/2026` |
| 工作日计算 | 计算N个工作日后的日期 | `/api/holiday/workday/2026-01-14/5` |

### 2.3 数据库设计

```sql
-- 节假日缓存表（减少API调用，支持离线）
CREATE TABLE public.sys_holiday_cache (
    id BIGSERIAL PRIMARY KEY,
    
    -- 日期信息
    date DATE NOT NULL UNIQUE,           -- 日期
    year INTEGER NOT NULL,               -- 年份
    month INTEGER NOT NULL,              -- 月份
    
    -- 类型信息
    day_type INTEGER NOT NULL,           -- 0=工作日, 1=周末, 2=法定节假日, 3=调休工作日
    day_type_name VARCHAR(20),           -- 类型名称
    
    -- 节假日信息
    holiday_name VARCHAR(50),            -- 节假日名称（如"春节"）
    is_off BOOLEAN NOT NULL,             -- 是否休息日（周末+节假日为true，调休工作日为false）
    
    -- 元数据
    data_source VARCHAR(20) DEFAULT 'TIMOR',
    fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_holiday_date ON public.sys_holiday_cache(date);
CREATE INDEX idx_holiday_year_month ON public.sys_holiday_cache(year, month);
CREATE INDEX idx_holiday_is_off ON public.sys_holiday_cache(is_off);

COMMENT ON TABLE public.sys_holiday_cache IS '节假日缓存表，存储节假日信息用于工作日计算';
COMMENT ON COLUMN public.sys_holiday_cache.day_type IS '日期类型：0=工作日,1=周末,2=法定节假日,3=调休工作日';
```

### 2.4 后端代码设计

#### 2.4.1 目录结构

```
backend/src/main/java/com/lawfirm/
├── infrastructure/
│   └── external/
│       └── holiday/
│           ├── HolidayClient.java           # API客户端
│           ├── HolidayService.java          # 节假日服务
│           └── dto/
│               └── HolidayInfo.java         # 节假日信息DTO
├── domain/
│   └── system/
│       └── entity/
│           └── HolidayCache.java            # 节假日缓存实体
└── interfaces/
    └── scheduler/
        └── HolidaySyncScheduler.java        # 节假日同步定时任务
```

#### 2.4.2 节假日API客户端

```java
package com.lawfirm.infrastructure.external.holiday;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 节假日API客户端
 * 
 * 使用 timor.tech 免费节假日API
 * 文档：https://timor.tech/api/holiday
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HolidayClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String BASE_URL = "https://timor.tech/api/holiday";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 查询单日节假日信息
     */
    public HolidayInfo getHolidayInfo(LocalDate date) {
        String url = BASE_URL + "/info/" + date.format(DATE_FORMATTER);
        
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            if (root.path("code").asInt() != 0) {
                log.warn("节假日API返回错误: {}", root.path("msg").asText());
                return null;
            }
            
            JsonNode type = root.path("type");
            JsonNode holiday = root.path("holiday");
            
            return HolidayInfo.builder()
                    .date(date)
                    .dayType(type.path("type").asInt())
                    .dayTypeName(type.path("name").asText())
                    .holidayName(holiday.isNull() ? null : holiday.path("name").asText())
                    .isOff(type.path("type").asInt() == 1 || type.path("type").asInt() == 2)
                    .build();
                    
        } catch (Exception e) {
            log.error("查询节假日信息失败: date={}", date, e);
            return null;
        }
    }

    /**
     * 批量查询某年的节假日
     */
    public List<HolidayInfo> getYearHolidays(int year) {
        String url = BASE_URL + "/year/" + year;
        List<HolidayInfo> holidays = new ArrayList<>();
        
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            if (root.path("code").asInt() != 0) {
                log.warn("查询年度节假日失败: {}", root.path("msg").asText());
                return holidays;
            }
            
            JsonNode holidayData = root.path("holiday");
            holidayData.fields().forEachRemaining(entry -> {
                String dateStr = entry.getKey();
                JsonNode info = entry.getValue();
                
                LocalDate date = LocalDate.parse(year + "-" + dateStr, 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                
                holidays.add(HolidayInfo.builder()
                        .date(date)
                        .dayType(info.path("holiday").asBoolean() ? 2 : 3)  // 节假日或调休
                        .dayTypeName(info.path("name").asText())
                        .holidayName(info.path("name").asText())
                        .isOff(info.path("holiday").asBoolean())
                        .build());
            });
            
            log.info("获取{}年节假日数据成功，共{}条", year, holidays.size());
            
        } catch (Exception e) {
            log.error("查询年度节假日失败: year={}", year, e);
        }
        
        return holidays;
    }

    /**
     * 计算N个工作日后的日期
     * 
     * @param startDate 起始日期
     * @param workdays 工作日数量（正数向后，负数向前）
     * @return 目标日期
     */
    public LocalDate calculateWorkday(LocalDate startDate, int workdays) {
        String url = BASE_URL + "/workday/" + startDate.format(DATE_FORMATTER) + "/" + workdays;
        
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            if (root.path("code").asInt() != 0) {
                log.warn("计算工作日失败: {}", root.path("msg").asText());
                return null;
            }
            
            String resultDate = root.path("result").path("date").asText();
            return LocalDate.parse(resultDate, DATE_FORMATTER);
            
        } catch (Exception e) {
            log.error("计算工作日失败: startDate={}, workdays={}", startDate, workdays, e);
            return null;
        }
    }
}
```

#### 2.4.3 节假日服务

```java
package com.lawfirm.infrastructure.external.holiday;

import com.lawfirm.domain.system.entity.HolidayCache;
import com.lawfirm.infrastructure.persistence.mapper.HolidayCacheMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 节假日服务
 * 
 * 提供工作日计算、节假日判断等功能
 * 优先使用本地缓存，缓存未命中时调用API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayClient holidayClient;
    private final HolidayCacheMapper holidayCacheMapper;

    /**
     * 判断是否为工作日
     */
    public boolean isWorkday(LocalDate date) {
        // 1. 查本地缓存
        HolidayCache cache = holidayCacheMapper.selectByDate(date);
        if (cache != null) {
            return !cache.getIsOff();
        }
        
        // 2. 调用API
        HolidayInfo info = holidayClient.getHolidayInfo(date);
        if (info != null) {
            // 保存到缓存
            saveToCache(info);
            return !info.isOff();
        }
        
        // 3. 兜底：周末为非工作日
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    /**
     * 判断是否为节假日（仅法定节假日，不含周末）
     */
    public boolean isHoliday(LocalDate date) {
        HolidayCache cache = holidayCacheMapper.selectByDate(date);
        if (cache != null) {
            return cache.getDayType() == 2;  // 2=法定节假日
        }
        
        HolidayInfo info = holidayClient.getHolidayInfo(date);
        if (info != null) {
            saveToCache(info);
            return info.getDayType() == 2;
        }
        
        return false;
    }

    /**
     * 计算N个工作日后的日期
     * 
     * @param startDate 起始日期
     * @param workdays 工作日数量（正数向后计算，负数向前计算）
     * @return 目标日期
     */
    public LocalDate addWorkdays(LocalDate startDate, int workdays) {
        // 先尝试使用API计算（更准确）
        LocalDate apiResult = holidayClient.calculateWorkday(startDate, workdays);
        if (apiResult != null) {
            return apiResult;
        }
        
        // 兜底：使用本地缓存计算
        return calculateWorkdaysLocal(startDate, workdays);
    }

    /**
     * 计算两个日期之间的工作日数量
     */
    public int countWorkdays(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            return -countWorkdays(endDate, startDate);
        }
        
        int count = 0;
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            if (isWorkday(current)) {
                count++;
            }
            current = current.plusDays(1);
        }
        
        return count;
    }

    /**
     * 计算诉讼期限截止日期
     * 
     * 根据《民事诉讼法》规定，期间以时计算的，从开始的次日起算
     * 
     * @param startDate 起算日期（如判决书送达日）
     * @param days 期限天数
     * @param excludeHolidays 是否排除节假日（工作日计算）
     * @return 截止日期
     */
    public LocalDate calculateDeadline(LocalDate startDate, int days, boolean excludeHolidays) {
        // 从次日起算
        LocalDate calcStart = startDate.plusDays(1);
        
        if (excludeHolidays) {
            // 按工作日计算
            return addWorkdays(calcStart, days - 1);
        } else {
            // 按自然日计算，但如果到期日是节假日则顺延
            LocalDate deadline = calcStart.plusDays(days - 1);
            while (!isWorkday(deadline)) {
                deadline = deadline.plusDays(1);
            }
            return deadline;
        }
    }

    /**
     * 本地计算工作日（使用缓存数据）
     */
    private LocalDate calculateWorkdaysLocal(LocalDate startDate, int workdays) {
        LocalDate current = startDate;
        int direction = workdays > 0 ? 1 : -1;
        int remaining = Math.abs(workdays);
        
        while (remaining > 0) {
            current = current.plusDays(direction);
            if (isWorkday(current)) {
                remaining--;
            }
        }
        
        return current;
    }

    /**
     * 保存到本地缓存
     */
    private void saveToCache(HolidayInfo info) {
        try {
            HolidayCache cache = new HolidayCache();
            cache.setDate(info.getDate());
            cache.setYear(info.getDate().getYear());
            cache.setMonth(info.getDate().getMonthValue());
            cache.setDayType(info.getDayType());
            cache.setDayTypeName(info.getDayTypeName());
            cache.setHolidayName(info.getHolidayName());
            cache.setIsOff(info.isOff());
            cache.setDataSource("TIMOR");
            
            holidayCacheMapper.insertOrUpdate(cache);
        } catch (Exception e) {
            log.warn("保存节假日缓存失败: date={}", info.getDate(), e);
        }
    }

    /**
     * 同步指定年份的节假日数据到本地缓存
     */
    public int syncYearHolidays(int year) {
        List<HolidayInfo> holidays = holidayClient.getYearHolidays(year);
        
        for (HolidayInfo info : holidays) {
            saveToCache(info);
        }
        
        log.info("同步{}年节假日数据完成，共{}条", year, holidays.size());
        return holidays.size();
    }
}
```

#### 2.4.4 节假日同步定时任务

```java
package com.lawfirm.interfaces.scheduler;

import com.lawfirm.infrastructure.external.holiday.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 节假日数据同步定时任务
 * 
 * 每年1月1日自动同步当年和下一年的节假日数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HolidaySyncScheduler {

    private final HolidayService holidayService;

    /**
     * 每年1月1日凌晨1点同步节假日数据
     */
    @Scheduled(cron = "0 0 1 1 1 ?")
    public void syncYearlyHolidays() {
        int currentYear = LocalDate.now().getYear();
        
        log.info("开始同步节假日数据: {}年、{}年", currentYear, currentYear + 1);
        
        try {
            int count1 = holidayService.syncYearHolidays(currentYear);
            int count2 = holidayService.syncYearHolidays(currentYear + 1);
            
            log.info("节假日数据同步完成: {}年{}条, {}年{}条", 
                    currentYear, count1, currentYear + 1, count2);
        } catch (Exception e) {
            log.error("节假日数据同步失败", e);
        }
    }

    /**
     * 每月1日检查并补充缺失数据
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void checkAndSyncMissingData() {
        int currentYear = LocalDate.now().getYear();
        
        log.debug("检查节假日数据完整性");
        
        try {
            // 确保当年数据完整
            holidayService.syncYearHolidays(currentYear);
        } catch (Exception e) {
            log.error("补充节假日数据失败", e);
        }
    }
}
```

#### 2.4.5 REST控制器

```java
package com.lawfirm.interfaces.rest.system;

import com.lawfirm.common.result.Result;
import com.lawfirm.infrastructure.external.holiday.HolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "节假日服务", description = "工作日计算、诉讼期限计算")
@RestController
@RequestMapping("/system/holiday")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @Operation(summary = "判断是否工作日")
    @GetMapping("/is-workday")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> isWorkday(
            @Parameter(description = "日期") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.success(holidayService.isWorkday(date));
    }

    @Operation(summary = "计算N个工作日后的日期")
    @GetMapping("/add-workdays")
    @PreAuthorize("isAuthenticated()")
    public Result<LocalDate> addWorkdays(
            @Parameter(description = "起始日期") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "工作日数量（正数向后，负数向前）") 
            @RequestParam int workdays) {
        return Result.success(holidayService.addWorkdays(startDate, workdays));
    }

    @Operation(summary = "计算诉讼期限截止日期", description = "根据起算日和期限天数计算截止日期，自动处理节假日顺延")
    @GetMapping("/deadline")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> calculateDeadline(
            @Parameter(description = "起算日期（如判决书送达日）") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "期限天数") 
            @RequestParam int days,
            @Parameter(description = "是否按工作日计算") 
            @RequestParam(defaultValue = "false") boolean workdaysOnly) {
        
        LocalDate deadline = holidayService.calculateDeadline(startDate, days, workdaysOnly);
        
        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("days", days);
        result.put("workdaysOnly", workdaysOnly);
        result.put("deadline", deadline);
        result.put("isWorkday", holidayService.isWorkday(deadline));
        
        return Result.success(result);
    }

    @Operation(summary = "计算两个日期间的工作日数量")
    @GetMapping("/count-workdays")
    @PreAuthorize("isAuthenticated()")
    public Result<Integer> countWorkdays(
            @Parameter(description = "开始日期") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(holidayService.countWorkdays(startDate, endDate));
    }

    @Operation(summary = "同步节假日数据", description = "管理员手动触发同步指定年份的节假日数据")
    @PostMapping("/sync/{year}")
    @PreAuthorize("hasAuthority('system:config:manage')")
    public Result<Integer> syncHolidays(@PathVariable int year) {
        int count = holidayService.syncYearHolidays(year);
        return Result.success(count);
    }
}
```

### 2.5 前端集成

#### 2.5.1 API定义

```typescript
// frontend/apps/web-antd/src/api/system/holiday.ts

import { requestClient } from '#/api/request';

/** 判断是否工作日 */
export function isWorkday(date: string) {
  return requestClient.get<boolean>('/system/holiday/is-workday', {
    params: { date },
  });
}

/** 计算N个工作日后的日期 */
export function addWorkdays(startDate: string, workdays: number) {
  return requestClient.get<string>('/system/holiday/add-workdays', {
    params: { startDate, workdays },
  });
}

/** 计算诉讼期限截止日期 */
export function calculateDeadline(
  startDate: string,
  days: number,
  workdaysOnly = false,
) {
  return requestClient.get<{
    startDate: string;
    days: number;
    workdaysOnly: boolean;
    deadline: string;
    isWorkday: boolean;
  }>('/system/holiday/deadline', {
    params: { startDate, days, workdaysOnly },
  });
}

/** 计算两个日期间的工作日数量 */
export function countWorkdays(startDate: string, endDate: string) {
  return requestClient.get<number>('/system/holiday/count-workdays', {
    params: { startDate, endDate },
  });
}
```

#### 2.5.2 期限计算组件

```vue
<!-- frontend/apps/web-antd/src/components/DeadlineCalculator/index.vue -->
<template>
  <Card title="诉讼期限计算器" size="small">
    <Form layout="inline" :model="form">
      <FormItem label="起算日期">
        <DatePicker v-model:value="form.startDate" />
      </FormItem>
      <FormItem label="期限天数">
        <InputNumber v-model:value="form.days" :min="1" :max="365" />
      </FormItem>
      <FormItem label="计算方式">
        <RadioGroup v-model:value="form.workdaysOnly">
          <Radio :value="false">自然日（节假日顺延）</Radio>
          <Radio :value="true">工作日</Radio>
        </RadioGroup>
      </FormItem>
      <FormItem>
        <Button type="primary" @click="calculate" :loading="loading">
          计算
        </Button>
      </FormItem>
    </Form>

    <Alert v-if="result" type="info" style="margin-top: 16px" show-icon>
      <template #message>
        <Space direction="vertical">
          <span>
            <strong>截止日期：</strong>
            <Tag color="blue">{{ result.deadline }}</Tag>
            <Tag v-if="result.isWorkday" color="green">工作日</Tag>
            <Tag v-else color="orange">非工作日</Tag>
          </span>
          <span class="text-gray-500">
            从 {{ result.startDate }} 起算 {{ result.days }} 天
            （{{ result.workdaysOnly ? '按工作日' : '按自然日，节假日顺延' }}）
          </span>
        </Space>
      </template>
    </Alert>
  </Card>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { Card, Form, FormItem, DatePicker, InputNumber, RadioGroup, Radio, Button, Alert, Tag, Space } from 'ant-design-vue';
import dayjs from 'dayjs';
import { calculateDeadline } from '#/api/system/holiday';

const form = reactive({
  startDate: dayjs(),
  days: 15,
  workdaysOnly: false,
});

const loading = ref(false);
const result = ref<any>(null);

async function calculate() {
  if (!form.startDate || !form.days) return;
  
  loading.value = true;
  try {
    result.value = await calculateDeadline(
      form.startDate.format('YYYY-MM-DD'),
      form.days,
      form.workdaysOnly,
    );
  } finally {
    loading.value = false;
  }
}
</script>
```

---

## 三、企业微信机器人集成

### 3.1 需求背景

当前系统的通知只保存在数据库中，用户需要登录系统才能看到。通过企业微信机器人推送，可以实现**即时触达**。

### 3.2 推送场景

| 场景 | 触发条件 | 消息内容 |
|------|---------|---------|
| **开庭提醒** | 开庭前1天/3天 | 开庭时间、法院、案号 |
| **任务到期** | 到期前3天/当天 | 任务名称、截止日期 |
| **任务逾期** | 已超过截止日期 | 任务名称、逾期天数 |
| **合同到期** | 到期前30天/7天 | 合同名称、到期日期、金额 |
| **审批待办** | 有新审批待处理 | 审批类型、申请人 |
| **利冲审批结果** | 利冲检查完成 | 审批结果、备注 |

### 3.3 数据库设计

```sql
-- 在 sys_external_integration 表中添加企业微信机器人配置
INSERT INTO public.sys_external_integration (
    integration_code,
    integration_name,
    integration_type,
    description,
    api_url,
    auth_type,
    extra_config,
    enabled
) VALUES (
    'WECOM_BOT',
    '企业微信机器人',
    'NOTIFICATION',
    '企业微信群机器人，用于推送系统通知到企业微信群',
    '',  -- webhook地址在 api_key 中配置
    'WEBHOOK',
    '{
        "mentionAll": false,
        "enabledTypes": ["TASK", "SCHEDULE", "APPROVAL", "CONTRACT"]
    }',
    false
);

-- 用户企业微信绑定表（用于@指定人）
CREATE TABLE public.sys_user_wecom (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,      -- 系统用户ID
    wecom_userid VARCHAR(100),           -- 企业微信UserId（用于@）
    wecom_mobile VARCHAR(20),            -- 企业微信绑定手机号（备用）
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_user_wecom_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

COMMENT ON TABLE public.sys_user_wecom IS '用户企业微信绑定表，用于消息推送时@指定用户';
```

### 3.4 后端代码设计

#### 3.4.1 目录结构

```
backend/src/main/java/com/lawfirm/
├── infrastructure/
│   └── notification/
│       ├── NotificationChannel.java         # 通知渠道接口
│       ├── DatabaseNotificationChannel.java # 数据库通知（现有）
│       ├── WecomBotNotificationChannel.java # 企业微信机器人
│       └── WecomBotClient.java              # 企业微信机器人客户端
├── domain/
│   └── system/
│       └── entity/
│           └── UserWecom.java               # 用户企业微信绑定实体
└── application/
    └── system/
        └── service/
            └── NotificationAppService.java  # 增强通知服务
```

#### 3.4.2 集成类型常量扩展

```java
// 在 ExternalIntegration.java 中添加
public static final String TYPE_NOTIFICATION = "NOTIFICATION";
```

#### 3.4.3 企业微信机器人客户端

```java
package com.lawfirm.infrastructure.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 企业微信机器人客户端
 * 
 * 文档：https://developer.work.weixin.qq.com/document/path/91770
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WecomBotClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 发送文本消息
     * 
     * @param webhookUrl Webhook地址
     * @param content 消息内容
     * @param mentionedList 需要@的用户ID列表（企业微信UserId）
     * @param mentionedMobileList 需要@的手机号列表
     */
    public boolean sendText(String webhookUrl, String content, 
                           List<String> mentionedList, List<String> mentionedMobileList) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "text");
            
            Map<String, Object> text = new HashMap<>();
            text.put("content", content);
            
            if (mentionedList != null && !mentionedList.isEmpty()) {
                text.put("mentioned_list", mentionedList);
            }
            if (mentionedMobileList != null && !mentionedMobileList.isEmpty()) {
                text.put("mentioned_mobile_list", mentionedMobileList);
            }
            
            body.put("text", text);
            
            return sendRequest(webhookUrl, body);
            
        } catch (Exception e) {
            log.error("发送企业微信文本消息失败", e);
            return false;
        }
    }

    /**
     * 发送Markdown消息
     */
    public boolean sendMarkdown(String webhookUrl, String content) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "markdown");
            
            Map<String, Object> markdown = new HashMap<>();
            markdown.put("content", content);
            
            body.put("markdown", markdown);
            
            return sendRequest(webhookUrl, body);
            
        } catch (Exception e) {
            log.error("发送企业微信Markdown消息失败", e);
            return false;
        }
    }

    /**
     * 发送卡片消息（模板卡片）
     */
    public boolean sendCard(String webhookUrl, String title, String description, 
                           String url, String btnText) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "template_card");
            
            Map<String, Object> card = new HashMap<>();
            card.put("card_type", "text_notice");
            
            Map<String, Object> source = new HashMap<>();
            source.put("desc", "律所管理系统");
            card.put("source", source);
            
            Map<String, Object> mainTitle = new HashMap<>();
            mainTitle.put("title", title);
            mainTitle.put("desc", description);
            card.put("main_title", mainTitle);
            
            Map<String, Object> cardAction = new HashMap<>();
            cardAction.put("type", 1);
            cardAction.put("url", url);
            card.put("card_action", cardAction);
            
            body.put("template_card", card);
            
            return sendRequest(webhookUrl, body);
            
        } catch (Exception e) {
            log.error("发送企业微信卡片消息失败", e);
            return false;
        }
    }

    private boolean sendRequest(String webhookUrl, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
                int errcode = (Integer) result.getOrDefault("errcode", -1);
                if (errcode == 0) {
                    log.debug("企业微信消息发送成功");
                    return true;
                } else {
                    log.warn("企业微信消息发送失败: errcode={}, errmsg={}", 
                            errcode, result.get("errmsg"));
                }
            }
        } catch (Exception e) {
            log.error("发送企业微信消息请求失败", e);
        }
        return false;
    }
}
```

#### 3.4.4 企业微信通知渠道

```java
package com.lawfirm.infrastructure.notification;

import com.lawfirm.application.system.service.ExternalIntegrationAppService;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.domain.system.entity.Notification;
import com.lawfirm.domain.system.entity.UserWecom;
import com.lawfirm.infrastructure.persistence.mapper.UserWecomMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 企业微信机器人通知渠道
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WecomBotNotificationChannel {

    private final WecomBotClient wecomBotClient;
    private final ExternalIntegrationAppService integrationService;
    private final UserWecomMapper userWecomMapper;

    /**
     * 异步发送通知到企业微信
     */
    @Async
    public void sendNotification(Notification notification) {
        try {
            // 1. 获取企业微信机器人配置
            ExternalIntegration config = integrationService.getFirstEnabledIntegrationByType(
                    ExternalIntegration.TYPE_NOTIFICATION
            );
            
            if (config == null || !config.getEnabled()) {
                log.debug("企业微信机器人未配置或未启用，跳过推送");
                return;
            }
            
            // 2. 检查通知类型是否需要推送
            if (!shouldPush(config, notification.getType())) {
                log.debug("通知类型{}不在推送范围内，跳过", notification.getType());
                return;
            }
            
            // 3. 获取Webhook地址
            String webhookUrl = config.getApiKey();
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                log.warn("企业微信机器人Webhook地址未配置");
                return;
            }
            
            // 4. 获取接收者的企业微信ID
            List<String> mentionedList = new ArrayList<>();
            if (notification.getReceiverId() != null) {
                UserWecom userWecom = userWecomMapper.selectByUserId(notification.getReceiverId());
                if (userWecom != null && userWecom.getWecomUserid() != null) {
                    mentionedList.add(userWecom.getWecomUserid());
                }
            }
            
            // 5. 构建消息内容
            String content = buildMessageContent(notification, mentionedList);
            
            // 6. 发送消息
            boolean success = wecomBotClient.sendText(webhookUrl, content, mentionedList, null);
            
            if (success) {
                log.info("企业微信通知发送成功: title={}, receiver={}", 
                        notification.getTitle(), notification.getReceiverId());
            }
            
        } catch (Exception e) {
            log.error("发送企业微信通知失败: notificationId={}", notification.getId(), e);
        }
    }

    /**
     * 批量发送通知
     */
    @Async
    public void sendNotifications(List<Notification> notifications) {
        for (Notification notification : notifications) {
            sendNotification(notification);
        }
    }

    /**
     * 发送紧急通知（Markdown格式，更醒目）
     */
    @Async
    public void sendUrgentNotification(String title, String content, List<Long> receiverIds) {
        try {
            ExternalIntegration config = integrationService.getFirstEnabledIntegrationByType(
                    ExternalIntegration.TYPE_NOTIFICATION
            );
            
            if (config == null || !config.getEnabled()) {
                return;
            }
            
            String webhookUrl = config.getApiKey();
            
            // 构建Markdown内容
            StringBuilder markdown = new StringBuilder();
            markdown.append("## ⚠️ ").append(title).append("\n\n");
            markdown.append(content).append("\n\n");
            markdown.append("> 请及时处理！");
            
            wecomBotClient.sendMarkdown(webhookUrl, markdown.toString());
            
        } catch (Exception e) {
            log.error("发送紧急通知失败", e);
        }
    }

    /**
     * 检查是否应该推送该类型通知
     */
    private boolean shouldPush(ExternalIntegration config, String notificationType) {
        if (config.getExtraConfig() == null) {
            return true;  // 默认全部推送
        }
        
        Object enabledTypes = config.getExtraConfig().get("enabledTypes");
        if (enabledTypes instanceof List) {
            return ((List<?>) enabledTypes).contains(notificationType);
        }
        
        return true;
    }

    /**
     * 构建消息内容
     */
    private String buildMessageContent(Notification notification, List<String> mentionedList) {
        StringBuilder content = new StringBuilder();
        
        // 添加图标
        String icon = getTypeIcon(notification.getType());
        content.append(icon).append(" ");
        
        // 标题
        content.append("【").append(notification.getTitle()).append("】\n");
        
        // 内容
        content.append(notification.getContent());
        
        return content.toString();
    }

    /**
     * 获取通知类型图标
     */
    private String getTypeIcon(String type) {
        return switch (type) {
            case "TASK" -> "📋";
            case "SCHEDULE" -> "📅";
            case "APPROVAL" -> "✅";
            case "REMINDER" -> "⏰";
            case "WARNING" -> "⚠️";
            case "CONTRACT" -> "📄";
            default -> "📢";
        };
    }
}
```

#### 3.4.5 增强通知服务

修改现有的 `NotificationAppService`，在发送通知时同时推送到企业微信：

```java
// 在 NotificationAppService.java 中添加

@Autowired
private WecomBotNotificationChannel wecomChannel;

/**
 * 发送通知（增强版：同时推送到企业微信）
 */
@Transactional
public void sendNotificationWithPush(SendNotificationCommand command) {
    // 1. 原有逻辑：保存到数据库
    sendNotification(command);
    
    // 2. 新增：推送到企业微信
    for (Long receiverId : command.getReceiverIds()) {
        Notification notification = Notification.builder()
                .title(command.getTitle())
                .content(command.getContent())
                .type(command.getType())
                .receiverId(receiverId)
                .businessType(command.getBusinessType())
                .businessId(command.getBusinessId())
                .build();
        
        wecomChannel.sendNotification(notification);
    }
}

/**
 * 发送系统通知（增强版）
 */
public void sendSystemNotificationWithPush(Long receiverId, String title, String content,
                                           String businessType, Long businessId) {
    // 保存到数据库
    sendSystemNotification(receiverId, title, content, businessType, businessId);
    
    // 推送到企业微信
    Notification notification = Notification.builder()
            .title(title)
            .content(content)
            .type(Notification.TYPE_SYSTEM)
            .receiverId(receiverId)
            .businessType(businessType)
            .businessId(businessId)
            .build();
    
    wecomChannel.sendNotification(notification);
}
```

### 3.5 前端配置界面

在系统设置-外部集成页面添加企业微信机器人配置：

```vue
<!-- 企业微信机器人配置表单 -->
<template>
  <Form :model="form" :rules="rules" ref="formRef">
    <FormItem label="Webhook地址" name="apiKey" required>
      <Input 
        v-model:value="form.apiKey" 
        placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx"
      />
      <div class="text-gray-500 text-sm mt-1">
        在企业微信群中添加机器人后获取
      </div>
    </FormItem>
    
    <FormItem label="推送通知类型">
      <CheckboxGroup v-model:value="form.extraConfig.enabledTypes">
        <Checkbox value="TASK">任务提醒</Checkbox>
        <Checkbox value="SCHEDULE">日程提醒</Checkbox>
        <Checkbox value="APPROVAL">审批通知</Checkbox>
        <Checkbox value="CONTRACT">合同到期</Checkbox>
        <Checkbox value="WARNING">警告通知</Checkbox>
      </CheckboxGroup>
    </FormItem>
    
    <FormItem>
      <Space>
        <Button type="primary" @click="testConnection">
          测试发送
        </Button>
        <Button @click="save">保存配置</Button>
      </Space>
    </FormItem>
  </Form>
</template>
```

---

## 四、集成效果

### 4.1 节假日API效果

1. **期限计算准确**：系统自动排除节假日，计算出正确的截止日期
2. **提醒时间合理**：提前N个**工作日**提醒，避免节假日期间打扰
3. **离线可用**：数据缓存到本地，断网也能计算

### 4.2 企业微信推送效果

消息样式示例：

```
📋 【任务到期提醒】
任务"准备XX案开庭材料"将于3天后到期
截止日期：2026-01-17
@张律师

---

📅 【开庭提醒】
您明天9:00有开庭安排
案号：（2026）京01民初123号
法院：北京市第一中级人民法院
地点：第5审判庭
@李律师

---

⚠️ 【合同到期预警】
## ⚠️ 合同即将到期

客户"XX科技有限公司"的委托代理合同将于7天后到期
合同金额：50,000元
到期日期：2026-01-21

> 请及时跟进续约事宜！
```

---

## 五、实施步骤

### 第一阶段：节假日API（1-2天）
1. 创建数据库表
2. 实现 HolidayClient 和 HolidayService
3. 添加定时同步任务
4. 添加REST接口
5. 前端添加期限计算器组件

### 第二阶段：企业微信机器人（2-3天）
1. 添加集成类型常量
2. 创建用户企业微信绑定表
3. 实现 WecomBotClient
4. 实现 WecomBotNotificationChannel
5. 修改现有调度器，增加企业微信推送
6. 前端添加配置界面

---

## 六、注意事项

### 6.1 节假日API
- timor.tech API 无请求频率限制，但建议缓存数据
- 每年年初需要同步新年份的数据
- 调休信息可能在节前更新，建议定期同步

### 6.2 企业微信机器人
- Webhook地址不要泄露，建议加密存储
- 同一机器人每分钟最多发送20条消息
- @人功能需要企业微信的UserId，需用户自行绑定
- 建议只推送重要通知，避免消息轰炸
