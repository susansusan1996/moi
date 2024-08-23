package com.example.pentaho.resource;

import com.example.pentaho.component.*;
import com.example.pentaho.service.QrcodeService;
import com.example.pentaho.service.SingleQueryService;
import com.example.pentaho.service.SingleTrackQueryService;
import com.example.pentaho.utils.*;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.example.pentaho.service.SystemUpdateService;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.*;

@RestController
@RequestMapping("/api/singlequery")
@SecurityRequirement(name = "Authorization")
public class SingleQueryResouce {

    private static Logger log = LoggerFactory.getLogger(SingleQueryResouce.class);

    @Autowired
    private SingleQueryService singleQueryService;


    @Autowired
    private QrcodeService qrcodeService;


    @Autowired
    private SingleTrackQueryService singleQueryTrackService;


    @Autowired
    private AddressParser addressParser;


    @Autowired
    private SystemUpdateService systemUpdateService;

    @Autowired
    private Directory directory;

    @Autowired
    private ResourceUtils resourceUtils;


    @Autowired
    private RSAJWTUtils rsajwtUtils;


    @Autowired
    private KeyComponent keyComponent;




    @Operation(description = "單筆查詢",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER,
                            name = "Authorization",
                            description = "驗證jwt token,body附帶userInfo={\"Id\":1,\"departName\":\"A05\"} ,departName需為代號",
                            required = true,
                            schema = @Schema(type = "string"))}
    )
    @Authorized(keyName = "SHENG")
    @PostMapping("/query-single")
    public ResponseEntity<SingleQueryResultDTO> queryAddress(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "單筆查詢，request body 要帶 json，需包含:originalAddress、county(可為空)、town(可為空)。具體資料格式如下:",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SingleQueryDTO.class),
                            examples = @ExampleObject(value = "{\"originalAddress\":\"台南市東區衛國里007鄰衛國街１１４巷９弄１０號B六樓之５\",\"county\":\"臺南市(可為空)\",\"town\":\"東區(可為空)\"}")
                    )
            )
            @RequestBody SingleQueryDTO singleQueryDTO
    ) {
        try {
            SingleQueryResultDTO result = singleQueryService.findJson(singleQueryDTO);
            log.info("result.getText():{}",result.getText());
            result.getData().forEach(data->{
                try {
                    data.setJoinStep(resourceUtils.getJoinStepDes(data.getJoinStep()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            //todo:有查到
            if("查詢結果".equals(result.getText())){
                //todo:產生Qrcode都要成async方法，先讓單筆查詢結果回去
                //todo:改成1筆資料1張圖 url上 hardcore 資料
//                Map<String, String> urls = qrcodeService.generateURLBySeqs(result.getData(), singleQueryDTO.getOriginalAddress());
//                qrcodeService.generateQrcodeByUrls(urls);

                //todo:改成1筆資料1張圖 url上 加密seqs
//                Map<String, String> tokenUrls = qrcodeService.generateUrlsByToken(result.getData(), singleQueryDTO.getOriginalAddress());
//                qrcodeService.generateQrcodeByUrls(tokenUrls);

                HashMap<String, String> param = new HashMap<>(){{
                    put("origrinalAddress",singleQueryDTO.getOriginalAddress());
                }};

                //todo:改成1筆資料1張圖 url上 不加密seq,originalAddress,joinStep
                result.getData().forEach(data->{
                    param.put("taskId",String.valueOf(data.getSeq()));
                    param.put("joinStep",String.valueOf(data.getSeq()));
                    qrcodeService.generateURLs(param);
                });

                //todo:舊的 一次查詢只做一張(限制五筆)
//                String url = generateURL(result.getData(),singleQueryDTO.getOriginalAddress());
//                log.info("url:{}",url);
//                String filename = UserContextUtils.getUserHolder().getId();
//                String absolute = directory.getQrcodePath() +filename+".jpg";
//                QRCodeUtils.generateQrcode(url,directory.getLogoPath(),absolute);
            }


            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.info("e:{}",e.toString());
            log.info("無法解析地址:{}", e.getMessage());
            SingleQueryResultDTO dto = new SingleQueryResultDTO();
            dto.setText("無法解析地址");
            return ResponseEntity.ok(dto);
        }
    }

    /***
     * 單筆查詢軌跡
     */
    @Operation(
            description = "單筆軌跡",
            parameters = {@Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "jwt token,body附帶 userInfo={\"Id\":1,\"orgId\":\"Admin\"}",
                    schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "",
                            content = @Content(schema = @Schema(implementation = SingleQueryTrackDTO.class)))})
    @PostMapping("/query-track")
    @Authorized(keyName = "SHENG")
    public ResponseEntity<List<SingleQueryTrackDTO>> queryTrack(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "編碼",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "BSZ7538-0")
                    )
            )
            @RequestBody String addressId) {
        try {
            if (addressId.indexOf("\"") >= 0) {
                addressId = addressId.replaceAll("\"", "").trim();
            }
            return new ResponseEntity<>(singleQueryTrackService.querySingleTrack(addressId), HttpStatus.OK);
        }catch (Exception e){
            log.info("e:{}",e.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }finally {
            User user = UserContextUtils.getUserHolder();
            systemUpdateService.singleQuerySystemUpdate(user.getId(),"CHANGE");
        }
    }


    /**
     * 單筆未登入測試
     */
    @Operation(description = "單筆未登入測試")
    @GetMapping("/query-single-forguest")
    @UnAuthorized
    @RateLimiting(name="forguest",tokens = 0.3333)
    public ResponseEntity<SingleQueryResultDTO> forGuestUser(
            @Parameter(
                    description ="輸入地址" ,
                    required = true,
                    schema = @Schema(type = "string", example = "台南市東區衛國里007鄰衛國街１１４巷９弄１０號之五3樓"))
            @RequestParam("inputAddress") String inputAddress,
            @Parameter(
                    description ="選擇縣市" ,
                    schema = @Schema(type = "string", example = "台南市(可為空)"))
            @Nullable
            @RequestParam("county") String county,
            @Parameter(
                    description ="選擇鄉鎮市區" ,
                    schema = @Schema(type = "string", example = "東區(可為空)"))
            @Nullable
            @RequestParam("town") String town
    ) {

        SingleQueryDTO singleQueryDTO = new SingleQueryDTO(inputAddress, county, town);
        log.info("SingleQueryDTO:{}",singleQueryDTO);

        try{
            SingleQueryResultDTO result = singleQueryService.findJson(singleQueryDTO);
                List<IbdTbAddrCodeOfDataStandardDTO> datas = result.getData();
                datas.forEach(data->{
                    try {
                        data.setJoinStep(resourceUtils.getJoinStepDes(data.getJoinStep()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    data.setAddressId(null);
                });
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            log.info("e:{}",e.toString());
            log.info("無法解析地址:{}", e.getMessage());
            SingleQueryResultDTO dto = new SingleQueryResultDTO();
            dto.setText("無法解析地址");
            return ResponseEntity.ok(dto);
        }
    }

    /***
     * QRcode 掃描獲取data接口
     * @return
     */
    @GetMapping("/qrcode-data-token")
    @Authorized(keyName = "AP")
    public ResponseEntity<List<OpenPageDTO>> findbBySeq(){
      return new ResponseEntity<>(singleQueryService.findbBySeq(),HttpStatus.OK);
    }


    /***
     * QRcode 掃描獲取data接口
     * @return
     */
    @PostMapping("/qrcode-data")
    @RateLimiting(name="qrcode-data",tokens = 0.3333)
    @UnAuthorized
    public ResponseEntity<List<OpenPageDTO>> findbBySeq(@RequestBody Map<String,String> param){
        log.info("parans:{}",param);
        String[] keys  = new String[]{"taskId","origrinalAddress","joinStep"};
        for(String key : keys){
            if(StringUtils.isNullOrEmpty(param.get(key))){
                throw  new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(singleQueryService.findbBySeq(param),HttpStatus.OK);
    }



    @GetMapping("/test")
    @Hidden
    public void checkSum(@RequestParam String addressId) throws Exception{
        boolean isValidate = singleQueryTrackService.checkSum(addressId);
        log.info("isValidate:{}",isValidate);
    }

}
