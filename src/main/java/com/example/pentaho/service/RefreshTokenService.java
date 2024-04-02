package com.example.pentaho.service;

import com.example.pentaho.component.RefreshToken;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    public RefreshToken createRefreshToken(String id) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(id);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plus(Duration.ofDays(1))); //refresh_token，效期先設一天
        refreshTokenRepository.saveRefreshToken(refreshToken);
        return refreshToken;
    }


    /**
     * 檢查db是否有存這筆token
     */
    public List<RefreshToken> findByRefreshToken(RefreshToken refreshToken) {
        return refreshTokenRepository.findByRefreshToken(refreshToken.getToken());
    }

    /**
     * 驗證refreshtoken是否有過期，過期的話db要刪掉該筆過期的refreshtoken
     */
    public Boolean verifyExpiration(RefreshToken refreshToken) {
        if (refreshToken.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.deleteByRefreshToken(refreshToken);
            throw new MoiException(refreshToken.getToken() + "， refreshToken過期");
        }
        return true;
    }
}
