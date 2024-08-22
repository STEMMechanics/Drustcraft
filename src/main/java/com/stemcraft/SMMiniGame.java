package com.stemcraft;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

@Getter
public class SMMiniGame {
    /**
     * The mini-game name
     */
    private final String name;

    /**
     * The formatted mini-game name
     */
    private final String formattedName;

    /**
     * The mini-game configuration path
     */
    @Getter
    private final String configPath;

    /**
     * The mini-game command object
     */
    private final SMCommand command;

    /**
     * The mini-game arenas
     */
    private final HashMap<String, SMMiniGameArena> arenas = new HashMap<>();

    /**
     * The arena tab completion name
     */
    private final String arenaTabCompletionName;

    /**
     * True if arenas use a lobby
     */
    protected boolean arenaUsesLobby = true;

    /**
     * The mini-game arena set options
     */
    private final HashMap<String, BiConsumer<SMMiniGameArena, Player>> setOptions = new HashMap<>();


    /**
     * Constructor
     * @param name The mini-game name
     */
    public SMMiniGame(String name) {
        this.name = name.toLowerCase();
        formattedName = name.replaceAll("[^a-z]", "");

        arenaTabCompletionName = "mini-game." + this.name + ".arenas";
        SMTabCompletion<String> tabCompleter = new SMTabCompletion<String>(arenaTabCompletionName, (unused) -> arenas.keySet().stream().toList());
        STEMCraft.addTabCompletion(arenaTabCompletionName, tabCompleter);

        command = new SMCommand(formattedName);
        command.tabCompletion("join", "{" + arenaTabCompletionName + "}");
        command.tabCompletion("start", "{" + arenaTabCompletionName + "}");
        command.tabCompletion("stop", "{" + arenaTabCompletionName + "}");
        command.tabCompletion("create");
        command.tabCompletion("delete", "{" + arenaTabCompletionName + "}");
        command.tabCompletion("activate", "{" + arenaTabCompletionName + "}");
        command.tabCompletion("deactivate", "{" + arenaTabCompletionName + "}");
        command.tabCompletion("list");
        command.setExecutionHandler(this::onCommand);
        command.register();

        configPath = "config.mini-games." + this.name;

        addSetOption("bounds", (SMMiniGameArena arena, Player player) -> {
            // not implemented
        });

        addSetOption("spawn", (SMMiniGameArena arena, Player player) -> {
            // not implemented
        });

        List<String> arenaList = SMConfig.getKeys(configPath + ".arenas");
        for (String arenaName : arenaList) {
            SMMiniGameArena arena = new SMMiniGameArena(this, arenaName);
            arenas.put(arenaName, arena);

            onArenaEnable(arena);
        }
    }

    /**
     * Return an arena based on name or null
     * @param name The name of the arena
     * @return The arena object or null
     */
    protected SMMiniGameArena getArena(String name) {
        return arenas.get(name);
    }

    /**
     * Add an arena set option for configuration
     * @param option The option to add
     */
    protected void addSetOption(String option, BiConsumer<SMMiniGameArena, Player> function) {
        setOptions.put(option.toLowerCase(), function);
        command.tabCompletion("set", "{" + arenaTabCompletionName + "}", option);
    }

    /**
     * Mini-game command handler
     * @param context The command context data
     */
    public void onCommand(SMCommandContext ctx) {
        String action = ctx.args.shift("join");
        String arenaName = ctx.args.shift();
        boolean arenaExists = arenas.containsKey(arenaName);

        if(action.equalsIgnoreCase("join")) {
            SMMiniGameArena arena = getArena(arenaName);


        }
    }

    /**
     * When an arena is enabled
     * @param arena The arena being enabled
     */
    public void onArenaEnable(SMMiniGameArena arena) {
        onArenaEnable(arena, Bukkit.getConsoleSender());
    }

    public boolean onArenaEnable(SMMiniGameArena arena, CommandSender player) {
        String worldName = SMConfig.getString(arena.getConfigPath("world"));
        if(worldName == null) {
            SMMessenger.send(SMMessenger.MessageType.ERROR, player, "{mini-game} arena {arena-name} could not be enabled as the world name is not configured", "{mini-game}", name, "{arena-name}", arena.getName());
            arena.setStatus(SMMiniGameArena.STATUS_CONFIGURATION_ERROR);
            return false;
        }

        World world = Bukkit.getWorld(worldName);
        if(world == null) {
            SMMessenger.send(SMMessenger.MessageType.ERROR, player, "{mini-game} arena {arena-name} could not be enabled as the world {world-name} is not loaded", "{mini-game}", name, "{arena-name}", arena.getName(), "{world-name}", worldName);
            arena.setStatus(SMMiniGameArena.STATUS_CONFIGURATION_ERROR);
            return false;
        } else {
            arena.setWorld(world);
        }

        SMRegion bounds = SMRegion.loadFromConfig(arena.getConfigPath("bounds"));
        if(bounds == null) {
            SMMessenger.send(SMMessenger.MessageType.ERROR, player, "{mini-game} arena {arena-name} could not be enabled as no boundary is set", "{mini-game}", name, "{arena-name}", arena.getName());
            arena.setStatus(SMMiniGameArena.STATUS_CONFIGURATION_ERROR);
            return false;
        } else {
            arena.setRegion("bounds", bounds);
        }

        if(arenaUsesLobby) {
            SMRegion lobby = SMRegion.loadFromConfig(arena.getConfigPath("lobby"));
            if (lobby == null) {
                SMMessenger.send(SMMessenger.MessageType.ERROR, player, "{mini-game} arena {arena-name} could not be enabled as no lobby is set", "{mini-game}", name, "{arena-name}", arena.getName());
                arena.setStatus(SMMiniGameArena.STATUS_CONFIGURATION_ERROR);
                return false;
            } else {
                arena.setRegion("lobby", lobby);
            }
        }

        SMRegion spawn = SMRegion.loadFromConfig(arena.getConfigPath("spawn"));
        if(spawn == null) {
            SMMessenger.send(SMMessenger.MessageType.ERROR, player, "{mini-game} arena {arena-name} could not be enabled as no spawn region is set", "{mini-game}", name, "{arena-name}", arena.getName());
            arena.setStatus(SMMiniGameArena.STATUS_CONFIGURATION_ERROR);
            return false;
        } else {
            arena.setRegion("spawn", spawn);
        }

        arena.setStatus(SMMiniGameArena.STATUS_WAITING_PLAYERS);
        return true;
    }

    /**
     * Called when a player joins an arena
     * @param arena The arena being joined
     * @param player The player joining
     * @param afterJoin Runnable after the player joins the arena
     * @return The location to teleport the player or null to cancel the request
     */
    public Location onPlayerJoinArena(SMMiniGameArena arena, Player player, Runnable afterJoin) {
        int arenaStatus = arena.getStatus();

        if(arenaStatus == SMMiniGameArena.STATUS_WAITING_PLAYERS) {
            if(arenaUsesLobby) {
                SMRegion lobby = arena.getRegion("lobby");
                SMPlayer.teleport(player, lobby.randomLocation());

                arena.messageAll(SMMessenger.MessageType.INFO, player.getName() + " has joined the game!");

                // are we now at the minimum number to begin???
            } else {
                SMRegion spawn = arena.getRegion("spawn");
                SMPlayer.teleport(player, spawn.randomLocation());

//                arena.messageAll(SMMessenger.MessageType.INFO, player.getName() + " has joined the game!");
            }
        } else if(arenaStatus == SMMiniGameArena.STATUS_IN_PROGRESS) {
            Location spectatorLoc = arena.getLocation("spectator");
            if(spectatorLoc == null) {
                SMRegion spawn = arena.getRegion("spawn");
                spectatorLoc = spawn.randomLocation();
            }

            SMPlayer.teleport(player, spectatorLoc, () -> {
                player.setGameMode(GameMode.SPECTATOR);
                SMMessenger.send(SMMessenger.MessageType.INFO, player, "A game is currently in progress. You will join the next round");
            });
        } else {

        }

        return null;
    }

    /**
     * Called after a player joins (and teleports into) an arena
     * @param arena The arena the player joined
     * @param player The player that joined the arena
     */
    public void afterPlayerJoinArena(SMMiniGameArena arena, Player player) {
        // override
    }









    /**
     * Initialize the mini-game.
     * @return If the initialization was successful
     */
    public boolean initialize() {
        return true;
    }


    public SMMiniGameArena addArena(String name, World world, List<String> teams) {
        SMMiniGameArena arena = new SMMiniGameArena(name, world);
        arenas.put(name, arena);

        return arena;
    }

    /**
     * Can a player join an existing arena?
     * @param arena The arena the player wants to join
     * @param player The player in question
     * @return True/False is the player can join
     */
    public boolean CanPlayerJoinArena(SMMiniGameArena arena, Player player) {
        if(arena.getStatus() == SMMiniGameArena.STATUS_WAITING_PLAYERS) {
            return true;
        }

        return false;
    }

    /**
     * Called when a player joins an arena
     * @param arena The arena being joined
     * @param player The player joining
     */
    public void OnPlayerJoinArena(SMMiniGameArena arena, Player player) {
        Location lobbyLocation = arena.getLocation("lobby");
        if(lobbyLocation == null) {
            SMMessenger.send(SMMessenger.MessageType.ERROR, player, "You cannot join this mini-game as it is not configured correctly.");
        } else {
            SMPlayer.teleport(player, lobbyLocation);
            arena.messageAll(SMMessenger.MessageType.ERROR, player.getName() + " has joined the game");

            // Set team???

            // Check if we are at minimum starting player count, if so, start countdown
        }
    }

    /**
     * Called when a player leaves an arena
     * @param arena The arena left
     * @param player The player leaving
     */
    public void OnPlayerLeaveArena(SMMiniGameArena arena, Player player) {
        // If the status is waiting players and we have dropped below the minimum, stop countdown
        // If the game is running and we drop below the minumum, end game
    }

    /**
     * Player has left the arena boundry (usually assumes death)
     * @param arena The arena
     * @param player The player
     */
    public void OnPlayerOutsideBounds(SMMiniGameArena arena, Player player) {
        OnPlayerDeath(arena, player);
    }

    /**
     * Called when a player dies
     * @param arena The arena
     * @param player The player who died or null
     */
    public void OnPlayerDeath(SMMiniGameArena arena, Player player, Player killer) {

    }

    public void OnPlayerDeath(SMMiniGameArena arena, Player player) {
        OnPlayerDeath(arena, player, null);
    }

    public boolean OnPlayerPlacesBlock(SMMiniGameArena arena, Player player, Location blockLocation) {
        return false;
    }

    public boolean OnPlayerBreaksBlock(SMMiniGameArena arena, Player player, Location blockLocation) {
        return false;
    }

    public boolean OnPlayerInteractsWithBlock(SMMiniGameArena arena, Player player, Location blockLocation) {
        return false;
    }

    public void OnPlayerMoves(SMMiniGameArena arena, Player player, Location moveLocation) {

    }
}
