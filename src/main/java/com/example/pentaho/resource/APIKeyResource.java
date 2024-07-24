package com.example.pentaho.resource;

import com.example.pentaho.component.*;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.service.ApiKeyService;
import com.example.pentaho.service.RedisService;
import com.example.pentaho.service.RefreshTokenService;
import com.example.pentaho.service.SingleQueryService;
import com.example.pentaho.service.SingleTrackQueryService;
import com.example.pentaho.utils.*;
import com.google.common.util.concurrent.RateLimiter;
import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.PrivateKey;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/***
 * 產生APIKey & 使用APIKey驗證的API
 */
@RestController
@RequestMapping("/api/api-key")
@SecurityRequirement(name = "Authorization")
public class APIKeyResource {
    private static Logger log = LoggerFactory.getLogger(APIKeyResource.class);

    /**
     * API效期先設定1天
     **/
    private final int VALID_TIME = 1440;


    private KeyComponent keyComponent;


    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private SingleTrackQueryService singleTrackQueryService;

    @Autowired
    private SingleQueryService singleQueryService;


    @Autowired
    private AddressParser  addressParser;



    private RateLimiter rateLimiter = RateLimiter.create(1);


    public APIKeyResource(KeyComponent keyComponent) {
        this.keyComponent = keyComponent;
    }

    @Operation(description = "獲取APIKEY",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER,
                            name = "Authorization",
                            description = "聖森私鑰加密 jwt token,body附帶userInfo={\"Id\":\"673f7eec-8ae5-4e79-ad3a-42029eedf742\",\"orgId\":\"ADMIN\"}",
                            schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.QUERY,
                            name = "userId",
                            description = "query字串要帶被審核通過的該userId ex. ?userId=673f7eec-8ae5-4e79-ad3a-42029eedf742",
                            required = true,
                            schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.QUERY,
                            name = "username",
                            description = "query字串要帶被審核通過的該userId ex. ?username=username12345",
                            required = true,
                            schema = @Schema(type = "string"))}
            ,
            responses = {
                    @ApiResponse(responseCode = "200", description = "資拓私鑰加密JWT Token 時效為1天，refresh_token時效為2天",
                            content = @Content(schema = @Schema(implementation = JwtReponse.class)))}
    )
    @GetMapping("/get-api-key")
    @Authorized(keyName = "SHENG")
    public ResponseEntity<JwtReponse> getAPIKey(@RequestParam("userId") String userId,@RequestParam("username") String username) throws Exception {
        return new ResponseEntity<>(apiKeyService.getApiKey(userId,username), HttpStatus.OK);
    }


    @Operation(description = "產生APIKEY",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER,
                            name = "Authorization",
                            description = "聖森私鑰加密 jwt token,body附帶userInfo={\"Id\":\"673f7eec-8ae5-4e79-ad3a-42029eedf742\",\"orgId\":\"ADMIN\"}",
                            schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.QUERY,
                            name = "userId",
                            description = "query字串要帶被審核通過的該userId ex. ?userId=673f7eec-8ae5-4e79-ad3a-42029eedf742",
                            required = true,
                            schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.QUERY,
                            name = "username",
                            description = "query字串要帶被審核通過的該userId ex. ?username=username12345",
                            required = true,
                            schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.QUERY,
                            name = "reviewResult",
                            description = "query字串帶審核結果(AGREE、REJECT) ex. reviewResult=AGREE，或是 reviewResult=REJECT",
                            required = true,
                            schema = @Schema(type = "string"))}
            ,
            responses = {
                    @ApiResponse(responseCode = "200", description = "資拓私鑰加密JWT Token 時效為1天，refresh_token時效為2天",
                            content = @Content(schema = @Schema(implementation = JwtReponse.class)))}
    )
    @PostMapping("/create-api-key")
    @Authorized(keyName = "SHENG")
    public ResponseEntity<JwtReponse> createApiKey(@RequestParam("userId")String userId,@RequestParam("username")String username, @RequestParam("reviewResult") String reviewResult) throws ParseException {
        /*回應內容*/
        JwtReponse response = new JwtReponse();
        if (reviewResult.equals("REJECT") || reviewResult.equals("AGREE")){
            /**拒絕->redis找有沒有存在，把token,refreshToken清掉，重新存入result*/
            if ("REJECT".equals(reviewResult)) {
                refreshTokenService.saveRefreshToken(userId,username,null, null, reviewResult);
                response.setErrorResponse("已儲存被拒絕申請的使用者資訊");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            try {
                /**同意 用userid找redis有沒有存在
                 * 存在:直接返回查找內容
                 * 不存在:產生token,refreshtokeen 存入redis
                 * */
                return new ResponseEntity<>(apiKeyService.createApiKey(userId,username ,null, "fromApi"), HttpStatus.OK);
            } catch (MoiException e) {
                response.setErrorResponse(String.valueOf(e));
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            } catch (Exception e) {
                log.info("e:{}", e.toString());
                throw new MoiException("generate error");
            }
        }else{
            /**不會有REJECT | AGREE 以外的結果**/
            response.setErrorResponse("參數錯誤");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }
    }

    /**
     * 單筆APIKEY測試
     */
    @Operation(description = "單筆APIKEY測試",
            parameters = {@Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "資拓私鑰加密的jwt token",
                    schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "userId",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "500", description = ""),
            })
    @PostMapping("/forapikey")
    @Authorized(keyName = "AP")
    public ResponseEntity<String> forAPIKeyUser() {
        User user = UserContextUtils.getUserHolder();
        log.info("user:{}", user);
        return new ResponseEntity<>(user.getId(), HttpStatus.OK);
    }



    @PostMapping("/refreshToken")
    @UnAuthorized
    @Hidden
    public ResponseEntity<JwtReponse> refreshToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用refreshToken取得新的token",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RefreshToken.class),
                            examples = @ExampleObject(value = "{\"refreshToken\": \"eyJhbGciOiJSUzI1NiJ9.eyJ1c2VySW5mbyI6IntcInRva2VuXCI6XCJCZWFyZXJcIixcInJlZnJlc2hUb2tlblwiOlwiQmVhcmVyXCIsXCJyZW1vdGVBZGRyXCI6XCIxOTIuMTY4LjMxLjE2N1wiLFwieHJlYWxJcFwiOlwiMTExLjgyLjI0MS41OFwiLFwiaGF2ZVRvQ2hhbmdlUHdkXCI6ZmFsc2UsXCJsb2NhbGl6ZU5hbWVcIjpcIueuoeeQhuWToeWnk-WQjVwiLFwiZW1haWxcIjpcImFkbWluQGdtYWlsLmNvbVwiLFwicm9sZXNcIjpbXCJST0xFX0FETUlOXCIsXCJST0xFX0lVU0VSXCIsXCJST0xFX01PREVSQVRPUlwiXSxcImlkXCI6XCI2NzNmN2VlYy04YWU1LTRlNzktYWQzYS00MjAyOWVlZGY3NDJcIixcInVzZXJuYW1lXCI6XCJhZG1pblwiLFwib3JnSWRcIjpcIkFETUlOXCIsXCJkZXBhcnROYW1lXCI6XCLnrqHnkIbmqZ_pl5xf5Zau5L2NVVBEQVRFXCIsXCJwYXNzd29yZFwiOm51bGx9IiwianRpIjoiWlRrM05UVTVZVE10TXpBMllTMDBPVFJtTFRsa1l6WXRNbVpsTXpSaE56UmtaakE1IiwiZXhwIjoxNzEyODAwNDMyfQ.a1egTZEfAVdBWkf9x1GMMXEV6ml41gQ2HoLFHAa7RR2eK7-4u--D92-cQLoF-644cgJyzhWdF_cggyg11IWo5ADRpZRAD1nYbLpt5Fl9aIRbTjA-Liey6rjsquoEcuGC4cqnCrHwdI_Ko_pu0DbevI4fi4lhLnBdvfFs0wTvaqPWhc935k_NWLwarGeV525S9k3soDJfBO1buU9VikFojalsIxQ5kuKMKsmZgEQAFb0eS4W_07HutvhdaJAmZNSIPOElSLa_mpuwRsqlho153lYsAvwjbhZuVMBHM3i72ZNSXNdz2olUXG6844s2YbQWTadOv3pXsN9oGw7j3ssugg\",\n" +
                                    "\"id\":\"673f7eec-8ae5-4e79-ad3a-42029eedf742\"}")
                    )
            )
            @RequestBody RefreshToken request) {
        /**/
        log.info("refreshToken:{}", request);
        RefreshToken refreshToken = refreshTokenService.findRefreshTokenByUserId(request.getId(),"");
        //檢查refresh_token跟id是一致的
        if (refreshToken != null &&  refreshToken.getRefreshToken().equals(request.getRefreshToken())) {
            try {
                if (refreshTokenService.verifyExpiration(request.getId(), refreshToken.getRefreshToken(), "refresh_token")) {
                    //refresh_token沒有過期
                    //卷新的token給他
                    return new ResponseEntity<>(apiKeyService.exchangeForNewToken(request.getId()), HttpStatus.OK);
                }
            } catch (ExpiredJwtException e) {
                return new ResponseEntity<>(new JwtReponse("refresh_token過期"), HttpStatus.FORBIDDEN);
            } catch (Exception e) {
                log.info("refresh_token失敗:{}", e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }


    @GetMapping("/refresh-api-key")
    @UnAuthorized
    public ResponseEntity<String> refreshToken(
            @RequestParam("refreshToken") String refreshToken) throws Exception {
        log.info("refreshToken:{}", refreshToken);
        JwtReponse jwtReponse = new JwtReponse();
        /*資拓公鑰解密*/
        String apPubkeyName = keyComponent.getApPubkeyName();
        /*result: isValid(可解密未過期)、ExpiredJwtException(可解密過期)、Exception(其他錯誤)*/
        String result = Token.isVaildRSAJWTToken(refreshToken, apPubkeyName);
        switch (result){
            case "isValid":
                /*有userId,userType*/
                User user = Token.extractUserFromRSAJWTToken(refreshToken, apPubkeyName);
                if(!"refresh_token".equals(user.getTokenType())){
                    jwtReponse.setErrorResponse("請使用刷新金鑰");
                    return new ResponseEntity<>("請使用刷新金鑰",HttpStatus.OK);
                }
                /*用userId找出refreshToken物件*/
                RefreshToken refreshTokenObj = refreshTokenService.findRefreshTokenByUserId(user.getId());
                /*檢查reviewResult*/
                /*應該是不會進到這裡，因為Reject後就只保留userId,username,reviewResult**/
                if("REJECT".equals(refreshTokenObj.getReviewResult())){
                    jwtReponse.setErrorResponse("申請遭拒，請重新申請");
                    return new ResponseEntity<>("申請遭拒，請重新申請",HttpStatus.OK);
                }

                /*表示AGREE 檢查APIKEY有沒有過期*/
                String vaildAPIKEY = Token.isVaildRSAJWTToken(refreshTokenObj.getToken(),apPubkeyName);
                if("isValid".equals(vaildAPIKEY)){
                    log.info("apikey未過期");
                    /*未過期直接返回即可*/
                    jwtReponse.setToken(refreshTokenObj.getToken());
                    return new ResponseEntity<>(refreshTokenObj.getToken(),HttpStatus.OK);
                }
                /*apikey過期 重新眷*/
                if("ExpiredJwtException".equals(vaildAPIKEY)){
                    log.info("apikey過期充新眷");
                    /*type換回token再放進playload**/
                    user.setTokenType("token");
                    PrivateKey privateKey = RsaUtils.getPrivateKey((keyComponent.getApPrikeyName()));
                    Map<String, Object> tokenMap = RSAJWTUtils.generateTokenExpireInMinutes(user, privateKey, VALID_TIME);
                    /**更新redis的apiKey*/
                    refreshTokenService.updateTokenByUserId(user.getId(),(String)tokenMap.get("token"),(String) tokenMap.get("expiryDate"));
                    jwtReponse.setToken((String)tokenMap.get("token"));
                    return new ResponseEntity<>((String)tokenMap.get("token"),HttpStatus.OK);
                }
                break;
            case "ExpiredJwtException" :
                jwtReponse.setErrorResponse("刷新金鑰已過期，請重新整理");
                return new ResponseEntity<>("刷新金鑰已過期，請重新整理",HttpStatus.OK);

            default:
                jwtReponse.setErrorResponse("發生錯誤，請聯絡管理員");
                return new ResponseEntity<>("發生錯誤，請聯絡管理員",HttpStatus.OK);
        }
        jwtReponse.setErrorResponse("發生錯誤，請聯絡管理員");
        return new ResponseEntity<>("發生錯誤，請聯絡管理員",HttpStatus.OK);
    }


    /***
     * 取得指定【地址識別碼】之異動軌跡
     */
    @Operation(description = "取得指定【地址識別碼】之異動軌跡",
            parameters = {@Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "資拓私鑰加密的jwt token",
                    schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "",
                            content = @Content(schema = @Schema(implementation = SingleQueryTrackDTO.class))),
                  })
    @GetMapping("/query-track")
    @Authorized(keyName = "AP")
    public ResponseEntity<List<SingleQueryTrackDTO>> queryTrack(
            @Parameter(
                    description = "編碼",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "BSZ7538-0")
                    )
            ) @RequestParam String addressId) {
        UsageUtils.writeUsageLog("/api/api-key/query-track",addressId);
        try {
            if (addressId.indexOf("\"") >= 0) {
                addressId = addressId.replaceAll("\"", "").trim();
            }
            return new ResponseEntity<>(singleTrackQueryService.querySingleTrack(addressId), HttpStatus.OK);
        }catch (Exception e){
            log.info("e:{}",e.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /***
     * OpenAPI
     * 指定【地址】之標準格式地址
     */
    @Operation(description = "指定【地址】之標準格式地址",
            parameters = {@Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "資拓私鑰加密的jwt token",
                    schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "",
                            content = @Content(schema = @Schema(implementation = Address.class))),
                    @ApiResponse(responseCode = "500", description = "",
                            content = @Content(schema = @Schema(implementation = String.class), examples = @ExampleObject(value = ""))
                    )})
    @GetMapping("/query-standard-address")
    @Authorized(keyName = "AP")
    public ResponseEntity<Address> queryStandardAddress(
            @Parameter(description ="地址",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "台南市東區衛國里007鄰衛國街１１４巷９弄１０號B六樓之５,臺南市(可為空),東區(可為空)")
            )) @RequestParam String address) {
        log.info("address:{}",address);
        UsageUtils.writeUsageLog("/api/api-key/query-standard-address",address);
        return new ResponseEntity<>(addressParser.parseAddress(address,null),HttpStatus.OK);
    }

    /**
     * 取得指定【地址】之地址識別碼相關資訊
     */
    @Operation(description = "取得指定【地址】之地址識別碼相關資訊",
            parameters = {@Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "資拓私鑰加密的jwt token",
                    schema = @Schema(type = "string"))
            }
    )
    @GetMapping("/query-single")
    @Authorized(keyName = "AP")
    public ResponseEntity<SingleQueryResultDTO> queryAddressJson(
            @Parameter(
                    description = "地址、縣市、鄉鎮市區以,區隔組合成字串(順序不可改)。",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "台南市東區衛國里007鄰衛國街１１４巷９弄１０號B六樓之５,臺南市(可為空),東區(可為空)")
                    )
            ) @RequestParam String singleQueryStr) {
        try {
            UsageUtils.writeUsageLog("/api/api-key/query-single", singleQueryStr);
            log.info("單筆查詢，參數為:{}", singleQueryStr);

            if (singleQueryStr.indexOf(",") >= 0) {
                String[] params = singleQueryStr.split(",");
                SingleQueryDTO singleQueryDTO = new SingleQueryDTO();
                switch (params.length) {
                    case 1:
                        singleQueryDTO.setOriginalAddress(params[0]);
                        break;
                    case 2:
                        singleQueryDTO.setOriginalAddress(params[0]);
                        singleQueryDTO.setCounty(params[1]);
                        break;
                    case 3:
                        singleQueryDTO.setOriginalAddress(params[0]);
                        singleQueryDTO.setCounty(params[1]);
                        singleQueryDTO.setTown(params[2]);
                        break;
                    default:
                        SingleQueryResultDTO singleQueryResultDTO = new SingleQueryResultDTO();
                        singleQueryResultDTO.setText("格式輸入錯誤，請重新確認");
                        return new ResponseEntity<>(singleQueryResultDTO, HttpStatus.BAD_REQUEST);
                }
                log.info("singleQueryDTO:{}", singleQueryDTO);
                return ResponseEntity.ok(singleQueryService.findJson(singleQueryDTO));
            } else {
                SingleQueryDTO singleQueryDTO = new SingleQueryDTO();
                singleQueryDTO.setOriginalAddress(singleQueryStr);
                singleQueryService.findJson(singleQueryDTO);
                return ResponseEntity.ok(singleQueryService.findJson(singleQueryDTO));
            }
        }catch (Exception e){
            log.info("e:{}",e.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }




}
