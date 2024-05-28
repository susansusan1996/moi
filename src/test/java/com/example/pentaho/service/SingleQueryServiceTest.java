package com.example.pentaho.service;

import com.example.pentaho.component.SingleQueryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(classes = SingleQueryService.class)
@ActiveProfiles({"dev"})
class SingleQueryServiceTest {

    @MockBean
    private SingleQueryService singleQueryService;

    private SingleQueryDTO singleQueryDTO;


    private final static Logger log = LoggerFactory.getLogger(SingleQueryServiceTest.class);

    @BeforeEach
    void setUp(){
        singleQueryDTO = new SingleQueryDTO();
        singleQueryDTO.setCounty("");
        singleQueryDTO.setTown("");
        singleQueryDTO.setOriginalAddress("新北市新莊路中正路514巷");
    }
    @Test
    void findJsonTest() {

    }

    @Test
    void findJson() throws NoSuchFieldException, IllegalAccessException {
        log.info("in");
        singleQueryService.findJson(singleQueryDTO);
    }

    @Test
    void parseAddressAndfindMappingId() {
    }

    @Test
    void findSeqByMappingIdAndJoinStep() {
    }

    @Test
    void singleQueryTrack() {
    }

    @Test
    void findSeqByMappingId() {
    }

    @Test
    void splitSeqAndStep() {
    }

    @Test
    void setAddressAndFindCdByRedis() {
    }

    @Test
    void formatCoutinuousFlrNum() {
    }

    @Test
    void removeBasementAndChangeFtoFloor() {
    }

    @Test
    void setNumFlrId() {
    }

    @Test
    void findNeighborCd() {
    }

    @Test
    void getNumFlrPos() {
    }

    @Test
    void checkIfMultiAddress() {
    }
}