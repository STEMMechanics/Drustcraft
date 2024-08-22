package com.stemcraft.tabcompletions;

import com.stemcraft.SMTabCompletion;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class SMTabCompletionWorld extends SMTabCompletion<World> {

    public SMTabCompletionWorld() {
        super("world");
    }

    @Override
    public List<String> completions(String arg) {
        List<String> names = new ArrayList<>();

        Bukkit.getServer().getWorlds().forEach(world -> {
            names.add(world.getName());
        });

        return names;
    }

    @Override
    public World resolve(String arg) {
        return Bukkit.getWorld(arg);
    }
}
