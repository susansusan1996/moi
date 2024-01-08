package com.example.pentaho.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

@Component
public class JWTUtils {


    private final static Logger log = LoggerFactory.getLogger(JWTUtils.class);

    private final static long vaildityTime = 10000000;

    private PublicKey publicKey;

    private PrivateKey privateKey;

    private static final String JWT_PAYLOAD_USER_KEY = "user";


    public JWTUtils() throws Exception {
        generateKeyPair();
    }


    /***
     * 取得 RSA非對稱演算法生成的公鑰 & 密鑰
     * @return
     * @throws Exception
     */
    public void generateKeyPair() throws Exception {
        /**RSA非對稱演算法**/
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        /***设置密钥长度*/
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        /**公鑰 & 密鑰 **/
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();
    }

    public String createJwtToken(String userId) {
        /**需要設定過期時間嗎**/
        Instant declaireDate = Instant.now();
        return Jwts.builder().claim("userId", userId).setIssuedAt(Date.from(declaireDate))
                .setExpiration(Date.from(declaireDate.plus(vaildityTime, ChronoUnit.MINUTES)))
                /**密鑰 + RS256**/
                /**Base64-encoded key bytes may only be specified for HMAC signatures.  If using RSA or Elliptic Curve, use the signWith(SignatureAlgorithm, Key) method instead.**/
                .signWith(SignatureAlgorithm.RS256, privateKey)
                .compact();
    }


    /***
     * 驗證聖森的JWTtoken
     * @param jwtToken
     */
    public boolean verifyJwtToken(String jwtToken) {
        try {
            Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(jwtToken);
            log.info("JWT Token is valid");
            return true;
        } catch (Exception e) {
            log.info("JWT Token verification failed: " + e.getMessage());
            return false;
        }
    }


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
