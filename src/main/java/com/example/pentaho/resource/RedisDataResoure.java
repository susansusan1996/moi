package com.example.pentaho.resource;

import com.example.pentaho.component.Authorized;
import com.example.pentaho.component.UnAuthorized;
import com.example.pentaho.utils.SftpImpl;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/redis-data")
//@SecurityRequirement(name = "Authorization")
public class RedisDataResoure {

    private final static Logger log = LoggerFactory.getLogger(RedisDataResoure.class);
    @Autowired
    private SftpImpl sftp;


    @PostMapping("/stop")
    @Authorized(keyName = "AP")
    public void stopApServer(@RequestBody Map<String,String> command) throws Exception {
        log.info("body:{}",command);
        //ssh & exec command
        sftp.commandExce(command.get("command"));
    }
}
