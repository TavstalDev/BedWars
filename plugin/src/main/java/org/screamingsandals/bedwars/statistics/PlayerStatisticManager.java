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

package org.screamingsandals.bedwars.statistics;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.screamingsandals.bedwars.Main;
import org.screamingsandals.bedwars.api.events.BedwarsSavePlayerStatisticEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.screamingsandals.bedwars.api.statistics.LeaderboardEntry;
import org.screamingsandals.bedwars.api.statistics.PlayerStatisticsManager;
import org.screamingsandals.bedwars.api.statistics.ELeaderboardKind;
import org.screamingsandals.bedwars.api.statistics.ELeaderboardStatType;

import java.io.File;
import java.sql.*;
import java.util.*;

public class PlayerStatisticManager implements PlayerStatisticsManager {
    private File databaseFile = null;
    private FileConfiguration fileDatabase = null;
    private final Map<UUID, PlayerStatistic> allScores = new HashMap<>();

    private File seasonalDatabaseFile = null;
    private FileConfiguration seasonalFileDatabase = null;;
    private final Map<UUID, PlayerStatistic> seasonalScores = new HashMap<>();

    private File dailyDatabaseFile = null;
    private FileConfiguration dailyFileDatabase = null;;
    private final Map<UUID, PlayerStatistic> dailyScores = new HashMap<>();

    // TODO: Remove this after the event ended, this is a temporal solution
    private File eventDatabaseFile = null;
    private FileConfiguration eventFileDatabase = null;;
    private final Map<UUID, PlayerStatistic> eventScores = new HashMap<>();

    public PlayerStatistic getStatistic(OfflinePlayer player) {
        if (player == null) {
            return null;
        }
        return getStatistic(player.getUniqueId());
    }

    public PlayerStatistic getStatistic(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        var local = allScores.get(uuid);
        // Try prevent null pointer exceptions
        if (local == null) {
            addStatistic(uuid);
            local = allScores.get(uuid);
        }
        return local;
    }

    public PlayerStatistic getSeasonalStatistic(OfflinePlayer player) {
        if (player == null) {
            return null;
        }
        return getSeasonalStatistic(player.getUniqueId());
    }

    public PlayerStatistic getSeasonalStatistic(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        var local =  seasonalScores.get(uuid);
        // Try prevent null pointer exceptions
        if (local == null) {
            addSeasonalStatistic(uuid);
            local =  seasonalScores.get(uuid);
        }
        return local;
    }

    public PlayerStatistic getDailyStatistic(OfflinePlayer player) {
        if (player == null) {
            return null;
        }
        return getDailyStatistic(player.getUniqueId());
    }

    public PlayerStatistic getDailyStatistic(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        var local =  dailyScores.get(uuid);
        // Try prevent null pointer exceptions
        if (local == null) {
            addDailyStatistic(uuid);
            local = dailyScores.get(uuid);
        }
        return local;
    }

    // TODO: Remove this after the event ended, this is a temporal solution
    public PlayerStatistic getEventStatistic(OfflinePlayer player) {
        if (player == null) {
            return null;
        }
        return getEventStatistic(player.getUniqueId());
    }

    // TODO: Remove this after the event ended, this is a temporal solution
    public PlayerStatistic getEventStatistic(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        var local =  eventScores.get(uuid);
        // Try prevent null pointer exceptions
        if (local == null) {
            addEventStatistic(uuid);
            local = eventScores.get(uuid);
        }
        return local;
    }

    public void initialize() {
        if (!Main.getConfigurator().config.getBoolean("statistics.enabled", false)) {
            return;
        }

        if (Main.getConfigurator().config.getString("statistics.type").equalsIgnoreCase("database")) {
            this.initializeDatabase();
        } else {
            File allStatFile = new File(Main.getInstance().getDataFolder() + "/database/bw_stats_players.yml");
            File seasonalStatFile = new File(Main.getInstance().getDataFolder() + "/database/bw_seasonal_stats_players.yml");
            File dailyStatFile = new File(Main.getInstance().getDataFolder() + "/database/bw_daily_stats_players.yml");
            // TODO: Remove this after the event ended, this is a temporal solution
            File eventStatFile = new File(Main.getInstance().getDataFolder() + "/database/bw_event_stats_players.yml");
            this.loadYml(allStatFile, seasonalStatFile, dailyStatFile, eventStatFile);
        }

        this.initializeLeaderboard();
    }

    public void initializeDatabase() {
        Main.getInstance().getLogger().info("Loading statistics from database ...");

        try {
            Main.getDatabaseManager().initialize();
            try (Connection connection = Main.getDatabaseManager().getConnection()) {
                connection.setAutoCommit(false);

                // Create all-time stats
                try (PreparedStatement preparedStatement = connection
                        .prepareStatement(Main.getDatabaseManager().getCreateTableSql())) {
                    preparedStatement.executeUpdate();
                } catch (Exception ex) {
                    Main.getInstance().getLogger().severe("Couldn't create all-time statistics table.");
                    Main.getInstance().getLogger().severe(ex.getMessage());
                }

                // Create seasonal stats
                try (PreparedStatement preparedStatement = connection
                        .prepareStatement(Main.getDatabaseManager().getSeasonalCreateTableSql())) {
                    preparedStatement.executeUpdate();
                } catch (Exception ex) {
                    Main.getInstance().getLogger().severe("Couldn't create seasonal statistics table.");
                    Main.getInstance().getLogger().severe(ex.getMessage());
                }

                // Create daily stats
                try (PreparedStatement preparedStatement = connection
                        .prepareStatement(Main.getDatabaseManager().getDailyCreateTableSql())) {
                    preparedStatement.executeUpdate();
                } catch (Exception ex) {
                    Main.getInstance().getLogger().severe("Couldn't create daily statistics table.");
                    Main.getInstance().getLogger().severe(ex.getMessage());
                }

                // TODO: Remove this after the event ended, this is a temporal solution
                // Create event stats
                try (PreparedStatement preparedStatement = connection
                        .prepareStatement(Main.getDatabaseManager().getEventCreateTableSql())) {
                    preparedStatement.executeUpdate();
                } catch (Exception ex) {
                    Main.getInstance().getLogger().severe("Couldn't create event statistics table.");
                    Main.getInstance().getLogger().severe(ex.getMessage());
                }

                connection.commit();
            } catch (Exception ex) {
                Main.getInstance().getLogger().severe("Couldn't create statistics tables.");
                Main.getInstance().getLogger().severe(ex.getMessage());
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Couldn't initialize database connection.");
            Main.getInstance().getLogger().severe(e.getMessage());
        }

    }

    private void initializeLeaderboard() {
        allScores.clear();
        seasonalScores.clear();
        dailyScores.clear();
        eventScores.clear();

        if (Main.getConfigurator().config.getString("statistics.type").equalsIgnoreCase("database")) {
            try (Connection connection = Main.getDatabaseManager().getConnection()) {
                connection.setAutoCommit(false);

                // Load all-time stats
                try (PreparedStatement preparedStatement = connection
                        .prepareStatement(Main.getDatabaseManager().getScoresSql(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (resultSet.first()) {
                        do {
                            UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                            PlayerStatistic statistic = new PlayerStatistic(uuid);
                            statistic.addKills(resultSet.getInt("kills"));
                            statistic.addDeaths(resultSet.getInt("deaths"));
                            statistic.addWins(resultSet.getInt("wins"));
                            statistic.addLoses(resultSet.getInt("loses"));
                            statistic.addDestroyedBeds(resultSet.getInt("destroyedBeds"));
                            statistic.addScore(resultSet.getInt("score"));
                            statistic.setName(resultSet.getString("name"));
                            allScores.put(uuid, statistic);
                        } while (resultSet.next());
                    }
                } catch (Exception ex) {
                    Main.getInstance().getLogger().severe("Couldn't load all-time statistics table.");
                    Main.getInstance().getLogger().severe(ex.getMessage());
                }

                // Seasonal stats
                try (PreparedStatement preparedStatement = connection
                        .prepareStatement(Main.getDatabaseManager().getSeasonalScoresSql(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (resultSet.first()) {
                        do {
                            UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                            PlayerStatistic statistic = new PlayerStatistic(uuid);
                            statistic.addKills(resultSet.getInt("kills"));
                            statistic.addDeaths(resultSet.getInt("deaths"));
                            statistic.addWins(resultSet.getInt("wins"));
                            statistic.addLoses(resultSet.getInt("loses"));
                            statistic.addDestroyedBeds(resultSet.getInt("destroyedBeds"));
                            statistic.addScore(resultSet.getInt("score"));
                            statistic.setName(resultSet.getString("name"));
                            seasonalScores.put(uuid, statistic);
                        } while (resultSet.next());
                    }
                } catch (Exception ex) {
                    Main.getInstance().getLogger().severe("Couldn't load seasonal statistics table.");
                    Main.getInstance().getLogger().severe(ex.getMessage());
                }

                // Daily stats
                try (PreparedStatement preparedStatement = connection
                        .prepareStatement(Main.getDatabaseManager().getDailyScoresSql(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (resultSet.first()) {
                        do {
                            UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                            PlayerStatistic statistic = new PlayerStatistic(uuid);
                            statistic.addKills(resultSet.getInt("kills"));
                            statistic.addDeaths(resultSet.getInt("deaths"));
                            statistic.addWins(resultSet.getInt("wins"));
                            statistic.addLoses(resultSet.getInt("loses"));
                            statistic.addDestroyedBeds(resultSet.getInt("destroyedBeds"));
                            statistic.addScore(resultSet.getInt("score"));
                            statistic.setName(resultSet.getString("name"));
                            dailyScores.put(uuid, statistic);
                        } while (resultSet.next());
                    }
                } catch (Exception ex) {
                    Main.getInstance().getLogger().severe("Couldn't load daily statistics table.");
                    Main.getInstance().getLogger().severe(ex.getMessage());
                }

                // TODO: Remove this after the event ended, this is a temporal solution
                // Event stats
                try (PreparedStatement preparedStatement = connection
                        .prepareStatement(Main.getDatabaseManager().getEventScoresSql(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (resultSet.first()) {
                        do {
                            UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                            PlayerStatistic statistic = new PlayerStatistic(uuid);
                            statistic.addKills(resultSet.getInt("kills"));
                            statistic.addDeaths(resultSet.getInt("deaths"));
                            statistic.addWins(resultSet.getInt("wins"));
                            statistic.addLoses(resultSet.getInt("loses"));
                            statistic.addDestroyedBeds(resultSet.getInt("destroyedBeds"));
                            statistic.addScore(resultSet.getInt("score"));
                            statistic.setName(resultSet.getString("name"));
                            dailyScores.put(uuid, statistic);
                        } while (resultSet.next());
                    }
                } catch (Exception ex) {
                    Main.getInstance().getLogger().severe("Couldn't load event statistics table.");
                    Main.getInstance().getLogger().severe(ex.getMessage());
                }

                connection.commit();
            } catch (Exception ex) {
                Main.getInstance().getLogger().severe("Couldn't load statistics from the database.");
                Main.getInstance().getLogger().severe(ex.getMessage());
            }
        } else {
            if (!fileDatabase.isSet("data") || !fileDatabase.isConfigurationSection("data")) {
                return;
            }

            // Load all-time stats
            for (String key : fileDatabase.getConfigurationSection("data").getKeys(false)) {
                PlayerStatistic statistic = new PlayerStatistic(UUID.fromString(key));
                statistic.addKills(fileDatabase.getInt("data." + key + ".kills"));
                statistic.addDeaths(fileDatabase.getInt("data." + key + ".deaths"));
                statistic.addWins(fileDatabase.getInt("data." + key + ".wins"));
                statistic.addLoses(fileDatabase.getInt("data." + key + ".loses"));
                statistic.addDestroyedBeds(fileDatabase.getInt("data." + key + ".destroyedBeds"));
                statistic.addScore(fileDatabase.getInt("data." + key + ".score"));
                statistic.setName(fileDatabase.getString("data." + key + ".name"));
                allScores.put(UUID.fromString(key), statistic);
            }

            // Load seasonal stats
            for (String key : seasonalFileDatabase.getConfigurationSection("data").getKeys(false)) {
                PlayerStatistic statistic = new PlayerStatistic(UUID.fromString(key));
                statistic.addKills(seasonalFileDatabase.getInt("data." + key + ".kills"));
                statistic.addDeaths(seasonalFileDatabase.getInt("data." + key + ".deaths"));
                statistic.addWins(seasonalFileDatabase.getInt("data." + key + ".wins"));
                statistic.addLoses(seasonalFileDatabase.getInt("data." + key + ".loses"));
                statistic.addDestroyedBeds(seasonalFileDatabase.getInt("data." + key + ".destroyedBeds"));
                statistic.addScore(seasonalFileDatabase.getInt("data." + key + ".score"));
                statistic.setName(seasonalFileDatabase.getString("data." + key + ".name"));
                seasonalScores.put(UUID.fromString(key), statistic);
            }

            // Load daily stats
            for (String key : dailyFileDatabase.getConfigurationSection("data").getKeys(false)) {
                PlayerStatistic statistic = new PlayerStatistic(UUID.fromString(key));
                statistic.addKills(dailyFileDatabase.getInt("data." + key + ".kills"));
                statistic.addDeaths(dailyFileDatabase.getInt("data." + key + ".deaths"));
                statistic.addWins(dailyFileDatabase.getInt("data." + key + ".wins"));
                statistic.addLoses(dailyFileDatabase.getInt("data." + key + ".loses"));
                statistic.addDestroyedBeds(dailyFileDatabase.getInt("data." + key + ".destroyedBeds"));
                statistic.addScore(dailyFileDatabase.getInt("data." + key + ".score"));
                statistic.setName(dailyFileDatabase.getString("data." + key + ".name"));
                dailyScores.put(UUID.fromString(key), statistic);
            }

            // TODO: Remove this after the event ended, this is a temporal solution
            // Load event stats
            for (String key : eventFileDatabase.getConfigurationSection("data").getKeys(false)) {
                PlayerStatistic statistic = new PlayerStatistic(UUID.fromString(key));
                statistic.addKills(eventFileDatabase.getInt("data." + key + ".kills"));
                statistic.addDeaths(eventFileDatabase.getInt("data." + key + ".deaths"));
                statistic.addWins(eventFileDatabase.getInt("data." + key + ".wins"));
                statistic.addLoses(eventFileDatabase.getInt("data." + key + ".loses"));
                statistic.addDestroyedBeds(eventFileDatabase.getInt("data." + key + ".destroyedBeds"));
                statistic.addScore(eventFileDatabase.getInt("data." + key + ".score"));
                statistic.setName(eventFileDatabase.getString("data." + key + ".name"));
                eventScores.put(UUID.fromString(key), statistic);
            }
        }

    }

    public List<LeaderboardEntry> getLeaderboard(int count) {
        List<LeaderboardEntry> entries = new ArrayList<>();

        allScores.entrySet().stream()
                .sorted((c1, c2) -> Comparator.<Integer>reverseOrder().compare(c1.getValue().getScore(), c2.getValue().getScore()))
                .limit(count)
                .forEach(entry -> entries.add(new org.screamingsandals.bedwars.statistics.LeaderboardEntry(Bukkit.getOfflinePlayer(entry.getKey()), entry.getValue().getScore(), entry.getValue().getName())));

        return entries;
    }

    public List<LeaderboardEntry> getLeaderboard(int count, ELeaderboardStatType stat, ELeaderboardKind kind) {
        List<LeaderboardEntry> entries = new ArrayList<>();

        Map<UUID, PlayerStatistic> scores;
        switch (kind) {
            case Daily: {
                scores = this.dailyScores;
                break;
            }
            case Season: {
                scores = this.seasonalScores;
                break;
            }
            case AllTime: {
                scores = this.allScores;
                break;
            }
            default: {
                return entries;
            }
        }

        switch (stat) {
            case Kills: {
                scores.entrySet().stream()
                        .sorted((c1, c2) -> Comparator.<Integer>reverseOrder().compare(c1.getValue().getKills(), c2.getValue().getKills()))
                        .limit(count)
                        .forEach(entry -> entries.add(new org.screamingsandals.bedwars.statistics.LeaderboardEntry(Bukkit.getOfflinePlayer(entry.getKey()), entry.getValue().getKills(), entry.getValue().getName())));
                break;
            }
            case Deaths: {
                scores.entrySet().stream()
                        .sorted((c1, c2) -> Comparator.<Integer>reverseOrder().compare(c1.getValue().getDeaths(), c2.getValue().getDeaths()))
                        .limit(count)
                        .forEach(entry -> entries.add(new org.screamingsandals.bedwars.statistics.LeaderboardEntry(Bukkit.getOfflinePlayer(entry.getKey()), entry.getValue().getDeaths(), entry.getValue().getName())));

                break;
            }
            case Wins: {
                scores.entrySet().stream()
                        .sorted((c1, c2) -> Comparator.<Integer>reverseOrder().compare(c1.getValue().getWins(), c2.getValue().getWins()))
                        .limit(count)
                        .forEach(entry -> entries.add(new org.screamingsandals.bedwars.statistics.LeaderboardEntry(Bukkit.getOfflinePlayer(entry.getKey()), entry.getValue().getWins(), entry.getValue().getName())));

                break;
            }
            case Loses: {
                scores.entrySet().stream()
                        .sorted((c1, c2) -> Comparator.<Integer>reverseOrder().compare(c1.getValue().getLoses(), c2.getValue().getLoses()))
                        .limit(count)
                        .forEach(entry -> entries.add(new org.screamingsandals.bedwars.statistics.LeaderboardEntry(Bukkit.getOfflinePlayer(entry.getKey()), entry.getValue().getLoses(), entry.getValue().getName())));

                break;
            }
            case DestroyedBeds: {
                scores.entrySet().stream()
                        .sorted((c1, c2) -> Comparator.<Integer>reverseOrder().compare(c1.getValue().getDestroyedBeds(), c2.getValue().getDestroyedBeds()))
                        .limit(count)
                        .forEach(entry -> entries.add(new org.screamingsandals.bedwars.statistics.LeaderboardEntry(Bukkit.getOfflinePlayer(entry.getKey()), entry.getValue().getDestroyedBeds(), entry.getValue().getName())));

                break;
            }
            case Score:
            default: {
                scores.entrySet().stream()
                        .sorted((c1, c2) -> Comparator.<Integer>reverseOrder().compare(c1.getValue().getScore(), c2.getValue().getScore()))
                        .limit(count)
                        .forEach(entry -> entries.add(new org.screamingsandals.bedwars.statistics.LeaderboardEntry(Bukkit.getOfflinePlayer(entry.getKey()), entry.getValue().getScore(), entry.getValue().getName())));

                break;
            }
        }

        return entries;
    }

    public LeaderboardEntry getLeaderboardEntry(int index) {
        return allScores.entrySet().stream()
                .sorted((c1, c2) -> Comparator.<Integer>reverseOrder().compare(c1.getValue().getScore(), c2.getValue().getScore()))
                .skip(index)
                .findFirst()
                .map(entry -> new org.screamingsandals.bedwars.statistics.LeaderboardEntry(Bukkit.getOfflinePlayer(entry.getKey()), entry.getValue().getScore(), entry.getValue().getName()))
                .orElse(null);
    }

    private void loadYml(File ymlFile, File seasonalYmlFile, File dailyYmlFile, File eventYmlFile) {
        try {
            Main.getInstance().getLogger().info("Loading statistics from YAML-File ...");

            YamlConfiguration config;

            this.databaseFile = ymlFile;
            this.seasonalDatabaseFile = seasonalYmlFile;
            this.dailyDatabaseFile = dailyYmlFile;

            // All-time stats
            if (!ymlFile.exists()) {
                ymlFile.getParentFile().mkdirs();
                ymlFile.createNewFile();

                config = new YamlConfiguration();
                config.createSection("data");
                config.save(ymlFile);
            } else {
                config = YamlConfiguration.loadConfiguration(ymlFile);
            }
            this.fileDatabase = config;

            // Seasonal stats
            if (!seasonalYmlFile.exists()) {
                seasonalYmlFile.getParentFile().mkdirs();
                seasonalYmlFile.createNewFile();

                config = new YamlConfiguration();
                config.createSection("data");
                config.save(seasonalYmlFile);
            } else {
                config = YamlConfiguration.loadConfiguration(seasonalYmlFile);
            }
            this.seasonalFileDatabase = config;

            // Daily stats
            if (!dailyYmlFile.exists()) {
                dailyYmlFile.getParentFile().mkdirs();
                dailyYmlFile.createNewFile();

                config = new YamlConfiguration();
                config.createSection("data");
                config.save(dailyYmlFile);
            } else {
                config = YamlConfiguration.loadConfiguration(dailyYmlFile);
            }
            this.dailyFileDatabase = config;

            // TODO: Remove this after the event ended, this is a temporal solution
            // Event stats
            if (!eventYmlFile.exists()) {
                eventYmlFile.getParentFile().mkdirs();
                eventYmlFile.createNewFile();

                config = new YamlConfiguration();
                config.createSection("data");
                config.save(eventYmlFile);
            } else {
                config = YamlConfiguration.loadConfiguration(eventYmlFile);
            }
            this.eventFileDatabase = config;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void storeDatabaseStatistic(PlayerStatistic allTimeStatistics, PlayerStatistic seasonalStatistics, PlayerStatistic dailyStatistics, PlayerStatistic eventStatistics) {
        try (Connection connection = Main.getDatabaseManager().getConnection()) {
            connection.setAutoCommit(false);

            // All-time stats
            try (PreparedStatement preparedStatement = connection
                    .prepareStatement(Main.getDatabaseManager().getWriteObjectSql())) {
                if (allTimeStatistics != null) {
                    preparedStatement.setString(1, allTimeStatistics.getId().toString());
                    preparedStatement.setString(2, allTimeStatistics.getName());
                    preparedStatement.setInt(3, allTimeStatistics.getDeaths());
                    preparedStatement.setInt(4, allTimeStatistics.getDestroyedBeds());
                    preparedStatement.setInt(5, allTimeStatistics.getKills());
                    preparedStatement.setInt(6, allTimeStatistics.getLoses());
                    preparedStatement.setInt(7, allTimeStatistics.getScore());
                    preparedStatement.setInt(8, allTimeStatistics.getWins());
                    preparedStatement.executeUpdate();
                } else
                    Main.getInstance().getLogger().warning("Tried to store null all-time statistics!");
            }
            catch (Exception ex) {
                Main.getInstance().getLogger().warning("Couldn't store all-time statistic data");
                Main.getInstance().getLogger().severe(ex.getMessage());
            }

            // Seasonal stats
            try (PreparedStatement preparedStatement = connection
                    .prepareStatement(Main.getDatabaseManager().getSeasonalWriteObjectSql()))
            {
                if (seasonalStatistics != null) {
                    preparedStatement.setString(1, seasonalStatistics.getId().toString());
                    preparedStatement.setString(2, seasonalStatistics.getName());
                    preparedStatement.setInt(3, seasonalStatistics.getDeaths());
                    preparedStatement.setInt(4, seasonalStatistics.getDestroyedBeds());
                    preparedStatement.setInt(5, seasonalStatistics.getKills());
                    preparedStatement.setInt(6, seasonalStatistics.getLoses());
                    preparedStatement.setInt(7, seasonalStatistics.getScore());
                    preparedStatement.setInt(8, seasonalStatistics.getWins());
                    preparedStatement.executeUpdate();
                }
                else
                    Main.getInstance().getLogger().warning("Tried to store null seasonal statistics!");
            }
            catch (Exception ex) {
                Main.getInstance().getLogger().warning("Couldn't store seasonal statistic data");
                Main.getInstance().getLogger().severe(ex.getMessage());
            }

            // Daily stats
            try (PreparedStatement preparedStatement = connection
                    .prepareStatement(Main.getDatabaseManager().getDailyWriteObjectSql()))
            {
                if (dailyStatistics != null) {
                    preparedStatement.setString(1, dailyStatistics.getId().toString());
                    preparedStatement.setString(2, dailyStatistics.getName());
                    preparedStatement.setInt(3, dailyStatistics.getDeaths());
                    preparedStatement.setInt(4, dailyStatistics.getDestroyedBeds());
                    preparedStatement.setInt(5, dailyStatistics.getKills());
                    preparedStatement.setInt(6, dailyStatistics.getLoses());
                    preparedStatement.setInt(7, dailyStatistics.getScore());
                    preparedStatement.setInt(8, dailyStatistics.getWins());
                    preparedStatement.executeUpdate();
                }
                else
                    Main.getInstance().getLogger().warning("Tried to store null daily statistics!");
            }
            catch (Exception ex) {
                Main.getInstance().getLogger().warning("Couldn't store daily statistic data");
                Main.getInstance().getLogger().severe(ex.getMessage());
            }

            // TODO: Remove this after the event ended, this is a temporal solution
            // Event stats
            try (PreparedStatement preparedStatement = connection
                    .prepareStatement(Main.getDatabaseManager().getEventWriteObjectSql()))
            {
                if (eventStatistics != null) {
                    preparedStatement.setString(1, eventStatistics.getId().toString());
                    preparedStatement.setString(2, eventStatistics.getName());
                    preparedStatement.setInt(3, eventStatistics.getDeaths());
                    preparedStatement.setInt(4, eventStatistics.getDestroyedBeds());
                    preparedStatement.setInt(5, eventStatistics.getKills());
                    preparedStatement.setInt(6, eventStatistics.getLoses());
                    preparedStatement.setInt(7, eventStatistics.getScore());
                    preparedStatement.setInt(8, eventStatistics.getWins());
                    preparedStatement.executeUpdate();
                }
                else
                    Main.getInstance().getLogger().warning("Tried to store null event statistics!");
            }
            catch (Exception ex) {
                Main.getInstance().getLogger().warning("Couldn't store event statistic data");
                Main.getInstance().getLogger().severe(ex.getMessage());
            }

            connection.commit();
        } catch (SQLException e) {
            Main.getInstance().getLogger().warning("Couldn't store statistic data for player.");
            Main.getInstance().getLogger().severe(e.getMessage());
        }

    }

    public void storeStatistic(PlayerStatistic statistic, PlayerStatistic seasonalStatistic, PlayerStatistic dailyStatistic, PlayerStatistic eventStatistic) {
        BedwarsSavePlayerStatisticEvent savePlayerStatisticEvent = new BedwarsSavePlayerStatisticEvent(statistic);
        Main.getInstance().getServer().getPluginManager().callEvent(savePlayerStatisticEvent);

        if (savePlayerStatisticEvent.isCancelled()) {
            return;
        }

        if (Main.getConfigurator().config.getString("statistics.type").equalsIgnoreCase("database")) {
            this.storeDatabaseStatistic(statistic, seasonalStatistic, dailyStatistic, eventStatistic);
        } else {
            this.storeYamlStatistic(statistic, seasonalStatistic, dailyStatistic, eventStatistic);
        }
    }

    private synchronized void storeYamlStatistic(PlayerStatistic allTimeStatistics, PlayerStatistic seasonalStatistics, PlayerStatistic dailyStatistics, PlayerStatistic eventStatistics) {
        // Store all-time stats
        if (allTimeStatistics != null) {
            this.fileDatabase.set("data." + allTimeStatistics.getId().toString(), null);
            this.fileDatabase.createSection("data." + allTimeStatistics.getId().toString(), allTimeStatistics.serialize());
            try {
                this.fileDatabase.save(this.databaseFile);
            } catch (Exception ex) {
                Main.getInstance().getLogger().warning("Couldn't store statistic data for player with uuid: " + allTimeStatistics.getId().toString());
                Main.getInstance().getLogger().severe(ex.getMessage());
            }
        }
        else
            Main.getInstance().getLogger().warning("Tried to store null all-time statistics!");

        // Store seasonal stats
        if (seasonalStatistics != null) {
            this.seasonalFileDatabase.set("data." + seasonalStatistics.getId().toString(), null);
            this.seasonalFileDatabase.createSection("data." + seasonalStatistics.getId().toString(), seasonalStatistics.serialize());
            try {
                this.seasonalFileDatabase.save(this.seasonalDatabaseFile);
            } catch (Exception ex) {
                Main.getInstance().getLogger().warning("Couldn't store seasonal statistic data for player with uuid: " + seasonalStatistics.getId().toString());
                Main.getInstance().getLogger().severe(ex.getMessage());
            }
        }
        else
            Main.getInstance().getLogger().warning("Tried to store null seasonal statistics!");

        // Store daily stats
        if (dailyStatistics != null) {
            this.dailyFileDatabase.set("data." + dailyStatistics.getId().toString(), null);
            this.dailyFileDatabase.createSection("data." + dailyStatistics.getId().toString(), dailyStatistics.serialize());
            try {
                this.dailyFileDatabase.save(this.dailyDatabaseFile);
            } catch (Exception ex) {
                Main.getInstance().getLogger().warning("Couldn't store daily statistic data for player with uuid: " + dailyStatistics.getId().toString());
                Main.getInstance().getLogger().severe(ex.getMessage());
            }
        }
        else
            Main.getInstance().getLogger().warning("Tried to store null daily statistics!");

        // TODO: Remove this after the event ended, this is a temporal solution
        // Event stats
        if (eventStatistics != null) {
            this.eventFileDatabase.set("data." + eventStatistics.getId().toString(), null);
            this.eventFileDatabase.createSection("data." + eventStatistics.getId().toString(), eventStatistics.serialize());
            try {
                this.eventFileDatabase.save(this.eventDatabaseFile);
            } catch (Exception ex) {
                Main.getInstance().getLogger().warning("Couldn't store event statistic data for player with uuid: " + eventStatistics.getId().toString());
                Main.getInstance().getLogger().severe(ex.getMessage());
            }
        }
        else
            Main.getInstance().getLogger().warning("Tried to store null event statistics!");
    }

    public void unloadStatistic(OfflinePlayer player) {
        if (Main.getConfigurator().config.getString("statistics.type").equalsIgnoreCase("database")) {
            //this.playerStatistic.remove(player.getUniqueId());
        }
    }

    public void addStatistic(UUID playerId) {
        if (allScores.containsKey(playerId)) {
            return;
        }

        allScores.put(playerId, new PlayerStatistic(playerId));
        if (Main.getLeaderboardHolograms() != null) {
            Main.getLeaderboardHolograms().updateAllTimeEntries(true);
        }
    }

    public void updateAllTImeScore(PlayerStatistic playerStatistic) {
        allScores.put(playerStatistic.getId(), playerStatistic);
        if (Main.getLeaderboardHolograms() != null) {
            Main.getLeaderboardHolograms().updateAllTimeEntries(true);
        }
    }

    public void addSeasonalStatistic(UUID playerId) {
        if (seasonalScores.containsKey(playerId)) {
            return;
        }

        seasonalScores.put(playerId, new PlayerStatistic(playerId));
        if (Main.getLeaderboardHolograms() != null) {
            Main.getLeaderboardHolograms().updateSeasonEntries(true);
        }
    }

    public void updateSeasonalScore(PlayerStatistic playerStatistic) {
        seasonalScores.put(playerStatistic.getId(), playerStatistic);
        if (Main.getLeaderboardHolograms() != null) {
            Main.getLeaderboardHolograms().updateSeasonEntries(true);
        }
    }

    public void addDailyStatistic(UUID playerId) {
        if (dailyScores.containsKey(playerId)) {
            return;
        }

        dailyScores.put(playerId, new PlayerStatistic(playerId));
    }

    public void updateDailyScore(PlayerStatistic playerStatistic) {
        dailyScores.put(playerStatistic.getId(), playerStatistic);
    }

    // TODO: Remove this after the event ended, this is a temporal solution
    public void addEventStatistic(UUID playerId) {
        if (eventScores.containsKey(playerId)) {
            return;
        }

        eventScores.put(playerId, new PlayerStatistic(playerId));
    }

    // TODO: Remove this after the event ended, this is a temporal solution
    public void updateEventScore(PlayerStatistic playerStatistic) {
        eventScores.put(playerStatistic.getId(), playerStatistic);
    }

    public void resetSeasonalScores() {
        seasonalScores.clear();

        if (seasonalFileDatabase != null) {
            seasonalFileDatabase.set("data", null); // remove old section
            seasonalFileDatabase.createSection("data");
            // Create new data for online players to prevent issues
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerStatistic statistic = new PlayerStatistic(player.getUniqueId());
                this.seasonalFileDatabase.createSection("data." + statistic.getId().toString(), statistic.serialize());
                seasonalScores.put(player.getUniqueId(), statistic);
            }
            try {
                seasonalFileDatabase.save(this.seasonalDatabaseFile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            try (Connection connection = Main.getDatabaseManager().getConnection()) {
                connection.setAutoCommit(false);

                PreparedStatement preparedStatement = connection
                        .prepareStatement(Main.getDatabaseManager().getSeasonalResetSql());

                preparedStatement.executeUpdate();

                connection.commit();
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        Main.getLeaderboardHolograms().updateSeasonEntries(true);
    }

    public void resetDailyScores() {
        dailyScores.clear();

        if (dailyFileDatabase != null) {
            dailyFileDatabase.set("data", null); // remove old section
            dailyFileDatabase.createSection("data");
            // Create new data for online players to prevent issues
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerStatistic statistic = new PlayerStatistic(player.getUniqueId());
                this.dailyFileDatabase.createSection("data." + statistic.getId().toString(), statistic.serialize());
                dailyScores.put(player.getUniqueId(), statistic);
            }
            try {
                dailyFileDatabase.save(this.dailyDatabaseFile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            try (Connection connection = Main.getDatabaseManager().getConnection()) {
                connection.setAutoCommit(false);

                PreparedStatement preparedStatement = connection
                        .prepareStatement(Main.getDatabaseManager().getDailyResetSql());

                preparedStatement.executeUpdate();

                connection.commit();
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
