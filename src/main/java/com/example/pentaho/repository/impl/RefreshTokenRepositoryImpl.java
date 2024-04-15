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

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                .append("insert into ADDR_ODS.REFRESH_TOKEN (id, refresh_token, refresh_token_expiry_date, token ,expiry_date,create_timestamp, review_result )")
                .append("VALUES (:id, :refreshtoken, CAST(:refreshtokenexpirydate AS DATETIME), :token, CAST(:expirydate AS DATETIME), :now, 'AGREE' )",
                        refreshToken.getId(),
                        refreshToken.getRefreshToken(), refreshToken.getRefreshTokenExpiryDate() == null ? null : java.sql.Timestamp.from(refreshToken.getRefreshTokenExpiryDate()),
                        refreshToken.getToken(), refreshToken.getExpiryDate() == null ? null : java.sql.Timestamp.from(refreshToken.getExpiryDate()),
                        java.sql.Timestamp.from(Instant.now())
                )
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
    public List<RefreshToken> findByUserIdAndReviewResult(String id) {
            Query query = Query.builder()
                    .append("SELECT * ")
                    .append("FROM ADDR_ODS.REFRESH_TOKEN WHERE id = :id AND REVIEW_RESULT = 'AGREE'" , id)
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

    @Override
    public void updateByUserId(RefreshToken refreshToken) {
        Map<String,Object> map = new HashMap<>();
        map.put("refresh_token",refreshToken.getRefreshToken());
        map.put("token",refreshToken.getToken());
        map.put("refresh_token_expiry_date", refreshToken.getRefreshTokenExpiryDate() != null ? java.sql.Timestamp.from(refreshToken.getRefreshTokenExpiryDate()) : null);
        map.put("expiry_Date", refreshToken.getExpiryDate() != null ? java.sql.Timestamp.from(refreshToken.getExpiryDate()) : null);
        map.put("review_result",refreshToken.getReviewResult());
        map.put("create_timestamp",java.sql.Timestamp.from(Instant.now()));
        map.put("id",refreshToken.getId());
        Query query = Query.builder()
                .append("update ADDR_ODS.REFRESH_TOKEN set refresh_token = :refresh_token , token = :token, refresh_token_expiry_date = :refresh_token_expiry_date, expiry_Date = :expiry_Date, review_result = :review_result , create_timestamp = :create_timestamp")
                .append("WHERE id = :id")
                .putAll(map)
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        sqlExecutor.update(query);
    }

}
