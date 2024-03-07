package com.example.pentaho.repository.impl;

import com.cht.commons.persistence.query.Query;
import com.cht.commons.persistence.query.SqlExecutor;
import com.example.pentaho.component.AliasDTO;
import com.example.pentaho.repository.AliasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AliasRepositoryImpl implements AliasRepository {
    private static final Logger log = LoggerFactory.getLogger(AliasRepositoryImpl.class);

    private final SqlExecutor sqlExecutor;

    public AliasRepositoryImpl(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }


    @Override
    public List<AliasDTO> queryAllAlias() {
        Query query = Query.builder()
                .append("SELECT\n" +
                        "\tALTERNATIVE_NAME AS ALIAS,\n" +
                        "\t'COUNTY' AS TYPENAME\n" +
                        "FROM\n" +
                        "\taddr_stage.IBD_TB_CODE_COUNTY_ALIAS\n" +
                        "UNION \n" +
                        "SELECT\n" +
                        "\tCOALESCE(ALTERNATIVE_ROAD_NAME,\n" +
                        "\tALTERNATIVE_AREA_NAME,\n" +
                        "\t'NONE') AS ALIAS,\n" +
                        "\tcase\n" +
                        "\t\twhen ALTERNATIVE_ROAD_NAME is not null then 'ROAD'\n" +
                        "\t\twhen ALTERNATIVE_AREA_NAME is not null then 'AREA'\n" +
                        "\tEND AS TYPENAME\n" +
                        "FROM\n" +
                        "\taddr_stage.IBD_TB_CODE_ROAD_AREA_ALIAS\n" +
                        "UNION \n" +
                        "SELECT\n" +
                        "\tCOALESCE(ALTERNATIVE_NAME) AS ALIAS,\n" +
                        "\t'TOWN' AS TYPENAME\n" +
                        "FROM\n" +
                        "\taddr_stage.IBD_TB_CODE_TOWN_ALIAS\n" +
                        "UNION \n" +
                        "SELECT\n" +
                        "\tCOALESCE(ALTERNATIVE_NAME) AS ALIAS,\n" +
                        "\t'VILLAGE' AS TYPENAME\n" +
                        "FROM\n" +
                        "\taddr_stage.IBD_TB_CODE_VILLAGE_ALIAS\n" +
                        "ORDER BY\n" +
                        "\tTYPENAME")
                .build();
        log.info("query:{}", query);
        return sqlExecutor.queryForList(query, AliasDTO.class);
    }
}
