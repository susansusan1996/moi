package com.example.pentaho.repository;

import com.example.pentaho.component.RefreshToken;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefreshTokenRepository {

    void saveRefreshToken(RefreshToken refreshToken);

    List<RefreshToken> findById(String id);

    List<RefreshToken> findByRefreshToken(String id);

    void deleteByRefreshToken(RefreshToken refreshToken);

}
