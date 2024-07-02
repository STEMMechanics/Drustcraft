package com.stemcraft.tabcompletions;

import com.stemcraft.SMConfig;
import com.stemcraft.SMTabCompletion;
import com.stemcraft.STEMCraft;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class SMTabCompletionBook extends SMTabCompletion<String> {

    public SMTabCompletionBook() {
        super("book");
    }

    @Override
    public List<String> completions(String arg) {
        return SMConfig.getKeys("books");
    }

    @Override
    public String resolve(String arg) {
        if(SMConfig.contains("books." + arg)) {
            return arg;
        }

        return null;
    }
}
