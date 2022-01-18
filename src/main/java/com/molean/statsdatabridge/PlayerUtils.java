package com.molean.statsdatabridge;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class PlayerUtils {
    public static void kickAsync(Player player, String reason) {
        Tasks.INSTANCE.async(() -> player.kick(Component.text(reason)));
    }

}
