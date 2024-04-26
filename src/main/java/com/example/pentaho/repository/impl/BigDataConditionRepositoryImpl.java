package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.component.BigDataParams;
import com.example.pentaho.repository.BigDataConditionRepository;
import com.example.pentaho.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class BigDataConditionRepositoryImpl implements BigDataConditionRepository {

    private final static Logger log = LoggerFactory.getLogger(BigDataConditionRepositoryImpl.class);

    private final SqlExecutor sqlExecutor;


    public BigDataConditionRepositoryImpl(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    @Override
    public int saveConditions(BigDataParams bigDataParams) {
        log.info("bigDataParams:{}",bigDataParams);
        try {
            Query query = Query.builder()
                    .append("INSERT INTO your_table_name \n" +
                            "(ID, COUNTY, TOWN, VILLAGE, NEIGHBOR_CD, ROAD, AREA, LANE, ALLEY, SUBALLEY, NUM_TYPE, NUM_FLR, ROOM, X, Y, VERSION, TOWN_SN, " +
                            "GEO_HASH, ROAD_ID, ROAD_ID_DT, POST_CODE, POST_CODE_DT, SOURCE, VALIDITY, INTEGRITY) \n" +
                            " VALUES (")
                    .appendWhen(StringUtils.isNotNullOrEmpty(bigDataParams.getFormId()), ":formId ,", bigDataParams.getFormId())
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getCounty()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getTown()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getVillage()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getNeighborCd()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getRoad()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getArea()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getLane()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getAlley()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getSuballey()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getNumType()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getNumFlr()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getRoom()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getX()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getY()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getVersion()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getTownSn()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getGeoHash()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getRoadId()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getRoadIdDt()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getPostCode()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getPostCodeDt()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getSource()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getValidity()) ? "'Y'" : "'N' ,")
                    .append(StringUtils.isNotNullOrEmpty(bigDataParams.getIntegrity()) ? "'Y'" : "'N' )").build();
            log.info("query:{}", query);
            return sqlExecutor.insert(query);
        }catch (Exception e){
            log.info("e:{}",e.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
