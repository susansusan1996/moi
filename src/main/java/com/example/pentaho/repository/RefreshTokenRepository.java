package com.example.pentaho.repository;

import com.example.pentaho.component.JwtReponse;
import com.example.pentaho.component.RefreshToken;
import org.springframework.stereotype.Repository;

import java.text.ParseException;
import java.util.List;

@Repository
public interface RefreshTokenRepository {

    void saveRefreshToken(RefreshToken refreshToken);

    List<RefreshToken> findById(String id);

    List<RefreshToken> findByRefreshTokenAndUserId(String refreshToken, String id);

    void deleteByRefreshToken(String refreshToken);

    void deleteByToken(String token);

    void updateTokenByUserId(String id, JwtReponse reponse) throws ParseException;

    void updateByUserId(String userId);


}
