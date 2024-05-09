package com.example.pentaho.service;

import com.example.pentaho.component.BigDataParams;
import com.example.pentaho.repository.ColumnSelectionRepository;
import com.example.pentaho.repository.IbdTbAddrStatisticsOverallDevRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@Service
public class BigDataService {


    private final static Logger log = LoggerFactory.getLogger(BigDataService.class);


    @Autowired
    private IbdTbAddrStatisticsOverallDevRepository ibdTbAddrStatisticsOverallDevRepository;


    @Autowired
    private ColumnSelectionRepository columnSelectionRepository;




    /**
     *
     * @param formName
     * @return List<IbdTbAddrStatisticsOverallDev>
     */
    public Integer findLog(String formName){
        List<Integer> cnts = ibdTbAddrStatisticsOverallDevRepository.findCntByDataset(formName);
        return cnts.isEmpty()? 0:cnts.get(0);
    }


    public boolean saveConditions(BigDataParams bigDataParams){
        int cnt = columnSelectionRepository.saveConditions(bigDataParams);
        if(cnt > 0){
            return true;
        }
        return false;
    }

}
