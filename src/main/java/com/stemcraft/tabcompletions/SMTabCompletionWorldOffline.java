package com.stemcraft.tabcompletions;

import com.stemcraft.SMTabCompletion;
import com.stemcraft.SMWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SMTabCompletionWorldOffline extends SMTabCompletion<World> {

    public SMTabCompletionWorldOffline() {
        super("world-offline");
    }

    @Override
    public List<String> completions(String arg) {
        Collection<String> names = SMWorld.list();

        names.removeIf(SMWorld::isLoaded);

        return new ArrayList<>(names);
    }
}
