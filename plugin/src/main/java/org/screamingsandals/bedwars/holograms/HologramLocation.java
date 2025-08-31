package org.screamingsandals.bedwars.holograms;

import org.bukkit.Location;

public class HologramLocation extends Location {
    public ELeaderboardType leaderboardType;

    public HologramLocation(Location loc, ELeaderboardType type) {
        super(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        this.leaderboardType = type;
    }
}
