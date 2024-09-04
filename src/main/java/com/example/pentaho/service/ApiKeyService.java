package com.example.pentaho.service;

import com.example.pentaho.component.*;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.utils.RSAJWTUtils;
import com.example.pentaho.utils.RsaUtils;
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


    /**
     * 返回 userID 的 金鑰
     * (1) 金鑰過期 -> 刷新金鑰無過期 -> 生成新的金鑰 -> 更新 redis -> 返回
     * (2) 金鑰過期 -> 刷新金鑰過期 -> 生成刷新金鑰 + 金鑰  -> 更新 redis -> 返回
     * @param userId
     * @param username
     * @return
     * @throws Exception
     */
    public JwtReponse getApiKey(String userId,String username) throws Exception {
        JwtReponse jwtReponse = new JwtReponse();
        /*用userId找資料把redis中的集合拿出來~*/
        RefreshToken refreshToken = refreshTokenService.findRefreshTokenByUserId(userId,username);
        /*表示有資料**/
        if (refreshToken != null) {
            //REJECT -> redis有2組:id:review_result,id:username
            if(StringUtils.isNotNullOrEmpty(refreshToken.getReviewResult()) && "REJECT".equals(refreshToken.getReviewResult())){
                jwtReponse.setId(userId);
                jwtReponse.setReviewResult(refreshToken.getReviewResult());
                return jwtReponse;
            }

            /**AGREE -> redis有7組
             * id:username
             * id:review_result
             * id:token
             * id:create_timestamp
             * id:expiry_date
             * id:refresh_token
             * id:refresh_token_expiry_date
             * **/
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
                //token過期,檢查refresh_token有無過期
                //refreshToken沒有過期，卷token即可
                if (refreshTokenService.verifyExpiration(userId, refreshToken.getRefreshToken(), "refresh_token")) {
                    log.info("token過期，檢查refreshToken沒有過期,重新卷token");
                    return createApiKey(userId,username,refreshToken, "");
                }
                //refreshToken也過期，全部重卷
                log.info("token過期，檢查refreshToken也過期,全部重新眷");
                return createApiKey(userId, username,null, "");
            }
        }
        //沒有資訊，要申請
        return jwtReponse;
    }

    /**
     * AGREE 才會進來這裡
     * @param userId 金鑰申請者id
     * @param username 金鑰申請者名稱
     * @param refreshToken
     * @param type
     * @return
     * @throws Exception
     */
    public JwtReponse createApiKey(String userId,String username,RefreshToken refreshToken, String type) throws Exception {
        PrivateKey privateKey = RsaUtils.getPrivateKey((keyComponent.getApPrikeyName()));
        /*token的payload**/
        User user = new User();
        user.setId(userId);
        user.setTokenType("token");
        Map<String, Object> toeknMap = RSAJWTUtils.generateTokenExpireInMinutes(user, privateKey, VALID_TIME);
        Token token = new Token((String) toeknMap.get("token"), (String) toeknMap.get("expiryDate"));

        //refresh_token
        Map<String, Object> refreshTokenMap = null;

        /***
         * refreshToken == null
         * (1) 從 /create-api-key 接口進入，type="fromApi"
         * (2) 從 /get-api-key 街口進入，且apikey + refreshToken都過期重眷 type=""
         * ===========================================================
         * refreshToken != null
         * (1) 從 /get-api-key 街口進入，refreshToken沒有過期，重新眷apikey type=""
         * */
        RefreshToken refreshTokenFromRedis = refreshToken;
        if (refreshToken == null) {
            if ("fromApi".equals(type)) {
            //todo:檢查有沒有refreshToken
             refreshTokenFromRedis = refreshTokenService.findRefreshTokenByUserId(userId,username);
            }
            /**
             * 用userId反查redis是否已申請過後，判斷下方條件
             * (1) type="fromApi ==> /create-api-key 進來 ; refreshTokenData == null ==> 表示之前從未申請過  生成一整組~
             * (2) type="fromApi ==> /create-api-key 進來 ; refreshTokenData != null ==> 表示之前申請過 ; refreshTokenFromRedis.getReviewResult()="REJECT" ==> 表示之前申請被拒絕，這次被AGREE 生成一整組
             * (3) type="" ==> /get-api-key 近來 ; refreshTokenData != null ==> 表示之前申請過、!fromApi(從/get-api-key接口來的)，表示get-api-key時，發現refresh_token也過期，重產refresh_token
             * */
            if ((refreshTokenFromRedis == null && "fromApi".equals(type)) ||
                (refreshTokenFromRedis != null && "fromApi".equals(type) && "REJECT".equals(refreshTokenFromRedis.getReviewResult()))||
                (refreshTokenFromRedis == null && !"fromApi".equals(type))
            ) {
                /**refreshToken 的 playload*/
                user.setTokenType("refresh_token");
                /**生成refreshToken refreshTokenMap ={tokem:xxxxx,exprirDate:XXXX} */
                refreshTokenMap = RSAJWTUtils.generateTokenExpireInMinutes(user, privateKey, VALID_TIME * 2);  //REFRESH_TOKEN效期先設2天
            } else {
                //
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
