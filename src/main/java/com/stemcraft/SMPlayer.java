package com.stemcraft;

import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.geysermc.geyser.api.GeyserApi;

import java.util.*;

public class SMPlayer {
    private final Player player;
    @Setter
    private boolean recentlyJoined = false;

    private PotionEffect previousNightVision = null;

    final int FOREVER_NIGHT_VISION_THRESHOLD = 1000000;

    private static Boolean geyserInstalled = null;
    private static GeyserApi geyserApi = null;
    private static final Map<UUID, PlayerState> playerFreezeStates = new HashMap<>();

    private static final List<World> ignorePlayerStateWorld = new ArrayList<>();

    private static class PlayerState {
        float walkSpeed;
        float flySpeed;
        GameMode gameMode;
        boolean collidable;

        PlayerState(Player player) {
            this.walkSpeed = player.getWalkSpeed();
            this.flySpeed = player.getFlySpeed();
            this.gameMode = player.getGameMode();
            this.collidable = player.isCollidable();
        }
    }

    public SMPlayer(Player player) {
        this.player = player;
    }

    /**
     * Return the base player object.
     *
     * @return The base player object.
     */
    public Player getBase() {
        return this.player;
    }

    /**
     * Return if the player recently joined the server.
     *
     * @return If the player recently joined the server.
     */
    public boolean recentlyJoined() {
        return this.recentlyJoined;
    }

    /**
     * Teleport a player after 1 tick. This avoids the moved too quickly issue
     *
     * @param location The location to teleport the player
     */
    public static void teleport(Player player, Location location) {
        teleport(player, location, null);
    }

    public static void teleport(Player player, Location location, Runnable runnable) {
        STEMCraft.runLater(() -> {
            player.teleport(location);
            if(runnable != null) {
                runnable.run();
            }
        });
    }

    /**
     * Toggle night vision for the player.
     */
    public boolean hasForeverNightVision() {
        if(getBase().hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            PotionEffect effect = getBase().getPotionEffect(PotionEffectType.NIGHT_VISION);
            if(effect != null) {
                return effect.getDuration() >= FOREVER_NIGHT_VISION_THRESHOLD;
            }
        }

        return false;
    }

    /**
     * Enable night vision for the player.
     */
    public void enableForeverNightVision() {
        if(!hasForeverNightVision()) {
            if(getBase().hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                previousNightVision = getBase().getPotionEffect(PotionEffectType.NIGHT_VISION);
                getBase().removePotionEffect(PotionEffectType.NIGHT_VISION);
            }

            getBase().addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1, true, false));
        }
    }

    /**
     * Disable night vision for the player.
     */
    public void disableForeverNightVision() {
        if(hasForeverNightVision()) {
            getBase().removePotionEffect(PotionEffectType.NIGHT_VISION);
            if(previousNightVision != null) {
                getBase().addPotionEffect(previousNightVision);
                previousNightVision = null;
            }
        }
    }

    public static SMPlayerState getLastState(Player player, World world, GameMode gameMode) {
        if(!ignorePlayerStateWorld.contains(world)) {
            List<SMPlayerState> states = SMPlayerState.find(player, world, gameMode);
            if (!states.isEmpty()) {
                if (states.size() > 5) {
                    STEMCraft.runLater(() -> states.subList(5, states.size()).forEach(SMPlayerState::remove));
                }

                return states.get(0);
            }
        }

        return new SMPlayerState(player);
    }

    public static void saveState(Player player) {
        if(!ignorePlayerStateWorld.contains(player.getWorld())) {
            SMPlayerState state = new SMPlayerState(player);
            state.save();
        }
    }

    public void saveState() {
        SMPlayerState state = new SMPlayerState(player);
        state.save();
    }

    public void setWalkSpeed(float speed) {
        getBase().setWalkSpeed(getRealSpeed(speed, false));
    }

    public void setFlySpeed(float speed) {
        getBase().setFlySpeed(getRealSpeed(speed, true));
    }

    public void resetWalkSpeed() {
        getBase().setWalkSpeed(getDefaultSpeed(false));
    }

    public void resetFlySpeed() {
        getBase().setFlySpeed(getDefaultSpeed(true));
    }

    private float getDefaultSpeed(final boolean isFly) {
        return isFly ? 0.1f : 0.2f;
    }

    private float getRealSpeed(final float speed, final boolean isFly) {
        final float defaultSpeed = getDefaultSpeed(isFly);
        float maxSpeed = 1f;

        if (speed < 1f) {
            return defaultSpeed * speed;
        } else {
            final float ratio = ((speed - 1) / 9) * (maxSpeed - defaultSpeed);
            return ratio + defaultSpeed;
        }
    }

    public static boolean isBedrock(Player player) {
        if (geyserApi == null) {
            if (geyserInstalled != null && !geyserInstalled) {
                return false;
            } else if (geyserInstalled == null) {
                geyserInstalled = Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null;
            }

            if (geyserInstalled) {
                geyserApi = GeyserApi.api();
            }

            if (geyserApi == null) {
                return false;
            }
        }

        return geyserApi.isBedrockPlayer(player.getUniqueId());
    }

    public static Boolean give(Player player, ItemStack item, Boolean dropOnFail, Boolean quiet) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            if (dropOnFail) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
                return true;
            } else if (!quiet) {
                SMMessenger.send(SMMessenger.MessageType.ERROR, player, "You don't have enough room in your inventory!");
            }

            return false;
        }

        return true;
    }

    public static Boolean give(Player player, ItemStack item) {
        return give(player, item, false, false);
    }

    /**
     * Freeze the player and prevent from moving or being attacked
     * @param player The player to freeze
     */
    public static void freeze(Player player) {
        UUID playerUUID = player.getUniqueId();

        // Check if the player is already frozen
        if (playerFreezeStates.containsKey(playerUUID)) {
            return; // Player is already frozen, do nothing
        }

        // Save current state and apply freeze effects
        playerFreezeStates.put(playerUUID, new PlayerState(player));

        player.setWalkSpeed(0);
        player.setFlySpeed(0);
        player.setGameMode(GameMode.ADVENTURE);
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 128));
        player.setCollidable(false);
    }

    /**
     * Unfreeze the player if they have been frozen
     * @param player The player to unfreeze
     */
    public static void unfreeze(Player player) {
        UUID playerUUID = player.getUniqueId();
        PlayerState originalState = playerFreezeStates.remove(playerUUID);

        if (originalState != null) {
            // Restore original state
            player.setWalkSpeed(originalState.walkSpeed);
            player.setFlySpeed(originalState.flySpeed);
            player.setGameMode(originalState.gameMode);
            player.removePotionEffect(PotionEffectType.JUMP);
            player.setCollidable(originalState.collidable);
        }
    }

    /**
     * Return if the player is currently frozen
     * @param player The player to check
     * @return If the player is frozen
     */
    public static boolean isFrozen(Player player) {
        return playerFreezeStates.containsKey(player.getUniqueId());
    }
}
