package com.example.pentaho.repository.impl;


import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.component.IbdTbIhChangeDoorplateHis;
import com.example.pentaho.repository.IbdTbIhChangeDoorplateHisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class IbdTbIhChangeDoorplateHisRepositoryImpl implements IbdTbIhChangeDoorplateHisRepository {


    private static final Logger log = LoggerFactory.getLogger(IbdTbIhChangeDoorplateHisRepositoryImpl.class);
    private final SqlExecutor sqlExecutor;

    public IbdTbIhChangeDoorplateHisRepositoryImpl(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    /**
     * @param addressId
     * @return
     */
    //todo:還有兩張表躍join
    @Override
    public List<IbdTbIhChangeDoorplateHis> findByAddressId(String addressId)  {

        Query query = Query.builder()
                .append("select \n")
                .append("HIS.ADDRESS_ID, \n")
                .append("SPLIT_PART (HIS.HIS_ADR, '臺灣省', 2), \n")
                .append("STD.WGS_X, \n")
                .append("STD.WGS_Y, \n")
                .append("HIS.UPDATE_DT, \n")
                .append("UPD.UPDATE_TYPE \n")
                .append("from addr_ods.IBD_TB_IH_CHANGE_DOORPLATE_HIS HIS \n")
                .append("left join addr_ods.IBD_TB_ADDR_CODE_OF_DATA_STANDARD STD \n")
                .append("on HIS.ADDRESS_ID = STD.ADDRESS_ID \n")
                .append("left join addr_stage.IBD_TB_HISTORY_CODE_UPDATE UPD \n")
                .append(" on HIS.UPDATE_CODE = UPD.UPDATE_CODE \n")
                .append(" where 1 = 1 \n")
                .append(" and HIS.ADDRESS_ID = :addressId \n", addressId)
                .append("union \n")
                .append(" select \n")
                .append(" ADDRESS_ID, \n")
                .append(" FULL_ADDRESS , \n")
                .append(" WGS_X, \n")
                .append(" WGS_Y, \n")
                .append(" '-' UPDATE_DT,  \n")
                .append(" '現行門牌' UPDATE_TYPE \n")
                .append(" from addr_ods.IBD_TB_ADDR_CODE_OF_DATA_STANDARD \n")
                .append(" where 1 = 1 \n")
                .append(" and ADDRESS_ID = :addressId ", addressId)
                .build();
        log.info("query:{}",query);
        log.info("params:{}",query.getParameters());
        return sqlExecutor.queryForList(query, IbdTbIhChangeDoorplateHis.class);
    }

    @Override
    public List<IbdTbIhChangeDoorplateHis> findByAddressIdList(List<String> addressIdList) {
        try {
            Query query = Query.builder()
                    .append("select \n" +
                            "HIS.ADDRESS_ID, \n" +
                            "HIS.HIS_ADR, \n" +
                            "STD.WGS_X, \n" +
                            "STD.WGS_Y, \n" +
                            "HIS.UPDATE_DT, \n" +
                            "UPD.UPDATE_TYPE \n" +
                            "from addr_ods.IBD_TB_IH_CHANGE_DOORPLATE_HIS HIS \n" +
                            "left join addr_ods.IBD_TB_ADDR_CODE_OF_DATA_STANDARD STD \n" +
                            "on HIS.ADDRESS_ID = STD.ADDRESS_ID \n" +
                            "left join addr_stage.IBD_TB_HISTORY_CODE_UPDATE UPD \n" +
                            "on HIS.UPDATE_CODE = UPD.UPDATE_CODE \n" +
                            "where 1 = 1 \n")
                    .appendWhen((addressIdList != null || !addressIdList.isEmpty()), "and HIS.ADDRESS_ID in (:addressIdList)", addressIdList)
                    .build();

            String queryString = query.getString();
            log.info("queryString:{}", queryString);
            log.info("params:{}", query.getParameters());
            return sqlExecutor.queryForList(query, IbdTbIhChangeDoorplateHis.class);
        }catch (Exception e){
            log.info("e:{}",e);
            return null;
        }
    }

    @Override
    public List<IbdTbIhChangeDoorplateHis> findByHistorySeq(List<String> seq) {
        List<Integer> seqWhenEmpty = new ArrayList<>();
        seqWhenEmpty.add(-1);
        Query query = Query.builder()
                .append("SELECT ADDRESS_ID , STATUS, HISTORY_SEQ , ADR_VERSION, UPDATE_CODE")
                .append("FROM ADDR_ODS.IBD_TB_IH_CHANGE_DOORPLATE_HIS WHERE HISTORY_SEQ IN (:SEQ) ", seq.isEmpty() ? seqWhenEmpty: seq)
                .append("AND ADR_VERSION IN (SELECT MAX( ADR_VERSION ) FROM ADDR_ODS.IBD_TB_IH_CHANGE_DOORPLATE_HIS)")
                .build();
        log.info("query:{}", query);
        log.info("params:{}", query.getParameters());
        return sqlExecutor.queryForList(query, IbdTbIhChangeDoorplateHis.class);
    }
}
