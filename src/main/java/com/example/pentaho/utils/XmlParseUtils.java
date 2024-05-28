package com.example.pentaho.utils;

import net.sf.jasperreports.engine.json.expression.filter.FilterExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.Map;

public class XmlParseUtils {
    
    private final static Logger log = LoggerFactory.getLogger(XmlParseUtils.class);
    /*完全禁用*/
    private final static String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
    /*禁用外部dtd*/
    private final static String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    /*禁止加载外部实体*/
    private final static String EXTERNAL_GENERAL_ENITIES = "http://xml.org/sax/features/external-general-entities";
    /*禁止加载参数实体*/
    private final static String EXTERNAL_PARAMETER_ENITIES = "http://xml.org/sax/features/external-parameter-entities";
    /*禁止加载HTTP参数*/
    private final static String SECURE_PROCESSING = "http://javax.xml.XMLConstants/feature/secure-processing";

    private DocumentBuilder builder = newDocumentBuilderFactory();


    public DocumentBuilder newDocumentBuilderFactory(){
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            /***/
            String FEATURE = null;
            FEATURE = EXTERNAL_PARAMETER_ENITIES;
            factory.setFeature(FEATURE, factory.isValidating());
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder;
        }catch (Exception e){
            log.info("e:{}",e);
        }
        return null;
    }

    public Element parser(String eleStr){
        log.info("parser:{}",eleStr);
        try {
//            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(eleStr.getBytes()));
            Element element = document.getDocumentElement();
            return element;
        }catch (Exception e){
            log.info("e:{}",e.toString());
            return null;
        }
    }


    public Map<String,String> getAttributes(String eleStr, Map<String, String> keys){
        Element element = parser(eleStr);
        if(element == null){
            return null;
        }

        NodeList nodeList = element.getChildNodes();

        for(String key:keys.keySet()){
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElement = (Element) node;
                    if(childElement.getTagName().equals(key)){
                        keys.put(key,childElement.getTextContent());
                    }
                }
            }
        }
        return keys;
    }


    public Map<String,String> parser(InputStream inputStream, Map<String, String> result){
        try {
            StringBuilder content = new StringBuilder();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = reader.readLine())!=null){
                content.append(line);
            }

            if(StringUtils.isNullOrEmpty(content.toString())){
                return result;
            }

            getAttributes(content.toString(),result);
        }catch (Exception e){
            log.info("e:{}",e.toString());
            log.info("result:{}",result);
        }
        return result;
    }
}
