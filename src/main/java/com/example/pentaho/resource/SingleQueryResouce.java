package com.example.pentaho.resource;

import com.example.pentaho.component.Address;
import com.example.pentaho.component.IbdTbIhChangeDoorplateHis;
import com.example.pentaho.component.SingleQueryDTO;
import com.example.pentaho.service.SingleQueryService;
import com.example.pentaho.utils.AddressParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/singlequery")
public class SingleQueryResouce {

    private static Logger log = LoggerFactory.getLogger(SingleQueryResouce.class);

    @Autowired
    private SingleQueryService singleQueryService;

    @Autowired
    private AddressParser addressParser;


    /**
     * 拆分地址
     */
    @GetMapping("/query-address")
    public ResponseEntity<Address> queryData(@RequestBody SingleQueryDTO singleQueryDTO) {
//        return ResponseEntity.ok(new Address(singleQueryDTO.getOriginalAddress()));
        return ResponseEntity.ok(addressParser.parseAddress(singleQueryDTO.getOriginalAddress(),null));

    }

    /**
     * 找出地址相對應的json資訊
     */
    @Operation(description = "單筆查詢",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER,
                            name = "Authorization",
                            description = "驗證jwt token,body附帶userInfo={\"Id\":1,\"departName\":\"A05\"} ,departName需為代號",
                            required = true,
                            schema = @Schema(type = "string"))}
    )
    @PostMapping("/query-single")
    public ResponseEntity<String> queryAddressJson(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "單筆查詢",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SingleQueryDTO.class),
                            examples = @ExampleObject(value = "{\"originalAddress\":\"衛國里007鄰衛國街１１４巷９弄１０號B六樓之５\",\"county\":\"臺南市(可為空)\",\"town\":\"東區(可為空)\"}")
                    )
            )
            @RequestBody SingleQueryDTO singleQueryDTO) {
        log.info("單筆查詢，參數為:{}", singleQueryDTO);
        return ResponseEntity.ok(singleQueryService.findJsonTest(singleQueryDTO));
    }




    @GetMapping("/query-single-test")
    public ResponseEntity<String> queryAddress(@RequestBody String originalString) {
        return ResponseEntity.ok(singleQueryService.findJson(originalString));
    }


    /***
     * 單筆查詢軌跡
     */
    @Operation(description = "單筆查詢軌跡",
            parameters = {  @Parameter(in = ParameterIn.HEADER,
                    name = "Authorization",
                    description = "驗證jwt token,body附帶userInfo={\"Id\":1,\"departName\":\"A05\"} ,departName需為代號",
                    required = true,
                    schema = @Schema(type = "string"))
            })
    @PostMapping("/query-track")
    public ResponseEntity<List<IbdTbIhChangeDoorplateHis>> queryTrack(@RequestBody String addressId ){
        return new ResponseEntity<>(singleQueryService.singleQueryTrack(addressId), HttpStatus.OK);
    }

}
