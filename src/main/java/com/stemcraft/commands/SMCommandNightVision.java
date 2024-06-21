package com.stemcraft.commands;

import com.stemcraft.*;
import com.stemcraft.utils.SMUtilsPlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SMCommandNightVision extends SMCommand {

    public SMCommandNightVision() {
        super("nightvision");
        alias("nv");
        permission("stemcraft.command.nightvision");
        tabCompletion("?{players}", "on|off", "?r:{int}", "?w:{world}", "?gm:{gamemode}");
    }

    public void execute(SMCommandContext ctx) {
        if(ctx.args.wantsHelp()) {
            ctx.error("Usage: /nightvision [player] on|off [r:radius] [w:worlds] [gm:gamemodes]");
            return;
        }

        SMCommandArgResult<Player> player = ctx.args.shiftIfConsoleOrExists("{player}", ctx.player);
        if(ctx.fromConsole() && !player.found) {
            ctx.error("A player is required from the console");
            return;
        } else if(player.value == null) {
            ctx.error("Player '{name}' not found", "name", player.arg);
            return;
        }

        String action = ctx.args.shift("on|off|toggle", "toggle");
        if(action == null) {
            ctx.error("The action must be either 'on', 'off', or 'toggle'");
            return;
        }

        SMPlayer smPlayer = new SMPlayer(player.value);
        String actionString = "unknown";

        if(action.equalsIgnoreCase("toggle")) {
            if(smPlayer.hasForeverNightVision()) {
                action = "off";
            } else {
                action = "on";
            }
        }

        if(action.equalsIgnoreCase("on")) {
            actionString = "enabled";
            smPlayer.enableForeverNightVision();
        } else if(action.equalsIgnoreCase("off")) {
            actionString = "disabled";
            smPlayer.disableForeverNightVision();
        }

        if(player.value != ctx.player) {
            SMMessenger.send(SMMessenger.MessageType.INFO, player.value, "Night vision " + actionString + " by " + ctx.senderName());
        }

        StringBuilder response = new StringBuilder();
        response.append("Night vision ").append(actionString);
        if(player.value != null && player.value != ctx.player) {
            response.append(" for ").append(player.value.getName());
        }

        ctx.info(response.toString());
    }
}
