package com.stemcraft.commands;

import com.stemcraft.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

public class SMCommandClearInventory  extends SMCommand {

    public SMCommandClearInventory() {
        super("clearinventory");
        alias("clearinv");
        permission("stemcraft.command.inventory.clear");
        tabCompletion("{player}");
    }

    public void execute(SMCommandContext ctx) {
        if(ctx.args.wantsHelp()) {
            ctx.error("Usage: /clearinventory [player]");
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

        PlayerInventory inventory = player.value.getInventory();
        inventory.clear();
        inventory.setArmorContents(null);

        if(player.value != ctx.player) {
            SMMessenger.send(SMMessenger.MessageType.INFO, player.value, "Inventory cleared by " + ctx.senderName());
        }

        StringBuilder response = new StringBuilder();
        response.append("Inventory cleared");
        if(player.value != null && player.value != ctx.player) {
            response.append(" for ").append(player.value.getName());
        }

        ctx.info(response.toString());
    }
}
