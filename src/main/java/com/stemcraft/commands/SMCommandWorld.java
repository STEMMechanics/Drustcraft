package com.stemcraft.commands;

import com.stemcraft.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SMCommandWorld extends SMCommand {

    public SMCommandWorld() {
        super("world");
        description("World management");
        permission("stemcraft.command.world");
        tabCompletion("create");
        tabCompletion("delete", "{world}");
        tabCompletion("list");
        tabCompletion("load", "{world-offline}");
        tabCompletion("unload", "{world}");
        tabCompletion("teleport", "{world}");
        tabCompletion("set", "spawn", "{world}");
        tabCompletion("set", "gamemode", "{world}", "none");
        tabCompletion("set", "gamemode", "{world}", "{gamemode}");
        register();
    }

    @Override
    public String usage() {
        return "/world [create|delete|list|load|unload|teleport] [world]";
    }

    @Override
    public void execute(SMCommandContext ctx) {
        if(ctx.args.wantsHelp()) {
            ctx.usage();
            return;
        }

        String action = ctx.args.shift("create|delete|list|load|unload|teleport");
        if(action == null) {
            ctx.usage();
        } else if(action.equalsIgnoreCase("load")) {
            String worldName = ctx.args.shift();
            if (worldName == null) {
                ctx.error("You must specify a world name");
                return;
            }

            SMWorld smWorld = new SMWorld(worldName);

            if (smWorld.isLoaded()) {
                ctx.error("World '{worldName}' is already loaded", "worldName", worldName);
                return;
            }

            if (!smWorld.exists()) {
                ctx.error("World '{worldName}' does not exist", "worldName", worldName);
                return;
            }

            ctx.success("Loading world '{worldName}'...", "worldName", worldName);
            smWorld.load();
            ctx.success("World '{worldName}' loaded", "worldName", worldName);
        } else if(action.equalsIgnoreCase("create")) {
            String worldName = ctx.args.shift();
            if (worldName == null) {
                ctx.error("You must specify a world name");
                return;
            }

            SMWorld smWorld = new SMWorld(worldName);

            if (smWorld.isLoaded()) {
                ctx.error("World '{worldName}' already exists", "worldName", worldName);
                return;
            }

            smWorld.create();
            ctx.success("World '{worldName}' created", "worldName", worldName);
        } else if(action.equalsIgnoreCase("teleport")) {
            String worldName = ctx.args.shift();
            if (worldName == null) {
                ctx.error("You must specify a world name");
                return;
            }

            SMWorld smWorld = new SMWorld(worldName);

            if (!smWorld.exists()) {
                ctx.error("World '{worldName}' does not exist", "worldName", worldName);
                return;
            }

            if (!smWorld.isLoaded()) {
                ctx.error("World '{worldName}' is not loaded", "worldName", worldName);
                return;
            }

            Player player = ctx.player;
            SMPlayer.teleport(player, smWorld.getBase().getSpawnLocation());
            ctx.success("Teleported to world '{worldName}'", "worldName", worldName);
        } else if(action.equalsIgnoreCase("list")) {
            Collection<String> worlds = SMWorld.list();

            new SMPaginate(ctx.sender, 1)
                .count(worlds.size())
                .command("world list")
                .title("Worlds")
                .none("No worlds where found")
                .showItems((start, max) -> {
                    List<BaseComponent[]> rows = new ArrayList<>();
                    int end = Math.min(start + max, worlds.size());

                    for (String worldName : worlds.stream().skip(start).limit(end - start).toList()) {
                        BaseComponent[] row = new BaseComponent[2];
                        row[0] = new TextComponent(ChatColor.GOLD + worldName + " ");

                        SMWorld smWorld = new SMWorld(worldName);
                        if (smWorld.isLoaded()) {
                            row[1] = new TextComponent(ChatColor.GREEN + "Loaded");
                        } else {
                            row[1] = new TextComponent(ChatColor.RED + "Not loaded");
                        }

                        rows.add(row);
                    }

                    return rows;
                });
        } else if(action.equalsIgnoreCase("set")) {
            String subAction = ctx.args.shift("create|delete|list|load|unload|teleport");

            // world name is not needed under 'spawn' and if called by a player
            String worldName = ctx.args.shift();
            if (worldName == null) {
                ctx.error("You must specify a world name");
                return;
            }

            SMWorld smWorld = new SMWorld(worldName);

            if (!smWorld.exists()) {
                ctx.error("World '{worldName}' does not exist", "worldName", worldName);
                return;
            }

            if (!smWorld.isLoaded()) {
                ctx.error("World '{worldName}' is not loaded", "worldName", worldName);
                return;
            }

            if(subAction == null) {
                ctx.usage();
            } else if(subAction.equalsIgnoreCase("spawn")) {
                Location location = ctx.player.getLocation();
                smWorld.setSpawn(location);
            } else if(subAction.equalsIgnoreCase("gamemode")) {
                String gameMode = ctx.args.shift("none|survival|creative|adventure|spectator");
                if(gameMode != null) {
                    smWorld.setGameMode(gameMode);
                } else {
                    ctx.error("Unknown gamemode");
                }
            }

        } else {
            ctx.error("Unknown action: " + action);
        }
    }
}
