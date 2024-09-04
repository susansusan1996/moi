package com.example.pentaho.service;

import com.example.pentaho.component.*;
import com.example.pentaho.repository.IbdTbAddrCodeOfDataStandardRepository;
import com.example.pentaho.repository.IbdTbIhChangeDoorplateHisRepository;
import com.example.pentaho.utils.AddressParser;
import com.example.pentaho.utils.QrcodeContextUtils;
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

    private static final String[] KEY_WORDS = new String[]{"COUNTY", "TOWN", "VILLAGE", "ROAD", "AREA", "LANE", "ALLEY", "NUM_FLR_1", "NUM_FLR_2", "NUM_FLR_3", "NUM_FLR_4", "NUM_FLR_5", "NEIGHBOR", "ROOM"};


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

        /**===用於判斷num_flr_pos====**/
        List<IbdTbAddrCodeOfDataStandardDTO> mapList = new ArrayList<IbdTbAddrCodeOfDataStandardDTO>();
        List<IbdTbAddrCodeOfDataStandardDTO> unMapList = new ArrayList<>();
        List<IbdTbAddrCodeOfDataStandardDTO> resultList = new ArrayList<>();

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

        /**==從redis的結果中拿出排序最前的joinStep===**/
        String redisMappingJoinStep = address.getJoinStep();

        if (!seqSet.isEmpty()) {
            log.info("有找到seq ===>:{}", seqSet);

            /**用 seq 取 標準地址 跟 numflrpos**/
            list = queryAddressDataAndGetAll(address);

            /**IbdTbAddrDataRepositoryNewdto = 各seq對應的資料**/
            for (IbdTbAddrCodeOfDataStandardDTO IbdTbAddrCodeOfDataStandardNewdto : list) {
                /***
                 * (1) 撈出的join_step 為空
                 * (2) DB撈出join_step + redis+程式比對的join_step 接皆不含 "JE621", "JD721", "JE431", "JE421", "JE511"
                 */

                if (
                        IbdTbAddrCodeOfDataStandardNewdto.getJoinStep() == null
                                || (!EXCLUDED_JOIN_STEPS.contains(IbdTbAddrCodeOfDataStandardNewdto.getJoinStep()) &&
                                !EXCLUDED_JOIN_STEPS.contains(address.getJoinStep()))
                ) {
                    IbdTbAddrCodeOfDataStandardNewdto.setJoinStep(address.getJoinStep());
                }

                /**
                 *(1)來源地址 與 完整地址的num_flr_pos + room 比對去檢核joinStep
                 */
                String addresNumRoom = address.getNumFlrPos() + address.getRoomIdSn();
                String idbNumRoom = IbdTbAddrCodeOfDataStandardNewdto.getNumFlrPos() + IbdTbAddrCodeOfDataStandardNewdto.getRoomIdSn();
                log.info("來源地址num_frl_pos + rooom ===>:{}", addresNumRoom);
                log.info("完整地址num_frl_pos + rooom ===>:{}", idbNumRoom);

                //兩者相等表示前端numflrpos正確，用redis mapping 排序最前的joinStep就好
                if (addresNumRoom.equals(idbNumRoom)) {
                    log.info("來源地址 & 完整地址 號樓之室 <相同>");
                    address.setJoinStep(redisMappingJoinStep);
                    IbdTbAddrCodeOfDataStandardNewdto.setJoinStep(redisMappingJoinStep);
                    mapList.add(IbdTbAddrCodeOfDataStandardNewdto);
                } else {
                    //兩者不相等，表示前端numflrpos經過JB2~5、JC4其中一個轉換才筆對到
                    log.info("來源地址 & 完整地址 號樓之室 <不同> ，開始修正 redis joinStep:{}", redisMappingJoinStep);
                    String joinStep = redisMappingJoinStep;
                    /**跳過JE431..等**/
                    if (!EXCLUDED_JOIN_STEPS.contains(joinStep)) {
                        //用可能地址的num_flr_pos，確認要退到哪一個joinStep
                        log.info("num_flr_pos，確認要退到哪一個joinStep");
                        joinStep = addressParser.checkJoinStepByNumFlrPos(IbdTbAddrCodeOfDataStandardNewdto.getNumFlrPos(), address);
                    }

                    /**要重新拼湊*/
                    if (joinStep.length() < 5) {
                        joinStep = renewJoinStep(joinStep, IbdTbAddrCodeOfDataStandardNewdto);
                    }
                    log.info("檢查完後joinStep:{}", joinStep);
                    address.setJoinStep(joinStep);
                    IbdTbAddrCodeOfDataStandardNewdto.setJoinStep(joinStep);
                    unMapList.add(IbdTbAddrCodeOfDataStandardNewdto);
                }

                String joinStep = IbdTbAddrCodeOfDataStandardNewdto.getJoinStep();
                if (!EXCLUDED_JOIN_STEPS.contains(joinStep)) {
                    joinStep = addressParser.checkJoinStepBySegNum(IbdTbAddrCodeOfDataStandardNewdto.getFullAddress(), address);
                }

                /**要重新拼湊*/
                if (joinStep.length() < 5) {
                    joinStep = renewJoinStep(joinStep, IbdTbAddrCodeOfDataStandardNewdto);
                }
                IbdTbAddrCodeOfDataStandardNewdto.setJoinStep(joinStep);
            }
            ;
        }

        /***==解決:22號 撈出 22號 、 22號五樓==***/
        if (!mapList.isEmpty()) {
            resultList = mapList;
        } else {
            resultList = unMapList;
        }

        Address finalAddress = address;

        Map<Boolean, List<IbdTbAddrCodeOfDataStandardDTO>> filteredList = resultList.stream()
                .filter(dto -> !StringUtils.isNullOrEmpty(dto.getNumFlrId()))
                .collect(Collectors.partitioningBy(dto -> dto.getNumFlrId().equals(finalAddress.getNumFlrId())));

        mapList = filteredList.get(true);
        unMapList = filteredList.get(false);
        log.info("map:{}", mapList);
        log.info("unMap:{}", unMapList);
        resultList = !mapList.isEmpty() ? mapList : unMapList;
        resultList = resultList.stream()
                .collect(Collectors.toMap(
                        IbdTbAddrCodeOfDataStandardDTO::getSeq,
                        dto -> dto,
                        (existing, replacement) -> existing))
                .values()
                .stream()
                .collect(Collectors.toList());

        result.setText("查詢結果");

        //多址判斷
        replaceJoinStepWhenMultiAdress(address, resultList);
        //查無資料，JE431、JE421、JE511、JE311會在這邊寫入
        //resultList -> 空集合;沒有找到任何一個seq
        setJoinStepWhenResultIsEmpty(resultList, result, address);
        result.setData(resultList);
        return result;
    }


    /**
     * @param singleQueryDTO
     * @return
     */
    public List<Map<String, String>> findByDataRepository(SingleQueryDTO singleQueryDTO) {
        /**=== 回傳物件 ===**/
        List<Map<String, String>> result = new ArrayList<>();

        /**=== 可能的地址，會放進 SingleQueryResultDTO ===**/
        List<DataStandardAndRespositoryDTO> list = new ArrayList<>();

        /**===用於判斷num_flr_pos====**/
        List<DataStandardAndRespositoryDTO> map = new ArrayList<>();
        List<DataStandardAndRespositoryDTO> unMap = new ArrayList<>();
        List<DataStandardAndRespositoryDTO> resultList = new ArrayList<>();

        /**=== 確認是否是"連號"的地址 ===**/
        if (checkIfMultiAddress(singleQueryDTO)) {
            HashMap<String, String> msg = new HashMap<>();
            msg.put("msg", "該地址屬於多重地址");
            result.add(msg);
            return result;
        }

        /**=== 刪除使用者重複input的縣市、鄉鎮 ===**/
        String cleanAddress = removeRepeatCountyAndTown(singleQueryDTO);


        /**=== 切地址 + 取得地址cd組合成mappingIdList ===**/
        Address address = parseAddressAndFindMappingId(cleanAddress);

        /**=== 排列組合後56碼mappingId找seq ===**/
        address = findSeq(address);


        /**==seqSet 可能的seqs,可能是空集合==**/
        Set<String> seqSet = address.getSeqSet();


        if (!seqSet.isEmpty()) {
            log.info("有找到seq ===>:{}", seqSet);

            list = queryAllAddressData(address);

            /**IbdTbAddrDataRepositoryNewdto = 各seq對應的資料**/
            for (DataStandardAndRespositoryDTO standardAndRespositoryDTO : list) {
                /**
                 *(1)確認 joinStep 然後與 num_flr_pos + room比對
                 */
                String addresNumRoom = address.getNumFlrPos() + address.getRoomIdSn();
                String idbNumRoom = standardAndRespositoryDTO.getNumFlrPos() + standardAndRespositoryDTO.getRoomIdSn();
                log.info("來源地址num_frl_pos + rooom ===>:{}", addresNumRoom);
                log.info("完整地址num_frl_pos + rooom ===>:{}", idbNumRoom);

                //兩者相等表示前端numflrpos正確，用redis mapping 排序最前的joinStep就好
//               if(address.getNumFlrPos().equals(IbdTbAddrDataRepositoryNewdto.getNumFlrPos())){
                if (addresNumRoom.equals(idbNumRoom)) {
                    log.info("來源地址 & 完整地址 號樓之室 <相同>");
                    map.add(standardAndRespositoryDTO);
                } else {
                    unMap.add(standardAndRespositoryDTO);
                }
            }
            ;
        }

        /***==解決:22號 撈出 22號 、 22號五樓==***/
        //前端輸入的numflrpos有比到
        if (!map.isEmpty()) {
            resultList = map;
        } else {
            resultList = unMap;
        }
        log.info("map:{}", map);
        log.info("unMap:{}", unMap);

        //resultList -> 空集合;沒有找到任何一個seq
        if (resultList.isEmpty()) {
            HashMap<String, String> msg = new HashMap<>();
            msg.put("msg", "查無地址");
            result.add(msg);
            return result;
        }

        Address finalAddress = address;
        resultList.forEach(dto -> {
            Map<String, String> data = extractMistakePart(dto, finalAddress);
            result.add(data);
        });

        return result;
    }

    /**
     * 開始與origrinalAddress比較 -> 用代碼
     */
    private Map<String, String> extractMistakePart(DataStandardAndRespositoryDTO dataStandardAndRespositoryDTO, Address address) {
        log.info("地址片段:{}", address);
        HashMap<String, String> result = new HashMap<>();
        address.setRoad(StringUtils.isNullOrEmpty(address.getRoad()) ? "" : address.getRoad());
        address.setArea(StringUtils.isNullOrEmpty(address.getArea()) ? "" : address.getArea());
        dataStandardAndRespositoryDTO.setRoad(StringUtils.isNullOrEmpty(dataStandardAndRespositoryDTO.getRoad()) ? "" : dataStandardAndRespositoryDTO.getRoad());
        dataStandardAndRespositoryDTO.setArea(StringUtils.isNullOrEmpty(dataStandardAndRespositoryDTO.getArea()) ? "" : dataStandardAndRespositoryDTO.getArea());
        StringBuilder msg = new StringBuilder();

        if (!address.getRoad().equals(dataStandardAndRespositoryDTO.getRoad())) {
            msg.append("路名錯誤").append(";");
        }
        if (!address.getArea().equals(dataStandardAndRespositoryDTO.getArea())) {
            msg.append("地名錯誤").append(";");
        }
        //todo:最後一個判斷msg如果還是維持空字串，表示填寫內容都正確!
        if (StringUtils.isNullOrEmpty(msg.toString())) {
            msg.append("地址填寫正確");
        }

        log.info("msg:{}", msg);
        result.put("msg", msg.toString());
        result.put("origrinalAddress", address.getOriginalAddress());
        result.put("fullAddress", dataStandardAndRespositoryDTO.getFullAddress());
        return result;
    }


    /**
     * 地址切割 + 組合56碼
     *
     * @param input -> 已去除連號、特殊符號、指定關鍵字、重複 county + town 的地址
     * @return
     */
    public Address parseAddressAndFindMappingId(String input) {
        Address address = addressParser.parseAddress(input, null);
        /**把切髒的來源地址初始回去**/
        address.setOriginalAddress(input);
        /**給定預設的numTypeCd，後開始確認**/
        String numTypeCd = "95";
        address.setNumTypeCd(numTypeCd);
        /**
         * handleAddressRemains
         * 臨建特付 -> 拔臨(96),建(97),特(98),付(99)的地址，再切一次
         * 非臨建特付，可能是地名、num_flr沒切出來 -> 拔remain中文當作area，從input中移再切一次
         **/
        handleAddressRemains(address);
        return findCdAndMappingId(address);
    }

    /**
     * 臨建特附(setNumType) 或 地名 (setArea)
     * 拔除後再切割一次地址
     *
     * @param address
     */
    private void handleAddressRemains(Address address) {
        if (StringUtils.isNotNullOrEmpty(address.getAddrRemains()) && StringUtils.isNullOrEmpty(address.getContinuousNum())) {
            log.info("地址有remain:{},且沒有連號:{}", address.getAddrRemains(), address.getContinuousNum());
            log.info("檢查是否含臨(96)、建(97)、特(98)、付(99)、其他(95)");
            String numTypeCd = getNumTypeCd(address);
            if (!"95".equals(numTypeCd)) {
                log.info("我是 <臨建特附>，來源地址拔除臨建特附，再切割一次地址:{}", address.getCleanAddress());
                address = addressParser.parseAddress(address.getCleanAddress(), address);
            } else {
                //todo:除了有可能是AREA沒有切出來 也有可能是NUM_FLR_1~5 導致有remain
                address = addressParser.parseArea(address);
                log.info("我不是 <臨建特附>，提取remain的中文部分當作area,並從input中拔除的地址，再切割一次地址後:{}", address);
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
        Map<String, Set<String>> resultsBy56 = findMapsByKeys(address);
        if (!resultsBy56.isEmpty()) {
            log.info("56碼第一次就有比到，開始filter county town");
            seqSet = mappingCountyAndTown(address, resultsBy56);
        } else {
            log.info("所有56碼都沒找到 往下拔 neighbor & village 進行查詢");
            //(2) redis key查詢 -> 000000 + 50碼
            build50MappingIds(address);
            Map<String, Set<String>> resultsBy50 = fuzzyWithoutVillageAndNeighbor(address);
            //拔鄰、裡查詢後還是都找不到東西
            if (resultsBy50.isEmpty() || resultsBy50 == null) {
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
     * @param resultMap
     * @return ->符合條件的 value
     */
    public Set<String> filterCountyAndTown(Address address, Map<String, Set<String>> resultMap) {
        Set<String> seqSet = new HashSet<>();
        Set<String> joinStepSet = new HashSet<>();
        /**注意同名不同cd的狀況*/
        String[] countys = address.getCountyCd().split(",");
        String[] towns = address.getTownCd().split(",");
        Set<String> countyTowns = new HashSet<String>();
        for (String county : countys) {
            for (String town : towns) {
                log.info("county+town:{}", county + town);
                countyTowns.add(county + town);
            }
        }


        resultMap.keySet().forEach(key -> {
            if (!resultMap.get(key).isEmpty() && resultMap.get(key) != null) {
                resultMap.get(key).forEach(str -> {
                    String[] split = str.split(":");
                    String addressCd = split[0];
                    String seq = split[2];
                    String joinStep = split[1];
                    countyTowns.forEach(countyAndTown -> {
                        if (countyAndTown.equals(addressCd)) {
                            log.info("符合的mapping:{}", key);
                            seqSet.add(seq);
                            joinStepSet.add(joinStep);
                        }
                    });
                });
            }
        });

        //排序join_step 把第一個塞到address.join_step
        List<String> sortedJoinStepList = joinStepSet.stream().sorted().toList();
        if (!joinStepSet.isEmpty()) {
            address.setJoinStep(sortedJoinStepList.get(0));
        }
        if ("JC211".equals(address.getJoinStep()) && StringUtils.isNullOrEmpty(address.getArea())) {
            address.setJoinStep("JC311"); //路地名，連寫都沒寫
        }
        return seqSet;
    }


    /**
     * 沒有使用
     *
     * @param addressId
     * @return
     */
    public List<IbdTbIhChangeDoorplateHis> singleQueryTrack(String addressId) {
        log.info("addressId:{}", addressId);
        return ibdTbIhChangeDoorplateHisRepository.findByAddressId(addressId);
    }


    /**
     * 沒有使用
     * 多址join_step判斷
     *
     * @param address
     * @param seqSet
     */
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
     * 多址join_step判斷
     *
     * @param address
     * @param resultList
     */
    private void replaceJoinStepWhenMultiAdress(Address address, List<IbdTbAddrCodeOfDataStandardDTO> resultList) {
        if (address.getJoinStep() != null && resultList.size() > 1) {

            //todo:判斷地址切割的NUM_FLR_POS是否為於redis比對成功，成功的話代表00000的選擇要拿掉~
            if ("1".equals(address.getSegmentExistNumber().indexOf(7))) {
                //
                resultList.forEach(dto -> {

                });
            }

            switch (address.getJoinStep()) {
                case "JA211", "JA311", "JA212", "JA312" -> address.setJoinStep("JD111");
                case "JB111", "JB112" -> address.setJoinStep("JD311");
                case "JB311" -> address.setJoinStep("JD411");
                case "JB312" -> address.setJoinStep("JD412");
                case "JB411" -> address.setJoinStep("JD511");
                case "JB412" -> address.setJoinStep("JD512");
            }
            resultList.forEach(ele -> {
                ele.setJoinStep(address.getJoinStep());
            });
        }
    }

    /**
     * 沒有使用
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

    /**
     * 找出地址片段的cd
     *
     * @param address
     * @return
     */
    Map<String, Set<String>> findMapsByKeys(Address address) {
        return redisService.findMapsByKeys(address);
    }


    /**
     * 拔鄰、里 模糊查詢
     *
     * @param address
     * @return
     */
    Map<String, Set<String>> fuzzyWithoutVillageAndNeighbor(Address address) {
        return redisService.fuzzyWithoutVillageAndNeighbor(address);
    }

    /**
     * 沒有使用到
     *
     * @param address
     * @return
     */
    Map<String, List<String>> findMapByMappingId(Address address) {
        Map<String, List<String>> result = new HashMap();
        /**排除重複*/
        redisService.findSetsByKeys(address.getMappingId());
        return result;
    }

    /**
     * 沒有使用到
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
            for (String seqsStr : resultsBeforeSplit) {
                String[] seqArray = seqsStr.split(":");
                String addressCd = seqArray[0];
                /**有相符才取其join_step & seq*/
                if (countyAndTown.equals(addressCd)) {
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


    /**
     * 切割字串
     *
     * @param cdStr
     * @return
     */
    private List splitCdStr(String cdStr) {
        if (cdStr.indexOf(",") >= 0) {
            return Arrays.stream(cdStr.split(",")).toList();
        }
        return Arrays.asList(cdStr);
    }

    /**
     * 第一次查詢就有找到，會進到這裡比對county+town
     * 模糊查詢不會進到這裡
     *
     * @param address
     * @param resultsBeforeSplit = {56碼1:["00000000:JB411:5141047",..],56碼2:["00000000:JB411:5141047",..],...]
     */
    Set<String> mappingCountyAndTown(Address address, Map<String, Set<String>> resultsBeforeSplit) {
        Set<String> joinStepSet = new HashSet<>();
        Set<String> seqSet = new HashSet<>();
        /**有找到相對應的56碼*/
        if (!resultsBeforeSplit.isEmpty()) {
            /**input的county + town*/
            /*todo:同名不同cd*/
            List countyCds = splitCdStr(address.getCountyCd());
            List townCds = splitCdStr(address.getTownCd());
            ArrayList<String> countyTownCds = new ArrayList<>();
            countyCds.forEach(countyCd -> {
                townCds.forEach(townCd -> {
                    countyTownCds.add(String.valueOf(countyCd) + townCd);
                });
            });
            log.info("前端輸入縣市、鄉鎮轉代碼:{}", countyTownCds);

            List villageCds = splitCdStr(address.getVillageCd());
            log.info("前端輸入村里轉代碼:{}", villageCds);

            /**input的 county + town、villiage+negihbor 與 redis value比對**/
            log.info("準備比對mappingIds:{}", resultsBeforeSplit);
            if (!resultsBeforeSplit.keySet().isEmpty()) {
                for (String mappingId : resultsBeforeSplit.keySet()) {
                    if (resultsBeforeSplit.get(mappingId) != null && resultsBeforeSplit.get(mappingId).size() > 0) {
                        for (String seqsStr : resultsBeforeSplit.get(String.valueOf(mappingId))) {
                            log.info("seqsStr:{}", seqsStr);
                            String[] seqArray = seqsStr.split(":");
                            log.info("seqArray[0]:{}", seqArray[0]);
                            String addressCd = seqArray[0];

                            /**county+town優先比對，取其join_step & seq*/
                            if (countyTownCds.contains(addressCd)) {
                                String joinStep = seqArray[1];
                                joinStepSet.add(joinStep);
                                String seq = seqArray[2];
                                seqSet.add(seq);
                                log.info("縣市、鄉鎮市區相符的地址:{}", seq);
                            }
                        }
                    }
                }
            }
            log.info("seqSet <:{}>,joinStepSet <:{}>", seqSet,joinStepSet);
            if (!seqSet.isEmpty()) {
                List<String> sortedJoinStepList = joinStepSet.stream().sorted().toList();
                //todo:joinStep取排序最前面的 OK
                address.setJoinStep(sortedJoinStepList.get(0));
                if ("JC211".equals(address.getJoinStep()) && StringUtils.isNullOrEmpty(address.getArea())) {
                    address.setJoinStep("JC311"); //路地名，連寫都沒寫
                }
            }
        }
        return seqSet;
    }


    /**
     * 檢查redis mapping到的joinStep，由前至後
     *
     * @param newStartedcode
     * @param ibdTbAddrCodeOfDataStandardDTO
     * @return
     */
    String renewJoinStep(String newStartedcode, IbdTbAddrCodeOfDataStandardDTO ibdTbAddrCodeOfDataStandardDTO) {
        String result = newStartedcode + ibdTbAddrCodeOfDataStandardDTO.getJoinStep().substring(3, 5);
        //取前三碼 + 原本的
        log.info("revised join_step:{}", result);
        return result;
    }


    /**
     * 沒有用到
     *
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

    /**
     * 沒有用到
     *
     * @param address
     * @return
     */
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
     *
     * @param address
     * @return
     */
    public Address findCdAndMappingId(Address address) {
        log.info("切割好的:{} ==> 要拿去redis找代碼", address);
        /**
         * "COUNTY", "TOWN", "VILLAGE", "ROAD", "AREA", "LANE", "ALLEY", 1~7
         * "NUM_FLR_1", "NUM_FLR_2", "NUM_FLR_3", "NUM_FLR_4", "NUM_FLR_5" 8
         * "NEIGHBOR" 9
         */
        segmentExistNumber = "";
        /**===========redis取各地址片段===========================*/
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

        //todo:因為沒辦法避免 6號86室 誤被切成 6號86 OK
        // 讓num_frl的正則也有 室 的比對，最後再放回正確的address.room
        addressParser.extractRoom(address);

        /**========NUM_FLR_1~5========**/
        //todo:當層有值，代表前面一定也有值 OK
        String numFlr1 = address.getNumFlr1();
        String numFlr2 = address.getNumFlr2();
        String numFlr3 = address.getNumFlr3();
        String numFlr4 = address.getNumFlr4();
        String numFlr5 = address.getNumFlr5();

        /**========室========**/
        String room = address.getRoom();

        /**=========== key:地址片段(不能有null)；default value都是對應字數0===========================*/
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
        //todo:LANE ALLEY ROOM 都由數字組成就不找了
        /* 巷；4碼；統一阿拉伯數 放入56碼*/
        /* 弄、弄+subAlley；7碼；統一阿拉伯數 放入56碼*/
        keyMap.put("ALLEY:" + alleyIdSnKey, "0000000");
        keyMap.put("LANE:" + replaceWithHalfWidthNumber(lane), "0000");
        keyMap.put("ROOM:" + replaceWithHalfWidthNumber(address.getRoom()), "00000"); //5
        /* 正規化num_flr_1~5 的地址片段，數字部分統一半形阿拉伯數字*/
        keyMap.put("NUM_FLR_1:" + normalizeFloor(numFlr1, address, "NUM_FLR_1").getNumFlr1(), "000000"); //6
        keyMap.put("NUM_FLR_2:" + normalizeFloor(numFlr2, address, "NUM_FLR_2").getNumFlr2(), "00000"); //5
        keyMap.put("NUM_FLR_3:" + normalizeFloor(numFlr3, address, "NUM_FLR_3").getNumFlr3(), "0000"); //4
        keyMap.put("NUM_FLR_4:" + normalizeFloor(numFlr4, address, "NUM_FLR_4").getNumFlr4(), "000"); //3
        keyMap.put("NUM_FLR_5:" + normalizeFloor(numFlr5, address, "NUM_FLR_5").getNumFlr5(), "0"); //1
        /**===========把存有各地址片段的map丟到redis找cd碼，沒有找到還會再做模糊查詢===========================*/
        /* keyMap={"COUNTY:新北市":"00000","TOWN:新莊渠":"000",....}
          --------------------------------------------------------
           resultMap ={"COUNTY:新北市":"12345,54321"(同名不同cd的情況),"TOWN:新莊渠":"0000"(找不到cd塞default))}
           1) 有找到 -> cd
           2) 沒有找到 -> default 000
         */
        Map<String, String> resultMap = redisService.findSetByKeys(keyMap, segmentExistNumber);
        /**===========把找到的地址片段cd碼組裝成56碼==========================**/
        /**特別注意會有同名不同cd的狀況 ex:county="63000,12364"***/
        address.setCountyCd(resultMap.get("COUNTY:" + county));
        address.setTownCd(resultMap.get("TOWN:" + town));
        address.setVillageCd(resultMap.get("VILLAGE:" + village));
        /**鄰；三碼；不從Redis找cd，直接用拼的**/
        address.setNeighborCd(findNeighborCd(address.getNeighbor()));
        address.setRoadAreaSn(StringUtils.isNullOrEmpty(roadAreaKey) ? "0000000" : resultMap.get("ROADAREA:" + roadAreaKey));
        address.setLaneCd(StringUtils.isNullOrEmpty(lane) ? "0000" : resultMap.get("LANE:" + replaceWithHalfWidthNumber(lane)));
        address.setAlleyIdSn(StringUtils.isNullOrEmpty(alleyIdSnKey) ? "0000000" : resultMap.get("ALLEY:" + alleyIdSnKey));
        /**判斷redis有沒有找到Num_FLR_,沒有的話就手動組 ex:NUM_FLR_1:10樓找不到，就自己組000010，所以不需要模糊查詢**/
        address.setNumFlr1Id(setNumFlrId(resultMap, address, "NUM_FLR_1"));
        address.setNumFlr2Id(setNumFlrId(resultMap, address, "NUM_FLR_2"));
        address.setNumFlr3Id(setNumFlrId(resultMap, address, "NUM_FLR_3"));
        address.setNumFlr4Id(setNumFlrId(resultMap, address, "NUM_FLR_4"));
        address.setNumFlr5Id(setNumFlrId(resultMap, address, "NUM_FLR_5"));
        /**0 =>一般樓層 / 1 => 地下 / 2 => 屋頂 ***/
        String basementStr = address.getBasementStr() == null ? "0" : address.getBasementStr();
        /**===========用num_flr_1~5 組成 numFlrPos===========================**/
        String numFlrPos = getNumFlrPos(address);
        address.setNumFlrPos(numFlrPos);
        /**room 不去redis找，用程式轉換成5碼 ex:86室 => 00086*/
        address.setRoomIdSn(resultMap.get("ROOM:" + replaceWithHalfWidthNumber(room)));
        //todo:這一段只是印log，如果想拿掉也ok!
        logAddressCodes(address, numTypeCd, basementStr, numFlrPos);
        /**排列組合56碼 mappingId,放入address.mappingId = ["56碼排列1",...],address.mappingIdMAp =[{"COUNTY":"12345","town":"000",..},{...}]**/
        assembleMultiMappingIdWithoutCountyAndTownWithNumFlrId(address);
        /**==============================排列組合結束=======================================*/

        /**
         * segmentExistNumber，一開始先由12個數字組成，用來判斷每一個欄位，使用者是否有填寫。有寫:1，沒寫:0
         * 編碼如下:
         * COUNTY,TOWN,VILLAGE,ROAD,AREA,LANE,ALLEY (index:0~6)
         * NUM_FLR_1,NUM_FLR_2,NUM_FLR_3,NUM_FLR_4,NUM_FLR_5 (index:7)
         * 送進 combineSegment()後，會合併NUM_FLR_1~5(index = 7-11碼)，變成一個數 (0或1)
         */
        address.setSegmentExistNumber(combineSegment(resultMap.getOrDefault("segmentExistNumber", ""), address));
        log.info("整理完的要件清單:{}", address.getSegmentExistNumber());
        return address;
    }


//    private boolean checkSkipOrNot(Address address) {
//        Map<String,String> LANE = new HashMap<>() {{
//                put("keyWord", "巷");
//                put("format", "%04d");
//                put("default", "0000");
//                put("redisKey", "LANE:");
//            }};
//
//            Map<String,String> ROOM = new HashMap<>() {{
//                put("keyWord", "室");
//                put("format", "%05d");
//                put("default", "00000");
//                put("redisKey", "ROOM:");
//            }};
//
//            if (!address.getLane().chars().allMatch(Character::isDigit)) {
//                /* 巷；4碼；統一阿拉伯數 放入56碼*/
//                keyMap.put("LANE:" + replaceWithHalfWidthNumber(address.getLane()), "0000");
//            } else {
//
//            }
//
//            if (!address.getRoom().chars().allMatch(Character::isDigit)) {
//
//            } else {
//
//            }
//        }
//    }


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
     * <p>
     * <p>
     * 1 -> 數字+號 、 數字  ex:1號 、1(NUM_FLR_ID會以7開頭)
     * 2 -> 數字+樓  ex:一樓
     * 3 -> 數字+之  ex:3之  一樓之3 (24) ->  3之一樓 (32)
     * 4-> 之+數字 ex:之4
     * 5 -> 非數字+棟 ex:A棟、乙棟
     * 6 -> 非數字 + 區 ex: A區、甲區
     * 7 -> 非數字+數字 ex:北3、南1
     *
     * @param input
     * @param address
     */
    public void formatCoutinuousFlrNum(String input, Address address) {
        log.info("開始處理連號:{}", input);
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
        log.info("準備把:{} 與 :{} 數字部分統一成半形數字", first, second);
        first = replaceWithHalfWidthNumber(first);
        second = replaceWithHalfWidthNumber(second);
        log.info("目前最大到:{}，要再往後塞到:{}", "numFlr" + (count - 1), "numFlr" + (count), "numFlr" + (count + 1), "numFlr" + (count + 2));
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

    /**
     * 沒有用到
     * 補"segmentExistNumber"
     */

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
     *
     * @param segmentExistNumber
     * @return
     */
    public static String combineSegment(String segmentExistNumber, Address address) {
        log.info("segmentExistNumber:{}", segmentExistNumber);
        //
        if (segmentExistNumber.length() != 12) {
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

//        String roonSegNum = segmentExistNumber.substring(segmentExistNumber.length() - 1, segmentExistNumber.length());
        segmentExistNumber = segmentExistNumber.substring(0, 7) + flrSegNum;
        // 保留segmentExistNumber的1到7碼(index=0~6)，並把index 7,8的值改成negihbor,room
        if (StringUtils.isNullOrEmpty(address.getNeighbor()) || "000".equals(address.getNeighborCd())) {
            segmentExistNumber += "0";
        } else {
            segmentExistNumber += "1";
        }

        if (StringUtils.isNotNullOrEmpty(address.getRoom()) || "00000".equals(address.getNeighborCd())) {
            segmentExistNumber += "0";
        } else {
            segmentExistNumber += "1";
        }

        return segmentExistNumber;
    }


    /**
     * 正規化num_flr_1~5的value
     * 統一阿拉伯數 -> 取除basement:字眼 -> 之、樓 取代 -、F
     * ex:basement:二十四- -> 24之
     *
     * @param rawString NUM_FLR_1~5正則比對出的地址片段，address.num_flr_1: basement:二十四-
     * @param address   地址片段物件
     * @param flrType   NUM_FLR_1~5
     * @return address.num_flr_1:24之
     * -¯－－ ─ ?─
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
     *
     * @param address 取出 NumFlr1~5的地址片段拼成NumFlrPro
     * @return
     */
    public String getNumFlrPos(Address address) {
//      String[] patternFlr1 = {".+號$", ".+樓$", ".+之$"};//1,2,3

        /**每一層去filter 1~7號*/
        /***/
//        String[] patternFlr1 = {".+號$", ".+樓$", ".+之$", "^之.+", ".+棟$", ".+區$", "^之.+號", "^[A-ZＡ-Ｚ]+$"}; //~號、樓、之、棟、區、之~、之~號、字串內只有能半形、全形大寫英
//        String[] patternFlr2 = {".+號$", ".+樓$", ".+之$", "^之.+", ".+棟$", ".+區$", "^之.+號", "^[A-ZＡ-Ｚ]+$"}; //~號、樓、之、棟、區、之~、之~號、字串內只有能半形、全形大寫英
        String[] patternFlr1 = {".+號$", ".+樓$", ".+之$", "^之.+", ".+棟$", ".+區$", "^[0-9０-９a-zA-Zａ-ｚＡ-Ｚ一二三四五六七八九東南西北甲乙丙]+$"};
        String[] patternFlr2 = {".+號$", ".+樓$", ".+之$", "^之.+", ".+棟$", ".+區$", "^[0-9０-９a-zA-Zａ-ｚＡ-Ｚ一二三四五六七八九東南西北甲乙丙]+$"};
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
     * 沒用用到
     * 這裡改成不排加入COUNTY、TOWN 的組合
     * 因為county、town、village、road、area、lane可能會有同名，但不同代碼的狀況，要組出不同的mappingId
     *
     * @param address
     */
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
     * 沒有用到
     * County 用 00000
     * Town 用 000
     * 再與其他Cd組成56碼
     * @param address
     */
    private void assembleMultiMappingIdWithoutCountyAndTown(Address address) {
        /**臨建特附**/
        String numTypeCd = address.getNumTypeCd();
        /**一般樓層、地下室、頂樓**/
        String basementStr = address.getBasementStr() == null ? "0" : address.getBasementStr();

        /**同名不同cd**/
        List<String> villageCds = new ArrayList<>(splitAndAddToList(address.getVillageCd()));

        /**把 退鄰(000) 改到模糊查詢才做 (填錯但存在的鄰撈出的結果，導致找不到地址)**/
        List<String> neighborCds = Arrays.asList(address.getNeighborCd());
//      List<String> neighborCds = Arrays.asList(address.getNeighborCd(), "000");

        /**把 退路(00000) 改到模糊查詢才做 (填錯、模糊查詢找錯，但存在的鄰撈出的結果，導致找不到地址)**/
        //模糊查詢找到cd (road要件清單=0),拼出的mappingid找得到，要件清單原本就0 => 不動
        //寫錯找到cd(road要件清單=1),但最終是0000000 mapping到,要件清單改為0->
        List<String> roadAreaCds = splitAndAddToList(address.getRoadAreaSn());
        //todo:7/31 宗哲討論這個為要件沒寫或寫錯就不硬比了
        roadAreaCds.add("0000000");

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

        for (String villageCd : villageCds) {
            for (String neighbor : neighborCds) {
                for (String roadAreaCd : roadAreaCds) {
                    for (String laneCd : lanes) {
                        for (String alleyIdSn : alleyIdSns) {
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


    /***
     * 排列組合56號碼!
     * 依據 JB~JB5 + JC1 去拼湊 num_flr_Id & num_frl_pos
     * @param address
     */
    private void assembleMultiMappingIdWithoutCountyAndTownWithNumFlrId(Address address) {
        /**臨建特附**/
        String numTypeCd = address.getNumTypeCd();

        /**一般樓層0、地下室1、頂樓2**/
        String basementStr = address.getBasementStr() == null ? "0" : address.getBasementStr();

        /**同名不同cd**/
        List<String> villageCds = new ArrayList<>(splitAndAddToList(address.getVillageCd()));

        //todo:彌補 填錯路地名有找到cd 或 模糊查詢找錯cd，導致mapping不到
        // =>改到56碼的模糊查詢再做，(避免撈出退鄰的結果，joinStep就跑掉變成JA) OK
        List<String> neighborCds = Arrays.asList(address.getNeighborCd());

        List<String> roadAreaCds = new ArrayList<>(splitAndAddToList(address.getRoadAreaSn()));
        //todo:彌補 填錯路地名有找到cd 或 模糊查詢找錯cd，導致mapping不到 OK
        if (!roadAreaCds.contains("0000000")) {
            roadAreaCds.add("0000000");
        }

        List<String> lanes = new ArrayList<>(splitAndAddToList(address.getLaneCd()));
        List<String> alleyIdSns = new ArrayList<>(Arrays.asList(address.getAlleyIdSn()));

        //todo:彌補 填錯室名有找到cd 或 模糊查詢找錯cd，導致mapping不到 OK
        List<String> roomIdSns = new ArrayList<String>() {{
            add(address.getRoomIdSn());
            if (!"00000".equals(address.getRoomIdSn())) {
                add("00000");
            }
        }};

        List<LinkedHashMap<String, String>> mappingIdMapList = new ArrayList<>();
        List<String> mappingIdStringList = new ArrayList<>();


        String originalNumFlrId = address.getNumFlr1Id() + address.getNumFlr2Id() + address.getNumFlr3Id() + address.getNumFlr4Id() + address.getNumFlr5Id();
        address.setNumFlrId(originalNumFlrId);

        //todo:numFlrPos,numFlrId 配合 joinstep 做調整 OK
        assembleNumFlrIdByNumFlrPos(address);
        List<String> filteredNumFlrIds = Arrays.asList(address.getNumFlrId(), address.getJB2NumFlrId(), address.getJB3NumFlrId(), address.getJB4NumFlrId(), address.getJB5NumFlrId(), address.getJC4NumFlrId()).stream()
                .filter(StringUtils::isNotNullOrEmpty)
                .collect(Collectors.toList());


        List<String> filteredNumFlrPos = Arrays.asList(address.getNumFlrPos(), address.getJB2NumFlrPos(), address.getJB3NumFlrPos(), address.getJB4NumFlrPos(), address.getJB5NumFlrPos(), address.getJC4NumFlrPos()).stream()
                .filter(StringUtils::isNotNullOrEmpty)
                .collect(Collectors.toList());

        for (String villageCd : villageCds) {
            for (String neighbor : neighborCds) {
                for (String roadAreaCd : roadAreaCds) {
                    for (String laneCd : lanes) {
                        for (String alleyIdSn : alleyIdSns) {
                            for (String numFlrId : filteredNumFlrIds) {
                                for (String numFlrPos : filteredNumFlrPos) {
                                    for (String roomIdsn : roomIdSns) {
                                        //一個 mappingIdMap 所有value組成一個 String 是一組mappingId
                                        LinkedHashMap<String, String> mappingIdMap = new LinkedHashMap<>();
                                        mappingIdMap.put("VILLAGE", villageCd);//里
                                        mappingIdMap.put("NEIGHBOR", neighbor);
                                        mappingIdMap.put("ROADAREA", roadAreaCd);
                                        mappingIdMap.put("LANE", laneCd);
                                        mappingIdMap.put("ALLEY", alleyIdSn);//弄
                                        mappingIdMap.put("NUMTYPE", numTypeCd);
                                        mappingIdMap.put("NUM_FLR_ID", numFlrId);
                                        mappingIdMap.put("BASEMENT", basementStr);
                                        mappingIdMap.put("NUMFLRPOS", numFlrPos);
                                        mappingIdMap.put("ROOM", roomIdsn);
                                        List<String> mappingIdList = Stream.of(
                                                        villageCd, neighbor,
                                                        roadAreaCd, laneCd, alleyIdSn, numTypeCd,
                                                        numFlrId, basementStr, numFlrPos, roomIdsn)
                                                .map(Object::toString)
                                                .collect(Collectors.toList());
                                        //一個 mappingIdMap 所有value組成一個 String 是一組mappingId
                                        mappingIdMapList.add(mappingIdMap);
                                        mappingIdStringList.add(String.join("", mappingIdList));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        address.setMappingIdMap(mappingIdMapList);
        address.setMappingId(mappingIdStringList);
    }


    /**
     * 來源地址的num_flr_id & num_flr_pos 依照joinStep做調整
     * 退樓、退樓後之、號樓之要件缺漏(遺寫之)
     *
     * @param address
     * @return
     */
    private void assembleNumFlrIdByNumFlrPos(Address address) {
        /**
         * JC4(號樓之缺漏)
         * (1) 7 (?7) 找 之7 (4?) redis有存
         * (2) 之7(4?) 找 7(?7)  num_flr_pos 4的位置換成7,num_frl_Id 首字母換 7
         */
        if (address.getNumFlrPos().indexOf("4") >= 0) {
            int whichFloor = address.getNumFlrPos().indexOf("4") + 1;
            log.info("< 之 > 位於 < num_flr_" + whichFloor);
            log.info("把 <num_flr_Id_" + whichFloor + "> 的value換成7開頭");
            String JC4NumFlrId = replaceFirstNumber(whichFloor, address);
            String JC4NumFlrPos = address.getNumFlrPos().replace("4", "7");
            address.setJC4NumFlrId(JC4NumFlrId);
            address.setJC4NumFlrPos(JC4NumFlrPos);
            log.info("JC4-號樓之要件缺露 < 之7 找 7 > JC4NumFlrId <:{}> , JC4NumFlrPos <:{}> ", JC4NumFlrId, JC4NumFlrPos);
        }


        /**
         * JB2 (樓之之樓)
         * 之樓(32) 找 樓之(24) redis有存
         * 樓之(24) 找 之樓(32): num_flr_pos 換 42
         */

        if (address.getNumFlrPos().indexOf("24") >= 0) {
            address.setJB2NumFlrId(address.getNumFlrId());
            String JB2NumFlrPos = address.getNumFlrPos().replace("24", "32");
            address.setJB2NumFlrPos(JB2NumFlrPos);
            log.info("JB2-樓之之樓轉換 < 樓之 找 之樓 > JB2NumFlrId <:{}> , JB2NumFlrPos <:{}> ", address.getNumFlrId(), JB2NumFlrPos);
        }

        //todo:這組應該不需要，因為redis有存了
        // 1之2樓(32) 找 2樓之1(24)  num_flr_pos 換 24
//        if(address.getNumFlrPos().indexOf("32")>=0){
//            address.setJB2NumFlrId(address.getNumFlrId());
//            address.setJB2NumFlrPos(address.getNumFlrPos().replace("32","24"));
//        }


        //JB3(退樓後之: num_flr_Id '之'那層拔掉，樓前保留，樓後往前移 num_flr_pos '之'=> 20)
//        if(address.getNumFlrPos().indexOf("24")>=0){
//            //二樓之一(24) 找 二樓 (20)
//            int floorPos = address.getNumFlrPos().indexOf("24")+1;
//            int ziAfterFloor = floorPos + 1;
//            log.info("樓之的之位置:{}",ziAfterFloor);
//            String JB3NumFlrId = moveNumFlrForward(address, ziAfterFloor);
//            String JB3NumFlrPos = address.getNumFlrPos().replace("24", "20");
//            log.info("退樓後之 numFlrId:{} , numFlrPos:{}",JB3NumFlrId,JB3NumFlrPos);
//            address.setJB3NumFlrId(JB3NumFlrId);
//            address.setJB3NumFlrPos(JB3NumFlrPos);
//        }


        /**
         * 皓宸joinstep邏輯
         * JB3(退樓後之: num_flr_Id '之'那層拔掉，樓前保留，樓後都拔掉 num_flr_pos)
         * 12000,12100,12400,12440,12700 ->12000
         * 31200,31240,31270 -> 31200
         * 14240,1
         */
        if (address.getNumFlrPos().indexOf("2") >= 0) {
            //二樓之一(24) 找 二樓 (20)
            int floorPos = address.getNumFlrPos().indexOf("2") + 1;
            log.info("<樓> 的位置在 num_flr_" + floorPos);
            String JB3NumFlrId = removeNumFlrAfter(address, floorPos);
            String JB3NumFlrPos = getJB3NumFlrPos(address.getNumFlrPos());
            log.info("退樓後之 numFlrId:{},numFlrPos:{}", JB3NumFlrId, JB3NumFlrPos);
            address.setJB3NumFlrId(JB3NumFlrId);
            address.setJB3NumFlrPos(JB3NumFlrPos);
        }


//        //JB4(退樓 ->  num_flr_Id '樓'那層拔掉，樓前保留，樓後往前移 num_flr_pos 全部是0)
//        if(address.getNumFlrPos().indexOf("2")>=0) {
//            //２３號二樓之一 -> ２３號之１
//            int floorIndex = address.getNumFlrPos().indexOf("2") + 1;
//            String JB4NumFlrId = moveNumFlrForward(address, floorIndex);
//            String JB4NumFlrPos = moveNumFlrPosForward(address, floorIndex);
//            log.info("退樓Id:{},num_Flr_pos:{}",JB4NumFlrId,JB4NumFlrPos);
//            address.setJB4NumFlrId(JB4NumFlrId);
//            address.setJB4NumFlrPos(JB4NumFlrPos);
//        }

        /**
         * 皓宸邏輯
         * JB4 退樓
         * num_flr_Id '樓' 那層開始往後改0，樓前保留
         * num_flr_pos 樓' 那層開始往後改0，樓前保留
         */
        if (address.getNumFlrPos().indexOf("2") >= 0) {
            getJB4NumFlrIdAndNumFlrPos(address);
            log.info("JB4 退樓 num_flr_id <:{}> , num_Flr_pos <:{}> ", address.getJB4NumFlrId(),address.getJB4NumFlrPos());
        }


        /**
         *  JB5 號之之號
         *  之號(31) 找 號之(14) : redis 有存
         *  號之(14) 找 之號(31) : redis 有存
         *  todo:皓宸邏輯補了 14找31，所以這組應該可以刪掉了
         */
//        if (address.getNumFlrPos().indexOf("14") >= 0) {
//            String JB5NumFlrPos = address.getNumFlrPos().replace("14", "31");
//            log.info("號之 找 之號 numFlrId, numFlrPos:{}", address.getNumFlrId(), JB5NumFlrPos);
//            address.setJB5NumFlrId(address.getNumFlrId());
//            address.setJB5NumFlrPos(JB5NumFlrPos);
//        }

    }

    private String replaceFirstNumber(int whichFlr, Address address) {
        String numFlr1Id = address.getNumFlr1Id();
        String numFlr2Id = address.getNumFlr2Id();
        String numFlr3Id = address.getNumFlr3Id();
        String numFlr4Id = address.getNumFlr4Id();
        String numFlr5Id = address.getNumFlr5Id();
        switch (whichFlr) {
            case 1:
                //1以後的往前移
                numFlr1Id = "7" + address.getNumFlr1Id().substring(1, address.getNumFlr1Id().length());
                break;
            case 2:
                numFlr2Id = "7" + address.getNumFlr2Id().substring(1, address.getNumFlr2Id().length());
                break;
            case 3:
                numFlr3Id = "7" + address.getNumFlr3Id().substring(1, address.getNumFlr3Id().length());
                break;
            case 4:
                numFlr4Id = "7" + address.getNumFlr4Id().substring(1, address.getNumFlr4Id().length());
                break;
            default:
                numFlr1Id = address.getNumFlr1Id();
                numFlr2Id = address.getNumFlr2Id();
                numFlr3Id = address.getNumFlr3Id();
                numFlr4Id = address.getNumFlr4Id();
                numFlr5Id = address.getNumFlr5Id();
                break;
        }
        return numFlr1Id + numFlr2Id + numFlr3Id + numFlr4Id + numFlr5Id;

    }

    private String moveNumFlrPosForward(Address address, int replaceIndex) {
        String result = address.getNumFlrPos();
        log.info("目前的num_flr_pos:{},拔除:{},之前保留，之後往前移", result, "NUM_FLR_" + replaceIndex);
        switch (replaceIndex) {
            case 1:
                result = result.substring(1, result.length()) + "0";
                break;
            case 2:
                result = result.substring(0, 1) + result.substring(2, result.length()) + "0";
                break;
            case 3:
                result = result.substring(0, 2) + result.substring(3, result.length()) + "0";
                break;
            case 4:
                result = result.substring(0, 3) + result.substring(4, result.length()) + "0";
                break;
            case 5:
                result = result.substring(0, result.length() - 1) + "0";
                break;
            default:
                break;
        }
        return result;
    }

    private void getJB4NumFlrIdAndNumFlrPos(Address address) {
        String numFlr1Id = address.getNumFlr1Id();
        String numFlr2Id = address.getNumFlr2Id();
        String numFlr3Id = address.getNumFlr3Id();
        String numFlr4Id = address.getNumFlr4Id();
        String numFlr5Id = address.getNumFlr5Id();
        switch (address.getNumFlrPos()) {
            case "10000":
            case "12000":
            case "12100":
            case "12400":
            case "12440":
            case "12700":
            case "15200":
                address.setJB4NumFlrId(numFlr1Id+"0000000000000");
                break;
            case "31000":
            case "31200":
            case "14240":
            case "14200":
            case "14244":
            case "31210":
            case "31240":
            case "31520":
                address.setJB4NumFlrId(numFlr1Id+numFlr2Id+"00000000");
                break;
            case "14420":
            case "33120":
            case "33124":
                address.setJB4NumFlrId(numFlr1Id+numFlr2Id+numFlr3Id+"0000");
                break;
        }


        switch (address.getNumFlrPos()) {
            case "10000":
            case "12000":
            case "12100":
            case "12400":
            case "12440":
            case "12700":
            case "15200":
                address.setJB4NumFlrPos("100000");
                break;
            case "31000":
            case "31200":
            case "31210":
            case "31240":
            case "31520":
                address.setJB4NumFlrPos("31000");
                break;
            case "14420":
                address.setJB4NumFlrPos("14400");
                break;
            case "33120":
            case "33124":
                address.setJB4NumFlrPos("33120");
                break;
        }
    }


    /***
     * 拔除 replaceIndex之後的num_flr_id
     */
    private String removeNumFlrAfter(Address address, int replaceIndex) {
        String numFlr1Id = address.getNumFlr1Id();
        String numFlr2Id = address.getNumFlr2Id();
        String numFlr3Id = address.getNumFlr3Id();
        String numFlr4Id = address.getNumFlr4Id();
        String numFlr5Id = address.getNumFlr5Id();
        log.info("從< num_flr_" + replaceIndex + " >之後開始拔除");
        switch (replaceIndex) {
            case 1:
                //1以後的都改0
                numFlr2Id = "00000";
                numFlr3Id = "0000";
                numFlr4Id = "000";
                numFlr5Id = "0";
                break;
            case 2:
                //2以後的都改0
                numFlr3Id = "0000";
                numFlr4Id = "000";
                numFlr5Id = "0";
                break;
            case 3:
                numFlr4Id = "000";
                numFlr5Id = "0";
                break;
            case 4:
                //4以後的都改0
                numFlr5Id = "0";
                break;
            case 5:
                //5改0
                numFlr5Id = "0";
                break;
            default:
                break;
        }
        return numFlr1Id + numFlr2Id + numFlr3Id + numFlr4Id + numFlr5Id;
    }


    private String getJB3NumFlrPos(String origrinalNumFlrPos) {
        switch (origrinalNumFlrPos) {
            case "12000":
            case "12100":
            case "12400":
            case "12440":
            case "12700":
                return "12000";
            case "31200":
            case "31240":
            case "31270":
                return "31200";
            case "14240":
            case "14244":
                return "14200";
            default:
                return origrinalNumFlrPos;
        }
    }

    /***
     * 指定位置num_flr從num_flr_id拔除，以前的保留，以後的往前遞補
     */
    private String moveNumFlrForward(Address address, int replaceIndex) {
        String numFlr1Id = "";
        String numFlr2Id = "";
        String numFlr3Id = "";
        String numFlr4Id = "";
        String numFlr5Id = "";
        log.info("拔除:{},:{}以前保留，:{}以後往前", replaceIndex, replaceIndex, replaceIndex);
        switch (replaceIndex) {
            case 1:
                //1以後的往前移
                //5 -> 6 (5 +1)
                numFlr1Id = address.getNumFlr2Id().startsWith("7") ? address.getNumFlr2Id().substring(0, 1) + "0" + address.getNumFlr2Id().substring(1, address.getNumFlr2Id().length()) : "7" + address.getNumFlr2Id();
                //4 -> 5 (4+1)
                numFlr2Id = address.getNumFlr3Id().startsWith("7") ? address.getNumFlr3Id().substring(0, 1) + 0 + address.getNumFlr3Id().substring(1, address.getNumFlr3Id().length()) : "0" + address.getNumFlr3Id();
                //3 -> 4 (3+1)
                numFlr3Id = address.getNumFlr4Id().startsWith("7") ? address.getNumFlr4Id().substring(0, 1) + 0 + address.getNumFlr4Id().substring(1, address.getNumFlr4Id().length()) : "0" + address.getNumFlr4Id();
                //1 -> 3 (1+2)
                numFlr4Id = address.getNumFlr5Id().startsWith("7") ? address.getNumFlr5Id().substring(0, 1) + 00 + address.getNumFlr5Id().substring(1, address.getNumFlr5Id().length()) : "00" + address.getNumFlr5Id();
                numFlr5Id = "0";
                break;
            case 2:
                //2拔掉；2以前的保留；2以後的往前移
                numFlr1Id = address.getNumFlr1Id();
                numFlr2Id = address.getNumFlr3Id().startsWith("7") ? address.getNumFlr3Id().substring(0, 1) + 0 + address.getNumFlr3Id().substring(1, address.getNumFlr3Id().length()) : "0" + address.getNumFlr3Id();
                log.info("numFlr2Id:{}", numFlr2Id);
                numFlr3Id = address.getNumFlr4Id().startsWith("7") ? address.getNumFlr4Id().substring(0, 1) + 0 + address.getNumFlr4Id().substring(1, address.getNumFlr4Id().length()) : "0" + address.getNumFlr4Id();
                //1 -> 3 (1+2)
                numFlr4Id = "00" + address.getNumFlr5Id();
                numFlr5Id = "0";
                break;
            case 3:
                //3拔掉；3以前的保留；3以後的往前移
                numFlr1Id = address.getNumFlr1Id();
                numFlr2Id = address.getNumFlr2Id();
                numFlr3Id = address.getNumFlr4Id().startsWith("7") ? address.getNumFlr4Id().substring(0, 1) + "0" + address.getNumFlr4Id().substring(1, address.getNumFlr4Id().length()) : "0" + address.getNumFlr4Id();
                log.info("numFlr3Id:{}", numFlr3Id);
                numFlr4Id = "00" + address.getNumFlr5Id();
                numFlr5Id = "0";
                break;
            case 4:
                //4拔掉；4以前的保留；4以後的往前移
                numFlr1Id = address.getNumFlr1Id();
                numFlr2Id = address.getNumFlr2Id();
                numFlr3Id = address.getNumFlr3Id();
                numFlr4Id = "00" + address.getNumFlr5Id();
                numFlr5Id = "0";
                break;
            case 5:
                //5拔掉；5以前的保留；5以後的往前移
                numFlr1Id = address.getNumFlr1Id();
                numFlr2Id = address.getNumFlr2Id();
                numFlr3Id = address.getNumFlr3Id();
                numFlr4Id = address.getNumFlr4Id();
                numFlr5Id = "0";
                break;
            default:
                numFlr1Id = address.getNumFlr1Id();
                numFlr2Id = address.getNumFlr2Id();
                numFlr3Id = address.getNumFlr3Id();
                numFlr4Id = address.getNumFlr4Id();
                numFlr5Id = address.getNumFlr5Id();
                break;
        }
        return numFlr1Id + numFlr2Id + numFlr3Id + numFlr4Id + numFlr5Id;
    }


    private static List<String> splitAndAddToList(String input) {
        List<String> result = new ArrayList<>();
//        if (input.contains(",")) {
//            String[] split = input.split(",");
//            for(String word :split){
//                result.add(word);
//            }
//        } else {
//            result.add(input);
//        }
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

            if (!segNum.startsWith("11") && (StringUtils.isNullOrEmpty(address.getCounty()) || StringUtils.isNullOrEmpty(address.getCounty()))) {
                //缺少行政區(連寫都沒有寫) >>> 如果最後都沒有比到的話，同時沒有寫 縣市、鄉鎮市區
                //todo:JE431 可能會有join_step是JE431但有找得到地址的情況
                log.info("我是JE431，county 或 town沒寫或找不到cd");
                setResult(dto, result, "JE431", "缺少行政區");

            } else if (segNum.startsWith("11") && '0' == segNum.charAt(3) && '0' == segNum.charAt(4) && '0' == segNum.charAt(5)) {
                //缺少路地名(連寫都沒有寫) >>> 如果最後都沒有比到的話，地址中同時沒有寫路名(3)、地名(4)、巷名(5)、弄(6)
                log.info("我是JE421，路名(3)、地名(4)、巷名(5)、弄(6)，同時沒寫 或 有寫但全都找不到cd");
                setResult(dto, result, "JE421", "缺少路地名");
                log.info("dto:{},result:{}", dto, result);

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
        } else {

        }
        //多增加判斷NEIGHBOR、ROOM 確保JOIN_STEP不會跑掉
    }

    void reviseJoinStep(List<IbdTbAddrCodeOfDataStandardDTO> list, SingleQueryResultDTO result, Address address) {
        list.forEach(IbdTbAddrCodeOfDataStandardDTO -> {
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
        String hasRoad = "";
        String noRoad = "";

        String[] viilages = new String[]{"0", "1"};
        String[] areas = new String[]{"0", "1"};
        String[] alleys = new String[]{"0", "1"};
        String[] lanes = new String[]{"0", "1"};
        Set<String> patterns = new HashSet<>();

        for (String village : viilages) {
            for (String area : areas) {
                for (String lane : lanes) {
                    for (String alley : alleys) {
                        hasRoad = "11" + village + "1" + area + lane + alley + "1";
                        noRoad = "11" + village + "0" + area + lane + alley + "1";
                        patterns.add(hasRoad);
                        patterns.add(noRoad);
                    }
                }
            }
        }
        log.info("patterns:{}", patterns);
        for (String pattern : patterns) {
            if (segNum.substring(0, segNum.length() - 2).equals(pattern)) {
                return true;
            }
        }
        return false;
    }


    /***
     * 檢查input的 num_flr_pos 與 標準地址的 num_frl_pos
     * 解決 ２２號 撈出 ２２號,２２號五樓,...
     * @param address
     * @return
     */
    private List<IbdTbAddrCodeOfDataStandardDTO> queryAddressDataAndGetAll(Address address) {
        /**不影響後續判斷*/
        /**檢查是否history(歷史門牌)，2的話就是history**/
        if ('2' == address.getJoinStep().charAt(3)) {
            //檢查是否history(歷史門牌)，2的話就是history
            log.info("歷史門牌! ADDRESS_ID , STATUS, HISTORY_SEQ , ADR_VERSION, UPDATE_CODE ");
            List<IbdTbIhChangeDoorplateHis> hisList = ibdTbIhChangeDoorplateHisRepository.findByHistorySeq(address.getSeqSet().stream().toList());
            //seq撈出num_flr_pos 要檢查
            return ibdTbAddrCodeOfDataStandardRepository.findByAddressIdGetNumFlrPOS(hisList, address);
        } else {
            return ibdTbAddrCodeOfDataStandardRepository.findBySeqsGetNumFlrPOS(address.getSeqSet().stream().map(Integer::parseInt).collect(Collectors.toList()));
        }


    }


    /***
     * for 地址片段錯誤回覆
     * @param address
     * @return
     */
    private List<DataStandardAndRespositoryDTO> queryAllAddressData(Address address) {
        /**不影響後續判斷*/
        /**檢查是否history(歷史門牌)，2的話就是history**/
        if ('2' == address.getJoinStep().charAt(3)) {
            //檢查是否history(歷史門牌)，2的話就是history
            log.info("歷史門牌!");
            List<IbdTbIhChangeDoorplateHis> hisList = ibdTbIhChangeDoorplateHisRepository.findByHistorySeq(address.getSeqSet().stream().toList());
            //seq撈出num_flr_pos 要檢查
            return null;
//            return ibdTbAddrCodeOfDataStandardRepository.findByAddressId(hisList, address);
        } else {
            log.info("檢查要件清單中的index:7(num_flr_1~5的總和):{}", address.getSegmentExistNumber().indexOf(7));
            log.info("address.getSeqSet().size():{}", address.getSeqSet().size());
            return ibdTbAddrCodeOfDataStandardRepository.findFromDataStandardAndRepository(address.getSeqSet().stream().map(Integer::parseInt).collect(Collectors.toList()));
        }


    }

    /**
     * 沒有使用
     *
     * @param address
     * @return
     */
    private List<IbdTbAddrCodeOfDataStandardDTO> queryAddressData(Address address) {
        /**不影響後續判斷*/
        if ('2' == address.getJoinStep().charAt(3)) {
            //檢查是否history(歷史門牌)，2的話就是history
            log.info("歷史門牌!");
            List<IbdTbIhChangeDoorplateHis> hisList = ibdTbIhChangeDoorplateHisRepository.findByHistorySeq(address.getSeqSet().stream().toList());
            return ibdTbAddrCodeOfDataStandardRepository.findByAddressId(hisList, address);
        } else {
            log.info("address.getSegmentExistNumber().indexOf(7):{}", address.getSegmentExistNumber().indexOf(7));
            log.info("address.getSeqSet().size():{}", address.getSeqSet().size());
            //todo:如果要件清單中的NUM_FLR_POS(index:7) == 1，要跑InnerJoin的sql
            if ("1".equals(address.getSegmentExistNumber().substring(7, 8)) && address.getSeqSet().size() > 1) {
                if (address.getSegmentExistNumber().endsWith("1")) {
                    //表示成
                }
                return ibdTbAddrCodeOfDataStandardRepository.findBySeqsAndNumFlrPOS(address.getSeqSet().stream().map(Integer::parseInt).collect(Collectors.toList()), address.getNumFlrPos());
            }
            return ibdTbAddrCodeOfDataStandardRepository.findBySeq(address.getSeqSet().stream().map(Integer::parseInt).collect(Collectors.toList()));
        }
    }


    /**
     * 模糊查詢
     *
     * @param address
     */
    void build50MappingIds(Address address) {
        List<String> newMappingIds = new ArrayList<>();
        address.getMappingId().forEach(mappingId -> {
            String newId = "000000" + mappingId.substring(6, mappingId.length());
            newMappingIds.add(newId);
        });
        address.setMappingId(newMappingIds);
        log.info("模糊查詢的mappingIds:{}", address.getMappingId());
    }


    /**
     * OpenPage
     *
     * @return
     */
    public List<OpenPageDTO> findbBySeq() {
        OpenPageDTO.QrcodeDTO qrcodeData = QrcodeContextUtils.getQrcodeData();
        log.info("QrcodeDTO:{}", qrcodeData.toString());
        List<OpenPageDTO> bySeq = ibdTbAddrCodeOfDataStandardRepository.findBySeq(Integer.valueOf(qrcodeData.getSeq()));
        bySeq.forEach(data -> {
            data.setJoinStep(qrcodeData.getJoinStep());
            data.setOrigrinalAddress(qrcodeData.getOriginalAddress());
        });
        return bySeq;
    }


    public List<OpenPageDTO> findbBySeq(Map<String, String> param) {
        String seq = param.get("taskId");
        String origrinalAddress = param.get("origrinalAddress");
        String joinStep = param.get("joinStep");

        List<OpenPageDTO> bySeq = ibdTbAddrCodeOfDataStandardRepository.findBySeq(Integer.valueOf(seq));
        bySeq.forEach(data -> {
            data.setJoinStep(joinStep);
            data.setOrigrinalAddress(origrinalAddress);
        });
        return bySeq;
    }


}
