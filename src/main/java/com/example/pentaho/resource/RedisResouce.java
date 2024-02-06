package com.example.pentaho.resource;

import com.example.pentaho.component.SingleQueryDTO;
import com.example.pentaho.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/redis")
public class RedisResouce {

    private static Logger log = LoggerFactory.getLogger(RedisResouce.class);

    @Autowired
    private RedisService redisService;


    /**
     * 獲取單筆(set、get)
     */
    @GetMapping("/query-data")
    public ResponseEntity<String> queryData(@RequestBody SingleQueryDTO singleQueryDTO) {
        return ResponseEntity.ok(redisService.findByKey(singleQueryDTO));
    }


    /**
     * 獲取一個key的多筆資料(RPUSH、LRANGE )
     */
    @GetMapping("/query-data-list")
    public ResponseEntity<List<String>> findAddr(@RequestBody SingleQueryDTO singleQueryDTO) {
        return ResponseEntity.ok(redisService.findListByKey(singleQueryDTO));
    }


    /**
     * 獲取一個key的SET(SADD、SMEMBERS)
     */
    @GetMapping("/query-data-set")
    public ResponseEntity<Set<String>> findSetByKey(@RequestBody SingleQueryDTO singleQueryDTO) {
        return ResponseEntity.ok(redisService.findSetByKey(singleQueryDTO));
    }


    /**
     * 模糊比對，找出相符的 KEY (scan)
     */
    @GetMapping("/scan-data-list")
    public ResponseEntity<Set<String>> scanAddr(@RequestBody SingleQueryDTO singleQueryDTO) {
        return ResponseEntity.ok(redisService.findListByScan(singleQueryDTO));
    }


    /**
     * set單筆值(set)
     */
    @PostMapping("/set-data")
    public void setData(@RequestBody SingleQueryDTO singleQueryDTO) {
        redisService.setData(singleQueryDTO);
    }


    /**
     * 一個key塞多筆值(RPUSH)
     */
    @PostMapping("/rpush-data")
    public void pushData(@RequestBody SingleQueryDTO singleQueryDTO) {
        redisService.pushData(singleQueryDTO);
    }

}
