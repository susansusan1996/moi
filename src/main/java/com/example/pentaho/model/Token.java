package com.example.pentaho.model;


import com.example.pentaho.utils.RsaUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;


@Component
@ConfigurationProperties(prefix = "application.security")
public class Token {


    private final static Logger log = LoggerFactory.getLogger(Token.class);


    private static String accessPrivateKey;

    private static String refreshPrivateKey;

    private String token;

    public Token() {
    }

    public Token(String token) {
        this.token = token;
    }


    public String getAccessPrivateKey() {
        return accessPrivateKey;
    }

    public void setAccessPrivateKey(String accessPrivateKey) {
        this.accessPrivateKey = accessPrivateKey;
    }

    public String getRefreshPrivateKey() {
        return refreshPrivateKey;
    }

    public void setRefreshPrivateKey(String refreshPrivateKey) {
        this.refreshPrivateKey = refreshPrivateKey;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public static Token of(Long userId, Long vaildityInMinutes, String privateKey) {
        /****/
        Instant issuedDate = Instant.now();
        String tokenStr = Jwts.builder().claim("userId", String.valueOf(userId))
                .setIssuedAt(Date.from(issuedDate.plus(vaildityInMinutes, ChronoUnit.MINUTES)))
                .signWith(SignatureAlgorithm.HS256, Base64.getEncoder().encode(privateKey.getBytes()))
                .compact();
        /**
         * Token.getToken will return the tokenStr
         * */
        return new Token(tokenStr);
    }

    /**
     * 解密!!
     * 這裡有坑 弄好久才解密成功!
     **/
    public static long from(String refreshTokenStr) {
        log.info("refreshPrivateKey:{}", refreshPrivateKey);
        Key key = new SecretKeySpec(Base64.getEncoder().encode(refreshPrivateKey.getBytes()), SignatureAlgorithm.HS256.getJcaName());
        Jws<Claims> claimsJws = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(refreshTokenStr);
        Claims body = claimsJws.getBody();


        log.info("body:{}", body);
        String userId = body.get("userId", String.class);
        log.info("userId:{}", userId);
        return Long.valueOf(userId);
    }

    public static boolean fromA(String accessTokenStr) {
        try {
            log.info("accessPrivateKey:{}", accessPrivateKey);
            //公鑰驗證jwt token
            InputStream inputStream = RsaUtils.class.getClassLoader().getResourceAsStream("rsa.pub");
            byte[] keyBytes = inputStream.readAllBytes();
            byte[] decodedKeyBytes = Base64.getDecoder().decode(keyBytes);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            Jws<Claims> claimsJws = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(accessTokenStr);
            Claims body = claimsJws.getBody();
            log.info("body:{}", body.toString());
            return true;
        } catch (Exception e) {
            log.info("e:{}", e.toString());
            return false;
        }
    }

}
