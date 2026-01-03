package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.Resignation;
import com.lawfirm.infrastructure.persistence.mapper.ResignationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 离职申请 Repository
 */
@Repository
public class ResignationRepository extends AbstractRepository<ResignationMapper, Resignation> {

    /**
     * 根据申请编号查询
     */
    public Optional<Resignation> findByApplicationNo(String applicationNo) {
        Resignation resignation = lambdaQuery()
                .eq(Resignation::getApplicationNo, applicationNo)
                .one();
        return Optional.ofNullable(resignation);
    }

    /**
     * 根据员工ID查询离职申请
     */
    public List<Resignation> findByEmployeeId(Long employeeId) {
        return baseMapper.selectByEmployeeId(employeeId);
    }
}

