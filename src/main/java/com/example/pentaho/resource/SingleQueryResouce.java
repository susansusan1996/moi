package com.example.pentaho.resource;

import com.example.pentaho.component.Address;
import com.example.pentaho.component.IbdTbIhChangeDoorplateHis;
import com.example.pentaho.component.SingleQueryDTO;
import com.example.pentaho.service.SingleQueryService;
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


    /**
     * 拆分地址
     */
    @GetMapping("/query-address")
    public ResponseEntity<Address> queryData(@RequestBody SingleQueryDTO singleQueryDTO) {
        return ResponseEntity.ok(new Address(singleQueryDTO.getOriginalAddress()));
    }

    /***
     * 單筆查詢軌跡
     */
    @PostMapping("/query-track")
    public ResponseEntity<List<IbdTbIhChangeDoorplateHis>> queryTrack(@RequestBody IbdTbIhChangeDoorplateHis IbdTbIhChangeDoorplateHis){
       return new ResponseEntity<>(singleQueryService.singleQueryTrack(IbdTbIhChangeDoorplateHis), HttpStatus.OK);
    }

}
