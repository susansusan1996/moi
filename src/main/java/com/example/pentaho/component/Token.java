package com.example.pentaho.component;


import com.example.pentaho.utils.JWTUtils;
import com.example.pentaho.utils.RsaUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Component
@ConfigurationProperties(prefix = "application.security")
public class Token {


    private final static Logger log = LoggerFactory.getLogger(Token.class);

    private static ObjectMapper objectMapper =new ObjectMapper();


    /**
     * for HS256 +
     */
    private static String accessPrivateKey;

    /**
     * for HS256
     */
    private static String refreshPrivateKey;

    /**
     * for HS256
     */
   private final static long vaildityTime = 10000000;

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

    /**
     * HS256
     */
    public static Token acessTokenofHS256(Long userId) {
        /****/
        Instant issuedDate = Instant.now();
        String tokenStr = Jwts.builder().claim("userId", String.valueOf(userId))
                .setIssuedAt(Date.from(issuedDate.plus(vaildityTime, ChronoUnit.MINUTES)))
                .signWith(SignatureAlgorithm.HS256, Base64.getEncoder().encode(accessPrivateKey.getBytes()))
                .compact();
        /**
         * Token.getToken will return the tokenStr
         * */
        return new Token(tokenStr);
    }

    /**
     * HS256
     */
    public static Token refreshTokenofHS256(Long userId) {
        /****/
        Instant issuedDate = Instant.now();
        String tokenStr = Jwts.builder().claim("userId", String.valueOf(userId))
                .setIssuedAt(Date.from(issuedDate.plus(vaildityTime, ChronoUnit.MINUTES)))
                .signWith(SignatureAlgorithm.HS256, Base64.getEncoder().encode(refreshPrivateKey.getBytes()))
                .compact();
        /**
         * Token.getToken will return the tokenStr
         * */
        return new Token(tokenStr);
    }


    /**
     * HS256解密!!
     * 這裡有坑 弄好久才解密成功!
     **/
    public static long fromHS256RefreshPrivateKey(String refreshTokenStr) {
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

    /**
     * HS256解密!!
     * 這裡有坑 弄好久才解密成功!
     **/
    public static long fromHS256AccessPrivateKey(String accessTokenStr) {
        log.info("accessPrivateKey:{}", accessPrivateKey);
        Key key = new SecretKeySpec(Base64.getEncoder().encode(accessPrivateKey.getBytes()), SignatureAlgorithm.HS256.getJcaName());
        Jws<Claims> claimsJws = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(accessTokenStr);
        Claims body = claimsJws.getBody();
        log.info("body:{}", body);
        String userId = body.get("userId", String.class);
        log.info("userId:{}", userId);
        return Long.valueOf(userId);
    }

    /***
     *產生RSAJWTToken
     */
    public static Token ofRSAJWT(User user) {
        try {
            InputStream inputStream = RsaUtils.class.getClassLoader().getResourceAsStream("rsa.pri");
            byte[] keyBytes = inputStream.readAllBytes();
            byte[] decodedKeyBytes = Base64.getDecoder().decode(keyBytes);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey RSAprivateKey = keyFactory.generatePrivate(spec);
          return new Token(JWTUtils.generateTokenExpireInMinutes(user, RSAprivateKey, 20));//20分鐘過期
        }catch (Exception e){
            log.info("e:{}",e.toString());
          return null;
        }
    }

    /**
     * 解密RSAJWTToken中的userInfo
     * */
    public static boolean fromRSAJWTToken(String RSAJWTToken) {
        try {
            log.info("RSAJWTToken:{}", RSAJWTToken);
            //公鑰驗證jwt token
            InputStream inputStream = RsaUtils.class.getClassLoader().getResourceAsStream("rsa.pub");
            byte[] keyBytes = inputStream.readAllBytes();
            byte[] decodedKeyBytes = Base64.getDecoder().decode(keyBytes);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            Jws<Claims> claimsJws = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(RSAJWTToken);
            Claims body = claimsJws.getBody();
            log.info("body:{}", body.toString());
         return true;
        } catch (Exception e) {
            log.info("e:{}", e.toString());
            return false;
        }
    }

    /**
     * 解密,取出userInfo
     * @param RSAJWTToken
     * @return
     */
    public static User extractUserFromRSAJWTToken(String RSAJWTToken) {
        try {
            log.info("RSAJWTToken:{}", RSAJWTToken);
            //公鑰驗證jwt token
            InputStream inputStream = RsaUtils.class.getClassLoader().getResourceAsStream("rsa.pub");
            byte[] keyBytes = inputStream.readAllBytes();
            byte[] decodedKeyBytes = Base64.getDecoder().decode(keyBytes);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            Jws<Claims> claimsJws = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(RSAJWTToken);
            Claims body = claimsJws.getBody();
            log.info("body:{}", body.toString());
            String userInfo = body.get("user", String.class);
            if(userInfo.equals("") || userInfo == null){
              return null;
            }
            return objectMapper.readValue(userInfo, User.class);
        } catch (Exception e) {
            log.info("e:{}", e.toString());
            return null;
        }
    }

    @Override
    public String toString() {
        return "Token{" +
                "token='" + token + '\'' +
                '}';
    }
}
