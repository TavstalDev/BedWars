package org.screamingsandals.bedwars.utils;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public class CheatUtil {
    public static boolean isLookingAtTargetBlock(Player player, double maxDistance, Block targetBlock) {
        RayTraceResult result = player.rayTraceBlocks(maxDistance);
        if (result == null) {
            return false;
        }

        Block hitBlock = result.getHitBlock();
        if (hitBlock == null) {
            return false;
        }

        return hitBlock.getType() == targetBlock.getType();
    }
}
