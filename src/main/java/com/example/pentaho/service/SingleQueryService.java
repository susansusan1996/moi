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
        return redisService.setAddressAndFindCdByRedis(address);
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
            Set<String> newMappingIdSet = redisService.fuzzySearchMappingId(address);
            log.info("最終可比對的mappingId:{}", newMappingIdSet);
            //=========比對代碼====================
            seqSet.addAll(joinStepService.findJoinStep(address, newMappingIdSet, seqSet));
        }
        address.setSeqSet(seqSet);
        return address;
    }



    public List<IbdTbIhChangeDoorplateHis> singleQueryTrack(String addressId) {
        log.info("addressId:{}", addressId);
        return ibdTbIhChangeDoorplateHisRepository.findByAddressId(addressId);
    }

}
