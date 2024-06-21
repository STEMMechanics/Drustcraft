package com.stemcraft;

import lombok.Setter;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class SMPlayer {
    private final Player player;
    @Setter
    private boolean recentlyJoined = false;

    private PotionEffect previousNightVision = null;

    final int FOREVER_NIGHT_VISION_THRESHOLD = 1000000;

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
    public void teleport(Location location) {
        STEMCraft.runLater(() -> {
            this.player.teleport(location);
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
    };

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
    };

    public SMPlayerState getLastState(World world, GameMode gameMode) {
        List<SMPlayerState> states = SMPlayerState.find(player, world, gameMode);
        if(!states.isEmpty()) {
            return states.get(0);
        }

        return new SMPlayerState(player);
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
}
