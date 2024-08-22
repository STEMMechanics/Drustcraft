package com.stemcraft;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SMBossBar {
    private static final Map<UUID, SMBossBar> playerBossBars = new HashMap<>();

    private final BossBar bossBar;
    private final String title;

    public SMBossBar(String title, BarColor color, BarStyle style) {
        this.title = title;
        this.bossBar = Bukkit.createBossBar(title, color, style);
    }

    public void addViewer(Player player) {
        // Remove player from their previous boss bar, if any
        SMBossBar previousBossBar = playerBossBars.remove(player.getUniqueId());
        if (previousBossBar != null && previousBossBar != this) {
            previousBossBar.removeViewerInternal(player);
        }

        bossBar.addPlayer(player);
        playerBossBars.put(player.getUniqueId(), this);
    }

    public void removeViewer(Player player) {
        removeViewerInternal(player);
        playerBossBars.remove(player.getUniqueId());
    }

    private void removeViewerInternal(Player player) {
        bossBar.removePlayer(player);
        if (bossBar.getPlayers().isEmpty()) {
            destroy();
        }
    }

    public void setTitle(String title) {
        bossBar.setTitle(title);
    }

    public void setProgress(double progress) {
        bossBar.setProgress(progress);
    }

    public void setColor(BarColor color) {
        bossBar.setColor(color);
    }

    public void setStyle(BarStyle style) {
        bossBar.setStyle(style);
    }

    public void setVisible(boolean visible) {
        bossBar.setVisible(visible);
    }

    public void destroy() {
        for (Player player : bossBar.getPlayers()) {
            playerBossBars.remove(player.getUniqueId());
        }
        bossBar.removeAll();
    }

    public static void removePlayer(Player player) {
        SMBossBar bossBar = playerBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removeViewer(player);
        }
    }

    public static SMBossBar getBossBar(Player player) {
        return playerBossBars.get(player.getUniqueId());
    }
}