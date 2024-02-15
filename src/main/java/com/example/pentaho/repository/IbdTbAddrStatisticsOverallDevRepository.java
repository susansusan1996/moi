package com.example.pentaho.repository;

import com.example.pentaho.component.IbdTbAddrStatisticsOverallDev;
import org.springframework.stereotype.Repository;

import java.util.List;


public interface IbdTbAddrStatisticsOverallDevRepository {
    List<IbdTbAddrStatisticsOverallDev> findAll();
}
