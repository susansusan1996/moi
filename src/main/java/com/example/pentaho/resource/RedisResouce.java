package com.example.pentaho.resource;

import com.example.pentaho.component.SingleQueryDTO;
import com.example.pentaho.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/redis")
public class RedisResouce {

    private static Logger log = LoggerFactory.getLogger(RedisResouce.class);

    @Autowired
    private RedisService redisService;


    /**
     * 獲取單筆(set、get)
     */
    @PostMapping("/query-data")
    public void queryData(@RequestBody SingleQueryDTO singleQueryDTO) {
        redisService.findByKey(singleQueryDTO);
    }


    /**
     * 獲取一個key的多筆資料(RPUSH、LRANGE )
     */
    @PostMapping("/query-data-list")
    public void findAddr(@RequestBody SingleQueryDTO singleQueryDTO) {
        redisService.findListByKey(singleQueryDTO);
    }


    /**
     * 模糊比對，找出相符的 KEY (scan)
     */
    @PostMapping("/scan-data-list")
    public void scanAddr(@RequestBody SingleQueryDTO singleQueryDTO) {
        redisService.findListByScan(singleQueryDTO);
    }

}
