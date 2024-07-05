package com.stemcraft.commands;

import com.stemcraft.SMBackup;
import com.stemcraft.SMCommand;
import com.stemcraft.SMCommandContext;

public class SMCommandBackup extends SMCommand {

    /**
     * Constructor
     */
    public SMCommandBackup() {
        super("backup");
        permission("stemcraft.command.backup");
        description("Backup the server");
        tabCompletion("-noupload");
        register();
    }

    /**
     * Return the usage string of the command
     *
     * @return The usage string
     */
    @Override
    public String usage() {
        return "/backup -noupload";
    }

    /**
     * Execute the command
     *
     * @param ctx The command context
     */
    @Override
    public void execute(SMCommandContext ctx) {
        if(!SMBackup.inProgress()) {
            SMBackup.backup(!ctx.args.hasDashArg("noupload"));
        } else {
            ctx.error("A server backup is already in progress");
        }
    }
}
