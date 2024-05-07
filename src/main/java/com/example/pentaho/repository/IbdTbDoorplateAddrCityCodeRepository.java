package com.example.pentaho.repository;

import java.util.List;

public interface IbdTbDoorplateAddrCityCodeRepository {

    List<String> queryTownByCounty(String COUNTY);
}
