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

package org.screamingsandals.bedwars.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.screamingsandals.bedwars.Main;
import org.screamingsandals.bedwars.commands.AdminCommand;
import org.screamingsandals.bedwars.game.Game;
import org.screamingsandals.bedwars.lib.signmanager.SignBlock;
import org.screamingsandals.bedwars.lib.signmanager.SignOwner;

import static org.screamingsandals.bedwars.lib.lang.I.*;

public class BedWarsSignOwner implements SignOwner {

	@Override
	public boolean isNameExists(String name) {
		return Main.isGameExists(name) || name.equalsIgnoreCase("autojoin") || name.equalsIgnoreCase("leave");
	}

	@Override
	public void updateSign(SignBlock sign) {
		String name = sign.getName();
		if (Main.isGameExists(name)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					Main.getGame(name).updateSigns();
				}
			}.runTask(Main.getInstance());

		} else if ("leave".equalsIgnoreCase(name)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					updateLeaveSign(sign);
				}
			}.runTask(Main.getInstance());
		} else if ("autojoin".equalsIgnoreCase(name)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateAutoJoinSign(sign);
                }
            }.runTask(Main.getInstance());
        }
	}

	private void updateLeaveSign(SignBlock sign) {
		List<String> texts = Main.getConfigurator().config.getStringList("sign.leave-lines").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());

		Block block = sign.getLocation().getBlock();
		if (block.getState() instanceof Sign) {
			Sign state = (Sign) block.getState();

			for (int i = 0; i < texts.size(); i++) {
				String text = texts.get(i);
				state.setLine(i, text);
			}

			state.update();
		}
	}

    private void updateAutoJoinSign(SignBlock sign) {
        List<String> texts = Main.getConfigurator().config.getStringList("sign.auto-join-lines").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());

        Block block = sign.getLocation().getBlock();
        if (block.getState() instanceof Sign) {
            Sign state = (Sign) block.getState();

            for (int i = 0; i < texts.size(); i++) {
                String text = texts.get(i);
                state.setLine(i, text);
            }

            state.update();
        }
    }

	@Override
	public List<String> getSignPrefixes() {
		return Arrays.asList("[bedwars]", "[bwgame]");
	}

	@Override
	public void onClick(Player player, SignBlock sign) {
		if (sign.getName().equalsIgnoreCase("leave")) {
            if (Main.isPlayerInGame(player)) {
                Main.getPlayerGameProfile(player).changeGame(null);
            }
        } else if (sign.getName().equalsIgnoreCase("autojoin")) {
            if (Main.isPlayerInGame(player)) {
                return;
            }
            Bukkit.dispatchCommand(player, "bw autojoin");
        } else {
            Game game = Main.getGame(sign.getName());
            if (game != null) {
                game.joinToGame(player);
            } else {
                m("sign_game_not_exists").send(player);
            }
        }
	}

	@Override
	public List<String> getSignCreationPermissions() {
		return AdminCommand.ADMIN_PERMISSION;
	}

	@Override
	public String returnTranslate(String key) {
		return i18n(key);
	}

}
