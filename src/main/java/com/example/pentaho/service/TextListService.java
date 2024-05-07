package com.example.pentaho.service;

import com.example.pentaho.repository.IbdTbDoorplateAddrCityCodeRepository;
import com.example.pentaho.repository.IbdTbDoorplateCounCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TextListService {

    private final static Logger log = LoggerFactory.getLogger(TextListService.class);

    @Autowired
    private IbdTbDoorplateCounCodeRepository ibdTbDoorplateCounCodeRepository;

    @Autowired
    private IbdTbDoorplateAddrCityCodeRepository ibdTbDoorplateAddrCityCodeRepository;

    public List<String> findAllCounty(){
        List<String> countyList = ibdTbDoorplateCounCodeRepository.queryAllCounty();
        log.info("find all county:{}", countyList);
        return countyList;
    }

    public List<String> findTownByCounty(String COUNTY){
        List<String> townList = ibdTbDoorplateAddrCityCodeRepository.queryTownByCounty(COUNTY);
        log.info("find town by county:{}", townList);
        return townList;
    }

}
