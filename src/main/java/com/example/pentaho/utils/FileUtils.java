package com.example.pentaho.utils;

import com.example.pentaho.exception.MoiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);


    public static boolean isValidDirectory(Path directoryPath) {
        return Files.isDirectory(directoryPath) && Files.isWritable(directoryPath);
    }


    /**
     * @param pathPrefix
     * @param multiFile
     * @throws IOException
     */
    public static void saveFile(String pathPrefix, MultipartFile multiFile) throws IOException {
        log.info("檔案路徑為:{}", pathPrefix);
        File directory = new File(pathPrefix);
        if (!directory.exists()) {
            log.info("目錄不存在，另外創建目錄");
            directory.mkdirs(); // 創建路徑
        }
        Path directoryPath = Paths.get(pathPrefix);
        if (isValidDirectory(directoryPath)) {
            InputStream inputStream = null;
            if (multiFile != null) {
                inputStream = multiFile.getInputStream();
            }
            byte[] inputByteArr = FileCopyUtils.copyToByteArray(inputStream);
            String filePath = pathPrefix + cleanString(multiFile.getResource().getFilename()); // 指定文件路徑
            try {
                saveByteArrayToFile(inputByteArr, filePath);
                log.info("檔案儲存成功,{}", filePath);
            } catch (IOException e) {
                throw new MoiException("檔案儲存失敗 {}: " + e.getMessage());
            }
            inputStream.close();
        }
    }


    public static String cleanString(String aString) {
        if (aString == null) return null;
        String cleanString = "";
        for (int i = 0; i < aString.length(); ++i) {
            cleanString += cleanChar(aString.charAt(i));
        }
        return cleanString;
    }


    private static char cleanChar(char ch) {

        // 0 - 9
        for (int i = 48; i < 58; ++i) {
            if (ch == i) return (char) i;
        }

        // 'A' - 'Z'
        for (int i = 65; i < 91; ++i) {
            if (ch == i) return (char) i;
        }

        // 'a' - 'z'
        for (int i = 97; i < 123; ++i) {
            if (ch == i) return (char) i;
        }

        // other valid characters
        switch (ch) {
            case '/':
                return '/';
            case '.':
                return '.';
            case '-':
                return '-';
            case '_':
                return '_';
            case ',':
                return ',';
            case ' ':
                return ' ';
            case '!':
                return '!';
            case '@':
                return '@';
            case '#':
                return '#';
            case '$':
                return '$';
            case '%':
                return '%';
            case '^':
                return '^';
            case '&':
                return '&';
            case '*':
                return '*';
            case '(':
                return '(';
            case ')':
                return ')';
            case '+':
                return '+';
            case '=':
                return '=';
            case ':':
                return ':';
            case ';':
                return ';';
            case '?':
                return '?';
            case '"':
                return '"';
            case '<':
                return '<';
            case '>':
                return '>';
            case '`':
                return '`';
        }
//        if (isChineseChar(ch))
//            return ch;
        return '%';
    }

    // 判斷中文字
    private static boolean isChineseChar(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
            || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;
    }


    private static void saveByteArrayToFile(byte[] byteArray, String filePath) throws IOException {
        File file = new File(filePath);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            FileCopyUtils.copy(byteArray, fos);
        }
    }

}
