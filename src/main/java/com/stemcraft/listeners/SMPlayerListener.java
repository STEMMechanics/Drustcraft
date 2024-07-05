package com.stemcraft.listeners;

import com.stemcraft.*;
import com.stemcraft.utils.SMUtilsWorld;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class SMPlayerListener extends SMListener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        SMPlayer smPlayer = new SMPlayer(event.getPlayer());

        smPlayer.setRecentlyJoined(true);
        STEMCraft.runOnceDelay("player_recently_joined_" + event.getPlayer().getUniqueId(), 100, () -> smPlayer.setRecentlyJoined(false));

        smPlayer.disableForeverNightVision();

        STEMCraft.runLater(() -> {
            if (!event.getPlayer().hasPermission("stemcraft.hub.override")) {
                if(event.getPlayer().getWorld() == Bukkit.getWorld("world")) {
                    updateGameMode(event.getPlayer(), event.getPlayer().getWorld());
                }

                SMPlayer.teleport(event.getPlayer(), Objects.requireNonNull(Bukkit.getWorld("world")).getSpawnLocation());
            } else {
                SMPlayerState state = smPlayer.getLastState(event.getPlayer().getWorld(), event.getPlayer().getGameMode());
                state.restore();
            }

            STEMCraft.runLater(20, () -> {
                SMRegion.updatePlayerRegions(event.getPlayer());
                SMSkipNight.update(event.getPlayer().getWorld());
            });
        });
    }

    @EventHandler
    public void onPlayerChangesGameMode(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();

        SMPlayer smPlayer = new SMPlayer(player);
        if(player.getGameMode().equals(GameMode.SURVIVAL) || player.getGameMode().equals(GameMode.CREATIVE)) {
            smPlayer.disableForeverNightVision();
            smPlayer.resetFlySpeed();
            smPlayer.resetWalkSpeed();

            smPlayer.saveState();
        }

        STEMCraft.runLater(() -> {
            SMPlayerState state = smPlayer.getLastState(player.getWorld(), event.getNewGameMode());
            state.restore();

            SMSkipNight.update(player.getWorld());
        });
    }

    @EventHandler
    public void onPlayerTeleports(PlayerTeleportEvent event) {
        if(event.getFrom().getWorld() != Objects.requireNonNull(event.getTo()).getWorld()) {
            SMPlayer smPlayer = new SMPlayer(event.getPlayer());
            smPlayer.disableForeverNightVision();
            smPlayer.resetFlySpeed();
            smPlayer.resetWalkSpeed();

            smPlayer.saveState();
            if(!SMUtilsWorld.getOverworldName(Objects.requireNonNull(event.getFrom().getWorld()).getName()).equalsIgnoreCase(SMUtilsWorld.getOverworldName(Objects.requireNonNull(event.getTo().getWorld()).getName()))) {
                STEMCraft.runLater(() -> {
                    SMPlayerState state = smPlayer.getLastState(event.getTo().getWorld(), event.getPlayer().getGameMode());
                    state.restore();
                    updateGameMode(event.getPlayer(), event.getTo().getWorld());
                });
            } else {
                updateGameMode(event.getPlayer(), event.getTo().getWorld());
            }

            STEMCraft.runLater(20, () -> SMSkipNight.update(event.getPlayer().getWorld()));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        SMPlayer smPlayer = new SMPlayer(event.getPlayer());
        smPlayer.saveState();
        SMSkipNight.update(event.getPlayer().getWorld());
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if(event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            STEMCraft.runLater(() -> SMSkipNight.update(event.getPlayer().getWorld()));
        }
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        if(event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            STEMCraft.runLater(() -> SMSkipNight.update(event.getPlayer().getWorld()));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if(event.getEntity().getGameMode() == GameMode.SURVIVAL) {
            SMSkipNight.update(event.getEntity().getWorld());
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        for(SMRegion region : SMRegion.playerRegions(event.getPlayer())) {
            if(region.getAllowDrops() == Boolean.FALSE) {
                event.setCancelled(true);
                return;
            }
        }

        ItemStack item = event.getItemDrop().getItemStack();

        if(SMItem.destroyOnDrop(item)) {
            event.getItemDrop().remove();
        }
    }

    /**
     * When a player moves event
     * @param event The event
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if(SMRegion.updatePlayerRegions(event.getPlayer(), event.getTo())) {
            event.setCancelled(true);
        }
    }

    private void updateGameMode(Player player, World world) {
        SMWorld smWorld = new SMWorld(world);

        String newGameModeName = smWorld.getGameMode();
        if(!Objects.equals(newGameModeName, "")) {
            GameMode newGameMode = GameMode.valueOf(newGameModeName.toUpperCase());
            player.setGameMode(newGameMode);
        }
    }
}
