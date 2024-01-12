package com.dnamaster10.tcgui.util.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseAccessor {
    private static final HikariDataSource dataSource;
    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DatabaseConfig.getDbUrl());
        config.setUsername(DatabaseConfig.getDbUsername());
        config.setPassword(DatabaseConfig.getDbPassword());

        dataSource = new HikariDataSource(config);
    }
    public Connection getConnection() throws SQLException {
        //Returns a new connection to the database
        return dataSource.getConnection();
    }
    public DatabaseAccessor() throws SQLException {
        //Constructor here in case anything needs to be added in future
    }
}