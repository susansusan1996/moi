package com.example.pentaho.service;

import com.example.pentaho.component.*;
import com.example.pentaho.repository.RefreshTokenRepository;
import com.example.pentaho.utils.StringUtils;
import com.example.pentaho.utils.UserContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.Ref;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RefreshTokenService {
    private static Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private KeyComponent keyComponent;

    @Autowired
    private RedisService redisService;


    @Autowired
    @Qualifier("stringRedisTemplate0")
    private StringRedisTemplate stringRedisTemplate0;

    public RefreshToken saveRefreshToken(String userId,String username,Map<String, Object> tokenMap, Map<String, Object> refreshTokenMap, String reviewResult) throws ParseException {

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(userId);
        refreshToken.setUsername(username);
        refreshToken.setReviewResult(reviewResult);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //表示生成refreshToken - >AGREE
        if (refreshTokenMap != null) {
            refreshToken.setRefreshToken((String) refreshTokenMap.get("token"));
            Date refreshTokenExpiryDate = dateFormat.parse(String.valueOf(refreshTokenMap.get("expiryDate")));
            refreshToken.setRefreshTokenExpiryDate(refreshTokenExpiryDate.toInstant().toString()); //refresh_token，效期先設2天
        }
        //表示生成APIKEY - >AGREE
        if(tokenMap != null){
            refreshToken.setToken((String) tokenMap.get("token"));
            Date expiryDate = dateFormat.parse(String.valueOf(tokenMap.get("expiryDate")));
            refreshToken.setExpiryDate(String.valueOf(expiryDate.toInstant())); //refresh_token，效期先設1天
        }

        //不管REJECT或AGREE都要儲存
        saveRefreshToken(refreshToken);

        return refreshToken;
    }


    /**
     * 檢查db是否有存這筆token
     */
    public List<RefreshToken> findByRefreshTokenAndUserId(RefreshToken refreshToken) {
        return refreshTokenRepository.findByRefreshTokenAndUserId(refreshToken.getRefreshToken(), refreshToken.getId());
    }


    /**
     * 用userId找到該筆API_KEY資訊
     */
    public List<RefreshToken> findByUserId(String userId) {
        return refreshTokenRepository.findById(userId);
    }


    /**
     * 用userId找到該筆API_KEY資訊
     */
    public List<RefreshToken> findByUserIdAndReviewResult(String userId) {
        return refreshTokenRepository.findByUserIdAndReviewResult(userId);
    }

    /**
     * 驗證token是否有過期，過期的話db要刪掉該筆過期的token
     */
    public Boolean verifyExpiration(String userId, String token, String type) {
        String keyName = keyComponent.getApPubkeyName();
        try {
            if (Token.findExpireDateOfToken(token, keyName)) {
                return true;
            }
        } catch (Exception e) {
            deleteToken(userId, type);
            log.info(token + "，" + type + "， token過期");
            return false;
        }
        return false;
    }


    public void deleteToken(String id, String type) {
        if (StringUtils.isNotNullOrEmpty(id)) {
            //判斷刪哪種token
            if ("token".equals(type)) {
                stringRedisTemplate0.delete(id + ":token");
                stringRedisTemplate0.delete(id + ":expiry_date");
            } else {
                stringRedisTemplate0.delete(id + ":refresh_token");
                stringRedisTemplate0.delete(id + ":refresh_token_expiry_date");
            }
        }
    }


    public void updateByUserId(String userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(userId);
        refreshToken.setReviewResult("REJECT");
        refreshTokenRepository.updateByUserId(refreshToken);
    }


    /**
     * redis存refresh_token
     */
    public void saveRefreshToken(RefreshToken refreshToken) {
        String id = refreshToken.getId();
        Map<String, String> valuesToSet = new HashMap<>();
        valuesToSet.put("username",refreshToken.getUsername());
        valuesToSet.put("token", refreshToken.getToken());
        valuesToSet.put("refresh_token", refreshToken.getRefreshToken());
        valuesToSet.put("expiry_date", refreshToken.getExpiryDate());
        valuesToSet.put("refresh_token_expiry_date", refreshToken.getRefreshTokenExpiryDate());
        valuesToSet.put("review_result", refreshToken.getReviewResult());
        for (Map.Entry<String, String> entry : valuesToSet.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null) {
                stringRedisTemplate0.opsForValue().set(id + ":" + key, value);
            } else {
                /*REJECT時會空**/
                stringRedisTemplate0.delete(id + ":" + key);
            }
        }
        stringRedisTemplate0.opsForValue().set(id + ":create_timestamp", Instant.now().toString());
    }



    public void updateTokenByUserId(String id, JwtReponse response) {
        if (StringUtils.isNotNullOrEmpty(id)) {
            stringRedisTemplate0.opsForValue().set(id + ":token", response.getToken());
            stringRedisTemplate0.opsForValue().set(id + ":expiry_date", response.getExpiryDate());
            stringRedisTemplate0.opsForValue().set(id + ":create_timestamp", Instant.now().toString());
        }
    }


    public RefreshToken findRefreshTokenByUserId(String id,String username) {
        RefreshToken refreshToken = new RefreshToken();
        if (StringUtils.isNotNullOrEmpty(id)) {
            String reviewResult = stringRedisTemplate0.opsForValue().get(id + ":review_result");
            if ("AGREE".equals(reviewResult)) {
                /**表示已申請成功*/
                refreshToken.setId(id);
                refreshToken.setToken(stringRedisTemplate0.opsForValue().get(id + ":token"));
                refreshToken.setRefreshToken(stringRedisTemplate0.opsForValue().get(id + ":refresh_token"));
                refreshToken.setExpiryDate(stringRedisTemplate0.opsForValue().get(id + ":expiry_date"));
                refreshToken.setRefreshTokenExpiryDate(stringRedisTemplate0.opsForValue().get(id + ":refresh_token_expiry_date"));
                refreshToken.setReviewResult(stringRedisTemplate0.opsForValue().get(id + ":review_result"));
                return refreshToken;
            }else if("REJECT".equals(reviewResult)){
                /**表示過去被拒絕，這次重新申請*/
                refreshToken.setId(id);
                refreshToken.setUsername(username);
                refreshToken.setReviewResult(reviewResult);
                return refreshToken;
            }else{
                /*表示第一次申請**/
                return null;
            }
        }
        /*參數不符規定，開頭就會擋掉了**/
        return null;
    }
}
