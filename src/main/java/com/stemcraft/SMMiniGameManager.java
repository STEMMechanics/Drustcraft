package com.stemcraft;

import java.util.HashMap;

public class SMMiniGameManager {
    private HashMap<String, SMMiniGame> minigames = new HashMap<>();

    /**
     * Initialize the mini-game manager and load the mini-games
     */
    public void initialize() {
        STEMCraft.loadPackageClasses("minigames", SMMiniGame.class, instance -> {
            if(instance.initialize()) {
                minigames.put(instance.getName(), instance);
            }

            return true;
        });
    }
}
