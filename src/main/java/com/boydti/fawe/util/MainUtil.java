package com.boydti.fawe.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FawePlayer;

public class MainUtil {
    /*
     * Generic non plugin related utils
     *  e.g. sending messages
     */
    public static void sendMessage(final FawePlayer<?> player, String message) {
        message = ChatColor.translateAlternateColorCodes('&', message);
        if (player == null) {
            Fawe.debug(message);
        } else {
            player.sendMessage(message);
        }
    }

    public static void sendAdmin(final String s) {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("fawe.admin")) {
                player.sendMessage(s);
            }
        }
        Fawe.debug(s);
    }
}
