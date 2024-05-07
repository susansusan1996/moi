package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.repository.IbdTbDoorplateCounCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class IbdTbDoorplateCounCodeRepositoryImpl implements IbdTbDoorplateCounCodeRepository {

    private final SqlExecutor sqlExecutor;

    public IbdTbDoorplateCounCodeRepositoryImpl(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    private final static Logger log = LoggerFactory.getLogger(IbdTbAddrStatisticsOverallDevRepositoryImpl.class);

    @Override
    public List<String> queryAllCounty() {
        Query query = Query.builder()
                .append("SELECT COUN FROM addr_stage.IBD_TB_DOORPLATE_COUN_CODE ")
                .append("WHERE COUN is not null and IS_LEGACY IS NULL")
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        return sqlExecutor.queryForList(query, String.class);
    }
}
