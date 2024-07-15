package com.example.pentaho.resource;

import com.example.pentaho.utils.RSAJWTUtils;
import com.example.pentaho.utils.RsaReadUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class RSAJWTTest {

    private String RSAToken;


    private final static Logger log = LoggerFactory.getLogger(RSAJWTTest.class);



    @BeforeEach
    public void createTest() throws Exception {
        rsaCreateTest();
    }

    public void rsaCreateTest() throws Exception {

        //playload
        Map userinfo = new HashMap() {{
//            put("account", "jack");
//            put("auths", Stream.of("a", "b", "c", "d").collect(Collectors.toList()));
        }};
        String path = ResourceUtils.getFile("D:\\rsa_key\\rsa.pri").getPath();
        PrivateKey privateKey = RsaReadUtils.getPrivateKey(path);
        RSAToken ="Bearer "+ RSAJWTUtils.generateTokenExpireInMinutes(userinfo, privateKey, 525600).get("token"); //20分鐘過期
        log.info("RSAToken: "+RSAToken);
    }


    @Test //用公鑰解密jwt token
    public void rsaRead() throws Exception {
        //解析token，要替換成上面rsaCreateTest產出的token
        //用固定字串測會有過期的錯誤
        String path1 = ResourceUtils.getFile("D:\\rsa_key\\rsa.pub").getPath();
        PublicKey publicKey = RsaReadUtils.getPublicKey(path1);
        Map infoFromToken = (Map) RSAJWTUtils.getInfoFromToken(RSAToken, publicKey, Map.class);
        log.info("account:{}",infoFromToken.get("account"));
        log.info("auth:{}",infoFromToken.get("auth"));
    }
}

