package com.brimmatech.saas.crytograph;

import com.brimmatech.general.errorhandling.EncryptDecryptException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

@Service
@Deprecated
/**
 * This causes a lot of leaks (bytea old gen)
 *
 * Here is a better alternative
 * Cipher cipher = Cipher.getInstance(algorithm);
 * cipher.init(Cipher.ENCRYPT_MODE, key);
 *
 * try (
 *     InputStream in = sourceInputStream;   // DB / file / HTTP
 *     CipherInputStream cin = new CipherInputStream(in, cipher);
 *     OutputStream out = blobOutputStream     // Azure Blob / file
 * ) {
 *     byte[] buffer = new byte[64 * 1024];   // reusable
 *     int n;
 *     while ((n = cin.read(buffer)) != -1) {
 *         out.write(buffer, 0, n);
 *     }
 * }
 *
 */
public class EncryptDecryptService {

    @Autowired
    ObjectMapper objectMapper;
    @Value("${app.crypto.encryption-key}")
    private String encryptionSecretKey;
    @Value("${app.crypto.encryption-algorithm}")
    private String encryptionAlgorithm;
    @Value("${app.crypto.message-digest-algorithm}")
    private String messageDigestAlgorithm;
    @Value("${app.crypto.algorithm}")
    private String secretKeyAlgorithm;

    public String encrypt(Object strToEncrypt) {

        Cipher cipher;
        try {
            String request = objectMapper.writeValueAsString(strToEncrypt);
            cipher = Cipher.getInstance(encryptionAlgorithm);
            cipher.init(Cipher.ENCRYPT_MODE, generateSecretKey());
            return Base64.getEncoder().encodeToString(cipher.doFinal(request.getBytes(StandardCharsets.UTF_8)));

        } catch (GeneralSecurityException | UnsupportedEncodingException exception) {
            throw new EncryptDecryptException("Not able to encrypt the request!!", exception);
        } catch (JsonProcessingException e) {
            throw new EncryptDecryptException("Error occurred while processing json");
        }
    }

    public Object decrypt(String strToDecrypt) {
        if (strToDecrypt == null || strToDecrypt.isEmpty()) {
            throw new EncryptDecryptException("No request is available to decrypt.");
        }

        JsonNode decryptedJsonNode;
        try {
            Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
            cipher.init(Cipher.DECRYPT_MODE, generateSecretKey());
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(strToDecrypt));
            String decryptedString = new String(decryptedBytes, StandardCharsets.UTF_8);
            decryptedJsonNode = objectMapper.readTree(decryptedString);
        } catch (IllegalArgumentException | GeneralSecurityException | UnsupportedEncodingException e) {
            try {
                decryptedJsonNode = objectMapper.readTree("\"" + strToDecrypt + "\"");
            } catch (JsonProcessingException ex) {
                throw new EncryptDecryptException("Not able to decrypt or parse the request.", ex);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return decryptedJsonNode;
    }

    public String decryptAsString(String strToDecrypt) {
        if (strToDecrypt == null || strToDecrypt.isEmpty()) {
            throw new EncryptDecryptException("No request is available to decrypt.");
        }

        try {
            Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
            cipher.init(Cipher.DECRYPT_MODE, generateSecretKey());
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(strToDecrypt));
            String decrypted = new String(decryptedBytes, StandardCharsets.UTF_8);

            try {
                JsonNode node = objectMapper.readTree(decrypted);
                if (node.isTextual()) {
                    return node.asText();
                } else {
                    return decrypted;
                }
            } catch (JsonProcessingException e) {
                return decrypted; // Not JSON: return raw string
            }

        } catch (IllegalArgumentException | GeneralSecurityException | UnsupportedEncodingException e) {
            // If Base64 decode or decryption fails, assume it is plain text
            return strToDecrypt;
        }
    }


    private SecretKeySpec generateSecretKey() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] key = encryptionSecretKey.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance(messageDigestAlgorithm);
        return new SecretKeySpec(Arrays.copyOf(sha.digest(key), 16), secretKeyAlgorithm);
    }

}
