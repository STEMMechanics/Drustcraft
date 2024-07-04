package com.stemcraft.commands;

import com.stemcraft.*;
import com.stemcraft.bridges.SMWorldEdit;
import com.stemcraft.utils.SMUtilsMath;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SMCommandRegion extends SMCommand {

    public SMCommandRegion() {
        super("region");
        description("Region management");
        alias("rg");
        permission("stemcraft.command.region");
        tabCompletion("create");
        tabCompletion("delete", "{region}");
        tabCompletion("select", "{region}");
        tabCompletion("list");
        tabCompletion("set", "{region}", "teleport-enter");
        register();
    }

    @Override
    public String usage() {
        return "/region [create|select|delete|list|load|unload|teleport|update] [region]";
    }

    @Override
    public void execute(SMCommandContext ctx) {
        if(ctx.args.wantsHelp()) {
            ctx.usage();
            return;
        }

        String action = ctx.args.shift("create|delete|list|set|select|update");
        String regionName = "";
        SMRegion region = null;

        if(action == null) {
            ctx.usage();
            return;
        }

        if(!action.equalsIgnoreCase("list")) {
            regionName = ctx.args.shift();

            if (regionName.isEmpty()) {
                ctx.error("You must specify a region name");
                return;
            }

            region = SMRegion.get(regionName);

            if(region == null && !action.equalsIgnoreCase("create")) {
                ctx.error("The region '{regionName}' does not exists", "regionName", regionName);
                return;
            }
        }

        if(action.equalsIgnoreCase("create")) {
            if(ctx.fromConsole()) {
                ctx.error("Command must be run by a player");
                return;
            }

            List<Location> points = SMWorldEdit.getSelection(ctx.player);
            if(points == null) {
                ctx.error("You do not have a world edit selection");
                return;
            }

            if(SMRegion.exists(regionName)) {
                ctx.error("A region with that name already exists");
                return;
            }

            SMRegion.create(regionName, points);
            ctx.success("Region '{regionName}' created", "regionName", regionName);
        } else if(action.equalsIgnoreCase("delete")) {
            assert region != null;
            region.delete();
            ctx.success("Region '{regionName}' deleted", "regionName", regionName);
        } else if(action.equalsIgnoreCase("select")) {
            assert region != null;

            if(ctx.fromConsole()) {
                ctx.error("This command requires to be run by a player");
                return;
            }

            SMWorldEdit.setSelection(ctx.player, region.getPoints());
            ctx.success("Region '{regionName}' selected", "regionName", regionName);
        } else if(action.equalsIgnoreCase("update")) {
            assert region != null;

            if(ctx.fromConsole()) {
                ctx.error("This command requires to be run by a player");
                return;
            }

            List<Location> points = SMWorldEdit.getSelection(ctx.player);
            if(points == null) {
                ctx.error("You do not have a world edit selection");
                return;
            }

            region.setPoints(points);
            region.save();

            SMRegion.reloadPlayerRegions();
            ctx.success("Region '{regionName}' updated", "regionName", regionName);
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
            String subAction = ctx.args.shift("teleport-enter|teleport-exit");

            if(ctx.fromConsole()) {
                ctx.error("Command must be run by a player");
                return;
            }

            if(subAction == null) {
                ctx.usage();
            } else if(subAction.equalsIgnoreCase("teleport-enter")) {
                Location location = SMUtilsMath.round(ctx.player.getLocation());
                region.setTeleportEnter(location);
                region.save();

                ctx.success("The 'teleport-enter' flag has been set");
            }
        } else {
            ctx.error("Unknown action: " + action);
        }
    }
}
