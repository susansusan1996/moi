package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.repository.IbdTbDoorplateAddrCityCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class IbdTbDoorplateAddrCityCodeRepositoryImpl implements IbdTbDoorplateAddrCityCodeRepository {

    private final SqlExecutor sqlExecutor;

    public IbdTbDoorplateAddrCityCodeRepositoryImpl(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    private final static Logger log = LoggerFactory.getLogger(IbdTbAddrStatisticsOverallDevRepositoryImpl.class);

    @Override
    public List<String> queryTownByCounty(String COUNTY) {
        Query query = Query.builder()
                .append("SELECT ADDR_TOWN FROM addr_stage.IBD_TB_DOORPLATE_ADDR_CITY_CODE")
                .append("WHERE IS_LEGACY IS NULL and ADDR_CITY LIKE :county","%" + COUNTY + "%")
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        return sqlExecutor.queryForList(query, String.class);
    }
}

