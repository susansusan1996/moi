package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.component.BigDataParams;
import com.example.pentaho.repository.ColumnSelectionRepository;
import com.example.pentaho.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class ColumnSelectionRepositoryImpl implements ColumnSelectionRepository {

    private final static Logger log = LoggerFactory.getLogger(ColumnSelectionRepositoryImpl.class);

    private final SqlExecutor sqlExecutor;


    public ColumnSelectionRepositoryImpl(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    @Override
    public int saveConditions(BigDataParams bigDataParams) {
        log.info("bigDataParams:{}",bigDataParams);
        String Y = "\'Y\'";
        String N = "\'N\'";

        try {
            Query query = Query.builder()
                    .append("INSERT INTO addr_system.COLUMN_SELECTION \n" +
                           "(USER_ID, FORM_ID, FORM_NAME, ADDR_ID, ADDRESS, COUNTY, TOWN, VILLAGE, NEIGHBOR, ROAD, AREA, LANE, ALLEY, SUB_ALLEY, NUM_TYPE," +
                            " NUM_FLR, ROOM, X, Y, Z, VERSION, TOWN_SN, GEOHASH_CODE, ROAD_ID, ROAD_ID_DT, POST_CODE, POST_CODE_DT,SOURCE, VALIDITY, INTEGRITY)" +
                            " VALUES (")
                    .append(":userId ,\n",bigDataParams.getUserId())
                    .append(":formId ,\n",bigDataParams.getFormId())
                    .append(":formName,\n",bigDataParams.getFormName())
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getAddrId()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getAddress()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getCounty()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getTown()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getVillage()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getNeighbor()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getRoad()) ? Y : N)  +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getArea()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getLane()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getAlley()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getSubAlley()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getNumType()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getNumFlr()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getRoom()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getX()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getY()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getZ()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getVersion()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getTownSn()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getGeohashCode()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getRoadId()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getRoadIdDt()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getPostCode()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getPostCodeDt()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getSource()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getValidity()) ? Y : N) +", \n")
                    .append((StringUtils.isNotNullOrEmpty(bigDataParams.getIntegrity()) ? Y : N )+ ")").build();
            log.info("query:{}", query);
            return sqlExecutor.insert(query);
        }catch (Exception e){
            log.info("e:{}",e.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
