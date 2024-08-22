package com.stemcraft;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;


import java.util.*;

public class SMScoreboard {
    private static final Map<UUID, SMScoreboard> playerScoreboards = new HashMap<>();
    private static int objectiveCounter = 0;

    private final Map<String, Integer> lines;
    private final Set<UUID> viewers;
    private Scoreboard scoreboard;
    private Objective objective;

    /**
     * Constructor
     * @param title The title of the scoreboard
     */
    public SMScoreboard(String title) {
        String formattedTitle = ChatColor.translateAlternateColorCodes('&', title);
        this.lines = new LinkedHashMap<>();
        this.viewers = new HashSet<>();
        String objectiveName = "smboard" + (++objectiveCounter);

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if(manager != null) {
            this.scoreboard = manager.getNewScoreboard();
            this.objective = scoreboard.registerNewObjective(objectiveName, Criteria.DUMMY, formattedTitle);
            this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
    }

    /**
     * Set a scoreboard line
     * @param line The line index to set
     * @param content The string value of the time
     */
    public void setLine(int line, String content) {
        content = ChatColor.translateAlternateColorCodes('&', content);
        lines.put(content, line);
        updateScoreboard();
    }

    /**
     * Set the line with a following green tick
     * @param line The line index to set
     * @param content The string value of the time
     */
    public void setLineWithTick(int line, String content) {
        setLine(line, content + " §a✔");
    }

    /**
     * Set the line with a following red cross
     * @param line The line index to set
     * @param content The string value of the time
     */
    public void setLineWithCross(int line, String content) {
        setLine(line, content + " §c✘");
    }

    /**
     * Set the line with a progress bar
     * @param line The line index to set
     * @param current The current progress bar size
     * @param max The maximum size of the progress bar
     * @param color The color of filled progress bar squares
     */
    public void setProgressBar(int line, int current, int max, ChatColor color) {
        StringBuilder progressBar = new StringBuilder();
        progressBar.append("  ");

        for (int i = 0; i < current; i++) {
            progressBar.append(color).append("■");
        }

        for (int i = current; i < max; i++) {
            progressBar.append(ChatColor.GRAY).append("■");
        }

        setLine(line, progressBar.toString());
    }

    public void setProgressBar(int line, int current, int max) {
        setProgressBar(line, current, max, ChatColor.AQUA);
    }

    /**
     * Set the line with a heart based progress bar
     * @param line The line index to set
     * @param current The current progress bar size
     * @param max The maximum size of the progress bar
     */
    public void setHeartProgressBar(int line, int current, int max) {
        StringBuilder progressBar = new StringBuilder();

        // Add indentation
        progressBar.append("  ");

        for (int i = 0; i < current; i++) {
            progressBar.append(ChatColor.RED).append("❤");
        }

        for (int i = current; i < max; i++) {
            progressBar.append(ChatColor.GRAY).append("❤");
        }

        setLine(line, progressBar.toString());
    }

    /**
     * Remove a scoreboard line
     * @param line The line index to remove
     */
    public void removeLine(int line) {
        lines.entrySet().removeIf(entry -> entry.getValue() == line);
        updateScoreboard();
    }

    /**
     * Update the scoreboard for associated players
     */
    private void updateScoreboard() {
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        for (Map.Entry<String, Integer> entry : lines.entrySet()) {
            objective.getScore(entry.getKey()).setScore(entry.getValue());
        }

        for (UUID uuid : viewers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.setScoreboard(scoreboard);
            }
        }
    }

    /**
     * Add a player to this scoreboard
     * @param player The player to add
     */
    public void addPlayer(Player player) {
        UUID playerUUID = player.getUniqueId();

        // Remove player from their previous scoreboard, if any
        SMScoreboard previousScoreboard = playerScoreboards.remove(playerUUID);
        if (previousScoreboard != null && previousScoreboard != this) {
            previousScoreboard.removePlayerInternal(playerUUID);
        }

        viewers.add(playerUUID);
        player.setScoreboard(scoreboard);
        playerScoreboards.put(playerUUID, this);
    }

    /**
     * Remove a player from the scoreboard
     * @param player The player to remove
     */
    public void removePlayer(Player player) {
        removePlayerInternal(player.getUniqueId());
        ScoreboardManager sbManager = Bukkit.getScoreboardManager();
        if(sbManager != null) {
            player.setScoreboard(sbManager.getNewScoreboard());
        }
        playerScoreboards.remove(player.getUniqueId());
    }

    private void removePlayerInternal(UUID playerUUID) {
        viewers.remove(playerUUID);
        if (viewers.isEmpty()) {
            destroy();
        }
    }

    /**
     * Destroy the scoreboard
     */
    private void destroy() {
        for (UUID uuid : new HashSet<>(viewers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                removePlayer(player);
            }
        }

        if (scoreboard != null) {
            for (Objective obj : scoreboard.getObjectives()) {
                obj.unregister();
            }
            for (Team team : scoreboard.getTeams()) {
                team.unregister();
            }
        }
        lines.clear();
        scoreboard = null;
        objective = null;
    }

    /**
     * Remove the scoreboard from a player without knowing the scoreboard
     * @param player The player to remove scoreboard from
     */
    public static void remove(Player player) {
        SMScoreboard scoreboard = playerScoreboards.remove(player.getUniqueId());
        if (scoreboard != null) {
            scoreboard.removePlayer(player);
        }
    }

    /**
     * Get the scoreboard associated to a player
     * @param player The player to check
     * @return The scoreboard used by the player or null
     */
    public static SMScoreboard get(Player player) {
        return playerScoreboards.get(player.getUniqueId());
    }
}