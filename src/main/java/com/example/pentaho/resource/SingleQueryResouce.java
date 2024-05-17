package com.example.pentaho.resource;

import com.example.pentaho.component.*;
import com.example.pentaho.service.SingleQueryService;
import com.example.pentaho.service.SingleTrackQueryService;
import com.example.pentaho.utils.AddressParser;
import com.example.pentaho.utils.UserContextUtils;
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
import com.example.pentaho.service.SystemUpdateService;

import java.util.List;

@RestController
@RequestMapping("/api/singlequery")
@SecurityRequirement(name = "Authorization")
public class SingleQueryResouce {

    private static Logger log = LoggerFactory.getLogger(SingleQueryResouce.class);

    @Autowired
    private SingleQueryService singleQueryService;


    @Autowired
    private SingleTrackQueryService singleQueryTrackService;


    @Autowired
    private AddressParser addressParser;


    @Autowired
    private SystemUpdateService systemUpdateService;


    /**
     * 拆分地址
     */
    @GetMapping("/query-address")
    @Hidden
    public ResponseEntity<Address> queryData(@RequestBody SingleQueryDTO singleQueryDTO) {
//        return ResponseEntity.ok(new Address(singleQueryDTO.getOriginalAddress()));
        return ResponseEntity.ok(addressParser.parseAddress(singleQueryDTO.getOriginalAddress(),null,null));

    }



    @Operation(description = "單筆查詢",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER,
                            name = "Authorization",
                            description = "驗證jwt token,body附帶userInfo={\"Id\":1,\"departName\":\"A05\"} ,departName需為代號",
                            required = true,
                            schema = @Schema(type = "string"))}
    )
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
    ) throws NoSuchFieldException, IllegalAccessException {
//        try {
        return ResponseEntity.ok(singleQueryService.findJson(singleQueryDTO));
//        } catch (Exception e) {
//            log.info("無法解析地址:{}", e.getMessage());
//            return ResponseEntity.ok("無法解析地址");
//        }
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



    @GetMapping("/test")
    @Hidden
    public void checkSum(@RequestParam String addressId){
        boolean isValidate = singleQueryTrackService.checkSum(addressId);
        log.info("isValidate:{}",isValidate);
    }



}
