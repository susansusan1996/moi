package com.example.pentaho.component;


import com.example.pentaho.utils.RSAJWTUtils;
import com.example.pentaho.utils.RsaUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Token {


    private final static Logger log = LoggerFactory.getLogger(Token.class);

    private static ObjectMapper objectMapper =new ObjectMapper();


    private String token;

    public Token() {

    }


    public Token(String token) {
        this.token = token;
    }


    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
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
          return new Token(RSAJWTUtils.generateTokenExpireInMinutes(user, RSAprivateKey, 20));//20分鐘過期
        }catch (Exception e){
            log.info("e:{}",e.toString());
          return null;
        }
    }

    /**
     * 解密RSAJWTToken中的userInfo
     * */
    public static boolean fromRSAJWTToken(String RSAJWTToken,String keyName) {
        try {
            log.info("RSAJWTToken:{}", RSAJWTToken);
            //公鑰驗證jwt token
            InputStream inputStream = RsaUtils.class.getClassLoader().getResourceAsStream(keyName);
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
    public static User extractUserFromRSAJWTToken(String RSAJWTToken,String keyName) {
        try {
            log.info("RSAJWTToken:{}", RSAJWTToken);
            //公鑰驗證jwt token
            InputStream inputStream = RsaUtils.class.getClassLoader().getResourceAsStream(keyName);
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