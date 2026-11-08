package com.learning.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AppConfig {

    @Bean("postgresDataSourceProperties")
    @Primary
    @ConfigurationProperties("app.datasource.postgres")
    public DataSourceProperties postgresDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean("postgresDataSource")
    @Primary
    @ConfigurationProperties("app.datasource.postgres.configuration")
    public HikariDataSource postgresDataSourcePool(
            @Qualifier("postgresDataSourceProperties") DataSourceProperties dsp) {
        return dsp.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }
}
