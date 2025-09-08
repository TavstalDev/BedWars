package org.screamingsandals.bedwars.holograms;

import org.bukkit.Location;
import org.screamingsandals.bedwars.api.statistics.ELeaderboardKind;
import org.screamingsandals.bedwars.api.statistics.ELeaderboardStatType;

import java.util.Objects;
import java.util.UUID;

public class HologramLocation extends Location {
    public UUID id;
    public ELeaderboardKind leaderboardKind;
    public ELeaderboardStatType leaderboardType;

    public HologramLocation(UUID id, Location loc, ELeaderboardStatType type, ELeaderboardKind kind) {
        super(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        this.id = id;
        this.leaderboardType = type;
        this.leaderboardKind = kind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        // Check the parent class fields first using the super.equals() call
        if (!super.equals(o)) return false;

        HologramLocation that = (HologramLocation) o;

        // Then, check the fields unique to HologramLocation
        return Objects.equals(id, that.id) &&
                leaderboardKind == that.leaderboardKind &&
                leaderboardType == that.leaderboardType &&
                getWorld() == that.getWorld() &&
                getX() == that.getX() &&
                getY() == that.getY() &&
                getZ() == that.getZ() &&
                getYaw() == that.getYaw() &&
                getPitch() == that.getPitch();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, leaderboardKind, leaderboardType);
    }
}
