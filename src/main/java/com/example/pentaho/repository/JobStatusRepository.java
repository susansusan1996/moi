package com.example.pentaho.repository;

import com.example.pentaho.component.JobStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface JobStatusRepository {

    int saveJob(JobStatus jobStatus);

    int updateJobByBatchId(JobStatus jobStatus);

    int updateJobByJobId(Map<String,String> result);

    List<JobStatus> getJobStatusByResultAndExcuteDate();

}
