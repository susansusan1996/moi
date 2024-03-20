package com.example.pentaho.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class NumberParser {
    private static final Logger log = LoggerFactory.getLogger(NumberParser.class);

    public static String replaceWithHalfWidthNumber(String input) {
        if (input != null && !input.isEmpty()) {
            Map<String, String> map = new HashMap<>();
            map.put("壹", "1");
            map.put("貳", "2");
            map.put("叁", "3");
            map.put("肆", "4");
            map.put("伍", "5");
            map.put("陸", "6");
            map.put("柒", "7");
            map.put("捌", "8");
            map.put("玖", "9");
            map.put("零", "0");
            map.put("一", "1");
            map.put("二", "2");
            map.put("三", "3");
            map.put("四", "4");
            map.put("五", "5");
            map.put("六", "6");
            map.put("七", "7");
            map.put("八", "8");
            map.put("九", "9");
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
            String result =  input.substring(0, input.length() - 1) + "樓";
            return result.replace("-","之");
        }else{
            return input.replace("-","之");
        }
    }
}
