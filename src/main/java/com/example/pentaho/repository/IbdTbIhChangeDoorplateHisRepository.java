package com.example.pentaho.repository;


import com.example.pentaho.component.IbdTbIhChangeDoorplateHis;

import java.util.ArrayList;
import java.util.List;

public interface IbdTbIhChangeDoorplateHisRepository {

    List<IbdTbIhChangeDoorplateHis> findByAddressId(String addressId);

    List<IbdTbIhChangeDoorplateHis> findByAddressIdList(List<String> addressIdList);

}
