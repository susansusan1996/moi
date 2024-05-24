package com.example.pentaho.service;

import com.example.pentaho.component.*;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.utils.RSAJWTUtils;
import com.example.pentaho.utils.RsaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.security.PrivateKey;
import java.util.Map;

@Service
public class ApiKeyService {
    private static Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    @Autowired
    private KeyComponent keyComponent;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RedisService redisService;

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
                log.info("token沒過期，直接返回資訊");
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
                    log.info("token過期，重新卷token");
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
            //refreshTokenData == null、fromApi，表示/create-api-key過來的
            //refreshTokenData != null、fromApi、"REJECT"，表示之前有被拒絕過，又重新AGREE的申請
            //refreshTokenData != null、!fromApi，表示get-api-key時，發現refresh_token也過期，重產refresh_token
            if ((refreshTokenData == null && "fromApi".equals(type)) ||
                    (refreshTokenData != null && "fromApi".equals(type) && "REJECT".equals(refreshTokenData.getReviewResult()))||
                    (refreshTokenData != null && !"fromApi".equals(type))
            ) {
                user.setTokenType("refresh_token");
                refreshTokenMap = RSAJWTUtils.generateTokenExpireInMinutes(user, privateKey, VALID_TIME * 2);  //REFRESH_TOKEN效期先設2天
//                //token存redis
//                refreshTokenService.saveRefreshToken(userId, toeknMap, refreshTokenMap, "AGREE");
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



}
