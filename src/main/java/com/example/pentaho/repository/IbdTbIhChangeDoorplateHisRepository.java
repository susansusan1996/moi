package com.example.pentaho.repository;


import com.example.pentaho.component.IbdTbIhChangeDoorplateHis;

import java.util.List;

public interface IbdTbIhChangeDoorplateHisRepository {

    List<IbdTbIhChangeDoorplateHis> findByhisCity(String hisCity);
}