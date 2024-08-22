package com.stemcraft;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class SMMiniGameInstance {
    @Getter
    private String name;
    @Getter
    private Integer status;
    private final List<String> teams;
    private final HashMap<Player, String> players = new HashMap<>();
    private final HashMap<Player, Location> playerReturn = new HashMap<>();

    public static Integer STATUS_INITIALIZING = 0;
    public static Integer STATUS_READY = 1;
    public static Integer STATUS_RUNNING = 2;
    public static Integer STATUS_ENDED = 3;
    public static Integer STATUS_CLEANUP = 4;

    public SMMiniGameInstance(String name, List<String> teams) {
        this.name = name;
        this.teams = teams;
        this.status = STATUS_INITIALIZING;
    }

    /**
     * Is the instance ready?
     *
     * @return If the instance is ready.
     */
    public boolean isReady() {
        return Objects.equals(status, STATUS_READY);
    }

    /**
     * Return if the specified player is managed by this instance.
     *
     * @param player The player to check.
     * @return If the player is being managed.
     */
    public boolean playerInInstance(Player player) {
        return players.containsKey(player);
    }

    public List<Player> getPlayers() {
        return players.keySet().stream().toList();
    }

    /**
     * Return the team the player is associated within this instance.
     *
     * @param player The player to check.
     * @return The team name or null.
     */
    public String getPlayerTeam(Player player) {
        return players.get(player);
    }

    public List<Player> getTeamPlayers(String team) {
        return players.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(team))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public Map<String, List<Player>> getTeams() {
        return players.entrySet().stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ));
    }

    public Map<String, Integer> getTeamSizes() {
        Map<String, Integer> teamSizes = new HashMap<>();
        for (String team : teams) {
            teamSizes.put(team, 0);
        }
        for (String team : players.values()) {
            teamSizes.merge(team, 1, Integer::sum);
        }
        return teamSizes;
    }

    public String getSmallestTeam() {
        return teams.stream()
                .min(Comparator.comparingInt(team -> getTeamPlayers(team).size()))
                .orElse(teams.isEmpty() ? null : teams.get(0));
    }

    public String getLargestTeam() {
        return teams.stream()
                .max(Comparator.comparingInt(team -> getTeamPlayers(team).size()))
                .orElse(teams.isEmpty() ? null : teams.get(0));
    }

    /**
     * Add a player into this instance.
     * @param player The player to add.
     */
    public void addPlayer(Player player, Location tpLocation) {
        if(!players.containsKey(player)) {
            players.put(player, getSmallestTeam());
            playerReturn.put(player, player.getLocation());

            SMPlayer.teleport(player, tpLocation);
        }
    }

    /**
     * Remove a player from this instance.
     * @param player The player to remove.
     */
    public void removePlayer(Player player) {
        if(players.containsKey(player)) {
            SMPlayer.teleport(player, playerReturn.get(player));

            players.remove(player);
            playerReturn.remove(player);
        }
    }

    /**
     * End this instance of the game.
     */
    public void endGame() {
        status = SMMiniGameInstance.STATUS_ENDED;
    }
}
