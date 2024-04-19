package com.example.pentaho.utils;

import com.example.pentaho.component.UsageLog;
import com.example.pentaho.component.UsageLogDTO;
import com.example.pentaho.repository.impl.UsageLogRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class UsageLogService {

    private final static Logger log = LoggerFactory.getLogger(UsageLogService.class);

    @Value("${UsageLogsLocation}")
    private String usageLogsLocation;

    private final static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd");

    private final static String usageLog = "usageLog_"+dateTimeFormat.format(new Date())+".log";

    private final static String split = "@,@";
    @Autowired
    private UsageLogRepositoryImpl usageLogRepository;



  @Scheduled(cron = "0 0 15 * * *", zone = "Asia/Taipei")
//    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Taipei")
    public void saveUsageLog(){
        List<String[]> logs = readUsageLogs();
        if(logs == null || logs.size()<=0){
            log.info("無使用紀錄可儲存");
            return;
        }
        usageLogRepository.saveUsageLog(logs);
    }

    public List<String[]> readUsageLogs() {
        String filePath = usageLogsLocation + usageLog;
        log.info("usageLog檔案路徑:{}",filePath);
        List<String[]> logs = new ArrayList<>();
        try {
            byte[] bytes= readFile(filePath);
            String newContent = new String(bytes, StandardCharsets.UTF_8);
//            log.info("newContent:{}",newContent);
            String[] lines = newContent.split("\n");
//            log.info("lines.length:{}",lines.length);
            if(lines.length <= 0){
                return null;
            }

            for(String line :lines){
                if(StringUtils.isNotNullOrEmpty(line)){
//                    log.info("line:{}",line);
                    String[] datas = line.split(split);
//                    log.info("datas:{}",datas.length);
                    if(!isNullOrEmpty(datas)){
                        logs.add(datas);
                    }
                }
            }
            log.info("共讀取:"+logs.size()+"筆記錄");
            return logs;
        }catch (Exception e){
            log.info("e:{}",e.toString());
            return null;
        }
    }


    /**判断陣列是否為空或元素都為空字串**/
    private boolean isNullOrEmpty(String[] array) {
        return array == null || Arrays.stream(array).allMatch(String::isEmpty);
    }



    private static byte[] readFile(String filePath) throws Exception {
        return Files.readAllBytes(new File(filePath).toPath());
    }

    public List<UsageLog> getUsageLogs(UsageLogDTO usageLogDTO){
        return usageLogRepository.getUsageLogs(usageLogDTO);
    }
}
