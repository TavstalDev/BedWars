package org.screamingsandals.bedwars.holograms;

import org.bukkit.Location;
import org.screamingsandals.bedwars.api.statistics.ELeaderboardKind;
import org.screamingsandals.bedwars.api.statistics.ELeaderboardStatType;

public class HologramLocation extends Location {
    public ELeaderboardKind leaderboardKind;
    public ELeaderboardStatType leaderboardType;

    public HologramLocation(Location loc, ELeaderboardStatType type, ELeaderboardKind kind) {
        super(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        this.leaderboardType = type;
        this.leaderboardKind = kind;
    }
}
