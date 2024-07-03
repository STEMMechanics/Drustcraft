package com.stemcraft.listeners;

import com.stemcraft.SMConfig;
import com.stemcraft.SMListener;
import com.stemcraft.STEMCraft;
import com.stemcraft.utils.SMUtilsString;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.ServerListPingEvent;

public class SMServerListener extends SMListener {

    /**
     * When the server is pinged by a client
     * @param event The event
     */
    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        String motdTitle = SMConfig.getString("config.motd.title", "");
        String motdText = SMConfig.getString("config.motd.text", "");
        String motdTextVersion = "";

        if (SMConfig.getBoolean("config.motd.show-version", true)) {
            motdTextVersion = "&8v" + STEMCraft.getDisplayVersion() + " &r";
        }

        event.setMotd(SMUtilsString.colorize(motdTitle + "\n" + motdTextVersion + motdText));
    }
}
