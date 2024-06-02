package com.example.pentaho.resource;


import com.example.pentaho.component.*;
import com.example.pentaho.service.UserService;
import com.example.pentaho.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
@Profile("dev")
public class UserResource {

    private final static Logger log = LoggerFactory.getLogger(UserResource.class);

    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier("stringRedisTemplate2") //DB2
    private StringRedisTemplate stringRedisTemplate2;

    private Map<String,Set<String>> resultMap = new HashMap<>();

    private String pattern ="";

    private final String[] namedGroups ={"zipcode", "county", "town", "village", "neighbor", "speciallane", "road", "area", "lane", "alley", "subAlley", "numFlr1", "numFlr2", "numFlr3", "numFlr4", "numFlr5", "continuousNum", "room", "basementStr", "addrRemains", "remark"};

    private final String BASEMENT_PATTERN = "basement:[一二三四五六七八九十百千]+樓"; //經過一次PARSE之後，如果有地下或屋頂，都會被改為basement:開頭
    private final String ALL_CHAR_FOR_ALLEY = "[0-9０-９A-ZＡ-Ｚa-zａ-ｚ\\uFF10-\\uFF19零一二三四五六七八九十百千甲乙丙丁戊己庚壹貳參肆伍陸柒捌玖拾佰卅廿整棟之-]";
    private final String ALL_CHAR = "[0-9０-９A-ZＡ-Ｚa-zａ-ｚ\\uFF10-\\uFF19零一二三四五六七八九十百千甲乙丙丁戊己庚壹貳參肆伍陸柒捌玖拾佰卅廿整棟]";
    private final String DYNAMIC_ALLEY_PART = "|卓厝|安農新邨|吉祥園|蕭厝|泰安新村|美喬|１弄圳東|堤外|中興二村|溝邊|長埤|清水|南苑|二橫路|朝安|黃泥塘|建行新村|牛頭|永和山莊";
    private final String COUNTY = "(?<zipcode>(^\\d{5}|^\\d{3}|^\\d)?)(?<county>.*?縣|.*?市|%s)?";
    private final String TOWN = "(?<town>\\D+?(市區|鎮區|鎮市|[鄉鎮市區])|%s)?";

    /*不含路字串+新里里、村里、里、村hardcore*/
    private final String VILLAGE = "(?<village>(?<!路)(新里里|村里|[^路]*?里|[^路]*?村|%s))?";
    private final String NEIGHBOR = "(?<neighbor>" + ALL_CHAR + "+鄰)?";
    private final String SPECIALLANE = "(?<speciallane>鐵路.*巷|丹路.*巷)?"; //避免被切到路，直接先寫死在這裡
    private final String ROAD = "(?<road>(.*?段|.*?街|.*?大道|.*?路(?!巷)|%s)?)";
    private final String SPECIAL_AREA = "(?<area>%s)?"; //"村"結尾的AREA先抓出來
    private final String LANE = "(?<lane>(?<![段街大道路村]).*?巷|%s)?";
    private final String ALLEY = "(?<alley>" + ALL_CHAR_FOR_ALLEY + "+弄" + DYNAMIC_ALLEY_PART + ")?";
    //todo:ALL_CHAR前會有[//-－之]{1}嗎?*/
    private final String SUBALLEY = "(?<subAlley>" + ALL_CHAR + "+[衖衕橫]{1})?";
    private final String NUMFLR1 = "(?<numFlr1>" + ALL_CHAR + "+[\\-號Ff樓之區棟]{1}|" + BASEMENT_PATTERN + ")?";
    private final String NUMFLR2 = "(?<numFlr2>"+ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +"\\{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]{1}|"+  BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR3 = "(?<numFlr3>"+ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +"[之\\-－]{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]{1}|"+  BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR4 = "(?<numFlr4>"+ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +"[之\\-－]{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]{1}|"+  BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String NUMFLR5 = "(?<numFlr5>"+ALL_CHAR + "+[\\-－號樓FｆＦf之區棟]{1}|" +"[之\\-－]{1}" + ALL_CHAR + "+(?!.*[樓FｆＦf])|" + ALL_CHAR+"+[FｆＦf]{1}|"+  BASEMENT_PATTERN + "|" + ALL_CHAR + "+(?!室))?";
    private final String CONTINUOUS_NUM = "(?<continuousNum>[之\\-－]{1}" + ALL_CHAR + "+[樓FｆＦf]{1})?"; //之四十五F、－45樓、-四5f

    private final String ROOM = "(?<room>.*?室)?";
    private final String BASEMENTSTR = "(?<basementStr>屋頂突出.*層|地下.*層|地下.*樓|地下|地下室|底層|屋頂|頂樓|屋頂突出物|屋頂樓|頂層|頂加|頂)?";
    private final String ADDRREMAINS = "(?<addrRemains>.+)?";
    private final String REMARK = "(?<remark>[\\(\\{\\〈\\【\\[\\〔\\『\\「\\「\\《\\（](.*?)[\\)\\〉\\】\\]\\〕\\』\\」\\}\\」\\》\\）])?";
    //〈〉【】[]〔〕()『』「」{}「」《》（）



    @GetMapping("/login")
    public ResponseEntity<String> login() {
        Login login = userService.findUserByUserName();
        return new ResponseEntity<>(login.getAcessToken().getToken(), HttpStatus.OK);
    }


    /**
     *
     */
    public void practiceSingleQuery(SingleQueryDTO singleQueryDTO) throws InvocationTargetException, IllegalAccessException {
        /**
        *確認originalAddress 是否是"連號"的地址
        *寫一個前後含 ~、的正則
        * */
        String continuteAddressRules = ".*[~、].*";
        boolean isMatch = singleQueryDTO.getOriginalAddress().matches(continuteAddressRules);
        log.info("連號地址:{}",isMatch?":是":":否");
        /***
         * 刪除重複出現在originalAddress中一次以上縣市、區
         */
        getCleanAddress("","","");

        /***
         * 刪除特殊字元 或 指定特殊詞彙
         */
        String specialCommaRules = "[`!@#$%^&*+=|';',\\[\\].<>/！@#￥%……&*+|‘”“’。，\\\\\\s]";
        String specialWords = "台灣省";
        singleQueryDTO.setOriginalAddress(singleQueryDTO.getOriginalAddress().replaceAll(specialWords,"").replaceAll(specialCommaRules,""));
        log.info("去除特殊字元後:{}",singleQueryDTO.getOriginalAddress());

        /**
         *  Redis.Set.Name
         * "COUNTY_ALIAS:", "TOWN_ALIAS:", "VILLAGE_ALIAS:", "ROAD_ALIAS:", "SPECIAL_AREA:"
         */
        String[] setNames = {};
        Map<String, Set<String>> addressPartBySetName = findAddressPartBySetName(setNames);

        /**開始組正則*/
        pattern = getPattern();

        /**開始complie*/
        Matcher firstMatcher = matcher(singleQueryDTO.getOriginalAddress(), null);
        Address firstParse = new Address();
        setAddress(firstParse,firstMatcher);
        /**給 default numType
         * 檢查 addressRemain
         * 臨96、建97、特98、付99
         * 其他95
         * **/


    }

    @PostMapping("/test-handleAddressRemain")
    public Address handleAddressRemain(Address adderss) throws InvocationTargetException, IllegalAccessException {
        String numTypeCd = "95";
        //有remain且沒有被ContinuousNum切出來過
        if(StringUtils.isNotNullOrEmpty(adderss.getAddrRemains()) && StringUtils.isNullOrEmpty(adderss.getContinuousNum())){
            //ContinuousNum([之\\-－]{1}" + ALL_CHAR + "+[樓FｆＦf]{1}) ex:之－45樓、之-四十五F
            //
            //開始比對addressTRemain 並給予NumType
            numTypeCd = getNumType(adderss);
            if(!"95".equals(numTypeCd)){
                matcher(adderss.getOriginalAddress(),adderss);
            }else{
                //
            }
        }
        return adderss;
    }

    public String getNumType(Address address){
        String numType = "";
        String numTypeCd = "95";
        if(address.getAddrRemains().indexOf("臨")>=0){
             numType="臨";
             numTypeCd="96";
        }
        if(address.getAddrRemains().indexOf("建")>=0){
            numType="建";
            numTypeCd="97";
        }
        if(address.getAddrRemains().indexOf("特")>=0){
            numType="特";
            numTypeCd="98";
        }
        if(address.getAddrRemains().indexOf("付")>=0){
            numType="付";
            numTypeCd="99";
        }
        if(!"95".equals(numTypeCd)){
            address.setOriginalAddress(address.getOriginalAddress().replaceAll(numType,""));
        }
        return numTypeCd;

    }

    @GetMapping("/test-matcher")
    public Matcher matcher(String inputAddress,Address address) throws InvocationTargetException, IllegalAccessException {
        if(address == null){
            address = new Address();
        }
        log.info("Address:{}",address);
        Pattern compile = Pattern.compile(pattern);
        Matcher matcher = compile.matcher(inputAddress);
        return matcher;
    }

    /***
     * 處理 basementStr
     * @param address
     * @param matcher
     */
    public void setAddress(Address address,Matcher matcher) throws InvocationTargetException, IllegalAccessException {
        log.info("address:{}",address);
        if(StringUtils.isNotNullOrEmpty(matcher.group("basementStr"))){
            log.info("basementStr{}:",matcher.group("basementStr"));
            pasrseBasementStr(address.getOriginalAddress(),matcher.group("basementStr"),address);
            matcher(address.getOriginalAddress(),address);

        }
        if(matcher.matches()){
            address.setZipcode(matcher.group("zipcode"));
            address.setCounty(matcher.group("county"));
            address.setTown(matcher.group("town"));
            address.setVillage(matcher.group("village"));
            address.setNeighbor(matcher.group("neighbor"));
            address.setRoad(matcher.group("road"));
            address.setArea(matcher.group("area"));
            address.setLane(matcher.group("lane"));
            address.setAlley(matcher.group("alley"));
            address.setSubAlley(matcher.group("subAlley"));
            address.setNumFlr1(matcher.group("numFlr1"));
            address.setNumFlr2(matcher.group("numFlr2"));
            address.setNumFlr3(matcher.group("numFlr3"));
            address.setNumFlr4(matcher.group("numFlr4"));
            address.setNumFlr5(matcher.group("numFlr5"));
            address.setContinuousNum(matcher.group("continuousNum"));
            address.setRoom(matcher.group("room"));
            address.setBasementStr(matcher.group("basementStr"));
            address.setAddrRemains(matcher.group("addrRemains"));
            address.setRemark(matcher.group("remark"));
        }

    }

    /**
     * basementStr 正則細分三類組
     * 第一類組比對 地下|地下室|... keyWord 改成 一樓，Address.basementStr 改 1
     * 第二類組比對 屋頂|頂樓|... keyWord 改成 ""，Address.basementStr 改 2
     * 第三類組比對 屋頂突出.*層","地下.*層,keyWord 改成 basement:.*(阿拉伯數字)樓，Address.basementStr 改 1,2
     * 用 Address.basementStr 分 0(非地下、屋頂), 1(地下) , 2(屋頂)
     * ex: 地下 -> 一樓
     *     屋頂 -> ""
     *     地下貳層 -> basement:2樓,Address.basementStr = 1
     *     屋頂突出拾八層 -> basement:18樓,Address.basementStr = 2
     * @param basementStr
     * @return
     */
    public void pasrseBasementStr(String originalAddress,String basementStr,Address address){
         String[] basementPattern1 ={"地下","地下室","底層"};
         String[] basementPattern2 ={"屋頂","頂樓","屋頂突出物","屋頂樓","頂層","頂加","頂"};
         String[] basementPattern3 ={"屋頂突出.*層","地下.*層","地下.*樓"};
         if(Arrays.asList(basementPattern1).contains(basementStr)){
             String replacedStr = originalAddress.replaceAll(basementStr, "一樓");
             address.setBasementStr("1");
             address.setOriginalAddress(replacedStr);
             return;
         }

        if(Arrays.asList(basementPattern2).contains(basementStr)){
            String replacedStr = originalAddress.replaceAll(basementStr, "");
            address.setBasementStr("2");
            address.setOriginalAddress(replacedStr);
            return;
        }

        //basementPattern是正則比對
        Pattern compile = null;
        Matcher matcher = null;
        String patternStr = "";
        for(String pattern:basementPattern3){
            compile = Pattern.compile(pattern);
            matcher = compile.matcher(basementStr);
            if(matcher.find()){
                log.info("正則:"+pattern+"比到:{}",matcher.group());
                //取出數字部分並轉換成阿拉伯數字
                String digitalNumer = mappingToDigitalNumer(matcher.group());
                address.setOriginalAddress(originalAddress.replaceAll(matcher.group(),"basement:"+digitalNumer+"樓"));
                patternStr = pattern;
                break;
            }
        }

        /**todo:不太有可能發生 basementStr一個正則都沒比到的情況**/
        if(!StringUtils.isNullOrEmpty(patternStr)){
            switch (patternStr){
                case "屋頂突出.*層":
                    address.setBasementStr("2");
                    break;
                case "地下.*層":
                case "地下.*樓":
                    address.setBasementStr("1");
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * 把 content 內取出各種型態的數字換成阿拉伯數字
     * todo:如果 A-ZＡ-Ｚa-zａ-ｚ 或  甲乙丙丁戊己庚 或 整棟 要替換為什麼數字?
     * @param content
     * @return
     */
    public String extractNumer(String content){
        //
        String[] numPatterns ={"[0-9]","[０-９]","[零一二三四五六七八九十百千壹貳參肆伍陸柒捌玖拾佰卅廿]"};
        Pattern compile = null;
        Matcher numberMatcher = null;
        String result="";

        for(int index = 0;index < numPatterns.length;index++){
             compile = Pattern.compile(numPatterns[index]);
             numberMatcher = compile.matcher(content);
             if(numberMatcher.find()){
                 if(index==0){
                     return numberMatcher.group();
                 }
                 result = mappingToDigitalNumer(numberMatcher.group());
             }
        }
        return result;
    }

    public String mappingToDigitalNumer(String pattern){
        Map<String,String> mappingType1 = new HashMap(){{
            put("零","0");
            put("一","1");
            put("二","2");
            put("三","3");
            put("四","4");
            put("五","4");
            put("六","6");
            put("七","7");
            put("八","8");
            put("九","9'");
            put("十","10");
            put("百","100");
            put("千","1000");
            put("壹","1");
            put("貳","2");
            put("參","3");
            put("肆","4");
            put("伍","5");
            put("陸","6");
            put("柒","7");
            put("捌","8");
            put("玖","9");
            put("拾","10");
            put("佰","100");
            put("卅","30");
            put("廿","20");
        }};

        StringBuilder result = new StringBuilder("");
        char[] chars = pattern.toCharArray();
        for(char s:chars){
            String word = String.valueOf(s);
            if(mappingType1.containsKey(word)){
                result.append(mappingType1.get(word));
            }
        }
        log.info("numberPart:{}",result);
        return result.toString();
    }



    @GetMapping("/test-cleanAddress")
    public SingleQueryDTO getCleanAddress(@RequestParam("county") String county,
            @RequestParam("town") String town,
            @RequestParam("Input") String input){
        SingleQueryDTO singleQueryDTO = new SingleQueryDTO();
        singleQueryDTO.setCounty(county);
        singleQueryDTO.setTown(town);
        singleQueryDTO.setOriginalAddress(input);

        String newCounty = StringUtils.isNullOrEmpty(singleQueryDTO.getCounty()) ? "" : singleQueryDTO.getCounty();
        String newTown = StringUtils.isNullOrEmpty(singleQueryDTO.getTown()) ? "" : singleQueryDTO.getTown();
        /*正則**/
        log.info("點選縣市、區的正則:{}",newCounty+newTown);

        Pattern compile = Pattern.compile(newCounty + newTown);
        Matcher matcher = compile.matcher(singleQueryDTO.getOriginalAddress());
        /*假設點選縣市、區只會append一次在input**/
        int count = 0;
        if(matcher.find()){
            while (matcher.find()){
                count+=1;
                if(count>=2){
                    String append = matcher.group();
                    //全部移除，再把縣市append到最前
                    String cleanStr = singleQueryDTO.getOriginalAddress().replaceAll(append, "");
                    log.info("排除重複內容的乾淨地址:{}",append+cleanStr);
                    singleQueryDTO.setOriginalAddress(append+cleanStr);
                    break;
                }
            }
        }
            return singleQueryDTO;
    }

    @GetMapping("/test-getpattern")
    public String getPattern(){
        //
        String newCounty = String.format(COUNTY , String.join("|",resultMap.get("COUNTY_ALIAS:")));
        String newTown = String.format(TOWN , String.join("|",resultMap.get("TOWN_ALIAS:")));
        String newVillage = String.format(VILLAGE , String.join("|",resultMap.get("VILLAGE_ALIAS:")));
        String newRoad = String.format(ROAD , String.join("|",resultMap.get("ROAD_ALIAS:")));
        String newArea = String.format(SPECIAL_AREA , String.join("|",resultMap.get("SPECIAL_AREA:")));
        String newLane =String.format(LANE,String.join("|",resultMap.get("LANE")));
        pattern = newCounty + newTown + newArea + newVillage + NEIGHBOR + SPECIALLANE + newRoad + newLane + ALLEY + SUBALLEY + NUMFLR1 + NUMFLR2 + NUMFLR3 + NUMFLR4 + NUMFLR5 + CONTINUOUS_NUM + ROOM + BASEMENTSTR + REMARK + ADDRREMAINS;
        log.info("pattern:{}",pattern);
        return pattern;
    }

    @GetMapping("/test-findAddressPartBySetNames")
    private Map<String, Set<String>> findAddressPartBySetName(@RequestParam("setNames") String[] setNames){
        RedisConnection redisConnection = null;
        try{
            //開啟連線
            redisConnection = stringRedisTemplate2.getConnectionFactory().getConnection();
            //序列化工具
            RedisSerializer<String> stringSerializer = stringRedisTemplate2.getStringSerializer();
            for(String setName : setNames){
                log.info("集合名稱:{}",setNames);
                Set<byte[]> bytesSet= redisConnection.sMembers(stringSerializer.serialize(setName));
                Set<String> resultSet = new HashSet<>();
                if(bytesSet.size()>0 && bytesSet !=null){
                    //表示有集合內有資料，反序列化
                    bytesSet.forEach(bytes -> {
                        String deserializeStr = stringSerializer.deserialize(bytes);
                        log.info("反序列:{}",deserializeStr);
                        resultSet.add(deserializeStr);
                    });
                }
                resultMap.put(setName,resultSet);
            }
            log.info("resultMap:{}",resultMap);
        }catch (Exception e){
            log.info("e:{}",e.toString());
        }finally {
            if(redisConnection != null){
                redisConnection.closePipeline();
            }
        }
        return resultMap;
    }


    @GetMapping("/test-compiler")
    public ResponseEntity<Object> testCompiler(@RequestParam("input") String input,@RequestParam("type") String type){
        String setName ="";
        String formatter="";
        HashMap<String, String> compilerResult = new HashMap<>();
        switch (type){
            case "COUNTY":
                setName ="COUNTY_ALIAS:";
                formatter=COUNTY;
            break;
            case "TOWN":
                setName ="TOWN_ALIAS:";
                formatter=TOWN;
                break;
            case "VILLAGE":
                setName ="VILLAGE_ALIAS:";
                formatter=VILLAGE;
                break;
            case "ROAD":
                setName ="ROAD_ALIAS:";
                formatter=ROAD;
                break;
            case "SPECIAL_AREA":
                setName ="SPECIAL_AREA:";
                formatter=SPECIAL_AREA;
                break;
            case "NUMFLR1":
                formatter=NUMFLR1;
                break;
            case "NUMFLR2":
                formatter=NUMFLR2;
                break;
            case "NUMFLR3":
                formatter=NUMFLR3;
                break;
            case "NUMFLR4":
                formatter=NUMFLR4;
                break;
            default:
                break;
        }
        Matcher matcher =null;
        if(!resultMap.containsKey(setName)){
            Pattern compile = Pattern.compile(formatter);
            log.info("正則:{}",formatter);
            matcher = compile.matcher(input);
        }else {
            Set<String> stringSet =  resultMap.get(setName);
            /*stringSet = ["台中市","台北市",...]*/
            String stringSetFormat = String.join("|", stringSet);
            String format = String.format(formatter, stringSetFormat);
            log.info("加上地址片段後format的正則:{}",format);
            Pattern compile = Pattern.compile(format);
            matcher = compile.matcher(input);
        }

        if(matcher.matches()){
            log.info("比對成功");
            for(int count =1;count<=matcher.groupCount();count++){
                String group = matcher.group(count);
                log.info("group"+count+":"+group);
                compilerResult.put(type+":"+group+count,group);
            }
        }
        return new ResponseEntity<>(compilerResult,HttpStatus.OK);
    }

    public String testCounty(String input){
        return "";
    }


    public void numFlr1(){
        String regex = "(?<numFlr2>" +
           /*條件一*/ "[0-9０-９A-ZＡ-Ｚa-zａ-ｚ\\uFF10-\\uFF19零一二三四五六七八九十百千甲乙丙丁戊己庚壹貳參肆伍陸柒捌玖拾佰卅廿整棟]+[\\-－號樓FｆＦf之區棟]{1} |" +
           /*條件二*/ "\\{1}[0-9０-９A-ZＡ-Ｚa-zａ-ｚ\\uFF10-\\uFF19零一二三四五六七八九十百千甲乙丙丁戊己庚壹貳參肆伍陸柒捌玖拾佰卅廿整棟]+(?!.*[樓FｆＦf]) |" +
           /*條件三*/ "[0-9０-９A-ZＡ-Ｚa-zａ-ｚ\\uFF10-\\uFF19零一二三四五六七八九十百千甲乙丙丁戊己庚壹貳參肆伍陸柒捌玖拾佰卅廿整棟]+[FｆＦf]{1} |" +
           /*條件四*/"basement:[一二三四五六七八九十百千]+樓|[0-9０-９A-ZＡ-Ｚa-zａ-ｚ\\uFF10-\\uFF19零一二三四五六七八九十百千甲乙丙丁戊己庚壹貳參肆伍陸柒捌玖拾佰卅廿整棟]+(?!室)" +
           /*條件一~四匹配0~1個字串*/  ")?";
    }
}
