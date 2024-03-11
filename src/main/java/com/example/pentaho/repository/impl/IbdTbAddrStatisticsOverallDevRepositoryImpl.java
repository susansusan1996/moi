package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.component.IbdTbAddrStatisticsOverallDev;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.repository.IbdTbAddrStatisticsOverallDevRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class IbdTbAddrStatisticsOverallDevRepositoryImpl implements IbdTbAddrStatisticsOverallDevRepository {

    private final SqlExecutor sqlExecutor;

    public IbdTbAddrStatisticsOverallDevRepositoryImpl(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    private final static Logger log = LoggerFactory.getLogger(IbdTbAddrStatisticsOverallDevRepositoryImpl.class);

    @Override
    public List<Integer> findCntByDataset(String dataset) {
        log.info("formName:{}",dataset);
try {
    Query.Builder builder = Query.builder().append("SELECT CNT \n" +
                    "FROM addr_ods.IBD_TB_ADDR_STATISTICS_OVERALL_DEV \n" +
                    "WHERE ID = ( \n" +
                    "SELECT MAX(ID) \n" +
                    "FROM addr_ods.IBD_TB_ADDR_STATISTICS_OVERALL_DEV \n" +
                    "WHERE DETAIL = 'TOTAL_CNT'\n")
//                .append("AND DATASET = '"+ dataset+"'")
            .append("AND DATASET = :dataset", dataset)
            .append(" ) \n")
            .append("AND DETAIL = 'TOTAL_CNT' \n")
//                .append("AND DATASET = '"+ dataset+"'")
            .append("AND DATASET = :dataset", dataset);
    log.info("query:{}", builder.build());
    return sqlExecutor.queryForList(builder.build(), Integer.class);
    }catch (Exception e){
        log.info("e:{}",e.toString());
    throw new MoiException("查找log失敗 {}: " + e.getMessage());
    }
    }
}
