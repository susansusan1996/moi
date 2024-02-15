package com.example.pentaho.cofig;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.hibernate.dialect.VerticaDialect;
import javax.sql.DataSource;


/**
 * 也可以寫在yaml
 **/
@Configuration
public class DataSourceConfig {

    /**
     * Bean
     **/
    @Bean("verticaDataSource")
    @Qualifier("verticaDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
//        DriverManagerDataSource dataSource = new DriverManagerDataSource();
//        /** MSSQL:com.mysql.cj.jdbc.Driver **/
//        /** Postgret:org.postgresql.Driver **/
//        dataSource.setDriverClassName("org.h2.Driver");
//        /**MSSQL:jdbc:sqlserver://localhost:1433;database=spt**/
//        /**Postgret:jdbc:postgresql://localhost:5432/dw**/
//        dataSource.setUrl("jdbc:h2:file:C:/Users/2212009/Desktop/ppppp/pentaho/pentaho/src/main/resources/h2/testdb");
//        dataSource.setUsername("sa");
//        dataSource.setPassword("password");
//        return dataSource;
        return DataSourceBuilder.create().build();
    }



}


