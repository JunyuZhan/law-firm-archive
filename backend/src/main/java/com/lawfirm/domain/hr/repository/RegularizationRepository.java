package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.common.base.BaseRepository;
import com.lawfirm.domain.hr.entity.Regularization;
import com.lawfirm.infrastructure.persistence.mapper.RegularizationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 转正申请 Repository
 */
@Repository
public class RegularizationRepository extends AbstractRepository<RegularizationMapper, Regularization> {

    /**
     * 根据申请编号查询
     */
    public Optional<Regularization> findByApplicationNo(String applicationNo) {
        Regularization regularization = lambdaQuery()
                .eq(Regularization::getApplicationNo, applicationNo)
                .one();
        return Optional.ofNullable(regularization);
    }

    /**
     * 根据员工ID查询转正申请
     */
    public List<Regularization> findByEmployeeId(Long employeeId) {
        return baseMapper.selectByEmployeeId(employeeId);
    }
}

