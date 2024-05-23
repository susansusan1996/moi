package com.example.pentaho.resource;

import com.example.pentaho.component.KeyComponent;
import com.example.pentaho.utils.RSAJWTUtils;
import com.example.pentaho.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;


public class TestRSAJWT {

    private String RSAToken;


    @BeforeEach
    public void createTest() throws Exception {
        rsaCreateTest();

    }
    @Test //創建用rsa密鑰生成的token
    public void rsaCreateTest() throws Exception {
        Map userinfo = new HashMap() {{
//            put("account", "jack");
//            put("auths", Stream.of("a", "b", "c", "d").collect(Collectors.toList()));
        }};
        String path = ResourceUtils.getFile("D:\\rsa_key\\rsa.pri").getPath();
        PrivateKey privateKey = RsaUtils.getPrivateKey(path);
//        String token = RSAJWTUtils.generateTokenExpireInMinutes(userinfo, privateKey, 100000); //20分鐘過期
        RSAToken ="Bearer "+ RSAJWTUtils.generateTokenExpireInMinutes(userinfo, privateKey, 525600).get("token"); //20分鐘過期
        System.out.println("RSAToken: "+RSAToken);
    }

    @Test //用公鑰解密jwt token
    public void rsaRead() throws Exception {
        //解析token，要替換成上面rsaCreateTest產出的token
        //用固定字串測會有過期的錯誤
        String path1 = ResourceUtils.getFile("D:\\rsa_key\\rsa.pub").getPath();
        PublicKey publicKey = RsaUtils.getPublicKey(path1);
        Map infoFromToken = (Map) RSAJWTUtils.getInfoFromToken(RSAToken, publicKey, Map.class);
        System.out.println(infoFromToken.get("account"));
        System.out.println(infoFromToken.get("auth"));
    }
}

