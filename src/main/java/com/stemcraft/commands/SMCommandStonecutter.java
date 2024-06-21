package com.stemcraft.commands;

import com.stemcraft.SMCommand;
import com.stemcraft.SMCommandArgResult;
import com.stemcraft.SMCommandContext;
import com.stemcraft.utils.SMUtilsPlayer;
import org.bukkit.entity.Player;

public class SMCommandStonecutter extends SMCommand {

        public SMCommandStonecutter() {
            super("stonecutter");
            permission("stemcraft.command.stonecutter");
            description("Opens the stonecutter interface");
            tabCompletion("{players}");
            register();
        }

        @Override
        public void execute(SMCommandContext ctx) {
            SMCommandArgResult<Player> player = ctx.args.shiftIfConsoleOrExists("{players}", ctx.player);
            if(ctx.fromConsole() && !player.found) {
                ctx.error("A player is required from the console");
                return;
            } else if(player.value == null) {
                ctx.error("Player '{name}' not found", "name", player.arg);
                return;
            }

            if(player.value != ctx.player && !ctx.hasPermission("stemcraft.command.stonecutter.others")) {
                ctx.error("You do not have permission to open the stonecutter interface for other players");
                return;
            }

            SMUtilsPlayer.openStonecutter(player.value, null, true);
        }
}
