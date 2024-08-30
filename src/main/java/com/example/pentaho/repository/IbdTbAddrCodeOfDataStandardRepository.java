package com.example.pentaho.repository;

import com.example.pentaho.component.*;

import java.util.List;

public interface IbdTbAddrCodeOfDataStandardRepository {
    List<IbdTbAddrCodeOfDataStandardDTO> findBySeq(List<Integer> seq);

    List<IbdTbAddrCodeOfDataStandardDTO> findByAddressId(List<IbdTbIhChangeDoorplateHis> addressId, Address address);

    List<IbdTbAddrCodeOfDataStandardDTO> findBySeqsAndNumFlrPOS(List<Integer> seq, String numFlrPos);

    List<IbdTbAddrCodeOfDataStandardDTO> findBySeqsGetNumFlrPOS(List<Integer> seq);

    List<DataStandardAndRespositoryDTO> findFromDataStandardAndRepository(List<Integer> seq);


    List<OpenPageDTO> findBySeq(Integer seq);

}
