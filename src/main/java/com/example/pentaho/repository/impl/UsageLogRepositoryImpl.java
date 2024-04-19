package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.component.UsageLog;
import com.example.pentaho.component.UsageLogDTO;
import com.example.pentaho.repository.UsageLogRepository;
import com.example.pentaho.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class UsageLogRepositoryImpl implements UsageLogRepository {

    private Logger logger = LoggerFactory.getLogger(UsageLogRepositoryImpl.class);

    private final static String split = ",";

    private final SqlExecutor sqlExecutor;

    public UsageLogRepositoryImpl(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }
    @Override
    public void saveUsageLog(List<String[]> logs) {
        String sql = "insert into addr_ods.USAGE_LOG (ip,user_id, uri, params,dateTimeTrace) \n";
        for(int i = 0; i<logs.size();i++){
            String[] line = logs.get(i);
            /**整理line*/
            if(logs.size() == 1){
                sql += "values(" + line[0] + split + line[1] + split + line[2] + split + line[3] + split + line[4] + ")\n";
                break;
            }

            if(i == 0 ){
              sql += "values(" + line[0] + split + line[1] + split + line[2] + split + line[3] + split + line[4] + ")" + split+"\n";
            }else if( i == (logs.size()-1)){
                sql += "(" + line[0] + split + line[1] + split + line[2] + split + line[3] + split + line[4] + ")";
            }else{
                sql += "(" + line[0] + split + line[1] + split + line[2] + split + line[3] + split + line[4] + ")" + split+"\n";
            }
        }
        logger.info("sql:{}",sql);
        int insert = sqlExecutor.insert(sql);
        logger.info("新增:{}",insert);
    }

    @Override
    public List<UsageLog> getUsageLogs(UsageLogDTO usageLogDTO) {
        boolean useUserId = usageLogDTO.getUserIds() != null && !usageLogDTO.getUserIds().isEmpty();
        boolean useUri =  usageLogDTO.getUris() != null &&!usageLogDTO.getUris().isEmpty();
        boolean useIps = usageLogDTO.getIps() != null && !usageLogDTO.getIps().isEmpty();
                Query query = Query.builder()
                .append("SELECT user_id, uri, params, dateTimeTrace, ip \n")
                .append("FROM addr_ods.USAGE_LOG \n")
                .append("where 1 = 1 \n")
                .appendWhen(useUserId, "and user_id in (:userIds) \n", usageLogDTO.getUserIds())
                .appendWhen(useUri, "and uri in (:uris) \n", usageLogDTO.getUris())
                .appendWhen(useIps, "and ip in (:ips)", usageLogDTO.getIps())
                .appendWhen(StringUtils.isNotNullOrEmpty(usageLogDTO.getDataDateStart()), "and dateTimeTrace >= CAST(:dataDateStart AS TIMESTAMP) \n", usageLogDTO.getDataDateStart())
                .appendWhen(StringUtils.isNotNullOrEmpty(usageLogDTO.getDataDateEnd()), "and dateTimeTrace <= CAST(:dataDateEnd AS TIMESTAMP) \n", usageLogDTO.getDataDateEnd())
                .append("order by dateTimeTrace ,uri,user_id ,ip \n")
                .build();
        logger.info("sql:{}",query);
        logger.info("params:{}",query.getParameters());
        return sqlExecutor.queryForList(query,UsageLog.class);
    }
}
