package com.example.pentaho.repository;

import com.example.pentaho.component.UsageLog;
import com.example.pentaho.component.UsageLogDTO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsageLogRepository {

    void saveUsageLog(List<String[]> logs);


    List<UsageLog> getUsageLogs(UsageLogDTO usageLogDTO);
}
