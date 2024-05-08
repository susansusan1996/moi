package com.example.pentaho.repository;

import com.example.pentaho.component.IbdTbAddrCodeOfDataStandardDTO;

import java.util.List;

public interface IbdTbAddrCodeOfDataStandardRepository {
    List<IbdTbAddrCodeOfDataStandardDTO> findBySeq(List<Integer> seq);

    List<IbdTbAddrCodeOfDataStandardDTO> findByAddressId(List<String> addressId);

}
