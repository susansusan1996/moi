package com.example.pentaho.service;

import com.example.pentaho.component.*;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.utils.HeaderUtils;
import com.example.pentaho.utils.RSAJWTUtils;
import com.example.pentaho.utils.RsaUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

@Service
public class ApiKeyService {
    private static Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    @Autowired
    private KeyComponent keyComponent;

    @Autowired
    private ApServerComponent apServerComponent;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RedisService redisService;

    private final static String SYSTEM_UPDATE_URI = "/functionRecord/singleQuery/systemUpdate";


    private final static  ObjectMapper objectMapper = new ObjectMapper();

    private final int VALID_TIME = 1440;

    //返回該userID的api key
    //同時檢查token有沒有過期，有過期的話，看refresh_token有沒有過期，refresh_token沒有過期重新卷token即可
    //refresh_token有過期，整組要重建，並返回給前端
    public JwtReponse getApiKey(String userId) throws Exception {
        RefreshToken refreshToken = refreshTokenService.findRefreshTokenByUserId(userId);
        JwtReponse jwtReponse = new JwtReponse();
        if (refreshToken != null) {
            //token沒有過期
            if (refreshTokenService.verifyExpiration(userId, refreshToken.getToken(), "token")) {
                //直接返回即可
                jwtReponse.setId(userId);
                jwtReponse.setToken(refreshToken.getToken());
                jwtReponse.setRefreshToken(refreshToken.getRefreshToken());
                jwtReponse.setExpiryDate(refreshToken.getExpiryDate());
                jwtReponse.setRefreshTokenExpiryDate(refreshToken.getRefreshTokenExpiryDate());
                jwtReponse.setReviewResult(refreshToken.getReviewResult());
                return jwtReponse;
            } else {
                //token過期
                //檢查refresh_token有無過期
                //refreshToken沒有過期，卷token即可
                if (refreshTokenService.verifyExpiration(userId, refreshToken.getRefreshToken(), "refresh_token")) {
                    return createApiKey(userId, refreshToken, "");
                }
                //refreshToken也過期，全部重卷
                return createApiKey(userId, null, "");
            }
        }
        return jwtReponse;
    }


    public JwtReponse createApiKey(String userId, RefreshToken refreshToken, String type) throws Exception {
        User user = new User();
        PrivateKey privateKey = RsaUtils.getPrivateKey((keyComponent.getApPrikeyName()));
        //一般token
        user.setId(userId); //userId要用api帶過來的(如果是審核通過的)
        user.setTokenType("token");
        Map<String, Object> toeknMap = RSAJWTUtils.generateTokenExpireInMinutes(user, privateKey, VALID_TIME); //一般TOKEN效期先設一天
        Token token = new Token((String) toeknMap.get("token"), (String) toeknMap.get("expiryDate"));
        //refresh_token
        Map<String, Object> refreshTokenMap = null;
        if (refreshToken == null) {
            RefreshToken refreshTokenData = refreshTokenService.findRefreshTokenByUserId(userId);
            //refreshToken == null、fromApi，表示/create-api-key過來的
            //refreshToken != null、!fromApi，表示get-api-key時，發現token過期，重產
            if ((refreshTokenData == null && "fromApi".equals(type)) ||
                    (refreshTokenData != null && "fromApi".equals(type) && "REJECT".equals(refreshTokenData.getReviewResult()))||
                    (refreshTokenData != null && !"fromApi".equals(type))
            ) {
                user.setTokenType("refresh_token");
                refreshTokenMap = RSAJWTUtils.generateTokenExpireInMinutes(user, privateKey, VALID_TIME * 2);  //REFRESH_TOKEN效期先設2天
                //token存redis
                refreshTokenService.saveRefreshToken(userId, toeknMap, refreshTokenMap, "AGREE");
            } else {
                throw new MoiException("該用戶已申請過ApiKey");
            }
        }
        //設定返回給前端的物件
        JwtReponse jwtReponse = new JwtReponse();
        jwtReponse.setRefreshToken(refreshTokenMap == null ? refreshToken.getRefreshToken() : (String) refreshTokenMap.get("token"));
        jwtReponse.setRefreshTokenExpiryDate(refreshTokenMap == null ? String.valueOf(refreshToken.getRefreshTokenExpiryDate()) : (String) refreshTokenMap.get("expiryDate"));
        jwtReponse.setToken(token.getToken());
        jwtReponse.setExpiryDate(token.getExpiryDate());
        refreshTokenService.saveRefreshToken(userId, toeknMap, refreshTokenMap, "AGREE");
        return jwtReponse;
    }


    //終端user持有效的refreshToken，來換取token
    public JwtReponse exchangeForNewToken(String userId) throws Exception {
        User user = new User();
        PrivateKey privateKey = RsaUtils.getPrivateKey((keyComponent.getApPrikeyName()));
        //一般token
        user.setId(userId); //userId要用api帶過來的(如果是審核通過的)
        user.setTokenType("token");
        Map<String, Object> tokenMap = RSAJWTUtils.generateTokenExpireInMinutes(user, privateKey, VALID_TIME); //一般TOKEN效期先設一天
        JwtReponse response = new JwtReponse();
        response.setExpiryDate((String) tokenMap.get("expiryDate")); //refresh_token，效期先設2天
        response.setToken((String) tokenMap.get("token"));
        //更新db裡的token、expiryDate
        refreshTokenService.updateTokenByUserId(userId, response);
        return response;
    }


    /*
    * 單筆查詢 & 單筆軌跡 給聖森更新使用狀況
    * */
    public void singleQuerySystemUpdate(String userId,String type){
        log.info("userId:{}",userId);
        log.info("type:{}",type);
        try {
            String targerUrl = apServerComponent.getTargetUrl() + SYSTEM_UPDATE_URI;
            log.info("targetUrl: {}", targerUrl);

            Path path = Path.of(apServerComponent.getToken());
            String token = Files.readString(path, StandardCharsets.UTF_8);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HashMap<String,String> params = new HashMap<>();
            params.put("userId",userId);
            params.put("type", type);

            String jsonStr = objectMapper.writeValueAsString(params);
            HttpEntity<String> request = new HttpEntity<>(jsonStr, headers);
            log.info("request:{}",request);
            String response = restTemplate.postForObject(targerUrl, request, String.class);
            log.info("response:{}", response);
        }catch (Exception e){
            log.info("e:{}",e.toString());
        }
    }

}
