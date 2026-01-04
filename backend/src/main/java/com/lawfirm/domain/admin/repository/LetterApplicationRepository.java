package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.LetterApplication;
import com.lawfirm.infrastructure.persistence.mapper.LetterApplicationMapper;
import org.springframework.stereotype.Repository;

/**
 * 出函申请仓储
 */
@Repository
public class LetterApplicationRepository extends AbstractRepository<LetterApplicationMapper, LetterApplication> {
}
