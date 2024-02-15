package com.example.pentaho.repository;

import com.example.pentaho.component.User;
import com.example.pentaho.entity.IbdTbAddrStatisticsOverallDev;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IbdTbAddrStatisticsOverallDevRepository extends JpaRepository<IbdTbAddrStatisticsOverallDev, Integer> {
//    Optional<IbdTbAddrStatisticsOverallDev> findIbdTbAddrStatisticsOverallDevById(int id);
}
