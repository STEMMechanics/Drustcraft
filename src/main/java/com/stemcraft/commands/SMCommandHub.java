package com.stemcraft.commands;

import com.stemcraft.SMCommand;
import com.stemcraft.SMCommandContext;

public class SMCommandHub extends SMCommand {

        public SMCommandHub() {

            super("hub");
            alias("lobby");
            permission("stemcraft.command.hub");
            description("Teleport to the hub");
            register();
        }

        @Override
        public void execute(SMCommandContext ctx) {
//            List<Player> players = ctx.args.shiftIf("{players}");
//            if(players.isEmpty() && !ctx.fromConsole()) {
//                players.add(ctx.player);
//            }


//            SMPlayer player = new SMPlayer(this.sender);
//            player.teleport(SMConfig.getHubLocation());
        }
}
