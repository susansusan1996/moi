package com.example.pentaho.service;

import com.example.pentaho.component.Directory;
import com.example.pentaho.component.IbdTbAddrCodeOfDataStandardDTO;
import com.example.pentaho.component.KeyComponent;
import com.example.pentaho.component.OpenPageDTO;
import com.example.pentaho.utils.QRCodeUtils;
import com.example.pentaho.utils.RSAJWTUtils;
import com.example.pentaho.utils.RsaUtils;
import com.example.pentaho.utils.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.zxing.WriterException;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class QrcodeService {

    private final static Logger log = LoggerFactory.getLogger(QrcodeService.class);

    private final int VALID_TIME = 5256000;

    @Autowired
    private Environment env;


    @Autowired
    private KeyComponent keyComponent;

    @Autowired
    private Directory directory;


    @Autowired
    private RSAJWTUtils rsajwtUtils;

    /***
     * 限制 5筆內 組成 url，生成1張圖
     * @param datas
     * @param inputAddress
     * @return
     */
    private String generateURL(List<IbdTbAddrCodeOfDataStandardDTO> datas, String inputAddress){
        StringBuilder full =  new StringBuilder();
        StringBuilder id =  new StringBuilder();
        StringBuilder xy =  new StringBuilder();
        StringBuilder js =  new StringBuilder();
        StringBuilder url = new StringBuilder(directory.getQrcodeUrl());

        if(datas.isEmpty()){
            //查無資料
            url.append("os=").append(inputAddress);
            return url.toString();
        }



        if(datas.size()==1){

            full.append(datas.get(0).getFullAddress());
            id.append(datas.get(0).getAddressId());
            String formattedWgsX = String.format("%.5f", datas.get(0).getWgsX());
            String formattedWgsY = String.format("%.5f", datas.get(0).getWgsY());
            xy.append(formattedWgsX+":"+formattedWgsY);
            js.append(datas.get(0).getJoinStep());

            url.append("os=").append(inputAddress).append("&");
            url.append("full=").append(full).append("&");
            url.append("id=").append(id).append("&");
            url.append("xy=").append(xy).append("&");
            url.append("js=").append(js);
            return url.toString();
        }

        //todo:先限制5筆
        int max = 5;
        if(datas.size()<5){ //2,3,4
            max = datas.size();
        }

        for(int i=0;i<max;i++){
            String formattedWgsX = String.format("%.5f", datas.get(i).getWgsX());
            String formattedWgsY = String.format("%.5f", datas.get(i).getWgsY());
            String xyStr = formattedWgsX +":"+ formattedWgsY;
            if(i==(max-1)){
                full.append(datas.get(i).getFullAddress());
                id.append(datas.get(i).getAddressId());
                xy.append(xyStr);
                js.append(datas.get(i).getJoinStep());
            }else{
                full.append(datas.get(i).getFullAddress()).append(",");
                id.append(datas.get(i).getAddressId()).append(",");
                xy.append(xyStr).append(",");
                js.append(datas.get(i).getJoinStep()).append(",");
            }
        }

        url.append("os=").append(inputAddress).append("&");
        url.append("full=").append(full).append("&");
        url.append("id=").append(id).append("&");
        url.append("xy=").append(xy).append("&");
        url.append("js=").append(js);
        log.info("url:{}",url);
        return url.toString();
    }

    /***
     * 加密單個seq & 原始地址組成 url,1筆1張圖
     * @param datas
     * @param inputAddress
     * @return
     * @throws Exception
     */
    public Map<String,String> generateUrlsByToken(List<IbdTbAddrCodeOfDataStandardDTO> datas, String inputAddress) throws Exception {

        if(datas.isEmpty()){
            //todo:查無資料要生產圖嗎
            return null ;
        }
        /**取得資拓私鑰加密**/
        PrivateKey privateKey = null;
        try {
            privateKey = RsaUtils.getPrivateKey((keyComponent.getApPrikeyName()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Map<String,String>  urls = new HashMap<String,String>();
        PrivateKey finalPrivateKey = privateKey;
        datas.forEach(data->{
            /**把data 中的 seq 與 originalAddress,joinStep 放入token 的payload**/
            /**payload 放入 addressInfo : { "originalAddress": "新北市..." ,"seq":"1","joinStep":"JA111"}**/
            OpenPageDTO.QrcodeDTO qrCodeDTO = new OpenPageDTO.QrcodeDTO();
            qrCodeDTO.setSeq(String.valueOf(data.getSeq()));
            qrCodeDTO.setOriginalAddress(inputAddress);
            qrCodeDTO.setJoinStep(data.getJoinStep());
            Map<String, Object> tokenMap = null;
            try {
                tokenMap = rsajwtUtils.generateTokenBySeqExpireInMinutes(qrCodeDTO, finalPrivateKey, VALID_TIME);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            log.info("tokenMap:{}",tokenMap);
            /**與baseUrl組成queryString*/
            //todo:http://localhost:8080/iisi/single-query-token?
            StringBuilder baseUrl = new StringBuilder(directory.getQrcodeUrl());
            if(StringUtils.isNotNullOrEmpty(String.valueOf(tokenMap.get("token")))){
                baseUrl.append("task=").append(tokenMap.get("token"));
            }
            log.info("tokenUrl:{}",baseUrl);
            urls.put(String.valueOf(data.getSeq()),baseUrl.toString());
        });
        return urls;
    }

    /***
     * 不加密seq,originalAddress,joinStep
     * @param params
     */
    public void generateURLs(Map<String,String> params){
        /**不加密**/
        //todo:http://localhost:8080/iisi/single-query-taskId?
        String[] activeProfiles = env.getActiveProfiles();
        String baseUrl = "http://localhost:8080/iisi";
        for (String activeProfile : activeProfiles) {
            if("prod".equals(activeProfile)){
                baseUrl="http://34.211.215.66:8080/iisi";
            }

            if("uat".equals(activeProfile)){
                baseUrl="http://10.32.32.207:8080/iisi";
            }

        }

        StringBuilder queryString = new StringBuilder(baseUrl+"/single-query-taskId?");
        AtomicInteger size = new AtomicInteger(params.size());
        params.keySet().forEach(key ->{
            if(size.get() == 0){
               return;
            }
            queryString.append(key).append("=").append(params.get(key)).append("&");
            size.getAndDecrement();
        });

        log.info("queryString:{}",queryString);
        try {
            String seq = params.get("taskId");
            String absolute = directory.getQrcodePath() +seq+".jpg";
            QRCodeUtils.generateQrcode(queryString.toString(),directory.getLogoPath(),absolute);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (WriterException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 1筆資料、1個url、1張圖
     * @param datas
     * @param inputAddress
     * @return
     */
    public Map<String,String> generateURLBySeqs(List<IbdTbAddrCodeOfDataStandardDTO> datas, String inputAddress){

        if(datas.isEmpty()){
            //todo:查無資料要生產圖嗎
            return null ;
        }

        Map<String,String>  seqMap = new HashMap<>();
        log.info("總共要產生:{}張圖", datas.size());
        datas.forEach(data->{
            StringBuilder full =  new StringBuilder();
            StringBuilder id =  new StringBuilder();
            StringBuilder xy =  new StringBuilder();
            StringBuilder js =  new StringBuilder();
            StringBuilder url = new StringBuilder(directory.getQrcodeUrl());
            full.append(datas.get(0).getFullAddress());
            id.append(datas.get(0).getAddressId());
            String formattedWgsX = String.format("%.5f", datas.get(0).getWgsX());
            String formattedWgsY = String.format("%.5f", datas.get(0).getWgsY());
            xy.append(formattedWgsX+":"+formattedWgsY);
            js.append(datas.get(0).getJoinStep());

            url.append("os=").append(inputAddress).append("&");
            url.append("full=").append(full).append("&");
            url.append("id=").append(id).append("&");
            url.append("xy=").append(xy).append("&");
            url.append("js=").append(js);
            seqMap.put(String.valueOf(data.getSeq()),url.toString());
        });

        return seqMap;

    }


    /**
     * 生成多張QRCODE
     * @param urls
     */
    public void generateQrcodeByUrls(Map<String, String> urls){
        Set<String> seqs = urls.keySet();
        seqs.forEach(seq->{
            String absolute = directory.getQrcodePath() +seq+".jpg";
            String url = urls.get(seq);
            try {
                QRCodeUtils.generateQrcode(url,directory.getLogoPath(),absolute);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (WriterException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
