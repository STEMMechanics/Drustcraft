package com.stemcraft.tabcompletions;

import com.stemcraft.SMTabCompletion;
import com.stemcraft.utils.SMUtilsString;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class SMTabCompletionInteger extends SMTabCompletion<Integer> {

    public SMTabCompletionInteger() {
        super("int");
    }

    @Override
    public List<String> completions(String arg) {
        String[] integers = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        List<String> result = new ArrayList<>();

        if (SMUtilsString.isInteger(arg)) {
            for (String integer : integers) {
                result.add(arg + integer);
            }
        } else {
            result = Arrays.asList(integers);
        }

        return result;
    }

    @Override
    public Integer resolve(String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
