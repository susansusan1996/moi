package com.example.pentaho.service;

import com.example.pentaho.component.JobParams;
import com.example.pentaho.component.PentahoComponent;
import com.example.pentaho.component.PentahoWebService;
import com.example.pentaho.utils.WebServiceUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Service
public class BatchService {


    @Autowired
    private PentahoComponent pentahoComponent;


    @Autowired
    private WebServiceUtils webServiceUtils;

    private Gson gson = new Gson();



    private final static String sperator ="&";

    private final static Logger log = LoggerFactory.getLogger(BatchService.class);

    public Integer excuteTrans(JobParams jobParams) throws IOException {
        /**實測:user&pass不需要，需要的是basicAuthentication**/
//      ex:http://52.33.116.195:8081/pentaho/kettle/executeTrans/?rep=PetahoRepository&trans=/home/addr/dev/PI_TEST.ktr&level=Debug

        /**
         * 使用檔案在Pentaho BI server 上的 '絕對路徑'
         * 如果有指定repositpry(rep=kettle-itbigbird),才可以使用'相對路徑'
         **/
        String webService = PentahoWebService.executeTrans;
        String fileAbsolutePath = "/home/addr/dev/MAIN.ktr";
        /**這裡寫一個可以把jobParams寫成url的方法**/
        String url = pentahoComponent.getWebTarget() +
                "/kettle/executeTrans/?" +
                "rep=PentahoRepository"+sperator +
                "trans=" + fileAbsolutePath + sperator +
                "level=Debug";

        log.info("request url:{}", url);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        /**很重要**/
        basicAuthentication(con);

        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();


        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        in.close();

        log.info("Response Code:{}", responseCode);
        log.info("Response Content:{}", response.toString());
        return responseCode;
    }


    /***
     *  添加 Basic Authentication header
     *  改讀取路徑下的檔案
     * @param connection
     */
    private void basicAuthentication(HttpURLConnection connection) throws IOException {
        Path path = Path.of(pentahoComponent.getEncodeAuth());
        String encode = Files.readString(path, StandardCharsets.UTF_8);
        connection.setRequestProperty("Authorization",encode);
    }
}
