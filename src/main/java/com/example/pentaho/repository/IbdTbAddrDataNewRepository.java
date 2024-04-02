package com.example.pentaho.repository;

import com.example.pentaho.component.SingleQueryDTO;

import java.util.List;

public interface IbdTbAddrDataNewRepository {
    Integer querySeqByCriteria(SingleQueryDTO singleQueryDTO);

    List<String> queryDataSourceAndValidityBySeq(List<String> seqList);

    String queryDataSourceAndValidityBySeq(String seq);

    List<String> queryAllArea();
}
