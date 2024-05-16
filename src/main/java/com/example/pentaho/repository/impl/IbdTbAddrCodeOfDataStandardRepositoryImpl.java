package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.component.Address;
import com.example.pentaho.component.IbdTbAddrCodeOfDataStandardDTO;
import com.example.pentaho.component.IbdTbIhChangeDoorplateHis;
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
    public List<IbdTbAddrCodeOfDataStandardDTO> findBySeq(List<Integer> seq) {
        List<Integer> seqWhenEmpty = new ArrayList<>();
        seqWhenEmpty.add(-1);
        Query query = Query.builder()
                .append("SELECT ADDR_ODS.IBD_TB_ADDR_CODE_OF_DATA_STANDARD.*")
                .append("FROM ADDR_ODS.IBD_TB_ADDR_CODE_OF_DATA_STANDARD WHERE SEQ IN (:SEQ) ", seq.isEmpty() ? seqWhenEmpty: seq)
                .append("AND ADR_VERSION IN (SELECT MAX( ADR_VERSION ) FROM ADDR_ODS.IBD_TB_ADDR_CODE_OF_DATA_STANDARD)")
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        return sqlExecutor.queryForList(query, IbdTbAddrCodeOfDataStandardDTO.class);
    }

    @Override
    public List<IbdTbAddrCodeOfDataStandardDTO> findByAddressId(List<IbdTbIhChangeDoorplateHis> IbdTbIhChangeDoorplateHisList, Address address) {
        List<IbdTbAddrCodeOfDataStandardDTO> resultList = new ArrayList<>();
        IbdTbIhChangeDoorplateHisList.forEach(his -> {
            IbdTbAddrCodeOfDataStandardDTO dto = new IbdTbAddrCodeOfDataStandardDTO();
            if (his.getAddressId() == null && "X".equals(his.getStatus())) { //門牌廢止
                dto.setSeq(his.getHistorySeq());
                dto.setAdrVersion(his.getAdrVersion());
                dto.setFullAddress(address.getOriginalAddress());
                dto.setJoinStep("JE621");//異動軌跡有異
                resultList.add(dto);
            } else if (his.getAddressId() != null) {
                Query query = Query.builder()
                        .append("SELECT ADDR_ODS.IBD_TB_ADDR_CODE_OF_DATA_STANDARD.*")
                        .append("FROM ADDR_ODS.IBD_TB_ADDR_CODE_OF_DATA_STANDARD WHERE ADDRESS_ID = :ADDRESS_ID ", his.getAddressId())
                        .append("AND ADR_VERSION IN (SELECT MAX( ADR_VERSION ) FROM ADDR_ODS.IBD_TB_ADDR_CODE_OF_DATA_STANDARD)")
                        .build();
                log.info("query:{}", query);
                log.info("params:{}", query.getParameters());
                resultList.addAll(sqlExecutor.queryForList(query, IbdTbAddrCodeOfDataStandardDTO.class));

            }
        });
        return resultList;
    }
}
