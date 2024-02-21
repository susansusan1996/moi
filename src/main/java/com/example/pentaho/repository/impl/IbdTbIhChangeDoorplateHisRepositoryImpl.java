package com.example.pentaho.repository.impl;


import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.component.IbdTbIhChangeDoorplateHis;
import com.example.pentaho.repository.IbdTbIhChangeDoorplateHisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class IbdTbIhChangeDoorplateHisRepositoryImpl implements IbdTbIhChangeDoorplateHisRepository {


    private static final Logger log = LoggerFactory.getLogger(IbdTbIhChangeDoorplateHisRepositoryImpl.class);
    private final SqlExecutor sqlExecutor;


    public IbdTbIhChangeDoorplateHisRepositoryImpl(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }


    @Override
    public List<IbdTbIhChangeDoorplateHis> findByhisCity(String hisCity) {
        Query query = Query.builder()
                .append("SELECT * \n")
                .append("FROM addr_ods.IBD_TB_IH_CHANGE_DOORPLATE_HIS \n")
                .append("WHERE HIS_VILLAGE = :hisCity", hisCity)
                .build();
        log.info("query:{}",query);
        log.info("params:{}",query.getParameters());
        return sqlExecutor.queryForList(query, IbdTbIhChangeDoorplateHis.class);
    }
}
