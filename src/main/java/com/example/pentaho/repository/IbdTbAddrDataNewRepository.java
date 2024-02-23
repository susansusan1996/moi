package com.example.pentaho.repository;

import com.example.pentaho.component.SingleQueryDTO;

public interface IbdTbAddrDataNewRepository {
    Integer querySeqByCriteria(SingleQueryDTO singleQueryDTO);
}
