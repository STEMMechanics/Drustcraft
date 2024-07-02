package com.stemcraft.tabcompletions;

import com.stemcraft.SMTabCompletion;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class SMTabCompletionGameMode extends SMTabCompletion<GameMode> {

    public SMTabCompletionGameMode() {
        super("gamemode");
    }

    @Override
    public List<String> completions(String arg) {
        String[] gameMode = {"adventure", "creative", "spectator", "survival"};
        return Arrays.asList(gameMode);
    }

    @Override
    public GameMode resolve(@NotNull String arg) {
        return GameMode.valueOf(arg.toUpperCase());
    }
}
