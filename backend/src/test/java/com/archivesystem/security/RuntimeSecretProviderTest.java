package com.archivesystem.security;

import com.archivesystem.entity.SysConfig;
import com.archivesystem.repository.SysConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuntimeSecretProviderTest {

    @Mock
    private SysConfigMapper sysConfigMapper;

    @InjectMocks
    private RuntimeSecretProvider runtimeSecretProvider;

    @Test
    void getJwtSecret_shouldLockExistingConfigMetadata() {
        SysConfig existing = SysConfig.builder()
                .configKey(RuntimeSecretProvider.KEY_JWT_SECRET)
                .configValue("existing-secret")
                .editable(true)
                .build();

        when(sysConfigMapper.selectByKey(RuntimeSecretProvider.KEY_JWT_SECRET)).thenReturn(existing);

        String secret = runtimeSecretProvider.getJwtSecret();

        assertEquals("existing-secret", secret);
        verify(sysConfigMapper, never()).insert(any(SysConfig.class));
        verify(sysConfigMapper).updateById(existing);
        assertEquals(false, existing.getEditable());
    }

    @Test
    void getJwtSecret_shouldUseFallbackAndPersistWhenMissing() {
        ReflectionTestUtils.setField(runtimeSecretProvider, "fallbackJwtSecret", "fallback-secret-12345678901234567890123456789012");
        when(sysConfigMapper.selectByKey(RuntimeSecretProvider.KEY_JWT_SECRET)).thenReturn(null);

        String secret = runtimeSecretProvider.getJwtSecret();

        assertEquals("fallback-secret-12345678901234567890123456789012", secret);
        verify(sysConfigMapper).insert(any(SysConfig.class));
    }

    @Test
    void getLegacyCryptoSecret_shouldLockExistingConfigMetadata() {
        SysConfig existing = SysConfig.builder()
                .configKey(RuntimeSecretProvider.KEY_CRYPTO_LEGACY_SECRET)
                .configValue("legacy-secret")
                .editable(true)
                .build();

        when(sysConfigMapper.selectByKey(RuntimeSecretProvider.KEY_CRYPTO_LEGACY_SECRET)).thenReturn(existing);

        String secret = runtimeSecretProvider.getLegacyCryptoSecret();

        assertEquals("legacy-secret", secret);
        verify(sysConfigMapper, never()).insert(any(SysConfig.class));
        verify(sysConfigMapper).updateById(existing);
        assertEquals(false, existing.getEditable());
    }

    @Test
    void getLegacyCryptoSecret_shouldUseFallbackAndPersistWhenMissing() {
        ReflectionTestUtils.setField(runtimeSecretProvider, "fallbackLegacyCryptoSecret", "legacy-fallback-secret");
        when(sysConfigMapper.selectByKey(RuntimeSecretProvider.KEY_CRYPTO_LEGACY_SECRET)).thenReturn(null);

        String secret = runtimeSecretProvider.getLegacyCryptoSecret();

        assertEquals("legacy-fallback-secret", secret);
        verify(sysConfigMapper).insert(any(SysConfig.class));
    }
}
