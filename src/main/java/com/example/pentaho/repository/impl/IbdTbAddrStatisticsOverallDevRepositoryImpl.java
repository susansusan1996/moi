package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.component.IbdTbAddrStatisticsOverallDev;
import com.example.pentaho.repository.IbdTbAddrStatisticsOverallDevRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class IbdTbAddrStatisticsOverallDevRepositoryImpl implements IbdTbAddrStatisticsOverallDevRepository {

    private final SqlExecutor sqlExecutor;

    public IbdTbAddrStatisticsOverallDevRepositoryImpl(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    @Override
    public List<IbdTbAddrStatisticsOverallDev> findAll() {
        Query.Builder builder = Query.builder().append("SELECT * FROM addr_ods.IBD_TB_ADDR_STATISTICS_OVERALL_DEV");
        return sqlExecutor.queryForList(builder.build(), IbdTbAddrStatisticsOverallDev.class);    }
}
