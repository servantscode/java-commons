package org.servantscode.commons;

import org.servantscode.commons.db.ConfigDB;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

// Inspiration taken from these excellent articles:
// https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9
// and
// https://howtodoinjava.com/security/aes-256-encryption-decryption/
public class ConfigUtils {
    private static final String SALT = "WlW@Mlzdk*uogY>quHz3"; //TODO: Consider randomizing and storing this as well.
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static ConfigDB CONFIG_DB = new ConfigDB();

    private static SecretKey secretKey;

    public static String encryptConfig(String value) {
        try {
            byte[] iv = new byte[12]; //NEVER REUSE THIS IV WITH SAME KEY
            SECURE_RANDOM.nextBytes(iv);

            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv); //128 bit auth tag length
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), parameterSpec);

            byte[] cipherText = cipher.doFinal(value.getBytes());
            ByteBuffer byteBuffer = ByteBuffer.allocate(4 + iv.length + cipherText.length);
            byteBuffer.putInt(iv.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            byte[] cipherMessage = byteBuffer.array();

            return Base64.getEncoder().encodeToString(cipherMessage);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to encrypt config string.", e);
        }
    }

    public static String decryptConfig(String value) {
        try {
            byte[] cipherMessage = Base64.getDecoder().decode(value);
            ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
            int ivLength = byteBuffer.getInt();
            if(ivLength < 12 || ivLength >= 16) { // check input parameter
                throw new IllegalArgumentException("invalid iv length");
            }
            byte[] iv = new byte[ivLength];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(128, iv));

            return new String(cipher.doFinal(cipherText));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to decrypt config string.", e);
        }
    }

    public static String getConfiguration(String config) {
        return CONFIG_DB.getConfiguration(config);
    }

    // ----- Private -----
    private static SecretKey getSecretKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        if(secretKey != null)
            return secretKey;

        String encryptionKey = EnvProperty.get("SECURE_CONFIG_KEY", "AB@dK3y");
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(encryptionKey.toCharArray(), SALT.getBytes(), 65536, 128);
        SecretKey tmp = factory.generateSecret(spec);
        secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

        return secretKey;
    }
}
