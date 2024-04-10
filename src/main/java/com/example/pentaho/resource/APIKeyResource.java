package com.example.pentaho.resource;

import com.example.pentaho.component.JwtReponse;
import com.example.pentaho.component.RefreshToken;
import com.example.pentaho.component.User;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.service.ApiKeyService;
import com.example.pentaho.service.RefreshTokenService;
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
    public ResponseEntity<JwtReponse> getAPIKey(@RequestParam String userId) {
        try {
            return new ResponseEntity<>(apiKeyService.getApiKey(userId), HttpStatus.OK);
        } catch (Exception e) {
            log.info("e:{}", e.toString());
            throw new MoiException("generate error");
        }
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
                            schema = @Schema(type = "string"))}
            ,
            responses = {
                    @ApiResponse(responseCode = "200", description = "資拓私鑰加密JWT Token 時效為1天，refresh_token時效為2天",
                            content = @Content(schema = @Schema(implementation = JwtReponse.class)))}
    )
    @PostMapping("/create-api-key")
    public ResponseEntity<JwtReponse> createApiKey(@RequestParam String userId) {
        try {
            return new ResponseEntity<>(apiKeyService.createApiKey(userId, null), HttpStatus.OK);
        } catch (MoiException e) {
            JwtReponse response = new JwtReponse();
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
    public ResponseEntity<String> forAPIKeyUser() {
        User user = UserContextUtils.getUserHolder();
        log.info("user:{}", user);
        return new ResponseEntity<>(user.getId(), HttpStatus.OK);
    }


    /**
     * 單筆未登入測試
     */
    @Operation(description = "單筆未登入測試")
    @PostMapping("/forguest")
    public ResponseEntity<String> forGuestUser() {
        return new ResponseEntity<>("用戶未登入", HttpStatus.OK);
    }


    @PostMapping("/refreshToken")
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
        List<RefreshToken> refreshTokens = refreshTokenService.findByRefreshTokenAndUserId(request);
        if (!refreshTokens.isEmpty()) {
            try {
                if (refreshTokenService.verifyExpiration(refreshTokens.get(0).getRefreshToken(), "refresh_token")) {
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
}
