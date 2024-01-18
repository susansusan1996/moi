package com.example.pentaho.utils;

import com.example.pentaho.component.KeyComponent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

@Component
public class RSAJWTUtils {
    private final static Logger log = LoggerFactory.getLogger(RSAJWTUtils.class);


    private static final String JWT_PAYLOAD_USER_KEY = "user";



    /**
     * 私钥加密token
     *
     * @param userInfo   payload中的數據
     * @param privateKey 私鑰
     * @param expire     過期時間，單位分鐘
     * @return JWT
     */
    public static String generateTokenExpireInMinutes(Object userInfo, PrivateKey privateKey, int expire) throws JsonProcessingException {
        //计算过期时间
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, expire);
        ObjectMapper objectMapper = new ObjectMapper();
        return Jwts.builder()
                .claim(JWT_PAYLOAD_USER_KEY, objectMapper.writeValueAsString(userInfo))
                .setId(new String(Base64.getEncoder().encode(UUID.randomUUID().toString().getBytes())))
                .setExpiration(c.getTime())
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * 獲取token中的用戶資訊
     *
     * @param token
     * @param publicKey
     * @return 用戶資訊
     */
    public static Object getInfoFromToken(String token, PublicKey publicKey, Class userType) throws JsonProcessingException {
        //解析token
        Jws<Claims> claimsJws = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(token.substring(7));//去掉 "Bearer " 前缀
        ObjectMapper objectMapper = new ObjectMapper();
        Claims body = claimsJws.getBody();
        String userInfoJson = body.get(JWT_PAYLOAD_USER_KEY).toString();
        return objectMapper.readValue(userInfoJson, userType);
    }

}
