package com.stemcraft.timers;

import com.stemcraft.SMConfig;
import com.stemcraft.SMTimer;
import com.stemcraft.utils.SMUtilsPlayer;
import com.stemcraft.utils.SMUtilsString;
import com.stemcraft.utils.SMUtilsWorld;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SMTimerCoord extends SMTimer {
    private static List<Player> players = new ArrayList<Player>();

    @Override
    public void runTimer() {

        for(Player player : players) {
            if(!player.isOnline()) {
                players.remove(player);
                continue;
            }

            String world = SMUtilsString.beautifyCapitalize(player.getLocation().getWorld().getName());
            String time = SMUtilsWorld.toRealTime(player.getLocation().getWorld());
            String direction = SMUtilsPlayer.yawToCompass(player.getLocation().getYaw());

            String coordString = SMConfig.getString("config.coord.action-bar", "&6XYZ: &f{x} {y} {z}  &6{direction}      {time}");
            coordString = SMUtilsString.replaceVariables(
                    coordString,
                    "world",
                    world,
                    "time",
                    time,
                    "direction",
                    direction,
                    "x", String.valueOf(player.getLocation().getBlockX()),
                    "y", String.valueOf(player.getLocation().getBlockY()),
                    "z", String.valueOf(player.getLocation().getBlockZ())
            );

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(SMUtilsString.colorize(coordString)));
        }

        sleep(5);
    }

    public static void addPlayer(Player player) {
        players.add(player);
    }

    public static void removePlayer(Player player) {
        players.remove(player);
    }

    public static boolean hasPlayer(Player player) {
        return players.contains(player);
    }
}
