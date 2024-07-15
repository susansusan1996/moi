package com.example.pentaho.repository;

import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public interface JobStatusRepository {

    public int insertJobStatus(Map<String,String> result);
}
