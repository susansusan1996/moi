package com.example.pentaho.repository;

import java.util.List;

public interface IbdTbAddrCodeOfDataStandardRepository {
    List<String> findBySeq(List<Integer> seq);
}
