package com.example.pentaho.service;

import com.example.pentaho.component.*;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.utils.RSAJWTUtils;
import com.example.pentaho.utils.RsaReadUtils;
import com.example.pentaho.utils.StringUtils;
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
    public JwtReponse getApiKey(String userId,String username) throws Exception {
        JwtReponse jwtReponse = new JwtReponse();
        /*用userId找資料*/
        RefreshToken refreshToken = refreshTokenService.findRefreshTokenByUserId(userId,username);
        /*表示有資料**/
        if (refreshToken != null) {
            //REJECT 表示什麼都沒有
            if(StringUtils.isNotNullOrEmpty(refreshToken.getReviewResult()) && "REJECT".equals(refreshToken.getReviewResult())){
                jwtReponse.setId(userId);
                jwtReponse.setReviewResult(refreshToken.getReviewResult());
                return jwtReponse;
            }

            //AGREE 表示會有token & refreshToken
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
                    return createApiKey(userId,username,refreshToken, "");
                }
                //refreshToken也過期，全部重卷
                return createApiKey(userId, username,null, "");
            }
        }
        //沒有資訊，要申請
        return jwtReponse;
    }

    /**
     * AGREE 才會進來這裡
     * @param userId
     * @param refreshToken
     * @param type
     * @return
     * @throws Exception
     */
    public JwtReponse createApiKey(String userId,String username,RefreshToken refreshToken, String type) throws Exception {
        PrivateKey privateKey = RsaUtils.getPrivateKey((keyComponent.getApPrikeyName()));
        /*用來用token的playload**/
        User user = new User();
        user.setId(userId);
        //表示這組token是APIKEY(acessToken)
        user.setTokenType("token");
        //產生APIKEY效期先設一天,tokenMap:{token:XXXX,expiryDate:XXXXX}
        Map<String, Object> toeknMap = RSAJWTUtils.generateTokenExpireInMinutes(user, privateKey, VALID_TIME);
        //已用Token物件
        Token token = new Token((String) toeknMap.get("token"), (String) toeknMap.get("expiryDate"));

        //refresh_token
        Map<String, Object> refreshTokenMap = null;
        /***
         * refreshToken==null
         * (1) 從 /create-api-key 接口進入
         * (2) 從 /get-api-key 街口進入，且apikey + refreshToken都過期重眷
         * refreshToken != null
         * (1) 從 /get-api-key 街口進入，refreshToken沒有過期，重新眷apikey
         * */
        if (refreshToken == null) {
            RefreshToken refreshTokenData = refreshTokenService.findRefreshTokenByUserId(userId,username);
            /**
             * 用userId反查redis是否已申請過後，判斷下方條件
             * (1) refreshTokenData == null、fromApi，表示/create-api-key 接口過來的
             * (2) refreshTokenData != null(有id,username,reviewResult)、fromApi、"REJECT"，表示之前有被拒絕過，又重新AGREE的申請
             * (3) refreshTokenData != null(userId撈出發現已過期)、!fromApi(從/get-api-key接口來的)，表示get-api-key時，發現refresh_token也過期，重產refresh_token
             * */
            if ((refreshTokenData == null && "fromApi".equals(type)) ||
                    (refreshTokenData != null && "fromApi".equals(type) && "REJECT".equals(refreshTokenData.getReviewResult()))||
                    (refreshTokenData != null && !"fromApi".equals(type))
            ) {
                /**refreshToken 的 playload*/
                user.setTokenType("refresh_token");
                /**生成refreshToken refreshTokenMap ={tokem:xxxxx,exprirDate:XXXX} */
                refreshTokenMap = RSAJWTUtils.generateTokenExpireInMinutes(user, privateKey, VALID_TIME * 2);  //REFRESH_TOKEN效期先設2天
            } else {
                throw new MoiException("該用戶已申請過ApiKey");
            }
        }
        //設定返回給前端的物件
        JwtReponse jwtReponse = new JwtReponse();
        /**是否有重新生成refreshToken*/
        jwtReponse.setRefreshToken(refreshTokenMap == null ? refreshToken.getRefreshToken() : (String) refreshTokenMap.get("token"));
        jwtReponse.setRefreshTokenExpiryDate(refreshTokenMap == null ? String.valueOf(refreshToken.getRefreshTokenExpiryDate()) : (String) refreshTokenMap.get("expiryDate"));
        jwtReponse.setToken(token.getToken());
        jwtReponse.setExpiryDate(token.getExpiryDate());
        refreshTokenService.saveRefreshToken(userId,username,toeknMap, refreshTokenMap, "AGREE");
        return jwtReponse;
    }


    //終端user持有效的refreshToken，來換取token
    public JwtReponse exchangeForNewToken(String userId) throws Exception {
        User user = new User();
        PrivateKey privateKey = RsaReadUtils.getPrivateKey((keyComponent.getApPrikeyName()));
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
