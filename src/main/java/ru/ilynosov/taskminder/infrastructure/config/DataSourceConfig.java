package ru.ilynosov.taskminder.infrastructure.config;

import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;

public class DataSourceConfig {

    public static DataSource createDataSource() {

        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(AppConfig.getDbUrl());
        ds.setUser(AppConfig.getDbUser());
        ds.setPassword(AppConfig.getDbPassword());

        return ds;
    }

    public static void migrate(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();
    }
}