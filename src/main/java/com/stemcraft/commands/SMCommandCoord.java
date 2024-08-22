package com.stemcraft.commands;

import com.stemcraft.*;
import com.stemcraft.timers.SMTimerCoord;

public class SMCommandCoord extends SMCommand {

    public SMCommandCoord() {
        super("coord");
        permission("stemcraft.command.coord");
    }

    @Override
    public String usage() {
        return "/coord";
    }

    public void execute(SMCommandContext ctx) {
        if(ctx.args.wantsHelp()) {
            ctx.info("Usage: " + usage());
            return;
        }

        if(SMTimerCoord.hasPlayer(ctx.player)) {
            SMTimerCoord.removePlayer(ctx.player);
            ctx.info("Coordinates display disabled.");
        } else {
            SMTimerCoord.addPlayer(ctx.player);
            ctx.info("Coordinates display enabled.");
        }
    }
}
