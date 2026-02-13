package com.archivesystem.service.impl;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.Fonds;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.FondsMapper;
import com.archivesystem.service.FondsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 全宗服务实现.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FondsServiceImpl implements FondsService {

    private final FondsMapper fondsMapper;
    private final ArchiveMapper archiveMapper;

    @Override
    @Transactional
    public Fonds create(Fonds fonds) {
        // 检查全宗号是否已存在
        if (fondsMapper.selectByFondsNo(fonds.getFondsNo()) != null) {
            throw new BusinessException("全宗号已存在: " + fonds.getFondsNo());
        }
        
        fonds.setStatus(Fonds.STATUS_ACTIVE);
        fondsMapper.insert(fonds);
        log.info("创建全宗: id={}, fondsNo={}", fonds.getId(), fonds.getFondsNo());
        return fonds;
    }

    @Override
    @Transactional
    public Fonds update(Long id, Fonds fonds) {
        Fonds existing = fondsMapper.selectById(id);
        if (existing == null) {
            throw NotFoundException.of("全宗", id);
        }
        
        // 如果修改了全宗号，检查是否重复
        if (!existing.getFondsNo().equals(fonds.getFondsNo())) {
            Fonds byNo = fondsMapper.selectByFondsNo(fonds.getFondsNo());
            if (byNo != null && !byNo.getId().equals(id)) {
                throw new BusinessException("全宗号已存在: " + fonds.getFondsNo());
            }
        }
        
        existing.setFondsNo(fonds.getFondsNo());
        existing.setFondsName(fonds.getFondsName());
        existing.setFondsType(fonds.getFondsType());
        existing.setDescription(fonds.getDescription());
        existing.setStatus(fonds.getStatus());
        
        fondsMapper.updateById(existing);
        return existing;
    }

    @Override
    public Fonds getById(Long id) {
        Fonds fonds = fondsMapper.selectById(id);
        if (fonds == null) {
            throw NotFoundException.of("全宗", id);
        }
        return fonds;
    }

    @Override
    public Fonds getByFondsNo(String fondsNo) {
        return fondsMapper.selectByFondsNo(fondsNo);
    }

    @Override
    public List<Fonds> list() {
        LambdaQueryWrapper<Fonds> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Fonds::getDeleted, false)
               .orderByAsc(Fonds::getFondsNo);
        return fondsMapper.selectList(wrapper);
    }

    @Override
    public PageResult<Fonds> query(String keyword, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Fonds> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Fonds::getDeleted, false);
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Fonds::getFondsNo, keyword)
                    .or().like(Fonds::getFondsName, keyword)
            );
        }
        
        wrapper.orderByAsc(Fonds::getFondsNo);
        
        Page<Fonds> page = new Page<>(pageNum, pageSize);
        Page<Fonds> result = fondsMapper.selectPage(page, wrapper);
        
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Fonds fonds = fondsMapper.selectById(id);
        if (fonds == null) {
            return;
        }
        
        // 检查是否有关联档案
        long archiveCount = countArchives(id);
        if (archiveCount > 0) {
            throw new BusinessException("该全宗下存在 " + archiveCount + " 个档案，无法删除");
        }
        
        fondsMapper.deleteById(id);
        log.info("删除全宗: id={}", id);
    }

    @Override
    public long countArchives(Long fondsId) {
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Archive::getFondsId, fondsId)
               .eq(Archive::getDeleted, false);
        return archiveMapper.selectCount(wrapper);
    }
}
