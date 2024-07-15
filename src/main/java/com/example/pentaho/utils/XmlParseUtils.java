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
    private final static String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
    /*禁用外部dtd*/
    private final static String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    /*禁止加載外部實體*/
    private final static String EXTERNAL_GENERAL_ENITIES = "http://xml.org/sax/features/external-general-entities";
    /*禁止加載參數實體*/
    private final static String EXTERNAL_PARAMETER_ENITIES = "http://xml.org/sax/features/external-parameter-entities";
    /*禁止加載HTTP實體*/
    private final static String SECURE_PROCESSING = "http://javax.xml.XMLConstants/feature/secure-processing";


    /**
     *
     * @return
     * @throws ParserConfigurationException
     */
    public final static DocumentBuilderFactory GenerateDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature(DISALLOW_DOCTYPE_DECL, true);
        factory.setFeature(LOAD_EXTERNAL_DTD, false);
        factory.setFeature(EXTERNAL_PARAMETER_ENITIES, false);
        factory.setFeature(EXTERNAL_GENERAL_ENITIES, false);
        /***/
        return factory;
    }


    /**
     * 解析ResponseXml
     * @param inputStream
     * @param result -> 放解析後元素
     * @return
     */
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
        }
        log.info("result:{}",result);
        return result;
    }


    /**
     * xmlElementStr -> 轉 Element 物件 -> 放入result
     * @param xmlContent -> xmlElementStr
     * @param result keySet -> pentaho可能回傳的內容，參考官方文件
     * @return
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public final static Map<String,String> getAttributes(String xmlContent,Map<String,String> result) throws ParserConfigurationException, IOException, SAXException {
        Element element = parser(xmlContent);
        if(element == null){
            return result;
        }

        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                result.put(childElement.getTagName(),childElement.getTextContent());
            }
        }
        return result;
    }

    /**
     * xmlContent -> Element物件
     * @param xmlContent
     * @return
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public final static Element parser(String xmlContent) throws ParserConfigurationException, IOException, SAXException {
        log.info("要轉換成Element物件的xmlContent:{}",xmlContent);
        DocumentBuilderFactory factory = GenerateDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));
        Element element = document.getDocumentElement();
        return element;
    }
}
