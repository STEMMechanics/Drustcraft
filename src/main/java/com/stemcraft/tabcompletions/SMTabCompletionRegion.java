package com.stemcraft.tabcompletions;

import com.stemcraft.SMRegion;
import com.stemcraft.SMTabCompletion;

import java.util.List;

public class SMTabCompletionRegion extends SMTabCompletion<SMRegion> {

    public SMTabCompletionRegion() {
        super("region");
    }

    @Override
    public List<String> completions(String arg) {
        return SMRegion.getRegionNames();
    }

    @Override
    public SMRegion resolve(String arg) {
        return SMRegion.get(arg);
    }
}
