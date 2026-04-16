package com.archivesystem.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretCryptoServiceTest {

    private static final String VALID_SECRET = "test-crypto-secret-key-must-be-at-least-32-bytes!";
    private static final String LEGACY_SECRET = "legacy-jwt-secret-key-must-be-at-least-32!!";

    @Test
    void testEncryptAndDecrypt() {
        SecretCryptoService service = new SecretCryptoService(VALID_SECRET);

        String ciphertext = service.encrypt("smb-password");

        assertNotNull(ciphertext);
        assertNotEquals("smb-password", ciphertext);
        assertEquals("smb-password", service.decrypt(ciphertext));
    }

    @Test
    void testDecryptLegacyCiphertextWhenLegacySecretProvided() {
        SecretCryptoService legacyService = new SecretCryptoService(LEGACY_SECRET);
        String ciphertext = legacyService.encrypt("legacy-smb-password");

        SecretCryptoService service = new SecretCryptoService(VALID_SECRET, LEGACY_SECRET, true, 0);

        assertEquals("legacy-smb-password", service.decrypt(ciphertext));
    }

    @Test
    void testConstructor_BlankSecret_ShouldFailFast() {
        assertThrows(IllegalStateException.class, () -> new SecretCryptoService(" "));
    }

    @Test
    void testConstructor_WeakSecret_ShouldFailFast() {
        assertThrows(IllegalStateException.class, () -> new SecretCryptoService("weak-secret"));
    }
}
