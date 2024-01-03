package com.example.pentaho.resource;

import com.example.pentaho.utils.RsaUtils;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestRSA {

    @Test
    public void testpass() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode("123456");
        System.out.println(encode);
    }

    @Test
    public void createKeyPair() throws Exception {
        String privateFilePath = "D:\\rsa_key\\rsa.pri";
        String publicFilePath = "D:\\rsa_key\\rsa.pub";
        //生成功鑰密鑰的方法。需要以下四個參數
        /*
            參數1：生成公鑰的儲存位置
            參數2：生成私鑰的儲存位置
            參數3：生成密鑰對的密鑰
            參數3：生成密鑰對的長度
         */
        RsaUtils.generateKey(publicFilePath, privateFilePath, "itsource", 2048);
        //獲取私鑰
        System.out.println(RsaUtils.getPrivateKey(privateFilePath));
        //獲取公鑰
        System.out.println(RsaUtils.getPublicKey(publicFilePath));
    }
}
