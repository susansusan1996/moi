package com.example.pentaho.service;

import com.example.pentaho.component.Address;
import com.example.pentaho.component.IbdTbAddrCodeOfDataStandardDTO;
import com.example.pentaho.component.IbdTbIhChangeDoorplateHis;
import com.example.pentaho.component.SingleQueryDTO;
import com.example.pentaho.repository.IbdTbAddrCodeOfDataStandardRepository;
import com.example.pentaho.repository.IbdTbIhChangeDoorplateHisRepository;
import com.example.pentaho.utils.AddressParser;
import com.example.pentaho.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.pentaho.utils.NumberParser.*;

@Service
public class SingleQueryService {

    private static Logger log = LoggerFactory.getLogger(SingleQueryService.class);

    @Autowired
    private IbdTbIhChangeDoorplateHisRepository ibdTbIhChangeDoorplateHisRepository;

    @Autowired
    private AddressParser addressParser;

    @Autowired
    private IbdTbAddrCodeOfDataStandardRepository ibdTbAddrCodeOfDataStandardRepository;

    @Autowired
    private RedisService redisService;

    @Autowired
    private JoinStepService joinStepService;

    @Autowired
    private SetAddressService setAddressService;



    public String findJsonTest(SingleQueryDTO singleQueryDTO) {
        return redisService.findByKey(null, "1066693", null);
    }

    public List<IbdTbAddrCodeOfDataStandardDTO> findJson(String originalString) throws NoSuchFieldException, IllegalAccessException {
        Address address = findMappingId(originalString);
        log.info("mappingId:{}", address.getMappingId());
        address = findSeqByMappingIdAndJoinStep(address);
        Set<String> seqSet = address.getSeqSet();
        log.info("seq:{}", seqSet);
        List<IbdTbAddrCodeOfDataStandardDTO> list = ibdTbAddrCodeOfDataStandardRepository.findBySeq(seqSet.stream().map(Integer::parseInt).collect(Collectors.toList()));
        //放地址比對代碼
        Address finalAddress = address;
        list.forEach(IbdTbAddrDataRepositoryNewdto -> IbdTbAddrDataRepositoryNewdto.setJoinStep(finalAddress.getJoinStep()));
        return list;
    }

    public Address findMappingId(String originalString) {
        //切地址
        Address address = addressParser.parseAddress(originalString, null, null);
        log.info("getOriginalAddress:{}",address.getOriginalAddress());
        String numTypeCd = "95";
        //如果有addrRemain的話，表示有可能是"臨建特附"，要把"臨建特附"先拿掉，再PARSE一次地址
        if(StringUtils.isNotNullOrEmpty(address.getAddrRemains())){
            numTypeCd = getNumTypeCd(address);
            //臨建特附，再parse一次地址
            if (!"95".equals(numTypeCd)) {
                address = addressParser.parseAddress(null, address.getOriginalAddress(), address);
            }else{
                //有可能是地址沒有切出來導致有remain
                address = addressParser.parseNotFoundArea(address);
            }
            address.setNumTypeCd(numTypeCd);
        }else{
            address.setNumTypeCd(numTypeCd); //95
        }
        return setAddressService.setAddressAndFindCdByRedis(address);
    }

    private static String getNumTypeCd(Address address) {
        String oldAddrRemains = address.getAddrRemains();
        String newAddrRemains = "";
        String numTypeCd;
        if (oldAddrRemains.startsWith("臨") || oldAddrRemains.endsWith("臨")) {
            numTypeCd = "96";
            newAddrRemains = oldAddrRemains.replace("臨", "");
        } else if (oldAddrRemains.startsWith("建") || oldAddrRemains.endsWith("建")) {
            numTypeCd = "97";
            newAddrRemains = oldAddrRemains.replace("建", "");
        } else if (oldAddrRemains.startsWith("特") || oldAddrRemains.endsWith("特")) {
            numTypeCd = "98";
            newAddrRemains = oldAddrRemains.replace("特", "");
        } else if (oldAddrRemains.startsWith("附") || oldAddrRemains.endsWith("附")) {
            numTypeCd = "99";
            newAddrRemains = oldAddrRemains.replace("附", "");
        } else {
            numTypeCd = "95";
            newAddrRemains = oldAddrRemains;
        }
        //再把addrRemains拼回原本的address，再重新切一次地址
        address.setOriginalAddress(address.getOriginalAddress().replace(oldAddrRemains, newAddrRemains));
        return numTypeCd;
    }


    Address findSeqByMappingIdAndJoinStep(Address address) throws NoSuchFieldException, IllegalAccessException {
        Set<String> seqSet = new HashSet<>();
        String seq = redisService.findByKey("mappingId 64碼", address.getMappingId(), null);
        if (!StringUtils.isNullOrEmpty(seq)) {
            seqSet.add(seq);
            //如果一次就找到seq，表示地址很完整，比對代碼為JA111
            address.setJoinStep("JA111");
        } else {
            //如果找不到完整代碼，要用正則模糊搜尋
            Map<String, String> map = buildRegexMappingId(address);
            String newMappingId = map.get("newMappingId");
            String regex = map.get("regex");
            log.info("因為地址不完整，組成新的 mappingId {}，以利模糊搜尋", newMappingId);
            log.info("模糊搜尋正則表達式為:{}", regex);
            Set<String> mappingIdSet = redisService.findListByScan(newMappingId);
            log.info("mappingIdSet:{}",mappingIdSet);
            Pattern regexPattern = Pattern.compile(String.valueOf(regex));
            Set<String> newMappingIdSet = new HashSet<>();
            for (String newMapping : mappingIdSet) {
                //因為redis的scan命令，無法搭配正則，限制*的位置只能有多少字元，所以要再用java把不符合的mappingId刪掉
                Matcher matcher = regexPattern.matcher(newMapping);
                //有符合的mappingId，才是真正要拿來處理比對代碼的mappingId
                if (matcher.matches()) {
                    newMappingIdSet.add(newMapping);
                }
            }
            log.info("最終可比對的mappingId:{}", newMappingIdSet);
            //=========比對代碼====================
            seqSet.addAll(joinStepService.findJoinStep(address, newMappingIdSet, seqSet));
        }
        address.setSeqSet(seqSet);
        return address;
    }





    private Map<String, String> buildRegexMappingId(Address address) {
        String segNum = address.getSegmentExistNumber();
        StringBuilder newMappingId = new StringBuilder(); //組給redis的模糊搜尋mappingId ex.10010***9213552**95********
        StringBuilder regex = new StringBuilder();   //組給java的模糊搜尋mappingId ex.10010020017029\d{18}95\d{19}000000\d{5}
        //mappingCount陣列代表，COUNTY_CD要5位元，TOWN_CD要3位元，VILLAGE_CD要3位元，以此類推
        //6,5,4,3,1 分別是NUM_FLR_1~NUM_FLR_5
        int[] mappingCount = {5, 3, 3, 3, 7, 4, 7, 2, 6, 5, 4, 3, 1, 1, 5, 5};
        int sum = 0;
        for (int i = 0; i < segNum.length(); i++) {
            //該欄位是1的情況(找的到的情況)
            if ("1".equals(String.valueOf(segNum.charAt(i)))) {
                newMappingId.append(address.getMappingIdList().get(i));
                regex.append(address.getMappingIdList().get(i));
                sum = 0; //歸零
                //該欄位是0的情況(找不到的情況)
            } else {
                String segAfter = "";
                String segBefore = "";
                //第一碼
                if (i == 0) {
                    segAfter = String.valueOf(segNum.charAt(i + 1));
                    //如果前後碼也是0的話，表示要相加
                    if ("0".equals(segAfter)) {
                        sum += mappingCount[i];
                    }
                    //後面那碼是1，表示不用相加了
                    else if ("1".equals(segAfter)) {
                        sum += mappingCount[i];
                        regex.append("\\d{").append(sum).append("}");
                        sum = 0; //歸零
                    }
                }
                //不是最後一個
                else if (i != segNum.length() - 1) {
                    segAfter = String.valueOf(segNum.charAt(i + 1));
                    segBefore = String.valueOf(segNum.charAt(i - 1));
                    //如果前後都是是1的話，不用相加
                    if ("1".equals(segBefore) && "1".equals(segAfter)) {
                        sum = 0; //歸零
                        regex.append("\\d{").append(mappingCount[i]).append("}");
                    }
                    //如果前後碼也是0的話，表示要相加
                    else if ("0".equals(segAfter)) {
                        sum += mappingCount[i];
                    }
                    //後面那碼是1，表示不用相加了
                    else if ("1".equals(segAfter)) {
                        sum += mappingCount[i];
                        regex.append("\\d{").append(sum).append("}");
                        sum = 0; //歸零
                    }
                }
                //是最後一個
                else {
                    segBefore = String.valueOf(segNum.charAt(i - 1));
                    //如果前面是0表示最後一碼也要加上去
                    if ("0".equals(segBefore)) {
                        sum += mappingCount[i];
                        regex.append("\\d{").append(sum).append("}");
                    } else {
                        regex.append("\\d{").append(mappingCount[i]).append("}");
                    }
                    sum = 0; //歸零
                }
                newMappingId.append("*");
            }
        }
        Map<String, String> map = new HashMap<>();
        map.put("newMappingId", newMappingId.toString());
        map.put("regex", regex.toString());
        return map;
    }






    public List<IbdTbIhChangeDoorplateHis> singleQueryTrack(String addressId) {
        log.info("addressId:{}", addressId);
        return ibdTbIhChangeDoorplateHisRepository.findByAddressId(addressId);
    }

}
