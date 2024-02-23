package com.example.pentaho.service;

import com.example.pentaho.component.IbdTbIhChangeDoorplateHis;
import com.example.pentaho.component.SingleQueryDTO;
import com.example.pentaho.repository.IbdTbAddrDataNewRepository;
import com.example.pentaho.repository.IbdTbIhChangeDoorplateHisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Service
public class SingleQueryService {

    private static Logger log = LoggerFactory.getLogger(SingleQueryService.class);

    Integer SCAN_SIZE = 1000;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IbdTbIhChangeDoorplateHisRepository ibdTbIhChangeDoorplateHisRepository;

    @Autowired
    private IbdTbAddrDataNewRepository ibdTbAddrDataNewRepository;


    /**
     * 找單一個值 (redis: get)
     */
    public String findByKey(SingleQueryDTO singleQueryDTO) {
        String redisValue = stringRedisTemplate.opsForValue().get(singleQueryDTO.getRedisKey());
        log.info("redisValue: {}", redisValue);
        return redisValue;
    }


    /**
     * 找為LIST的值 (redis: LRANGE)
     */
    public List<String> findListByKey(SingleQueryDTO singleQueryDTO) {
        ListOperations<String, String> listOps = stringRedisTemplate.opsForList();
        List<String> elements = listOps.range(singleQueryDTO.getRedisKey(), 0, -1);
        log.info("elements:{}", elements);
        return elements;
    }

    /**
     * 模糊比對，找出相符的 KEY (redis: scan)
     */
    public Set<String> findListByScan(SingleQueryDTO singleQueryDTO) {
        Set<String> keySet = stringRedisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keySetTemp = new ConcurrentSkipListSet<>();
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions()
                    .match(singleQueryDTO.getRedisKey() + "*") //模糊比對
                    .count(SCAN_SIZE)
                    .build())) {
                while (cursor.hasNext() && keySetTemp.size() < SCAN_SIZE) {
                    keySetTemp.add(new String(cursor.next(), "utf-8"));
                }
            } catch (Exception e) {
                log.error("redis，模糊比對錯誤:{}", e.getMessage());
            }
            return keySetTemp;
        });
        log.info("keySet:{}", keySet);
        return keySet;
    }


    /**
     * set單一個值 (redis: set)
     */
    public void setData(SingleQueryDTO singleQueryDTO) {
        stringRedisTemplate.opsForValue().set(singleQueryDTO.getRedisKey(), singleQueryDTO.getRedisValue());
        log.info("set單一個值，key: {}, value: {}", singleQueryDTO.getRedisKey(), singleQueryDTO.getRedisValue());
    }


    /**
     * 一個key塞多筆值(redis: RPUSH)
     */
    public void pushData(SingleQueryDTO singleQueryDTO) {
        stringRedisTemplate.opsForList().rightPushAll(singleQueryDTO.getRedisKey(), singleQueryDTO.getRedisValueList());
        log.info("push value to a key，key: {}, value: {}", singleQueryDTO.getRedisKey(), singleQueryDTO.getRedisValueList());
    }


    /**
     * 找為SET的值 (redis: SMEMBERS)
     */
    public Set<String> findSetByKey(SingleQueryDTO singleQueryDTO) {
        SetOperations<String, String> setOps = stringRedisTemplate.opsForSet();
        Set<String> elements = setOps.members(singleQueryDTO.getRedisKey());
        log.info("elements:{}", elements);
        return elements;
    }



    public String findJson(String originalString) {
        //先到addr_ods.IBD_TB_ADDR_DATA_REPOSITORY_NEW找相對應的seq
        //TODO:先寫死
        SingleQueryDTO singleQueryDTO = new SingleQueryDTO();
        singleQueryDTO.setRedisKey("1066693");
        Integer seq =  ibdTbAddrDataNewRepository.querySeqByCriteria(singleQueryDTO);
        //再到REDIS(ADDR_ods.IBD_TB_ADDR_CODE_OF_DATA_STANDARD)找seq相對應column組裝成的json
        singleQueryDTO.setRedisKey(String.valueOf(seq));
        return findByKey(singleQueryDTO);
    }

    public List<IbdTbIhChangeDoorplateHis> singleQueryTrack(IbdTbIhChangeDoorplateHis IbdTbIhChangeDoorplateHis) {
        return ibdTbIhChangeDoorplateHisRepository.findByhisCity(IbdTbIhChangeDoorplateHis.getHisCity());
    }
}
