package com.stemcraft.tabcompletions;

import com.stemcraft.SMConfig;
import com.stemcraft.SMTabCompletion;

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
