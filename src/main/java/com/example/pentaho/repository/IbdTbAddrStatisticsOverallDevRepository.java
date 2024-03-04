package com.example.pentaho.repository;

import com.example.pentaho.component.IbdTbAddrStatisticsOverallDev;

import java.util.List;


public interface IbdTbAddrStatisticsOverallDevRepository {
    List<Integer> findCntByDataset(String formName);
}
