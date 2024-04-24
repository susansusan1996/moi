package com.example.pentaho.service;

import com.example.pentaho.component.*;
import com.example.pentaho.repository.RefreshTokenRepository;
import com.example.pentaho.utils.UserContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    public RefreshToken saveRefreshToken(String id, Map<String, Object> tokenMap, Map<String, Object> refreshTokenMap, String reviewResult) throws ParseException {
        RefreshToken refreshToken = new RefreshToken();
        if("AGREE".equals(reviewResult)) {
            User user = UserContextUtils.getUserHolder();
            log.info("user:{}", user);
            refreshToken.setId(id);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (refreshTokenMap != null) {
                refreshToken.setRefreshToken((String) refreshTokenMap.get("token"));
                Date refreshTokenExpiryDate = dateFormat.parse(String.valueOf(refreshTokenMap.get("expiryDate")));
                refreshToken.setRefreshTokenExpiryDate(refreshTokenExpiryDate.toInstant().toString()); //refresh_token，效期先設2天
            }
            refreshToken.setToken((String) tokenMap.get("token"));
            Date expiryDate = dateFormat.parse(String.valueOf(tokenMap.get("expiryDate")));
            refreshToken.setExpiryDate(String.valueOf(expiryDate.toInstant())); //refresh_token，效期先設1天
            refreshToken.setReviewResult(reviewResult);
            redisService.saveRefreshToken(refreshToken);
        }
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
            redisService.deleteToken(userId, type);
            log.info(token + "，" + type + "， token過期");
            return false;
        }
        return false;
    }

    public void updateTokenByUserId(String userId, JwtReponse reponse) throws ParseException {
        refreshTokenRepository.updateTokenByUserId(userId, reponse);
    }

    public void updateByUserId(String userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(userId);
        refreshToken.setReviewResult("REJECT");
        refreshTokenRepository.updateByUserId(refreshToken);
    }
}
