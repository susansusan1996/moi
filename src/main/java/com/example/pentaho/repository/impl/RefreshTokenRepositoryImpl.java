package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.component.JwtReponse;
import com.example.pentaho.component.RefreshToken;
import com.example.pentaho.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenRepositoryImpl.class);
    private final SqlExecutor sqlExecutor;

    public RefreshTokenRepositoryImpl(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    @Override
    public void saveRefreshToken(RefreshToken refreshToken) {
        Query query = Query.builder()
                .append("insert into ADDR_ODS.REFRESH_TOKEN (id, refresh_token, refresh_token_expiry_date, token ,expiry_date )")
                .append("VALUES (:id, :refreshtoken, CAST(:refreshtokenexpirydate AS DATETIME), :token, CAST(:expirydate AS DATETIME))",
                        refreshToken.getId(),
                        refreshToken.getRefreshToken(), java.sql.Timestamp.from(refreshToken.getRefreshTokenExpiryDate()),
                        refreshToken.getToken(), java.sql.Timestamp.from(refreshToken.getExpiryDate()))
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        sqlExecutor.insert(query);
    }

    @Override
    public List<RefreshToken> findById(String id) {
        Query query = Query.builder()
                .append("SELECT * ")
                .append("FROM ADDR_ODS.REFRESH_TOKEN WHERE id = :id", id)
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        return sqlExecutor.queryForList(query, RefreshToken.class);
    }

    @Override
    public List<RefreshToken> findByRefreshTokenAndUserId(String refreshToken, String id) {
        Query query = Query.builder()
                .append("SELECT *")
                .append("FROM ADDR_ODS.REFRESH_TOKEN WHERE refresh_token = :refreshToken and id = :id", refreshToken, id)
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        return sqlExecutor.queryForList(query, RefreshToken.class);
    }

    @Override
    public void deleteByRefreshToken(String refreshToken) {
        Query query = Query.builder()
                .append("delete FROM ADDR_ODS.REFRESH_TOKEN")
                .append("WHERE refresh_token = :refreshToken", refreshToken)
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        sqlExecutor.delete(query);
    }

    @Override
    public void deleteByToken(String token) {
        Query query = Query.builder()
                .append("delete FROM ADDR_ODS.REFRESH_TOKEN")
                .append("WHERE token = :token", token)
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        sqlExecutor.delete(query);
    }

    @Override
    public void updateTokenByUserId(String userId, JwtReponse reponse) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date expiryDate = dateFormat.parse(String.valueOf(reponse.getExpiryDate()));
        Query query = Query.builder()
                .append("update ADDR_ODS.REFRESH_TOKEN set token = :token , expiry_Date = :expiryDate", reponse.getToken(), java.sql.Timestamp.from(expiryDate.toInstant()))
                .append("WHERE id = :id", userId)
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        sqlExecutor.update(query);
    }

}
