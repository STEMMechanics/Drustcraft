package com.stemcraft.commands;

import com.stemcraft.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

public class SMCommandSpeed extends SMCommand {

    public SMCommandSpeed() {
        super("speed");
        permission("stemcraft.command.speed");
        tabCompletion("walk", "{speed}", "{player}");
        tabCompletion("fly", "{speed}", "{player}");
        tabCompletion("{speed}", "{player}");
        tabCompletion("reset", "{player}");
    }

    @Override
    public String usage() {
        return "Usage: /speed [walk|fly|reset] speed [player]";
    }

    public void execute(SMCommandContext ctx) {
        if(ctx.args.wantsHelp() || ctx.args.length() > 3) {
            ctx.info(usage());
            return;
        }

        SMCommandArgResult<String> action = ctx.args.popIfExists("walk|fly|reset");
        SMCommandArgResult<Float> speed = ctx.args.popIfExists("{speed}");
        SMCommandArgResult<Player> player = ctx.args.popIfExists("{player}", ctx.player);

        if(ctx.fromConsole() && !player.found) {
            ctx.error("A player is required from the console");
            return;
        } else if(player.value == null) {
            ctx.error("Player '{name}' not found", "name", player.arg);
            return;
        }

        SMPlayer smPlayer = new SMPlayer(player.value);

        if (action.value == null) {
            action.value = player.value.isFlying() ? "fly" : "walk";
        }

        String actionString = "";

        if(action.value.equalsIgnoreCase("reset")) {
            smPlayer.resetWalkSpeed();
            smPlayer.resetFlySpeed();
            actionString = "Speed reset";
        } else if(speed.value != null) {
            if(action.value.equalsIgnoreCase("walk")) {
                smPlayer.setWalkSpeed(speed.value);
                actionString = "Walk";
            } else if(action.value.equalsIgnoreCase("fly")) {
                smPlayer.setFlySpeed(speed.value);
                actionString = "Fly";
            }

            actionString += "speed set to " + speed.value;
        } else if(ctx.args.isEmpty()){
            ctx.info(player.value.getName() + " walk speed: " + player.value.getWalkSpeed());
            ctx.info(player.value.getName() + " fly speed: " + player.value.getFlySpeed());
            return;
        } else {
            ctx.info(usage());
            return;
        }

        if(player.value != ctx.player) {
            SMMessenger.send(SMMessenger.MessageType.INFO, player.value, actionString + " by " + ctx.senderName());
        }

        StringBuilder response = new StringBuilder();
        response.append(actionString);
        if(player.value != null && player.value != ctx.player) {
            response.append(" for ").append(player.value.getName());
        }

        ctx.info(response.toString());
    }
}
