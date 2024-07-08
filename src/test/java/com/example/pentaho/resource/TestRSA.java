package com.example.pentaho.resource;

import com.example.pentaho.utils.RsaReadUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


public class TestRSA {

    private final static Logger log = LoggerFactory.getLogger(TestRSA.class);

    @Test
    public void testpass() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode("123456");
        log.info(encode);
    }

    @Test
    public void createKeyPair() throws Exception {
        /*
            生成功鑰密鑰的方法。需要以下四個參數
            參數1：生成公鑰的儲存位置
            參數2：生成私鑰的儲存位置
            參數3：生成密鑰對的密鑰
            參數3：生成密鑰對的長度
         */
        String privateFilePath = "D:\\rsa_key\\rsa.pri";
        String publicFilePath = "D:\\rsa_key\\rsa.pub";
        //獲取私鑰
        log.info("PrivateKey:{}", RsaReadUtils.getPrivateKey(privateFilePath));
        //獲取公鑰
        log.info("PublicKey:{}", RsaReadUtils.getPublicKey(publicFilePath));
    }
}
