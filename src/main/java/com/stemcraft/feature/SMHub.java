package com.stemcraft.feature;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import com.stemcraft.core.config.SMConfig;
import com.stemcraft.core.event.SMEvent;
import com.stemcraft.STEMCraft;
import com.stemcraft.core.SMFeature;
import com.stemcraft.core.SMLocale;
import com.stemcraft.core.SMMessenger;
import com.stemcraft.core.SMReplacer;
import com.stemcraft.core.command.SMCommand;
import com.stemcraft.core.exception.SMException;

public class SMHub extends SMFeature {
    SMJail jailFeature = null;

    /**
     * When the feature is enabled
     */
    @Override
    protected Boolean onEnable() {
        if (STEMCraft.featureEnabled("jail")) {
            jailFeature = STEMCraft.getFeature("jail", SMJail.class);
        }

        new SMCommand("hub")
            .alias("lobby")
            .permission("stemcraft.command.hub")
            .action(ctx -> {
                ctx.checkNotConsole();
                if (ctx.player != null) {
                    teleportToHub(ctx.player);
                }
            })
            .register();

        SMEvent.register(PlayerJoinEvent.class, (ctx) -> {
            PlayerJoinEvent event = (PlayerJoinEvent) ctx.event;
            Player player = event.getPlayer();

            if (jailFeature != null) {
                if (jailFeature.isJailed(player)) {
                    return;
                }
            }

            if (!player.hasPermission("stemcraft.hub.override")) {
                STEMCraft.runLater(() -> {
                    teleportToHub(player);
                });
            }
        });

        return true;
    }

    /**
     * Teleport player to hub world.
     * 
     * @param player
     */
    private final static void teleportToHub(Player player) {
        final String hubWorldName = SMConfig.main().getString("hub.world", "world");
        final String key = "hub.tp-commands." + player.getWorld().getName().toLowerCase();

        World hubWorld = Bukkit.getWorld(hubWorldName);
        if (hubWorld == null) {
            SMMessenger.error(player, SMLocale.get("HUB_NOT_DEFINED"));
            throw new SMException("Hub world " + hubWorldName + " not found");
        }

        if (SMConfig.main().contains(key)) {
            SMConfig.main().getStringList(key).forEach(command -> {
                String replacedCommand = SMReplacer.replaceVariables(command, "player", player.getName(), "hub-world", hubWorldName);
                CommandSender sender;


                if (replacedCommand.startsWith("SERVER:")) {
                    replacedCommand = replacedCommand.substring(7); // remove "SERVER:"
                    sender = Bukkit.getConsoleSender();
                } else {
                    sender = player;
                    if (replacedCommand.startsWith("PLAYER:")) {
                        replacedCommand = replacedCommand.substring(7); // remove "PLAYER:"
                    }
                }

                Bukkit.getServer().dispatchCommand(sender, replacedCommand);
            });
        } else {
            player.teleport(hubWorld.getSpawnLocation());
        }

        if (!STEMCraft.hasPlayerRecentlyJoined(player)) {
            SMMessenger.info(player, SMLocale.get("HUB_TELEPORTED"));
        }
    }
}
