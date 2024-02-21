package com.example.pentaho.resource;

import com.example.pentaho.component.Address;
import com.example.pentaho.component.SingleQueryDTO;
import com.example.pentaho.service.SingleQueryService;
import com.example.pentaho.utils.AddressParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/singlequery")
public class SingleQueryResouce {

    private static Logger log = LoggerFactory.getLogger(SingleQueryResouce.class);

    @Autowired
    private SingleQueryService singleQueryService;


    /**
     * 拆分地址
     */
  @Operation(description = "單筆查詢",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER,
                            name = "Authorization",
                            description = "聖森的 JWT Token，用聖森的 RSAPublicKey 驗證 Token",
                            required = true,
                            schema = @Schema(type = "string"))}
    )
    @GetMapping("/query-address")
    public ResponseEntity<Address> queryData(@RequestBody SingleQueryDTO singleQueryDTO) {
//        return ResponseEntity.ok(new Address(singleQueryDTO.getOriginalAddress()));
        return ResponseEntity.ok(AddressParser.parseAddress(singleQueryDTO.getOriginalAddress()));

    }


}
