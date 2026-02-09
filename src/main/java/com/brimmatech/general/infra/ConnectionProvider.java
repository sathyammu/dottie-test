package com.brimmatech.general.infra;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import java.sql.Connection;

@Component
public class ConnectionProvider {

    @Autowired
    HikariDataSource dataSource;

    public Connection provideConnection() {
        return DataSourceUtils.getConnection(dataSource);
    }
}
