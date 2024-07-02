package com.stemcraft.commands;

import com.stemcraft.SMBackup;
import com.stemcraft.SMCommand;
import com.stemcraft.SMCommandArgResult;
import com.stemcraft.SMCommandContext;
import com.stemcraft.utils.SMUtilsPlayer;
import org.bukkit.entity.Player;

public class SMCommandBackup extends SMCommand {

        public SMCommandBackup() {
            super("backup");
            permission("stemcraft.command.backup");
            description("Backup the server");
            register();
        }

        @Override
        public void execute(SMCommandContext ctx) {
            SMBackup.backup();
        }
}
