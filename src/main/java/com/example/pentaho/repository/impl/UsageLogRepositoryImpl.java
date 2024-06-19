package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.component.UsageLog;
import com.example.pentaho.component.UsageLogDTO;
import com.example.pentaho.component.UsageLogReport;
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
        String sql = "insert into addr_system.USAGE_LOG (ip,user_id, uri, params,dateTimeTrace) \n";
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
    public List<UsageLog> getUsageLogsByParams(UsageLogDTO usageLogDTO) {
        boolean useUserId = usageLogDTO.getUserIds() != null && !usageLogDTO.getUserIds().isEmpty();
        boolean useUri =  usageLogDTO.getUris() != null &&!usageLogDTO.getUris().isEmpty();
        boolean useIps = usageLogDTO.getIps() != null && !usageLogDTO.getIps().isEmpty();
                Query query = Query.builder()
                .append("SELECT user_id, uri, params, dateTimeTrace, ip \n")
                .append("FROM addr_system.USAGE_LOG \n")
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

    @Override
    public List<UsageLogReport> getUsageLog(UsageLogDTO usageLogDTO) {
        String startDate ="\'" + usageLogDTO.getDataDateStart() + " 00:00:00'";
        String endDate ="\'" + usageLogDTO.getDataDateEnd() + " 24:00:00'";

        Query query = Query.builder().append("with cnt_result as (\n" +
                "select \n" +
                " cast(dateTimeTrace as DATE) as date_time,\n" +
                " uri,\n" +
                " count(*) api_cnt\n" +
                "from addr_system.USAGE_LOG \n")
                .append("where dateTimeTrace between cast(" + startDate + " as datetime) AND cast( " + endDate + " as datetime) ")
                .append("group by 1,2\n" +
                ")\n" +
                " \n" +
                "select a.date_time, \"query_single\", \"query_standard_address\", \"query_track\"\n" +
                "from\n" +
                " (select date_time, api_cnt as 'query_single'\n" +
                " from cnt_result where uri = '/api/api-key/query-single') a\n" +
                "left join (select date_time, api_cnt as 'query_standard_address'\n" +
                "from cnt_result where uri = '/api/api-key/query-standard-address') b\n" +
                " on a.date_time = b.date_time\n" +
                "left join (select date_time, api_cnt as 'query_track'\n" +
                " from cnt_result where uri = '/api/api-key/query-track') c\n" +
                " on a.date_time = c.date_time \n" +
                " order by 1").build();
        logger.info("query:{}",query);
        return sqlExecutor.queryForList(query, UsageLogReport.class);
    }
}
