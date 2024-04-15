package com.example.pentaho.service;

import com.example.pentaho.component.*;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.utils.RSAJWTUtils;
import com.example.pentaho.utils.RsaUtils;
import com.example.pentaho.utils.UserContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class ApiKeyService {
    private static Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    @Autowired
    private KeyComponent keyComponent;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private final int VALID_TIME = 1440;

    //返回該userID的api key
    //同時檢查token有沒有過期，有過期的話，看refresh_token有沒有過期，refresh_token沒有過期重新卷token即可
    //refresh_token有過期，整組要重建，並返回給前端
    public JwtReponse getApiKey(String userId) throws Exception {
        List<RefreshToken> refreshTokens =  refreshTokenService.findByUserId(userId);
        RefreshToken refreshToken;
        JwtReponse jwtReponse = new JwtReponse();
        if(!refreshTokens.isEmpty()){
            refreshToken = refreshTokens.get(0);
            //token沒有過期
            if(refreshTokenService.verifyExpiration(refreshToken.getToken(),"token")){
                //直接返回即可
                jwtReponse.setRefreshToken(refreshToken.getRefreshToken());
                jwtReponse.setRefreshTokenExpiryDate(String.valueOf(refreshToken.getRefreshTokenExpiryDate()));
                jwtReponse.setToken(refreshToken.getToken());
                jwtReponse.setExpiryDate(String.valueOf(refreshToken.getExpiryDate()));
                return jwtReponse;
            }else{
                //token過期
                //檢查refresh_token有無過期
                if(refreshTokenService.verifyExpiration(refreshToken.getRefreshToken(),"refresh_token")){
                    //refreshToken沒有過期，卷token即可
                    return createApiKey(userId, refreshToken);
                }else {
                    //refreshToken也過期，全部重卷
                    return createApiKey(userId, null);
                }
            }
        }
        return jwtReponse;
    }


    public JwtReponse createApiKey(String userId, RefreshToken refreshToken) throws Exception {
        User user = new User();
        PrivateKey privateKey = RsaUtils.getPrivateKey((keyComponent.getApPrikeyName()));
        //一般token
        user.setId(userId); //userId要用api帶過來的(如果是審核通過的)
        user.setTokenType("token");
        Map<String, Object> toeknMap = RSAJWTUtils.generateTokenExpireInMinutes(user, privateKey, VALID_TIME); //一般TOKEN效期先設一天
        Token token = new Token((String) toeknMap.get("token"), (String) toeknMap.get("expiryDate"));
        //refresh_token
        Map<String, Object> refreshTokenMap = null;
        //refreshToken == null，表示全新的申請
        if (refreshToken == null) {
            List<RefreshToken>  refreshTokens = refreshTokenService.findByUserIdAndReviewResult(userId);
            //找不到該userId相對應的TOKEN資料，才需要再存一筆新的，不然會重複申請
            if(refreshTokens.isEmpty()){
                user.setTokenType("refresh_token");
                refreshTokenMap = RSAJWTUtils.generateTokenExpireInMinutes(user, privateKey, VALID_TIME*2);  //REFRESH_TOKEN效期先設2天
                //token存table
                refreshTokenService.saveRefreshToken(userId, toeknMap, refreshTokenMap, "AGREE");
            }else{
                throw new MoiException("該用戶已申請過ApiKey");
            }
        }
        //設定返回給前端的物件
        JwtReponse jwtReponse = new JwtReponse();
        jwtReponse.setRefreshToken(refreshTokenMap == null ? refreshToken.getRefreshToken() : (String) refreshTokenMap.get("token"));
        jwtReponse.setRefreshTokenExpiryDate(refreshTokenMap == null ? String.valueOf(refreshToken.getExpiryDate()) : (String) refreshTokenMap.get("expiryDate"));
        jwtReponse.setToken(token.getToken());
        jwtReponse.setExpiryDate(token.getExpiryDate());
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
