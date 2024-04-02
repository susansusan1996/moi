package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.component.RefreshToken;
import com.example.pentaho.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

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
                .append("insert into ADDR_ODS.REFRESH_TOKEN (id, refresh_token, expiry_date)")
                .append("VALUES (:id, :refreshtoken, CAST(:expirydate AS DATETIME))", refreshToken.getId(), refreshToken.getToken(), java.sql.Timestamp.from(refreshToken.getExpiryDate()))
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
    public List<RefreshToken> findByRefreshToken(String refreshToken) {
        Query query = Query.builder()
                .append("SELECT *")
                .append("FROM ADDR_ODS.REFRESH_TOKEN WHERE refresh_token = :refreshToken", refreshToken)
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        return sqlExecutor.queryForList(query, RefreshToken.class);
    }

    @Override
    public void deleteByRefreshToken(RefreshToken refreshToken) {
        Query query = Query.builder()
                .append("delete FROM ADDR_ODS.REFRESH_TOKEN")
                .append("WHERE refresh_token = :refreshToken", refreshToken.getToken())
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        sqlExecutor.delete(query);
    }

}
