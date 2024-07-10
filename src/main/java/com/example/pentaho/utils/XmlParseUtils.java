package com.example.pentaho.utils;

import net.sf.jasperreports.engine.json.expression.filter.FilterExpression;
import org.jfree.chart.block.Arrangement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlParseUtils {

    private final static Logger log = LoggerFactory.getLogger(XmlParseUtils.class);
    /*完全禁用*/
    private final static String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
    /*禁用外部dtd*/
    private final static String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    /*禁止加载外部實體*/
    private final static String EXTERNAL_GENERAL_ENITIES = "http://xml.org/sax/features/external-general-entities";
    /*禁止加载参数實體*/
    private final static String EXTERNAL_PARAMETER_ENITIES = "http://xml.org/sax/features/external-parameter-entities";
    /*禁止加载HTTP實體*/
    private final static String SECURE_PROCESSING = "http://javax.xml.XMLConstants/feature/secure-processing";


    public DocumentBuilderFactory newDocumentBuilderFactory() throws ParserConfigurationException {
        HashMap<String, Boolean> features = new HashMap<>() {{
            put(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            put(DISALLOW_DOCTYPE_DECL, true);
            put(LOAD_EXTERNAL_DTD, false);
            put(EXTERNAL_PARAMETER_ENITIES, false);
            put(EXTERNAL_GENERAL_ENITIES, false);
        }};

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        features.keySet().forEach(featureName->{
            try {
                factory.setFeature(featureName,features.get(featureName));
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
        });


        /***/
        return factory;
    }

    public final static Element parser(String eleStr) throws ParserConfigurationException, IOException, SAXException {
        log.info("parser:{}",eleStr);
        HashMap<String, Boolean> features = new HashMap<>() {{
            put(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            put(DISALLOW_DOCTYPE_DECL, true);
            put(LOAD_EXTERNAL_DTD, false);
            put(EXTERNAL_PARAMETER_ENITIES, false);
            put(EXTERNAL_GENERAL_ENITIES, false);
        }};

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        features.keySet().forEach(featureName->{
            try {
                factory.setFeature(featureName,features.get(featureName));
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
        });
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(eleStr.getBytes()));
            Element element = document.getDocumentElement();
            return element;
    }


    public final static Map<String,String> getAttributes(String eleStr,Map<String,String> keys) throws ParserConfigurationException, IOException, SAXException {
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
