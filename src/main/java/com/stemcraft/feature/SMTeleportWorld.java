package com.stemcraft.feature;

import com.stemcraft.core.SMCommon;
import com.stemcraft.core.SMFeature;
import com.stemcraft.core.SMMessenger;
import com.stemcraft.core.SMPersistent;
import com.stemcraft.core.command.SMCommand;
import com.stemcraft.core.event.SMEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class SMTeleportWorld extends SMFeature {

    /**
     * When feature is enabled
     */
    @Override
    protected Boolean onEnable() {
        SMEvent.register(PlayerTeleportEvent.class, ctx -> {
            Player player = ctx.event.getPlayer();
            Location from = ctx.event.getFrom();
            this.saveLastLocation(player, from);
        });

        SMEvent.register(PlayerQuitEvent.class, ctx -> {
            Player player = ctx.event.getPlayer();
            this.saveLastLocation(player, player.getLocation());
        });


        new SMCommand("tpworld")
            .alias("teleportworld")
            .permission("stemcraft.teleport.world")
            .tabComplete("{player}", "{world}", "-d")
            .action(ctx -> {
                ctx.checkArgs(2, "TPWORLD_USAGE");
                Player targetPlayer = ctx.getArgAsPlayer(1, ctx.player);

                // Check target player exists
                ctx.checkNotNullLocale(targetPlayer, "CMD_PLAYER_NOT_FOUND");

                String key = targetPlayer.getUniqueId() + "_" + ctx.getArg(2, "");
                Location from = SMPersistent.getObject(this, key, Location.class);
                if(from == null) {
                    ctx.returnErrorLocale("TPWORLD_NOT_FOUND", "world", ctx.getArg(2, ""));
                    return;
                }

                // Teleport player
                SMCommon.delayedPlayerTeleport(targetPlayer, from);
            })
            .register();

        return true;
    }

    protected void saveLastLocation(Player player, Location location) {
        String key = player.getUniqueId() + "_" + location.getWorld().getName();

        // Remove "_nether" or "_the_end" from the key if present
        key = key.replaceFirst("(_nether|_the_end)$", "");

        SMPersistent.set(this, key, location);
    }
}
