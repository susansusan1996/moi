package com.example.pentaho.service;

import com.example.pentaho.component.Address;
import com.example.pentaho.component.IbdTbIhChangeDoorplateHis;
import com.example.pentaho.component.SingleQueryDTO;
import com.example.pentaho.repository.IbdTbAddrDataNewRepository;
import com.example.pentaho.repository.IbdTbIhChangeDoorplateHisRepository;
import com.example.pentaho.utils.AddressParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Autowired
    private AddressParser addressParser;


    /**
     * 找單一個值 (redis: get)
     */
    public String findByKey(String key, String defaultValue) {
        if (key != null && !key.isEmpty()) {
            String redisValue = stringRedisTemplate.opsForValue().get(key);
            if (redisValue != null && !redisValue.isEmpty()) {
                log.info("redisKey: {} , redisValue: {}", key, redisValue);
                return redisValue;
            }
        }
        return defaultValue;
    }


    /**
     * 找為LIST的值 (redis: LRANGE)
     */
    public List<String> findListByKey(String key) {
        ListOperations<String, String> listOps = stringRedisTemplate.opsForList();
        List<String> elements = listOps.range(key, 0, -1);
        log.info("elements:{}", elements);
        return elements;
    }

    /**
     * 模糊比對，找出相符的 KEY (redis: scan)
     */
    public Set<String> findListByScan(String key) {
        Set<String> keySet = stringRedisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keySetTemp = new ConcurrentSkipListSet<>();
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions()
                    .match("*" + key + "*") //模糊比對
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


    public String findJsonTest(SingleQueryDTO singleQueryDTO) {
        return findByKey("1066693", null);
    }


    public Address findJson(String originalString) {
        //切地址
        Address address = addressParser.parseAddress(originalString, null);
        if (address != null) {
            log.info("address:{}", address);
            String county = address.getCounty();
            //如果是別名，要找到正確的名稱

            String countyCd = findByKey(county, null);

            String town = address.getTown();
            String townCd = findByKey(county + ":" + town, "000");

            String village = address.getVillage(); //里
            String villageCd = findByKey(town + ":" + village, "000");

            String neighbor = findNeighborCd(address.getNeighbor()); //鄰

            String road = address.getRoad();
            String area = address.getArea();

            String roadAreaSn = findByKey((road == null ? "" : road) + (area == null ? "" : area), "0000000");

            String lane = address.getLane(); //巷
            String laneCd = findByKey(lane, "0000");

            String alley = address.getAlley(); //弄
            String subAlley = address.getSubAlley(); //弄
            String alleyIdSn = findByKey((alley == null ? "" : alley) + (subAlley == null ? "" : subAlley), "0000000");

            String numFlr1 = address.getNumFlr1();
            String numFlr1Id = findByKey("NUM_FLR_1:" + deleteBasementString(numFlr1), "000000");

            String numFlr2 = address.getNumFlr2();
            String numFlr2Id = findByKey("NUM_FLR_2:" + deleteBasementString(numFlr2), "00000");

            String numFlr3 = address.getNumFlr3();
            String numFlr3d = findByKey("NUM_FLR_3:" + deleteBasementString(numFlr3), "0000");

            String numFlr4 = address.getNumFlr4();
            String numFlr4d = findByKey("NUM_FLR_4:" + deleteBasementString(numFlr4), "000");

            String numFlr5 = address.getNumFlr5();
            String numFlr5d = findByKey("NUM_FLR_5:" + deleteBasementString(numFlr5), "0");

            String numTypeCd = "95";
            String numFlrId = numFlr1Id + numFlr2Id + numFlr3d + numFlr4d + numFlr5d;
//            String basementStr = "0"; //可能為0、1、2 (1為地下、2為頂樓)
//            String numFlrPos = "10000"; //五碼數字(10000、12000為最多)
            String room = address.getRoom(); //里
            String roomIdSn = findByKey(room, "00000");
            String basementStr = address.getBasementStr() == null ? "0" : address.getBasementStr();
            address.setMappingId(countyCd + townCd + villageCd + neighbor + roadAreaSn + laneCd + alleyIdSn + numTypeCd +
                            numFlrId + basementStr
//                    + numFlrPos
                            + roomIdSn
            );
        }
        return address;
    }

    public List<IbdTbIhChangeDoorplateHis> singleQueryTrack(String addressId) {
        log.info("addressId:{}", addressId);
        return ibdTbIhChangeDoorplateHisRepository.findByAddressId(addressId);
    }


    public String findNeighborCd(String rawNeighbor) {
        Pattern pattern = Pattern.compile("\\d+"); //指提取數字
        Matcher matcher = pattern.matcher(rawNeighbor);
        if (matcher.find()) {
            String neighborResult = matcher.group();
            // 往前補零，補到三位數
            String paddedNumber = String.format("%03d", Integer.parseInt(neighborResult));
            log.info("提取的數字部分為：{}", paddedNumber);
            return paddedNumber;
        } else {
            log.info("沒有數字部分");
            return "000";
        }
    }

    public String deleteBasementString(String rawString) {
        if (rawString != null) {
            return rawString.replace("basement:", "");
        }
        return "";
    }
}
