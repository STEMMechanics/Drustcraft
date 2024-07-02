package com.stemcraft;

import com.stemcraft.utils.SMUtilsString;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

public class SMSkipNight {
    private static float skipPercentange = 1f;
    private static int skipRandomTickSpeed = 3;
    private static HashMap<World, BossBar> worlds = new HashMap<>();
    private static HashMap<World, Integer> worldRandomTickCount = new HashMap<>();

    public static void initialize() {
        skipPercentange = SMConfig.getFloat("config.skip-night.required", 1f);
        skipRandomTickSpeed = SMConfig.getInt("config.skip-night.random-tick-speed", 300);
        List<String> worldsList = SMConfig.getStringList("config.skip-night.worlds");

        worldsList.forEach(worldName -> {
            World world = Bukkit.getServer().getWorld(worldName);
            if (world != null) {
                worlds.put(world, null);
            }
        });
    }

    public static void update(World world) {

        if (!worlds.containsKey(world)) {
            return;
        }

        List<Player> players = world.getPlayers();
        int numPlayers = players.size();
        int numSleepers = 0;

        for (Player player : players) {
            if (player.isSleeping()) {
                numSleepers++;
            }
        }

        int required = Math.round(numPlayers * skipPercentange);
        if(required < 1) {
            required = 1;
        }

        BossBar bar = worlds.get(world);

        if (numSleepers == 0) {
            if (bar != null) {
                bar.removeAll();
                bar = null;
                worlds.put(world, null);
            }
        } else {
            String title = SMUtilsString.colorize(
                    SMUtilsString.replaceVariables(
                    "{:SkipNightBarTitle}", "sleeping", String.valueOf(numSleepers), "required", String.valueOf(required)));

            if (bar == null) {
                bar = Bukkit.createBossBar(title, BarColor.BLUE, BarStyle.SOLID);
                worlds.put(world, bar);
            } else {
                bar.setTitle(title);
            }

            double progress = (double) numSleepers / required;
            if(progress > 1.0d) {
                progress = 1.0d;
            } else if(progress < 0.0d) {
                progress = 0.0d;
            }

            bar.setProgress(progress);

            for (Player player : bar.getPlayers()) {
                if (player.getLocation().getWorld() != world || player.getGameMode() != GameMode.SURVIVAL) {
                    bar.removePlayer(player);
                }
            }

            for (Player player : players) {
                if (!bar.getPlayers().contains(player)) {
                    bar.addPlayer(player);
                }
            }

            if (!isSkippingNight(world)) {
                if (numSleepers >= required) {
                    for (Player player : players) {
                        SMMessenger.send(SMMessenger.MessageType.INFO, player, "SKIP_NIGHT_ENOUGH_PLAYERS");
                    }

                    skipNight(world);
                }
            } else {
                if (numSleepers < required) {
                    skipNightFinish(world);
                }
            }
        }
    }

    /**
     * Skip the night of a specified world
     *
     * @param world The world to skip the night
     */
    public static void skipNight(World world) {
        if (!worldRandomTickCount.containsKey(world)) {
            if (world.getTime() > 13000) {
                if (!worldRandomTickCount.containsKey(world)) {
                    worldRandomTickCount.put(world, world.getGameRuleValue(GameRule.RANDOM_TICK_SPEED));
                    world.setGameRule(GameRule.RANDOM_TICK_SPEED, skipRandomTickSpeed);

                    skipNightStep(world);
                }
            }
        }
    }

    /**
     * Skip the night of a specified world
     *
     * @param world The world to skip the night
     */
    private static void skipNightStep(World world) {
        STEMCraft.runOnceDelay("skip_night_" + world.getName(), 1, () -> {
            if (world.getTime() > 1000) {
                world.setTime(world.getTime() + 100);
            }

            if (world.getTime() < 24000 && world.getTime() > 1000 && worldRandomTickCount.containsKey(world)) {
                skipNightStep(world);
            } else {
                skipNightFinish(world);
            }
        });
    }

    /**
     * Complete the skip night task
     *
     * @param world The world to finish
     */
    private static void skipNightFinish(World world) {
        if (worldRandomTickCount.containsKey(world)) {
            world.setGameRule(GameRule.RANDOM_TICK_SPEED, worldRandomTickCount.get(world));
            worldRandomTickCount.remove(world);
        }
    }

    /**
     * Are we skipping the night in the specified world
     *
     * @param world The world to check
     * @return boolean True if the night being skipped
     */
    public static boolean isSkippingNight(World world) {
        return worldRandomTickCount.containsKey(world);
    }
}
