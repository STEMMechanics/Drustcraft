package com.stemcraft.tabcompletions;

import com.stemcraft.SMTabCompletion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SMTabCompletionPlayer extends SMTabCompletion<Player> {

    public SMTabCompletionPlayer() {
        super("player");
    }

    @Override
    public List<String> completions(String arg) {
        List<String> completions = new ArrayList<>();
        for(Player player : Bukkit.getOnlinePlayers()) {
            if(player.getName().toLowerCase().startsWith(arg.toLowerCase())) {
                completions.add(player.getName());
            }
        }
        return completions;
    }

    @Override
    public Player resolve(String arg) {
        return Bukkit.getPlayerExact(arg);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Player> resolveAll(Collection<String> args) {
        return (Collection<Player>) Bukkit.getOnlinePlayers();
    }
}
