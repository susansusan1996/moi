package com.example.pentaho.service;

import com.example.pentaho.component.*;
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
    private FuzzySearchService fuzzySearchService;

    String segmentExistNumber = ""; //紀錄user是否有輸入每個地址片段，有:1，沒有:0

    /***
     * JE621
     * JD721
     * JE431
     * JE421
     * JE511
     */
    private static final Set<String> EXCLUDED_JOIN_STEPS = Set.of("JE621", "JD721", "JE431", "JE421", "JE511");

    private static final String[] KEY_WORDS = new String[]{"COUNTY","TOWN","VILLAGE","ROAD","AREA","LANE","ALLEY","NUM_FLR_1","NUM_FLR_2","NUM_FLR_3","NUM_FLR_4","NUM_FLR_5","NEIGHBOR","ROOM"};


    public String findJsonTest(SingleQueryDTO singleQueryDTO) {
        return redisService.findByKey(null, "1066693", null);
    }


    /**
     * @param singleQueryDTO
     * @return
     */
    public SingleQueryResultDTO findJson(SingleQueryDTO singleQueryDTO) {
        /**=== 回傳物件 ===**/
        SingleQueryResultDTO result = new SingleQueryResultDTO();
        /**=== 可能的地址，會放進 SingleQueryResultDTO ===**/
        List<IbdTbAddrCodeOfDataStandardDTO> list = new ArrayList<>();
        List<IbdTbAddrCodeOfDataStandardDTO>  resultList = new ArrayList<>();

        /**=== 確認是否是"連號"的地址 ===**/
        if (checkIfMultiAddress(singleQueryDTO)) {
            result.setText("該地址屬於多重地址");
            return result;
        }

        /**=== 刪除使用者重複input的縣市、鄉鎮 ===**/
        String cleanAddress = removeRepeatCountyAndTown(singleQueryDTO);


        /**=== 切地址 + 取得地址cd組合成mappingIdList ===**/
        Address address = parseAddressAndFindMappingId(cleanAddress);
        log.info("排列組合56碼mappingIdList:{}", address.getMappingId());

        /**=== 排列組合後56碼mappingId找seq ===**/
        address = findSeq(address);
        /**=== 要件清單 ===**/
        String segmentExistNumber = address.getSegmentExistNumber();
        /**==seqSet 可能的seqs,可能是空集合==**/
        Set<String> seqSet = address.getSeqSet();
        if (!seqSet.isEmpty()) {
            log.info("有可能的seqs:{}", seqSet);

            /**db取地址資料**/
            list = queryAddressData(address);
            /**放返回的資料**/
//            Address finalAddress = address;
            /**IbdTbAddrDataRepositoryNewdto = 各seq對應的資料**/
           for(IbdTbAddrCodeOfDataStandardDTO IbdTbAddrDataRepositoryNewdto:list){
                /***
                 * (1) 撈出的join_step 為空
                 * (2) DB撈出join_step + redis+程式比對的join_step 接皆不含 "JE621", "JD721", "JE431", "JE421", "JE511"
                 */
                if (
                        IbdTbAddrDataRepositoryNewdto.getJoinStep() == null
                                || (!EXCLUDED_JOIN_STEPS.contains(IbdTbAddrDataRepositoryNewdto.getJoinStep()) &&
                                !EXCLUDED_JOIN_STEPS.contains(address.getJoinStep()))
                ) {
                    //todo:多址怎麼辦呢
                    IbdTbAddrDataRepositoryNewdto.setJoinStep(address.getJoinStep());
                }
                /**
                 *(1)確認 joinStep 然後與 segmentExistNumber比對
                 */
                String joinStep = addressParser.checkJoinStepBySegNum(IbdTbAddrDataRepositoryNewdto.getFullAddress(),address);
                log.info("檢查完後joinStep:{}",joinStep);
                /**跳過JE431..等**/
                if(!EXCLUDED_JOIN_STEPS.contains(joinStep) && joinStep.length()<5){
                    joinStep = renewJoinStep(joinStep, IbdTbAddrDataRepositoryNewdto);
                }
                address.setJoinStep(joinStep);
                IbdTbAddrDataRepositoryNewdto.setJoinStep(joinStep);
                resultList.add(IbdTbAddrDataRepositoryNewdto);

               //todo:這裡的邏輯要再確認
//                /**最後還要filter NUM_FLR ，解決 完整地址只到122號，但會撈出122號~*之 多址*/
//                log.info("finalAddress.getNumFlrPos():{}",address.getNumFlrPos());
//                /**任意數 + "0000" 表示前端輸入像這樣的地址: OOO路1號、OOO路1號A室 這裡要判斷是斷在哪裡~*/
//               if(address.getNumFlrPos().indexOf("0000")!=1){
//                   //10000以外的12000
//                   IbdTbAddrCodeOfDataStandardDTO dto  = addressParser.filterNumFlrPosAndRoom(IbdTbAddrDataRepositoryNewdto, address);
//                    if(dto != null){
//                        resultList.add(dto);
//                    }
//                }else{
//                   resultList.add(IbdTbAddrDataRepositoryNewdto);
//                }
            };
        }
        result.setText("查詢結果");

        //多址判斷
        replaceJoinStepWhenMultiAdress(address,resultList);
        //查無資料，JE431、JE421、JE511、JE311會在這邊寫入
        //resultList -> 空集合;沒有找到任何一個seq
        setJoinStepWhenResultIsEmpty(resultList, result, address);
        result.setData(resultList);
        return result;
    }

    /**
     * 地址切割 + 組合56碼
     *
     * @param input -> 已去除連號、特殊符號、指定關鍵字、重複 county + town 的地址
     * @return
     */
    public Address parseAddressAndFindMappingId(String input) {
        log.info("開始第一次parseAddress");
        Address address = addressParser.parseAddress(input, null);
        /**把origrinalAddress 初始回 input**/
        address.setOriginalAddress(input);
        String numTypeCd = "95";
        address.setNumTypeCd(numTypeCd);
        /**
         * handleAddressRemains
         * 臨建特付 ->拔臨建特付的地址，再切一次
         * 非臨建特付，可以是地名沒辦切出來 -> 拔remain中文當作area，從input中移再切一次
         **/
        handleAddressRemains(address);
        return findCdAndMappingId(address);
    }

    /**
     * 臨建特附(setNumType) 或 地名 (setArea)
     * 拔除後再切割一次地址
     * @param address
     */
    private void handleAddressRemains(Address address) {
        if (StringUtils.isNotNullOrEmpty(address.getAddrRemains()) && StringUtils.isNullOrEmpty(address.getContinuousNum())) {
            log.info("地址有remain:{},且沒有連號:{}",address.getAddrRemains(),address.getContinuousNum());
            log.info("檢查是否含臨(96)、建(97)、特(98)、付(99)、其他(95)");
            String numTypeCd = getNumTypeCd(address);
            if (!"95".equals(numTypeCd)) {
                log.info("我是 <臨建特附>，要拔除臨建特附的地址，再切割一次地址:{}", address.getCleanAddress());
                address = addressParser.parseAddress(address.getCleanAddress(), address);
            } else {
                //todo:除了有可能是AREA沒有切出來 也有可能是NUM_FLR_1~5 導致有remain
                address = addressParser.parseArea(address);
                log.info("我不是 <臨建特附>，提取remain的中文部分當作area,並從input中拔除的地址，再切割一次地址後:{}",address);
            }
        }
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
        /**再移除關鍵字的addrRemains拼回原本的address，再重新切一次地址**/
        address.setCleanAddress(address.getCleanAddress().replace(oldAddrRemains, newAddrRemains));
        return numTypeCd;
    }


    /**
     * 一個map就是一組mappingId
     * address.getMappingIdMap() =[
     * {"COUNTY":"00000","TOWN":"000","VILLAGE":"123456",...},
     * {"COUNTY":"00000","TOWN":"000","VILLAGE":"654321",...},]
     * 一個String 就是一組mappingId
     * address.getMappingId() ->["00000000123456.....","00000000654321.."]
     *
     * @param address
     * @return
     */
    Address findSeq(Address address) {
        Set<String> seqSet = new HashSet<>();
        log.info("排列組合56碼mappingIdList:{}", address.getMappingId());
        /**=== 排列組合 56碼 mappingId，進redis db1 找有沒有符合的  ==**/
        Map<String, Set<String>> resultsBy56  = findMapsByKeys(address);
        if (!resultsBy56.isEmpty()) {
        log.info("56碼第一次就有比到!!");
         seqSet = mappingCountyTownVilliageNeighbor(address,resultsBy56);
        } else {
            log.info("所有56碼都沒找到 拔 neighbor & village 進行查詢");
            //(2) redis key查詢 -> 000000 + 50碼
            build50MappingIds(address);
            Map<String, Set<String>> resultsBy50  = findMapsByKeys(address);
            //拔鄰、裡查詢後還是都找不到東西
            if(resultsBy50.isEmpty() || resultsBy50 == null){
                    log.info("拔鄰、裡查詢後還是都找不到東西");
                    //放入空集合
                    address.setSeqSet(seqSet);
                    return address;
            }

            seqSet = filterCountyAndTown(address, resultsBy50);
        }
        address.setSeqSet(seqSet);
        return address;
    }

    /**
     *
     * @param resultMap
     * @return ->符合條件的 value
     */
    public Set<String> filterCountyAndTown(Address address,Map<String,Set<String>> resultMap){
        Set<String> seqSet = new HashSet<>();
        Set<String> joinStepSet = new HashSet<>();
        //地址片段
        String countyAndTown = address.getCountyCd() + address.getTownCd();
        resultMap.keySet().forEach(key->{
            if(!resultMap.get(key).isEmpty() && resultMap.get(key) != null){
                resultMap.get(key).forEach(str->{
                    String[] split = str.split(":");
                    String addressCd = split[0];
                    String seq = split[2];
                    String joinStep = split[1];
                    if(countyAndTown.equals(addressCd)){
                        log.info("符合的mapping:{}",key);
                        seqSet.add(seq);
                        joinStepSet.add(joinStep);
                    }
                });
            }
        });

        //排序join_step 把第一個塞到address.join_step
        List<String> sortedJoinStepList = joinStepSet.stream().sorted().toList();
        if(!joinStepSet.isEmpty()){
            address.setJoinStep(sortedJoinStepList.get(0));
        }
        if ("JC211".equals(address.getJoinStep()) && StringUtils.isNullOrEmpty(address.getArea())) {
            address.setJoinStep("JC311"); //路地名，連寫都沒寫
        }
        return seqSet;
    }


    public List<IbdTbIhChangeDoorplateHis> singleQueryTrack(String addressId) {
        log.info("addressId:{}", addressId);
        return ibdTbIhChangeDoorplateHisRepository.findByAddressId(addressId);
    }


    /**
     * @param address
     * @param seqSet
     */
    //多址join_step判斷
    private void replaceJoinStepWhenMultiAdress(Address address, Set<String> seqSet) {
        if (address.getJoinStep() != null && seqSet.size() > 1) {
            switch (address.getJoinStep()) {
                case "JA211", "JA311", "JA212", "JA312" -> address.setJoinStep("JD111");
                case "JB111", "JB112" -> address.setJoinStep("JD311");
                case "JB311" -> address.setJoinStep("JD411");
                case "JB312" -> address.setJoinStep("JD412");
                case "JB411" -> address.setJoinStep("JD511");
                case "JB412" -> address.setJoinStep("JD512");
            }
        }
    }


    /**
     * @param address
     * @param resultList
     */
    //多址join_step判斷
    private void replaceJoinStepWhenMultiAdress(Address address, List<IbdTbAddrCodeOfDataStandardDTO> resultList) {
        if (address.getJoinStep() != null && resultList.size()>1) {
            switch (address.getJoinStep()) {
                case "JA211", "JA311", "JA212", "JA312" -> address.setJoinStep("JD111");
                case "JB111", "JB112" -> address.setJoinStep("JD311");
                case "JB311" -> address.setJoinStep("JD411");
                case "JB312" -> address.setJoinStep("JD412");
                case "JB411" -> address.setJoinStep("JD511");
                case "JB412" -> address.setJoinStep("JD512");
            }
            resultList.forEach(ele->{
                ele.setJoinStep(address.getJoinStep());
            });
        }
    }

    /**
     * 一個map就是一組mappingId
     * address.getMappingIdMap() =[
     * {"COUNTY":"00000","TOWN":"000","VILLAGE":"123456",...},
     * {"COUNTY":"00000","TOWN":"000","VILLAGE":"654321",...},]
     * 一個String 就是一組mappingId
     * address.getMappingId() -> ["00000000123456.....","00000000654321.."]
     *
     * @param address
     * @return 空集合 | seqList ->所有key查找的String組成不重複seqList
     */
    List<String> findSeqByMappingId(Address address) {
        List<String> seqList = new ArrayList<>();
        /**排除重複*/
        seqList.addAll(redisService.findSetsByKeys(address.getMappingId()));
        log.info("用排列組合的56碼mappingId找到value:{}", seqList);
        return seqList;
    }


    Map<String,Set<String>>findMapsByKeys(Address address){
        return redisService.findMapsByKeys(address);
    }


    Map<String,List<String>> findMapByMappingId(Address address) {
        Map<String,List<String>> result = new HashMap();
        /**排除重複*/
         redisService.findSetsByKeys(address.getMappingId());
         return result;
    }

    /**
     *
     * @param address
     * @param resultsBeforeSplit = ["00000000:JB411:5141047","00000001:JB311:5141047","12345:001:JB411:5141047",...]
     * @param seqSet
     * @return
     */
    void splitSeqAndStep(Address address, List<String> resultsBeforeSplit, Set<String> seqSet) {
        Set<String> joinStepSet = new HashSet<>();
        /**有找到相對應的56碼*/
        if (!resultsBeforeSplit.isEmpty()) {
            /**input的county + town*/
            String countyAndTown = address.getCountyCd() + address.getTownCd();
            /**input的county + town*/
            /**與redis value比對**/
            for(String seqsStr:resultsBeforeSplit){
                String[] seqArray = seqsStr.split(":");
                String addressCd = seqArray[0];
                /**有相符才取其join_step & seq*/
                if(countyAndTown.equals(addressCd)){
                    String joinStep = seqArray[1];
                    joinStepSet.add(joinStep);
                    String seq = seqArray[2];
                    seqSet.add(seq);
                }
            }
            //join_step排序
            List<String> sortedJoinStepList = joinStepSet.stream().sorted().toList();
            address.setJoinStep(sortedJoinStepList.get(0));

            if ("JC211".equals(address.getJoinStep()) && StringUtils.isNullOrEmpty(address.getArea())) {
                address.setJoinStep("JC311"); //路地名，連寫都沒寫
            }
        }
    }

    private List splitCdStr(String cdStr){
     if(cdStr.indexOf(",")>=0){
       return Arrays.stream(cdStr.split(",")).toList();
     }
        return Arrays.asList(cdStr);
    }

    /**
     * 模糊查詢不會進到這裡
     * @param address
     * @param resultsBeforeSplit = {56碼1:["00000000:JB411:5141047",..],56碼2:["00000000:JB411:5141047",..],...]
     */
    Set<String> mappingCountyTownVilliageNeighbor(Address address, Map<String,Set<String>> resultsBeforeSplit) {
        Set<String> joinStepSet = new HashSet<>();
        Set<String> seqSet = new HashSet<>();
        /**有找到相對應的56碼*/
        if (!resultsBeforeSplit.isEmpty()) {
            /**input的county + town*/
            /*todo:同名不同cd*/
            List countyCds = splitCdStr(address.getCountyCd());
            List townCds = splitCdStr(address.getTownCd());
            ArrayList<String> countyTownCds = new ArrayList<>();
            countyCds.forEach(countyCd->{
                townCds.forEach(townCd->{
                    countyTownCds.add(String.valueOf(countyCd)+townCd);
                });
            });
            log.info("前端輸入縣市、鄉鎮轉代碼:{}", countyTownCds);

            List villageCds = splitCdStr(address.getVillageCd());
            log.info("前端輸入村里轉代碼:{}",villageCds);

            /**input的 county + town、villiage+negihbor 與 redis value比對**/
            log.info("準備比對mappingIds:{}",resultsBeforeSplit);
            if(!resultsBeforeSplit.keySet().isEmpty()){
            for(String mappingId:resultsBeforeSplit.keySet()) {
                if(resultsBeforeSplit.get(mappingId) != null && resultsBeforeSplit.get(mappingId).size()>0){
                for (String seqsStr : resultsBeforeSplit.get(String.valueOf(mappingId))) {
                    log.info("seqsStr:{}",seqsStr);
                        String[] seqArray = seqsStr.split(":");
                        log.info("seqArray[0]:{}",seqArray[0]);
                        String addressCd = seqArray[0];

                    /**county+town優先比對，取其join_step & seq*/
                    if (countyTownCds.contains(addressCd)) {
                        String joinStep = seqArray[1];
                        joinStepSet.add(joinStep);
                        String seq = seqArray[2];
                        seqSet.add(seq);
                        log.info("縣市、鄉鎮市區相符的地址:{}", seq);
                        /**mapping villiage (不是000 就是另外1個唯一代碼)**/
//                        if(!villageCds.isEmpty()){
//                            villageCds.forEach(villageCd->{
//                                if(mappingId.startsWith(String.valueOf(villageCd))) {
//                                    joinStepSet.add(joinStep);
//                                    seqSet.add(seq);
//                                    log.info("里相符的地址:{}", seq);
//                                }
//                            });
//                        }
                    }
                    }
                }
            }
        }
            log.info("seqSet:{}",seqSet);
            log.info("joinStepSet:{}",joinStepSet);
        if(!seqSet.isEmpty()){
            //join_step排序
            List<String> sortedJoinStepList = joinStepSet.stream().sorted().toList();
            address.setJoinStep(sortedJoinStepList.get(0));
            //todo:改取最後大的
//            address.setJoinStep(sortedJoinStepList.get(sortedJoinStepList.size()-1));

            if ("JC211".equals(address.getJoinStep()) && StringUtils.isNullOrEmpty(address.getArea())) {
                address.setJoinStep("JC311"); //路地名，連寫都沒寫
            }
        }
        }
        return seqSet;
    }


    String renewJoinStep(String newStartedcode,IbdTbAddrCodeOfDataStandardDTO ibdTbAddrCodeOfDataStandardDTO){
        String result  =newStartedcode+ibdTbAddrCodeOfDataStandardDTO.getJoinStep().substring(3, 5);
        //取前三碼 + 原本的
        log.info("revised join_step:{}",result);
        ibdTbAddrCodeOfDataStandardDTO.setJoinStep(result);
        return result;
    }


    /**
     * @param address
     * @param resultMap = {"56碼":"63000320:JA111:seq,00000320:JA112:seq,63000000:JA112:seq,.."]}
     * @return Set<String> 所有可能的seq
     */
    Set<String> checkCountyAndTownBeforeSplitSeqAndStep(Address address, Map<String, String> resultMap) {
        /**check county+town 先把它們組成排列組合，然後看MappingId撈出的value比對**/

        List<String> allPossibleTargetCds = allPossibleTargetCd(address);

        String countyTownCd = "";
        String joinStep = "";
        String seq = "";
        Set<String> seqSet = new HashSet<>();
        Set<String> joinStepSet = new HashSet<>();
        if (!resultMap.isEmpty() || resultMap != null) {
            //與比對到的56碼 value 與所有可能的前8碼 相比
            for (String value : resultMap.values()) {
                String[] values = value.split(",");
                for (String valuesStr : values) {
                    String[] valueArrary = valuesStr.split(":");
                    countyTownCd = valueArrary[0];
                    joinStep = valueArrary[1];
                    seq = valueArrary[2];

                    //開始比對所有可能的targetCd
                    for (String targetCd : allPossibleTargetCds) {
                        /**所有可能的seq都要加入，才能判斷是不是多址**/
                        if (targetCd.equals(countyTownCd)) {
                            log.info("比到的townCd:{}", targetCd);
                            joinStepSet.add(joinStep);
                            seqSet.add(seq);
                        }
                    }
                }
            }

            if (joinStepSet.isEmpty()) {
                //完全比對不到,seq也會是空集合
                return seqSet;
            }
            //排序join_step 把第一個塞到address.join_step
            List<String> sortedJoinStepList = joinStepSet.stream().sorted().toList();
            address.setJoinStep(sortedJoinStepList.get(0));
            if ("JC211".equals(address.getJoinStep()) && StringUtils.isNullOrEmpty(address.getArea())) {
                address.setJoinStep("JC311"); //路地名，連寫都沒寫
            }
        }
        return seqSet;
    }

    private List<String> allPossibleTargetCd(Address address) {
        log.info("countyCd:{}", address.getCountyCd());
        log.info("townCd 會有同名不同Cd的狀況:{}", address.getTownCd());
        if (!address.getCountyCd().contains(",")) {
            address.setCounty(address.getCountyCd() + ",");
        }

        if (!address.getTownCd().contains(",")) {
            address.setTown(address.getTownCd() + ",");
        }
        String[] countyCds = address.getCountyCd().split(",");
        String[] townCds = address.getTownCd().split(",");
        log.info("countyCds:{}", countyCds);
        log.info("townCds:{}", townCds);
        ArrayList<String> allPossibleTargetCds = new ArrayList<>();
        for (String countyCd : countyCds) {
            for (String townCd : townCds) {
                String allPossibleTargetCd = countyCd + townCd;
                allPossibleTargetCds.add(allPossibleTargetCd);
            }
        }
        log.info("allPossibleTargetCds:{}", allPossibleTargetCds);
        return allPossibleTargetCds;
    }

    /**
     * 去redis找出地址片段的cd
     * @param address
     * @return
     */
    public Address findCdAndMappingId(Address address) {
        log.info("去redis找出的cd:{}", address);
        //"COUNTY", "TOWN", "VILLAGE", "ROAD", "AREA", "LANE", "ALLEY", 1~7
        //"NUM_FLR_1", "NUM_FLR_2", "NUM_FLR_3", "NUM_FLR_4", "NUM_FLR_5" 8
        //"NEIGHBOR" 9
        segmentExistNumber = "";
        /*===========redis取各地址片段===========================*/
        /**========縣市=========**/
        String county = address.getCounty();
        /**=======鄉鎮市區=========**/
        String town = address.getTown();
        /**=======村里=========**/
        String village = address.getVillage();
        /**=======路段道街=========**/
        String road = address.getRoad();
        /**=======地名=========**/
        String area = address.getArea();
        /**========路段道街 + 地名========**/
        /**road == ""? "" : 轉換變形數字的 road， ex:roadAreaKey:明志路3段大學新村*/
        String roadAreaKey = replaceWithHalfWidthNumber(road) + (area == null ? "" : area);
        /**========巷========**/
        String lane = address.getLane();
        /**========弄========**/
        String alley = address.getAlley();
        /**========衖衕橫========**/
        String subAlley = address.getSubAlley();
        /**========弄+衖衕橫========**/
        String alleyIdSnKey = replaceWithHalfWidthNumber(alley) + replaceWithHalfWidthNumber(subAlley);
        /**=======其他(95),臨(96),建(97),特(98),附(99)========**/
        String numTypeCd = address.getNumTypeCd();
        /**========如果有連號(之45一樓)，要再用正則處理放入num_flr========**/
        if (StringUtils.isNotNullOrEmpty(address.getContinuousNum())) {
            formatCoutinuousFlrNum(address.getContinuousNum(), address);
        }
        /**========NUM_FLR_1~5========**/
        //todo:當層有值，代表前面一定也有值
        String numFlr1 = address.getNumFlr1();
        String numFlr2 = address.getNumFlr2();
        String numFlr3 = address.getNumFlr3();
        String numFlr4 = address.getNumFlr4();
        String numFlr5 = address.getNumFlr5();

        /**========室========**/
        String room = address.getRoom();

        /**===========將各地址片段放進map，default value都是對應字數0===========================*/
        Map<String, String> keyMap = new LinkedHashMap<>();
        /*5碼;縣市*/
        keyMap.put("COUNTY:" + county, "00000");
        /*3碼；鄉鎮市區*/
        keyMap.put("TOWN:" + town, "000");
        /*3碼；里;放入56碼*/
        keyMap.put("VILLAGE:" + village, "000");
        /*roadAreaKey的road要先將數字部分統一成阿拉伯數字*/
        /*7碼；路地名；road & area合併後，放入56碼*/
        keyMap.put("ROADAREA:" + roadAreaKey, "0000000");
        /*沒有要放進56碼，只是為了要看redis有沒有資料(後續更新要件清單，有資料:1，無資料:0)*/
        keyMap.put("ROAD:" + road, "");
        keyMap.put("AREA:" + area, "");
        /* 巷；4碼；統一阿拉伯數 放入56碼*/
        keyMap.put("LANE:" + replaceWithHalfWidthNumber(lane), "0000");
        /* 弄、弄+subAlley；7碼；統一阿拉伯數 放入56碼*/
        keyMap.put("ALLEY:" + alleyIdSnKey, "0000000");
        /* 正規化num_flr_1~5 的地址片段，數字部分統一半形阿拉伯數字*/
        keyMap.put("NUM_FLR_1:" + normalizeFloor(numFlr1, address, "NUM_FLR_1").getNumFlr1(), "000000"); //6
        keyMap.put("NUM_FLR_2:" + normalizeFloor(numFlr2, address, "NUM_FLR_2").getNumFlr2(), "00000"); //5
        keyMap.put("NUM_FLR_3:" + normalizeFloor(numFlr3, address, "NUM_FLR_3").getNumFlr3(), "0000"); //4
        keyMap.put("NUM_FLR_4:" + normalizeFloor(numFlr4, address, "NUM_FLR_4").getNumFlr4(), "000"); //3
        keyMap.put("NUM_FLR_5:" + normalizeFloor(numFlr5, address, "NUM_FLR_5").getNumFlr5(), "0"); //1
        keyMap.put("ROOM:" + replaceWithHalfWidthNumber(address.getRoom()), "00000"); //5
        //===========把存有各地址片段的map丟到redis找cd碼，沒有找到還會再做模糊查詢===========================
        /* keyMap={"COUNTY:新北市":"00000","TOWN:新莊渠":"000",....}
          -----------------
           resultMap ={"COUNTY:新北市":"12345,54321"(同名不同cd的情況),"TOWN:新莊渠":"0000"(找不到cd塞default))}
           1) 有找到 -> cd
           2) 沒有找到 -> default 000
         */
        /**/
        Map<String, String> resultMap = redisService.findSetByKeys(keyMap, segmentExistNumber);
        //===========把找到的各地址片段cd碼組裝好===========================
        /*把找到的cd放入Address物件*/
        /*cd有可能是像"12345,67891"這種字串，因為會有同名不同cd的狀態*/
        address.setCountyCd(resultMap.get("COUNTY:" + county));
        address.setTownCd(resultMap.get("TOWN:" + town));
        address.setVillageCd(resultMap.get("VILLAGE:" + village));
        /**鄰；三碼；不從Redis找cd，直接用拼的**/
        address.setNeighborCd(findNeighborCd(address.getNeighbor()));
        address.setRoadAreaSn(StringUtils.isNullOrEmpty(roadAreaKey) ? "0000000" : resultMap.get("ROADAREA:" + roadAreaKey));
        address.setLaneCd(StringUtils.isNullOrEmpty(lane) ? "0000" : resultMap.get("LANE:" + replaceWithHalfWidthNumber(lane)));
        address.setAlleyIdSn(StringUtils.isNullOrEmpty(alleyIdSnKey) ? "0000000" : resultMap.get("ALLEY:" + alleyIdSnKey));
        /**判斷redis有沒有找到Num_FLR_,沒有的話就手動組 ex:NUM_FLR_1:10樓找不到，就自己組000010**/
        address.setNumFlr1Id(setNumFlrId(resultMap, address, "NUM_FLR_1"));
        address.setNumFlr2Id(setNumFlrId(resultMap, address, "NUM_FLR_2"));
        address.setNumFlr3Id(setNumFlrId(resultMap, address, "NUM_FLR_3"));
        address.setNumFlr4Id(setNumFlrId(resultMap, address, "NUM_FLR_4"));
        address.setNumFlr5Id(setNumFlrId(resultMap, address, "NUM_FLR_5"));
        /*0 => 1~最高樓層 1 => 地下 2 => 屋頂 **/
        String basementStr = address.getBasementStr() == null ? "0" : address.getBasementStr();
        /**===========用num_flr_1~5 組成 numFlrPos===========================**/
        String numFlrPos = getNumFlrPos(address);
        address.setNumFlrPos(numFlrPos);
        /**room 也是用拼接的*/
        address.setRoomIdSn(resultMap.get("ROOM:" + replaceWithHalfWidthNumber(room)));
        //這一段只是印log，如果想拿掉也ok!
        logAddressCodes(address, numTypeCd, basementStr, numFlrPos);
        /**排列組合56碼 mappingId,放入address.mappingId = ["56碼排列1",...],address.mappingIdMAp =[{"COUNTY":"12345","town":"000",..},{...}]**/
        assembleMultiMappingIdWithoutCountyAndTown(address);
        //=====================================================================//

        /**
         * segmentExistNumber，一開始先由12個數字組成，用來判斷每一個欄位，使用者是否有填寫。有寫:1，沒寫:0
         * 編碼如下:
         * COUNTY,TOWN,VILLAGE,ROAD,AREA,LANE,ALLEY (index:0~6)
         * NUM_FLR_1,NUM_FLR_2,NUM_FLR_3,NUM_FLR_4,NUM_FLR_5 (index:7)
         * 送進 combineSegment()後，會合併NUM_FLR_1~5(index = 7-11碼)，變成一個數 (0或1)
         */
        address.setSegmentExistNumber(combineSegment(resultMap.getOrDefault("segmentExistNumber", "")));
        return address;
    }



    private void logAddressCodes(Address address, String numTypeCd, String basementStr, String numFlrPos) {
        log.info("=== getCountyCd:{}", address.getCountyCd());
        log.info("=== getTownCd:{}", address.getTownCd());
        log.info("=== getVillageCd:{}", address.getVillageCd());
        log.info("=== getNeighborCd:{}", address.getNeighborCd());
        log.info("=== getRoadAreaSn:{}", address.getRoadAreaSn());
        log.info("=== getLaneCd:{}", address.getLaneCd());
        log.info("=== getAlleyIdSn:{}", address.getAlleyIdSn());
        log.info("=== numTypeCd:{}", numTypeCd);
        log.info("=== getNumFlr1Id:{}", address.getNumFlr1Id());
        log.info("=== getNumFlr2Id:{}", address.getNumFlr2Id());
        log.info("=== getNumFlr3Id:{}", address.getNumFlr3Id());
        log.info("=== getNumFlr4Id:{}", address.getNumFlr4Id());
        log.info("=== getNumFlr5Id:{}", address.getNumFlr5Id());
        log.info("=== basementStr:{}", basementStr);
        log.info("=== numFlrPos:{}", numFlrPos);
        log.info("=== getRoomIdSn:{}", address.getRoomIdSn());
    }

    /**
     * 處理: 之45一樓、之四十五1樓 (像這種連續的號碼，就會被歸在這裡)
     * firstPattern -> 之45一樓 -> coutinuousNum1切出 之45 ;coutinuousNum2切出 一樓
     * secondPattern -> 之四十五1樓 ->coutinuousNum1切出 之四五;coutinuousNum2切出 1樓
     * 找出目前切到numFlr第幾層，並把切出的結果下塞(應該都會是塞兩層)
     *
     *
     * 1 -> 數字+號 、 數字  ex:1號 、1(NUM_FLR_ID會以7開頭)
     * 2 -> 數字+樓  ex:一樓
     * 3 -> 數字+之  ex:3之  一樓之3 (24) ->  3之一樓 (32)
     * 4-> 之+數字 ex:之4
     * 5 -> 非數字+棟 ex:A棟、乙棟
     * 6 -> 非數字 + 區 ex: A區、甲區
     * 7 -> 非數字+數字 ex:北3、南1
     * @param input
     * @param address
     */
    public void formatCoutinuousFlrNum(String input, Address address) {
        log.info("開始處理連號:{}",input);
        if (StringUtils.isNotNullOrEmpty(input)) {
            String firstPattern = "(?<coutinuousNum1>[之-]+[\\d\\uFF10-\\uFF19]+)(?<coutinuousNum2>\\D+[之樓FｆＦf])?"; //之45一樓
            String secondPattern = "(?<coutinuousNum1>[之-]\\D+)(?<coutinuousNum2>[\\d\\uFF10-\\uFF19]+[之樓FｆＦf])?"; //之四五1樓
            Matcher matcherFirst = Pattern.compile(firstPattern).matcher(input);
            Matcher matcherSecond = Pattern.compile(secondPattern).matcher(input);

            /**找出目前切到第幾層*/
            String[] flrArray = {address.getNumFlr1(), address.getNumFlr2(), address.getNumFlr3(), address.getNumFlr4(), address.getNumFlr5()};
            int count = 0;
            for (int i = 0; i < flrArray.length; i++) {
                if (StringUtils.isNullOrEmpty(flrArray[i])) {
                    log.info("目前最大到:{}，新分解好的flr，就要再往後塞到:{}", "numFlr" + (i), "numFlr" + (i + 1));
                    count = i + 1;
                    break;
                }
            }

            if (matcherFirst.matches()) {
                log.info("符合(1) [之-][半形數字] 或 (2)中文數字[之樓FｆＦf]:{}");
                setFlrNum(count, matcherFirst.group("coutinuousNum1"), matcherFirst.group("coutinuousNum2"), address);
            } else if (matcherSecond.matches()) {
                log.info("符合(1) [之-][中文數字] 或 (2)半形數字[之樓FｆＦf]:{}");
                setFlrNum(count, matcherSecond.group("coutinuousNum1"), matcherSecond.group("coutinuousNum2"), address);
            }
        }
    }


    private void setFlrNum(int count, String first, String second, Address address) {
        log.info("準備把:{} 與 :{} 數字部分統一成半形數字",first,second);
        first = replaceWithHalfWidthNumber(first);
        second = replaceWithHalfWidthNumber(second);
        log.info("目前最大到:{}，要再往後塞到:{}", "numFlr" + (count-1), "numFlr" + (count), "numFlr" + (count+1), "numFlr" + (count+2));
        switch (count) {
            case 1:
                address.setNumFlr1(first);
                address.setNumFlr2(second);
                address.setNumFlr3(address.getAddrRemains()); //剩下跑到remain的就塞到最後
                break;
            case 2:
                address.setNumFlr2(first);
                address.setNumFlr3(second);
                address.setNumFlr4(address.getAddrRemains());//剩下跑到remain的就塞到最後
                break;
            case 3:
                address.setNumFlr3(first);
                address.setNumFlr4(second);
                address.setNumFlr5(address.getAddrRemains());//剩下跑到remain的就塞到最後
                break;
            case 4:
                address.setNumFlr4(first);
                address.setNumFlr5(second);
                break;
        }
    }

    //補"segmentExistNumber"
    private String insertCharAtIndex(String segmentExistNumber, Address address) {
        StringBuilder stringBuilder = new StringBuilder(segmentExistNumber);
        //鄰
        if ("000".equals(address.getNeighborCd())) {
            stringBuilder.insert(3, '0');  //鄰找不到
        } else {
            stringBuilder.insert(3, '1');  //鄰找的到
        }
        stringBuilder.insert(7, '0');  //numTypeCd一律當作找不到，去模糊比對
        stringBuilder.insert(13, '0'); //basementStr一律當作找不到，去模糊比對
        stringBuilder.insert(14, '0'); //numFlrPos一律當作找不到，去模糊比對
        String result = stringBuilder.toString();
        log.info("segmentExistNumber: {}", result);
        return result;
    }

    /**
     * COUNTY,TOWN,VILLAGE,ROAD,AREA,LANE,ALLEY (index:0~6)
     * NUM_FLR_1,NUM_FLR_2,NUM_FLR_3,NUM_FLR_4,NUM_FLR_5 (index:7)
     * 新增 NEIGHOBR,ROOM (index:8,9) -> 別處append
     * @param segmentExistNumber
     * @return
     */
    public static String combineSegment(String segmentExistNumber) {
       //
        if (segmentExistNumber.length() !=12) {
            throw new IllegalArgumentException("segmentExistNumber initial value 應為 14 碼");
        }
        String flrSegNum = "0";
        // Num_FLR_1~5 有寫1各
        // 只要INDEX 7-11碼，有一碼為1，就返回1
        for (int i = 7; i <= 11; i++) {
            if (segmentExistNumber.charAt(i) == '1') {
                flrSegNum = "1";
                break;
            }
        }

        String roonSegNum = segmentExistNumber.substring(segmentExistNumber.length() - 1, segmentExistNumber.length());

        // 保留segmentExistNumber的1到7碼(index=0~6)，並把index 7的值改成flrSegNum
        return segmentExistNumber.substring(0, 7) + flrSegNum+roonSegNum;
    }


    /**
     * 正規化num_flr_1~5的value
     * 統一阿拉伯數 -> 取除basement:字眼 -> 之、樓 取代 -、F
     * ex:basement:二十四- -> 24之
     * @param rawString NUM_FLR_1~5正則比對出的地址片段，address.num_flr_1: basement:二十四-
     * @param address   地址片段物件
     * @param flrType   NUM_FLR_1~5
     * @return address.num_flr_1:24之
     */
    public Address normalizeFloor(String rawString, Address address, String flrType) {
        if (rawString != null) {
            //十樓->10樓;basement:十樓->10樓
            String result = convertFToFloorAndHyphenToZhi(replaceWithHalfWidthNumber(rawString).replace("basement:", ""));
            switch (flrType) {
                case "NUM_FLR_1":
                    address.setNumFlr1(result);
                    break;
                case "NUM_FLR_2":
                    address.setNumFlr2(result);
                    break;
                case "NUM_FLR_3":
                    address.setNumFlr3(result);
                    break;
                case "NUM_FLR_4":
                    address.setNumFlr4(result);
                    break;
                case "NUM_FLR_5":
                    address.setNumFlr5(result);
                    break;
            }
            return address;
        }
        return address;
    }

    //找numFlrId，如果redis裡找不到的，就直接看能不能抽取數字部分，前面補0

    /**
     * @param resultMap ->redis撈出的cd
     * @param address   ->地址片段
     * @param flrType   -> Num_flr名稱
     * @return
     */
    public String setNumFlrId(Map<String, String> resultMap, Address address, String flrType) {
        String result = "";
        //對應NUM_FLR名稱的value、地址片段、NUM_FLR名稱
        //ex:basement:二十四- -> 24之
        address = normalizeFloor(getNumFlrByType(address, flrType), address, flrType);
        //取出數字部分
        String numericPart = replaceWithHalfWidthNumber(extractNumericPart(getNumFlrByType(address, flrType)));
        switch (flrType) {
            case "NUM_FLR_1":
                result = resultMap.get(flrType + ":" + address.getNumFlr1());
                //如果redis找不到的話，取出的result就會跟comparisionValue 相等
                return getResult(result, "000000", numericPart);
            case "NUM_FLR_2":
                result = resultMap.get(flrType + ":" + address.getNumFlr2());
                return getResult(result, "00000", numericPart);
            case "NUM_FLR_3":
                result = resultMap.get(flrType + ":" + address.getNumFlr3());
                return getResult(result, "0000", numericPart);
            case "NUM_FLR_4":
                result = resultMap.get(flrType + ":" + address.getNumFlr4());
                return getResult(result, "000", numericPart);
            case "NUM_FLR_5":
                result = resultMap.get(flrType + ":" + address.getNumFlr5());
                return getResult(result, "0", numericPart);
            default:
                return result;
        }
    }

    /**
     * 如果Redis找不到cd comparisonValue就會與result相等
     *
     * @param result
     * @param comparisonValue
     * @param numericPart
     * @return
     */

    private String getResult(String result, String comparisonValue, String numericPart) {
        if (comparisonValue.equals(result)) {
            return padNumber(comparisonValue, numericPart);
        } else {
            return result;
        }
    }

    /**
     * 鄰的cd，額外處理
     * 提取數字統一成阿拉伯數字，前方補0直至3位
     *
     * @param rawNeighbor 7鄰
     * @return 007
     */
    public String findNeighborCd(String rawNeighbor) {
        if (StringUtils.isNotNullOrEmpty(rawNeighbor)) {
            /**指提取數字**/
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(replaceWithHalfWidthNumber(rawNeighbor));
            if (matcher.find()) {
                String neighborResult = matcher.group();
                // 往前補零，補到三位數
                String paddedNumber = String.format("%03d", Integer.parseInt(neighborResult));
                log.info("提取的數字部分為：{}", paddedNumber);
                return paddedNumber;
            }
        } else {
            log.info("沒有數字部分");
            return "000";
        }
        return "000";
    }

    /**
     * 1 -> 數字+號,數字,之+數字+號  ex:1號、1(NUM_FLR_ID會以7開頭)、之45號 (NUM_FLR_ID 會是null)
     * 2 -> 數字+樓 ex:一樓
     * 3 -> 數字+之 ex:3之 一樓之3 (24) -> 3之一樓 (32)
     * 4->  之+數字 ex:之4
     * 5 -> 非數字+棟 ex:A棟、乙棟
     * 6 -> 非數字 + 區 ex: A區、甲區
     * 7 -> 非數字+數字 ex:北3、南1
     * @param address 取出 NumFlr1~5的地址片段拼成NumFlrPro
     * @return
     */
    public String getNumFlrPos(Address address) {
//      String[] patternFlr1 = {".+號$", ".+樓$", ".+之$"};//1,2,3

        String[] patternFlr1 = {".+號$", ".+樓$", ".+之$", "^之.+", ".+棟$", ".+區$", "^之.+號", "^[A-ZＡ-Ｚ]+$"}; //~號、樓、之、棟、區、之~、之~號、字串內只有能半形、全形大寫英
        String[] patternFlr2 = {".+號$", ".+樓$", ".+之$", "^之.+", ".+棟$", ".+區$", "^之.+號", "^[A-ZＡ-Ｚ]+$"}; //~號、樓、之、棟、區、之~、之~號、字串內只有能半形、全形大寫英
        String[] patternFlr3 = {".+號$", ".+樓$", ".+之$", "^之.+", ".+棟$", ".+區$", "^[0-9０-９a-zA-Zａ-ｚＡ-Ｚ一二三四五六七八九東南西北甲乙丙]+$"};
        String[] patternFlr4 = {".+號$", ".+樓$", ".+之$", "^之.+", ".+棟$", ".+區$", "^[0-9０-９a-zA-Zａ-ｚＡ-Ｚ一二三四五六七八九東南西北甲乙丙]+$"};
        String[] patternFlr5 = {".+號$", ".+樓$", ".+之$", "^之.+", ".+棟$", ".+區$", "^[0-9０-９a-zA-Zａ-ｚＡ-Ｚ一二三四五六七八九東南西北甲乙丙]+$"};


        return getNum(address.getNumFlr1(), patternFlr1) + getNum(address.getNumFlr2(), patternFlr2) +
                getNum(address.getNumFlr3(), patternFlr3) + getNum(address.getNumFlr4(), patternFlr4) +
                getNum(address.getNumFlr5(), patternFlr5);
    }

    /**
     * @param inputString               ->NumFlr1~5地址片段
     * @param patternArray->取出的片段去match NumFlr1~5對應的正則
     * @return 回傳對應到的 patternArray index +1
     */
    private String getNum(String inputString, String[] patternArray) {
        if (inputString != null && !inputString.isEmpty()) {
            for (int i = 0; i < patternArray.length; i++) {
                Pattern pattern = Pattern.compile(patternArray[i]);
                Matcher matcher = pattern.matcher(inputString);
                if (matcher.matches()) {
                    return String.valueOf(i + 1);
                }
            }
        } else {
            //如果沒有該片段地址，就補0
            return "0";
        }
        return "0";
    }

    /**
     * 處出NUM_FLR對應的VALUE
     *
     * @param address
     * @param flrType
     * @return
     */
    private String getNumFlrByType(Address address, String flrType) {
        switch (flrType) {
            case "NUM_FLR_1":
                return address.getNumFlr1();
            case "NUM_FLR_2":
                return address.getNumFlr2();
            case "NUM_FLR_3":
                return address.getNumFlr3();
            case "NUM_FLR_4":
                return address.getNumFlr4();
            case "NUM_FLR_5":
                return address.getNumFlr5();
            default:
                return "";
        }
    }

    /**
     * 這裡改成不排加入COUNTY、TOWN 的組合
     *
     * @param address
     */

    //因為county、town、village、road、area、lane可能會有同名，但不同代碼的狀況，要組出不同的mappingId
    private void assembleMultiMappingId(Address address) {
        String numTypeCd = address.getNumTypeCd(); //臨建特附
        String basementStr = address.getBasementStr() == null ? "0" : address.getBasementStr();
        List<String> countys = new ArrayList<>(splitAndAddToList(address.getCountyCd()));
        List<String> townCds = new ArrayList<>(splitAndAddToList(address.getTownCd()));
        List<String> villageCds = new ArrayList<>(splitAndAddToList(address.getVillageCd()));
        List<String> roadAreaCds = new ArrayList<>(splitAndAddToList(address.getRoadAreaSn()));
        // Add lanes here
        List<String> lanes = new ArrayList<>(splitAndAddToList(address.getLaneCd()));
        //=========================================================================//
        List<LinkedHashMap<String, String>> mappingIdMapList = new ArrayList<>();
        List<String> mappingIdStringList = new ArrayList<>();
        //=======================================================================//
        for (String countyCd : countys) {
            for (String townCd : townCds) {
                for (String villageCd : villageCds) {
                    for (String roadAreaCd : roadAreaCds) {
                        for (String laneCd : lanes) { // Add this loop for lanes
                            LinkedHashMap<String, String> mappingIdMap = new LinkedHashMap<>();
                            mappingIdMap.put("COUNTY", countyCd);
                            mappingIdMap.put("TOWN", townCd);
                            mappingIdMap.put("VILLAGE", villageCd);//里
                            mappingIdMap.put("NEIGHBOR", address.getNeighborCd());
                            mappingIdMap.put("ROADAREA", roadAreaCd);
                            mappingIdMap.put("LANE", laneCd);
                            mappingIdMap.put("ALLEY", address.getAlleyIdSn());//弄
                            mappingIdMap.put("NUMTYPE", numTypeCd);
                            mappingIdMap.put("NUM_FLR_1", address.getNumFlr1Id());
                            mappingIdMap.put("NUM_FLR_2", address.getNumFlr2Id());
                            mappingIdMap.put("NUM_FLR_3", address.getNumFlr3Id());
                            mappingIdMap.put("NUM_FLR_4", address.getNumFlr4Id());
                            mappingIdMap.put("NUM_FLR_5", address.getNumFlr5Id());
                            mappingIdMap.put("BASEMENT", basementStr);
                            mappingIdMap.put("NUMFLRPOS", address.getNumFlrPos());
                            mappingIdMap.put("ROOM", address.getRoomIdSn());
                            List<String> mappingIdList = Stream.of(
                                            countyCd, townCd,
                                            villageCd, address.getNeighborCd(),
                                            roadAreaCd, laneCd, address.getAlleyIdSn(), numTypeCd,
                                            address.getNumFlr1Id(), address.getNumFlr2Id(), address.getNumFlr3Id(), address.getNumFlr4Id(),
                                            address.getNumFlr5Id(), basementStr, address.getNumFlrPos(), address.getRoomIdSn())
                                    .map(Object::toString)
                                    .collect(Collectors.toList());
                            mappingIdMapList.add(mappingIdMap);
                            mappingIdStringList.add(String.join("", mappingIdList));
                            // 將NUMFLRPOS為00000的組合也塞進去mappingIdStringList
                            String oldPos = mappingIdMap.get("NUMFLRPOS");
                            //mappingIdStringList=["64碼排列1","64碼排列2"....]
                            mappingIdStringList.add(replaceNumFlrPosWithZero(mappingIdMap));
                            mappingIdMap.put("NUMFLRPOS", oldPos); //還原
                        }
                    }
                }
            }
        }
        address.setMappingIdMap(mappingIdMapList);
        address.setMappingId(mappingIdStringList);
    }


    /***
     * County 用 00000
     * Town 用 000
     * 再與其他Cd組成56碼
     * @param address
     */
    private void assembleMultiMappingIdWithoutCountyAndTown(Address address) {
        //臨建特附
        String numTypeCd = address.getNumTypeCd();
        String basementStr = address.getBasementStr() == null ? "0" : address.getBasementStr();

        List<String> villageCds = new ArrayList<>(splitAndAddToList(address.getVillageCd()));
        /**彌補鄰是用程式產生的，不會因為找不到而補000，所以要補一組000**/
        List<String> neighborCds = Arrays.asList(address.getNeighborCd(), "000");

        /**彌補redis找得到路地名，但路地名不在該地址的情況，所以要補一組000**/
        List<String> roadAreaCds = new ArrayList<>(splitAndAddToList(address.getRoadAreaSn()));
        //todo:7/31 宗哲討論這個為要件沒寫或寫錯就不硬比了
//        roadAreaCds.add("0000000");
        /**彌補redis找得到巷名，但巷名不在該地址的情況，所以要補一組0000**/
        List<String> lanes = new ArrayList<>(splitAndAddToList(address.getLaneCd()));
        //todo:7/31 宗哲討論這個為要件沒寫或寫錯就不硬比了
//        lanes.add("0000");
        /**彌補redis找得到弄名，但弄名不在該地址的情況，所以要補一組0000000**/
        List<String> alleyIdSns = new ArrayList<>(Arrays.asList(address.getAlleyIdSn()));
        //todo:7/31 宗哲討論這個為要件沒寫或寫錯就不硬比了
//        alleyIdSns.add("0000000");
        /**彌補redis找得到室名，但室名不在該地址的情況，所以要補一組00000**/
        List<String> roomIdSns = Arrays.asList(address.getRoomIdSn(), "00000");

        List<LinkedHashMap<String, String>> mappingIdMapList = new ArrayList<>();
        List<String> mappingIdStringList = new ArrayList<>();
        //
        for (String villageCd : villageCds) {
            for(String neighbor:neighborCds){
                for (String roadAreaCd : roadAreaCds) {
                    for (String laneCd : lanes) {
                        for(String alleyIdSn : alleyIdSns){
                        for (String roomIdsn : roomIdSns) {
                            //一個 mappingIdMap 所有value組成一個 String 是一組mappingId
                            LinkedHashMap<String, String> mappingIdMap = new LinkedHashMap<>();
                            mappingIdMap.put("VILLAGE", villageCd);//里
                            mappingIdMap.put("NEIGHBOR", neighbor);
                            mappingIdMap.put("ROADAREA", roadAreaCd);
                            mappingIdMap.put("LANE", laneCd);
                            mappingIdMap.put("ALLEY", alleyIdSn);//弄
                            mappingIdMap.put("NUMTYPE", numTypeCd);
                            mappingIdMap.put("NUM_FLR_1", address.getNumFlr1Id());
                            mappingIdMap.put("NUM_FLR_2", address.getNumFlr2Id());
                            mappingIdMap.put("NUM_FLR_3", address.getNumFlr3Id());
                            mappingIdMap.put("NUM_FLR_4", address.getNumFlr4Id());
                            mappingIdMap.put("NUM_FLR_5", address.getNumFlr5Id());
                            mappingIdMap.put("BASEMENT", basementStr);
                            mappingIdMap.put("NUMFLRPOS", address.getNumFlrPos());
                            mappingIdMap.put("ROOM", roomIdsn);
                            List<String> mappingIdList = Stream.of(
                                            villageCd, neighbor,
                                            roadAreaCd, laneCd, alleyIdSn, numTypeCd,
                                            address.getNumFlr1Id(), address.getNumFlr2Id(), address.getNumFlr3Id(), address.getNumFlr4Id(),
                                            address.getNumFlr5Id(), basementStr, address.getNumFlrPos(), roomIdsn)
                                    .map(Object::toString)
                                    .collect(Collectors.toList());
                            //一個 mappingIdMap 所有value組成一個 String 是一組mappingId
                            mappingIdMapList.add(mappingIdMap);
                            mappingIdStringList.add(String.join("", mappingIdList));
                            /**彌補NUM_FRL1~5中文部分填錯，造成NUM_FLR_POS錯誤，多拚一組56碼用NUMFLRPOS 00000的組合**/
                            String oldPos = mappingIdMap.get("NUMFLRPOS");
                            mappingIdStringList.add(replaceNumFlrPosWithZero(mappingIdMap));
                            /**還原**/
                            mappingIdMap.put("NUMFLRPOS", oldPos);
                        }
                    }
                }
            }
        }
    }
        address.setMappingIdMap(mappingIdMapList);
        address.setMappingId(mappingIdStringList);
    }


    private static List<String> splitAndAddToList(String input) {
        List<String> result = new ArrayList<>();
        if (input.contains(",")) {
            result.addAll(Arrays.asList(input.split(",")));
        } else {
            result.add(input);
        }
        return result;
    }

    private String replaceNumFlrPosWithZero(Map<String, String> mappingIdMap) {
        StringBuilder sb = new StringBuilder();
        // 將NUMFLRPOS為00000的組合也塞進去
        mappingIdMap.put("NUMFLRPOS", "00000");
        for (Map.Entry<String, String> entry : mappingIdMap.entrySet()) {
            sb.append(entry.getValue());
        }
        return sb.toString();
    }

    //刪除使用者重複input的縣市、鄉鎮
    private String removeRepeatCountyAndTown(SingleQueryDTO singleQueryDTO) {
        String county = singleQueryDTO.getCounty() == null ? "" : singleQueryDTO.getCounty();
        String town = singleQueryDTO.getTown() == null ? "" : singleQueryDTO.getTown();
        Pattern pattern = Pattern.compile("(" + county + town + ")");
        Matcher matcher = pattern.matcher(singleQueryDTO.getOriginalAddress());
        int count = 0;
        String result = singleQueryDTO.getOriginalAddress();
        while (matcher.find()) {
            count++;
            //出現兩次以上，才需要刪除第一次出現的鄉鎮市區
            if (count >= 2) {
                result = singleQueryDTO.getOriginalAddress().replaceFirst(matcher.group(), "");
                log.info("重複輸入:{}", matcher.group());
                return result;
            }
        }
        return result;
    }

    //如果input的地址包含、~就歸類在多重地址，就要吐回"該地址屬於多重地址"
    Boolean checkIfMultiAddress(SingleQueryDTO singleQueryDTO) {
        return singleQueryDTO.getOriginalAddress().matches(".*[、~].*");
    }

    /**
     * 會到這個階段的地址都是前幾個階段沒有比對到母體的地址的前提下
     * 查無資料，JE431、JE421、JE511、JE311會在這邊寫入
     */
    void setJoinStepWhenResultIsEmpty(List<IbdTbAddrCodeOfDataStandardDTO> list, SingleQueryResultDTO result, Address address) {
        if (list.isEmpty()) {
            log.info("DB查無資料");
            IbdTbAddrCodeOfDataStandardDTO dto = new IbdTbAddrCodeOfDataStandardDTO();
            String segNum = address.getSegmentExistNumber();
            log.info("要件清單:{}", segNum);
            log.info("乾淨地址:{}", address.getCleanAddress());

            if (!segNum.startsWith("11") && (StringUtils.isNullOrEmpty(address.getCounty()) ||StringUtils.isNullOrEmpty(address.getCounty()))) {
                //缺少行政區(連寫都沒有寫) >>> 如果最後都沒有比到的話，同時沒有寫 縣市、鄉鎮市區
                //todo:JE431 可能會有join_step是JE431但有找得到地址的情況
                log.info("我是JE431，county 或 town沒寫或找不到cd");
                setResult(dto, result, "JE431", "缺少行政區");

            } else if (segNum.startsWith("11") && '0' == segNum.charAt(3) && '0' == segNum.charAt(4) && '0' == segNum.charAt(5)) {
                //缺少路地名(連寫都沒有寫) >>> 如果最後都沒有比到的話，地址中同時沒有寫路名(3)、地名(4)、巷名(5)、弄(6)
                log.info("我是JE421，路名(3)、地名(4)、巷名(5)、弄(6)，同時沒寫 或 有寫但全都找不到cd");
                setResult(dto, result, "JE421", "缺少路地名");
                log.info("dto:{},result:{}",dto,result);

            } else if (address.getCounty() != null && address.getTown() != null && segNum.startsWith("00")) {
                // JE521 (行政區無法對應)
                //(1) 縣市+鄉鎮市區片段欄位有值，但要件編號00(redis找不到cd)
                log.info("我是JE521，有縣市+鄉鎮市區片段，但redis找不到cd");
                setResult(dto, result, "JE521", "查無地址");
            } else if ((address.getVillage() != null && '0' == segNum.charAt(2)) || (address.getRoad() != null && address.getArea() != null && '0' == segNum.charAt(3) && '0' == segNum.charAt(4))) {
                // JE531 (路地名無法對應)
                //(1) 如果村里有寫，但redis找不到cd
                //(2) 路地名有寫，但路地名redis找不到cd
                log.info("我是JE531:路地名無法對應 ; 村里有寫，但redis找不到cd 或  路地名有寫，但路地名redis找不到cd");
                setResult(dto, result, "JE531", "查無地址");
            } else if (checkSegNum(segNum)) {
                //JE511 (地址完整切割但比對不到母體檔)
                //若有各地址片段不但有寫且有在要件清單(redis找得到地址片段cd)，組成mappingId卻比對不到母體
                log.info("我是JE511:地址完整切割但比對不到母體檔");
                setResult(dto, result, "JE511", "查無地址");
            } else if (address.getOriginalAddress().contains("地號") || address.getOriginalAddress().contains("段號")) {
                //地段號 (地段號)
                setResult(dto, result, "JE311", "地段號");
            } else {
                result.setText("查無地址");
            }
            list.add(dto);
        }else{

        }
        //多增加判斷NEIGHBOR、ROOM 確保JOIN_STEP不會跑掉
    }

    void reviseJoinStep(List<IbdTbAddrCodeOfDataStandardDTO> list, SingleQueryResultDTO result, Address address){
        list.forEach(IbdTbAddrCodeOfDataStandardDTO->{
            //是否含鄰、含室
            IbdTbAddrCodeOfDataStandardDTO.getFullAddress();

        });
    }

    private void setResult(IbdTbAddrCodeOfDataStandardDTO dto, SingleQueryResultDTO result, String joinStep, String text) {
        dto.setJoinStep(joinStep);
        result.setText(text);
    }

    /*
    * 判斷segNum(10碼，新增neighbor,room) 是否完整切割且切割內容都有找到cd碼
    * 前提:COUNTY(1) +TOWN(1) + VILLAGE(0,1都可) +NUM_FLR_ID(1)
    * 邏輯:index: 3(ROAD)、4(AREA)、5(LANE) 、6(ALLEY) 中,ROAD或AREA至少有一個是1,LANE、ALLEY隨便
    * ROAD、AREA -> 10 ,01 + LANE 、ALLEY -> 00,01,10 的排列組合
    * todo:village -> 有寫有找(1) 沒寫(0)
    * */
    private Boolean checkSegNum(String segNum) {
        //road: 1 -> area 0,1 -> alley 0,1 -> lane 0,1
        //road: 0,1 -> area 1 -> alley 0,1 -> lane 0,1
        String hasRoad ="";
        String noRoad ="";

        String[] viilages = new String[]{"0","1"};
        String[] areas = new String[]{"0","1"};
        String[] alleys = new String[]{"0","1"};
        String[] lanes = new String[]{"0","1"};
        Set<String> patterns = new HashSet<>();

        for(String village:viilages) {
            for (String area : areas) {
                for (String lane : lanes) {
                    for (String alley : alleys) {
                        hasRoad = "11"+village+"1" + area + lane + alley+"1";
                        noRoad = "11"+village+"0" + area + lane + alley+"1";
                        patterns.add(hasRoad);
                        patterns.add(noRoad);
                    }
                }
            }
        }
        log.info("patterns:{}",patterns);
        for(String pattern:patterns){
            if(segNum.substring(0,segNum.length()-2).equals(pattern)){
                return true;
            }
        }
        return false;
    }

    private List<IbdTbAddrCodeOfDataStandardDTO> queryAddressData(Address address) {
        /**不影響後續判斷*/
        if ('2' == address.getJoinStep().charAt(3)) {
            //檢查是否history(歷史門牌)，2的話就是history
            log.info("歷史門牌!");
            List<IbdTbIhChangeDoorplateHis> hisList = ibdTbIhChangeDoorplateHisRepository.findByHistorySeq(address.getSeqSet().stream().toList());
            return ibdTbAddrCodeOfDataStandardRepository.findByAddressId(hisList, address);
        } else {
            return ibdTbAddrCodeOfDataStandardRepository.findBySeq(address.getSeqSet().stream().map(Integer::parseInt).collect(Collectors.toList()));
        }
    }



    void build50MappingIds(Address address){
        List<String> newMappingIds =new ArrayList<>();
        address.getMappingId().forEach(mappingId->{
            String newId = "000000" + mappingId.substring(6, mappingId.length());
            newMappingIds.add(newId);
        });
       address.setMappingId(newMappingIds);
        log.info("模糊查詢的mappingIds:{}",address.getMappingId());
    }
}
