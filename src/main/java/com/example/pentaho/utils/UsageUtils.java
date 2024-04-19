package com.example.pentaho.utils;

import com.example.pentaho.component.User;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.text.SimpleDateFormat;
import java.util.Date;

public class UsageUtils {


    private final static Logger log = LoggerFactory.getLogger(UsageUtils.class);

    private final static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final static Logger usageLog = LoggerFactory.getLogger("UsageLog");

    private final static String split = "@,@";


    public final static void writeUsageLog(@NotNull String uri,@NotNull String paramsStr) {
        User user = UserContextUtils.getUserHolder();
        String ip = String.format("'%s'", user.getRemoteAddr());
//        log.info("ip:{}",ip);
        String userId = String.format("'%s'", user.getId());
//        log.info("userId:{}",userId);
        uri = String.format("'%s'", uri);
//        log.info("uri:{}",uri);
        String timestamp = String.format("'%s'",dateTimeFormat.format(new Date()));
//        log.info("timestamp:{}",timestamp);
        String formatParamsStr = "''";
        if(StringUtils.isNotNullOrEmpty(paramsStr)){
            formatParamsStr = String.format("'%s'",paramsStr);
        }
        log.info("formatParamsStr:{}",formatParamsStr);

        MDC.put("usage", "v_1");
        /**
         * 順序不可更改
         * 'ip'@,@'usrId'@,@'uri'@,@'paramsStr'@,@'timestamp'**/
        usageLog.info(
                        ip + split
                        + userId +split
                        + uri + split
                        + formatParamsStr+split
                        + timestamp
        );
        MDC.clear();
    }

}
