package com.lawfirm.application.workbench.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 可用报表类型DTO ✅ 修复问题568: 使用类型安全的DTO替代Map */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableReportDTO {

  /** 报表类型代码 */
  private String type;

  /** 报表名称 */
  private String name;

  /** 报表描述 */
  private String description;

  /** 支持的导出格式 */
  private List<String> formats;
}
