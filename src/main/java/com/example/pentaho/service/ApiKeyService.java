package com.example.pentaho.service;

import com.example.pentaho.component.*;
import com.example.pentaho.utils.RSAJWTUtils;
import com.example.pentaho.utils.RsaUtils;
import com.example.pentaho.utils.UserContextUtils;
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

    private final int VALID_TIME = 1440;

    public JwtReponse getApiKey(String userId ) throws Exception {
        User user = UserContextUtils.getUserHolder();
        log.info("user:{}", user);
        PrivateKey privateKey = RsaUtils.getPrivateKey((keyComponent.getApPrikeyName()));
        Map<String, Object> map = RSAJWTUtils.generateTokenExpireInMinutes(user, privateKey, VALID_TIME);
        Map<String, Object> refreshTokenMap = RSAJWTUtils.generateTokenExpireInMinutes(user, privateKey, 1);
        String refreshToken = (String) refreshTokenMap.get("token");
        Token token = new Token((String) map.get("token"), (String) map.get("expiryDate"));
        refreshTokenService.saveRefreshToken( user == null? userId: user.getId(), refreshToken);
        JwtReponse jwtReponse = new JwtReponse();
        jwtReponse.setRefreshToken((String) refreshTokenMap.get("token"));
        jwtReponse.setRefreshTokenExpiryDate((String) refreshTokenMap.get("expiryDate"));
        jwtReponse.setToken(token.getToken());
        jwtReponse.setExpiryDate(token.getExpiryDate());
        return jwtReponse;
    }
}
