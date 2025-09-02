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

package org.screamingsandals.bedwars.holograms;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.screamingsandals.bedwars.Main;
import org.screamingsandals.bedwars.api.statistics.ELeaderboardKind;
import org.screamingsandals.bedwars.api.statistics.ELeaderboardStatType;
import org.screamingsandals.bedwars.api.statistics.LeaderboardEntry;
import org.screamingsandals.bedwars.commands.BaseCommand;
import org.screamingsandals.bedwars.lib.nms.holograms.Hologram;
import org.screamingsandals.bedwars.lib.nms.holograms.TouchHandler;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static org.screamingsandals.bedwars.lib.lang.I.i18n;
import static org.screamingsandals.bedwars.lib.lang.I.i18nonly;

public class LeaderboardHolograms implements TouchHandler {
    private ArrayList<HologramLocation> hologramLocations;
    private Map<HologramLocation, Hologram> holograms;
    private Map<ELeaderboardKind, Map<ELeaderboardStatType, List<LeaderboardEntry>>> entries;

    public void addHologramLocation(Location eyeLocation, ELeaderboardStatType type, ELeaderboardKind kind) {
        this.hologramLocations.add(new HologramLocation(eyeLocation.subtract(0, 3, 0), type, kind));
        this.updateHologramDatabase();

        if (entries == null) {
            updateEntries();
        } else {
            updateHolograms();
        }
    }

    public void updateEntries() {
        if (this.hologramLocations == null || this.hologramLocations.isEmpty()) {
            return;
        }

        if (this.entries == null) {
            this.entries = new HashMap<>();
        }

        this.entries.clear(); // Make sure to clear previous entries
        updateAllTimeEntries(false);
        updateSeasonEntries(false);
        updateHolograms();
    }

    public void updateAllTimeEntries(boolean updateHolograms) {
        Map<ELeaderboardStatType, List<LeaderboardEntry>> map = new HashMap<>();
        map.put(ELeaderboardStatType.Score, Main.getPlayerStatisticsManager().getLeaderboard(Main.getConfigurator().config.getInt("holograms.leaderboard.size"), ELeaderboardStatType.Score, ELeaderboardKind.AllTime));
        map.put(ELeaderboardStatType.Kills, Main.getPlayerStatisticsManager().getLeaderboard(Main.getConfigurator().config.getInt("holograms.leaderboard.killSize"), ELeaderboardStatType.Kills, ELeaderboardKind.AllTime));
        map.put(ELeaderboardStatType.Deaths, Main.getPlayerStatisticsManager().getLeaderboard(Main.getConfigurator().config.getInt("holograms.leaderboard.deathSize"), ELeaderboardStatType.Deaths, ELeaderboardKind.AllTime));
        map.put(ELeaderboardStatType.Wins, Main.getPlayerStatisticsManager().getLeaderboard(Main.getConfigurator().config.getInt("holograms.leaderboard.winSize"), ELeaderboardStatType.Wins, ELeaderboardKind.AllTime));
        map.put(ELeaderboardStatType.Loses, Main.getPlayerStatisticsManager().getLeaderboard(Main.getConfigurator().config.getInt("holograms.leaderboard.loseSize"), ELeaderboardStatType.Loses, ELeaderboardKind.AllTime));
        map.put(ELeaderboardStatType.DestroyedBeds, Main.getPlayerStatisticsManager().getLeaderboard(Main.getConfigurator().config.getInt("holograms.leaderboard.destroyedBedSize"), ELeaderboardStatType.DestroyedBeds, ELeaderboardKind.AllTime));
        this.entries.put(ELeaderboardKind.AllTime, map);

        if (updateHolograms) {
            updateHolograms();
        }
    }

    public void updateSeasonEntries(boolean updateHolograms) {
        Map<ELeaderboardStatType, List<LeaderboardEntry>> map = new HashMap<>();
        map.put(ELeaderboardStatType.Score, Main.getPlayerStatisticsManager().getLeaderboard(Main.getConfigurator().config.getInt("holograms.leaderboard.size"), ELeaderboardStatType.Score, ELeaderboardKind.Season));
        map.put(ELeaderboardStatType.Kills, Main.getPlayerStatisticsManager().getLeaderboard(Main.getConfigurator().config.getInt("holograms.leaderboard.killSize"), ELeaderboardStatType.Kills, ELeaderboardKind.Season));
        map.put(ELeaderboardStatType.Deaths, Main.getPlayerStatisticsManager().getLeaderboard(Main.getConfigurator().config.getInt("holograms.leaderboard.deathSize"), ELeaderboardStatType.Deaths, ELeaderboardKind.Season));
        map.put(ELeaderboardStatType.Wins, Main.getPlayerStatisticsManager().getLeaderboard(Main.getConfigurator().config.getInt("holograms.leaderboard.winSize"), ELeaderboardStatType.Wins, ELeaderboardKind.Season));
        map.put(ELeaderboardStatType.Loses, Main.getPlayerStatisticsManager().getLeaderboard(Main.getConfigurator().config.getInt("holograms.leaderboard.loseSize"), ELeaderboardStatType.Loses, ELeaderboardKind.Season));
        map.put(ELeaderboardStatType.DestroyedBeds, Main.getPlayerStatisticsManager().getLeaderboard(Main.getConfigurator().config.getInt("holograms.leaderboard.destroyedBedSize"), ELeaderboardStatType.DestroyedBeds, ELeaderboardKind.Season));
        this.entries.put(ELeaderboardKind.Season, map);

        if (updateHolograms) {
            updateHolograms();
        }
    }

    public void loadHolograms() {
        if (!Main.isHologramsEnabled()) {
            return;
        }

        if (this.hologramLocations != null || this.holograms != null) {
            // first unload all holograms
            this.unloadHolograms();
        }

        this.holograms = new HashMap<>();
        this.hologramLocations = new ArrayList<>();

        File file = new File(Main.getInstance().getDataFolder(), "holodb_leaderboard.yml");
        if (file.exists()) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                List<Map<?, ?>> serializedLocations = (List<Map<?, ?>>) config.getList("locations");

                if (serializedLocations == null) {
                    return;
                }

                hologramLocations.clear(); // Clear existing locations before loading new ones

                for (Map<?, ?> locationData : serializedLocations) {
                    String worldName = (String) locationData.get("world");
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        // World doesn't exist, skip this location
                        continue;
                    }

                    double x = (double) locationData.get("x");
                    double y = (double) locationData.get("y");
                    double z = (double) locationData.get("z");
                    float pitch = ((Double) locationData.get("pitch")).floatValue();
                    float yaw = ((Double) locationData.get("yaw")).floatValue();
                    ELeaderboardStatType leaderboardType = ELeaderboardStatType.valueOf((String) locationData.get("leaderboardType"));
                    ELeaderboardKind kind = ELeaderboardKind.valueOf((String)locationData.get("leaderboardKind"));

                    Location loc = new Location(world, x, y, z, yaw, pitch);
                    HologramLocation hologramLoc = new HologramLocation(loc, leaderboardType, kind);
                    hologramLocations.add(hologramLoc);
                }
            } catch (Throwable t) {
                Main.getInstance().getLogger().severe("Failed to load holograms from " + file.getAbsolutePath());
                t.printStackTrace();
            }
        }

        if (this.hologramLocations.isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTask(Main.getInstance(), this::updateEntries);
    }

    private void updateHologramDatabase() {
        try {
            File file = new File(Main.getInstance().getDataFolder(), "holodb_leaderboard.yml");
            YamlConfiguration config = new YamlConfiguration();

            if (!file.exists()) {
                file.createNewFile();
            }

            List<Map<String, Object>> serializedLocations = new ArrayList<>();
            for (HologramLocation loc : hologramLocations) {
                Map<String, Object> locationData = new HashMap<>();
                locationData.put("world", loc.getWorld().getName());
                locationData.put("x", loc.getX());
                locationData.put("y", loc.getY());
                locationData.put("z", loc.getZ());
                locationData.put("pitch", loc.getPitch());
                locationData.put("yaw", loc.getYaw());
                locationData.put("leaderboardType", loc.leaderboardType.name()); // Save the enum as a string
                locationData.put("leaderboardKind", loc.leaderboardKind.name());
                serializedLocations.add(locationData);
            }

            config.set("locations", serializedLocations);
            config.save(file);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void unloadHolograms() {
        if (Main.isHologramsEnabled()) {
            for (Hologram holo : holograms.values()) {
                holo.destroy();
            }
            holograms.clear();
        }
    }

    public void addViewer(Player player) {
        holograms.values().forEach(hologram -> {
            if (!hologram.getViewers().contains(player)) {
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> hologram.addViewer(player), 10L);
            }
        });
    }

    private void updateHolograms() {
        hologramLocations.forEach(location -> {
            if (!holograms.containsKey(location)) {
                holograms.put(location, Main.getHologramManager().spawnHologramTouchable(location));
                holograms.get(location).addHandler(this);
            }
            updateHologram(location.leaderboardType, location.leaderboardKind, holograms.get(location));
        });
        Bukkit.getOnlinePlayers().forEach(this::addViewer);
    }

    private void updateHologram(final ELeaderboardStatType type, final ELeaderboardKind kind, final Hologram holo) {
        List<String> lines = new ArrayList<>();

        lines.add(ChatColor.translateAlternateColorCodes('&', Main.getConfigurator().config.getString("holograms.leaderboard.headTopWrapper")));
        // Daily is not supported
        String title = kind == ELeaderboardKind.Season ? Main.getConfigurator().config.getString("holograms.leaderboard.seasonTitle") : Main.getConfigurator().config.getString("holograms.leaderboard.allTimeTitle");
        if (title != null && !title.isEmpty())
            lines.add(ChatColor.translateAlternateColorCodes('&', title));

        switch (type)
        {
            case Wins: {
                lines.add(ChatColor.translateAlternateColorCodes('&', Main.getConfigurator().config.getString("holograms.leaderboard.winHeadline")));
                break;
            }
            case Loses: {
                lines.add(ChatColor.translateAlternateColorCodes('&', Main.getConfigurator().config.getString("holograms.leaderboard.loseHeadline")));
                break;
            }
            case Kills: {
                lines.add(ChatColor.translateAlternateColorCodes('&', Main.getConfigurator().config.getString("holograms.leaderboard.killHeadline")));
                break;
            }
            case Deaths: {
                lines.add(ChatColor.translateAlternateColorCodes('&', Main.getConfigurator().config.getString("holograms.leaderboard.deathHeadline")));
                break;
            }
            case DestroyedBeds: {
                lines.add(ChatColor.translateAlternateColorCodes('&', Main.getConfigurator().config.getString("holograms.leaderboard.destroyedBedHeadline")));
                break;
            }
            case Score: {
                lines.add(ChatColor.translateAlternateColorCodes('&', Main.getConfigurator().config.getString("holograms.leaderboard.headline")));
                break;
            }
        }
        lines.add(ChatColor.translateAlternateColorCodes('&', Main.getConfigurator().config.getString("holograms.leaderboard.headBottomWrapper")));

        String line = ChatColor.translateAlternateColorCodes('&', Main.getConfigurator().config.getString("holograms.leaderboard.format"));

        if (entries == null || entries.isEmpty()) {
            lines.add(i18nonly("leaderboard_no_scores"));
        } else {
            AtomicInteger l = new AtomicInteger(1);
            entries.get(kind).get(type).forEach(leaderboardEntry -> {
                lines.add(line.replace("%name%", leaderboardEntry.getPlayer().getName() != null ? leaderboardEntry.getPlayer().getName() : (leaderboardEntry.getLatestKnownName() != null ? leaderboardEntry.getLatestKnownName() : leaderboardEntry.getPlayer().getUniqueId().toString())).replace("%score%", Integer.toString(leaderboardEntry.getTotalScore())).replace("%order%", Integer.toString(l.getAndIncrement())));
            });
        }

        for (int i = 0; i < lines.size(); i++) {
            holo.setLine(i, lines.get(i));
        }
    }

    @Override
    public void handle(Player player, Hologram hologram) {
        if (!player.hasMetadata("bw-remove-holo") || (!player.isOp() && !BaseCommand.hasPermission(player, BaseCommand.ADMIN_PERMISSION, false))) {
            return;
        }

        player.removeMetadata("bw-remove-holo", Main.getInstance());
        Main.getInstance().getServer().getScheduler().runTask(Main.getInstance(), () -> {
            hologram.destroy();
            new ArrayList<>(hologramLocations).forEach((location) -> {
                if (hologram.getLocation().getX() == location.getX() && hologram.getLocation().getY() == location.getY()
                        && hologram.getLocation().getZ() == location.getZ()) {
                    hologramLocations.remove(location);
                    holograms.remove(location);
                    updateHologramDatabase();
                }
            });
            player.sendMessage(i18n("holo_removed"));
        });
    }
}
