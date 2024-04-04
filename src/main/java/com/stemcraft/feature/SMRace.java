package com.stemcraft.feature;

import com.stemcraft.STEMCraft;
import com.stemcraft.core.*;
import com.stemcraft.core.command.SMCommand;
import com.stemcraft.core.config.SMConfig;
import com.stemcraft.core.event.SMEvent;
import com.stemcraft.core.tabcomplete.SMTabComplete;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allows the creation of custom books that can be saved and shown to the player using a command
 */
public class SMRace extends SMFeature {
    private Map<Player, Location> playerReturn = new HashMap<>();

    public static class RaceWorld {
        @Getter
        protected String name;
        @Getter
        protected World world;
        @Getter
        protected String title;
        @Getter
        protected boolean enabled;
        protected boolean raceStarted;
        protected int raceCountdown;
        private final Map<Player, BossBar> playerBossBars = new HashMap<>();
        private List<Location> checkpoints = new ArrayList<>();
        private final Map<Player, Integer> playerCheckpoints = new HashMap<>();
        private final List<Player> playerPositions = new ArrayList<>();

        private Scoreboard scoreboard;
        private Objective objective;
        private long startTime;

        private SMTask taskCountdown = null;
        private SMTask taskTracker = null;

        public RaceWorld(String name, World world) {
            this.name = name;
            this.world = world;

            raceStarted = false;
            raceCountdown = 0;
            title = SMConfig.main().getString("race." + name + ".title", "Race");

            SMConfig.main().getStringList("race." + name + ".checkpoints").forEach(checkpoint -> {
                checkpoints.add(SMCommon.stringToLocation(checkpoint));
            });

            if(checkpoints.isEmpty()) {
                STEMCraft.info("No checkpoints defined for race: " + name);
                enabled = false;
                return;
            }

            worldClean();
            enabled = true;
        }

        public void playerJoin(Player player) {
            if(!enabled) {
                return;
            }

            if(!playerBossBars.containsKey(player)) {
                BossBar bossBar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
                bossBar.addPlayer(player);
                playerBossBars.put(player, bossBar);
                playerInitInventory(player);
            }

            if(!raceStarted && world.getPlayers().size() > 1) {
                raceStart();
            }

            playerCheckpoints.put(player, 0);

            bossBarUpdateAll();

            if (scoreboard != null) {
                player.setScoreboard(scoreboard);
            }
        }

        public void playerLeave(Player player) {
            if(!enabled) {
                return;
            }

            BossBar bossBar = playerBossBars.get(player);
            if (bossBar != null) {
                bossBar.removePlayer(player);
                playerBossBars.remove(player);
            }

            playerCheckpoints.remove(player);

            List<Player> players = world.getPlayers();
            if(players.size() < 2) {
                if(raceStarted) {
                    players.forEach(p -> {
                        SMMessenger.error(p, "Race has ended due to lack of players");
                    });
                }

                raceStop();
            }

            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }

        public boolean playerExists(Player player) {
            return world.getPlayers().contains(player);
        }

        public void playerInitInventory(Player player) {
            PlayerInventory inv = player.getInventory();
            inv.clear();

            List<String> items = SMConfig.main().getStringList("race." + name + ".items");
            items.forEach(item -> {
                String[] parts = item.split(" ");
                Material material = Material.getMaterial(parts[0].toUpperCase());
                if (material == null) {
                    STEMCraft.info("Invalid material: " + parts[0]);
                    return;
                }

                int amount;
                try {
                    amount = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    STEMCraft.info("Invalid amount: " + parts[1]);
                    return;
                }

                inv.addItem(new ItemStack(material, amount));
            });
        }

        public void worldClean() {
            world.getEntities().forEach(entity -> {
                if(!(entity instanceof Player)) {
                    entity.remove();
                }
            });

            playerPositions.clear();
            playerCheckpoints.clear();
        }

        public void raceStart() {
            raceStarted = true;
            raceCountdown = 45;

            taskCountdown = STEMCraft.runTimer(20, () -> {
                if(raceCountdown > 0) {
                    raceCountdown--;
                    if (raceCountdown <= 5 && raceCountdown > 0) {
                        world.getPlayers().forEach(player -> {
                            player.sendTitle(ChatColor.YELLOW + String.valueOf(raceCountdown), "", 10, 70, 20);
                        });
                    } else if (raceCountdown <= 0) {
                        world.getPlayers().forEach(player -> {
                            player.sendTitle(ChatColor.GREEN + "GO", "", 10, 20, 20);
                        });

                        taskCountdown.cancel();

                        ScoreboardManager manager = Bukkit.getScoreboardManager();
                        scoreboard = manager.getNewScoreboard();
                        objective = scoreboard.registerNewObjective("race", "dummy", ChatColor.GREEN + title);
                        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

                        world.getPlayers().forEach(player -> {
                            player.setScoreboard(scoreboard);
                            playerCheckpoints.put(player, 0);
                        });

                        startTime = System.currentTimeMillis();

                        taskTracker = STEMCraft.runTimer(10, () -> {
                            playerCheckpoints.forEach((player, checkpoint) -> {
                                if(player.getLocation().getWorld() != world) {
                                    return;
                                }

                                if(checkpoint >= checkpoints.size()) {
                                    return;
                                }

                                Location playerLocation = player.getLocation();
                                Location checkpointLocation = checkpoints.get(checkpoint);

                                if (playerLocation.distance(checkpointLocation) <= 16) {
                                    playerCheckpoints.put(player, checkpoint + 1);

                                    if(checkpoint == checkpoints.size() - 1) {
                                        player.sendTitle(ChatColor.GREEN + "Finish!", "", 10, 20, 20);
                                    } else {
                                        player.sendTitle(ChatColor.GREEN + "Checkpoint " + (checkpoint + 1), "", 10, 20, 20);
                                    }

                                    playerPositions.remove(player);
                                    AtomicInteger index = new AtomicInteger(-1);

                                    playerPositions.forEach(p -> {
                                        if (playerCheckpoints.get(p) < checkpoint + 1) {
                                            index.set(playerPositions.indexOf(p));
                                        }
                                    });

                                    if (index.get() == -1) {
                                        playerPositions.add(player);
                                    } else {
                                        playerPositions.add(index.get(), player);
                                    }
                                }
                            });

                            bossBarUpdateAll();

                            // Update the scoreboard
                            String elapsedTime = SMCommon.convertSecondsToRelative((System.currentTimeMillis() - startTime) / 1000, true);
                            objective.setDisplayName(ChatColor.GREEN + title + " - " + elapsedTime);

                            for (String entry : scoreboard.getEntries()) {
                                scoreboard.resetScores(entry);
                            }

                            // Set the new scores
                            Score scoreLine = objective.getScore("");
                            scoreLine.setScore(5);

                            int score = 4;
                            for (int i = 0; i < playerPositions.size(); i++) {
                                String line = ChatColor.YELLOW + ((i + 1) + ": " + ChatColor.WHITE + playerPositions.get(i).getName());
                                scoreLine = objective.getScore(line);
                                scoreLine.setScore(score--);
                                if(score == 0) {
                                    break;
                                }
                            }

                            while (score > 0) {
                                String line = ChatColor.YELLOW + String.valueOf(5 - score) + ": " + ChatColor.GRAY + "------";
                                scoreLine = objective.getScore(line);
                                scoreLine.setScore(score);
                                score--;
                            }
                        });
                    }

                    bossBarUpdateAll();
                }
            });
        }

        public void raceStop() {
            if(taskCountdown != null) {
                taskCountdown.cancel();
                taskCountdown = null;
            }

            if(taskTracker != null) {
                taskTracker.cancel();
                taskTracker = null;
            }

            raceStarted = false;

            world.getPlayers().forEach(player -> {
                SMCommon.safePlayerTeleport(player, world.getSpawnLocation());
            });

            world.getPlayers().forEach(player -> {
                player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            });
            scoreboard = null;
            objective = null;

            worldClean();
            bossBarUpdateAll();
        }

        public void bossBarUpdateAll() {
            playerBossBars.forEach((player, bossBar) -> {
                if(!raceStarted) {
                    bossBar.setTitle("Waiting for players");
                    bossBar.setProgress(1);
                } else {
                    if(raceCountdown > 0) {
                        bossBar.setTitle("Get ready! Race starts in " + raceCountdown);
                        bossBar.setProgress((double) raceCountdown / 45);
                    } else {
                        int position = playerPositions.indexOf(player);
                        String positionString;

                        if(position == -1) {
                            positionString = "--";
                        } else {
                            positionString = String.valueOf(position + 1);
                        }

                        bossBar.setTitle("Racing!  -  " + positionString + " / " + playerCheckpoints.size());
                        double progress = (double) (playerCheckpoints.get(player) + 1) / checkpoints.size();
                        if(progress > 1) {
                            progress = 1;
                        }

                        bossBar.setProgress(progress);
                    }
                }
            });
        }
    }


    List<RaceWorld> raceWorlds = new ArrayList<>();

    /**
     * When feature is enabled
     */
    @Override
    protected Boolean onEnable() {
        List<String> names = SMConfig.main().getKeys("race");
        if(names.isEmpty()) {
            STEMCraft.info("No racing worlds defined in config");
            return false;
        }

        names.forEach(name -> {
            String worldName = SMConfig.main().getString("race." + name + ".world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                STEMCraft.info("Invalid world: " + worldName);
                return;
            }

            RaceWorld raceWorld = new RaceWorld(name, world);
            raceWorlds.add(raceWorld);
        });

        SMEvent.register(PlayerTeleportEvent.class, ctx -> {
            World from = ctx.event.getFrom().getWorld();
            World to = ctx.event.getTo().getWorld();

            if(from == null || to == null || from.equals(to)) {
                return;
            }

            raceWorlds.forEach(raceWorld -> {
                if(from.equals(raceWorld.world)) {
                    STEMCraft.runLater(2, () -> {
                        raceWorld.playerLeave(ctx.event.getPlayer());
                        playerReturn.remove(ctx.event.getPlayer());
                    });
                }

                if(to.equals(raceWorld.world)) {
                    STEMCraft.runLater(2, () -> {
                        raceWorld.playerJoin(ctx.event.getPlayer());

                        if(!playerReturn.containsKey(ctx.event.getPlayer())) {
                            playerReturn.put(ctx.event.getPlayer(), ctx.event.getFrom());
                        }
                    });
                }
            });
        });

        SMTabComplete.register("race", () -> {
            return List.of(raceWorlds.stream()
                    .map(RaceWorld::getName)
                    .toArray(String[]::new));
        });

        new SMCommand("race")
            .tabComplete("join", "{race}")
            .tabComplete("leave")
            .tabComplete("checkpoint", "add")
            .tabComplete("checkpoint", "remove")
            .action(ctx -> {
                String sub = ctx.args.get(0).toLowerCase();

                // Sub command - new
                if ("join".equals(sub)) {
                    ctx.checkNotConsole();
                    AtomicBoolean found = new AtomicBoolean(false);

                    raceWorlds.forEach(raceWorld -> {
                        if (raceWorld.getName().equalsIgnoreCase(ctx.args.get(1))) {
                            if(raceWorld.playerExists(ctx.player)) {
                                ctx.returnError("You are already in this race world");
                            }

                            if(!playerReturn.containsKey(ctx.player)) {
                                playerReturn.put(ctx.player, ctx.player.getLocation());
                            }

                            SMCommon.safePlayerTeleport(ctx.player, raceWorld.getWorld().getSpawnLocation());
                            raceWorld.playerJoin(ctx.player);

                            found.set(true);
                        }
                    });

                    if(!found.get()) {
                        ctx.returnError("Race was not found");
                    }
                } else if("leave".equals(sub)) {
                    AtomicBoolean found = new AtomicBoolean(false);

                    raceWorlds.forEach(raceWorld -> {
                        if(raceWorld.playerExists(ctx.player)) {
                            raceWorld.playerLeave(ctx.player);
                            returnPlayer(ctx.player);
                            found.set(true);
                        }
                    });

                    if(!found.get()) {
                        ctx.returnError("You are not in a race");
                    }
                } else if("checkpoint".equals(sub)) {
                    ctx.checkPermission("stemcraft.race.checkpoint");

                    if("add".equals(ctx.args.get(1))) {
                        ctx.checkNotConsole();

                        if(addCheckpoint(ctx.player)) {
                            ctx.returnSuccess("Checkpoint added");
                        } else {
                            ctx.returnError("You are not in a race world");
                        }
                    } else if("remove".equals(ctx.args.get(1))) {
                        ctx.checkNotConsole();

                        if(removeCheckpoint(ctx.player)) {
                            ctx.returnSuccess("Checkpoint removed");
                        } else {
                            ctx.returnError("No checkpoints found nearby");
                        }
                    }
                } else {
                    ctx.returnError("Unknown sub command");
                }
            }).register();

        return true;
    }

    public RaceWorld getRaceWorld(Player player) {
        for (RaceWorld raceWorld : raceWorlds) {
            if (raceWorld.playerExists(player)) {
                return raceWorld;
            }
        }
        return null;
    }

    public boolean addCheckpoint(Player player) {
        RaceWorld raceWorld = getRaceWorld(player);
        if (raceWorld != null) {
            String name = raceWorld.getName();
            List<String> checkpoints = SMConfig.main().getStringList("race." + name + ".checkpoints");
            checkpoints.add(SMCommon.locationToString(player.getLocation()));
            SMConfig.main().set("race." + name + ".checkpoints", checkpoints);
            SMConfig.main().save();

            return true;
        }

        return false;
    }

    public boolean removeCheckpoint(Player player) {
        RaceWorld raceWorld = getRaceWorld(player);
        if (raceWorld != null) {
            String name = raceWorld.getName();
            List<String> checkpoints = SMConfig.main().getStringList("race." + name + ".checkpoints");
            Location playerLocation = player.getLocation();

            for (Iterator<String> iterator = checkpoints.iterator(); iterator.hasNext();) {
                String checkpoint = iterator.next();
                Location checkpointLocation = SMCommon.stringToLocation(checkpoint);

                if (playerLocation.distance(checkpointLocation) <= 16) {
                    iterator.remove();
                    SMConfig.main().set("race." + name + ".checkpoints", checkpoints);
                    SMConfig.main().save();
                    return true;
                }
            }
        }

        return false;
    }

    public void returnPlayer(Player player) {
        if(playerReturn.containsKey(player)) {
            SMCommon.safePlayerTeleport(player, playerReturn.get(player));
            playerReturn.remove(player);
        } else {
            World world = Bukkit.getWorlds().get(0);
            SMCommon.safePlayerTeleport(player, world.getSpawnLocation());
        }
    }
}
