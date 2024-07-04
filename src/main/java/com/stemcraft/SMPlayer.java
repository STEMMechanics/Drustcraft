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

import java.util.List;
import java.util.Map;

public class SMPlayer {
    private final Player player;
    @Setter
    private boolean recentlyJoined = false;

    private PotionEffect previousNightVision = null;

    final int FOREVER_NIGHT_VISION_THRESHOLD = 1000000;

    private static Boolean geyserInstalled = null;
    private static GeyserApi geyserApi = null;

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
        STEMCraft.runLater(() -> player.teleport(location));
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

    public SMPlayerState getLastState(World world, GameMode gameMode) {
        List<SMPlayerState> states = SMPlayerState.find(player, world, gameMode);
        if(!states.isEmpty()) {
            return states.get(0);
        }

        return new SMPlayerState(player);
    }

    public static void saveState(Player player) {
        SMPlayerState state = new SMPlayerState(player);
        state.save();
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
}
