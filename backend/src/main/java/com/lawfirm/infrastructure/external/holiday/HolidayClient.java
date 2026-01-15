package com.lawfirm.infrastructure.external.holiday;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.infrastructure.external.holiday.dto.HolidayInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
     * 
     * @param date 查询日期
     * @return 节假日信息，查询失败返回null
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
            
            int dayType = type.path("type").asInt();
            
            return HolidayInfo.builder()
                    .date(date)
                    .dayType(dayType)
                    .dayTypeName(type.path("name").asText())
                    .holidayName(holiday.isNull() || holiday.isMissingNode() ? null : holiday.path("name").asText())
                    .isOff(dayType == 1 || dayType == 2)  // 周末或法定节假日
                    .build();
                    
        } catch (Exception e) {
            log.error("查询节假日信息失败: date={}", date, e);
            return null;
        }
    }

    /**
     * 批量查询某年的节假日和调休日
     * 
     * @param year 年份
     * @return 节假日列表
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
            Iterator<Map.Entry<String, JsonNode>> fields = holidayData.fields();
            
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String dateStr = entry.getKey();  // 格式: MM-dd
                JsonNode info = entry.getValue();
                
                try {
                    LocalDate date = LocalDate.parse(year + "-" + dateStr, DATE_FORMATTER);
                    boolean isHoliday = info.path("holiday").asBoolean();
                    
                    holidays.add(HolidayInfo.builder()
                            .date(date)
                            .dayType(isHoliday ? 2 : 3)  // 2=法定节假日, 3=调休工作日
                            .dayTypeName(info.path("name").asText())
                            .holidayName(info.path("name").asText())
                            .isOff(isHoliday)
                            .build());
                } catch (Exception e) {
                    log.warn("解析节假日日期失败: {}", dateStr, e);
                }
            }
            
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
     * @return 目标日期，计算失败返回null
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
            
            // API返回格式有两种可能：直接返回日期或在result.date中
            JsonNode result = root.path("result");
            String resultDate;
            
            if (result.isObject()) {
                resultDate = result.path("date").asText();
            } else {
                // 直接返回日期字符串
                resultDate = root.path("result").asText();
            }
            
            return LocalDate.parse(resultDate, DATE_FORMATTER);
            
        } catch (Exception e) {
            log.error("计算工作日失败: startDate={}, workdays={}", startDate, workdays, e);
            return null;
        }
    }

    /**
     * 批量查询某个月的节假日
     * 
     * @param year 年份
     * @param month 月份
     * @return 节假日列表
     */
    public List<HolidayInfo> getMonthHolidays(int year, int month) {
        String monthStr = String.format("%04d-%02d", year, month);
        String url = BASE_URL + "/year/" + monthStr;
        List<HolidayInfo> holidays = new ArrayList<>();
        
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            if (root.path("code").asInt() != 0) {
                log.warn("查询月度节假日失败: {}", root.path("msg").asText());
                return holidays;
            }
            
            JsonNode holidayData = root.path("holiday");
            Iterator<Map.Entry<String, JsonNode>> fields = holidayData.fields();
            
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String dateStr = entry.getKey();
                JsonNode info = entry.getValue();
                
                try {
                    LocalDate date = LocalDate.parse(year + "-" + dateStr, DATE_FORMATTER);
                    boolean isHoliday = info.path("holiday").asBoolean();
                    
                    holidays.add(HolidayInfo.builder()
                            .date(date)
                            .dayType(isHoliday ? 2 : 3)
                            .dayTypeName(info.path("name").asText())
                            .holidayName(info.path("name").asText())
                            .isOff(isHoliday)
                            .build());
                } catch (Exception e) {
                    log.warn("解析节假日日期失败: {}", dateStr, e);
                }
            }
            
        } catch (Exception e) {
            log.error("查询月度节假日失败: year={}, month={}", year, month, e);
        }
        
        return holidays;
    }
}
