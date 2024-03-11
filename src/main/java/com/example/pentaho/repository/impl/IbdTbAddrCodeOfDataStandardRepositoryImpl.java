package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.repository.IbdTbAddrCodeOfDataStandardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class IbdTbAddrCodeOfDataStandardRepositoryImpl implements IbdTbAddrCodeOfDataStandardRepository {
    private static final Logger log = LoggerFactory.getLogger(IbdTbAddrCodeOfDataStandardRepositoryImpl.class);

    private final SqlExecutor sqlExecutor;

    public IbdTbAddrCodeOfDataStandardRepositoryImpl(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }
    @Override
    public List<String> findBySeq(List<Integer> seq) {
        Query query = Query.builder()
                .append("SELECT TO_JSON(ADDR_ODS.IBD_TB_ADDR_CODE_OF_DATA_STANDARD.*) AS JSON_RESULT ")
                .append("FROM ADDR_ODS.IBD_TB_ADDR_CODE_OF_DATA_STANDARD WHERE SEQ IN (:SEQ)", seq == null ? "'XX'" : seq)
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        return sqlExecutor.queryForList(query, String.class);
    }
}
