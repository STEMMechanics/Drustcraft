package com.stemcraft.feature;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import com.stemcraft.core.SMCommon;
import com.stemcraft.core.SMFeature;
import com.stemcraft.core.SMMessenger;
import com.stemcraft.core.command.SMCommand;

public class SMGameMode extends SMFeature {
    @Override
    protected Boolean onEnable() {
        new SMCommand("gm")
            .alias("gma", "gmc", "gms", "gmsp")
            .tabComplete("{player}")
                .tabComplete("*")
            .permission("minecraft.command.gamemode")
            .action(ctx -> {
                ctx.checkNotConsole();

                Player targetPlayer = ctx.player;
                String gamemodeStr = "Unknown";
                boolean all = false;

                if (!ctx.args.isEmpty()) {
                    if(ctx.args.get(0).equals("*")) {
                        all = true;
                    } else {
                        targetPlayer = SMCommon.findPlayer(ctx.args.get(0));
                        if (targetPlayer == null) {
                            ctx.returnErrorLocale("CMD_PLAYER_NOT_FOUND");
                            return;
                        }
                    }
                }

                if ("gma".equals(ctx.alias)) {
                    if(all) {
                        for(Player player : Bukkit.getOnlinePlayers()) {
                            player.setGameMode(GameMode.ADVENTURE);
                        }
                    } else {
                        targetPlayer.setGameMode(GameMode.ADVENTURE);
                    }
                    gamemodeStr = "Adventure";
                } else if ("gmc".equals(ctx.alias)) {
                    if(all) {
                        for(Player player : Bukkit.getOnlinePlayers()) {
                            player.setGameMode(GameMode.CREATIVE);
                        }
                    } else {
                        targetPlayer.setGameMode(GameMode.CREATIVE);
                    }
                    gamemodeStr = "Creative";
                } else if ("gms".equals(ctx.alias)) {
                    if(all) {
                        for(Player player : Bukkit.getOnlinePlayers()) {
                            player.setGameMode(GameMode.SURVIVAL);
                        }
                    } else {
                        targetPlayer.setGameMode(GameMode.SURVIVAL);
                    }
                    gamemodeStr = "Survival";
                } else if ("gmsp".equals(ctx.alias)) {
                    if(all) {
                        for(Player player : Bukkit.getOnlinePlayers()) {
                            player.setGameMode(GameMode.SPECTATOR);
                        }
                    } else {
                        targetPlayer.setGameMode(GameMode.SPECTATOR);
                    }
                    gamemodeStr = "Spectator";
                } else {
                    ctx.returnErrorLocale("GAMEMODE_UNKNOWN");
                }

                if (targetPlayer == ctx.sender) {
                    SMMessenger.infoLocale(targetPlayer, "GAMEMODE_CHANGED", "gamemode", gamemodeStr);
                } else {
                    if(all) {
                        for(Player player : Bukkit.getOnlinePlayers()) {
                            SMMessenger.infoLocale(player, "GAMEMODE_CHANGED_BY", "player", ctx.senderName(), "gamemode", gamemodeStr);
                        }
                        SMMessenger.infoLocale(ctx.sender, "GAMEMODE_CHANGED_ALL", "gamemode", gamemodeStr);
                    } else {
                        SMMessenger.infoLocale(ctx.sender, "GAMEMODE_CHANGED_FOR", "player", targetPlayer.getName(), "gamemode", gamemodeStr);
                        SMMessenger.infoLocale(targetPlayer, "GAMEMODE_CHANGED_BY", "player", ctx.senderName(), "gamemode", gamemodeStr);
                    }
                }

            })
            .register();


        // String[][] tabCompletions = new String[][]{
        // {"gma", "%player%"},
        // {"gmc", "%player%"},
        // {"gms", "%player%"},
        // {"gmsp", "%player%"},
        // };

        return true;
    }
}
