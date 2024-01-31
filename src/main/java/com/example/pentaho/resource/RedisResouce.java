package com.example.pentaho.resource;

import com.example.pentaho.component.SingleQueryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/redis")
public class RedisResouce {

    private static Logger log = LoggerFactory.getLogger(RedisResouce.class);


    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @PostMapping("/query-data")
    public void queryData(@RequestBody SingleQueryDTO singleQueryDTO) {
        String redisValue = stringRedisTemplate.opsForValue().get(singleQueryDTO.getRedisKey());
        log.info("redisValue: {}", redisValue);
    }


    //用Jedis獲取redis，多線程中比較不安全
//    @PostMapping("/query-data-by-jedis")
//    public void queryDataByJedis() {
//        Jedis jedis = new Jedis("34.211.215.66", 8005);
//        String value = jedis.get("test");
//        System.out.println("Redis中的值：" + value);
//        jedis.close();
//    }
}
