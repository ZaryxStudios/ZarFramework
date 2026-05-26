package com.zaryx.framework.core.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HashingService {

    public String md5(String input) {
        return digest("MD5", input);
    }

    public String sha1(String input) {
        return digest("SHA-1", input);
    }

    public String sha256(String input) {
        return digest("SHA-256", input);
    }

    public String hmacSha256(String input, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return toHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate HMAC-SHA256", e);
        }
    }

    public boolean verifySha256(String input, String expectedHex) {
        return sha256(input).equalsIgnoreCase(expectedHex);
    }

    private String digest(String algorithm, String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing algorithm: " + algorithm, e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
