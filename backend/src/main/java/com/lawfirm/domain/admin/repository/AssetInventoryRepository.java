package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.AssetInventory;
import com.lawfirm.infrastructure.persistence.mapper.AssetInventoryMapper;
import org.springframework.stereotype.Repository;

/** 资产盘点仓储（M8-033） */
@Repository
public class AssetInventoryRepository
    extends AbstractRepository<AssetInventoryMapper, AssetInventory> {}
