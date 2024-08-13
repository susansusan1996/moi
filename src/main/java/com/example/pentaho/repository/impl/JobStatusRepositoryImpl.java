package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.component.JobStatus;
import com.example.pentaho.repository.JobStatusRepository;
import com.example.pentaho.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class JobStatusRepositoryImpl implements JobStatusRepository {

    private final static Logger logger = LoggerFactory.getLogger(JobStatusRepositoryImpl.class);
    private final SqlExecutor sqlExecutor;

    public JobStatusRepositoryImpl(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    @Override
    public int saveJob(JobStatus jobStatus) {
        logger.info("jobStatus:{}",jobStatus);
        Query query = Query.builder()
                .append("INSERT INTO addr_ods.JOB_STATUS \n")
                .append("(id, job_id, job_params, job_result, job_message, status, execute_date, update_date) \n")
                .append("VALUES( ")
                .append(":id,:jobId,:jobParams,:jobResult,:jobMessage,:status,:executeDate,:updateDate", jobStatus.getId(), jobStatus.getJobId(), jobStatus.getJobParams(), jobStatus.getJobResult(), jobStatus.getJobMessage(), jobStatus.getStatus(), jobStatus.getExecute_date(), jobStatus.getUpdate_date())
                .append(" )").build();
        int insert = sqlExecutor.insert(query);
        logger.info("query:{}",query);
        logger.info("params:{}",query.getParameters());
        return insert;
    }

    @Override
    public int updateJobByBatchId(JobStatus jobStatus) {
        Query query = Query.builder()
                .append("UPDATE addr_ods.JOB_STATUS \n")
                .append("SET \n")
                .appendWhen(StringUtils.isNotNullOrEmpty(jobStatus.getJobResult()), "job_result = :result , \n", jobStatus.getJobResult())
                .appendWhen(StringUtils.isNotNullOrEmpty(jobStatus.getJobMessage()), "job_message = :message, \n", jobStatus.getJobMessage())
                .appendWhen(StringUtils.isNotNullOrEmpty(jobStatus.getStatus()), "status = :status , \n", jobStatus.getStatus())
                .appendWhen(StringUtils.isNotNullOrEmpty(jobStatus.getUpdate_date()), "update_date = :updateDate \n", jobStatus.getUpdate_date())
                .append("WHERE id = :batch_id", jobStatus.getId())
                .build();
        logger.info("query:{}",query);
        logger.info("params:{}",query.getParameters());
        return sqlExecutor.update(query);
    }

    @Override
    public int updateJobByJobId(Map<String, String> result) {
        logger.info("result:{}",result);
        Query query = Query.builder()
                .append("update addr_ods.JOB_STATUS\n")
                .append("set \n")
                .appendWhen(StringUtils.isNotNullOrEmpty(result.get("result")), "job_result = :result , \n", result.get("result"))
                .appendWhen(StringUtils.isNotNullOrEmpty(result.get("message")), "job_message = :message, \n", result.get("message"))
                .appendWhen(StringUtils.isNotNullOrEmpty(result.get("status")), "status = :status , \n", result.get("status"))
                .appendWhen(StringUtils.isNotNullOrEmpty(result.get("updateDate")), "update_date = :updateDate \n", result.get("updateDate"))
                .append("WHERE job_id = :jobId", result.get("id"))
                .build();
        logger.info("query:{}",query);
        logger.info("params:{}",query.getParameters());
        return sqlExecutor.update(query);
    }

    @Override
    public List<JobStatus> getJobStatusByResultAndExcuteDate() {
        Query query = Query.builder()
                .append("SELECT * \n")
                .append("FROM addr_ods.JOB_STATUS \n")
                .append("WHERE status = 'SYS_FAILED'")
                .append("ORDER BY DATEDIFF('day', execute_date, CURRENT_DATE) DESC \n")
                .append("LIMIT 10").build();
        logger.info("query:{}",query);
        return sqlExecutor.queryForList(query,JobStatus.class);
    }


}
