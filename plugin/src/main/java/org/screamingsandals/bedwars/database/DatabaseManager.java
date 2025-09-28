/*
 * Copyright (C) 2023 ScreamingSandals
 *
 * This file is part of Screaming BedWars.
 *
 * Screaming BedWars is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Screaming BedWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Screaming BedWars. If not, see <https://www.gnu.org/licenses/>.
 */

package org.screamingsandals.bedwars.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.screamingsandals.bedwars.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

public class DatabaseManager {
    private String tablePrefix;
    private String database;
    private HikariDataSource dataSource = null;
    private String host;
    private String password;
    private int port;
    private String user;
    private ConfigurationSection params;
    private String type;
    private String driver;

    public DatabaseManager(String host, int port, String user, String password, String database, String tablePrefix, ConfigurationSection params, String type, String driver) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.database = database;
        this.tablePrefix = tablePrefix;
        this.params = params;
        this.type = type;
        this.driver = driver;
    }

    public void initialize() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:" +  this.type + "://" + this.host + ":" + this.port + "/" + this.database);
        config.setUsername(this.user);
        config.setPassword(this.password);

        for (String key : params.getKeys(false)) {
            config.addDataSourceProperty(key, params.getString(key));
        }

        ClassLoader contextCl = null;
        Class<?> driverClazz = null;

        if (driver != null && !"default".equalsIgnoreCase(driver)) {
            try {
                URLClassLoader cl = new URLClassLoader(new URL[] {Main.getInstance().getDataFolder().toPath().resolve(driver).toAbsolutePath().toUri().toURL()}, ClassLoader.getSystemClassLoader().getParent());
                try (InputStream is = cl.getResourceAsStream("META-INF/services/java.sql.Driver")) {
                    if (is == null) {
                        throw new RuntimeException("Database driver JAR does not contain JDBC 4 compatible driver");
                    }
                    String driverClassName = null;
                    try (InputStreamReader isr = new InputStreamReader(is);
                         BufferedReader reader = new BufferedReader(isr)) {
                        do {
                            driverClassName = reader.readLine();
                            if (driverClassName != null) {
                                // All characters after '#' should be ignored, whitespaces around the qualified name are also ignored
                                driverClassName = driverClassName.split("#", 2)[0].trim();
                            }
                        } while (driverClassName != null && driverClassName.isEmpty());
                    }
                    if (driverClassName == null) {
                        throw new RuntimeException("Database driver JAR does not contain JDBC 4 compatible driver");
                    }
                    driverClazz = cl.loadClass(driverClassName);
                    contextCl = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(cl);
                    config.setDriverClassName(driverClassName);
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        this.dataSource = new HikariDataSource(config);

        if (contextCl != null) {
            Thread.currentThread().setContextClassLoader(contextCl);
        }

        if (driverClazz != null) {
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                if (driver.getClass().equals(driverClazz)) {
                    try {
                        DriverManager.deregisterDriver(driver);
                    } catch (SQLException e) {
                        // ignore
                    }
                }
            }
        }
    }

    public Connection getConnection() {
        try {
            return this.dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //#region All Time
    public String getCreateTableSql() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix
                + "stats_players` (`kills` int(11) NOT NULL DEFAULT '0', `wins` int(11) NOT NULL DEFAULT '0', `score` int(11) NOT NULL DEFAULT '0', `loses` int(11) NOT NULL DEFAULT '0', `name` varchar(255) NOT NULL, `destroyedBeds` int(11) NOT NULL DEFAULT '0', `uuid` varchar(36) NOT NULL, `deaths` int(11) NOT NULL DEFAULT '0', PRIMARY KEY (`uuid`))";
    }

    public String getReadObjectSql() {
        return "SELECT * FROM " + tablePrefix + "stats_players WHERE uuid = ? LIMIT 1";
    }

    public String getWriteObjectSql() {
        return "INSERT INTO " + tablePrefix
                + "stats_players(uuid, name, deaths, destroyedBeds, kills, loses, score, wins) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE uuid=VALUES(uuid),name=VALUES(name),deaths=VALUES(deaths),destroyedBeds=VALUES(destroyedBeds),kills=VALUES(kills),loses=VALUES(loses),score=VALUES(score),wins=VALUES(wins)";
    }

    public String getScoresSql() {
        return "SELECT * FROM " + tablePrefix + "stats_players";
    }
    //#endregion

    //#region Seasonal
    public String getSeasonalCreateTableSql() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix
                + "seasonal_stats_players` (`kills` int(11) NOT NULL DEFAULT '0', `wins` int(11) NOT NULL DEFAULT '0', `score` int(11) NOT NULL DEFAULT '0', `loses` int(11) NOT NULL DEFAULT '0', `name` varchar(255) NOT NULL, `destroyedBeds` int(11) NOT NULL DEFAULT '0', `uuid` varchar(36) NOT NULL, `deaths` int(11) NOT NULL DEFAULT '0', PRIMARY KEY (`uuid`))";
    }

    public String getSeasonalReadObjectSql() {
        return "SELECT * FROM " + tablePrefix + "seasonal_stats_players WHERE uuid = ? LIMIT 1";
    }

    public String getSeasonalWriteObjectSql() {
        return "INSERT INTO " + tablePrefix
                + "seasonal_stats_players(uuid, name, deaths, destroyedBeds, kills, loses, score, wins) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE uuid=VALUES(uuid),name=VALUES(name),deaths=VALUES(deaths),destroyedBeds=VALUES(destroyedBeds),kills=VALUES(kills),loses=VALUES(loses),score=VALUES(score),wins=VALUES(wins)";
    }

    public String getSeasonalResetSql() {
        return "TRUNCATE TABLE " + tablePrefix + "seasonal_stats_players";
    }

    public String getSeasonalScoresSql() {
        return "SELECT * FROM " + tablePrefix + "seasonal_stats_players";
    }
    //#endregion

    //#region Daily
    public String getDailyCreateTableSql() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix
                + "daily_stats_players` (`kills` int(11) NOT NULL DEFAULT '0', `wins` int(11) NOT NULL DEFAULT '0', `score` int(11) NOT NULL DEFAULT '0', `loses` int(11) NOT NULL DEFAULT '0', `name` varchar(255) NOT NULL, `destroyedBeds` int(11) NOT NULL DEFAULT '0', `uuid` varchar(36) NOT NULL, `deaths` int(11) NOT NULL DEFAULT '0', PRIMARY KEY (`uuid`))";
    }

    public String getDailyReadObjectSql() {
        return "SELECT * FROM " + tablePrefix + "daily_stats_players WHERE uuid = ? LIMIT 1";
    }

    public String getDailyWriteObjectSql() {
        return "INSERT INTO " + tablePrefix
                + "daily_stats_players(uuid, name, deaths, destroyedBeds, kills, loses, score, wins) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE uuid=VALUES(uuid),name=VALUES(name),deaths=VALUES(deaths),destroyedBeds=VALUES(destroyedBeds),kills=VALUES(kills),loses=VALUES(loses),score=VALUES(score),wins=VALUES(wins)";
    }

    public String getDailyScoresSql() {
        return "SELECT * FROM " + tablePrefix + "daily_stats_players";
    }

    public String getDailyResetSql() {
        return "TRUNCATE TABLE " + tablePrefix + "daily_stats_players";
    }
    //#endregion

    //#region Events
    // TODO: Remove this after the event ended
    public String getEventCreateTableSql() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix
                + "event_stats_players` (`kills` int(11) NOT NULL DEFAULT '0', `wins` int(11) NOT NULL DEFAULT '0', `score` int(11) NOT NULL DEFAULT '0', `loses` int(11) NOT NULL DEFAULT '0', `name` varchar(255) NOT NULL, `destroyedBeds` int(11) NOT NULL DEFAULT '0', `uuid` varchar(36) NOT NULL, `deaths` int(11) NOT NULL DEFAULT '0', PRIMARY KEY (`uuid`))";
    }

    public String getEventReadObjectSql() {
        return "SELECT * FROM " + tablePrefix + "event_stats_players WHERE uuid = ? LIMIT 1";
    }

    public String getEventWriteObjectSql() {
        return "INSERT INTO " + tablePrefix
                + "event_stats_players(uuid, name, deaths, destroyedBeds, kills, loses, score, wins) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE uuid=VALUES(uuid),name=VALUES(name),deaths=VALUES(deaths),destroyedBeds=VALUES(destroyedBeds),kills=VALUES(kills),loses=VALUES(loses),score=VALUES(score),wins=VALUES(wins)";
    }

    public String getEventScoresSql() {
        return "SELECT * FROM " + tablePrefix + "event_stats_players";
    }

    public String getEventResetSql() {
        return "TRUNCATE TABLE " + tablePrefix + "event_stats_players";
    }
    //#endregion

    public String getTablePrefix() {
        return tablePrefix;
    }
}