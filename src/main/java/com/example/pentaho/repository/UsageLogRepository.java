package com.example.pentaho.repository;

import com.example.pentaho.component.UsageLog;
import com.example.pentaho.component.UsageLogDTO;
import com.example.pentaho.component.UsageLogReport;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsageLogRepository {

    void saveUsageLog(List<String[]> logs);


    List<UsageLog> getUsageLogsByParams(UsageLogDTO usageLogDTO);

    List<UsageLogReport> getUsageLog(UsageLogDTO usageLogDTO);
}
