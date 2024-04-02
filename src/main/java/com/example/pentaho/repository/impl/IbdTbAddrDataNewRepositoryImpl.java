package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.component.SingleQueryDTO;
import com.example.pentaho.repository.IbdTbAddrDataNewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class IbdTbAddrDataNewRepositoryImpl implements IbdTbAddrDataNewRepository {
    private static final Logger log = LoggerFactory.getLogger(IbdTbAddrDataNewRepositoryImpl.class);

    private final SqlExecutor sqlExecutor;

    public IbdTbAddrDataNewRepositoryImpl(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    @Override
    public Integer querySeqByCriteria(SingleQueryDTO singleQueryDTO) {
        Query query = Query.builder()
                .append("SELECT seq ")
                .append("FROM addr_ods.IBD_TB_ADDR_DATA_REPOSITORY_NEW where seq = :seq", singleQueryDTO.getRedisKey())
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        List<Integer> list = sqlExecutor.queryForList(query, Integer.class);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<String> queryDataSourceAndValidityBySeq(List<String> seqList) {
        Query query = Query.builder()
                .append("SELECT DATA_SOURCE AND VALIDITY")
                .append("FROM addr_ods.IBD_TB_ADDR_DATA_REPOSITORY_NEW where seq IN (:seqList) ", seqList)
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        List<String> list = sqlExecutor.queryForList(query, String.class);
        return list;
    }

    @Override
    public String queryDataSourceAndValidityBySeq(String seq) {
        Query query = Query.builder()
                .append("SELECT \n" +
                        "    CASE \n" +
                        "        WHEN DATA_SOURCE = 'HISTORY' THEN 'HISTORY'\n" +
                        "        WHEN VALIDITY = 'F' THEN 'HISTORY'\n" +
                        "        ELSE 'NOW'\n" +
                        "    END AS VALIDITY\n" +
                        "FROM \n" +
                        "    addr_ods.IBD_TB_ADDR_DATA_REPOSITORY_NEW \n" +
                        "WHERE \n" +
                        "    seq = :seq;",seq)
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        List<String> list = sqlExecutor.queryForList(query, String.class);
        return list.get(0);
    }

    @Override
    public List<String> queryAllArea() {
        Query query = Query.builder()
                .append("SELECT DISTINCT AREA FROM addr_ods.IBD_TB_ADDR_DATA_REPOSITORY_NEW ")
                .append("WHERE AREA IS NOT NULL ")
                .append("AND DATA_SOURCE = 'DOORPLATE'")
                .append("AND VALIDITY = 'T'")
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        return sqlExecutor.queryForList(query, String.class);
    }
}
