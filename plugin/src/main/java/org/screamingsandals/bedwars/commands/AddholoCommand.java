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

package org.screamingsandals.bedwars.commands;

import org.screamingsandals.bedwars.Main;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.screamingsandals.bedwars.holograms.ELeaderboardType;

import java.util.Arrays;
import java.util.List;

import static org.screamingsandals.bedwars.lib.lang.I18n.i18n;

public class AddholoCommand extends BaseCommand {

    public AddholoCommand() {
        super("addholo", ADMIN_PERMISSION, false, false);
    }

    @Override
    public boolean execute(CommandSender sender, List<String> args) {
        Player player = (Player) sender;
        if (!Main.isHologramsEnabled()) {
            player.sendMessage(i18n("holo_not_enabled"));
        } else {
            if (args.size() >= 1 && args.get(0).equalsIgnoreCase("leaderboard")) {

                if (args.size() < 2) {
                    player.sendMessage(i18n("leaderboard_type_missing"));
                    return true;
                }

                switch (args.get(1).toLowerCase()) {
                    case "w":
                    case "win":
                    case "wins": {
                        Main.getLeaderboardHolograms().addHologramLocation(player.getEyeLocation(), ELeaderboardType.Wins);
                        break;
                    }
                    case "l":
                    case "loss":
                    case "lose":
                    case "loses": {
                        Main.getLeaderboardHolograms().addHologramLocation(player.getEyeLocation(), ELeaderboardType.Loses);
                        break;
                    }
                    case "k":
                    case "kill":
                    case "kills": {
                        Main.getLeaderboardHolograms().addHologramLocation(player.getEyeLocation(), ELeaderboardType.Kills);
                        break;
                    }
                    case "d":
                    case "death":
                    case "deaths": {
                        Main.getLeaderboardHolograms().addHologramLocation(player.getEyeLocation(), ELeaderboardType.Deaths);
                        break;
                    }
                    case "beds":
                    case "beds_destroyed": {
                        Main.getLeaderboardHolograms().addHologramLocation(player.getEyeLocation(), ELeaderboardType.DestroyedBeds);
                        break;
                    }
                    case "score": {
                        Main.getLeaderboardHolograms().addHologramLocation(player.getEyeLocation(), ELeaderboardType.Score);
                        break;
                    }
                    default: {
                        player.sendMessage(i18n("leaderboard_type_invalid"));
                        return true;
                    }
                }

                player.sendMessage(i18n("leaderboard_holo_added"));
            } else {
                Main.getHologramInteraction().addHologramLocation(player.getEyeLocation());
                Main.getHologramInteraction().updateHolograms();
                player.sendMessage(i18n("holo_added"));
            }
        }
        return true;
    }

    @Override
    public void completeTab(List<String> completion, CommandSender sender, List<String> args) {
        if (args.size() == 1) {
            completion.addAll(Arrays.asList("leaderboard", "stats"));
        }
        else if (args.size() == 2 && args.get(0).equalsIgnoreCase("leaderboard")) {
            completion.addAll(Arrays.asList("wins", "loses", "kills", "deaths", "beds_destroyed", "score"));
        }
    }

}
