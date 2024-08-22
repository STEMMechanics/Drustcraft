package com.stemcraft.minigames;

import com.stemcraft.SMMiniGame;
import com.stemcraft.SMMiniGameInstance;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class SMMiniGameBridge extends SMMiniGame {
    public SMMiniGameBridge() {
        super("Bridge");

        // should use config for this...
        addInstance("Bridge", List.of("Red", "Blue"));
    }

    public void addPlayer(SMMiniGameInstance instance, Player player) {
        if(instance.isReady()) {
            instance.addPlayer(player);
        }
    }

    public void removePlayer(SMMiniGameInstance instance, Player player) {

    }

    public void playerKilled(SMMiniGameInstance instance, Player player, Player killer) {

    }

    public void playerMoved(SMMiniGameInstance instance, Player player, Location location) {

    }
}
