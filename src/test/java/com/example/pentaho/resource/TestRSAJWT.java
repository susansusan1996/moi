package com.example.pentaho.resource;

import com.example.pentaho.utils.JWTUtils;
import com.example.pentaho.utils.RsaUtils;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;


public class TestRSAJWT {

    @Test //創建用rsa密鑰生成的token
    public void rsaCreateTest() throws Exception {
        Map userinfo = new HashMap() {{
//            put("account", "jack");
//            put("auths", Stream.of("a", "b", "c", "d").collect(Collectors.toList()));
        }};
        String path = ResourceUtils.getFile("classpath:rsa.pri").getPath();
        PrivateKey privateKey = RsaUtils.getPrivateKey(path);
        String token = JWTUtils.generateTokenExpireInMinutes(userinfo, privateKey, 20); //20分鐘過期
        System.out.println(token);
    }

    @Test //用公鑰解密jwt token
    public void rsaRead() throws Exception {
        //解析token，要替換成上面rsaCreateTest產出的token
        String token = "Bearer eyJhbGciOiJSUzI1NiJ9.eyJ1c2VyIjoie30iLCJqdGkiOiJaak14TURWaU1qUXROak5rTVMwME1ESTVMVGczTVRFdFpUSXdNams0TmpZNU5tUmgiLCJleHAiOjE3MDQyNjY4OTN9.i2X0gp1q5cntUX36YHrIKZLad4mg307dbIOBn4vfzg_GsURjYkaRwLItYuE1d0ChgHsmBRXzeR5DKYog8llWgxhRfCvIPnOvERidk-hrPMcyoKn1Ke7QVIs5zWdFs6mU4et59BgvQQcK25ppFYIKIbDC8qnk7k7FQTCcSR_EZcIJgHGSEhwjz0E10e_EHdlsSCfHByk4my2tweoofdI6Osd0zkMBPvrLEcTdB9hpVj-TfmVfdXt0s2SDN1VwcTCw9107WP1k07TddHUm6aqlkNEteTGQ3PaqG2ao2pWTjWj8z1EP-VpNrryzEaQmE2Bp8lzs4DSEXX1pZqSVwJ_pQA";
        String path1 = ResourceUtils.getFile("classpath:rsa.pub").getPath();
        PublicKey publicKey = RsaUtils.getPublicKey(path1);
        Map infoFromToken = (Map) JWTUtils.getInfoFromToken(token, publicKey, Map.class);
        System.out.println(infoFromToken.get("account"));
        System.out.println(infoFromToken.get("auth"));
    }
}

