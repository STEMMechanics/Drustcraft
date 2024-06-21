package com.stemcraft.config;

import lombok.Getter;

public class SMWorldConfig extends SMConfig {
    @Getter
    private String worldName;

    @Getter
    private String gameMode;

    @Override
    public void load(String worldName) {
        this.worldName = worldName;
//        this.gameMode = SMConfigManager.main().getString("worlds." + worldName + ".gameMode", "SURVIVAL");
    }

//    @Override
//    public void save() {
//        SMConfigManager.main().set("worlds." + worldName + ".gameMode", gameMode);
//    }
}
