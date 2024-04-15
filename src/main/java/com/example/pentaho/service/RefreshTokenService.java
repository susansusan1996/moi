package com.example.pentaho.service;

import com.example.pentaho.component.*;
import com.example.pentaho.repository.RefreshTokenRepository;
import com.example.pentaho.utils.UserContextUtils;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
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

    public RefreshToken saveRefreshToken(String id, Map<String, Object> tokenMap, Map<String, Object> refreshTokenMap, String reviewResult) throws ParseException {
        RefreshToken refreshToken = new RefreshToken();
        if("AGREE".equals(reviewResult)){
            User user = UserContextUtils.getUserHolder();
            log.info("user:{}", user);
            refreshToken.setId(id);
            refreshToken.setRefreshToken((String) refreshTokenMap.get("token"));
            refreshToken.setToken((String) tokenMap.get("token"));
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date expiryDate = dateFormat.parse(String.valueOf(tokenMap.get("expiryDate")));
            Date refreshTokenExpiryDate = dateFormat.parse(String.valueOf(refreshTokenMap.get("expiryDate")));
            refreshToken.setRefreshTokenExpiryDate(refreshTokenExpiryDate.toInstant()); //refresh_token，效期先設2天
            refreshToken.setExpiryDate(expiryDate.toInstant()); //refresh_token，效期先設1天
            refreshToken.setReviewResult(reviewResult);
            if(refreshTokenRepository.findById(id).isEmpty()){
                refreshTokenRepository.saveRefreshToken(refreshToken);
            }else{
                refreshTokenRepository.updateByUserId(refreshToken);
            }
        }else{
            refreshToken.setId(id);
            refreshTokenRepository.saveRefreshToken(refreshToken);
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
    public Boolean verifyExpiration(String token, String type) {
        String keyName = keyComponent.getApPubkeyName();
        try {
            if (Token.findExpireDateOfToken(token, keyName)) {
                return true;
            }
        } catch (ExpiredJwtException e) {
            log.info(e.toString());
            if("token".equals(type)){
                refreshTokenRepository.deleteByToken(token);
            }else{
                refreshTokenRepository.deleteByRefreshToken(token);
            }
            log.info(token + "，" + type + "， token過期");
            throw e;
        } catch (FileNotFoundException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.info(e.toString());
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
