package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.repository.JobStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class JobStatusRepositoryImpl implements JobStatusRepository {

    private final static Logger log = LoggerFactory.getLogger(JobStatusRepositoryImpl.class);

    private final static List<String> KEYWORDS = Arrays.asList("formName","id","jobParamsJsonStr","result","message","status","executeDate","updateDate");

    private  SqlExecutor sqlExecutor;


    public JobStatusRepositoryImpl(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    @Override
    public int insertJobStatus(Map<String, String> result) {
        KEYWORDS.forEach(keyword->{
            if(!result.containsKey(keyword)){
                result.put(keyword,"");
            }

            if(result.get(keyword) == null){
              result.put(keyword,"");
            }
        });
        log.info("準備存入的參數:{}",result);
        Query query = Query.builder().append("INSERT INTO addr_ods.JOB_STATUS \n")
                .append("(id, job_id, job_params, job_result, job_message, status, execute_date, update_date) \n")
                .append("VALUES(:formName, :id, :jobParamsJsonStr, :result, :message, :status, :executeDate, :updateDate)",
                        result.get("formName"), result.get("id"), result.get("jobParamsJsonStr"), result.get("result"),
                        result.get("message"), result.get("status"),result.get("executeDate"), result.get("updateDate")
                ).build();
        return sqlExecutor.insert(query);
    }
}
