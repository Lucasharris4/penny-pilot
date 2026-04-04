package com.pennypilot.api.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CredentialEncryptorTest {

    private CredentialEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new CredentialEncryptor("test-encryption-key-for-unit-tests");
    }

    @Test
    void encryptDecrypt_roundTrip() {
        String original = "https://user:pass@bridge.simplefin.org/simplefin";

        String encrypted = encryptor.encrypt(original);
        String decrypted = encryptor.decrypt(encrypted);

        assertEquals(original, decrypted);
    }

    @Test
    void encrypt_producesUniqueOutputs() {
        String original = "same-input";

        String encrypted1 = encryptor.encrypt(original);
        String encrypted2 = encryptor.encrypt(original);

        // GCM uses random IV, so same plaintext produces different ciphertext
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void encrypt_outputIsDifferentFromInput() {
        String original = "my-secret-credential";

        String encrypted = encryptor.encrypt(original);

        assertNotEquals(original, encrypted);
        assertFalse(encrypted.contains(original));
    }

    @Test
    void decrypt_wrongKey_throws() {
        CredentialEncryptor otherEncryptor = new CredentialEncryptor("different-key-entirely-other");

        String encrypted = encryptor.encrypt("secret");

        assertThrows(CredentialEncryptor.EncryptionException.class,
                () -> otherEncryptor.decrypt(encrypted));
    }

    @Test
    void decrypt_corruptedData_throws() {
        assertThrows(Exception.class,
                () -> encryptor.decrypt("not-valid-base64-encrypted-data!!!"));
    }
}
