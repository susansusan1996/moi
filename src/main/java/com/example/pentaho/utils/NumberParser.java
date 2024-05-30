package com.example.pentaho.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberParser {
    private static final Logger log = LoggerFactory.getLogger(NumberParser.class);

    /***
     * 有可能會有 basement:一樓
     * [-－] 半形、全形- 用 之 取代
     * 其餘 表達數字的字符 轉為 半形數字
     * @param input
     * @return
     */
    public static String replaceWithHalfWidthNumber(String input) {
        if (input != null && !input.isEmpty()) {
            input = input.replaceAll("[-－]", "之");
             /* 判斷有無匹配 .*(十|零).* */
            if (containsLittleChineseNumbers(input)) {
                log.info("含有中文數字:{}", input);
                String newNum = LittleDigitConvert.convertToDigit(input);
                newNum = assembleString(input,newNum);
                log.info("含有中文數字，新字串:{}", newNum);
                return newNum;
             /* .*(拾|佰).* */
            } else if (containsBigChineseNumbers(input)) {
                log.info("含有大寫中文數字:{}", input);
                String newNum = BigDigitConvert.convertToDigit(input);
                newNum = assembleString(input,newNum);
                log.info("含有大寫中文數字，新字串:{}", newNum);
                return newNum;
            /* .*(０|１|２|３|４|５|６|７|８|９|一|二|三|四|五|六|七|八|九|零|壹|貳|參|肆|伍|陸|柒|捌|玖|卅|廿).* */
            } else if (containsNumbers(input)) {
                Map<String, String> map = new HashMap<>();
                map.put("０", "0");
                map.put("１", "1");
                map.put("２", "2");
                map.put("３", "3");
                map.put("４", "4");
                map.put("５", "5");
                map.put("６", "6");
                map.put("７", "7");
                map.put("８", "8");
                map.put("９", "9");
                map.put("一", "1");
                map.put("二", "2");
                map.put("三", "3");
                map.put("四", "4");
                map.put("五", "5");
                map.put("六", "6");
                map.put("七", "7");
                map.put("八", "8");
                map.put("九", "9");
                map.put("零", "0");
                map.put("壹", "1");
                map.put("貳", "2");
                map.put("參", "3");
                map.put("肆", "4");
                map.put("伍", "5");
                map.put("陸", "6");
                map.put("柒", "7");
                map.put("捌", "8");
                map.put("玖", "9");
                map.put("卅", "3"); //30的意思
                map.put("廿", "2"); //20的意思
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < input.length(); i++) {
                    String currentChar = String.valueOf(input.charAt(i));
                    if (map.containsKey(currentChar)) {
                        result.append(map.get(currentChar));
                    } else {
                        result.append(currentChar);
                    }
                }
                log.info("數字改為:{}", result);
                return result.toString();
            }
        } else if (input == null) {
            return "";
        }
        return input;
    }

    //檢查有無匹配國字大小寫數字
    public static boolean containsLittleChineseNumbers(String input) {
        String regex = ".*(十|零).*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }


    public static boolean containsBigChineseNumbers(String input) {
        String regex = ".*(拾|佰).*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }

    public static boolean containsNumbers(String input) {
        String regex = ".*(０|１|２|３|４|５|６|７|８|９|一|二|三|四|五|六|七|八|九|零|壹|貳|參|肆|伍|陸|柒|捌|玖|卅|廿).*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }

    private static final HashMap<Character, Integer> numberMap = new HashMap<>();

    static {
        numberMap.put('零', 0);
        numberMap.put('一', 1);
        numberMap.put('二', 2);
        numberMap.put('三', 3);
        numberMap.put('四', 4);
        numberMap.put('五', 5);
        numberMap.put('六', 6);
        numberMap.put('七', 7);
        numberMap.put('八', 8);
        numberMap.put('九', 9);
    }

    //中文數字改阿拉伯數字
    public static String chineseNumberToArabic(String chineseNumber) {
        Pattern p;
        Matcher m;
        for (String regex : regexMap.keySet()) {
            p = Pattern.compile(regex);
            m = p.matcher(chineseNumber);
            while (m.find()) {
                String exper = regexMap.get(regex);
                List<String> list = new ArrayList<>();
                for (int i = 1; i <= m.groupCount(); i++) {
                    list.add(NumRegex.numMap.get(m.group(i)));
                }
                exper = MessageFormat.format(exper, list.toArray());
                String text = m.group();
                String value = experToValue(exper);
                chineseNumber = chineseNumber.replace(text, value);
            }
        }
        return chineseNumber;
    }


    public static String replaceWithChineseNumber(String input) {
        if (input != null && !input.isEmpty()) {
            Map<String, String> map = new HashMap<>();
            map.put("壹", "一");
            map.put("貳", "二");
            map.put("叁", "三");
            map.put("肆", "四");
            map.put("伍", "五");
            map.put("陸", "六");
            map.put("柒", "七");
            map.put("捌", "八");
            map.put("玖", "九");
            map.put("零", "零");
            map.put("一", "一");
            map.put("二", "二");
            map.put("三", "三");
            map.put("四", "四");
            map.put("五", "五");
            map.put("六", "六");
            map.put("七", "七");
            map.put("八", "八");
            map.put("九", "九");
            map.put("０", "零");
            map.put("１", "一");
            map.put("２", "二");
            map.put("３", "三");
            map.put("４", "四");
            map.put("５", "五");
            map.put("６", "六");
            map.put("７", "七");
            map.put("８", "八");
            map.put("９", "九");
            map.put("０", "零");
            map.put("1", "一");
            map.put("2", "二");
            map.put("3", "三");
            map.put("4", "四");
            map.put("5", "五");
            map.put("6", "六");
            map.put("7", "七");
            map.put("8", "八");
            map.put("9", "九");
            map.put("0", "零");
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < input.length(); i++) {
                String currentChar = String.valueOf(input.charAt(i));
                if (map.containsKey(currentChar)) {
                    result.append(map.get(currentChar));
                } else {
                    result.append(currentChar);
                }
            }
            log.info("數字改為:{}", result.toString());
            return result.toString();
        }
        return "";
    }

    //將F轉換成樓，-轉成之
    public static String convertFToFloorAndHyphenToZhi(String input) {
        if (input.endsWith("F") || input.endsWith("ｆ") || input.endsWith("Ｆ") || input.endsWith("f")) {
            String result = input.substring(0, input.length() - 1) + "樓";
            return result.replace("-", "之");
        } else {
            return input.replace("-", "之");
        }
    }

    public static String extractNumericPart(String input) {
        if (StringUtils.isNotNullOrEmpty(input)) {
            Pattern numericPattern = Pattern.compile("[一二三四五六七八九十百千0-9]+");
            Matcher numericMatcher = numericPattern.matcher(input);
            if (numericMatcher.find()) {
                return numericMatcher.group();
            }
        }
        return "";
    }

    //補0
    public static String padNumber(String comparisonValue, String numPart) {
        // 計算需要補充0的個數
        int zeroPaddingCount = Math.max(0, comparisonValue.length() - numPart.length());
        // 在數字部分前面補0
        StringBuilder paddedNumBuilder = new StringBuilder();
        for (int i = 0; i < zeroPaddingCount; i++) {
            paddedNumBuilder.append("0");
        }
        paddedNumBuilder.append(numPart);
        return paddedNumBuilder.toString();
    }


    public static String experToValue(String exper) {
        String[] experArr = null;
        experArr = exper.split(encodeUnicode("+"));

        int value = 0;
        for (String sExper : experArr) {
            String[] sExperArr = sExper.split(encodeUnicode("*"));
            value += Integer.valueOf(sExperArr[0]) * Integer.valueOf(sExperArr[1]);
        }
        return String.valueOf(value);
    }

    //轉換為unicode
    private static String encodeUnicode(String gbString) {
        char[] utfBytes = gbString.toCharArray();
        String unicodeBytes = "";
        for (int i : utfBytes) {
            String hexB = Integer.toHexString(i);
            if (hexB.length() <= 2) {
                hexB = "00" + hexB;
            }
            unicodeBytes = unicodeBytes + "\\u" + hexB;
        }
        return unicodeBytes;
    }


    //一、十一、二十一、三百二十一、三百零一、二十、三百、三百二、十
    private static final Map<String, String> regexMap = new LinkedHashMap<String, String>();
    static {
        //三百二十一
        String regex = NumRegex.getNumRegex() + encodeUnicode("百") + NumRegex.getNumRegex() + encodeUnicode("十") + NumRegex.getNumRegex();
        String exper = "{0}*100+{1}*10+{2}*1";
        regexMap.put(regex, exper);
        //三百零一
        regex = NumRegex.getNumRegex() + encodeUnicode("百") + encodeUnicode("零") + NumRegex.getNumRegex();
        exper = "{0}*100+{1}*1";
        regexMap.put(regex, exper);
        //三百二
        regex = NumRegex.getNumRegex() + encodeUnicode("百") + NumRegex.getNumRegex();
        exper = "{0}*100+{1}*10";
        regexMap.put(regex, exper);
        //三百
        regex = NumRegex.getNumRegex() + encodeUnicode("百");
        exper = "{0}*100";
        regexMap.put(regex, exper);
        //二十一
        regex = NumRegex.getNumRegex() + encodeUnicode("十") + NumRegex.getNumRegex();
        exper = "{0}*10+{1}*1";
        regexMap.put(regex, exper);
        //二十
        regex = NumRegex.getNumRegex() + encodeUnicode("十");
        exper = "{0}*10";
        regexMap.put(regex, exper);
        //十一
        regex = encodeUnicode("十") + NumRegex.getNumRegex();
        exper = "1*10+{0}*1";
        regexMap.put(regex, exper);
        //十
        regex = encodeUnicode("十");
        exper = "1*10";
        regexMap.put(regex, exper);
        //一
        regex = NumRegex.getNumRegex();
        exper = "{0}*1";
        regexMap.put(regex, exper);
    }

    static class NumRegex {
        public static final Map<String, String> numMap = new HashMap<String, String>();

        static {
            numMap.put("一", "1");
            numMap.put("二", "2");
            numMap.put("三", "3");
            numMap.put("四", "4");
            numMap.put("五", "5");
            numMap.put("六", "6");
            numMap.put("七", "7");
            numMap.put("八", "8");
            numMap.put("九", "9");
        }

        private static String numRegex;

        public static String getNumRegex() {
            if (numRegex == null || numRegex.length() == 0) {
                numRegex = "([";
                for (String s : numMap.keySet()) {
                    numRegex += encodeUnicode(s);
                }
                numRegex += "])";
            }
            return numRegex;
        }

    }

    private static String assembleString(String input, String newNum){
        Matcher numMatcher = Pattern.compile("[零壹貳叁肆伍陸柒捌玖拾佰一二三四五六七八九十百]+").matcher(input);
        if (numMatcher.find()) {
            int numStart = numMatcher.start();
            String prefix = input.substring(0, numStart);
            String suffix = input.substring(numStart + numMatcher.group().length());
            StringBuilder result = new StringBuilder();
            result.append(prefix); // 添加数字前面的部分
            result.append(newNum); // 添加转换后的数字部分
            result.append(suffix); // 添加数字后面的部分
            return result.toString();
        } else {
            return newNum;
        }
    }

}
