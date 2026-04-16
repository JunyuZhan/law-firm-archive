package com.archivesystem.service;

import com.archivesystem.dto.config.ImageUpgradeStatusDTO;

public interface RegistryImageUpgradeCheckService {

    ImageUpgradeStatusDTO checkImageUpgrades();
}
