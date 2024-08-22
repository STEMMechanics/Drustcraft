package com.stemcraft;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SMMiniGameArena {
    private SMMiniGame owner;
    @Getter
    private String name;

    @Getter @Setter
    private int status;
    private List<Player> players = new ArrayList<>();

    @Getter
    @Setter
    private int minPlayers = 2;

    @Getter
    @Setter
    private int maxPlayers = 99;

    private Map<String, SMRegion> regions = new HashMap<>();
    private Map<String, Location> locations = new HashMap<>();
    private final Map<UUID, Map<String, Object>> playerData = new ConcurrentHashMap<>();

    private boolean allowBlockBreak = true;
    private boolean allowBlockPlace = true;
    private boolean allowBlockInteract = true;
    private final Map<Location, BlockState> savedBlockStates = new LinkedHashMap<>();

    private static class TypedValue<T> {
        private final T value;

        TypedValue(T value) {
            this.value = value;
        }

        T getValue() {
            return value;
        }
    }

    @FunctionalInterface
    public interface PlayerMoveCallback {
        void onMove(SMMiniGameArena arena, Player player);
    }

    private List<PlayerMoveCallback> moveCallbackList = new ArrayList<>();

    public static int STATUS_CONFIGURATION_ERROR = -1;
    public static int STATUS_INITIALIZING = 1;
    public static int STATUS_WAITING_PLAYERS = 1;
    public static int STATUS_IN_PROGRESS = 1;

    /**
     * Constructor
     * @param owner The mini-game owner of this arena
     * @param name The name of this arena
     */
    public SMMiniGameArena(SMMiniGame owner, String name) {
        this.owner = owner;
        this.name = name;
        this.status = STATUS_INITIALIZING;
    }

    public boolean isEnabled() {
        return true;
    }

    /**
     * Get the configuration path of this arena
     * @return The configuration path prefix
     */
    public String getConfigPath(String... additionalPaths) {
        StringBuilder path = new StringBuilder("mini-games.")
                .append(owner.getName())
                .append(".arenas.")
                .append(name);

        for (String additionalPath : additionalPaths) {
            path.append(".").append(additionalPath);
        }

        return path.toString();
    }

    public <T> T getPlayerData(Player player, String key, Class<T> type) {
        Object value = playerData.getOrDefault(player.getUniqueId(), Map.of()).get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    public <T> T getPlayerData(Player player, String key, Class<T> type, T defaultValue) {
        T value = getPlayerData(player, key, type);
        return value != null ? value : defaultValue;
    }

    public void setPlayerData(Player player, String key, Object data) {
        playerData.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(key, data);
    }

    public boolean hasPlayerData(Player player, String key) {
        return playerData.containsKey(player.getUniqueId()) &&
                playerData.get(player.getUniqueId()).containsKey(key);
    }

    public void clearPlayerData(Player player) {
        playerData.remove(player.getUniqueId());
    }

    public void clearAllPlayerData() {
        playerData.clear();
    }

    public void removePlayerData(Player player, String key) {
        playerData.computeIfPresent(player.getUniqueId(), (k, v) -> {
            v.remove(key);
            return v.isEmpty() ? null : v;
        });
    }

    /**
     * Get a list of active players in the arena
     * @return The player list
     */
    public List<Player> getActivePlayers() {
        return players.stream()
                .filter(player -> player.getGameMode() == GameMode.SURVIVAL)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of non-active players in the arena
     * @return The player list
     */
    public List<Player> getNonActivePlayers() {
        return players.stream()
                .filter(player -> player.getGameMode() == GameMode.SPECTATOR)
                .collect(Collectors.toList());
    }

    /**
     * Set a player to no longer be active in the arena
     * @param player The player to change
     */
    public void setNotActive(Player player) {
        if(players.contains(player)) {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    /**
     * Set a player to be active in the arena
     * @param player The player to change
     */
    public void setActive(Player player) {
        if(players.contains(player)) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    /**
     * Check if a player is active in the arena
     * @param player The player to check
     * @return If the player is active
     */
    public boolean isActive(Player player) {
        if(players.contains(player)) {
            return player.getGameMode() == GameMode.SURVIVAL;
        }

        return false;
    }

    /**
     * Send a message to all players within this arena
     * @param message The message to send
     */
    public void messageAll(SMMessenger.MessageType type, String message) {
        players.forEach(p -> {
            SMMessenger.send(type, p, message);
        });
    }

    public void messageAll(String message) {
        messageAll(SMMessenger.MessageType.ANNOUNCEMENT, message);
    }

    /**
     * Get an arena region
     * @param name The region name
     * @return The region object or null
     */
    public SMRegion getRegion(String name) {
        return regions.get(name);
    }

    /**
     * Set an arena region
     * @param name The region name
     * @param region The region object
     */
    public void setRegion(String name, SMRegion region) {
        if(name.equalsIgnoreCase("bounds")) {
            region.onBlockBreak((block, player) -> {
                if (allowBlockBreak) {
                    saveBlockState(block);
                }

                return !allowBlockBreak;
            });
        }

        regions.put(name, region);
    }

    /**
     * Remove a region from the arena
     * @param name The name of the region
     */
    public void removeRegion(String name) {
        regions.remove(name);
    }

    /**
     * Check if a region exists
     * @param name The name of the region
     * @return If the region exists
     */
    public boolean regionExists(String name) {
        return getRegion(name) != null;
    }





    public void onPlayerMove(PlayerMoveCallback callback) {
        moveCallbackList.add(callback);
    }


    public void saveBlockState(Block block) {
        saveBlockState(block.getLocation());
    }

    public void saveBlockState(Location location) {
        saveBlockState(location, false);
    }

    public void saveBlockState(Location location, Boolean override) {
        if (override || !savedBlockStates.containsKey(location)) {
            BlockState state = location.getBlock().getState();
            savedBlockStates.put(location, state);
        }
    }

    public void reset() {
        for (Map.Entry<Location, BlockState> entry : savedBlockStates.entrySet()) {
            BlockState state = entry.getValue();
            state.update(true, false); // Update the block to its saved state
        }
        savedBlockStates.clear(); // Clear the saved states after resetting
    }

    public void fireworks() {

    }


    public Location getLocation(String name) {

    }

    public void setLocation(String name, Location location) {

    }

    public void removeLocation(String name) {

    }

    public void locationExists(String name) {

    }

}
