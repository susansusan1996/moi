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
    public List<IbdTbIhChangeDoorplateHis> findByAddressId(String addressId) {
//        Query query = Query.builder()
//                .append("SELECT * \n")
//                .append("FROM addr_ods.IBD_TB_IH_CHANGE_DOORPLATE_HIS \n")
//                .append("WHERE ADDRESS_ID =:addressId", addressId)
//                .build();

        Query query = Query.builder().append("select \n" +
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
                .appendWhen(!"".equals(addressId), "and HIS.ADDRESS_ID = :addressId", addressId)
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
}
