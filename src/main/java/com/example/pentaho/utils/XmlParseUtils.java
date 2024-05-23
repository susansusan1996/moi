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


    public DocumentBuilderFactory newDocumentBuilderFactory(){
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        /***/
        factory.setFeature(VALID,factory.isValidating());
    }

    public final static Element parser(String eleStr){
        log.info("parser:{}",eleStr);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(eleStr.getBytes()));
            Element element = document.getDocumentElement();
            return element;
        }catch (Exception e){
            log.info("e:{}",e.toString());
            return null;
        }
    }


    public final static Map<String,String> getAttributes(String eleStr,Map<String,String> keys){
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


    public final static Map<String,String> parser(InputStream inputStream,Map<String,String> result){
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
