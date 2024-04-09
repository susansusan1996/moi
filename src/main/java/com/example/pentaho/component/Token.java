package com.example.pentaho.component;


import com.example.pentaho.utils.RSAJWTUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Component
public class Token {


    private final static Logger log = LoggerFactory.getLogger(Token.class);

    private static ObjectMapper objectMapper =new ObjectMapper();

    private static Gson gson = new Gson();


    private String token;

    private String expiryDate;

    public Token() {

    }


    public Token(String token) {
        this.token = token;
    }

    public Token(String token, String expiryDate) {
        this.token = token;
        this.expiryDate = expiryDate;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    /***
     *產生RSAJWTToken
     */
    public static Token ofRSAJWT(User user,String keyName) {
        try {
//            InputStream inputStream = RsaUtils.class.getClassLoader().getResourceAsStream("rsa.pri");
//            byte[] keyBytes = inputStream.readAllBytes();
            log.info("keyName:{}",keyName);
            File file = ResourceUtils.getFile(keyName);
            byte[] keyBytes = readFileAsBytes(file);
            byte[] decodedKeyBytes = Base64.getDecoder().decode(keyBytes);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey RSAprivateKey = keyFactory.generatePrivate(spec);
            Map<String,Object> map = RSAJWTUtils.generateTokenExpireInMinutes(user, RSAprivateKey, 1440);
          return new Token((String) map.get("token"));//20分鐘過期
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
            log.info("keyName:{}", keyName);
            //公鑰驗證jwt token
//            InputStream inputStream = RsaUtils.class.getClassLoader().getResourceAsStream(keyName);
//            byte[] keyBytes = inputStream.readAllBytes();
            File file = ResourceUtils.getFile(keyName);
            byte[] keyBytes = readFileAsBytes(file);
            byte[] decodedKeyBytes = Base64.getDecoder().decode(keyBytes);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            Jws<Claims> claimsJws = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(RSAJWTToken.trim());
            Claims body = claimsJws.getBody();
            log.info("body:{}", body.toString());
         return true;
        } catch (Exception e) {
            log.info("e:{}", e.toString());
            return false;
        }
    }



    public static boolean findExpireDateOfRefreshToken(String RSAJWTToken,String keyName) throws ExpiredJwtException, NoSuchAlgorithmException, FileNotFoundException, InvalidKeySpecException {
            log.info("keyName:{}", keyName);
            File file = ResourceUtils.getFile(keyName);
            byte[] keyBytes = readFileAsBytes(file);
            byte[] decodedKeyBytes = Base64.getDecoder().decode(keyBytes);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            Jws<Claims> claimsJws = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(RSAJWTToken.trim());
            Claims body = claimsJws.getBody();
            // 检查JWT令牌的过期时间
            Date expiration = body.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                log.info("JWT token has expired.");
            }
            log.info("body:{}", body.toString());
            return true;
    }

    /**
     * 解密,取出userInfo
     * @param RSAJWTToken
     * @return
     */
    public static User extractUserFromRSAJWTToken(String RSAJWTToken,String keyName) {
        try {
            log.info("keyName:{}", keyName);
            //公鑰驗證jwt token
            File file = ResourceUtils.getFile(keyName);
            byte[] keyBytes = readFileAsBytes(file);
            byte[] decodedKeyBytes = Base64.getDecoder().decode(keyBytes);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            Jws<Claims> claimsJws = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(RSAJWTToken);
            Claims body = claimsJws.getBody();
            log.info("body:{}", body.toString());
            log.info("userInfo",body.get("userInfo"));
            String userInfo = gson.toJson(body.get("userInfo"));
            return objectMapper.readValue(userInfo,User.class);
        } catch (Exception e) {
            log.info("e:{}", e.toString());
            return null;
        }
    }


    private static byte[] readFileAsBytes(File file){
        try {
            FileInputStream inputStreamn = new FileInputStream(file);
            return inputStreamn.readAllBytes();
        }catch (Exception e){
             log.info("read rsa.pri key error:{}",e.toString());
             return null;
        }
    }

    @Override
    public String toString() {
        return "Token{" +
                "token='" + token + '\'' +
                ", expiryDate=" + expiryDate +
                '}';
    }
}
