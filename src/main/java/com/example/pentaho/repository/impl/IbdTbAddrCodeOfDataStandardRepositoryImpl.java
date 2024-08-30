package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.component.*;
import com.example.pentaho.repository.IbdTbAddrCodeOfDataStandardRepository;
import com.example.pentaho.utils.StringUtils;
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
            } else if (StringUtils.isNotNullOrEmpty(his.getAddressId())) {
                Query query = Query.builder()
                        .append("SELECT ADDR_ODS.IBD_TB_ADDR_CODE_OF_DATA_STANDARD.*")
                        .append("FROM ADDR_ODS.IBD_TB_ADDR_CODE_OF_DATA_STANDARD WHERE ADDRESS_ID = :ADDRESS_ID ", his.getAddressId())
                        .append("AND ADR_VERSION IN (SELECT MAX( ADR_VERSION ) FROM ADDR_ODS.IBD_TB_ADDR_CODE_OF_DATA_STANDARD)")
                        .build();
                log.info("query:{}", query);
                log.info("params:{}", query.getParameters());
                if ("F".equals(his.getStatus()) && "3".equals(his.getUpdateCode())
                ) {
                    List<IbdTbAddrCodeOfDataStandardDTO> list =  sqlExecutor.queryForList(query, IbdTbAddrCodeOfDataStandardDTO.class);
                    list.forEach(standard->standard.setJoinStep("JD721"));//增編多址比對(change status是F、UPDATE_CODE=3)
                    resultList.addAll(list);
                }else{
                    resultList.addAll(sqlExecutor.queryForList(query, IbdTbAddrCodeOfDataStandardDTO.class));
                }
            }
        });
        return resultList;
    }

    @Override
    public List<IbdTbAddrCodeOfDataStandardDTO> findBySeqsAndNumFlrPOS(List<Integer> seq, String numFlrPos) {
        Query query = Query.builder()
                .append("WITH SUBQUERY AS ( \n")
                .append("SELECT A.* \n")
                .append("FROM ADDR_ODS.IBD_TB_ADDR_CODE_OF_DATA_STANDARD A \n")
                .append("INNER JOIN ( \n")
                .append("SELECT * \n")
                .append("FROM addr_ods.IBD_TB_ADDR_DATA_REPOSITORY_NEW \n")
                .append("WHERE ADR_VERSION in (select max(ADR_VERSION) from addr_ods.IBD_TB_ADDR_DATA_REPOSITORY_NEW ) \n")
                .append("AND SEQ in (:SEQ) \n",seq)
                .append("AND NUM_FLR_POS = :NUM_FLR_POS \n",numFlrPos)
                .append(" ) B \n")
                .append("ON A.SEQ = B.SEQ")
//                .append("WHERE ADR_VERSION in (select max(ADR_VERSION) from addr_ods.IBD_TB_ADDR_DATA_REPOSITORY_NEW ) \n")
                .append(") \n")
                .append("SELECT * \n")
                .append("FROM SUBQUERY \n")
//                .append("WHERE ADR_VERSION in (select max(ADR_VERSION) from addr_ods.IBD_TB_ADDR_DATA_REPOSITORY_NEW ) \n")
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        return sqlExecutor.queryForList(query,IbdTbAddrCodeOfDataStandardDTO.class);
    }


    @Override
    public List<IbdTbAddrCodeOfDataStandardDTO> findBySeqsGetNumFlrPOS(List<Integer> seq) {
        Query query = Query.builder()
                .append("WITH SUBQUERY AS ( \n")
                .append("SELECT A.*,B.NUM_FLR_POS,B.ROOM_ID_SN,B.NUM_FLR_ID\n")
                .append("FROM ADDR_ODS.IBD_TB_ADDR_CODE_OF_DATA_STANDARD A \n")
                .append("INNER JOIN ( \n")
                .append("SELECT * \n")
                .append("FROM addr_ods.IBD_TB_ADDR_DATA_REPOSITORY_NEW \n")
                .append("WHERE ADR_VERSION in (select max(ADR_VERSION) from addr_ods.IBD_TB_ADDR_DATA_REPOSITORY_NEW ) \n")
                .append("AND SEQ in (:SEQ) \n",seq)
                .append(" ) B \n")
                .append("ON A.SEQ = B.SEQ")
//                .append("WHERE ADR_VERSION in (select max(ADR_VERSION) from addr_ods.IBD_TB_ADDR_DATA_REPOSITORY_NEW ) \n")
                .append(") \n")
                .append("SELECT * \n")
                .append("FROM SUBQUERY \n")
//                .append("WHERE ADR_VERSION in (select max(ADR_VERSION) from addr_ods.IBD_TB_ADDR_DATA_REPOSITORY_NEW ) \n")
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        return sqlExecutor.queryForList(query,IbdTbAddrCodeOfDataStandardDTO.class);
    }

    @Override
    public List<DataStandardAndRespositoryDTO> findFromDataStandardAndRepository(List<Integer> seqs) {
        Query query = Query.builder().append("" +
                        "SELECT DISTINCT A.SEQ \n" +
                        ",A.ADDRESS_ID\n" +
                        ",A.FULL_ADDRESS\n" +
                        ",A.VALIDITY\n" +
                        ",A.COUNTY\n" +
                        ",A.COUNTY_CD\n" +
                        ",A.TOWN\n" +
                        ",A.TOWN_CD\n" +
                        ",A.POST_CODE\n" +
                        ",A.POST_CODE_DT\n" +
                        ",A.TC_ROAD\n" +
                        ",A.ROAD_ID\n" +
                        ",A.ROAD_ID_DT\n" +
                        ",A.X\n" +
                        ",A.Y\n" +
                        ",A.WGS_X\n" +
                        ",A.WGS_Y\n" +
                        ",A.GEOHASH\n" +
                        ",A.XY_YEAR\n" +
                        ",A.ADR_VERSION\n" +
                        ",A.ETLDT\n" +
                        ",B.* \n" +
                        "FROM addr_ods.IBD_TB_ADDR_CODE_OF_DATA_STANDARD A\n" +
                        "inner join\n" +
                        "( \n" +
                        "SELECT *\n" +
                        "FROM addr_ods.IBD_TB_ADDR_DATA_REPOSITORY_NEW\n" +
                        "WHERE ADR_VERSION in (select max(ADR_VERSION) from addr_ods.IBD_TB_ADDR_DATA_REPOSITORY_NEW) \n" +
                        ") B\n" +
                        "on A.seq = B.seq\n" +
                        "and A.ADR_VERSION = B.ADR_VERSION  \n")
                .append("WHERE  1 = 1 \n")
                .appendWhen(!seqs.isEmpty(), "and A.SEQ in (:seqs)", seqs).build();
        return sqlExecutor.queryForList(query, DataStandardAndRespositoryDTO.class);
    }

    @Override
    public List<OpenPageDTO> findBySeq(Integer seq) {
        Query query = Query.builder()
                .append("SELECT FULL_ADDRESS, \n")
                .append("ADDRESS_ID, \n")
                .append("TO_CHAR(ROUND(WGS_X, 5), 'FM999999999.00000')   || ':'  || TO_CHAR(ROUND(WGS_Y, 5), 'FM999999999.00000') AS LOCATION \n")
                .append("FROM ADDR_ODS.IBD_TB_ADDR_CODE_OF_DATA_STANDARD \n")
                .append("WHERE ADR_VERSION in (select max(ADR_VERSION) FROM ADDR_ODS.IBD_TB_ADDR_CODE_OF_DATA_STANDARD ) \n")
                .append("AND SEQ = :seq \n", seq)
                .build();
        log.info("query:{}",query);
        log.info("params:{}",query.getParameters());
        return sqlExecutor.queryForList(query,OpenPageDTO.class);
    }
}
