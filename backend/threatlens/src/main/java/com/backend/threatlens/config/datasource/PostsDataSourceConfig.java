package com.backend.threatlens.config.datasource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class PostsDataSourceConfig {

    @Bean
    @ConfigurationProperties("posts.datasource")
    public DataSourceProperties postsDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource postsDataSource( @Qualifier("postsDataSourceProperties") DataSourceProperties postsDataSourceProperties) {
        return postsDataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean
    public NamedParameterJdbcTemplate postsJdbcTemplate(@Qualifier("postsDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    // Usado só quando uma consulta precisa de transação no banco de posts (ex.: SET LOCAL);
    // o gerenciador padrão (@Primary) continua sendo o do banco da aplicação
    @Bean
    public PlatformTransactionManager postsTransactionManager(@Qualifier("postsDataSource") DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }
}
