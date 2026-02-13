package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.client.entity.ClientShareholder;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 客户股东信息 Mapper */
@Mapper
public interface ClientShareholderMapper extends BaseMapper<ClientShareholder> {

  /**
   * 根据客户ID查询股东列表.
   *
   * @param clientId 客户ID
   * @return 股东列表
   */
  @Select(
      "SELECT * FROM crm_client_shareholder WHERE client_id = #{clientId} "
          + "AND deleted = false ORDER BY shareholding_ratio DESC")
  List<ClientShareholder> selectByClientId(@Param("clientId") Long clientId);
}
