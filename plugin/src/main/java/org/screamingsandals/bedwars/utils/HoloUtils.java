package org.screamingsandals.bedwars.utils;

import com.maximde.hologramlib.hologram.RenderMode;
import com.maximde.hologramlib.hologram.TextHologram;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.screamingsandals.bedwars.Main;

import java.util.UUID;

public class HoloUtils {

    public static TextHologram createHologram(Location location, RenderMode renderMode, double viewRange) {
        TextHologram hologram = new TextHologram(UUID.randomUUID().toString(), renderMode);
        hologram.setSeeThroughBlocks(true);
        hologram.setViewRange(viewRange);
        hologram.setBackgroundColor(ColorChanger.toARGB(64, 0, 0, 0));
        hologram.setBillboard(Display.Billboard.VERTICAL);
        Main.getHologramLibManager().spawn(hologram, location);
        return hologram;
    }
}
