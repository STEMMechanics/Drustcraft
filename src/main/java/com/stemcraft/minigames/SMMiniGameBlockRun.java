package com.stemcraft.minigames;

import com.stemcraft.*;
import com.stemcraft.bridges.SMWorldEdit;
import com.stemcraft.utils.SMUtilsLocation;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SMMiniGameBlockRun extends SMMiniGame {

    /**
     * /blockrun join <arena>
     * /blockrun start <arena>
     * /blockrun stop <arena>
     * /blockrun list
     * /blockrun create <arena>
     * /blockrun delete <arena>
     * /blockrun activate <arena>
     * /blockrun deactivate <arena>
     * /blockrun set <arena> bounds
     * /blockrun set <arena> lobby
     * /blockrun set <arena> pit
     * /blockrun set <arena> spawn
     */

    /**
     * Constructor
     */
    public SMMiniGameBlockRun() {
        super("block-run");
    }

    @Override
    public boolean onArenaEnable(SMMiniGameArena arena, CommandSender player) {
        if(!onArenaEnable(arena, player)) {
            return false;
        }


//            SMRegion lobby = SMRegion.load(configPrefix + ".lobby");
//            if(lobby == null) {
//                STEMCraft.error("Block run arena " + arenaName + " was not loaded as the arena lobby is not configured");
//                continue;
//            }

//            SMRegion pit = SMRegion.load(configPrefix + ".pit");
//            if(pit == null) {
//                STEMCraft.error("Block run arena " + arenaName + " was not loaded as the arena pit is not configured");
//                continue;
//            }

            Location spawn = SMUtilsLocation.fromConfig(configPrefix + ".spawn", world);
            if(spawn == null) {
                STEMCraft.error("Block run arena " + arenaName + " was not loaded as the arena spawn is not configured");
                continue;
            }

            SMMiniGameArena arena = addArena(arenaName, world, null);

            bounds.onPlayerExit(player -> {
                // player is out of the game
                arena.messageAll(player.getName() + " has gone out of bounds");
                arena.setNotActive(player);
            });

//            pit.onPlayerEnter(player -> {
//                // player is out of the game
//                arena.setNotActive(player);
//
//                // check if they were the last living player, if so they won the game
//                Integer activePlayers = arena.getActivePlayers().size();
//
//                if(activePlayers == 2) {
//                    addToScore(configPrefix, player, 1);
//                }
//
//                if(activePlayers == 1) {
//                    addToScore(configPrefix, player, 2);
//                }
//
//                if(activePlayers == 0) {
//                    addToScore(configPrefix, player, 3);
//                    arena.messageAll(player.getName() + " was the last survivor!");
//                    arena.fireworks();
//                    STEMCraft.runLater(200, arena::reset);
//                } else {
//                    arena.messageAll(player.getName() + " has fallen into the pit");
//                }
//            });

            arena.setMinPlayers(SMConfig.getInt(configPrefix + ".min-players", 2));
            arena.setMaxPlayers(SMConfig.getInt(configPrefix + ".max-players", 40));
            arena.setRegion("bounds", bounds);
            arena.setRegion("lobby", lobby);
            arena.setRegion("pit", lobby);
            arena.setLocation("spawn", spawn);

            arena.onPlayerMove((playerArena, player) -> {
                if(player.getGameMode() == GameMode.SURVIVAL) {
                    Block blockBelowPlayer = player.getLocation().subtract(0, 1, 0).getBlock();
                    if(blockBelowPlayer.getType().isSolid()) {
                        STEMCraft.runLater(8, () -> {
                            playerArena.saveBlockState(blockBelowPlayer);
                            blockBelowPlayer.setType(Material.AIR);
                        });
                    }
                }
            });

            // arena is ready!
            arena.setStatus(SMMiniGameArena.STATUS_WAITING_PLAYERS);
        }
    }

    private void addToScore(String configPath, Player player, Integer score) {
        Integer previousScore = SMConfig.getInt(configPath + ".scores." + player.getUniqueId(), 0);
        SMConfig.set(configPath + ".scores." + player.getUniqueId(), previousScore + score);
    }

    public boolean onActivate(SMMiniGameArena arena) {

    }

    public void onSetCommand(String setCommand, CommandSender sender, SMMiniGameArena arena) {
        if(!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by players");
            return;
        }

        Player player = (Player)sender;

        List<Location> points = SMWorldEdit.getSelection(player);
        if(points == null || points.isEmpty()) {
            player.sendMessage("You do not have an area selected with WorldEdit");
            return;
        }

        for(Location location : points) {
            if(!location.getWorld().equals(arena.getWorld())) {
                player.sendMessage("Your WorldEdit selection is not in the same world as the arena");
                // ?????? is this even required?? can we just get the world from the bounds location?
                return;
            }
        }

        SMRegion newBounds = SMRegion.create(null, points);
        if(newBounds != null) {
            SMRegion oldBounds = arena.getRegion("bounds");
            if (oldBounds != null) {
                oldBounds.delete();
            }

            arena.setRegion("bounds", newBounds);
            newBounds.save(configPath(arena, "bounds"));
        } else {
            player.sendMessage("Could not set the arena bounds because a server error occurred");
        }
    }
}
