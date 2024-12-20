package com.stemcraft.minigames;

import com.stemcraft.*;
import com.stemcraft.bridges.SMWorldEdit;
import com.stemcraft.utils.SMUtilsString;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SMMiniGameParkour extends SMMiniGame {

    /**
     * Constructor
     */
    public SMMiniGameParkour() {
        super("parkour");
        arenaUsesLobby = false;

        addSetOption("end", (arena, player) -> {
            SMRegion end = SMWorldEdit.getSelection(player);
            if(end == null) {
                SMMessenger.send(SMMessenger.MessageType.ERROR, player, "You need to have a world edit selection before running this command");
            } else {
                arena.setRegion("end", end);
                SMMessenger.send(SMMessenger.MessageType.SUCCESS, player, "The arena end region has been updated");
            }
        });
    }

    @Override
    public void afterPlayerJoinArena(SMMiniGameArena arena, Player player) {
        SMPlayer.freeze(player);

        String bestTimeStr = "Never run";
        long bestTime = getArenaPlayerTime(arena, player);
        if(bestTime < Long.MAX_VALUE) {
            bestTimeStr = SMUtilsString.formatDuration(bestTime);
        }

        SMScoreboard scoreboard = new SMScoreboard("&e&lParkour");
        scoreboard.setLine(3, "&fMap: &a" + arena.getName());
        scoreboard.setLine(2, " ");
        scoreboard.setLine(1, "&fBest Time: &a" + bestTimeStr);
        scoreboard.setLine(0, "&fCurrent Time: &a0:00");
        scoreboard.addPlayer(player);

        // Start countdown
        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (countdown > 0) {
                    player.sendTitle(ChatColor.YELLOW + "" + countdown, "", 0, 20, 0);
                    countdown--;
                } else {
                    this.cancel();
                    SMPlayer.unfreeze(player);
                    onPlayerStart(arena, player);
                }
            }
        }.runTaskTimer(STEMCraft.getPlugin(), 0L, 20L);
    }

    public void onPlayerStart(SMMiniGameArena arena, Player player) {
        arena.setPlayerData(player, "startTime", System.currentTimeMillis());

        BukkitTask timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                long duration = System.currentTimeMillis() - arena.getPlayerData(player, "startTime", Long.class, 0L);
                String durationStr = SMUtilsString.formatDuration(duration);
                SMScoreboard scoreboard = SMScoreboard.get(player);
                scoreboard.setLine(4, durationStr);

//                if (SMRegion.isInside(player)) {
//                    this.cancel();
//                    timerTasks.remove(playerUUID);
//                    finishParkour(player, duration);
//                }
            }
        }.runTaskTimer(STEMCraft.getPlugin(), 0L, 20L);

        arena.setPlayerData(player, "timerTask", timerTask);
    }

    public void onPlayerFinish(SMMiniGameArena arena, Player player) {
        // save time if best
//        arena.fireworks(); maybe just player fireworks?
        // wait 10 seconds and teleport out of arena
    }

    private long getArenaPlayerTime(SMMiniGameArena arena, Player player) {
        return SMConfig.getLong(arena.getConfigPath() + ".durations." + player.getUniqueId(), Long.MAX_VALUE);
    }

    private Map<UUID, Long> getArenaBestTime(SMMiniGameArena arena) {
        Map<UUID, Long> bestTime = new HashMap<>();

        String configPath = arena.getConfigPath();
        Map<String, Object> times = SMConfig.getMap(configPath + ".times");

        if (times == null || times.isEmpty()) {
            return bestTime;
        }

        UUID bestPlayer = null;
        long bestTimeValue = Long.MAX_VALUE;

        for (Map.Entry<String, Object> entry : times.entrySet()) {
            try {
                UUID playerUUID = UUID.fromString(entry.getKey());
                if (Bukkit.getPlayer(playerUUID) == null && Bukkit.getOfflinePlayer(playerUUID).hasPlayedBefore()) {
                    // Skip if the UUID doesn't correspond to a valid player
                    continue;
                }

                if (entry.getValue() instanceof Long) {
                    long time = (Long) entry.getValue();
                    if (time < bestTimeValue) {
                        bestTimeValue = time;
                        bestPlayer = playerUUID;
                    }
                }
            } catch (IllegalArgumentException e) {
                // Invalid UUID string, skip this entry
                continue;
            }
        }

        if (bestPlayer != null) {
            bestTime.put(bestPlayer, bestTimeValue);
        }

        return bestTime;
    }

    /*
     * /parkour set <arena> bounds
     * /parkour set <arena> spawn
     * /parkour set <arena> end
    */

    public boolean onSetOption(SMMiniGameArena arena, String option, Player player) {
        if(option.equalsIgnoreCase("spawn")) {
            arena.setLocation("spawn", player.getLocation());
            SMMessenger.send(SMMessenger.MessageType.INFO, player, "Arena spawn location updated");
            return true;
        } else if(option.equalsIgnoreCase("end")) {
            SMRegion region = SMWorldEdit.getSelection(player);
            if(region == null) {
                SMMessenger.send(SMMessenger.MessageType.ERROR, player, "You need to select a region with /wand first");
            } else {
                arena.setRegion("end", region);
                SMMessenger.send(SMMessenger.MessageType.INFO, player, "Arena end region updated");
            }
            return true;
        }


        return false;
    }
}
