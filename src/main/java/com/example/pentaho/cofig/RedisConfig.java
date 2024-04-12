package com.example.pentaho.cofig;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Bean(name = "stringRedisTemplate0")
    public StringRedisTemplate stringRedisTemplate0(
            @Value("${spring.data.redis0.database}")
            int database,
            @Value("${spring.data.redis0.timeout:5}")
            long timeout,
            @Value("${spring.data.redis0.lettuce.pool.max-active}")
            int maxActive,
            @Value("${spring.data.redis0.lettuce.pool.max-wait}")
            int maxWait,
            @Value("${spring.data.redis0.lettuce.pool.max-idle}")
            int maxIdle,
            @Value("${spring.data.redis0.lettuce.pool.min-idle}")
            int minIdle,
            @Value("${spring.data.redis0.host}")
            String host,
            @Value("${spring.data.redis0.password}")
            String password,
            @Value("${spring.data.redis0.port}")
            int port
    ) {
        // connection config
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setPassword(RedisPassword.of(password));
        configuration.setDatabase(database);

        // pool config
        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxTotal(maxActive);
        genericObjectPoolConfig.setMinIdle(minIdle);
        genericObjectPoolConfig.setMaxIdle(maxIdle);
        genericObjectPoolConfig.setMaxWaitMillis(maxWait);

        // create connection factory
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder();
        builder.poolConfig(genericObjectPoolConfig);
        builder.commandTimeout(Duration.ofSeconds(timeout));
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
                configuration, builder.build()
        );
        connectionFactory.afterPropertiesSet();

        // create redis template
        return createStringRedisTemplate(connectionFactory);

    }


    @Bean(name = "stringRedisTemplate1")
    public StringRedisTemplate stringRedisTemplate1(
            @Value("${spring.data.redis1.database}")
            int database,
            @Value("${spring.data.redis1.timeout:5}")
            long timeout,
            @Value("${spring.data.redis1.lettuce.pool.max-active}")
            int maxActive,
            @Value("${spring.data.redis1.lettuce.pool.max-wait}")
            int maxWait,
            @Value("${spring.data.redis1.lettuce.pool.max-idle}")
            int maxIdle,
            @Value("${spring.data.redis1.lettuce.pool.min-idle}")
            int minIdle,
            @Value("${spring.data.redis1.host}")
            String host,
            @Value("${spring.data.redis1.password}")
            String password,
            @Value("${spring.data.redis1.port}")
            int port
    ) {
        // connection config
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setPassword(RedisPassword.of(password));
        configuration.setDatabase(database);

        // pool config
        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxTotal(maxActive);
        genericObjectPoolConfig.setMinIdle(minIdle);
        genericObjectPoolConfig.setMaxIdle(maxIdle);
        genericObjectPoolConfig.setMaxWaitMillis(maxWait);

        // create connection factory
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder();
        builder.poolConfig(genericObjectPoolConfig);
        builder.commandTimeout(Duration.ofSeconds(timeout));
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
                configuration, builder.build()
        );
        connectionFactory.afterPropertiesSet();

        // create redis template
        return createStringRedisTemplate(connectionFactory);

    }


    @Bean(name = "stringRedisTemplate2")
    public StringRedisTemplate stringRedisTemplate2(
            @Value("${spring.data.redis2.database}")
            int database,
            @Value("${spring.data.redis2.timeout:5}")
            long timeout,
            @Value("${spring.data.redis2.lettuce.pool.max-active}")
            int maxActive,
            @Value("${spring.data.redis2.lettuce.pool.max-wait}")
            int maxWait,
            @Value("${spring.data.redis2.lettuce.pool.max-idle}")
            int maxIdle,
            @Value("${spring.data.redis2.lettuce.pool.min-idle}")
            int minIdle,
            @Value("${spring.data.redis2.host}")
            String host,
            @Value("${spring.data.redis2.password}")
            String password,
            @Value("${spring.data.redis2.port}")
            int port
    ) {
        // connection config
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setPassword(RedisPassword.of(password));
        configuration.setDatabase(database);

        // pool config
        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxTotal(maxActive);
        genericObjectPoolConfig.setMinIdle(minIdle);
        genericObjectPoolConfig.setMaxIdle(maxIdle);
        genericObjectPoolConfig.setMaxWaitMillis(maxWait);

        // create connection factory
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder();
        builder.poolConfig(genericObjectPoolConfig);
        builder.commandTimeout(Duration.ofSeconds(timeout));
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
                configuration, builder.build()
        );
        connectionFactory.afterPropertiesSet();

        // create redis template
        return createStringRedisTemplate(connectionFactory);

    }

    /**
     * 建立StringRedisTemplate
     * 此function不能加 @Bean 否则onnectionFactory 将会一律采用预设值
     */
    private StringRedisTemplate createStringRedisTemplate(
            RedisConnectionFactory redisConnectionFactory
    ) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        return redisTemplate;
    }

}
