package com.example.pentaho.cofig;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableTransactionManagement /*開啟事務處理**/
@EnableJpaRepositories(entityManagerFactoryRef = "verticaEntityManagerFactory",
        transactionManagerRef = "verticaTransactionManager",
        basePackages = "com.example.pentaho.repository"
)
public class VerticaDBConfig {

    @Autowired
    private JpaProperties jpaProperties;

    @Autowired
    private HibernateProperties hibernateProperties;

    @Value("${spring.jpa.hibernate.dialect}")
    private String verticaDialect;

    @Autowired
    @Qualifier("verticaDataSource")
    private DataSource verticaDataSource;



    @Bean(name="verticaEntityManagerFactory")
     public LocalContainerEntityManagerFactoryBean verticaEntityManagerFactory(EntityManagerFactoryBuilder builder){
        Map<String, Object> properties = hibernateProperties.determineHibernateProperties(jpaProperties.getProperties(), new HibernateSettings());
        properties.put("hibernate.dialect",verticaDialect);
        return builder.dataSource(verticaDataSource)
                .properties(properties)
                .packages("com.example.pentaho.entity")
                .build();
    }


    @Bean(name="verticaEntityManager")
    public EntityManager verticaEntityManager(EntityManagerFactoryBuilder builder){
        return verticaEntityManagerFactory(builder).getObject().createEntityManager();
    }


    @Bean(name="verticaTransactionManager")
    public PlatformTransactionManager verticaTransactionManager(EntityManagerFactoryBuilder builder){
       return new JpaTransactionManager(verticaEntityManagerFactory(builder).getObject());
    }
}
