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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
        List<IbdTbAddrCodeOfDataStandardDTO> list = new ArrayList<>();
        //切地址+找mappingId
        Address address = parseAddressAndfindMappingId(originalString);
        log.info("mappingId:{}", address.getMappingId());
        //找seq
        address = findSeqByMappingIdAndJoinStep(address);
        Set<String> seqSet = address.getSeqSet();
        if(!seqSet.isEmpty()){
            log.info("seq:{}", seqSet);
            list = ibdTbAddrCodeOfDataStandardRepository.findBySeq(seqSet.stream().map(Integer::parseInt).collect(Collectors.toList()));
            //放地址比對代碼
            Address finalAddress = address;
            list.forEach(IbdTbAddrDataRepositoryNewdto -> IbdTbAddrDataRepositoryNewdto.setJoinStep(finalAddress.getJoinStep()));
        }
        return list;
    }

    public Address parseAddressAndfindMappingId(String originalString) {
        //切地址
        Address address = addressParser.parseAddress(originalString, null, null);
        log.info("getOriginalAddress:{}", address.getOriginalAddress());
        String numTypeCd = "95";
        //如果有addrRemain的話，表示有可能是"臨建特附"，要把"臨建特附"先拿掉，再PARSE一次地址
        if (StringUtils.isNotNullOrEmpty(address.getAddrRemains())) {
            if (!"95".equals(numTypeCd)) { //臨建特附，再parse一次地址
                log.info("臨建特附:{}", address.getOriginalAddress());
                numTypeCd = getNumTypeCd(address);
                address = addressParser.parseAddress(null, address.getOriginalAddress(), address);
            } else if (StringUtils.isNotNullOrEmpty(address.getContinuousNum())) { //連在一起的數字，再parse一次地址
                address = addressParser.parseAddress(null, address.getOriginalAddress(), address);
            } else {
                //有可能是地址沒有切出來導致有remain
                address = addressParser.parseNotFoundArea(address);
            }
            address.setNumTypeCd(numTypeCd);
        } else {
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
        log.info("mappingIdList:{}", address.getMappingId());
        //直接用input的地址組mappingId，進redis db1 找有沒有符合的mappingId
        AtomicReference<List<String>> seqList = findSeqByMappingId(address);
        //有找到
        if (!seqList.get().isEmpty()) {
            log.info("第一次就有比到!!");
            seqSet = splitSeqAndStep(address, seqList, seqSet);
        }
        //沒找到
        //找不到符合得64碼。那就要把這串64碼，用join_step的邏輯一步一步(StepByStep)比較看看，看到哪一個join_step，會找到匹配的64碼
        else {
            seqSet.addAll(joinStepService.findSeqStepByStep(address));
        }
        //多址判斷
        replaceJoinStepWhenMultiAdress(address,seqSet);
        address.setSeqSet(seqSet);
        return address;
    }



    public List<IbdTbIhChangeDoorplateHis> singleQueryTrack(String addressId) {
        log.info("addressId:{}", addressId);
        return ibdTbIhChangeDoorplateHisRepository.findByAddressId(addressId);
    }

    private String replaceNumFlrPosWithZero(Map<String,String> mappingIdMap){
        StringBuilder sb = new StringBuilder();
        // 將NUMFLRPOS為00000的組合也塞進去
        mappingIdMap.put("NUMFLRPOS", "00000");
        sb = new StringBuilder();
        for (Map.Entry<String, String> entry : mappingIdMap.entrySet()) {
            sb.append(entry.getValue());
        }
        return sb.toString();
    }


    //多址join_step判斷
    private void replaceJoinStepWhenMultiAdress(Address address, Set<String> seqSet) {
        if (address.getJoinStep() != null && seqSet.size() > 1) {
            switch (address.getJoinStep()) {
                case "JA211", "JA311" -> address.setJoinStep("JD111");
                case "JA212", "JA312" -> address.setJoinStep("JD112");
                case "JB111", "JB112" -> address.setJoinStep("JD311");
                case "JB311" -> address.setJoinStep("JD411");
                case "JB312" -> address.setJoinStep("JD412");
                case "JB411" -> address.setJoinStep("JD511");
                case "JB412" -> address.setJoinStep("JD512");
            }
        }
    }


    AtomicReference<List<String>> findSeqByMappingId(Address address) {
        AtomicReference<List<String>> seqList = new AtomicReference<>();
        for (String mappingId : address.getMappingId()) {
            seqList.set(redisService.findListByKey(mappingId));
            if (!seqList.get().isEmpty()) {
                break; //有比到就可以跳出迴圈了
            }
        }
        return seqList;
    }

    Set<String> splitSeqAndStep(Address address, AtomicReference<List<String>> seqList, Set<String> seqSet) {
        List<String> sortedList = seqList.get().stream().sorted().toList(); //排序
        String[] seqAndStep = sortedList.get(0).split(":");
        address.setJoinStep(seqAndStep[0]);
        for (String seq : sortedList) {
            seqAndStep = seq.split(":");
            seqSet.add(seqAndStep[1]);
        }
        if ("JC211".equals(address.getJoinStep()) && StringUtils.isNullOrEmpty(address.getArea())) {
            address.setJoinStep("JC311"); //路地名，連寫都沒寫
        }
        return seqSet;
    }

}
