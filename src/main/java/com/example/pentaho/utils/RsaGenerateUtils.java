package com.example.pentaho.utils;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.util.Base64;

/**
 * 僅供開發環境生成公鑰、私鑰
 */
public class RsaGenerateUtils {
    private static final int DEFAULT_KEY_SIZE = 2048;

    /**
     * 根據密文，生成RSA公鑰、私鑰，並保存到文件裡
     *
     * @param publicKeyFilename  公鑰文件路徑
     * @param privateKeyFilename 私鑰文件路徑
     * @param secret             生成密鑰的密文
     */
    public static void generateKey(String publicKeyFilename, String privateKeyFilename, String
            secret, int keySize) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        SecureRandom secureRandom = new SecureRandom(secret.getBytes());
        keyPairGenerator.initialize(Math.max(keySize, DEFAULT_KEY_SIZE), secureRandom);
        KeyPair keyPair = keyPairGenerator.genKeyPair();
        // 獲取公鑰並導出
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        publicKeyBytes = Base64.getEncoder().encode(publicKeyBytes);
        /*Fortify Privacy Violation:將私密數據導出*/
        writeFile(publicKeyFilename, publicKeyBytes);
        // 獲取私鑰並導出
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
        privateKeyBytes = Base64.getEncoder().encode(privateKeyBytes);
        /*Fortify Privacy Violation:將私密數據導出*/
        writeFile(privateKeyFilename, privateKeyBytes);
    }

    private static byte[] readFile(String fileName) throws Exception {
        return Files.readAllBytes(new File(fileName).toPath());
    }

    private static void writeFile(String destPath, byte[] bytes) throws IOException {
        File dest = new File(destPath);
        if (!dest.exists()) {
            dest.createNewFile();
        }
        Files.write(dest.toPath(), bytes);
    }
}

