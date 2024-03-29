package com.example.pentaho.resource;

import com.example.pentaho.component.IbdTbIhChangeDoorplateHis;
import com.example.pentaho.component.KeyComponent;
import com.example.pentaho.component.Token;
import com.example.pentaho.component.User;
import com.example.pentaho.exception.MoiException;
import com.example.pentaho.service.SingleTrackQueryService;
import com.example.pentaho.utils.RSAJWTUtils;
import com.example.pentaho.utils.RsaUtils;
import com.example.pentaho.utils.UserContextUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.PrivateKey;
import java.util.List;

/***
 * 產生APIKey & 使用APIKey驗證的API
 */
@RestController
@RequestMapping("/api/api-key")
public class APIKeyResource {
    private static Logger log = LoggerFactory.getLogger(APIKeyResource.class);

    /**API效期先設定1天**/
    private static final int VALID_TIME = 1440;

    @Autowired
    private SingleTrackQueryService singleQueryTrackService;

    private KeyComponent keyComponent;


    public APIKeyResource(KeyComponent keyComponent) {
        this.keyComponent = keyComponent;
    }


    @Operation(description = "獲取APIKEY",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER,
                            name = "Authorization",
                            description = "聖森私鑰加密 jwt token,body附帶userInfo={\"Id\":\"673f7eec-8ae5-4e79-ad3a-42029eedf742\",\"orgId\":\"ADMIN\"}",
                            required = true,
                            schema = @Schema(type = "string"))}
            ,
            responses = {
                    @ApiResponse(responseCode = "200", description = "資拓私鑰加密JWT Token 時效為1天",
                            content = @Content(schema = @Schema(implementation = String.class)))}
    )
    @PostMapping("/getAuthorization")
    public ResponseEntity<Token> getAPIKey(){
        try{
        User user = UserContextUtils.getUserHolder();
        log.info("user:{}",user);
        PrivateKey privateKey = RsaUtils.getPrivateKey((keyComponent.getApPrikeyName()));
            Token token = new Token(RSAJWTUtils.generateTokenExpireInMinutes(user, privateKey, VALID_TIME));
            return new ResponseEntity<>(token, HttpStatus.OK);
        }catch (Exception e){
            log.info("e:{}",e.toString());
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
                    required = true,
                    schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "userId",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "500", description = ""),
            })
    @PostMapping("/forapikey")
    public ResponseEntity<String> forAPIKeyUser(){
        User user = UserContextUtils.getUserHolder();
        log.info("user:{}",user);
        return new ResponseEntity<>(user.getId(),HttpStatus.OK);
    }


    /**
     * 單筆軌跡未登入測試
     */
    @Operation(description = "單筆軌跡未登入",
            parameters = {@Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "jwt token,body附帶 userInfo={\"Id\":1,\"orgId\":\"Admin\"}",
                    required = true,
                    schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "",
                            content = @Content(schema = @Schema(implementation = IbdTbIhChangeDoorplateHis.class))),
                    @ApiResponse(responseCode = "500", description = "",
                            content = @Content(schema = @Schema(implementation = String.class), examples = @ExampleObject(value = ""))
                    )})
    @PostMapping("/query-track-for-guest")
    public ResponseEntity<List<IbdTbIhChangeDoorplateHis>> queryTrack(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "編碼",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "BSZ7538-0")
                    )
            )
            @RequestBody String addressId) {
        return new ResponseEntity<>(singleQueryTrackService.querySingleTrack(addressId), HttpStatus.OK);
    }
}
