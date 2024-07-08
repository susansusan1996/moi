package com.example.pentaho.resource;

import com.example.pentaho.service.TextListService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/textList")
@Hidden
public class TextListResource {

    @Autowired
    private TextListService textListService;

    @GetMapping("/find-all-county")
    public ResponseEntity<List<String>> queryCounty(){
        return new ResponseEntity<>(textListService.findAllCounty(), HttpStatus.OK);

    }

    @GetMapping("/find-town-by-county")
    public ResponseEntity<List<String>> queryTown(@RequestParam String COUNTY){
        return new ResponseEntity<>(textListService.findTownByCounty(COUNTY), HttpStatus.OK);

    }

}
