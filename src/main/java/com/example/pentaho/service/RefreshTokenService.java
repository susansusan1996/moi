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

    /**
     *
     * @param applicantId
     * @param tokenMap
     * @param refreshTokenMap
     * @param reviewResult
     * @return
     * @throws ParseException
     */
    public RefreshToken saveRefreshToken(String applicantId, String applicantName,Map<String, Object> tokenMap, Map<String, Object> refreshTokenMap, String reviewResult) throws ParseException {
        RefreshToken refreshToken = new RefreshToken();
        /*不管 AGREE或REJECT 都要存的資料 */
        refreshToken.setId(applicantId);
        refreshToken.setName(applicantName);
        refreshToken.setReviewResult(reviewResult);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        /*表示AGREE*/
        if (refreshTokenMap != null) {
            refreshToken.setRefreshToken((String) refreshTokenMap.get("token"));
            Date refreshTokenExpiryDate = dateFormat.parse(String.valueOf(refreshTokenMap.get("expiryDate")));
            refreshToken.setRefreshTokenExpiryDate(refreshTokenExpiryDate.toInstant().toString()); //refresh_token，效期先設2天
        }
        /*表示AGREE*/
        if(tokenMap != null){
            refreshToken.setToken((String) tokenMap.get("token"));
            Date expiryDate = dateFormat.parse(String.valueOf(tokenMap.get("expiryDate")));
            refreshToken.setExpiryDate(String.valueOf(expiryDate.toInstant())); //acess_token，效期先設1天
        }

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
     * REJECT的會只有reviewResult,userId有值，其餘""
     *
     */
    public void saveRefreshToken(RefreshToken refreshToken) {
        String id = refreshToken.getId();
        Map<String, String> valuesToSet = new HashMap<>();
        valuesToSet.put("username",refreshToken.getName());
        valuesToSet.put("token", refreshToken.getToken());
        valuesToSet.put("refresh_token", refreshToken.getRefreshToken());
        valuesToSet.put("expiry_date", refreshToken.getExpiryDate());
        valuesToSet.put("refresh_token_expiry_date", refreshToken.getRefreshTokenExpiryDate());
        valuesToSet.put("review_result", refreshToken.getReviewResult());
        for (Map.Entry<String, String> entry : valuesToSet.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null) {
                //建立一個set,key為applicantId:token",並放入對應的value
                stringRedisTemplate0.opsForValue().set(id + ":" + key, value);
            } else {
                //當REJECT時，token,refresh_token,expiry_date,refresh_token_expiry_date會沒有值，所以要刪除
                stringRedisTemplate0.delete(id + ":" + key);
            }
        }
        //REJECT || AGREE　都要存建立時間
        stringRedisTemplate0.opsForValue().set(id + ":create_timestamp", Instant.now().toString());
    }



    public void updateTokenByUserId(String id, JwtReponse response) {
        if (StringUtils.isNotNullOrEmpty(id)) {
            stringRedisTemplate0.opsForValue().set(id + ":token", response.getToken());
            stringRedisTemplate0.opsForValue().set(id + ":expiry_date", response.getExpiryDate());
            stringRedisTemplate0.opsForValue().set(id + ":create_timestamp", Instant.now().toString());
        }
    }


    public RefreshToken findRefreshTokenByUserId(String id) {
        RefreshToken refreshToken = new RefreshToken();
        if (StringUtils.isNotNullOrEmpty(id)) {
            String reviewResult = stringRedisTemplate0.opsForValue().get(id + ":review_result");
            //表示過去申請成功
            if ("AGREE".equals(reviewResult)) {
                refreshToken.setId(id);
                refreshToken.setName(stringRedisTemplate0.opsForValue().get(id + ":username"));
                refreshToken.setToken(stringRedisTemplate0.opsForValue().get(id + ":token"));
                refreshToken.setRefreshToken(stringRedisTemplate0.opsForValue().get(id + ":refresh_token"));
                refreshToken.setExpiryDate(stringRedisTemplate0.opsForValue().get(id + ":expiry_date"));
                refreshToken.setRefreshTokenExpiryDate(stringRedisTemplate0.opsForValue().get(id + ":refresh_token_expiry_date"));
                refreshToken.setReviewResult(stringRedisTemplate0.opsForValue().get(id + ":review_result"));
                return refreshToken;
            }else if ("REJECT".equals(reviewResult)){
                //表示過去申請拒絕
                refreshToken.setId(id);
                refreshToken.setReviewResult(stringRedisTemplate0.opsForValue().get(id + ":review_result"));
                return refreshToken;
            }
            //表示之前沒申請過
            return null;
        }
        //表示之前沒申請過
        return null;
    }
}
