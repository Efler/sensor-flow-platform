package org.eflerrr.sfp.sparkjobs.verifier.utils;

import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

public class HMACUtils {
    private static final ConcurrentHashMap<String, Mac> HMACs = new ConcurrentHashMap<>();

    public static String calculateHMAC(String data, String secret) throws Exception {
        Mac mac = HMACs.computeIfAbsent(secret, (s) -> {
            try {
                return Mac.getInstance("HmacSHA256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        });
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(rawHmac);
    }
}
