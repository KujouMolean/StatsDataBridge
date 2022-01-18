package com.molean.statsdatabridge;

import com.alibaba.druid.pool.DruidDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;

public class DataSourceUtils {
    private static DruidDataSource dataSource;


    public static Connection getConnection() throws SQLException {

        if (dataSource != null) {
            return dataSource.getConnection();
        }

        DruidDataSource dataSource = new DruidDataSource();
        FileConfiguration config = JavaPlugin.getPlugin(StatsDataBridge.class).getConfig();
        String url = config.getString("db.url");
        String username =  config.getString("db.username");
        String password = config.getString("db.password");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        //validate
        dataSource.setValidationQueryTimeout(30);
        dataSource.setValidationQuery("select 1");

        //timeout
        dataSource.setMaxWait(30000);
        dataSource.setQueryTimeout(30);
        dataSource.setKillWhenSocketReadTimeout(true);

        //max active
        dataSource.setMaxActive(64);

        DataSourceUtils.dataSource = dataSource;
        return getConnection();
    }

}
