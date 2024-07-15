package com.example.pentaho.resource;

import com.example.pentaho.utils.RsaReadUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Base64;


public class RSATest {

    private final static Logger log = LoggerFactory.getLogger(RSATest.class);


    private static final int DEFAULT_KEY_SIZE = 2048;

    @Test
    public void testpass() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode("123456");
        log.info(encode);
    }


    /**
     * 根據密文，生成RSA公鑰、私鑰，並保存到文件裡

     */
    @Test
    public void generateKey() throws Exception {
        /**
         * publicKeyFilename  公鑰文件路徑
         * privateKeyFilename 私鑰文件路徑
         * secret 生成密鑰的密文
         **/
        String publicKeyFilename = "D:\\rsa_key\\generate\\rsa.pub";
        String privateKeyFilename ="D:\\rsa_key\\generate\\rsa.pri";
        String secret ="mysecret";
        int keySize = DEFAULT_KEY_SIZE;
        /*簽名*/
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        SecureRandom secureRandom = new SecureRandom(secret.getBytes());
        /*大小*/
        keyPairGenerator.initialize(Math.max(keySize, DEFAULT_KEY_SIZE), secureRandom);
        KeyPair keyPair = keyPairGenerator.genKeyPair();
        // 獲取公鑰並導出
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        publicKeyBytes = Base64.getEncoder().encode(publicKeyBytes);
        writeFile(publicKeyFilename, publicKeyBytes);
        // 獲取私鑰並導出
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
        privateKeyBytes = Base64.getEncoder().encode(privateKeyBytes);
        writeFile(privateKeyFilename, privateKeyBytes);
    }

    private static void writeFile(String destPath, byte[] bytes) throws IOException {
        File dest = new File(destPath);
        if (!dest.exists()) {
            dest.createNewFile();
        }
        Files.write(dest.toPath(), bytes);
    }


    /**
     * 獲取公、私鑰
     * @throws Exception
     */
    @Test
    public void createKeyPair() throws Exception {
       
        String privateFilePath = "D:\\rsa_key\\generate\\rsa.pri";
        String publicFilePath = "D:\\rsa_key\\generate\\rsa.pub";
        //獲取私鑰
        log.info("PrivateKey:{}", RsaReadUtils.getPrivateKey(privateFilePath));
        //獲取公鑰
        log.info("PublicKey:{}", RsaReadUtils.getPublicKey(publicFilePath));
    }
}
