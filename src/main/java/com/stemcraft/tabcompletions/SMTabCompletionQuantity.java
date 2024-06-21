package com.stemcraft.tabcompletions;

import com.stemcraft.SMTabCompletion;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class SMTabCompletionQuantity extends SMTabCompletion<Integer> {

    public SMTabCompletionQuantity() {
        super("quantity");
    }

    @Override
    public List<String> completions(String arg) {
        List<String> quantity = new ArrayList<>();

        quantity.add("1");
        quantity.add("2");
        quantity.add("3");
        quantity.add("5");
        quantity.add("10");
        quantity.add("15");
        quantity.add("20");
        quantity.add("50");
        quantity.add("64");

        return quantity;
    }

    @Override
    public Integer resolve(String arg) {
        return Integer.parseInt(arg);
    }
}
