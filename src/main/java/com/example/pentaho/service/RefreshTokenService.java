package com.example.pentaho.service;

import com.example.pentaho.component.KeyComponent;
import com.example.pentaho.component.RefreshToken;
import com.example.pentaho.component.Token;
import com.example.pentaho.component.User;
import com.example.pentaho.exception.MoiException;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private static Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private KeyComponent keyComponent;

    public RefreshToken saveRefreshToken(String id, String token) {
        User user = UserContextUtils.getUserHolder();
        log.info("user:{}", user);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(id);
        refreshToken.setRefreshToken(token);
        refreshToken.setExpiryDate(Instant.now().plus(Duration.ofDays(2))); //refresh_token，效期先設一天
        refreshTokenRepository.saveRefreshToken(refreshToken);
        return refreshToken;
    }


    /**
     * 檢查db是否有存這筆token
     */
    public List<RefreshToken> findByRefreshToken(RefreshToken refreshToken) {
        return refreshTokenRepository.findByRefreshTokenAndUserId(refreshToken.getRefreshToken(), refreshToken.getId());
    }

    /**
     * 驗證refreshtoken是否有過期，過期的話db要刪掉該筆過期的refreshtoken
     */
    public Boolean verifyExpiration(RefreshToken refreshToken) {
        String keyName = keyComponent.getApPubkeyName();
        try {
            if (Token.findExpireDateOfRefreshToken(refreshToken.getRefreshToken(), keyName)) {
                return true;
            }
        } catch (ExpiredJwtException e) {
            log.info(e.toString());
            refreshTokenRepository.deleteByRefreshToken(refreshToken);
            throw new MoiException(refreshToken.getRefreshToken() + "， refreshToken過期");
        } catch (FileNotFoundException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.info(e.toString());
        }
        return false;
    }
}
