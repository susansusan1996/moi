package com.example.pentaho.cofig;

import com.cht.commons.persistence.query.SqlExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class CompostPersistenceConfiguration {

    @Primary
    @Bean
    public SqlExecutor sqlExecutor(@Qualifier("namedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        return new SqlExecutor(jdbcTemplate);
    }

//    @Bean
//    public SqlExecutor dwSqlExecutor(@Qualifier("dwNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
//        return new SqlExecutor(jdbcTemplate);
//    }
}
