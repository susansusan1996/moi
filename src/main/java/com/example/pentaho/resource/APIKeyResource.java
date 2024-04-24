package com.example.pentaho.resource;

import com.example.pentaho.component.*;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.service.ApiKeyService;
import com.example.pentaho.service.RedisService;
import com.example.pentaho.service.RefreshTokenService;
import com.example.pentaho.service.SingleQueryService;
import com.example.pentaho.service.SingleTrackQueryService;
import com.example.pentaho.utils.AddressParser;
import com.example.pentaho.utils.UserContextUtils;
import io.jsonwebtoken.ExpiredJwtException;
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

import java.text.ParseException;
import java.util.List;

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
    private static final int VALID_TIME = 1440;


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
                            schema = @Schema(type = "string"))}
            ,
            responses = {
                    @ApiResponse(responseCode = "200", description = "資拓私鑰加密JWT Token 時效為1天，refresh_token時效為2天",
                            content = @Content(schema = @Schema(implementation = JwtReponse.class)))}
    )
    @GetMapping("/get-api-key")
    @Authorized(keyName = "SHENG")
    public ResponseEntity<JwtReponse> getAPIKey(@RequestParam String userId) throws Exception {
        return new ResponseEntity<>(apiKeyService.getApiKey(userId), HttpStatus.OK);
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
    public ResponseEntity<JwtReponse> createApiKey(@RequestParam String userId, @RequestParam String reviewResult) throws ParseException {
        JwtReponse response = new JwtReponse();
        if ("REJECT".equals(reviewResult)) {
            refreshTokenService.saveRefreshToken(userId, null, null, reviewResult);
            response.setErrorResponse("已儲存被拒絕申請的使用者資訊");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        try {
            return new ResponseEntity<>(apiKeyService.createApiKey(userId, null, "fromApi"), HttpStatus.OK);
        } catch (MoiException e) {
            response.setErrorResponse(String.valueOf(e));
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.info("e:{}", e.toString());
            throw new MoiException("generate error");
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


    /**
     * 單筆未登入測試
     */
    @Operation(description = "單筆未登入測試")
    @GetMapping("/forguest")
    @UnAuthorized
    @RateLimiting(capacity = 20,tokens = 20,mintues = 1)
    public ResponseEntity<String> forGuestUser() {
        return new ResponseEntity<>("用戶未登入", HttpStatus.OK);
    }


    @PostMapping("/refreshToken")
    @UnAuthorized
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
        log.info("refreshToken:{}", request);
        RefreshToken refreshToken = redisService.findRefreshTokenByUserId(request.getId());
        if (refreshToken != null) {
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


    /***
     * 取得指定【地址識別碼】之異動軌跡
     */
    @Operation(description = "取得指定【地址識別碼】之異動軌跡",
            parameters = {@Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "資拓私鑰加密的jwt token",
                    required = true,
                    schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "",
                            content = @Content(schema = @Schema(implementation = IbdTbIhChangeDoorplateHis.class))),
                    @ApiResponse(responseCode = "500", description = "",
                            content = @Content(schema = @Schema(implementation = String.class), examples = @ExampleObject(value = ""))
                    )})
    @GetMapping("/query-track")
    @Authorized(keyName = "AP")
    public ResponseEntity<List<IbdTbIhChangeDoorplateHis>> queryTrack(
            @Parameter(
                    description = "編碼",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "BSZ7538-0")
                    )
            ) @RequestParam String addressId) {
        return new ResponseEntity<>(singleTrackQueryService.querySingleTrack(addressId), HttpStatus.OK);
    }


    /***
     * OpenAPI
     * 指定【地址】之標準格式地址
     */
    @Operation(description = "指定【地址】之標準格式地址",
            parameters = {@Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "資拓私鑰加密的jwt token",
                    required = true,
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
        return new ResponseEntity<>(addressParser.parseAddress(address,null,null),HttpStatus.OK);
    }

    /**
     * 取得指定【地址】之地址識別碼相關資訊
     */
    @Operation(description = "取得指定【地址】之地址識別碼相關資訊",
            parameters = {@Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "資拓私鑰加密的jwt token",
                    required = true,
                    schema = @Schema(type = "string"))
            }
    )
    @GetMapping("/query-single")
    @Authorized(keyName = "AP")
    public ResponseEntity<String> queryAddressJson(
            @Parameter(
                    description = "地址、縣市、鄉鎮市區以,區隔組合成字串(順序不可改)。",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "台南市東區衛國里007鄰衛國街１１４巷９弄１０號B六樓之５,臺南市(可為空),東區(可為空)")
                    )
            ) @RequestParam String singleQueryStr) {
        log.info("單筆查詢，參數為:{}",singleQueryStr);
        if(singleQueryStr.indexOf(",") >=0){
            String[] params = singleQueryStr.split(",");
            SingleQueryDTO singleQueryDTO = new SingleQueryDTO();
            switch (params.length){
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
                    return new ResponseEntity<>("格式輸入錯誤，請重新確認",HttpStatus.BAD_REQUEST);
            }
            log.info("singleQueryDTO:{}",singleQueryDTO);
            return ResponseEntity.ok(singleQueryService.findJsonTest(singleQueryDTO));
        }else{
            SingleQueryDTO singleQueryDTO = new SingleQueryDTO();
            singleQueryDTO.setOriginalAddress(singleQueryStr);
            return ResponseEntity.ok(singleQueryService.findJsonTest(singleQueryDTO));
        }
    }


}
