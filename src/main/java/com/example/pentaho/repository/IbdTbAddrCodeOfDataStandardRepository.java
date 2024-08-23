package com.example.pentaho.repository;

import com.example.pentaho.component.Address;
import com.example.pentaho.component.IbdTbAddrCodeOfDataStandardDTO;
import com.example.pentaho.component.IbdTbIhChangeDoorplateHis;
import com.example.pentaho.component.OpenPageDTO;

import java.util.List;

public interface IbdTbAddrCodeOfDataStandardRepository {
    List<IbdTbAddrCodeOfDataStandardDTO> findBySeq(List<Integer> seq);

    List<IbdTbAddrCodeOfDataStandardDTO> findByAddressId(List<IbdTbIhChangeDoorplateHis> addressId, Address address);

    List<IbdTbAddrCodeOfDataStandardDTO> findBySeqsAndNumFlrPOS(List<Integer> seq, String numFlrPos);

    List<IbdTbAddrCodeOfDataStandardDTO> findBySeqsGetNumFlrPOS(List<Integer> seq);

    List<OpenPageDTO> findBySeq(Integer seq);

}
