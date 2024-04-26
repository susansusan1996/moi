package com.example.pentaho.repository;

import com.example.pentaho.component.BigDataParams;
import org.springframework.stereotype.Repository;

public interface BigDataConditionRepository {

    int saveConditions(BigDataParams bigDataParams);
}
